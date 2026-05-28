from pydantic import BaseModel
from typing import Literal


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
