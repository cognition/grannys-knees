# Granny's Knees 1.0 — Google Play publication plan

**Decision date:** 2026-11-05 (scope reconciled 2026-11-10; widget/Wear deferred 2026-11-10)
**Status:** Approved planning direction; phone weather index and journal present on `main`; Play Console setup not yet verified.
**Publisher:** Personal Google Play developer account (owner-managed; no account creation implied).
**Distribution:** Canada only initially.
**Monetization:** Free, no advertising for 1.0.
**Product:** Native Android **phone** application (Today outlook, city selection, on-device journal, privacy summary). Home-screen widget and Wear OS are **later iterations**, not 1.0 publication gates.

**Latest reviewed commit:** current `main` (post keystore-escaping fix).

## Release promise

“Your joints. Your weather. Your excuse.” A playful **experimental weather-derived** 0–10 joint-weather index, not a medical prediction or symptom diagnosis. Forecast score is not an individual's reported pain. Symptom journal entries are user-provided notes stored on the device; they are not sent to the developer. Use Canadian English and avoid clinical claims.

## In scope for 1.0

- First-run explanation, experimental disclaimer, About the Index and research/limitations transparency.
- Canadian city search and selection (`OpenMeteoGeocodingClient`), persist choice in app-private storage, refresh forecasts. **Optional** coarse/fine location when the user chooses “use this phone's location”; city search works without granting location.
- Current index score, location and last updated time.
- Current local-day hourly index curve and next five local days' low/high index, using the shared `index-v1` scorer.
- Honest loading, network error, missing data, stale/offline and cache-expired states.
- **On-device symptom journal:** Room-backed local storage, in-app UI, export via the system share sheet, and “delete everything” for local tables. No developer-operated cloud copy.
- In-app **Your privacy** summary (informational; full policy remains a public HTTPS URL).
- Accessible phone UI, dark/light themes, production icon/approved artwork, English listing, Canadian availability.
- Privacy policy and policy-compliant declarations; signed AAB, internal and closed testing, production submission.

## Explicitly out of scope for 1.0 (later iterations)

- **Home-screen Glance widget** — removed from the shipping phone AAB; restore in a dedicated widget iteration.
- **Wear OS companion** — `:wear` module and phone watch sync removed from the shipping tree; restore in a dedicated Wear iteration.
- Developer-operated accounts, sign-in, or cloud synchronization of journal or profile data.
- Sending symptom or journal data to the developer or to third-party analytics, crash, or advertising SDKs (none are planned in the current Gradle inventory; verify the release AAB).
- Individualized medical diagnosis, treatment recommendations, or flare **prediction** claims.
- Paid tiers, in-app advertising, AI personalization, or background location tracking.
- French store listing (tracked follow-up deliverable).

## Current code baseline

`applicationId` `ca.aeo3.grannysknees`, `targetSdk=37`. Weather uses Open-Meteo forecast and geocoding; city selection, optional GPS, journal and privacy UI are in source. Widget and Wear have been **stripped** from the phone AAB and Gradle modules for 1.0. Owner must still run phone device regression and policy sign-off before submission.

## Gates

### G0 — Release scope and publisher readiness
- Confirm personal account eligibility and identity verification, developer display name and support contact, account ownership and public contact disclosure.
- Confirm reverse-domain application ID `ca.aeo3.grannysknees` (and any Play Console registration) before upload.
- Confirm Canadian English first release; French localization is a tracked follow-up.
- Decide public privacy policy hosting and support URL; owner must approve any publicly disclosed personal contact information.
- Acknowledge 1.0 is **phone-first**: weather index + local journal; **no** widget or Wear marketing/testing obligations for this release.

### G1 — Functional beta
- City search and persistent location; optional location permission only after explicit user action.
- Onboarding, current score, graph, five-day low/high, journal and privacy screen work from one shared source of weather/index truth.
- Error, stale, unavailable, timezone/DST, network loss and reboot scenarios pass.
- `./gradlew test assembleDebug` / signed `bundleRelease` succeed; emulator and physical Samsung S24 FE smoke tests have screenshots and logs for **phone** flows only.

### G2 — Release and policy readiness
- Final versionCode/versionName strategy, minification/resource rules and reproducible `bundleRelease`.
- Play App Signing and owner-controlled upload key; keystores, passwords and service credentials never enter Git.
- Public HTML privacy policy, in-app link, accurate Data safety and Health apps declarations, app access/ads/content rating/target audience forms.
- Health-related feature declarations reviewed against actual functionality (weather index **and** optional user symptom notes); disclaimer in app and listing.
- Declare **Internet** and optional **ACCESS_COARSE_LOCATION** / **ACCESS_FINE_LOCATION** accurately; no background location.
- Approved dark/light PNGs committed, adaptive icon and store assets produced; unapproved SVG recreations not used.
- Play Console listing and screenshots match the phone release build; **do not** require widget or Wear screenshots for 1.0.

### G3 — Testing and production eligibility
- Internal Play track installation and upgrade smoke test (phone).
- For new personal accounts created after 2023-11-13: at least 12 opted-in closed testers continuously for 14 days; collect feedback and evidence, then apply for production access.
- No P0/P1 defects, reproducible signed AAB, policy declarations complete, crash/ANR checks reviewed.

### G4 — Canada launch and operations
- Submit production release with **Canada-only** country availability and staged rollout where available.
- Verify public listing, install/update path, support contact and privacy links.
- Monitor Play Console crashes, ANRs, reviews and policy notices; document rollback/hotfix process.
- Do not mark published until Google approves and listing is publicly available.

## Sequencing

1. Week 1: account, application ID confirmation, upload signing, privacy host, Data safety draft.
2. Weeks 2–3: phone UI/journal polish, policy draft and branding; **widget/Wear explicitly deferred**.
3. Week 4+: internal/closed testing, production access if required, Canada release.

## Success criteria

Public Canadian Play listing for the phone app, installable signed release, accurate privacy and health declarations, no medical over-claims, and no open P0/P1 defects on core Today/city/journal flows.
