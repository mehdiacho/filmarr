from abc import ABC, abstractmethod
from app.movies.models import Movie


class BaseRecommender(ABC):
    """Swappable recommender interface — all strategies implement this."""

    @abstractmethod
    async def recommend(self, user_id: int, limit: int = 20) -> list[Movie]:
        ...

    @abstractmethod
    async def library_build_suggestions(self, user_id: int, limit: int = 20) -> list[Movie]:
        """
        Returns movies the user has likely *already seen*, ordered by
        popularity within their inferred taste profile.
        """
        ...
