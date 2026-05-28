"""profile fields

Revision ID: 0002_profile_fields
Revises: 0001_create_schema
Create Date: 2026-05-28

"""
from alembic import op
import sqlalchemy as sa


revision = '0002_profile_fields'
down_revision = '0001_create_schema'
branch_labels = None
depends_on = None


def upgrade() -> None:
    conn = op.get_bind()
    conn.execute(sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(40)"))
    conn.execute(sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(80)"))
    conn.execute(sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT"))
    conn.execute(sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512)"))
    conn.execute(sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE"))
    conn.execute(sa.text(
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS favorite_film_ids JSON NOT NULL DEFAULT '[]'"
    ))
    conn.execute(sa.text(
        "CREATE UNIQUE INDEX IF NOT EXISTS ix_users_username ON users (username)"
    ))


def downgrade() -> None:
    conn = op.get_bind()
    conn.execute(sa.text("DROP INDEX IF EXISTS ix_users_username"))
    for col in (
        "username", "display_name", "bio", "avatar_url",
        "date_of_birth", "favorite_film_ids",
    ):
        conn.execute(sa.text(f"ALTER TABLE users DROP COLUMN IF EXISTS {col}"))
