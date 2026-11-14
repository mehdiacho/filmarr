"""
Recommender domain contracts.

This module defines the *interface* the recommendation engine is built against —
what goes in, what comes out, and the protocol every scoring strategy implements.
It is deliberately dependency-light (stdlib dataclasses + typing only) so the core
domain does not couple to FastAPI, pydantic, or the ORM. API-layer request/response
schemas and ORM models map to/from these types at the edges.

Design rationale lives in GRAPH_RECOMMENDER.md; §-references below point into it.

The single most important property of this interface: every scoring strategy —
content-only (Phase 1), + PCC/co-library (Phase 2), + temporal (Phase 3), + learned
GNN edges (Phase 4) — implements the SAME `Recommender` protocol and reads the SAME
graph via `GraphStore`. Phases add edge types and swap weights; they never change
this contract. The two feeds (discovery / library-building) are one backbone with
two scoring profiles, selected by `FeedMode`. See §2, §7, §13.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Mapping, Optional, Protocol, Sequence, runtime_checkable

MovieId = int  # TMDB id, catalog-wide primary key


# --------------------------------------------------------------------------- #
# Enums                                                                        #
# --------------------------------------------------------------------------- #

class FeedMode(str, Enum):
    """Which scoring profile to serve. Same graph, different objective (§7)."""

    DISCOVER = "discover"   # "stuff you'd like"        -> preference-weighted
    LIBRARY = "library"     # "stuff you've seen"       -> popularity-weighted


class EdgeType(str, Enum):
    """Layers of the graph. The scorer aggregates over whatever exists (§3.2)."""

    CONTENT_SIM = "content_sim"          # cosine of node features/embeddings (Phase 1)
    CO_RATING = "co_rating"              # Pearson corr of rating vectors, PCC (Phase 2)
    CO_LIBRARY = "co_library"            # PMI-normalized library co-occurrence (Phase 2)
    AGE_TRANSITION = "age_transition"    # directed: liked A young -> likes B later (Phase 3)
    ERA_COHORT = "era_cohort"            # co-liking concentrated in a calendar window (Phase 3)
    LEARNED = "learned"                  # GNN-produced edge scores / attention (Phase 4)


class InteractionType(str, Enum):
    """Swipe/gesture semantics, logged per event (§8). Feed-dependent meaning is
    resolved before this point: e.g. a right-swipe is INTERESTED in discover and
    WATCHED in library. HEARTED is orthogonal to swipe direction (double-tap)."""

    WATCHED = "watched"
    NOT_WATCHED = "not_watched"
    WATCHING = "watching"
    PLAN_TO_WATCH = "plan_to_watch"
    INTERESTED = "interested"
    ON_THE_FENCE = "on_the_fence"
    HEARTED = "hearted"                  # double-tap; drip-fed back into discover (§7)
    RATED = "rated"                      # explicit 5-star, focus mode (richest signal)


class AgeBracket(str, Enum):
    """Coarse life-stage bracket captured optionally at onboarding or later (§5.2, §11).
    Coarse, not DOB — privacy-light and sufficient for age-transition mining.
    UNKNOWN is a first-class value: if the user declines, age-transition edges simply
    do not fire for them and everything else still works."""

    TEEN = "13_17"
    YOUNG_ADULT = "18_24"
    ADULT = "25_34"
    MID = "35_44"
    MATURE = "45_54"
    SENIOR = "55_plus"
    UNKNOWN = "unknown"


# --------------------------------------------------------------------------- #
# Inputs: the user's taste state                                              #
# --------------------------------------------------------------------------- #

@dataclass(frozen=True, slots=True)
class TasteSeed:
    """One liked/rated title that seeds propagation. Carries the raw temporal facts
    the scorer needs to compute BOTH a decayed recency weight (§5.1) AND
    age-transition eligibility (§5.2) — the duality where an old like is decayed out
    as a direct preference but re-enters, transformed, as a forward-looking predictor."""

    movie_id: MovieId
    interaction: InteractionType
    interacted_at: datetime
    rating: Optional[float] = None            # 0..5 if explicitly rated, else None
    age_bracket_at_event: AgeBracket = AgeBracket.UNKNOWN


@dataclass(frozen=True, slots=True)
class UserContext:
    """Everything about the user that shapes scoring but isn't a single seed."""

    user_id: int
    current_age_bracket: AgeBracket = AgeBracket.UNKNOWN
    # Onboarding interest picks (genre/keyword ids) — the cold-start seed set (§10).
    interest_tags: Sequence[int] = field(default_factory=tuple)
    history_size: int = 0                     # drives content<->collaborative blend (§10)


@dataclass(frozen=True, slots=True)
class UserTasteState:
    """The full assembled input to a recommendation request. Built by the service
    layer from the ORM before the scorer is ever touched; the scorer is pure w.r.t.
    this object (easy to unit-test, easy to snapshot for eval)."""

    context: UserContext
    seeds: Sequence[TasteSeed]
    already_seen: frozenset[MovieId] = frozenset()   # filtered OUT of discover, NOT of library
    hearted: frozenset[MovieId] = frozenset()        # drip-fed into discover (§7)


# --------------------------------------------------------------------------- #
# Tuning: scoring config (hand-set now, learnable later — §9.4)               #
# --------------------------------------------------------------------------- #

@dataclass(frozen=True, slots=True)
class ScoringConfig:
    """All knobs live here. Phase 3.5 turns these into learned parameters trained
    against held-out swipe outcomes — the cheapest 'learned' win before the full GNN.
    Until then they are hand-set per FeedMode."""

    # Per-edge-type blend coefficients (§6 step 2). Missing type => 0.
    alpha: Mapping[EdgeType, float] = field(default_factory=dict)
    recency_lambda: float = 0.0               # decay rate on seed influence (§5.1); 0 = off
    negative_edge_penalty: float = 1.0        # weight on repulsion via negative co_rating (§6.4)
    age_transition_weight: float = 0.0        # injection strength for §5.4; 0 = off (Phase <3)
    diversity: float = 0.3                    # 0 = pure score, 1 = max spread (MMR/genre-cap, §6.6)
    min_edge_weight: float = 0.0              # prune weak edges at read time
    candidate_limit: int = 2000               # cap neighborhood expansion before ranking

    @staticmethod
    def phase1_discover() -> "ScoringConfig":
        """Content-only cold-start-friendly profile. Works on a brand-new account."""
        return ScoringConfig(alpha={EdgeType.CONTENT_SIM: 1.0}, diversity=0.3)

    @staticmethod
    def phase1_library() -> "ScoringConfig":
        """Popularity/recognition profile — leans on co_library once it exists,
        content lightly. Preference edges barely matter here (§7)."""
        return ScoringConfig(
            alpha={EdgeType.CO_LIBRARY: 1.0, EdgeType.CONTENT_SIM: 0.2},
            diversity=0.1,
        )


# --------------------------------------------------------------------------- #
# Outputs: recommendations + provenance                                       #
# --------------------------------------------------------------------------- #

@dataclass(frozen=True, slots=True)
class EdgeContribution:
    """Why this candidate scored: which seed, via which edge type, how much. Aggregated
    into a user-facing 'because you liked X' hint AND used for debugging/ablation.
    This is the same provenance data that powers the soft explainer screen (§ age UX)."""

    via_seed: MovieId
    edge_type: EdgeType
    contribution: float


@dataclass(frozen=True, slots=True)
class Recommendation:
    movie_id: MovieId
    score: float
    rank: int
    # Populated only when recommend(..., explain=True). Top few contributors, sorted.
    provenance: Sequence[EdgeContribution] = field(default_factory=tuple)


@dataclass(frozen=True, slots=True)
class RecommendationBatch:
    mode: FeedMode
    items: Sequence[Recommendation]
    generated_at: datetime
    graph_snapshot_id: Optional[int] = None   # which built graph produced this (§8, §11)
    config_fingerprint: Optional[str] = None  # hash of ScoringConfig, for eval/repro


# --------------------------------------------------------------------------- #
# Graph access boundary                                                        #
# --------------------------------------------------------------------------- #

@runtime_checkable
class GraphStore(Protocol):
    """The only surface through which a scorer reads the graph. Concrete backings evolve
    without the scorer knowing (see GRAPH_RECOMMENDER.md §16): Firestore `find_nearest` +
    a precomputed-edges collection early; Cloud SQL (Postgres + pgvector) + BigQuery-built
    edges at Phase 2–3; Vertex AI Vector Search at scale; an in-memory snapshot for a batch
    eval run. Keeps 'store the edge list, don't compute the graph in Firestore' honest (§2).
    `UserTasteState` is assembled by the service layer (from Firestore) before the scorer
    runs — that assembly is not this protocol's concern."""

    def neighbors(
        self,
        source_ids: Sequence[MovieId],
        edge_types: Sequence[EdgeType],
        *,
        min_weight: float = 0.0,
        limit_per_source: int = 100,
        from_bracket: Optional[AgeBracket] = None,   # only meaningful for AGE_TRANSITION
        to_bracket: Optional[AgeBracket] = None,
    ) -> Mapping[MovieId, Sequence[tuple[MovieId, EdgeType, float]]]:
        """For each source, its outgoing edges of the requested types.
        Returns {source_id: [(neighbor_id, edge_type, weight), ...]}."""
        ...

    def popularity_scores(
        self, movie_ids: Sequence[MovieId]
    ) -> Mapping[MovieId, float]:
        """Normalized popularity, used by the LIBRARY feed's recognition scoring (§7)."""
        ...


# --------------------------------------------------------------------------- #
# The recommender                                                              #
# --------------------------------------------------------------------------- #

@runtime_checkable
class Recommender(Protocol):
    """Every strategy implements this. The service layer picks a strategy + config by
    FeedMode and calls recommend(); nothing downstream changes as strategies evolve
    from heuristic to learned (§13.1)."""

    def recommend(
        self,
        state: UserTasteState,
        mode: FeedMode,
        n: int,
        config: ScoringConfig,
        *,
        explain: bool = False,
    ) -> RecommendationBatch:
        """Produce up to `n` ranked, deduplicated, filtered recommendations.

        Contract:
          - DISCOVER filters out `state.already_seen`; LIBRARY does not (§7).
          - DISCOVER may inject a rate-limited slice of `state.hearted` (§7).
          - Seeds are recency-decayed per config.recency_lambda (§5.1); decayed-out
            old likes remain eligible to fire AGE_TRANSITION edges (§5.4).
          - Output is diversified per config.diversity before truncation to `n` (§6.6).
          - When explain=True, each item carries its top EdgeContributions.
        Must be pure w.r.t. (state, config, and the GraphStore snapshot) — same inputs,
        same output — so eval runs are reproducible.
        """
        ...
