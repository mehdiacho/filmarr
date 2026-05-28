from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.movies.schemas import MovieOut
from app.recommender.content_based import ContentBasedRecommender

router = APIRouter(prefix="/recommend", tags=["recommend"])


@router.get("/discovery", response_model=list[MovieOut])
async def discovery(limit: int = 20, db: AsyncSession = Depends(get_db)):
    rec = ContentBasedRecommender(db)
    movies = await rec.recommend(user_id=1, limit=limit)
    return movies


@router.get("/build", response_model=list[MovieOut])
async def build(limit: int = 20, db: AsyncSession = Depends(get_db)):
    rec = ContentBasedRecommender(db)
    movies = await rec.library_build_suggestions(user_id=1, limit=limit)
    return movies
