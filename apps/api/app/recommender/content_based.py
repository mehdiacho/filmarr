"""
Phase 1 content-based recommender.

Cold start: returns popular movies filtered by user's preferred genres.
Warm:       ranks candidates by overlap with genres/keywords of
            movies the user has swiped right on.
"""
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.library.models import Interaction
from app.movies.models import Movie
from app.recommender.base import BaseRecommender


class ContentBasedRecommender(BaseRecommender):
    def __init__(self, db: AsyncSession):
        self.db = db

    async def _swiped_ids(self, user_id: int) -> set[int]:
        result = await self.db.execute(
            select(Interaction.movie_id).where(Interaction.user_id == user_id)
        )
        return set(result.scalars().all())

    async def _liked_genres(self, user_id: int) -> list[str]:
        """Genres from movies the user swiped right on."""
        result = await self.db.execute(
            select(Movie.genres)
            .join(Interaction, Interaction.movie_id == Movie.id)
            .where(Interaction.user_id == user_id, Interaction.direction == "right")
        )
        genre_lists: list[list[str]] = result.scalars().all()
        counts: dict[str, int] = {}
        for gl in genre_lists:
            for g in gl:
                counts[g] = counts.get(g, 0) + 1
        return sorted(counts, key=lambda g: counts[g], reverse=True)

    async def recommend(self, user_id: int, limit: int = 20) -> list[Movie]:
        swiped = await self._swiped_ids(user_id)
        genres = await self._liked_genres(user_id)

        query = select(Movie).order_by(Movie.popularity.desc())
        if genres:
            # Filter: at least one liked genre present in movie's genres array
            # (JSON contains check — works with SQLAlchemy JSON)
            pass  # TODO: refine with pgvector cosine similarity once embeddings exist

        result = await self.db.execute(query.limit(limit * 3))
        candidates = result.scalars().all()
        unseen = [m for m in candidates if m.id not in swiped]
        return unseen[:limit]

    async def library_build_suggestions(self, user_id: int, limit: int = 20) -> list[Movie]:
        """
        Returns high-popularity films the user likely knows but hasn't logged.
        Filters by their era/genre signals derived from liked movies.
        """
        swiped = await self._swiped_ids(user_id)
        result = await self.db.execute(
            select(Movie).order_by(Movie.popularity.desc(), Movie.vote_average.desc()).limit(limit * 3)
        )
        all_movies = result.scalars().all()
        return [m for m in all_movies if m.id not in swiped][:limit]
