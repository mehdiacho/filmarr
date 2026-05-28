import asyncio
import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Register models with SQLAlchemy metadata (needed by Alembic)
import app.auth.models  # noqa: F401
import app.movies.models  # noqa: F401
import app.library.models  # noqa: F401

from app.auth.router import router as auth_router
from app.movies.router import router as movies_router
from app.library.router import router as library_router
from app.recommender.router import router as recommend_router
from app.letterboxd.router import router as letterboxd_router

logger = logging.getLogger(__name__)


async def _startup_ingest() -> None:
    """Populate movies table on first boot if it's empty."""
    from app.db.session import AsyncSessionLocal
    from app.movies.service import ingest_popular, movie_count
    async with AsyncSessionLocal() as db:
        try:
            count = await movie_count(db)
            if count == 0:
                logger.info("Movies table empty — starting TMDB ingestion...")
                n = await ingest_popular(db, pages=5)
                logger.info("Ingested %d movies from TMDB", n)
            else:
                logger.info("Movies table has %d rows — skipping ingestion", count)
        except Exception:
            logger.exception("Startup ingestion failed (non-fatal)")


@asynccontextmanager
async def lifespan(app: FastAPI):  # type: ignore[type-arg]
    asyncio.create_task(_startup_ingest())
    yield


app = FastAPI(title="filmarr API", version="0.1.0", lifespan=lifespan)

_origins = [
    "http://localhost:5173",
    "http://localhost:4173",
    "https://filmarr.vercel.app",
]
if extra := os.getenv("FRONTEND_URL"):
    _origins.append(extra)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router,      prefix="/api")
app.include_router(movies_router,    prefix="/api")
app.include_router(library_router,   prefix="/api")
app.include_router(recommend_router, prefix="/api")
app.include_router(letterboxd_router, prefix="/api")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
