import re
from uuid import uuid4

import httpx
from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.config import settings
from app.db.session import get_db
from app.movies.schemas import MovieOut
from app.profile import schemas, service

router = APIRouter(prefix="/profile", tags=["profile"])

_AVATAR_TYPES = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}
_AVATAR_MAX_BYTES = 5_000_000

_USERNAME_RE = re.compile(r"^[a-z0-9_]{3,30}$")
_RESERVED = {"me", "admin", "auth", "profile", "settings", "api", "filmarr", "u"}


async def _build_public(db: AsyncSession, user: User) -> schemas.PublicProfile:
    favorites = await service.resolve_favorites(db, user.favorite_film_ids or [])
    stats = await service.compute_stats(db, user.id)
    return schemas.PublicProfile(
        id=user.id,
        username=user.username,
        display_name=user.display_name,
        bio=user.bio,
        avatar_url=user.avatar_url,
        joined=user.created_at,
        favorites=[MovieOut.model_validate(m) for m in favorites],
        stats=stats,
    )


@router.get("/me", response_model=schemas.MyProfile)
async def my_profile(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    base = await _build_public(db, current_user)
    return schemas.MyProfile(
        **base.model_dump(),
        email=current_user.email,
        date_of_birth=current_user.date_of_birth,
        preferences=current_user.preferences or {},
    )


@router.patch("/me", response_model=schemas.MyProfile)
async def update_profile(
    body: schemas.ProfileUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if body.username is not None:
        uname = body.username.strip().lower()
        if not _USERNAME_RE.match(uname):
            raise HTTPException(
                status_code=400,
                detail=(
                    "Username must be 3–30 chars: lowercase letters, "
                    "numbers, underscores."
                ),
            )
        if uname in _RESERVED:
            raise HTTPException(status_code=400, detail="That username is reserved.")
        existing = await db.execute(
            select(User).where(User.username == uname, User.id != current_user.id)
        )
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=400, detail="Username already taken.")
        current_user.username = uname

    if body.display_name is not None:
        current_user.display_name = body.display_name.strip() or None
    if body.bio is not None:
        current_user.bio = body.bio.strip() or None
    if body.date_of_birth is not None:
        current_user.date_of_birth = body.date_of_birth
    if body.favorite_film_ids is not None:
        current_user.favorite_film_ids = body.favorite_film_ids

    await db.commit()
    await db.refresh(current_user)

    base = await _build_public(db, current_user)
    return schemas.MyProfile(
        **base.model_dump(),
        email=current_user.email,
        date_of_birth=current_user.date_of_birth,
        preferences=current_user.preferences or {},
    )


@router.post("/me/avatar", response_model=schemas.MyProfile)
async def upload_avatar(
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not settings.supabase_service_key:
        raise HTTPException(
            status_code=503, detail="Avatar uploads not configured yet."
        )

    ext = _AVATAR_TYPES.get(file.content_type or "")
    if not ext:
        raise HTTPException(status_code=400, detail="Use a JPEG, PNG, or WebP image.")
    content = await file.read()
    if len(content) > _AVATAR_MAX_BYTES:
        raise HTTPException(status_code=400, detail="Image must be under 5 MB.")

    path = f"{current_user.id}/{uuid4().hex}.{ext}"
    upload_url = f"{settings.supabase_url}/storage/v1/object/avatars/{path}"
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            upload_url,
            content=content,
            headers={
                "Authorization": f"Bearer {settings.supabase_service_key}",
                "Content-Type": file.content_type or "application/octet-stream",
                "x-upsert": "true",
            },
            timeout=30,
        )
    if resp.status_code >= 300:
        raise HTTPException(status_code=502, detail="Avatar upload failed.")

    current_user.avatar_url = (
        f"{settings.supabase_url}/storage/v1/object/public/avatars/{path}"
    )
    await db.commit()
    await db.refresh(current_user)

    base = await _build_public(db, current_user)
    return schemas.MyProfile(
        **base.model_dump(),
        email=current_user.email,
        date_of_birth=current_user.date_of_birth,
        preferences=current_user.preferences or {},
    )


@router.get("/u/{username}", response_model=schemas.PublicProfile)
async def public_profile(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(User).where(User.username == username.lower())
    )
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="Profile not found")
    return await _build_public(db, user)
