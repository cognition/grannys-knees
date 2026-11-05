# Granny's Knees 1.0 — Google Play publication plan

**Decision date:** 2026-11-05
**Status:** Approved planning direction; implementation and Play Console setup not yet verified.
**Publisher:** Personal Google Play developer account (owner-managed; no account creation implied).
**Distribution:** Canada only initially.
**Monetization:** Free, no advertising for 1.0.
**Product:** Native Android phone application and a basic current-index home-screen widget.

## Release promise

“Your joints. Your weather. Your excuse.” A playful **experimental weather-derived** 0–10 joint-weather index, not a medical prediction or symptom diagnosis. Forecast score is not an individual's reported pain. Use Canadian English and avoid clinical claims.

## In scope for 1.0

- First-run explanation, experimental disclaimer, About the Index and research/limitations transparency.
- Search/select a Canadian city, persist choice, refresh forecasts. No mandatory device location permission; avoid GPS in 1.0 unless explicitly approved.
- Current index score, location and last updated time.
- Current local-day hourly index curve and next five local days' low/high index, using the shared `index-v1` scorer.
- Honest loading, network error, missing data, stale/offline and cache-expired states.
- Basic Glance home-screen widget showing the current score, city, timestamp and freshness; verify update and reboot behaviour.
- Accessible phone UI, dark/light themes, production icon/approved artwork, English listing, Canadian availability.
- Privacy policy and policy-compliant declarations; signed AAB, internal and closed testing, production submission.

## Explicitly out of scope

- Symptom logging, Room symptom history, device/watch sensor collection, individual correlations, export of symptom data.
- Expanded hourly/five-day widget layouts (optional later), Wear OS, AI personalization, paid tiers, ads, accounts and cloud sync.
- Any clinical prediction or diagnosis claims.

## Current code baseline

Last confirmed application commit: `ed8ba175` (T07 hourly and five-day outlook); T08 current-index Glance widget in `6d3b7bd`. Subsequent commits `a3ee657` and `906691f` are planning only. Existing `com.example.grannysknees` is a placeholder, `targetSdk=37`, `versionName=0.3.0`. Existing weather integration initially used fixed Toronto coordinates. These are review observations, not runtime acceptance. Start with an updated truth sweep and inspect actual current code.

## Gates

### G0 — Release scope and publisher readiness
- Confirm personal account eligibility and identity verification, developer display name and support contact, account ownership and public contact disclosure.
- Select a permanent, controlled reverse-domain application ID **before** any Play Console release; do not invent the owner's domain or identifier.
- Confirm Canadian English first release; French localization is a tracked follow-up.
- Decide public privacy policy hosting and support URL; owner must approve any publicly disclosed personal contact information.

### G1 — Functional beta
- City search and persistent location replace hard-coded Toronto; no permission necessary for city lookup.
- Onboarding, current score, graph, five-day low/high and basic widget work from one shared source of weather/index truth.
- Error, stale, unavailable, timezone/DST, network loss and reboot scenarios pass.
- `./gradlew test assembleDebug` succeeds; emulator and physical Samsung S24 FE smoke tests have screenshots and logs.

### G2 — Release and policy readiness
- Final application ID, versionCode/versionName strategy, minification/resource rules and reproducible `bundleRelease`.
- Play App Signing and owner-controlled upload key; keystores, passwords and service credentials never enter Git.
- Public HTML privacy policy, in-app link, accurate Data safety and Health apps declarations, app access/ads/content rating/target audience forms.
- Health-related feature declarations reviewed against actual functionality; disclaimer in app and listing. Avoid assuming “entertainment” removes health obligations.
- Approved dark/light PNGs committed, adaptive icon and store assets produced; unapproved SVG recreations not used.
- Play Console listing and screenshots match actual release build.

### G3 — Testing and production eligibility
- Internal Play track installation and upgrade smoke test.
- For new personal accounts created after 2023-11-13: at least 12 opted-in closed testers continuously for 14 days; collect feedback and evidence, then apply for production access. Confirm account-specific Console instructions.
- No P0/P1 defects, reproducible signed AAB, policy declarations complete, crash/ANR checks reviewed.

### G4 — Canada launch and operations
- Submit production release with **Canada-only** country availability and staged rollout where available.
- Verify public listing, install/update path, widget, support contact and privacy links.
- Monitor Play Console crashes, ANRs, reviews and policy notices; document rollback/hotfix process.
- Do not mark published until Google approves and listing is publicly available.

## Sequencing

1. Week 1: G0, repo truth sweep, account initiation and permanent package ID decision.
2. Weeks 2–3: city selection, onboarding, UI/widget fixes, policy draft and branding.
3. Week 4: signing, release bundle, internal testing and full device regression.
4. Weeks 5–6+: closed testing (where required), tester feedback and production-access request.
5. Following eligibility: production review, Canada rollout and post-launch monitoring.

These are **planning estimates**, not guaranteed deadlines; account verification, testers and Google review are external dependencies.

## Official policy references (checked 2026-11-05)

- Personal account selection: https://support.google.com/googleplay/android-developer/answer/13634885
- Personal account closed testing: https://support.google.com/googleplay/android-developer/answer/14151465
- Target API (new apps Android 16 / API 36+ from 2026-08-31): https://support.google.com/googleplay/android-developer/answer/11926878
- Health content and privacy: https://support.google.com/googleplay/android-developer/answer/16679511
- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291

Recheck current policies and account-specific requirements immediately before submission.
