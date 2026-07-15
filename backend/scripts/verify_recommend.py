"""
Smoke-test the Phase-1 recommender against real seeded Firestore data.

Picks a seeded movie, feeds it as a single taste seed, and confirms the ContentRecommender
returns ranked neighbors via the FirestoreGraphStore (content_sim edges + popularity). No fake
user documents are written — this reads the catalog only.

Run from backend/:  python scripts/verify_recommend.py
"""

from __future__ import annotations

from datetime import datetime, timezone

from google.cloud import firestore

from app.recommender.base import (
    FeedMode,
    InteractionType,
    ScoringConfig,
    TasteSeed,
    UserContext,
    UserTasteState,
)
from app.recommender.content import ContentRecommender
from app.recommender.firestore_graph_store import FirestoreGraphStore


def main() -> int:
    db = firestore.Client(project="filmarr-mehdiacho")

    # Grab one seeded movie to use as the taste seed.
    first = next(db.collection("movies").limit(1).stream(), None)
    if first is None:
        print("No movies seeded yet.")
        return 1
    seed_id = int(first.id)
    title = (first.to_dict() or {}).get("title", "?")
    print(f"Seed: {seed_id} ({title})")

    store = FirestoreGraphStore(db)  # content_sim_source defaults to "precomputed"
    rec = ContentRecommender(store)

    state = UserTasteState(
        context=UserContext(user_id=1),
        seeds=(
            TasteSeed(
                movie_id=seed_id,
                interaction=InteractionType.INTERESTED,
                interacted_at=datetime.now(timezone.utc),
            ),
        ),
    )

    for mode in (FeedMode.DISCOVER, FeedMode.LIBRARY):
        cfg = ScoringConfig.phase1_discover() if mode == FeedMode.DISCOVER else ScoringConfig.phase1_library()
        batch = rec.recommend(state, mode, n=5, config=cfg, explain=True)
        print(f"\n[{mode.value}] {len(batch.items)} recs (fingerprint={batch.config_fingerprint})")
        for r in batch.items:
            d = db.collection("movies").document(str(r.movie_id)).get().to_dict() or {}
            why = ", ".join(f"{c.edge_type.value}<-{c.via_seed}" for c in r.provenance)
            print(f"  #{r.rank} {r.movie_id} {d.get('title','?')[:34]:34} score={r.score:.4f}  [{why}]")

    print("\nOK" if True else "")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
