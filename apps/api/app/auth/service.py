from datetime import datetime, timedelta
from typing import Any

import bcrypt
from jose import jwt
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.models import User
from app.config import settings

# bcrypt has a hard 72-byte limit on the input; truncate consistently so
# hashing and verifying agree (and never raise on long passwords).
_BCRYPT_MAX_BYTES = 72


def hash_password(password: str) -> str:
    pw = password.encode("utf-8")[:_BCRYPT_MAX_BYTES]
    return bcrypt.hashpw(pw, bcrypt.gensalt()).decode("utf-8")


def verify_password(plain: str, hashed: str) -> bool:
    pw = plain.encode("utf-8")[:_BCRYPT_MAX_BYTES]
    try:
        return bcrypt.checkpw(pw, hashed.encode("utf-8"))
    except ValueError:
        # hashed is not a valid bcrypt hash (e.g. the placeholder user)
        return False


def create_access_token(data: dict[str, Any]) -> str:
    payload = data.copy()
    expires = timedelta(minutes=settings.access_token_expire_minutes)
    payload["exp"] = datetime.utcnow() + expires
    return jwt.encode(payload, settings.secret_key, algorithm="HS256")


async def get_user_by_email(db: AsyncSession, email: str) -> User | None:
    result = await db.execute(select(User).where(User.email == email))
    return result.scalar_one_or_none()


async def create_user(db: AsyncSession, email: str, password: str) -> User:
    user = User(email=email, password_hash=hash_password(password))
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


async def ensure_default_user(db: AsyncSession) -> None:
    """Seed the placeholder single user (id=1) used by the MVP until JWT auth
    is wired. Interactions/library routes hardcode user_id=1, so this row must
    exist or inserts fail on the foreign key. Idempotent."""
    existing = await db.get(User, 1)
    if existing is not None:
        return
    await db.execute(
        text(
            "INSERT INTO users (id, email, password_hash, preferences) "
            "VALUES (1, :email, :pw, '{}') ON CONFLICT (id) DO NOTHING"
        ),
        {"email": "placeholder@filmarr.local", "pw": "!placeholder-no-login"},
    )
    # Keep the serial sequence ahead of the manually-inserted id
    await db.execute(
        text(
            "SELECT setval(pg_get_serial_sequence('users','id'), "
            "GREATEST((SELECT max(id) FROM users), 1))"
        )
    )
    await db.commit()
