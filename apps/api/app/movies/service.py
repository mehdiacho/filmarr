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


async def upsert_movies(db: AsyncSession, items: list[dict[str, Any]], genre_map: dict[int, str]) -> int:
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
