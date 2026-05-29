import re
from uuid import uuid4

import httpx
from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile
from sqlalchemy import delete, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.config import settings
from app.db.session import get_db
from app.movies.schemas import MovieOut
from app.profile import schemas, service
from app.social.models import Follow

router = APIRouter(prefix="/profile", tags=["profile"])

_AVATAR_TYPES = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}
_AVATAR_MAX_BYTES = 5_000_000

_USERNAME_RE = re.compile(r"^[a-z0-9_]{3,30}$")
_RESERVED = {"me", "admin", "auth", "profile", "settings", "api", "filmarr", "u"}


async def _build_public(
    db: AsyncSession, user: User, viewer: User
) -> schemas.PublicProfile:
    favorites = await service.resolve_favorites(db, user.favorite_film_ids or [])
    stats = await service.compute_stats(db, user.id)
    is_self = user.id == viewer.id
    is_following = False
    if not is_self:
        is_following = user.id in await service.following_ids(db, viewer.id)
    return schemas.PublicProfile(
        id=user.id,
        username=user.username,
        display_name=user.display_name,
        bio=user.bio,
        avatar_url=user.avatar_url,
        joined=user.created_at,
        favorites=[MovieOut.model_validate(m) for m in favorites],
        favorite_genres=(user.preferences or {}).get("genres") or [],
        stats=stats,
        follower_count=await service.follower_count(db, user.id),
        following_count=await service.following_count(db, user.id),
        is_following=is_following,
        is_self=is_self,
    )


async def _build_mine(db: AsyncSession, user: User) -> schemas.MyProfile:
    base = await _build_public(db, user, user)
    return schemas.MyProfile(
        **base.model_dump(),
        email=user.email,
        date_of_birth=user.date_of_birth,
        phone=user.phone,
        preferences=user.preferences or {},
    )


@router.get("/me", response_model=schemas.MyProfile)
async def my_profile(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await _build_mine(db, current_user)


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
    if body.phone is not None:
        current_user.phone = body.phone.strip() or None
    if body.favorite_film_ids is not None:
        current_user.favorite_film_ids = body.favorite_film_ids

    await db.commit()
    await db.refresh(current_user)

    return await _build_mine(db, current_user)


@router.get("/username-available", response_model=schemas.UsernameAvailable)
async def username_available(
    u: str = Query(min_length=1),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    uname = u.strip().lower()
    if current_user.username and uname == current_user.username:
        return schemas.UsernameAvailable(available=True, reason="current")
    if not _USERNAME_RE.match(uname):
        return schemas.UsernameAvailable(available=False, reason="invalid")
    if uname in _RESERVED:
        return schemas.UsernameAvailable(available=False, reason="reserved")
    existing = await db.execute(
        select(User).where(User.username == uname, User.id != current_user.id)
    )
    if existing.scalar_one_or_none():
        return schemas.UsernameAvailable(available=False, reason="taken")
    return schemas.UsernameAvailable(available=True)


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

    return await _build_mine(db, current_user)


@router.delete("/me/avatar", response_model=schemas.MyProfile)
async def remove_avatar(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    current_user.avatar_url = None
    await db.commit()
    await db.refresh(current_user)
    return await _build_mine(db, current_user)


@router.get("/search", response_model=list[schemas.UserCard])
async def search_people(
    q: str = Query(min_length=2),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    term = f"%{q.lower()}%"
    result = await db.execute(
        select(User)
        .where(
            User.username.isnot(None),
            User.id != current_user.id,
            or_(
                func.lower(User.username).like(term),
                func.lower(User.display_name).like(term),
            ),
        )
        .limit(20)
    )
    users = list(result.scalars().all())
    return await service.build_user_cards(db, users, current_user.id)


async def _user_by_username(db: AsyncSession, username: str) -> User:
    result = await db.execute(select(User).where(User.username == username.lower()))
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=404, detail="Profile not found")
    return user


@router.post("/u/{username}/follow", status_code=204)
async def follow(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    target = await _user_by_username(db, username)
    if target.id == current_user.id:
        raise HTTPException(status_code=400, detail="You can't follow yourself.")
    exists = await db.execute(
        select(Follow).where(
            Follow.follower_id == current_user.id,
            Follow.followee_id == target.id,
        )
    )
    if exists.scalar_one_or_none() is None:
        db.add(Follow(follower_id=current_user.id, followee_id=target.id))
        await db.commit()


@router.delete("/u/{username}/follow", status_code=204)
async def unfollow(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    target = await _user_by_username(db, username)
    await db.execute(
        delete(Follow).where(
            Follow.follower_id == current_user.id,
            Follow.followee_id == target.id,
        )
    )
    await db.commit()


@router.get("/u/{username}/followers", response_model=list[schemas.UserCard])
async def followers(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    target = await _user_by_username(db, username)
    result = await db.execute(
        select(User)
        .join(Follow, Follow.follower_id == User.id)
        .where(Follow.followee_id == target.id)
        .order_by(Follow.created_at.desc())
    )
    users = list(result.scalars().all())
    return await service.build_user_cards(db, users, current_user.id)


@router.get("/u/{username}/following", response_model=list[schemas.UserCard])
async def following(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    target = await _user_by_username(db, username)
    result = await db.execute(
        select(User)
        .join(Follow, Follow.followee_id == User.id)
        .where(Follow.follower_id == target.id)
        .order_by(Follow.created_at.desc())
    )
    users = list(result.scalars().all())
    return await service.build_user_cards(db, users, current_user.id)


@router.get("/u/{username}", response_model=schemas.PublicProfile)
async def public_profile(
    username: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    user = await _user_by_username(db, username)
    return await _build_public(db, user, current_user)
