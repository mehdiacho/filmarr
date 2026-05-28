from datetime import datetime
from sqlalchemy import Integer, String, Float, Text, DateTime, Boolean, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column
from app.db.base import Base


class Interaction(Base):
    __tablename__ = "interactions"
    __table_args__ = (
        Index("ix_interactions_user_movie", "user_id", "movie_id"),
        Index("ix_interactions_user_feed", "user_id", "feed_type"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    movie_id: Mapped[int] = mapped_column(Integer, ForeignKey("movies.id"), nullable=False)
    feed_type: Mapped[str] = mapped_column(String(20), nullable=False)  # discovery | library_build
    direction: Mapped[str] = mapped_column(String(10), nullable=False)  # left|right|up|down
    hearted: Mapped[bool] = mapped_column(Boolean, default=False)
    rating: Mapped[float | None] = mapped_column(Float, nullable=True)
    review: Mapped[str | None] = mapped_column(Text, nullable=True)
    interacted_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class MovieEdge(Base):
    """Content-similarity / co-preference edges between movies."""
    __tablename__ = "movie_edges"
    __table_args__ = (
        Index("ix_edges_src_type", "src_movie_id", "edge_type"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    src_movie_id: Mapped[int] = mapped_column(Integer, ForeignKey("movies.id"), nullable=False)
    dst_movie_id: Mapped[int] = mapped_column(Integer, ForeignKey("movies.id"), nullable=False)
    weight: Mapped[float] = mapped_column(Float, nullable=False)
    edge_type: Mapped[str] = mapped_column(String(30), nullable=False)  # content | pcc | gnn
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
