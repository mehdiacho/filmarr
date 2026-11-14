# KICKOFF_PROMPT.md

*(Paste this into the Claude Code session together with the documents it references.)*

---

You're picking up an existing movie/TV tracking app — Tinder-style swipe logging, a graph-based recommender, Letterboxd interop. Android is the current client (web + iOS later, shared backend). The app already has a UI and designs. What's new and needs building is the **recommendation backend on Firebase + Google Cloud + Cloud Run**, and I want to ship a Phase-1 version fast — ideally within a day.

**Read these first, in order. They are the source of truth; don't re-derive decisions they've already made:**

1. `CLAUDE.md` — master project doc.
2. `GRAPH_RECOMMENDER.md` — the recommender design. Note especially the phasing (§4, §13), the temporal model (§5), the two-feed split (§7), and the stack/store evolution (§16). **The stack is Firebase + GCP + Cloud Run; the graph store is a swappable layer behind an interface — don't hard-couple to it.**
3. `base.py` — the recommender contract. It's already written and already placed at `backend/app/recommender/base.py`. Everything implements it; don't change its shape without flagging me.
4. `backend/` — a runnable Cloud Run skeleton I've drafted: `main.py` (`/recommend` + Firebase auth), `firestore_graph_store.py` (GraphStore via Firestore `find_nearest`), `content.py` (Phase-1 scorer), `services/state.py`, `auth.py`, `Dockerfile`. It compiles and the scorer passes a logic test with a fake graph. Treat it as a starting point to wire up, not gospel.
5. `firestore.rules`, `firestore.indexes.json`, `SETUP.md` — security, indexes, and bring-up (incl. the vector-index gcloud step that's easy to miss).
6. `UI_RECOMMENDER_FIT.md` — **a task for you.** After you've got your bearings, read the actual designs against the 8 questions in it and report back to me with findings + the 2–3 highest-leverage changes. Don't edit the designs; recommend.

**Day-1 goal (Phase 1 only):** a working `/recommend` on Cloud Run serving content-based recs for both feeds, Firebase Auth + Firestore wired, security rules deployed, and the Android app pulling from it. **Content similarity via genre/keyword/cast Jaccard is an acceptable day-1 shortcut** if generating embeddings is a blocker — embeddings + `find_nearest` are the immediate upgrade, not a launch requirement.

**Guardrails — respect these unless you make a case to me first:**
- Don't build the GNN, co-rating, or temporal edges yet. They need interaction data that day-1 logging starts collecting. Phase discipline is deliberate (`GRAPH_RECOMMENDER.md §13`).
- **Don't compute the graph inside Firestore** — it stores events and serves precomputed edges; graph construction is a batch job elsewhere. Firestore vector search is server-side only.
- Keep everything behind `/recommend` and `GraphStore` so the store can be swapped later (Cloud SQL/pgvector, Vertex AI) without the app noticing.
- The interaction log is append-only.

**Where to start:**
1. Confirm the `backend/` skeleton runs locally against a real Firestore (ADC), then deploy per `SETUP.md` and get `/healthz` green.
2. Write the TMDB → `movies/{tmdbId}` catalog-seeding script (metadata, `popularity`, and either embeddings or Jaccard edges).
3. Make sure the Android client logs interactions in the shape `services/state.py` and the rules expect, and calls `/recommend` with the ID token.
4. Then pick up `UI_RECOMMENDER_FIT.md` and report back.

Ask me clarifying questions early rather than guessing on anything that affects data shape or the swipe→event mapping. When in doubt, ship the smaller thing that starts collecting data.
