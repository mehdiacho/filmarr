# Filmarr — Android

Swipe-first film & TV tracker. Native **Kotlin + Jetpack Compose** (Material 3, cinema-dark), built from the Filmarr Android design + Ratio Spec. Google-native backend (Firebase + Cloud Run).

## Stack
- **UI:** Jetpack Compose, Material 3 (dark-only), Navigation-Compose. Bricolage Grotesque + Hanken Grotesk (bundled variable fonts).
- **Build:** AGP 9.2.1 · Gradle 9.6 · Kotlin 2.2.10 · Compose BOM 2024.09 · compileSdk 36.1 · minSdk 24.
- **Backend:** Firebase Auth (Google + Email/Password) · Firestore · Firebase AI (Gemini) for "Ask filmarr". Python recommender on Cloud Run (later).
- **Data:** TMDB for metadata/posters (token via `.env` → Secrets Gradle plugin → `BuildConfig`).
- **Package:** `app.greenforrest.filmarr` (namespace `com.mehdiacho.filmarr`).

## Screens (from the design, 16)
Onboarding · Discover (live swipe deck) · Build · Film detail · Library (grid/diary/lists) · Search + Quick-add · Backlog Blitz · Tonight · Confidence log · Ask filmarr (AI) · Overlap (social) · Profile/stats · Letterboxd import · Taste graph.

## Current state
- Full UI implemented against an in-memory mock repository (`FilmRepository`). The Discover deck is fully interactive (drag/rotate/commit, directional badges, double-tap heart, focus review, undo).
- Firebase project `filmarr-mehdiacho` created; Android app registered; `google-services.json` in place; Firestore (eur3) live; upload keystore generated.

## Setup
1. Android SDK at the path in `local.properties` (`sdk.dir`).
2. Copy `.env.example` → `.env` and paste your TMDB v4 read token.
3. `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Secrets (git-ignored — back these up)
`.env` (TMDB), `keystore.properties` + `upload-keystore.jks` (Play upload key), `google-services.json` (Firebase).

## TODO (next)
- Wire `FirebaseAuth` (Google Credential Manager + email/password) and Firestore behind `FilmRepository`.
- TMDB Retrofit service + Coil poster loading.
- Enable Email/Password + Google providers in Firebase Console → Auth.
- Cloud Run: containerize the Python recommender; `firebase-ai` for Ask filmarr.
- CI: `.github/workflows/release.yml` for Play upload; Play Console app + service account.
