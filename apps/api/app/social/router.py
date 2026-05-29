from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.library.models import Interaction
from app.movies.models import Movie
from app.movies.schemas import MovieOut
from app.profile import service as profile_service
from app.profile.schemas import UserCard
from app.social.schemas import FeedItem

router = APIRouter(prefix="/feed", tags=["feed"])

_DIR_STATUS = {"right": "watched", "up": "watching", "down": "plan"}


@router.get("", response_model=list[FeedItem])
async def feed(
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Recent activity from the people the current user follows."""
    followees = await profile_service.following_ids(db, current_user.id)
    if not followees:
        return []

    result = await db.execute(
        select(Interaction, Movie, User)
        .join(Movie, Movie.id == Interaction.movie_id)
        .join(User, User.id == Interaction.user_id)
        .where(
            Interaction.user_id.in_(followees),
            Interaction.direction.in_(["right", "up", "down"]),
        )
        .order_by(Interaction.interacted_at.desc())
        .limit(limit)
    )
    items: list[FeedItem] = []
    for interaction, movie, user in result.all():
        items.append(
            FeedItem(
                user=UserCard(
                    id=user.id,
                    username=user.username,
                    display_name=user.display_name,
                    avatar_url=user.avatar_url,
                    is_following=True,
                ),
                movie=MovieOut.model_validate(movie),
                status=_DIR_STATUS.get(interaction.direction, "watched"),
                rating=interaction.rating,
                review=interaction.review,
                at=interaction.interacted_at,
            )
        )
    return items
