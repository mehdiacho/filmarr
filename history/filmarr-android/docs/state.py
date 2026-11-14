"""
Assemble a `UserTasteState` from the Firestore user subtree (GRAPH_RECOMMENDER.md §11.1).

This is the only place that knows the data lives in Firestore. The scorer is pure with
respect to the object this produces — swap Firestore for anything else and only this file
changes.
"""

from __future__ import annotations

from datetime import datetime, timezone

from google.cloud import firestore

from ..recommender.base import (
    AgeBracket,
    InteractionType,
    TasteSeed,
    UserContext,
    UserTasteState,
)

# How many recent interactions to pull as seeds. Small: a user's taste is a handful of
# strong signals, not their entire history.
_MAX_SEEDS = 200
# Interaction types that count as positive taste signal for seeding.
_POSITIVE = {
    InteractionType.RATED,
    InteractionType.HEARTED,
    InteractionType.INTERESTED,
    InteractionType.WATCHING,
    InteractionType.WATCHED,
}


def _as_bracket(value) -> AgeBracket:
    try:
        return AgeBracket(value)
    except (ValueError, TypeError):
        return AgeBracket.UNKNOWN


def _as_dt(value) -> datetime:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    return datetime.now(timezone.utc)


def assemble_state(db: firestore.Client, uid: str) -> UserTasteState:
    user_doc = db.collection("users").document(uid).get()
    profile = user_doc.to_dict() or {}
    current_bracket = _as_bracket(profile.get("current_age_bracket"))
    interest_tags = tuple(profile.get("interest_tags", []) or ())

    user_ref = db.collection("users").document(uid)

    # Seeds: recent positive interactions.
    seeds: list[TasteSeed] = []
    seen_movie_ids: set[int] = set()
    q = (
        user_ref.collection("interactions")
        .order_by("ts", direction=firestore.Query.DESCENDING)
        .limit(_MAX_SEEDS)
    )
    for snap in q.stream():
        d = snap.to_dict() or {}
        try:
            itype = InteractionType(d.get("event_type"))
            mid = int(d["movie_id"])
        except (ValueError, KeyError, TypeError):
            continue
        seen_movie_ids.add(mid)
        if itype in _POSITIVE:
            seeds.append(
                TasteSeed(
                    movie_id=mid,
                    interaction=itype,
                    interacted_at=_as_dt(d.get("ts")),
                    rating=(float(d["rating"]) if d.get("rating") is not None else None),
                    age_bracket_at_event=_as_bracket(d.get("user_age_bracket_at_event")),
                )
            )

    # already_seen: everything in the library (any status) + anything swiped.
    library_ids: set[int] = set()
    for snap in user_ref.collection("library").stream():
        try:
            library_ids.add(int(snap.id))
        except ValueError:
            continue
    already_seen = frozenset(library_ids | seen_movie_ids)

    # hearted: the hidden double-tap list.
    hearted = frozenset(
        int(snap.id) for snap in user_ref.collection("hearted").stream() if snap.id.isdigit()
    )

    return UserTasteState(
        context=UserContext(
            user_id=hash(uid) & 0x7FFFFFFF,  # opaque int; uid stays the real key elsewhere
            current_age_bracket=current_bracket,
            interest_tags=interest_tags,
            history_size=len(seen_movie_ids),
        ),
        seeds=tuple(seeds),
        already_seen=already_seen,
        hearted=hearted,
    )
