from datetime import datetime
from typing import Literal

from pydantic import BaseModel

from app.movies.schemas import MovieOut


class InteractRequest(BaseModel):
    tmdb_id: int
    feed_type: Literal["discovery", "library_build"]
    direction: Literal["left", "right", "up", "down"]
    hearted: bool = False
    rating: float | None = None
    review: str | None = None


class InteractionOut(BaseModel):
    id: int
    movie_id: int
    feed_type: str
    direction: str
    hearted: bool
    rating: float | None
    review: str | None

    model_config = {"from_attributes": True}


class WatchedEntry(BaseModel):
    """A movie the user logged as watched, with their rating/review."""
    movie: MovieOut
    rating: float | None
    review: str | None
    watched_at: datetime


class LogRequest(BaseModel):
    """Idempotent set of the user's relationship to a film (film-page editor)."""
    tmdb_id: int
    status: Literal["watched", "watching", "plan", "not_interested"]
    rating: float | None = None
    review: str | None = None
