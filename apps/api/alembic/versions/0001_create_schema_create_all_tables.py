"""create all tables

Revision ID: 0001_create_schema
Revises: 7a9300f315e2
Create Date: 2026-05-28 18:34:04.999173

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '0001_create_schema'
down_revision = '7a9300f315e2'
branch_labels = None
depends_on = None


def upgrade() -> None:
    conn = op.get_bind()

    # pgvector extension (Supabase has it available; this is idempotent)
    conn.execute(sa.text("CREATE EXTENSION IF NOT EXISTS vector"))

    # users
    conn.execute(sa.text("""
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            preferences JSON NOT NULL DEFAULT '{}',
            created_at TIMESTAMP NOT NULL DEFAULT now()
        )
    """))
    conn.execute(sa.text(
        "CREATE UNIQUE INDEX IF NOT EXISTS ix_users_email ON users (email)"
    ))

    # movies
    conn.execute(sa.text("""
        CREATE TABLE IF NOT EXISTS movies (
            id SERIAL PRIMARY KEY,
            tmdb_id INTEGER NOT NULL,
            title VARCHAR(512) NOT NULL,
            year INTEGER,
            media_type VARCHAR(10) NOT NULL DEFAULT 'movie',
            overview TEXT,
            poster_path VARCHAR(255),
            backdrop_path VARCHAR(255),
            genres JSON NOT NULL DEFAULT '[]',
            vote_average FLOAT NOT NULL DEFAULT 0,
            popularity FLOAT NOT NULL DEFAULT 0,
            runtime INTEGER,
            keywords JSON NOT NULL DEFAULT '[]',
            "cast" JSON NOT NULL DEFAULT '[]',
            crew JSON NOT NULL DEFAULT '[]',
            cached_at TIMESTAMP NOT NULL DEFAULT now()
        )
    """))
    conn.execute(sa.text(
        "CREATE UNIQUE INDEX IF NOT EXISTS ix_movies_tmdb_id ON movies (tmdb_id)"
    ))
    conn.execute(sa.text(
        "ALTER TABLE movies ADD COLUMN IF NOT EXISTS embedding vector(384)"
    ))

    # interactions
    conn.execute(sa.text("""
        CREATE TABLE IF NOT EXISTS interactions (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL REFERENCES users(id),
            movie_id INTEGER NOT NULL REFERENCES movies(id),
            feed_type VARCHAR(20) NOT NULL,
            direction VARCHAR(10) NOT NULL,
            hearted BOOLEAN NOT NULL DEFAULT false,
            rating FLOAT,
            review TEXT,
            interacted_at TIMESTAMP NOT NULL DEFAULT now()
        )
    """))
    conn.execute(sa.text(
        "CREATE INDEX IF NOT EXISTS ix_interactions_user_movie ON interactions (user_id, movie_id)"
    ))
    conn.execute(sa.text(
        "CREATE INDEX IF NOT EXISTS ix_interactions_user_feed ON interactions (user_id, feed_type)"
    ))

    # movie_edges
    conn.execute(sa.text("""
        CREATE TABLE IF NOT EXISTS movie_edges (
            id SERIAL PRIMARY KEY,
            src_movie_id INTEGER NOT NULL REFERENCES movies(id),
            dst_movie_id INTEGER NOT NULL REFERENCES movies(id),
            weight FLOAT NOT NULL,
            edge_type VARCHAR(30) NOT NULL,
            updated_at TIMESTAMP NOT NULL DEFAULT now()
        )
    """))
    conn.execute(sa.text(
        "CREATE INDEX IF NOT EXISTS ix_edges_src_type ON movie_edges (src_movie_id, edge_type)"
    ))


def downgrade() -> None:
    conn = op.get_bind()
    conn.execute(sa.text("DROP TABLE IF EXISTS movie_edges"))
    conn.execute(sa.text("DROP TABLE IF EXISTS interactions"))
    conn.execute(sa.text("DROP TABLE IF EXISTS movies"))
    conn.execute(sa.text("DROP TABLE IF EXISTS users"))
