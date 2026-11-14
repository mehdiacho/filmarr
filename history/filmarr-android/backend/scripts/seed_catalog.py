"""
Seed the Firestore catalog from TMDB (GRAPH_RECOMMENDER.md §4 Phase 1, SETUP.md §5).

Two outputs, both idempotent:

  1. `movies/{tmdbId}`  — cached TMDB metadata + `popularity` (+ the id sets the
     Jaccard similarity is computed over: genre_ids, keyword_ids, cast_ids).
  2. `edges/{src}_{dst}_content_sim` — precomputed `content_sim` edges from
     genre/keyword/cast **Jaccard** overlap. This is the day-1, no-embeddings path:
     FirestoreGraphStore(content_sim_source="precomputed") reads these directly, so
     discovery + library feeds work with zero embedding infrastructure. Swapping to
     synopsis embeddings + find_nearest later touches only the store, not this contract.

Auth: TMDB v4 read token via env TMDB_READ_TOKEN (Bearer). Firestore via ADC
(`gcloud auth application-default login`), project from --project / GOOGLE_CLOUD_PROJECT.

Usage:
    python -m scripts.seed_catalog --pages 25                 # ~500 popular titles
    python -m scripts.seed_catalog --pages 5 --dry-run        # fetch + compute, no writes
    python -m scripts.seed_catalog --pages 25 --project filmarr-mehdiacho

Anti-goals respected (§15): no all-pairs — Jaccard is restricted to a candidate
neighborhood (titles sharing >=1 genre or keyword); top-k kept per node.
"""

from __future__ import annotations

import argparse
import os
import sys
import time
from collections import defaultdict
from typing import Any, Iterable

import requests

TMDB_BASE = "https://api.themoviedb.org/3"

# Jaccard blend weights over the three id sets (§4). Keywords carry the most specific
# signal, genres the coarsest; cast bridges franchises/collaborators.
W_GENRE, W_KEYWORD, W_CAST = 0.25, 0.50, 0.25
TOP_CAST = 10          # how many billed cast members feed the cast set
K_EDGES = 40           # max content_sim neighbors kept per title
MIN_SIM = 0.05         # prune near-zero overlaps
FIRESTORE_BATCH = 400  # < 500 op/commit limit, leaves headroom


# --------------------------------------------------------------------------- #
# TMDB fetch                                                                   #
# --------------------------------------------------------------------------- #

def _session(token: str) -> requests.Session:
    s = requests.Session()
    s.headers.update({"Authorization": f"Bearer {token}", "accept": "application/json"})
    return s


def _get(s: requests.Session, path: str, **params: Any) -> dict:
    for attempt in range(5):
        r = s.get(f"{TMDB_BASE}{path}", params=params, timeout=20)
        if r.status_code == 429:  # rate limited — respect Retry-After
            time.sleep(float(r.headers.get("Retry-After", "1")) + 0.5)
            continue
        r.raise_for_status()
        return r.json()
    r.raise_for_status()
    return {}


def fetch_popular_ids(s: requests.Session, pages: int) -> list[int]:
    ids: list[int] = []
    for page in range(1, pages + 1):
        data = _get(s, "/movie/popular", page=page, language="en-US")
        ids.extend(int(m["id"]) for m in data.get("results", []))
        time.sleep(0.05)
    # De-dup, preserve order.
    return list(dict.fromkeys(ids))


def fetch_movie(s: requests.Session, movie_id: int) -> dict | None:
    try:
        d = _get(
            s, f"/movie/{movie_id}",
            language="en-US",
            append_to_response="keywords,credits",
        )
    except requests.HTTPError:
        return None

    genres = d.get("genres") or []
    keywords = (d.get("keywords") or {}).get("keywords") or []
    credits = d.get("credits") or {}
    cast = credits.get("cast") or []
    crew = credits.get("crew") or []
    director = next((c["name"] for c in crew if c.get("job") == "Director"), None)
    year_str = (d.get("release_date") or "")[:4]
    year = int(year_str) if year_str.isdigit() else 0

    return {
        "id": int(d["id"]),
        "title": d.get("title") or d.get("original_title") or "",
        "original_title": d.get("original_title") or "",
        "year": year,
        "release_date": d.get("release_date") or None,
        "genres": [g["name"] for g in genres],
        "genre_ids": [int(g["id"]) for g in genres],
        "keywords": [k["name"] for k in keywords],
        "keyword_ids": [int(k["id"]) for k in keywords],
        "cast": [c["name"] for c in cast[:TOP_CAST]],
        "cast_ids": [int(c["id"]) for c in cast[:TOP_CAST]],
        "director": director,
        "overview": d.get("overview") or "",
        "poster_path": d.get("poster_path"),
        "backdrop_path": d.get("backdrop_path"),
        "popularity": float(d.get("popularity") or 0.0),
        "vote_average": float(d.get("vote_average") or 0.0),
        "vote_count": int(d.get("vote_count") or 0),
        "runtime": int(d.get("runtime") or 0),
        "original_language": d.get("original_language") or None,
    }


# --------------------------------------------------------------------------- #
# Jaccard content_sim edges                                                    #
# --------------------------------------------------------------------------- #

def _jaccard(a: set[int], b: set[int]) -> float:
    if not a or not b:
        return 0.0
    inter = len(a & b)
    if inter == 0:
        return 0.0
    return inter / len(a | b)


def build_content_edges(movies: list[dict]) -> list[tuple[int, int, float]]:
    """Undirected content_sim edges, restricted to a candidate neighborhood
    (titles sharing >=1 genre or keyword) — never all-pairs (§15)."""
    genres = {m["id"]: set(m["genre_ids"]) for m in movies}
    keywords = {m["id"]: set(m["keyword_ids"]) for m in movies}
    cast = {m["id"]: set(m["cast_ids"]) for m in movies}

    # Inverted indexes -> candidate generation.
    by_feature: dict[tuple[str, int], list[int]] = defaultdict(list)
    for m in movies:
        for gid in m["genre_ids"]:
            by_feature[("g", gid)].append(m["id"])
        for kid in m["keyword_ids"]:
            by_feature[("k", kid)].append(m["id"])

    # Per-node top-k, symmetric.
    neighbors: dict[int, list[tuple[int, float]]] = {m["id"]: [] for m in movies}
    seen_pairs: set[tuple[int, int]] = set()

    for m in movies:
        mid = m["id"]
        cand: set[int] = set()
        for gid in m["genre_ids"]:
            cand.update(by_feature[("g", gid)])
        for kid in m["keyword_ids"]:
            cand.update(by_feature[("k", kid)])
        cand.discard(mid)

        for other in cand:
            key = (mid, other) if mid < other else (other, mid)
            if key in seen_pairs:
                continue
            seen_pairs.add(key)
            sim = (
                W_GENRE * _jaccard(genres[mid], genres[other])
                + W_KEYWORD * _jaccard(keywords[mid], keywords[other])
                + W_CAST * _jaccard(cast[mid], cast[other])
            )
            if sim >= MIN_SIM:
                neighbors[mid].append((other, sim))
                neighbors[other].append((mid, sim))

    edges: list[tuple[int, int, float]] = []
    for src, nbrs in neighbors.items():
        nbrs.sort(key=lambda t: t[1], reverse=True)
        for dst, w in nbrs[:K_EDGES]:
            edges.append((src, dst, round(w, 6)))
    return edges


# --------------------------------------------------------------------------- #
# Firestore writes                                                             #
# --------------------------------------------------------------------------- #

def _batched(seq: list, size: int) -> Iterable[list]:
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


def write_movies(db, movies: list[dict]) -> None:
    col = db.collection("movies")
    for chunk in _batched(movies, FIRESTORE_BATCH):
        batch = db.batch()
        for m in chunk:
            batch.set(col.document(str(m["id"])), m)
        batch.commit()
    print(f"  wrote {len(movies)} movies/")


def write_edges(db, edges: list[tuple[int, int, float]]) -> None:
    col = db.collection("edges")
    docs = [
        (
            f"{src}_{dst}_content_sim",
            {
                "src_id": src,
                "dst_id": dst,
                "edge_type": "content_sim",
                "weight": w,
                "sign": 1,
                "n_support": None,
            },
        )
        for src, dst, w in edges
    ]
    for chunk in _batched(docs, FIRESTORE_BATCH):
        batch = db.batch()
        for doc_id, data in chunk:
            batch.set(col.document(doc_id), data)
        batch.commit()
    print(f"  wrote {len(edges)} content_sim edges/")


# --------------------------------------------------------------------------- #
# Main                                                                         #
# --------------------------------------------------------------------------- #

def main() -> int:
    ap = argparse.ArgumentParser(description="Seed Firestore catalog from TMDB (Phase 1).")
    ap.add_argument("--pages", type=int, default=25, help="TMDB /movie/popular pages (20 titles each).")
    ap.add_argument("--project", default=os.environ.get("GOOGLE_CLOUD_PROJECT"), help="GCP project id.")
    ap.add_argument("--dry-run", action="store_true", help="Fetch + compute, skip all Firestore writes.")
    args = ap.parse_args()

    token = os.environ.get("TMDB_READ_TOKEN")
    if not token:
        print("ERROR: set TMDB_READ_TOKEN (TMDB v4 read access token).", file=sys.stderr)
        return 2

    s = _session(token)
    print(f"Fetching {args.pages} pages of popular movies...")
    ids = fetch_popular_ids(s, args.pages)
    print(f"  {len(ids)} unique ids")

    movies: list[dict] = []
    for i, mid in enumerate(ids, 1):
        m = fetch_movie(s, mid)
        if m:
            movies.append(m)
        if i % 50 == 0:
            print(f"  detail {i}/{len(ids)}")
        time.sleep(0.03)
    print(f"  {len(movies)} movies with detail")

    print("Building Jaccard content_sim edges...")
    edges = build_content_edges(movies)
    print(f"  {len(edges)} directed edges (top-{K_EDGES}/node, min_sim={MIN_SIM})")

    if args.dry_run:
        print("DRY RUN — no writes. Sample edge:", edges[0] if edges else "none")
        return 0

    from google.cloud import firestore  # imported here so --dry-run needs no ADC

    db = firestore.Client(project=args.project) if args.project else firestore.Client()
    print(f"Writing to Firestore project '{db.project}'...")
    write_movies(db, movies)
    write_edges(db, edges)
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
