# filmarr

Swipe-based film & TV tracker. Tinder UI, graph recommender, Letterboxd export.

## Quick start

### Frontend (works now, no DB needed)

```bash
npm install
npm run dev          # http://localhost:5173
```

### Backend

**Step 1 — Database (one-time)**

Option A — Docker (recommended):
```bash
# Install Docker Desktop from https://docs.docker.com/desktop/install/windows-install/
docker compose up -d
```

Option B — Native Postgres on Windows:
1. Install from https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
2. In psql: `CREATE DATABASE filmarr;`
3. Install pgvector: https://github.com/pgvector/pgvector#windows

**Step 2 — Environment**

```bash
cp .env.example apps/api/.env
# Edit apps/api/.env and add your TMDB_API_KEY
# Get one free at https://developer.themoviedb.org/
```

**Step 3 — Migrate**

```bash
cd apps/api
uv sync
uv run alembic upgrade head
```

**Step 4 — Run API**

```bash
cd apps/api
uv run uvicorn app.main:app --port 8000 --reload
```

OpenAPI docs at http://localhost:8000/docs

## Stack

| Layer | Tech |
|---|---|
| Frontend | Vite + React + TS, Tailwind, Zustand, framer-motion, @use-gesture/react |
| Backend | FastAPI, SQLAlchemy async, Alembic |
| Database | Postgres 16 + pgvector |
| Package mgr | npm (frontend), uv (Python) |
| External data | TMDB API |

## Phase 1 checklist

- [x] Swipe UI (all 4 directions + double-tap + press-hold focus)
- [x] Undo stack (last 10 swipes, restores rating/review)
- [x] Discard warning toast (first 3 times)
- [x] Discovery + Library Build feeds
- [x] FastAPI skeleton (auth, movies, library, recommender, letterboxd)
- [x] Letterboxd CSV export adapter
- [ ] DB provisioned + migrations run
- [ ] TMDB API key wired → real movie data
- [ ] Frontend connected to API
- [ ] Auth flow wired end-to-end
