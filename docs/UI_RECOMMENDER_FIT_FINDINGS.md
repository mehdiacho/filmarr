# UI_RECOMMENDER_FIT — Findings

> My read of the **implemented Compose screens** (there are no separate Figma/PNG design files — the screens *are* the design) against the 8 questions in `UI_RECOMMENDER_FIT.md`. Nothing here is edited; these are recommendations for you to act on.

Screens reviewed: `OnboardingScreen`, `DiscoverScreen`, `BuildScreen`, `SwipeDeck`/`SwipeCardFace`, `TasteGraphScreen`, `FilmDetailScreen`, `TmdbRepository`, plus the data layer (`FilmRepository`, `Model`).

---

## The 3 highest-leverage changes

### 1. Onboarding seeds the graph with 5 genre chips and *nothing else* (Q-C) — biggest gap
`OnboardingScreen` collects **only genres** (`MockCatalog.onboardingGenres`, capped at 5) and offers a "Skip — surprise me". There are **no claimed-watched titles** and **no age-bracket ask**. §10 says cold-start needs interest picks **plus a few claimed-watched titles** — those titles are the actual `content_sim` seed nodes. With genres alone, the first discovery feed has almost nothing to propagate from. **Add a second onboarding step: "Pick a few films you've loved"** (a small searchable/poster-grid picker writing `interested`/`rated` seeds), and slot the **optional age bracket** here with a one-line "why" (never a gate — §11.3). This single change most determines whether the first feed feels alive.

### 2. The app doesn't consume `/recommend` yet — both decks pull raw TMDB (Q-A/B enabler)
`DiscoverScreen` → `TmdbRepository.discover()` → TMDB `trending`; `BuildScreen` → `popular`. The recommender is invisible to the user until the deck is fed by the backend. **Until this is wired, no card can show a "why", provenance is moot, and swipe data trains a graph nobody sees.** This is the integration that makes everything else real (a URL-parameterized client is drafted and ready to point at Cloud Run post-deploy).

### 3. Richest signal sits behind the highest friction, with no middle ground (Q-G)
Explicit 5-star + review (the Phase-2 `co_rating`/PCC fuel) is gated behind long-press → focus → rate → swipe-right. Meanwhile **discover left-swipe logs nothing** (no `not_interested` type), and the focus panel's review box is a **dead placeholder** (`Text("Add a note… (optional)")`, not an input). Add a **cheap middle signal** — a thumb up/down, or promote the existing double-tap heart as the light "yes I'd watch this" — so we capture graded interest without forcing the full ritual. Wire the note box to a real `TextField` or drop it.

---

## Question-by-question

### A. Do discovery vs library cards differ — should they?
**Finding:** One `SwipeCardFace` is reused for both. Build adds a `"Likely seen · {year}"` pill; Discover overlays the focus panel. Both are poster-forward with title + a genre/year `MetaRow`. Neither is synopsis-heavy.
**Verdict:** Minor tweak. They barely diverge, and the divergence is the wrong axis — Build is a *recognition* task and needs stronger memory cues.
**Suggestion:** On Build cards, surface **cast/director + franchise/collection markers** (recognition anchors), not just year. On Discover, a one-line synopsis snippet + a "why" tag. Same component, two content profiles keyed by feed.

### B. Any surface for *why* something was recommended?
**Finding:** Provenance exists on `TasteGraphScreen` ("Why we recommend what we do", `"92% match · via A24 core"`) and in `ResultRow` subtitles — but **not on the discovery swipe cards** at the moment of judgment.
**Verdict:** Minor tweak (blocked on #2). `base.py` already emits `EdgeContribution`; the endpoint returns a `why[]` array per item.
**Suggestion:** A subtle, optional "because you liked X" tag on the discover card (or long-press/info reveal). Cheap once the deck is backend-fed.

### C. Does onboarding cold-start the graph?
**Finding:** Genres only, no titles, no age. See highest-leverage #1.
**Verdict:** **Rethink.**
**Suggestion:** Add the claimed-watched-titles step + optional age bracket (with "why", skippable).

### D. Where does the soft "how this works" explainer live?
**Finding:** `TasteGraphScreen` ("Why we recommend what we do") is the nearest thing, but it's a **separate destination**, not a post-onboarding moment. No dismissible in-feed explainer.
**Verdict:** Minor tweak.
**Suggestion:** Show a **one-time dismissible in-feed card** on first discovery load (or after ~5 swipes), pointing to the taste graph and retroactively justifying the age ask. Reuse `TasteGraphScreen`'s copy — least-intrusive placement in the existing flow.

### E. Does the user feel swiping is *doing* something?
**Finding:** `TasteGraphScreen` (visual), Build's progress bar + "N logged", and Profile stats exist. But the **discovery feed itself shows no live "listening" signal** as you swipe.
**Verdict:** Minor tweak.
**Suggestion:** A lightweight in-feed pulse — a "taste taking shape" chip that updates every N swipes, or an occasional "added *Sicario* because you liked *X*" toast. Avoid gamification; one honest signal is enough.

### F. Is the hidden "hearted" list kept system-only?
**Finding:** No browsable "liked list" is surfaced anywhere — good. **But** the double-tap "heart" is stored as `LibraryEntry.favorite`, which is *also* the user-facing Favorite (a filled heart on cards and in `FilmDetailScreen`). So the **hidden interest signal and an intentional public "favorite" are the same bit.**
**Verdict:** Rethink the *semantics* (list stays hidden — that part's fine).
**Suggestion:** Separate the two concepts: keep `favorite` as the user's deliberate favorite, and treat the recommender's drip-fed "hearted" as its own hidden signal. (Backend already writes a distinct `hearted/{id}` subcollection — the app should stop conflating it with `favorite` so intentional favorites don't pollute the drip-feed and vice-versa.)

### G. Is rating discoverable/low-friction enough? Middle-ground signal?
**Finding:** Focus mode is reachable (long-press or kebab toggle) and the star row works, but the review note is a **non-functional placeholder**, and there's no signal lighter than full-stars yet richer than a bare swipe. Discover-left captures nothing.
**Verdict:** **Rethink.** See highest-leverage #3.
**Suggestion:** Add a thumb up/down (or elevate double-tap heart) as the graded middle signal; make the note box a real input or remove it; give discover-left *some* recorded outcome if you want negative signal.

### H. Does the swipe grammar map to distinct, unambiguous events?
**Finding:** Only **left/right** exist in `SwipeDeck` (`enum SwipeDir { LEFT, RIGHT }`). The spec's **up (watching) and down (plan-to-watch / on-the-fence)** are not implemented, so those `InteractionType`s can't be produced from the deck. The two implemented directions are well-labeled (Discover `INTERESTED`/`SKIP`; Build `SEEN`/`NOT SEEN` + rotated rail labels).
**Verdict:** Minor tweak for legibility; **rethink** for completeness.
**Suggestion:** Add up/down commit paths to `SwipeDeck` (the spec's 4-direction grammar) so `watching`/`plan_to_watch`/`on_the_fence` become capturable. Legibility of existing directions is fine; keep the feed-specific labels.

---

## What I already wired on the data side (so the review lands against a moving target)
- `users/{uid}/interactions/{autoId}` append-only log + `users/{uid}/hearted/{movieId}` now written on swipe/heart (`FirestoreInteractionLog`, `FilmRepository.logInteraction`, `toggleFavorite`). Event vocab matches `base.py`.
- Discover right → `interested` (or `rated` w/ stars); double-tap → `hearted`; Build right → `watched`, left → `not_watched`.
- **Not yet capturable** (pending deck up/down + onboarding titles): `watching`, `plan_to_watch`, `on_the_fence`, and any discover-left negative.
