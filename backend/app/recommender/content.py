"""
Phase-1 content recommender (GRAPH_RECOMMENDER.md §6, §13).

Implements the `Recommender` protocol using only whatever edge types appear in
`config.alpha`. On day 1 that's CONTENT_SIM (served live via find_nearest); as later
phases add edge types to the graph and to `config.alpha`, the SAME code blends them in
with no changes here. Recency decay and age-transition injection are wired but no-op
until their config knobs are turned on — so this one file carries Phases 1 through 3.
"""

from __future__ import annotations

import math
from collections import defaultdict
from datetime import datetime, timezone

from .base import (
    EdgeContribution,
    EdgeType,
    FeedMode,
    GraphStore,
    InteractionType,
    MovieId,
    Recommendation,
    RecommendationBatch,
    Recommender,
    ScoringConfig,
    UserTasteState,
)

# Base seed strength by interaction when no explicit rating is present.
_INTERACTION_BASE = {
    InteractionType.RATED: 0.9,
    InteractionType.HEARTED: 0.8,
    InteractionType.INTERESTED: 0.7,
    InteractionType.WATCHING: 0.6,
    InteractionType.WATCHED: 0.5,
    InteractionType.PLAN_TO_WATCH: 0.4,
    InteractionType.ON_THE_FENCE: 0.15,
    InteractionType.NOT_WATCHED: 0.0,
}
_HEART_DRIP = 3  # max hearted items re-injected into a discover batch (§7)


class ContentRecommender(Recommender):
    def __init__(self, graph: GraphStore) -> None:
        self._graph = graph

    def recommend(
        self,
        state: UserTasteState,
        mode: FeedMode,
        n: int,
        config: ScoringConfig,
        *,
        explain: bool = False,
    ) -> RecommendationBatch:
        now = datetime.now(timezone.utc)
        edge_types = [t for t, a in config.alpha.items() if a != 0.0] or [EdgeType.CONTENT_SIM]

        # 1. Seeds with recency-decayed strength (§5.1). Decay is off when lambda == 0.
        seed_weight: dict[MovieId, float] = {}
        for s in state.seeds:
            base = (s.rating / 5.0) if s.rating is not None else _INTERACTION_BASE.get(s.interaction, 0.0)
            if base <= 0.0:
                continue
            if config.recency_lambda > 0.0:
                days = max(0.0, (now - s.interacted_at).total_seconds() / 86400.0)
                base *= math.exp(-config.recency_lambda * days)
            seed_weight[s.movie_id] = max(seed_weight.get(s.movie_id, 0.0), base)

        if not seed_weight:
            return RecommendationBatch(mode=mode, items=(), generated_at=now)

        # 2. Propagate over weighted edges (§6.2).
        neigh = self._graph.neighbors(
            list(seed_weight.keys()),
            edge_types,
            min_weight=config.min_edge_weight,
            limit_per_source=200,
        )

        scores: dict[MovieId, float] = defaultdict(float)
        prov: dict[MovieId, list[EdgeContribution]] = defaultdict(list)
        for src, edges in neigh.items():
            sw = seed_weight[src]
            for dst, etype, weight in edges:
                alpha = config.alpha.get(etype, 0.0)
                if alpha == 0.0:
                    continue
                contribution = alpha * weight * sw
                if etype == EdgeType.CO_RATING and weight < 0:  # repulsion (§6.4)
                    contribution *= config.negative_edge_penalty
                scores[dst] += contribution
                if explain:
                    prov[dst].append(EdgeContribution(via_seed=src, edge_type=etype, contribution=contribution))

        # 3. Library feed: recognition, not preference — blend popularity, keep seen items.
        if mode == FeedMode.LIBRARY and scores:
            pop = self._graph.popularity_scores(list(scores.keys()))
            for mid in list(scores.keys()):
                scores[mid] = 0.3 * scores[mid] + 0.7 * pop.get(mid, 0.0)

        # 4. Filter. Discover hides already-seen; library deliberately does not (§7).
        if mode == FeedMode.DISCOVER:
            for mid in state.already_seen:
                scores.pop(mid, None)
        for mid in seed_weight:
            scores.pop(mid, None)

        # 5. Discover: drip-feed a few hearted-but-unseen items back in (§7).
        if mode == FeedMode.DISCOVER and state.hearted:
            for mid in list(state.hearted)[:_HEART_DRIP]:
                if mid not in state.already_seen:
                    scores[mid] = max(scores.get(mid, 0.0), 0.5)
                    if explain:
                        prov[mid].append(EdgeContribution(via_seed=mid, edge_type=EdgeType.CONTENT_SIM, contribution=0.5))

        if not scores:
            return RecommendationBatch(mode=mode, items=(), generated_at=now)

        # 6. Rank, apply source-diversity (§6.6), truncate.
        ranked = sorted(scores.items(), key=lambda kv: kv[1], reverse=True)
        ranked = self._diversify(ranked, prov, config.diversity, n)

        items = tuple(
            Recommendation(
                movie_id=mid,
                score=score,
                rank=i,
                provenance=tuple(sorted(prov[mid], key=lambda c: c.contribution, reverse=True)[:3]) if explain else (),
            )
            for i, (mid, score) in enumerate(ranked[:n])
        )
        return RecommendationBatch(
            mode=mode,
            items=items,
            generated_at=now,
            config_fingerprint=self._fingerprint(config),
        )

    # -- helpers ------------------------------------------------------------

    @staticmethod
    def _diversify(
        ranked: list[tuple[MovieId, float]],
        prov: dict[MovieId, list[EdgeContribution]],
        diversity: float,
        n: int,
    ) -> list[tuple[MovieId, float]]:
        """Stop a single seed from dominating the batch. `diversity` in [0,1] sets how
        many picks may trace to the same dominant seed before it's throttled. At 0 this
        is a no-op (pure score order)."""
        if diversity <= 0.0 or not prov:
            return ranked
        cap = max(1, int(math.ceil((1.0 - diversity) * n)) or 1)
        used: dict[MovieId, int] = defaultdict(int)
        picked, deferred = [], []
        for mid, score in ranked:
            contribs = prov.get(mid)
            dominant = max(contribs, key=lambda c: c.contribution).via_seed if contribs else None
            if dominant is not None and used[dominant] >= cap:
                deferred.append((mid, score))
                continue
            picked.append((mid, score))
            if dominant is not None:
                used[dominant] += 1
        return picked + deferred

    @staticmethod
    def _fingerprint(config: ScoringConfig) -> str:
        parts = [f"{t.value}:{a}" for t, a in sorted(config.alpha.items(), key=lambda kv: kv[0].value)]
        return f"lam={config.recency_lambda};div={config.diversity};" + ",".join(parts)
