from pydantic import BaseModel


class MovieOut(BaseModel):
    id: int
    tmdb_id: int
    title: str
    year: int | None
    media_type: str
    overview: str | None
    poster_path: str | None
    backdrop_path: str | None
    genres: list[str]
    vote_average: float
    runtime: int | None

    model_config = {"from_attributes": True}
