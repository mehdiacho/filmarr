from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.library.models import Interaction
from app.movies import schemas, service
from app.movies.models import Movie
from app.movies.schemas import MovieDetailOut, MovieOut

router = APIRouter(prefix="/movies", tags=["movies"])

# Swipe direction -> human-readable status for a film page.
_DIRECTION_STATUS = {
    "right": "watched",
    "up": "watching",
    "down": "plan",
    "left": "not_interested",
}


@router.get("/search", response_model=list[MovieOut])
async def search(
    q: str = Query(min_length=1),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await service.search_and_cache(db, q)


@router.get("/popular", response_model=list[MovieOut])
async def popular(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Movie).order_by(Movie.popularity.desc()).limit(50)
    )
    return result.scalars().all()


@router.get("/genres", response_model=list[str])
async def genres(db: AsyncSession = Depends(get_db)):
    """Distinct genres present in the local catalogue, most common first."""
    result = await db.execute(select(Movie.genres))
    counts: dict[str, int] = {}
    for (genre_list,) in result.all():
        for g in genre_list or []:
            counts[g] = counts.get(g, 0) + 1
    return sorted(counts, key=lambda g: counts[g], reverse=True)


@router.get("/{tmdb_id}/detail", response_model=MovieDetailOut)
async def movie_detail(
    tmdb_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    detail = await service.get_movie_detail(db, tmdb_id)
    if detail is None:
        raise HTTPException(status_code=404, detail="Movie not found")

    # Attach the viewer's relationship to this film.
    result = await db.execute(
        select(Interaction)
        .where(
            Interaction.user_id == current_user.id,
            Interaction.movie_id == detail["id"],
        )
        .order_by(Interaction.interacted_at.desc())
    )
    interactions = result.scalars().all()
    viewer = schemas.ViewerStatus()
    if interactions:
        latest = interactions[0]
        viewer.status = _DIRECTION_STATUS.get(latest.direction)
        viewer.rating = latest.rating
        viewer.review = latest.review
        viewer.hearted = any(i.hearted for i in interactions)
    detail["viewer"] = viewer
    return detail


@router.get("/{tmdb_id}", response_model=MovieOut)
async def get_movie(tmdb_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Movie).where(Movie.tmdb_id == tmdb_id))
    movie = result.scalar_one_or_none()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")
    return movie
