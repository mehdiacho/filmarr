# GRAPH_RECOMMENDER.md

> Design doc for the graph-based recommender. This is the conceptual + implementation spec for how the recommendation engine is built up over time. Read alongside `CLAUDE.md`. **Current stack: Firebase (Auth + Firestore) + Google Cloud, with the recommender as a FastAPI service containerized on Cloud Run. Android-first (web + iOS later, shared backend).** The recommender's *graph/vector store* is a swappable layer (see §11, §16) that starts on Firestore vector search and graduates to Cloud SQL / Vertex AI as it grows — the scorer never sees the difference. This doc governs everything behind the `/recommend` service.

---

## 0. TL;DR for Claude Code

We are **not** building the full learned GNN first. We build a graph whose edges start as cheap heuristics and get progressively replaced by learned weights. The graph is an **edge list** served from a store that changes as it scales (Firestore → Cloud SQL/pgvector → Vertex AI), always behind the same interface. Recommendation = seed the graph with a user's taste, propagate over weighted edges, rank unseen nodes. The **temporal dimension** (when a user liked something, and how old they were) is a first-class part of the design, not an afterthought — it's the whole reason this is a graph and not a flat similarity lookup.

Build order: **content edges → co-rating (PCC) edges → temporal/age-conditioned edges → learned GNN embeddings.** Each stage ships independently and improves the same `/recommend` interface.

---

## 1. The core idea

Taste is **relational and non-stationary**. Two facts drive the whole design:

1. **Relational:** what you'll like is best predicted by the structure of what you (and people like you) already like — not by any single feature of a film. This is a graph.
2. **Non-stationary:** taste moves over time. "I liked *Kick-Ass* at 16, I'm 24 now, I wouldn't rate it the same — but I like *Sicario* now." A good model shouldn't just down-weight the old like; it should **use** it, because the *transition* from "liked X young" to "likes Y now" is itself a signal that predicts a third film Z.

So the graph has to encode not just *"A is similar to B"* but *"people who liked A **when young** tend to like B **later**."* That directional, time-conditioned edge is the thing generic recommenders can't represent, and it's the reason to do this.

---

## 2. Design principles

- **One graph backbone, multiple scoring functions.** The discovery feed and the library-building feed both read the same graph; they differ only in how nodes are scored (see §7).
- **Edges are swappable and additive.** Every edge has an `edge_type`. Adding a new edge type (or a learned one) never requires touching consumers — the scorer aggregates over whatever edges exist.
- **Cheap heuristics first, learned weights later.** The graph is useful on day one with zero learned parameters. The GNN, when it lands, replaces edge weights and adds node embeddings; it does not change the interface.
- **Every interaction is a timestamped, labeled event.** We log everything with a timestamp from day one (already in the schema). This is the training substrate for later phases even before we use it.
- **Store the edge list; don't *compute* the graph in Firestore.** All pipelines want an edge list as input (→ pandas/PyG). Firestore is the system of record for events and can hold precomputed edges/recs for serving, but graph *construction* (PCC correlations, age-transition mining) is a batch job run elsewhere (BigQuery / a Cloud Run job / Cloud SQL) that reads the event log and writes edges back. Firestore has no joins or matrix aggregations — don't fight it. No graph database (Neo4j etc.); see §16.

---

## 3. What the graph is

### 3.1 Nodes

Start **homogeneous** (movies/shows only), evolve to **heterogeneous**.

- **Phase 1–2 (now):** nodes are titles, keyed by TMDB ID.
- **Phase 3+ (GNN):** heterogeneous graph — add `person` (director/actor), `genre`, `keyword` node types. Heterogeneity is where a GNN earns its keep and where recommendations become *explainable* ("because you like Villeneuve + slow-burn thrillers").

Node features (used by content edges now, by the GNN later):
- Synopsis embedding (stored on the movie doc/row; served via Firestore `find_nearest` now, Cloud SQL pgvector / Vertex AI later — see §16). Keep dims ≤ 2048 for Firestore vector search (768/1024/1536 models are all fine).
- Genre multi-hot
- Release year / decade
- Popularity + vote average (TMDB)
- Runtime, language, country (weak signals, cheap to include)

### 3.2 Edge types

All edges live in one table (`edges`, see §11) discriminated by `edge_type`:

| `edge_type`        | Direction | Source of weight                                   | Available |
|--------------------|-----------|----------------------------------------------------|-----------|
| `content_sim`      | undirected| cosine of node feature/embedding vectors           | Phase 1   |
| `co_rating`        | undirected| Pearson corr. of user rating vectors (PCC)         | Phase 2   |
| `co_library`       | undirected| co-occurrence in user libraries (PMI-normalized)   | Phase 2   |
| `age_transition`   | **directed** | P(likes B now \| liked A at younger age), aggregated across users | Phase 3   |
| `era_cohort`       | undirected| co-liking concentrated in a calendar window        | Phase 3   |
| `learned`          | either    | GNN-produced edge scores / attention               | Phase 4   |

The scorer treats these as **weighted layers of the same graph**. A tunable per-type coefficient (`α_content`, `α_corating`, `α_age`, …) lets us blend them and, later, learn the blend.

---

## 4. Edge construction, phase by phase

### Phase 1 — `content_sim` (cold-start-friendly, no user data needed)

For every pair of titles within a candidate neighborhood (don't do all-pairs on 700k titles — restrict to same-genre / KNN-retrieved neighbors), compute cosine similarity over the node feature vector. Keep top-k per node (k ≈ 50–100). This alone makes discovery work on a brand-new account.

```
weight = cosine(feat[A], feat[B])   # feat = concat(embedding, genre_multihot, scaled_year, ...)
```

**Day-1 shortcut (no embeddings needed):** genre + keyword + cast/crew **Jaccard overlap** gives a serviceable `content_sim` with zero embedding infrastructure. Ship on that, add synopsis embeddings as the immediate upgrade — don't let embedding setup block the first release.

**Retrieval:** use Firestore's `find_nearest` (KNN vector search, server-side from Cloud Run) to get candidate neighbors, then refine and store top-k as `content_sim` edges. At small/early scale this exact-KNN is fine; swap to Vertex AI Vector Search (approximate, scalable) or Cloud SQL pgvector when the catalog grows (§16).

### Phase 2 — `co_rating` (PCC) and `co_library`

This is **the same construction as the functional-connectivity graph in the EEG thesis**: build a node×node matrix from correlations of per-node signal vectors. Here the "signal" per movie is its column in the user×movie rating matrix.

```
R = user × movie rating matrix (sparse, mean-centered per user)
For movie pair (A, B) with enough co-raters:
    weight = pearson(R[:,A], R[:,B])   # over users who rated both
    keep if |weight| >= threshold and n_coraters >= min_support
```

- Threshold + minimum co-rater support to kill noise (identical guardrails you'd use for spurious EEG connectivity).
- Negative correlations are **kept** (they're informative — "people who like A tend to dislike B") but stored with an `edge_type`/sign the scorer can treat as repulsion.
- `co_library`: PMI-normalized co-occurrence in libraries, independent of rating — catches "commonly seen together" even without ratings.

Recompute as a batch job (nightly at first). Incremental update later.

### Phase 3 — temporal edges (the heart — see §5)

### Phase 4 — `learned` (GNN — see §9)

---

## 5. The temporal model (the important part)

There are **two distinct notions of "when"**, and they do different jobs. Don't collapse them.

### 5.1 Recency decay — "what do you like *now*"

When scoring for a user at time `t_now`, weight each of their historical likes by a decay:

```
seed_weight(like) = rating_strength × exp(-λ · (t_now − t_like))
```

Recent taste dominates the direct-preference signal. This alone handles "I wouldn't rate *Kick-Ass* the same now" — its influence as a *direct* seed fades.

**But decayed likes are not discarded.** They stay available as entry points into age-transition edges (below). This duality is the key trick:

- **Recent likes:** high weight as direct preference seeds.
- **Old likes:** low weight as direct preference, **high value as predictors via `age_transition` edges.**

### 5.2 Age-conditioned transitions — "liked X young → likes Z now"

This is what makes the graph do something a similarity lookup can't.

Each interaction carries a timestamp; combined with the user's age bracket (see §11), we can stamp each like with the **user's age at the time**. Aggregating across all users, we mine directed edges of the form:

```
age_transition(A → B, from_bracket, to_bracket) =
    P(user likes B in `to_bracket` | user liked A in `from_bracket`)
```

Concretely: for users who liked A as teens, how disproportionately do they like B as adults (vs base rate)? Lift/PMI over age-stratified cohorts, with support thresholds. These are the **maturation pathways**. They let the system reason: *you liked X at 16 → users like you matured toward Z → recommend Z*, even if X and Z share no obvious content features.

### 5.3 Calendar-cohort effects — "liked X specifically in 2017"

Separately from user age, some co-liking is concentrated in a **calendar window** (a film that defined a cultural moment; things people binged in the same year). `era_cohort` edges capture co-liking whose strength is concentrated in a time window rather than spread evenly. Weaker signal than age-transition, but cheap and occasionally sharp.

### 5.4 How temporal edges enter scoring

During propagation (§6), a user's decayed old likes are allowed to fire **along `age_transition` edges** into the candidate set, weighted by how well the user's own age trajectory matches the edge's `from_bracket → to_bracket`. So the same old like that's been decayed-out as a direct preference re-enters, transformed, as a forward-looking predictor.

---

## 6. Inference / scoring (how a recommendation is produced)

Generic recipe, independent of which edge types exist:

1. **Seed.** Take the user's liked/highly-rated nodes. Assign each a seed weight = rating strength × recency decay (§5.1).
2. **Propagate.** Spread seed mass over weighted edges to unseen neighbors. Two interchangeable implementations:
   - **Weighted neighbor aggregation** (fast, good enough for MVP): `score(candidate) = Σ_over_seeds Σ_over_edge_types α_type · edge_weight(seed, candidate) · seed_weight`.
   - **Personalized PageRank** (better global structure, slightly heavier): random walk with restart from the seed set. Use `networkx` or a sparse power-iteration; cache per-user.
3. **Inject temporal transitions.** Add contributions from `age_transition` edges fired by decayed old likes (§5.4).
4. **Penalize repulsion.** Subtract mass along negative `co_rating` edges.
5. **Filter.** Remove already-seen / already-in-library titles (except the library-building feed, which wants the opposite — see §7).
6. **Rank + diversify.** Top-N by score, then apply a diversity pass (MMR or genre-capping) so the feed isn't ten near-duplicates.

Everything after step 2 is a linear combination the system can eventually **learn the coefficients for** (§9.4). Start with hand-set `α`s.

---

## 7. Serving both feeds from one backbone

Same graph, two scoring functions — this is why they were kept as separate pipelines in `CLAUDE.md`.

### Discovery feed — "stuff you'd like"
- Full personalized propagation (§6).
- Filters **out** already-seen titles.
- Blends in the **hidden "hearted" list** drip-feed: titles the user double-tapped then swiped left re-enter here, rate-limited (a few per session), scored normally but with a small boost.

### Library-building feed — "stuff you've probably already seen"
- **Popularity-weighted, not preference-weighted.** Scoring biases toward titles common enough that the user plausibly saw them, filtered by their era / region / genre signals from onboarding.
- Uses `co_library` edges heavily (things that co-occur in libraries like theirs), `content_sim` lightly, `co_rating` barely.
- Does **not** filter out by preference — the goal is coverage of likely-watched history, not delight.

Same `/recommend` interface, a `mode` parameter (`discover` | `library`) selects the scoring profile.

---

## 8. Continuous / online learning

The graph is a living object.

- **Every swipe is a labeled event** (watched / not / interested / on-the-fence / plan-to-watch / hearted / rating / review). Log all of it with timestamps (already in schema).
- **Cheap online updates:** edge weights that are pure aggregates (`co_rating`, `co_library`, `age_transition` counts) can be updated incrementally on each event — no full recompute.
- **Batch recompute:** correlations and the GNN retrain on a schedule (nightly → weekly as data grows). Keep the last good graph live while recomputing; swap atomically.
- **Feedback closes the loop:** a right-swipe in discovery confirms a prediction; a left-swipe is a negative. These become training labels for Phase 4.

---

## 9. The GNN endgame (Phase 4)

Only start this once there's real interaction volume. Until then it will overfit noise.

### 9.1 Representation
- **Heterogeneous graph** in PyTorch Geometric (`HeteroData`): node types `movie`, `person`, `genre`, `keyword`; edge types from §3.2 plus metadata edges (`acted_in`, `directed`, `has_genre`, `has_keyword`).
- Node features as in §3.1. Movie synopsis embeddings come from the movie store (Firestore field / Cloud SQL column, §16).

### 9.2 Objective
Recommendation as **link prediction** between a user node and movie nodes (implicit feedback). Two viable heads:
- **BPR-style ranking loss** over (user, liked, not-liked) triples — robust for implicit feedback.
- **Rating-regression head** where explicit 5-star ratings exist, multi-tasked with the ranking loss.

The GNN learns **movie (and user) embeddings**; recommendation becomes a dot-product/MLP score over the learned space. Learned edge attention can also be written back as `learned` edges to enrich the heuristic scorer even when the full model isn't in the serving path.

### 9.3 Temporal GNN
This is where the §5 temporal model becomes learned rather than hand-built:
- **Time-encoded edges:** attach `Δt` and `age_bracket` as edge features; let attention learn how much old interactions matter.
- **Or full TGN-style** (Temporal Graph Networks) with memory modules per node updated by each timestamped event — heavier, but the "right" version of the maturation-pathway idea. Treat as a research track, not a deadline.
- The `age_transition` edges from §5.2 are an excellent **sanity check / warm start** for whatever the GNN learns — if the model doesn't rediscover the strong hand-mined pathways, something's wrong.

### 9.4 Learning the blend
The `α_type` coefficients from §6 and the decay `λ` from §5.1 become **learnable parameters** trained against held-out swipe outcomes. This is the cleanest first "learned" win before the full GNN — a tiny model over the existing heuristic edge scores.

### 9.5 Serving
- Train offline on Vertex AI (or a GPU VM) in batch. Export learned embeddings + edge scores back into the serving store (Firestore precomputed edges / Cloud SQL).
- Serve recommendations from the **precomputed embeddings / edge table**, not by running the GNN per request. The DB stays the serving layer; the GNN is an offline weight-producer. (Same shape as the current heuristic pipeline — nothing downstream changes.)

---

## 10. Cold start

- **Brand-new user:** onboarding interest picks + a few claimed-watched titles seed a `content_sim`-only propagation. Works with zero collaborative data.
- **New title (no ratings yet):** reachable immediately via `content_sim` from its metadata/embedding; gains `co_rating`/`co_library` edges as interactions accrue.
- **Sparse user:** lean on content edges; blend in collaborative edges as their history grows. The `α` blend can itself be a function of how much history the user has.

---

## 11. Data model

Split by role: **Firestore = system of record + serving; graph store = swappable analytical layer (§16).**

### 11.1 Firestore (system of record, read by the app in realtime/offline)

Document collections (fields, not SQL columns):

- `users/{uid}` — profile, `current_age_bracket` (optional, may be `unknown`), onboarding `interest_tags`, prefs (incl. focus-mode warning dismissal count).
- `users/{uid}/library/{movieId}` — status (`watched` / `watching` / `plan` / `not_watched`), `rating` (nullable), `review` (nullable), `updated_at`.
- `users/{uid}/interactions/{autoId}` — the **swipe log**: `movie_id`, `event_type`, `rating?`, `ts`, `user_age_bracket_at_event` (derived at write time). Append-only; this is the training substrate. **Never overwrite** — new events, not mutations.
- `users/{uid}/hearted/{movieId}` — the hidden double-tap list, system-only (§7), drip-fed into discover.
- `movies/{tmdbId}` — cached TMDB metadata, feature vectors, `embedding` (vector field, ≤2048 dims, for `find_nearest`).
- `recs/{uid}/{mode}` — optional cache of the last served batch (cheap re-open, offline).

Notes: interactions are append-only so the temporal model has a clean history. Age bracket rides on each event at write time so it's correct even if the user's bracket changes later.

### 11.2 Graph / edge store (analytical, behind `GraphStore`)

Wherever this lives (Firestore precomputed collection early; Cloud SQL/BigQuery-built later), the logical shape is:

- `edges(src_id, dst_id, weight, edge_type, sign, n_support, from_bracket?, to_bracket?, updated_at)` — `from_bracket`/`to_bracket` populated only for `age_transition`. Indexed for fast top-k by `(src_id, edge_type)`.
- `graph_snapshots(id, built_at, is_live)` — batch recomputes build a new snapshot and flip `is_live` atomically, so serving never reads a half-built graph.

### 11.3 Age bracket capture (product behavior)

- **Age bracket, not DOB** — never exact date of birth. Coarse, privacy-light, sufficient for age-transition mining.
- **Onboarding:** offer it, clearly **optional**, with a one-line "why" ("helps us see how your taste has changed over time so we recommend better"). Never a gate.
- **Later prompt:** if skipped, a soft, dismissible re-ask at a natural moment once the payoff is legible. At most once or twice; respect a decline.
- **Graceful degradation:** `unknown` → age-transition edges don't fire for that user; everything else (recency decay, all other signals) still works. Additive, never load-bearing.
- **Soft explainer ("how this works"):** short, non-technical, shown once the user is *in* (not during onboarding). Sets expectations *and* retroactively justifies the age ask. The scorer's `EdgeContribution` provenance (see `base.py`) is what makes a light "because you liked X" hint possible if the design wants one.

---

## 12. Evaluation

Don't fly blind. Minimum viable metrics:

- **Offline:** temporal hold-out (train on interactions before `t`, predict after). Recall@K, NDCG@K, MAP. Temporal split, **not** random split — random leaks the future and flatters the model.
- **Online:** swipe-through rate on discovery (right-swipe / shown), library-hit rate on the library feed (right-swipe "yes I've seen this" / shown). These are the real objectives.
- **Ablations:** does adding `co_rating` beat `content_sim` alone? Does `age_transition` beat recency-decay alone? Each edge type should earn its place — same discipline as the EEGNet-vs-GCN benchmarking. If a layer doesn't move the metric, cut it.

---

## 13. Concrete build order for this session

1. **Formalize the `edges` table + scorer interface.** One `Recommender` protocol with `recommend(user, mode, n) -> ranked list`. Everything below plugs in behind it.
2. **Phase 1: `content_sim` edges** — Jaccard on genre/keyword/cast for the day-1 ship, then synopsis embeddings via Firestore `find_nearest`. Ship discovery + library feeds on this alone.
3. **Phase 2: `co_rating` (PCC) + `co_library`** batch job. Blend into scorer with hand-set `α`. A/B against Phase 1 on swipe-through.
4. **Phase 3: temporal.** Add recency decay to seeds; capture age bracket; mine `age_transition` + `era_cohort` edges; wire §5.4 injection.
5. **Phase 3.5: learn the blend** (§9.4) — tiny model over existing edge scores + `λ`. Cheapest learned win.
6. **Phase 4: GNN** in PyG, heterogeneous, link-prediction objective, trained offline on Vertex AI, embeddings served from the graph store. Temporal GNN as a research track after the static hetero-GNN works.

**Stop and check in after each phase.** Don't skip ahead to the GNN — it's worthless without the data the earlier phases generate.

---

## 14. Note on the thesis parallel

The Phase 2 construction — build a node×node graph from Pearson correlations of per-node signal vectors, threshold for support, feed a graph conv net — is **structurally identical to the EEG functional-connectivity → GCN pipeline** (PCC edges over channel signals, GCN over the resulting graph). Movies-as-channels, rating-columns-as-signals. The tooling transfers directly (PyG, the connectivity-matrix construction, the support/thresholding discipline, the ablation mindset). This isn't a coincidence to lean on rhetorically — it's a genuine reason the graph approach is tractable here, and the same guardrails against spurious edges apply.

---

## 15. Anti-goals (don't do these yet)

- No graph database (Neo4j etc.). Edge list in the store of the moment; see §16.
- **Don't compute the graph inside Firestore** — it has no joins/aggregations. Firestore stores events + serves precomputed edges; construction is a batch job elsewhere (§2, §16).
- No GNN before Phases 1–3 have generated interaction data.
- No running the GNN per-request. It's an offline weight-producer; the store serves.
- No exact DOB collection — age bracket only.
- No all-pairs similarity over the full catalog — KNN-restricted candidate neighborhoods only.
- No random train/test split in evaluation — temporal split only.

---

## 16. Stack, deployment & the swappable store

**Guiding principle:** use what ships fastest and what the tooling can drive autonomously *now*; keep every scaling decision behind an interface so the backend can be upgraded later **without users seeing a visible change** (they may feel better recommendations; they never see a migration). The `GraphStore` protocol and the `/recommend` service in `base.py` are that seam — nothing below leaks past them.

### 16.1 Layers

- **Client — Android now (Compose), web + iOS later.** Thin. Firebase Auth + Firestore SDK for state (realtime + offline). Calls Cloud Run for recommendations. No recommender logic on-device.
- **System of record — Firestore.** §11.1. Great fit for a swipe app: offline swipes queue and sync; the interaction log accrues as the training substrate.
- **Recommender service — FastAPI on Cloud Run.** Stateless container, implements `base.py`, assembles `UserTasteState` from Firestore per request, deployable autonomously via the Cloud Run tooling. Scales to zero when idle.
- **Graph / vector store — swappable, phased (§16.3).**
- **ML pipeline (Phase 4) — Vertex AI / GPU VM + GCS**, offline. Firebase is not involved; it just receives exported edges/embeddings for serving.

### 16.2 Why this stack, honestly

- **Wins:** Firebase Auth (Android-native, minutes to integrate); Firestore offline/realtime (ideal for swiping); Cloud Run (keeps the Python contract, cheap, autoscaling); Firestore vector search is **GA**, so Phase 1 needs no separate vector DB.
- **Constraint to respect:** Firestore is a document store — not relational, not a graph. Its vector search is *exact* KNN, **server-side only** (Python/Node/Go/Java, not the mobile SDKs — so vector queries run in Cloud Run, never on-device), dims ≤ 2048, ≤ 1000 results. Fine early; not the endgame at catalog scale.

### 16.3 Store evolution (invisible to the app)

| Stage | Vector retrieval | Graph edges / batch build | Trigger to advance |
|---|---|---|---|
| **Day 1 (Phase 1)** | Firestore `find_nearest`, or in-memory numpy cosine in the Cloud Run container | Precomputed `content_sim` (Jaccard, then embeddings) in a Firestore collection | ships now |
| **Phase 2–3** | Firestore `find_nearest` still fine | **Cloud SQL (Postgres + pgvector)** for edges + **BigQuery** for the PCC / age-transition aggregations over the interaction export; write edges back for serving | co_rating/temporal needs real joins & matrix ops |
| **Phase 4 + scale** | **Vertex AI Vector Search** (approximate, scalable) or Cloud SQL pgvector | GNN embeddings/edges from Vertex AI exported to the serving store | catalog/traffic outgrows exact KNN |

**If a materially better fit appears later:** the natural candidate is a Postgres-native platform (Cloud SQL, or Supabase — which is Postgres + pgvector + auth + realtime and restores the original adjacency-table design wholesale, and is autonomously tool-drivable). Migrating means reimplementing `GraphStore` against it and repointing the service — the Android app, the scorer, and the `/recommend` contract are untouched. That is the whole point of the seam.

### 16.4 Cross-platform

Because the client is thin and all logic sits in Cloud Run + Firestore, the eventual **web** app is Firebase JS SDK (same Auth, same Firestore data model, same offline/realtime) hitting the **same** `/recommend`. iOS likewise. 100% shared backend; only the UI layer differs per platform. Nothing in this doc is platform-specific.
