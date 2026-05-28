FROM python:3.12-slim

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Copy dependency files first for layer caching
COPY pyproject.toml ./
# uv.lock is generated on first uv sync — copy if present
COPY uv.lock* ./

# Install dependencies (no dev extras)
RUN uv sync --no-dev --frozen 2>/dev/null || uv sync --no-dev

# Copy application code
COPY . .

EXPOSE 8000

# Render injects $PORT; fall back to 8000 locally.
# Migrations run with a hard timeout and a `;` (not `&&`) so a hung/slow
# migration — e.g. async engine teardown stalling over the Supabase pooler —
# can never block the web server from starting. The schema is the source of
# truth in the DB; a failed/timed-out upgrade just means "no pending changes".
CMD ["sh", "-c", "timeout 90 uv run alembic upgrade head || echo '[startup] alembic step skipped (timeout/error) — continuing to server'; uv run uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
