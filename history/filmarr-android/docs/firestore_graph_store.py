"""
Firestore-backed GraphStore (GRAPH_RECOMMENDER.md §16).

Branching by edge type is the whole trick:
  - CONTENT_SIM is served LIVE via Firestore `find_nearest` (KNN vector search) over
    movie synopsis/feature embeddings. No precomputed edge table needed for Phase 1 —
    this is why day-1 ships without a separate vector DB or a batch job.
  - Every other edge type (co_rating, co_library, age_transition, era_cohort, learned)
    is read from the precomputed `edges` collection, which is empty until Phase 2+.

When the catalog outgrows exact KNN, only this file changes: point CONTENT_SIM at
Vertex AI Vector Search or Cloud SQL/pgvector. The scorer and the app never notice.

Vector search notes (from Firestore docs): server-side only (this runs in Cloud Run,
never on-device); exact KNN; embedding dims <= 2048; COSINE/DOT_PRODUCT/EUCLIDEAN.
DOT_PRODUCT on unit-normalized vectors is the cheaper equivalent of COSINE if you
normalize at write time.
"""

from __future__ import annotations

from typing import Iterable, Mapping, Optional, Sequence

from google.cloud import firestore
from google.cloud.firestore_v1.base_vector_query import DistanceMeasure
from google.cloud.firestore_v1.vector import Vector

from .base import AgeBracket, EdgeType, GraphStore, MovieId

_PRECOMPUTED = {
    EdgeType.CO_RATING,
    EdgeType.CO_LIBRARY,
    EdgeType.AGE_TRANSITION,
    EdgeType.ERA_COHORT,
    EdgeType.LEARNED,
}


def _chunk(seq: Sequence, size: int) -> Iterable[Sequence]:
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


class FirestoreGraphStore(GraphStore):
    def __init__(
        self,
        db: firestore.Client,
        *,
        movies_collection: str = "movies",
        edges_collection: str = "edges",
        embedding_field: str = "embedding",
        distance_field: str = "_dist",
        vector_k: int = 60,
        distance_measure: DistanceMeasure = DistanceMeasure.COSINE,
        live_snapshot_id: Optional[int] = None,
    ) -> None:
        self._db = db
        self._movies = db.collection(movies_collection)
        self._edges = db.collection(edges_collection)
        self._embedding_field = embedding_field
        self._distance_field = distance_field
        self._vector_k = vector_k
        self._distance_measure = distance_measure
        self._live_snapshot_id = live_snapshot_id

    # -- GraphStore protocol ------------------------------------------------

    def neighbors(
        self,
        source_ids: Sequence[MovieId],
        edge_types: Sequence[EdgeType],
        *,
        min_weight: float = 0.0,
        limit_per_source: int = 100,
        from_bracket: Optional[AgeBracket] = None,
        to_bracket: Optional[AgeBracket] = None,
    ) -> Mapping[MovieId, Sequence[tuple[MovieId, EdgeType, float]]]:
        out: dict[MovieId, list[tuple[MovieId, EdgeType, float]]] = {
            sid: [] for sid in source_ids
        }
        want = set(edge_types)

        if EdgeType.CONTENT_SIM in want:
            self._content_neighbors(source_ids, out, min_weight, min(limit_per_source, self._vector_k))

        precomputed = [t for t in want if t in _PRECOMPUTED]
        if precomputed:
            self._precomputed_neighbors(
                source_ids, precomputed, out, min_weight, limit_per_source, from_bracket
            )
        return out

    def popularity_scores(self, movie_ids: Sequence[MovieId]) -> Mapping[MovieId, float]:
        if not movie_ids:
            return {}
        refs = [self._movies.document(str(mid)) for mid in movie_ids]
        raw: dict[MovieId, float] = {}
        for snap in self._db.get_all(refs):
            if snap.exists:
                raw[int(snap.id)] = float((snap.to_dict() or {}).get("popularity", 0.0))
        if not raw:
            return {}
        hi = max(raw.values()) or 1.0
        return {mid: v / hi for mid, v in raw.items()}

    # -- CONTENT_SIM via find_nearest --------------------------------------

    def _content_neighbors(
        self,
        source_ids: Sequence[MovieId],
        out: dict[MovieId, list[tuple[MovieId, EdgeType, float]]],
        min_weight: float,
        k: int,
    ) -> None:
        # Fetch source embeddings in one batched read, then one KNN query per source
        # (find_nearest takes a single query vector). Seed counts are small, so this is fine.
        refs = [self._movies.document(str(sid)) for sid in source_ids]
        embeddings: dict[MovieId, list[float]] = {}
        for snap in self._db.get_all(refs):
            if not snap.exists:
                continue
            emb = (snap.to_dict() or {}).get(self._embedding_field)
            if emb is not None:
                embeddings[int(snap.id)] = list(emb)

        for sid, emb in embeddings.items():
            try:
                q = self._movies.find_nearest(
                    vector_field=self._embedding_field,
                    query_vector=Vector(emb),
                    distance_measure=self._distance_measure,
                    limit=k + 1,  # +1 because the source itself comes back at distance 0
                    distance_result_field=self._distance_field,
                )
                for doc in q.stream():
                    nid = int(doc.id)
                    if nid == sid:
                        continue
                    dist = float((doc.to_dict() or {}).get(self._distance_field, 2.0))
                    weight = self._distance_to_weight(dist)
                    if weight >= min_weight:
                        out[sid].append((nid, EdgeType.CONTENT_SIM, weight))
            except Exception:
                # A missing vector index or a malformed embedding shouldn't kill the whole
                # request — degrade to "no content neighbors for this seed".
                continue

    def _distance_to_weight(self, distance: float) -> float:
        # COSINE distance in [0, 2], 0 == identical -> similarity in [0, 1].
        if self._distance_measure == DistanceMeasure.COSINE:
            return max(0.0, 1.0 - distance / 2.0)
        if self._distance_measure == DistanceMeasure.DOT_PRODUCT:
            # Higher dot == more similar; clamp to non-negative for use as a weight.
            return max(0.0, distance)
        # EUCLIDEAN: smaller is closer; squash to (0, 1].
        return 1.0 / (1.0 + max(0.0, distance))

    # -- Precomputed edge types --------------------------------------------

    def _precomputed_neighbors(
        self,
        source_ids: Sequence[MovieId],
        edge_types: Sequence[EdgeType],
        out: dict[MovieId, list[tuple[MovieId, EdgeType, float]]],
        min_weight: float,
        limit_per_source: int,
        from_bracket: Optional[AgeBracket],
    ) -> None:
        type_values = [t.value for t in edge_types]
        for id_chunk in _chunk(list(source_ids), 10):  # Firestore 'in' caps at ~10
            q = self._edges.where("src_id", "in", list(id_chunk))
            if len(type_values) == 1:
                q = q.where("edge_type", "==", type_values[0])
            else:
                q = q.where("edge_type", "in", type_values[:10])
            if self._live_snapshot_id is not None:
                q = q.where("snapshot_id", "==", self._live_snapshot_id)
            if from_bracket is not None:
                q = q.where("from_bracket", "==", from_bracket.value)

            for doc in q.stream():
                d = doc.to_dict() or {}
                src = int(d["src_id"])
                weight = float(d.get("weight", 0.0))
                if weight < min_weight or src not in out:
                    continue
                try:
                    etype = EdgeType(d["edge_type"])
                except ValueError:
                    continue
                out[src].append((int(d["dst_id"]), etype, weight))

        # Keep only the strongest `limit_per_source` per source.
        for sid, items in out.items():
            if len(items) > limit_per_source:
                items.sort(key=lambda t: t[2], reverse=True)
                out[sid] = items[:limit_per_source]
