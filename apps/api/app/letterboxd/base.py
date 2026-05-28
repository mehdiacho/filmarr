from abc import ABC, abstractmethod


class LetterboxdAdapter(ABC):
    """
    Adapter interface for Letterboxd interop.
    CSV impl is the concrete impl for now.
    Drop in an API-backed impl here once access is granted.
    """

    @abstractmethod
    async def export(self, user_id: int) -> bytes:
        """Return export payload (CSV bytes, or API response)."""
        ...
