# SETUP.md — Firebase + Cloud Run bring-up

Order matters. The one genuinely easy-to-miss step is the **vector index** (step 4): `find_nearest` returns an error until it exists, and it's created from the CLI, not `firestore.indexes.json`.

Replace `PROJECT_ID` and `REGION` (e.g. `europe-west1`) throughout. Set the embedding **DIMENSION** to match your model (768 / 1024 / 1536; must be ≤ 2048).

## 1. Project + services

```bash
gcloud config set project PROJECT_ID
gcloud services enable firestore.googleapis.com run.googleapis.com \
  cloudbuild.googleapis.com artifactregistry.googleapis.com aiplatform.googleapis.com
```

## 2. Firestore database (Native mode)

```bash
gcloud firestore databases create --location=REGION
```

## 3. Deploy security rules + composite indexes

With the Firebase CLI (`npm i -g firebase-tools`, `firebase login`), from the repo root where `firestore.rules` and `firestore.indexes.json` live:

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

## 4. Create the KNN vector index (the easy-to-forget one)

Not expressible in `firestore.indexes.json` — create it via gcloud. `movies` is the collection, `embedding` the vector field:

```bash
gcloud firestore indexes composite create \
  --collection-group=movies \
  --query-scope=COLLECTION \
  --field-config=field-path=embedding,vector-config='{"dimension":"DIMENSION","flat":"{}"}'
```

If you pre-filter vector queries (e.g. by genre before KNN), create a **composite** vector index with the filter field(s) first, then the vector field. Check status with `gcloud firestore indexes composite list`.

> Store embeddings as a Firestore vector value (`Vector([...])` / `FieldValue.vector([...])`), not a plain array, or the index won't apply. Generate them with Vertex AI text-embeddings (or the Firestore vector-search extension) — Firestore does not generate embeddings itself.

## 5. Seed the catalog

Populate `movies/{tmdbId}` from TMDB (metadata + `popularity` + `embedding`). This is a batch script (own task). For the **day-1 no-embeddings path**, skip embeddings and precompute genre/keyword/cast Jaccard neighbors into the `edges` collection instead — the GraphStore reads those as `content_sim` once you point it there.

## 6. Deploy the recommender to Cloud Run

From `backend/` (the runtime service account needs the **Cloud Datastore User** role for Firestore access — the default compute SA usually has it):

```bash
gcloud run deploy recommender \
  --source . \
  --region REGION \
  --allow-unauthenticated
```

`--allow-unauthenticated` lets the app receive requests; the app still enforces Firebase auth per-request (`app/auth.py`). Verify:

```bash
curl https://<cloud-run-url>/healthz            # {"ok": true}
# Authed call (ID token from a signed-in Firebase client):
curl -H "Authorization: Bearer <FIREBASE_ID_TOKEN>" \
  "https://<cloud-run-url>/recommend?mode=discover&n=20&explain=true"
```

## 7. Point the Android app at it

The client calls `GET /recommend` with the signed-in user's Firebase ID token in the `Authorization` header. It never queries Firestore vector search directly — vector search is server-side only, which is why it goes through Cloud Run.

## Local dev

```bash
cd backend
pip install -r requirements.txt
gcloud auth application-default login   # ADC for Firestore + token verification
PORT=8080 python -m app.main
```
