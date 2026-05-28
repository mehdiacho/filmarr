"""
Phase 1 content-based recommender.

Two distinct scoring functions over the same movie pool:

* discovery  — "stuff you'd like": ranks unseen movies by how well their
  genres match a taste profile built from your swipes (right swipes pull a
  genre up, left swipes push it down), tie-broken by quality. Hearted-but-
  not-watched items are drip-fed back in. Cold start falls back to popularity.

* library-build — "stuff you've probably already seen": popularity-primary,
  boosted toward the genres you like and the era you tend to watch, so it
  surfaces mainstream titles you likely have an opinion on.

Note: TMDB keywords/cast/crew aren't ingested yet (empty arrays), so genres +
quality + era are the available content signals. The interface is stable, so a
later pgvector/embedding upgrade slots in behind the same scoring hooks.
"""
from collections import defaultdict

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.models import User
from app.library.models import Interaction
from app.movies.models import Movie
from app.recommender.base import BaseRecommender

# How strongly each swipe direction shifts the weight of a movie's genres.
_DIRECTION_WEIGHT: dict[str, float] = {
    "right": 1.0,   # watched / interested
    "up": 0.6,      # currently watching
    "down": 0.4,    # on the fence / plan to watch
    "left": -0.6,   # not interested
}

_CANDIDATE_CAP = 300
_DRIP_MAX = 2  # hearted-but-not-right items re-surfaced per discovery fetch


class _Profile:
    def __init__(self) -> None:
        self.genre_weight: dict[str, float] = defaultdict(float)
        self.swiped: set[int] = set()
        self.hearted_not_right: set[int] = set()
        self._years: list[int] = []

    @property
    def has_signal(self) -> bool:
        return any(w > 0 for w in self.genre_weight.values())

    @property
    def liked_genres(self) -> set[str]:
        return {g for g, w in self.genre_weight.items() if w > 0}

    @property
    def mean_year(self) -> float | None:
        return sum(self._years) / len(self._years) if self._years else None


class ContentBasedRecommender(BaseRecommender):
    def __init__(self, db: AsyncSession):
        self.db = db

    # ── data access ──────────────────────────────────────────────────────────

    async def _build_profile(self, user_id: int) -> _Profile:
        result = await self.db.execute(
            select(
                Interaction.movie_id,
                Interaction.direction,
                Interaction.hearted,
                Movie.genres,
                Movie.year,
            )
            .join(Movie, Movie.id == Interaction.movie_id)
            .where(Interaction.user_id == user_id)
        )
        profile = _Profile()
        for movie_id, direction, hearted, genres, year in result.all():
            profile.swiped.add(movie_id)
            weight = _DIRECTION_WEIGHT.get(direction, 0.0)
            for genre in genres or []:
                profile.genre_weight[genre] += weight
            if direction == "right" and year:
                profile._years.append(year)
            if hearted and direction != "right":
                profile.hearted_not_right.add(movie_id)
        return profile

    async def _candidate_pool(self, cap: int = _CANDIDATE_CAP) -> list[Movie]:
        result = await self.db.execute(
            select(Movie).order_by(Movie.popularity.desc()).limit(cap)
        )
        return list(result.scalars().all())

    async def _movies_by_ids(self, ids: list[int]) -> list[Movie]:
        if not ids:
            return []
        result = await self.db.execute(select(Movie).where(Movie.id.in_(ids)))
        return list(result.scalars().all())

    async def _onboarding_genres(self, user_id: int) -> set[str]:
        """Genres the user picked during onboarding — used to warm up the
        discovery feed before they've swiped on anything."""
        user = await self.db.get(User, user_id)
        if user and isinstance(user.preferences, dict):
            return set(user.preferences.get("genres") or [])
        return set()

    # ── discovery: "stuff you'd like" ────────────────────────────────────────

    async def recommend(self, user_id: int, limit: int = 20) -> list[Movie]:
        profile = await self._build_profile(user_id)
        candidates = await self._candidate_pool()

        if not profile.has_signal:
            # Cold start — no swipe signal yet. Use onboarding genre picks if
            # present (popularity within chosen genres first), else popularity.
            onboarding = await self._onboarding_genres(user_id)
            if onboarding:
                def cold(m: Movie) -> tuple[int, float]:
                    match = len(set(m.genres or []) & onboarding)
                    return (match, m.popularity or 0.0)

                ranked = sorted(candidates, key=cold, reverse=True)
            else:
                ranked = sorted(
                    candidates, key=lambda m: m.popularity or 0, reverse=True
                )
        else:
            def score(m: Movie) -> float:
                genre_score = sum(
                    profile.genre_weight.get(g, 0.0) for g in (m.genres or [])
                )
                quality = 0.15 * (m.vote_average or 0.0)
                return genre_score + quality

            ranked = sorted(candidates, key=score, reverse=True)

        result = [m for m in ranked if m.id not in profile.swiped][:limit]

        # Drip-feed hearted-but-not-watched items back into discovery.
        if profile.hearted_not_right:
            drip_ids_in = list(profile.hearted_not_right)[:_DRIP_MAX]
            drip = await self._movies_by_ids(drip_ids_in)
            drip_ids = {m.id for m in drip}
            result = drip + [m for m in result if m.id not in drip_ids]
            result = result[:limit]

        return result

    # ── library-build: "stuff you've probably seen" ──────────────────────────

    async def library_build_suggestions(
        self, user_id: int, limit: int = 20
    ) -> list[Movie]:
        profile = await self._build_profile(user_id)
        candidates = await self._candidate_pool()
        liked = profile.liked_genres
        mean_year = profile.mean_year

        def score(m: Movie) -> float:
            s = m.popularity or 0.0  # popularity-primary: titles you likely know
            if liked and set(m.genres or []) & liked:
                s *= 1.5  # nudge toward genres they engage with
            if mean_year and m.year:
                # closer to the era they tend to watch => more likely already seen
                proximity = max(0.0, 1.0 - abs(m.year - mean_year) / 30.0)
                s *= 1.0 + 0.5 * proximity
            return s

        ranked = sorted(candidates, key=score, reverse=True)
        return [m for m in ranked if m.id not in profile.swiped][:limit]
