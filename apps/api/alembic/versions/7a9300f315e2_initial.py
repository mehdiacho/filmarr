"""initial

Revision ID: 7a9300f315e2
Revises:
Create Date: 2026-05-28 17:50:19.204425

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '7a9300f315e2'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Enable pgvector extension
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    # users
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("email", sa.String(255), nullable=False),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("preferences", sa.JSON(), nullable=False, server_default="{}"),
        sa.Column("created_at", sa.DateTime(), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_users_email", "users", ["email"], unique=True)

    # movies
    op.create_table(
        "movies",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("tmdb_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(512), nullable=False),
        sa.Column("year", sa.Integer(), nullable=True),
        sa.Column("media_type", sa.String(10), nullable=False, server_default="movie"),
        sa.Column("overview", sa.Text(), nullable=True),
        sa.Column("poster_path", sa.String(255), nullable=True),
        sa.Column("backdrop_path", sa.String(255), nullable=True),
        sa.Column("genres", sa.JSON(), nullable=False, server_default="[]"),
        sa.Column("vote_average", sa.Float(), nullable=False, server_default="0"),
        sa.Column("popularity", sa.Float(), nullable=False, server_default="0"),
        sa.Column("runtime", sa.Integer(), nullable=True),
        sa.Column("keywords", sa.JSON(), nullable=False, server_default="[]"),
        sa.Column("cast", sa.JSON(), nullable=False, server_default="[]"),
        sa.Column("crew", sa.JSON(), nullable=False, server_default="[]"),
        # Vector column added as raw DDL to avoid needing pgvector installed at migration time
        sa.Column("cached_at", sa.DateTime(), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_movies_tmdb_id", "movies", ["tmdb_id"], unique=True)
    # Add the vector column separately so pgvector extension is guaranteed to exist first
    op.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS embedding vector(384)")

    # interactions
    op.create_table(
        "interactions",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("movie_id", sa.Integer(), sa.ForeignKey("movies.id"), nullable=False),
        sa.Column("feed_type", sa.String(20), nullable=False),
        sa.Column("direction", sa.String(10), nullable=False),
        sa.Column("hearted", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("rating", sa.Float(), nullable=True),
        sa.Column("review", sa.Text(), nullable=True),
        sa.Column("interacted_at", sa.DateTime(), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_interactions_user_movie", "interactions", ["user_id", "movie_id"])
    op.create_index("ix_interactions_user_feed", "interactions", ["user_id", "feed_type"])

    # movie_edges
    op.create_table(
        "movie_edges",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("src_movie_id", sa.Integer(), sa.ForeignKey("movies.id"), nullable=False),
        sa.Column("dst_movie_id", sa.Integer(), sa.ForeignKey("movies.id"), nullable=False),
        sa.Column("weight", sa.Float(), nullable=False),
        sa.Column("edge_type", sa.String(30), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_edges_src_type", "movie_edges", ["src_movie_id", "edge_type"])


def downgrade() -> None:
    op.drop_table("movie_edges")
    op.drop_table("interactions")
    op.drop_index("ix_movies_tmdb_id", "movies")
    op.drop_table("movies")
    op.drop_index("ix_users_email", "users")
    op.drop_table("users")
