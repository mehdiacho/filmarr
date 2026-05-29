"""follows

Revision ID: 0003_follows
Revises: 0002_profile_fields
Create Date: 2026-05-29

"""
from alembic import op
import sqlalchemy as sa


revision = '0003_follows'
down_revision = '0002_profile_fields'
branch_labels = None
depends_on = None


def upgrade() -> None:
    conn = op.get_bind()
    conn.execute(sa.text("""
        CREATE TABLE IF NOT EXISTS follows (
          id SERIAL PRIMARY KEY,
          follower_id INTEGER NOT NULL REFERENCES users(id),
          followee_id INTEGER NOT NULL REFERENCES users(id),
          created_at TIMESTAMP NOT NULL DEFAULT now()
        )
    """))
    conn.execute(sa.text(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_follow_pair "
        "ON follows (follower_id, followee_id)"
    ))
    conn.execute(sa.text(
        "CREATE INDEX IF NOT EXISTS ix_follows_follower_id ON follows (follower_id)"
    ))
    conn.execute(sa.text(
        "CREATE INDEX IF NOT EXISTS ix_follows_followee_id ON follows (followee_id)"
    ))


def downgrade() -> None:
    op.get_bind().execute(sa.text("DROP TABLE IF EXISTS follows"))
