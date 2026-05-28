"""Authentication dependency: resolve the current user from a Bearer JWT."""
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.models import User
from app.config import settings
from app.db.session import get_db

bearer_scheme = HTTPBearer(auto_error=True)

_credentials_exc = HTTPException(
    status_code=status.HTTP_401_UNAUTHORIZED,
    detail="Could not validate credentials",
    headers={"WWW-Authenticate": "Bearer"},
)


async def get_current_user(
    creds: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    try:
        payload = jwt.decode(
            creds.credentials, settings.secret_key, algorithms=["HS256"]
        )
        sub = payload.get("sub")
        if sub is None:
            raise _credentials_exc
        user_id = int(sub)
    except (JWTError, ValueError):
        raise _credentials_exc

    user = await db.get(User, user_id)
    if user is None:
        raise _credentials_exc
    return user
