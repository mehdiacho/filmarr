# AGENTS.md

> Project bootstrap doc for a movie/TV tracking app with a Tinder-style swipe UI, graph-based recommender, and Letterboxd interop. Web first, Android later.

---

## 1. Original prompt (verbatim, do not paraphrase)

> you know how tinder works right, swipe left swipe right.
> i've previously built an ai powere app that you could tell what you've watched and it would log it and you could export to letterbox. i built it cause letterbox ui was a bit inconvenient. it was fun but i think i want to build something a bit more serios and a bit more usefull. it would still have the capabilities to export to letterbox, i'd like to take it a step further and automate that export so i acts more like a sync, this would be authorized and happen in the backend if possible so it's seamless. now, the 2 main things i want to implement are
>
> 1. A graph based recommender system. you know how i'm ussing GNNs for my thesis proposa right, i was thinking where else could processing data in grapghs be useful, and i though, the relationship between things that are liked vs disliked by an individual is often complex. i think a graph based structure would be that learns continuosly could do it. it would essentialy map the different movies and have their functional connectivities be weighted in order to determine what someone might like based on what they've claimed to like and when. i think the when is a crucial part as well, ie i liked x movie when i was 16, i'm 24 now and i wouldn't particularly like it, but i do like y now, the graph might be able to tell that i'd also like z based on that i liked x when i was younger, or that i liked x specifically in the year 2017, i'd hope for it to build those type of relations between movies and weigh all kind of factors. that is a very holistic view that will require us to design and train a model, for now we can go with something more simple and commonplave as we work towards that.
> 2. The second is the means of logging movies and shows, i mentioned that swipe left/right feature of tinder and a bunch of other apps. i'm thinking just like that, the ui will have a discovery page that let's the user select their interests and a few movies they've watched and would reccomend stuff to them, here they can swipe right for stuff they find interesting and left for stuff they don't, we coul'd have an additional gesture, a swipe down for stuff they're on the fence about. now in addition to discovery, we want another page that will uses the same gesture system, except it's purpose is to show the user movies and shows the system suspect they might have watched in an attempt to build up they're library. swipe right if watched left if not, up if watching, down if planning to watch. additionally a user can double tap a card to like such that even if they've not watched something, by double tapping before swiping left the system knows they're interested. these can be added to a lst of some sort visible to the system only for now, and when in the discovery page those double hearted stuff that were swiped left can appear, drip fed into the reccomendations list. another feature on this page would be a way to properly log their letterbox like opinion. by pressing and holding downon the card, it should enlarge a bit to fil l the screen, this will bring up a 5 star rating system at the base of the card as well as a comment box where the user can write a review. when this is done they can swipe right to log it as watched alongside the rating and review. they should still have the other swipe gestures, however if they swipe anywhere but right, the review and rating are discarded. the first fes times the user is using this system there should be warnings that pop up to inform them of the discard if they swipe anywhere but right. there should also be an undo button somewhere on the screen to revert up to the last 10 swipe actions including retrieving any review and rating data that may have been discarded. once the focus card has been swiped away the following card should not be in focus mode and should just be in normal ui mode, there should be a toggle in a 33 dot menu or somewhere on the screen to switch into focus mode, this is different from press and hold on the card in that if manually toggled all subsequent cards will be in focus mode as well. and for the sake of simplicity, the same way press and hold toggles a 1 time focus mode, when in focus mode it should also oggle out of it
>
> so that's the idea. for now i plan to make a web app but eventually i want to make an android app.

---

## 2. Product summary

A film/TV tracking app where the **primary input modality is swiping**, not searching. Two distinct swipe surfaces:

- **Discovery feed** — recommends new content the user is likely to enjoy.
- **Library-building feed** — surfaces content the user has *probably already seen*, to retroactively populate their library with low friction.

Backed by a **graph-based recommender** (eventually a learned GNN; simpler models first). Letterboxd is treated as an external destination — first via CSV export, eventually via authorized sync if/when API access is granted.

---

## 3. Gesture & interaction spec

Two feeds share the same gesture grammar, but the semantics differ:

| Gesture | Discovery feed | Library-building feed |
|---|---|---|
| Swipe right | Interested | Watched |
| Swipe left | Not interested | Not watched |
| Swipe up | (reserved) | Currently watching |
| Swipe down | On the fence | Plan to watch |
| Double tap | Heart (interested, independent of swipe direction) | Heart (interested, independent of swipe direction) |
| Press & hold | Enter focus mode (one-shot, this card only) | Enter focus mode (one-shot, this card only) |

### Focus mode rules

1. **One-shot focus** (press & hold on card): enlarges card, reveals 5-star rating + review textarea. Only swipe-right logs the rating/review; any other swipe **discards** them.
2. **Persistent focus** (toggle from kebab/3-dot menu): every subsequent card opens in focus mode.
3. **Press & hold acts as a toggle in both directions.** From normal → enters one-shot focus. From focus (whether one-shot or persistent) → exits focus.
4. **First-N-uses warning:** if user swipes anywhere but right while focus mode has unsaved rating/review, show a confirmation/warning toast. Stop showing it after some threshold (e.g. 3 dismissals, persisted in user prefs).

### Double-tap behavior

- A "hearted" card that is then swiped left (or anywhere other than right) is added to a **hidden interest list**.
- Hearted-but-not-watched items get **drip-fed back into the discovery feed** later. Not all at once.

### Undo

- **Last 10 swipe actions** are reversible.
- Undo must restore any **rating + review data** that was discarded by a non-right swipe in focus mode.
- Undo stack is in-memory per session (don't over-engineer; persistence across sessions can come later).
- UI: always-visible undo button or floating action button on both feeds.

---

## 4. Tech stack

**Frontend**
- Vite + React + TypeScript (strict)
- Tailwind CSS + shadcn/ui
- Zustand for state (single store per surface)
- `@use-gesture/react` + `framer-motion` for swipe + focus animations
- PWA-ready from the start

**Backend**
- **FastAPI (Python 3.12+)** — single-stack. ML-native, fewer moving parts, type hints throughout.
- `uv` for env/deps management.
- `ruff` + `mypy --strict`.

**Database**
- **Postgres + pgvector** (single instance).
- Graph stored as adjacency tables: `edges(src_movie_id, dst_movie_id, weight, edge_type, updated_at)` with indexes on `(src_movie_id, edge_type)`.
- pgvector for synopsis/metadata embeddings on the `movies` table, used by the content-based recommender.
- **Do not add Neo4j.** All recommender pipelines (content-based, PCC, GNN) want an edge list as input, not a graph DB to query at inference time. Revisit only if (a) an interactive multi-hop traversal UI gets built, or (b) Neo4j GDS becomes worth more than implementing PageRank/community detection in NetworkX. Neither applies to MVP and probably never will.

**External data**
- TMDB API for movie/TV metadata, posters, IDs, genres, credits.
- Letterboxd uses TMDB IDs internally — this is intentional and important for eventual sync mapping.

**Auth**
- Keep simple. JWT or session cookies. Email + password is fine for MVP. OAuth (Google) later.

---

## 5. Repo structure (proposed)

pnpm workspace for the frontend, sibling FastAPI app for the backend. Single repo.

```
/
├── apps/
│   ├── web/                  # Vite + React + TS frontend (pnpm)
│   └── api/                  # FastAPI backend (uv)
│       ├── app/
│       │   ├── main.py
│       │   ├── auth/
│       │   ├── movies/       # TMDB integration, caching
│       │   ├── library/      # user library, ratings, reviews
│       │   ├── recommender/  # swappable strategies behind a common interface
│       │   ├── letterboxd/   # adapter pattern: CSV impl now, API impl later
│       │   └── db/           # SQLAlchemy models, Alembic migrations
│       ├── tests/
│       └── pyproject.toml
├── AGENTS.md
├── README.md
└── pnpm-workspace.yaml
```

---

## 6. Phased build plan

### Phase 1 — MVP (the part Codex should focus on first)

Goal: a usable single-user app where I can swipe to log/review films, get vaguely-decent recommendations, and export to Letterboxd via CSV.

- [ ] Auth (email/password, single user is fine to start)
- [ ] TMDB integration with aggressive local caching of fetched metadata
- [ ] Discovery feed with swipe gestures (right/left/up/down + double-tap)
- [ ] Library-building feed with the alternate swipe semantics
- [ ] Focus mode: one-shot (press-hold) and persistent (kebab toggle), with the toggle-out-on-press-hold rule
- [ ] 5-star rating + review textarea in focus mode
- [ ] Discard-on-non-right-swipe + first-N-uses warning toast
- [ ] In-memory undo stack of last 10 actions, restoring rating/review data
- [ ] Hidden "hearted" list with drip-feed back into discovery
- [ ] Library page (just a list/grid of watched + ratings)
- [ ] **Content-based recommender** using TMDB metadata (genres, keywords, cast/crew similarity, maybe embed synopses). Hybridize with simple item-item collaborative once there's any signal.
- [ ] Letterboxd CSV export (their import format is well-documented)

### Phase 2

- [ ] Graph recommender v1: build the movie graph with edges weighted by **Pearson correlation** of user rating vectors (mirrors the construction approach from my thesis pipeline). Recommend via personalized PageRank or weighted neighbor aggregation.
- [ ] Apply for Letterboxd API access in parallel. If granted, replace CSV export with authorized sync.
- [ ] Multi-user, social features only if/when they're actually wanted.

### Phase 3

- [ ] Temporal-weighted GNN recommender. Edges parameterized by *when* the like happened (decay, era-clustering, etc.). This is where the actual research-flavored work lives.
- [ ] Android app (separate native client, shared backend API).

---

## 7. Open decisions to make at scaffold time

Codex should **ask before deciding** these, not silently pick:

1. **App name** — placeholder for now, real name TBD.
2. **Frontend package manager** — pnpm (matches my other projects) unless there's a reason not to.

Already locked: FastAPI backend, Postgres + pgvector, SQLAlchemy + Alembic, `uv` for Python env, no Neo4j.

---

## 8. Letterboxd integration — known constraints

- **Letterboxd's API is invite-only** and historically read-heavy. Do not assume write access.
- MVP path: **CSV export** in Letterboxd's documented import format.
- The sync layer must be an interface (`LetterboxdAdapter` or similar) with the CSV implementation as one concrete impl. When/if API access is granted, drop in an API-backed impl without touching the rest of the app.
- **Do not scrape Letterboxd with user credentials.** It violates ToS and will get accounts banned.

---

## 9. Recommender — implementation notes

- **Cold start matters.** First-time users will have ~0 interactions. The discovery feed must work *immediately* via content-based scoring against their interest selection during onboarding.
- **Two pipelines, not one:**
  - Discovery feed = "stuff you'd like" → personalization-weighted
  - Library-building feed = "stuff you've probably seen" → popularity-weighted, filtered by user's era/region/genre signals
- Same graph backbone can power both; just different scoring functions on top.
- Store every interaction with a timestamp from day one. Even before the temporal GNN is built, this data is the prerequisite for it.

---

## 10. Conventions

- TypeScript strict mode. No `any` unless commented why.
- Python: type hints throughout, `ruff` + `mypy`.
- One Zustand store per major surface (discovery, library, focus mode); don't put everything in one mega-store.
- Frontend: every gesture handler is unit-testable in isolation; logic separate from the motion component.
- Backend: each route has an OpenAPI-documentable handler; the recommender lives behind a clean `/recommend` interface that's easy to swap implementations behind.
- Commits: conventional commits.
- Don't add a dependency without checking with me first.

---

## 11. Out of scope (for now)

- Social features (following users, shared lists, etc.)
- Comments/community
- Push notifications
- Mobile app (planned, but not now)
- Anything involving scraping any third party

---

## 12. What I want from this Codex session

1. Scaffold the repo with Phase 1 structure.
2. Get the swipe UI to "feels right" quality before anything else — gestures are the product.
3. Wire up TMDB + Postgres + auth.
4. Implement a content-based recommender that's *good enough* to demo.
5. Get CSV export working end-to-end.
6. **Stop and check in** before starting Phase 2 work.

Ask clarifying questions early. Don't over-engineer. MVP first.
