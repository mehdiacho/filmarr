from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.movies import tmdb
from app.movies.models import Movie
from app.movies.schemas import MovieOut

router = APIRouter(prefix="/movies", tags=["movies"])


@router.get("/search", response_model=list[MovieOut])
async def search(q: str = Query(min_length=1), db: AsyncSession = Depends(get_db)):
    # TODO: upsert TMDB search results into the DB cache and return them
    await tmdb.search_movies(q)
    return []


@router.get("/popular", response_model=list[MovieOut])
async def popular(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Movie).order_by(Movie.popularity.desc()).limit(50)
    )
    return result.scalars().all()


@router.get("/genres", response_model=list[str])
async def genres(db: AsyncSession = Depends(get_db)):
    """Distinct genres present in the local catalogue, most common first.
    Used by the onboarding interest picker so it only offers genres we
    actually have content for."""
    result = await db.execute(select(Movie.genres))
    counts: dict[str, int] = {}
    for (genre_list,) in result.all():
        for g in genre_list or []:
            counts[g] = counts.get(g, 0) + 1
    return sorted(counts, key=lambda g: counts[g], reverse=True)


@router.get("/{tmdb_id}", response_model=MovieOut)
async def get_movie(tmdb_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Movie).where(Movie.tmdb_id == tmdb_id))
    movie = result.scalar_one_or_none()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")
    return movie
