from datetime import datetime, timedelta
from typing import Any

from jose import jwt
from passlib.context import CryptContext
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.models import User
from app.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


def create_access_token(data: dict[str, Any]) -> str:
    payload = data.copy()
    payload["exp"] = datetime.utcnow() + timedelta(minutes=settings.access_token_expire_minutes)
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
