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
from firebase_admin import auth as fb_auth
from google.cloud import firestore

from .auth import get_uid
from .recommender.base import FeedMode, ScoringConfig
from .recommender.content import ContentRecommender
from .recommender.firestore_graph_store import (
    CONTENT_SIM_PRECOMPUTED,
    FirestoreGraphStore,
)
from .services.state import assemble_state

app = FastAPI(title="Recommender", version="0.1.0")

# Open CORS for now so the eventual web client works; tighten to known origins before
# any real launch.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)


@lru_cache(maxsize=1)
def _db() -> firestore.Client:
    return firestore.Client()


@lru_cache(maxsize=1)
def _recommender() -> ContentRecommender:
    # Day 1: Jaccard content_sim edges from the `edges` collection ("precomputed").
    # Flip CONTENT_SIM_SOURCE=vector once movie embeddings + the KNN index exist —
    # a config change, not a code change (GRAPH_RECOMMENDER.md §16).
    source = os.environ.get("CONTENT_SIM_SOURCE", CONTENT_SIM_PRECOMPUTED)
    return ContentRecommender(FirestoreGraphStore(_db(), content_sim_source=source))


_CONFIGS = {
    FeedMode.DISCOVER: ScoringConfig.phase1_discover(),
    FeedMode.LIBRARY: ScoringConfig.phase1_library(),
}


# NOTE: Google Front End reserves the exact path `/healthz` on *.run.app and never forwards
# it to the container, so it's not externally reachable. `/health` is the usable liveness
# endpoint for uptime checks and the SETUP smoke test; `/healthz` stays for in-container probes.
@app.get("/healthz")
@app.get("/health")
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


# Subcollections written under users/{uid} (see the Android data layer: FirestoreLibraryStore,
# FirestoreInteractionLog, FirestoreUserProfile). Kept explicit so deletion is total and auditable
# rather than relying on recursive_delete's version-dependent availability.
_USER_SUBCOLLECTIONS = ("interactions", "hearted", "library")


def _delete_collection(col: firestore.CollectionReference, batch_size: int = 300) -> int:
    """Delete every document in a collection in batches. Returns the count removed."""
    db = _db()
    removed = 0
    while True:
        docs = list(col.limit(batch_size).stream())
        if not docs:
            return removed
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
        batch.commit()
        removed += len(docs)


@app.delete("/account")
def delete_account(uid: str = Depends(get_uid)) -> dict:
    """Permanently delete the caller's account: all Firestore data under users/{uid} and the
    Firebase Auth user. Runs with Admin credentials, so it bypasses the append-only client rules
    (interactions are create-only for clients) and needs no recent-login re-auth. Idempotent."""
    db = _db()
    user_ref = db.collection("users").document(uid)
    for name in _USER_SUBCOLLECTIONS:
        _delete_collection(user_ref.collection(name))
    user_ref.delete()
    # Delete the auth user last: if a Firestore batch above failed, the user still exists and can
    # retry. Tolerate an already-deleted user so retries succeed.
    try:
        fb_auth.delete_user(uid)
    except fb_auth.UserNotFoundError:
        pass
    return {"deleted": True}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
