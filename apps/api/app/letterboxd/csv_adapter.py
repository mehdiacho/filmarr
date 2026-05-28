"""
Letterboxd CSV import format:
  Date,Name,Year,Letterboxd URI,Rating,Rewatch,Tags,Watched Date,Review

Ref: https://letterboxd.com/about/importing-data/
"""
import csv
import io
from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.letterboxd.base import LetterboxdAdapter
from app.library.models import Interaction
from app.movies.models import Movie


class CSVAdapter(LetterboxdAdapter):
    def __init__(self, db: AsyncSession):
        self.db = db

    async def export(self, user_id: int) -> bytes:
        result = await self.db.execute(
            select(Movie, Interaction)
            .join(Interaction, Interaction.movie_id == Movie.id)
            .where(Interaction.user_id == user_id, Interaction.direction == "right")
            .order_by(Interaction.interacted_at.desc())
        )
        rows = result.all()

        buf = io.StringIO()
        writer = csv.writer(buf)
        writer.writerow(["Date", "Name", "Year", "Letterboxd URI", "Rating", "Rewatch", "Tags", "Watched Date", "Review"])

        for movie, interaction in rows:
            rating_str = str(interaction.rating) if interaction.rating else ""
            watched_date = interaction.interacted_at.strftime("%Y-%m-%d") if interaction.interacted_at else ""
            writer.writerow([
                datetime.utcnow().strftime("%Y-%m-%d"),
                movie.title,
                movie.year or "",
                "",  # Letterboxd URI — blank for import
                rating_str,
                "",  # Rewatch
                "",  # Tags
                watched_date,
                interaction.review or "",
            ])

        return buf.getvalue().encode("utf-8")
