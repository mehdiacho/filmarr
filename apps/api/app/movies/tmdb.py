from typing import Any
import httpx
from app.config import settings

HEADERS = {"accept": "application/json"}


async def _get(path: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
    url = f"{settings.tmdb_base_url}{path}"
    p = {"api_key": settings.tmdb_api_key, **(params or {})}
    async with httpx.AsyncClient() as client:
        r = await client.get(url, params=p, headers=HEADERS, timeout=10)
        r.raise_for_status()
        return r.json()  # type: ignore[no-any-return]


async def fetch_popular(page: int = 1) -> list[dict[str, Any]]:
    data = await _get("/movie/popular", {"page": page})
    return data.get("results", [])


async def fetch_trending() -> list[dict[str, Any]]:
    data = await _get("/trending/movie/week")
    return data.get("results", [])


async def fetch_movie(tmdb_id: int) -> dict[str, Any]:
    return await _get(f"/movie/{tmdb_id}", {"append_to_response": "keywords,credits"})


async def search_movies(query: str, page: int = 1) -> list[dict[str, Any]]:
    data = await _get("/search/multi", {"query": query, "page": page})
    return data.get("results", [])


def extract_genres(data: dict[str, Any]) -> list[str]:
    return [g["name"] for g in data.get("genres", [])]


def extract_year(data: dict[str, Any]) -> int | None:
    date = data.get("release_date") or data.get("first_air_date") or ""
    return int(date[:4]) if len(date) >= 4 else None
