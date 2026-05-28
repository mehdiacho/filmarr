from datetime import date, datetime

from pydantic import BaseModel, Field

from app.movies.schemas import MovieOut


class GenreCount(BaseModel):
    name: str
    count: int


class DecadeCount(BaseModel):
    decade: int  # e.g. 1990
    count: int


class ProfileStats(BaseModel):
    films_watched: int
    reviews: int
    avg_rating: float | None
    rating_histogram: dict[str, int]  # "0.5".."5.0" -> count
    top_genres: list[GenreCount]
    top_decades: list[DecadeCount]


class PublicProfile(BaseModel):
    id: int
    username: str | None
    display_name: str | None
    bio: str | None
    avatar_url: str | None
    joined: datetime
    favorites: list[MovieOut]
    stats: ProfileStats


class MyProfile(PublicProfile):
    email: str
    date_of_birth: date | None
    preferences: dict


class ProfileUpdate(BaseModel):
    username: str | None = Field(default=None, max_length=40)
    display_name: str | None = Field(default=None, max_length=80)
    bio: str | None = Field(default=None, max_length=2000)
    date_of_birth: date | None = None
    favorite_film_ids: list[int] | None = Field(default=None, max_length=8)
