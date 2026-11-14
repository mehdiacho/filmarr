"""
Firebase Auth dependency. Verifies the Bearer ID token the Android/web client sends and
returns the uid. On Cloud Run the Admin SDK picks up credentials from the runtime service
account via Application Default Credentials — no key file needed.
"""

from __future__ import annotations

import firebase_admin
from fastapi import Header, HTTPException, status
from firebase_admin import auth as fb_auth

# Idempotent init (module may import more than once under the server).
try:
    firebase_admin.get_app()
except ValueError:
    firebase_admin.initialize_app()


def get_uid(authorization: str | None = Header(default=None)) -> str:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or malformed Authorization header",
        )
    token = authorization.split(" ", 1)[1].strip()
    try:
        decoded = fb_auth.verify_id_token(token)
    except Exception:  # invalid / expired / revoked
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid ID token"
        )
    return decoded["uid"]
