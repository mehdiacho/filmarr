from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth import schemas, service
from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=schemas.TokenResponse,
    status_code=status.HTTP_201_CREATED,
)
async def register(body: schemas.RegisterRequest, db: AsyncSession = Depends(get_db)):
    if await service.get_user_by_email(db, body.email):
        raise HTTPException(status_code=400, detail="Email already registered")
    user = await service.create_user(db, body.email, body.password)
    token = service.create_access_token({"sub": str(user.id)})
    return schemas.TokenResponse(access_token=token)


@router.get("/me", response_model=schemas.UserOut)
async def me(current_user: User = Depends(get_current_user)) -> User:
    return current_user


@router.post("/onboarding", response_model=schemas.UserOut)
async def onboarding(
    body: schemas.OnboardingRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> User:
    prefs = dict(current_user.preferences or {})
    prefs["genres"] = body.genres
    prefs["onboarded"] = True
    current_user.preferences = prefs
    await db.commit()
    await db.refresh(current_user)
    return current_user


@router.post("/login", response_model=schemas.TokenResponse)
async def login(body: schemas.LoginRequest, db: AsyncSession = Depends(get_db)):
    user = await service.get_user_by_email(db, body.email)
    if not user or not service.verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    token = service.create_access_token({"sub": str(user.id)})
    return schemas.TokenResponse(access_token=token)
