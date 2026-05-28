from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.library import schemas
from app.library.models import Interaction
from app.movies.models import Movie
from app.movies.schemas import MovieOut

router = APIRouter(prefix="/library", tags=["library"])


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


@router.get("/watched", response_model=list[MovieOut])
async def watched(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Movie)
        .join(Interaction, Interaction.movie_id == Movie.id)
        .where(Interaction.user_id == current_user.id, Interaction.direction == "right")
        .order_by(Interaction.interacted_at.desc())
    )
    return result.scalars().all()
