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

app = FastAPI(title="filmarr API", version="0.1.0")

import os

_origins = [
    "http://localhost:5173",
    "http://localhost:4173",
]
# Allow the production frontend — set FRONTEND_URL env var on Render
if _frontend := os.getenv("FRONTEND_URL"):
    _origins.append(_frontend)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(movies_router)
app.include_router(library_router)
app.include_router(recommend_router)
app.include_router(letterboxd_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
