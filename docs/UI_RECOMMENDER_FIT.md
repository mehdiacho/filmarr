# UI_RECOMMENDER_FIT.md

> **This is a task for you, Claude Code, not a spec.** The app already has designs. This doc asks whether those designs are well-matched to what the graph recommender (see `GRAPH_RECOMMENDER.md`) needs to *receive* from the user and *show* to the user. **Go read the actual designs**, answer each question below against them yourself, and then report back to me (Mehdi) with (a) your findings and (b) any concrete redesign suggestions. I'm not attached to the current design — it's a decent start but I don't love it, so push where you think it's warranted. Don't silently change designs; surface recommendations and let me decide.

---

## Framing: the recommender is only as good as its two data surfaces

The engine has exactly two contact points with the UI:

1. **What the user gives it** — every swipe, heart, rating, review, interest pick, and (optional) age bracket is a labeled, timestamped training event. Sparse or ambiguous input = weak graph.
2. **What it shows the user** — cards must carry exactly the information needed to make the *right kind* of judgment for that feed, and no more (more = decision fatigue = fewer, noisier swipes).

The whole point of the questions below is: does the current design maximize the quality of #1 and the appropriateness of #2? Read the designs with that lens.

---

## Questions to answer against the designs

### A. Do discovery cards and library-building cards differ — and should they?

The two feeds ask the user to perform **cognitively different tasks**:

- **Discovery** = *"does this appeal to me?"* → a **judgment of appeal**. Wants: poster, title, year, genre, maybe a synopsis snippet, maybe a "why recommended" hint.
- **Library-building** = *"have I seen this?"* → a **memory-recognition task**. Wants recognition cues: prominent poster, release era, recognizable cast/director, franchise markers. A synopsis is nearly useless here — you either recognize it or you don't.

**Question:** In the designs, are these two card types visually/informationally distinct, or is it one card reused? If it's one card, is it synopsis-heavy (good for discovery, wasteful for library) or recognition-heavy (vice versa)? My instinct is they should diverge. Tell me if you agree after seeing them.

### B. Is there any surface for *why* something was recommended?

The scorer can emit provenance (`EdgeContribution` in `base.py`) — "recommended mostly because you liked *X*." This does three jobs: builds trust, makes the soft explainer concrete, and gives me a debugging window.

**Question:** Do the designs have room for a subtle "because you liked X" affordance on discovery cards (a tag, a long-press detail, an info tap)? If there's zero space for it, is that a deliberate minimalist choice or an oversight? I lean toward wanting at least an *optional* reveal.

### C. Does onboarding collect enough to cold-start the graph?

Cold start (§10) needs interest picks **plus a few claimed-watched titles** to seed content-based propagation. Too thin an onboarding = an empty-feeling first feed.

**Question:** What exactly does the current onboarding collect? Enough seeds for a non-embarrassing first discovery feed? If it only collects genres and no titles (or vice versa), flag it. Also: where does the **optional age-bracket** ask fit, and does the layout support an "optional + one-line why" treatment without it feeling invasive or like a gate?

### D. Where does the soft "how this works" explainer live?

Per the age-UX decision (`GRAPH_RECOMMENDER.md §11`), there's a short non-technical explainer shown **after** onboarding, once the user is in — sets expectations and retroactively justifies the age ask.

**Question:** Is there a natural screen/moment in the current flow for this (first discovery load? after N swipes? a dismissible card in-feed?), or would it have to be bolted on? Suggest the least-intrusive placement you can find in the existing flow.

### E. Does the user get any sense that swiping is *doing* something?

Recommenders feel dead without a feedback loop the user can perceive. If nothing visibly changes as they swipe, engagement (and therefore data) dries up.

**Question:** Do the designs have any "your taste is taking shape" moment — a taste profile, a visible shift in the feed, a counter, anything? If not, is there a low-effort place to add one? I don't want a gamified mess, but *some* signal that the system is listening.

### F. Is the hidden "hearted" list correctly kept system-only?

Per §7 the double-tap "hearted" list is **system-only for now** and drip-feeds back into discovery. It should not be a user-facing list yet.

**Question:** Do the designs accidentally surface a "liked" list anywhere? Confirm it's genuinely hidden. (If the design *does* expose it and it looks good, tell me — I might reconsider, but the default is hidden.)

### G. The friction/signal inversion — worth thinking about hard

Here's the tension I most want your read on. Our **richest** training signal (explicit 5-star rating + review, which powers the `co_rating`/PCC edges in Phase 2) sits behind our **highest-friction** interaction (press-hold → focus mode → rate → write → swipe right). Our **cheapest** signal (bare swipe direction) is abundant. So the data that would most help the graph is exactly the data users are least likely to produce.

The design doc already hedges by making implicit-signal content edges Phase 1 and explicit-rating edges Phase 2 — the engine doesn't *depend* on ratings. But the UI still shapes how much explicit signal we ever get.

**Question:** Look at how focus mode / rating is surfaced in the designs. Is rating *discoverable and low-friction enough* that a reasonable fraction of users will do it sometimes — without making the default swipe flow heavier? Is there a middle-ground signal the design could capture cheaply (e.g. a quick thumb-up/down that's lighter than a full star+review but richer than a bare swipe)? This is the one I'd most like a concrete opinion on.

### H. Does the swipe grammar map cleanly to distinct, unambiguous events?

The engine needs the four directions + double-tap to resolve to **distinct** `InteractionType`s per feed (see `base.py`), and the discard-on-non-right-swipe rule in focus mode must be visually unmistakable (there's already a first-N-uses warning specced).

**Question:** In the designs, is each gesture's *meaning* legible to the user in the moment — especially the feed-dependent difference (right = "interested" in discovery vs "watched" in library)? Ambiguous gestures produce mislabeled training data, which is worse than no data. Flag any gesture whose meaning isn't obvious from the card UI.

---

## How to report back

For each item A–H: a one-line **finding** (what the designs actually do), a **verdict** (fine as-is / minor tweak / rethink), and where you'd rethink, a **concrete suggestion**. Lead your report with the 2–3 highest-leverage changes rather than going strictly in order — I want to know what matters most first. Don't touch the design files; this is a written recommendation for me to act on.
