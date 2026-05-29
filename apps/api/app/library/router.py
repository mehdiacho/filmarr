from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.library import schemas
from app.library.models import Interaction
from app.movies.models import Movie
from app.movies.schemas import MovieOut

router = APIRouter(prefix="/library", tags=["library"])

_STATUS_DIRECTION = {
    "watched": "right",
    "watching": "up",
    "plan": "down",
    "not_interested": "left",
}


@router.post("/interact", response_model=schemas.InteractionOut, status_code=201)
async def log_interaction(
    body: schemas.InteractRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    movie = await db.execute(select(Movie).where(Movie.tmdb_id == body.tmdb_id))
    movie_row = movie.scalar_one_or_none()
    if not movie_row:
        raise HTTPException(status_code=404, detail="Movie not in local cache yet")

    interaction = Interaction(
        user_id=current_user.id,
        movie_id=movie_row.id,
        feed_type=body.feed_type,
        direction=body.direction,
        hearted=body.hearted,
        rating=body.rating,
        review=body.review,
    )
    db.add(interaction)
    await db.commit()
    await db.refresh(interaction)
    return interaction


@router.get("/diary", response_model=list[schemas.WatchedEntry])
async def diary(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Chronological log of every watch (not deduped), newest first."""
    result = await db.execute(
        select(Movie, Interaction)
        .join(Interaction, Interaction.movie_id == Movie.id)
        .where(Interaction.user_id == current_user.id, Interaction.direction == "right")
        .order_by(Interaction.interacted_at.desc())
    )
    return [
        schemas.WatchedEntry(
            movie=MovieOut.model_validate(movie),
            rating=interaction.rating,
            review=interaction.review,
            watched_at=interaction.interacted_at,
        )
        for movie, interaction in result.all()
    ]


@router.get("/watched", response_model=list[schemas.WatchedEntry])
async def watched(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    # DISTINCT ON keeps the most recent right-swipe per movie (dedupes rewatches).
    result = await db.execute(
        select(Movie, Interaction)
        .join(Interaction, Interaction.movie_id == Movie.id)
        .where(Interaction.user_id == current_user.id, Interaction.direction == "right")
        .order_by(Interaction.movie_id, Interaction.interacted_at.desc())
        .distinct(Interaction.movie_id)
    )
    entries = [
        schemas.WatchedEntry(
            movie=MovieOut.model_validate(movie),
            rating=interaction.rating,
            review=interaction.review,
            watched_at=interaction.interacted_at,
        )
        for movie, interaction in result.all()
    ]
    entries.sort(key=lambda e: e.watched_at, reverse=True)
    return entries


@router.get("/watchlist", response_model=list[schemas.WatchedEntry])
async def watchlist(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Films whose latest interaction is 'plan to watch' (swipe-down)."""
    result = await db.execute(
        select(Movie, Interaction)
        .join(Interaction, Interaction.movie_id == Movie.id)
        .where(Interaction.user_id == current_user.id)
        .order_by(Interaction.movie_id, Interaction.interacted_at.desc())
        .distinct(Interaction.movie_id)
    )
    entries = [
        schemas.WatchedEntry(
            movie=MovieOut.model_validate(movie),
            rating=interaction.rating,
            review=interaction.review,
            watched_at=interaction.interacted_at,
        )
        for movie, interaction in result.all()
        if interaction.direction == "down"
    ]
    entries.sort(key=lambda e: e.watched_at, reverse=True)
    return entries


@router.put("/log", response_model=schemas.InteractionOut)
async def log_film(
    body: schemas.LogRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Idempotently set the user's status/rating/review for a film (film-page
    editor). Updates the latest interaction in place rather than appending, so
    editing a rating doesn't create duplicate diary entries."""
    movie = await db.execute(select(Movie).where(Movie.tmdb_id == body.tmdb_id))
    movie_row = movie.scalar_one_or_none()
    if not movie_row:
        raise HTTPException(status_code=404, detail="Movie not in local cache yet")

    direction = _STATUS_DIRECTION[body.status]
    latest = await db.execute(
        select(Interaction)
        .where(
            Interaction.user_id == current_user.id,
            Interaction.movie_id == movie_row.id,
        )
        .order_by(Interaction.interacted_at.desc())
        .limit(1)
    )
    interaction = latest.scalar_one_or_none()
    if interaction is None:
        interaction = Interaction(
            user_id=current_user.id,
            movie_id=movie_row.id,
            feed_type="library_build",
        )
        db.add(interaction)
    interaction.direction = direction
    interaction.rating = body.rating
    interaction.review = body.review
    interaction.interacted_at = datetime.utcnow()
    await db.commit()
    await db.refresh(interaction)
    return interaction


@router.delete("/log/{tmdb_id}", status_code=204)
async def unlog_film(
    tmdb_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    """Remove the user's log(s) for a film entirely."""
    movie = await db.execute(select(Movie).where(Movie.tmdb_id == tmdb_id))
    movie_row = movie.scalar_one_or_none()
    if not movie_row:
        return
    await db.execute(
        delete(Interaction).where(
            Interaction.user_id == current_user.id,
            Interaction.movie_id == movie_row.id,
        )
    )
    await db.commit()
