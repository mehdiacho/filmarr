"""
Cloud Run entrypoint. Exposes GET /recommend behind Firebase auth.

Flow: verify token -> assemble UserTasteState from Firestore -> pick a ScoringConfig by
mode -> score with the ContentRecommender -> serialize. The recommender and GraphStore are
constructed once at startup; nothing per-request is heavy except the Firestore reads.
"""

from __future__ import annotations

import os
from functools import lru_cache

from fastapi import Depends, FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from google.cloud import firestore

from .auth import get_uid
from .recommender.base import FeedMode, ScoringConfig
from .recommender.content import ContentRecommender
from .recommender.firestore_graph_store import FirestoreGraphStore
from .services.state import assemble_state

app = FastAPI(title="Recommender", version="0.1.0")

# Open CORS for now so the eventual web client works; tighten to known origins before
# any real launch.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST"],
    allow_headers=["Authorization", "Content-Type"],
)


@lru_cache(maxsize=1)
def _db() -> firestore.Client:
    return firestore.Client()


@lru_cache(maxsize=1)
def _recommender() -> ContentRecommender:
    return ContentRecommender(FirestoreGraphStore(_db()))


_CONFIGS = {
    FeedMode.DISCOVER: ScoringConfig.phase1_discover(),
    FeedMode.LIBRARY: ScoringConfig.phase1_library(),
}


@app.get("/healthz")
def healthz() -> dict:
    return {"ok": True}


@app.get("/recommend")
def recommend(
    uid: str = Depends(get_uid),
    mode: FeedMode = Query(default=FeedMode.DISCOVER),
    n: int = Query(default=20, ge=1, le=100),
    explain: bool = Query(default=False),
) -> dict:
    state = assemble_state(_db(), uid)
    batch = _recommender().recommend(state, mode, n, _CONFIGS[mode], explain=explain)
    return {
        "mode": batch.mode.value,
        "generated_at": batch.generated_at.isoformat(),
        "config_fingerprint": batch.config_fingerprint,
        "items": [
            {
                "movie_id": r.movie_id,
                "score": round(r.score, 6),
                "rank": r.rank,
                "why": [
                    {"via": c.via_seed, "edge": c.edge_type.value, "weight": round(c.contribution, 6)}
                    for c in r.provenance
                ],
            }
            for r in batch.items
        ],
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
