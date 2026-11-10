# Granny's Knees 1.0 — Google Play publication plan

**Decision date:** 2026-11-05 (scope reconciled 2026-11-10)
**Status:** Approved planning direction; implementation present on `feature/play-store-readiness`; Play Console setup not yet verified.
**Publisher:** Personal Google Play developer account (owner-managed; no account creation implied).
**Distribution:** Canada only initially.
**Monetization:** Free, no advertising for 1.0.
**Product:** Native Android phone application with Glance home-screen widget and a Wear OS companion (`:wear` module).

**Latest reviewed commit:** `33fe033` (`feature/play-store-readiness`).

## Release promise

“Your joints. Your weather. Your excuse.” A playful **experimental weather-derived** 0–10 joint-weather index, not a medical prediction or symptom diagnosis. Forecast score is not an individual's reported pain. Symptom journal entries are user-provided notes stored on the device; they are not sent to the developer. Use Canadian English and avoid clinical claims.

## In scope for 1.0 (release candidate on this branch)

- First-run explanation, experimental disclaimer, About the Index and research/limitations transparency.
- Canadian city search and selection (`OpenMeteoGeocodingClient`), persist choice in app-private storage, refresh forecasts. **Optional** coarse/fine location when the user chooses “use this phone's location”; city search works without granting location.
- Current index score, location and last updated time.
- Current local-day hourly index curve and next five local days' low/high index, using the shared `index-v1` scorer.
- Honest loading, network error, missing data, stale/offline and cache-expired states.
- Glance home-screen widget showing the current score, city, timestamp and freshness; verify update and reboot behaviour (including expanded layouts where present in the build).
- **On-device symptom journal:** Room-backed local storage, in-app UI, export via the system share sheet, and “delete everything” for local tables. No developer-operated cloud copy.
- **Wear OS:** `:wear` module (tile) and phone `WatchLogService` for Wearable Data Layer messaging; sync stays between phone and paired watch, not to a developer backend.
- In-app **Your privacy** summary (informational; full policy remains a public HTTPS URL).
- Accessible phone UI, dark/light themes, production icon/approved artwork, English listing, Canadian availability.
- Privacy policy and policy-compliant declarations; signed AAB, internal and closed testing, production submission.

## Explicitly out of scope for 1.0

- Developer-operated accounts, sign-in, or cloud synchronization of journal or profile data.
- Sending symptom or journal data to the developer or to third-party analytics, crash, or advertising SDKs (none are planned in the current Gradle inventory; verify the release AAB).
- Individualized medical diagnosis, treatment recommendations, or flare **prediction** claims.
- Paid tiers, in-app advertising, AI personalization, or background location tracking.
- French store listing (tracked follow-up deliverable).

## Current code baseline

Observed on commit `33fe033`: `applicationId` `ca.aeo3.grannysknees` (placeholder until owner selects permanent ID), `targetSdk=37`, `versionName=0.3.81.1`. Weather uses Open-Meteo forecast and geocoding; city selection and optional GPS are implemented. Journal, watch sync, widget, and privacy UI are present in source. These are review observations, not Play acceptance. Owner must still run device regression and policy sign-off before submission.

## Gates

### G0 — Release scope and publisher readiness
- Confirm personal account eligibility and identity verification, developer display name and support contact, account ownership and public contact disclosure.
- Select a permanent, controlled reverse-domain application ID **before** any Play Console release; do not invent the owner's domain or identifier.
- Confirm Canadian English first release; French localization is a tracked follow-up.
- Decide public privacy policy hosting and support URL; owner must approve any publicly disclosed personal contact information.
- Acknowledge 1.0 includes local symptom journal and Wear sync; update store copy and Data safety accordingly.

### G1 — Functional beta
- City search and persistent location; optional location permission only after explicit user action.
- Onboarding, current score, graph, five-day low/high, journal, privacy screen, and widget work from one shared source of weather/index truth where applicable.
- Journal and watch paths tested on device/emulator to the extent hardware allows; document gaps (e.g. no paired watch in CI).
- Error, stale, unavailable, timezone/DST, network loss and reboot scenarios pass.
- `./gradlew test assembleDebug` succeeds; emulator and physical Samsung S24 FE smoke tests have screenshots and logs.

### G2 — Release and policy readiness
- Final application ID, versionCode/versionName strategy, minification/resource rules and reproducible `bundleRelease`.
- Play App Signing and owner-controlled upload key; keystores, passwords and service credentials never enter Git.
- Public HTML privacy policy, in-app link, accurate Data safety and Health apps declarations, app access/ads/content rating/target audience forms.
- Health-related feature declarations reviewed against actual functionality (weather index **and** optional user symptom notes); disclaimer in app and listing. Avoid assuming “entertainment” removes health obligations.
- Declare **Internet** and optional **ACCESS_COARSE_LOCATION** / **ACCESS_FINE_LOCATION** accurately; no background location.
- Approved dark/light PNGs committed, adaptive icon and store assets produced; unapproved SVG recreations not used.
- Play Console listing and screenshots match actual release build (including journal and wear only if shown in screenshots).

### G3 — Testing and production eligibility
- Internal Play track installation and upgrade smoke test.
- For new personal accounts created after 2023-11-13: at least 12 opted-in closed testers continuously for 14 days; collect feedback and evidence, then apply for production access. Confirm account-specific Console instructions.
- No P0/P1 defects, reproducible signed AAB, policy declarations complete, crash/ANR checks reviewed.
- Release only from `feature/play-store-readiness` or clean `origin/main`—not from local `main` carrying debug screenshot helpers.

### G4 — Canada launch and operations
- Submit production release with **Canada-only** country availability and staged rollout where available.
- Verify public listing, install/update path, widget, support contact and privacy links.
- Monitor Play Console crashes, ANRs, reviews and policy notices; document rollback/hotfix process.
- Do not mark published until Google approves and listing is publicly available.

## Sequencing

1. Week 1: G0, repo truth sweep, account initiation and permanent package ID decision.
2. Weeks 2–3: city selection, onboarding, UI/widget/journal/wear fixes, policy draft and branding.
3. Week 4: signing, release bundle, internal testing and full device regression.
4. Weeks 5–6+: closed testing (where required), tester feedback and production-access request.
5. Following eligibility: production review, Canada rollout and post-launch monitoring.

These are **planning estimates**, not guaranteed deadlines; account verification, testers and Google review are external dependencies.

## Official policy references (checked 2026-11-05; recheck before submit)

- Personal account selection: https://support.google.com/googleplay/android-developer/answer/13634885
- Personal account closed testing: https://support.google.com/googleplay/android-developer/answer/14151465
- Target API (new apps Android 16 / API 36+ from 2026-08-31): https://support.google.com/googleplay/android-developer/answer/11926878
- Health content and privacy: https://support.google.com/googleplay/android-developer/answer/16679511
- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291

Recheck current policies and account-specific requirements immediately before submission.
