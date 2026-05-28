from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.movies.schemas import MovieOut
from app.recommender.content_based import ContentBasedRecommender

router = APIRouter(prefix="/recommend", tags=["recommend"])


@router.get("/discovery", response_model=list[MovieOut])
async def discovery(
    limit: int = 20,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    rec = ContentBasedRecommender(db)
    return await rec.recommend(user_id=current_user.id, limit=limit)


@router.get("/build", response_model=list[MovieOut])
async def build(
    limit: int = 20,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    rec = ContentBasedRecommender(db)
    return await rec.library_build_suggestions(user_id=current_user.id, limit=limit)
