from datetime import datetime

from pydantic import BaseModel

from app.movies.schemas import MovieOut
from app.profile.schemas import UserCard


class FeedItem(BaseModel):
    user: UserCard
    movie: MovieOut
    status: str  # watched | watching | plan
    rating: float | None
    review: str | None
    at: datetime
