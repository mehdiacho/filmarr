"""TMDB API client. Uses Bearer token (Read Access Token) auth."""
from typing import Any
import httpx
from app.config import settings


def _headers() -> dict[str, str]:
    return {
        "accept": "application/json",
        "Authorization": f"Bearer {settings.tmdb_api_key}",
    }


async def _get(path: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
    url = f"{settings.tmdb_base_url}{path}"
    async with httpx.AsyncClient() as client:
        r = await client.get(url, params=params or {}, headers=_headers(), timeout=10)
        r.raise_for_status()
        return r.json()  # type: ignore[no-any-return]


async def fetch_popular(page: int = 1) -> list[dict[str, Any]]:
    data = await _get("/movie/popular", {"page": page})
    return data.get("results", [])


async def fetch_trending() -> list[dict[str, Any]]:
    data = await _get("/trending/movie/week")
    return data.get("results", [])


async def fetch_movie_detail(tmdb_id: int) -> dict[str, Any]:
    return await _get(f"/movie/{tmdb_id}", {"append_to_response": "keywords,credits"})


async def search_movies(query: str, page: int = 1) -> list[dict[str, Any]]:
    data = await _get("/search/multi", {"query": query, "page": page})
    return data.get("results", [])


async def fetch_genre_map() -> dict[int, str]:
    data = await _get("/genre/movie/list")
    return {g["id"]: g["name"] for g in data.get("genres", [])}


def extract_year(data: dict[str, Any]) -> int | None:
    date = data.get("release_date") or data.get("first_air_date") or ""
    try:
        return int(date[:4]) if len(date) >= 4 else None
    except ValueError:
        return None
