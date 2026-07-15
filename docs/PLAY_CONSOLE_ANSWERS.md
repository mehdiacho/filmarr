# Filmarr — Play Console answers

Concrete answers for every Play Console section, tailored to Filmarr. Instantiated from
`../../templates/android/play-console/PLAY_CONSOLE_SETUP_TEMPLATE.md`. Work top-to-bottom.

## 0 · Header

| Field | Value |
|---|---|
| App name (listing, ≤30) | **Filmarr** |
| applicationId / package | **app.greenforrest.filmarr** |
| Firebase project id | **filmarr-mehdiacho** |
| Support email | **mehdiacho@gmail.com** |
| Website / brand domain | **https://filmarr-mehdiacho.web.app** |
| Privacy policy URL | **https://filmarr-mehdiacho.web.app/privacy.html** |
| Account deletion URL | **https://filmarr-mehdiacho.web.app/delete-account.html** |
| Regions / price | **Free**, all regions (adjust as desired) |

`applicationId` is immutable; the listing name can change any time.

## Quick checklist

- [ ] Privacy policy URL live (deploy `admin/` to Firebase Hosting)
- [ ] App access (guest browse + test credentials for gated features)
- [ ] Ads declaration → **No ads**
- [ ] Content rating (IARC questionnaire)
- [ ] Target audience & content
- [ ] Data safety
- [ ] Government apps → No
- [ ] Financial features → None
- [ ] Health → None
- [ ] App category & contact details → **Entertainment**
- [ ] Store listing (name, short + full description)
- [ ] Graphics (icon 512, feature 1024×500, ≥4 phone screenshots ≥1080px)
- [ ] Release: land the draft AAB, publish first release
- [ ] Service accounts invited (CI publisher — **done**)
- [ ] App-signing SHA-1 registered in Firebase — **done**

---

## 1 · Privacy policy
`https://filmarr-mehdiacho.web.app/privacy.html` (page in `admin/privacy.html`).

## 2 · App access
Filmarr allows **guest browsing** (Search + film detail) but gates the swipe feeds, library, and
profile behind sign-in. Choose **"All or some functionality is restricted"** and provide a
**test account** so reviewers can see the gated features:

> Reviewer note: "Browse works with no account via 'Continue as guest'. To review the personalized
> feeds/library/profile, sign in with the test account below."
>
> Test email: `________`  ·  Test password: `________`
> *(⚠ create a throwaway Firebase email/password user for this and fill it in.)*

## 3 · Ads
**No ads** — no ad SDK is present.

## 4 · Content rating (IARC) ⚠ RE-DECIDE
- Category: **Entertainment / Reference / Other** (closest to a film catalogue).
- Violence / sexuality / language / controlled substances / gambling: **No**.
- Users interact / exchange content / share to other users: **No** — Filmarr's taste data is
  private to each account; there is no chat, no public posting, no user-to-user sharing today.
  *(⚠ revisit if the "Overlap"/social features ever expose one user's data to another.)*
- Shares location: **No**.
- Digital purchases: **No** (free, no IAP).
- Expected result: **Everyone / PEGI 3**. *(Note: catalogue shows real film posters/titles; some
  films are mature, but the app itself surfaces metadata, not the films — Everyone is appropriate.)*

## 5 · Target audience and content ⚠ RE-DECIDE
Target **18 and over** (keeps Filmarr out of Families-policy requirements; the film catalogue
includes adult-rated titles). "Could the listing unintentionally appeal to children?" → **No**.

## 6 · Data safety ⚠ RE-DECIDE (most app-specific)

- Collects/shares user data? → **Yes**.
- Encrypted in transit? → **Yes** (Firebase/HTTPS).
- **Sharing:** everything is **Collected, not shared**. Transfers to Firebase and to our own
  recommender backend (Cloud Run) are service-providers processing on our behalf. TMDB is queried
  only for public film metadata; no user data is sent to it.
- **Account creation methods:** **Email and password** *and* **Google (OAuth)**.
- **Account deletion URL:** `https://filmarr-mehdiacho.web.app/delete-account.html`.
  There is also an **in-app deletion path** (Settings → Delete account → backend `DELETE /account`
  wipes all `users/{uid}` data + the auth user), which Data safety review prefers.
  "Delete some data without deleting the account?" → **Yes** (per-item removal in-app; same URL).

**Data-types matrix** (Collected · not shared · stored):

| Type | Collected | Optional? | Purpose |
|---|---|---|---|
| **Email address** | Yes | No (if signing in) | Account management |
| **Name** | Yes | Yes (only via Google Sign-In) | Account personalization |
| **User IDs** (Firebase UID) | Yes | No | Account management, app functionality |
| **App activity** — in-app actions (swipes, hearts, library, watched/rating marks, genre picks) | Yes | No | App functionality, personalization (recommendations) |
| **Other info** — optional self-reported age bracket | Yes | Yes | Personalization |
| Photos (Google profile photo) | Yes | Yes (Google Sign-In only) | Account personalization |

Everything else — location, contacts, messages, audio, financial info, health, precise
identifiers, crash logs/analytics — **Not collected** (no ads SDK, no Crashlytics wired today;
⚠ add "Crash logs + Diagnostics" here if you later add Crashlytics/analytics).

## 7 · Government apps
**No.**

## 8 · Financial features
**"My app doesn't provide any financial features."** Filmarr has no payments at all.

## 9 · Health
**"My app does not have any health features."**

## 10 · App category & contact
- App or game: **App** · Category: **Entertainment**.
- Tags: movies, film, recommendations, watchlist.
- Contact email: **mehdiacho@gmail.com** · Website: **https://filmarr-mehdiacho.web.app**.

## 11 · Store listing
- **App name (≤30):** `Filmarr`
- **Short description (≤80):** `Swipe to discover films you'll love — no searching, just your taste.`
- **Full description (draft):**
  > Filmarr finds your next film by feel, not by search.
  >
  > Swipe through a deck tuned to your taste. Heart what you love, build your watchlist, and mark
  > what you've watched — Filmarr learns from every swipe and rebuilds your recommendations around
  > you.
  >
  > • **A feed that's actually yours** — recommendations from a taste graph, not a popularity chart.
  > • **Build your library** — watchlist, watching, watched, plan-to-watch, and ratings in one place.
  > • **Discover by swiping** — no endless searching; just react and go.
  > • **Free, no ads** — and your taste data stays private to you.

## 12 · Graphics
- **App icon** 512×512 — `store-assets/out/icon-512.png`.
- **Feature graphic** 1024×500 — `store-assets/out/feature-1024x500.png`.
- **Phone screenshots** ≥4, ≥1080px short side — capture Discover, Build/Library, film detail,
  and the taste graph on-device. *(Still to do by hand.)*

## 13 · Release
CI already uploads a signed AAB to the **alpha** (closed testing "A1") track as a **draft**.
Publish that first draft release by hand → clears the "draft app" state → then future pushes with
`status=completed` auto-release to testers.

## 14 · Monetization
N/A — Filmarr is free, no products.

## 15 · Service accounts & CI — **done**
- CI publisher: `play-publisher@filmarr-mehdiacho.iam.gserviceaccount.com` invited with
  "Release apps to testing tracks."
- GitHub secrets set: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
  `GOOGLE_SERVICES_JSON_BASE64`, `TMDB_READ_TOKEN`, `PLAY_SERVICE_ACCOUNT_JSON`.
- App-signing SHA-1 (`8d:bd:8e:…:2d:82`) registered in Firebase — Google Sign-In works on
  Play-delivered builds.

---

### Open items to fill before submitting
1. **Test account** for §2 (throwaway email/password Firebase user).
2. **4+ phone screenshots** for §12.
3. Confirm **developer display name** ("Green Forrest") matches your Play developer account.
4. ~~In-app "Delete account" flow~~ — **done** (Settings → Delete account; backend `DELETE /account`).
