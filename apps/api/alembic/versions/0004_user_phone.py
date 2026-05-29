"""user phone

Revision ID: 0004_user_phone
Revises: 0003_follows
Create Date: 2026-05-29

"""
from alembic import op
import sqlalchemy as sa


revision = '0004_user_phone'
down_revision = '0003_follows'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.get_bind().execute(
        sa.text("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(32)")
    )


def downgrade() -> None:
    op.get_bind().execute(sa.text("ALTER TABLE users DROP COLUMN IF EXISTS phone"))
