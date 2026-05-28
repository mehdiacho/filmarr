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


class CastMember(BaseModel):
    name: str
    character: str | None = None
    profile_path: str | None = None


class CrewMember(BaseModel):
    name: str
    job: str | None = None
    profile_path: str | None = None


class SimilarMovie(BaseModel):
    tmdb_id: int
    title: str
    year: int | None = None
    poster_path: str | None = None


class WatchProvider(BaseModel):
    name: str
    logo_path: str | None = None


class ViewerStatus(BaseModel):
    """The current user's relationship to a film."""
    status: str | None = None  # watched | watching | plan | not_interested
    rating: float | None = None
    review: str | None = None
    hearted: bool = False


class MovieDetailOut(BaseModel):
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
    director: str | None
    cast: list[CastMember]
    crew: list[CrewMember]
    trailer_key: str | None
    similar: list[SimilarMovie]
    watch_providers: list[WatchProvider]
    viewer: ViewerStatus
