"""Movie ingestion: fetches from TMDB and upserts into the local movies table."""
import logging
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.movies import tmdb
from app.movies.models import Movie

logger = logging.getLogger(__name__)


def _map_item(item: dict[str, Any], genre_map: dict[int, str]) -> dict[str, Any]:
    genres = [
        genre_map[gid]
        for gid in item.get("genre_ids", [])
        if gid in genre_map
    ]
    return {
        "tmdb_id": item["id"],
        "title": item.get("title") or item.get("name") or "Unknown",
        "year": tmdb.extract_year(item),
        "media_type": item.get("media_type", "movie"),
        "overview": item.get("overview") or "",
        "poster_path": item.get("poster_path"),
        "backdrop_path": item.get("backdrop_path"),
        "genres": genres,
        "vote_average": float(item.get("vote_average") or 0),
        "popularity": float(item.get("popularity") or 0),
        "keywords": [],
        "cast": [],
        "crew": [],
    }


async def upsert_movies(
    db: AsyncSession, items: list[dict[str, Any]], genre_map: dict[int, str]
) -> int:
    if not items:
        return 0
    rows = [_map_item(i, genre_map) for i in items if i.get("id")]
    stmt = (
        pg_insert(Movie)
        .values(rows)
        .on_conflict_do_update(
            index_elements=["tmdb_id"],
            set_={
                "popularity": pg_insert(Movie).excluded.popularity,
                "vote_average": pg_insert(Movie).excluded.vote_average,
                "genres": pg_insert(Movie).excluded.genres,
            },
        )
    )
    await db.execute(stmt)
    await db.commit()
    return len(rows)


async def ingest_popular(db: AsyncSession, pages: int = 5) -> int:
    genre_map = await tmdb.fetch_genre_map()
    total = 0

    for page in range(1, pages + 1):
        results = await tmdb.fetch_popular(page)
        total += await upsert_movies(db, results, genre_map)
        logger.info("Ingested page %d (%d movies so far)", page, total)

    trending = await tmdb.fetch_trending()
    total += await upsert_movies(db, trending, genre_map)
    logger.info("Ingestion complete — %d movies total", total)
    return total


async def movie_count(db: AsyncSession) -> int:
    result = await db.execute(select(func.count()).select_from(Movie))
    return result.scalar() or 0


# ── Search ──────────────────────────────────────────────────────────────────

async def search_and_cache(
    db: AsyncSession, query: str, limit: int = 20
) -> list[Movie]:
    """Multi-search TMDB, cache results, return matching rows in relevance order."""
    raw = await tmdb.search_movies(query)
    items = [
        i for i in raw
        if i.get("media_type") in ("movie", "tv")
        and i.get("id")
        and i.get("poster_path")
    ][:limit]
    if not items:
        return []
    genre_map = await tmdb.fetch_genre_map()
    await upsert_movies(db, items, genre_map)

    ids = [i["id"] for i in items]
    result = await db.execute(select(Movie).where(Movie.tmdb_id.in_(ids)))
    by_tmdb = {m.tmdb_id: m for m in result.scalars().all()}
    # preserve TMDB relevance ordering
    return [by_tmdb[i] for i in ids if i in by_tmdb]


# ── Detail enrichment ───────────────────────────────────────────────────────

def _parse_detail(data: dict[str, Any]) -> dict[str, Any]:
    credits = data.get("credits") or {}
    cast = [
        {
            "name": c.get("name"),
            "character": c.get("character"),
            "profile_path": c.get("profile_path"),
        }
        for c in (credits.get("cast") or [])[:12]
    ]
    crew_all = credits.get("crew") or []
    director = next(
        (c.get("name") for c in crew_all if c.get("job") == "Director"),
        None,
    )
    notable_jobs = {"Director", "Writer", "Screenplay", "Story", "Producer"}
    crew = [
        {
            "name": c.get("name"),
            "job": c.get("job"),
            "profile_path": c.get("profile_path"),
        }
        for c in crew_all
        if c.get("job") in notable_jobs
    ][:8]

    videos = (data.get("videos") or {}).get("results") or []
    trailer = next(
        (v.get("key") for v in videos
         if v.get("site") == "YouTube" and v.get("type") == "Trailer"),
        next((v.get("key") for v in videos if v.get("site") == "YouTube"), None),
    )

    similar = [
        {
            "tmdb_id": s.get("id"),
            "title": s.get("title") or s.get("name") or "Untitled",
            "year": tmdb.extract_year(s),
            "poster_path": s.get("poster_path"),
        }
        for s in ((data.get("similar") or {}).get("results") or [])
        if s.get("id") and s.get("poster_path")
    ][:8]

    providers_root = (data.get("watch/providers") or {}).get("results") or {}
    region = providers_root.get("US") or next(iter(providers_root.values()), {})
    seen: set[str] = set()
    watch_providers: list[dict[str, Any]] = []
    for bucket in ("flatrate", "rent", "buy"):
        for prov in region.get(bucket, []) or []:
            name = prov.get("provider_name")
            if name and name not in seen:
                seen.add(name)
                watch_providers.append(
                    {"name": name, "logo_path": prov.get("logo_path")}
                )

    keywords = [
        k.get("name")
        for k in ((data.get("keywords") or {}).get("keywords") or [])
        if k.get("name")
    ]
    genres = [g.get("name") for g in (data.get("genres") or []) if g.get("name")]

    return {
        "director": director,
        "cast": cast,
        "crew": crew,
        "trailer_key": trailer,
        "similar": similar,
        "watch_providers": watch_providers,
        "keywords": keywords,
        "genres": genres,
        "runtime": data.get("runtime"),
        "backdrop_path": data.get("backdrop_path"),
        "overview": data.get("overview"),
    }


async def get_movie_detail(db: AsyncSession, tmdb_id: int) -> dict[str, Any] | None:
    """Fetch full TMDB detail, persist the cacheable fields onto the movies row,
    and return the parsed detail (incl. live-only fields like trailer/similar)."""
    try:
        data = await tmdb.fetch_movie_detail(tmdb_id)
    except Exception:
        logger.exception("TMDB detail fetch failed for %s", tmdb_id)
        data = None

    if not data or not data.get("id"):
        # Fall back to whatever we have cached
        result = await db.execute(select(Movie).where(Movie.tmdb_id == tmdb_id))
        movie = result.scalar_one_or_none()
        if not movie:
            return None
        return _detail_from_row(movie)

    parsed = _parse_detail(data)

    # Persist the cacheable subset onto the row (create if missing).
    row = {
        "tmdb_id": tmdb_id,
        "title": data.get("title") or data.get("name") or "Untitled",
        "year": tmdb.extract_year(data),
        "media_type": "movie",
        "overview": parsed["overview"] or "",
        "poster_path": data.get("poster_path"),
        "backdrop_path": parsed["backdrop_path"],
        "genres": parsed["genres"],
        "vote_average": float(data.get("vote_average") or 0),
        "popularity": float(data.get("popularity") or 0),
        "runtime": parsed["runtime"],
        "keywords": parsed["keywords"],
        "cast": parsed["cast"],
        "crew": parsed["crew"],
    }
    stmt = (
        pg_insert(Movie)
        .values(row)
        .on_conflict_do_update(
            index_elements=["tmdb_id"],
            set_={
                k: pg_insert(Movie).excluded[k]
                for k in (
                    "title", "year", "overview", "poster_path", "backdrop_path",
                    "genres", "vote_average", "popularity", "runtime",
                    "keywords", "cast", "crew",
                )
            },
        )
    )
    await db.execute(stmt)
    await db.commit()

    result = await db.execute(select(Movie).where(Movie.tmdb_id == tmdb_id))
    movie = result.scalar_one()
    detail = _detail_from_row(movie)
    detail.update(
        director=parsed["director"],
        trailer_key=parsed["trailer_key"],
        similar=parsed["similar"],
        watch_providers=parsed["watch_providers"],
    )
    return detail


def _detail_from_row(m: Movie) -> dict[str, Any]:
    """Build a detail payload from a cached row (live-only fields default empty)."""
    director = next(
        (c.get("name") for c in (m.crew or []) if c.get("job") == "Director"), None
    )
    return {
        "id": m.id,
        "tmdb_id": m.tmdb_id,
        "title": m.title,
        "year": m.year,
        "media_type": m.media_type,
        "overview": m.overview,
        "poster_path": m.poster_path,
        "backdrop_path": m.backdrop_path,
        "genres": m.genres or [],
        "vote_average": m.vote_average,
        "runtime": m.runtime,
        "cast": m.cast or [],
        "crew": m.crew or [],
        "director": director,
        "trailer_key": None,
        "similar": [],
        "watch_providers": [],
    }
