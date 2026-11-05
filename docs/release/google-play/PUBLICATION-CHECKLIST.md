# Google Play 1.0 — publication readiness checklist

**Updated:** 2026-11-05. **Decisions:** personal developer account; Canada-only initial release; free, no ads; weather-index phone app and widget. **Status:** checklist, not proof of completion.

**Latest reviewed commit:** `32d8b9e` (expanded hourly/five-day widget, emulator evidence, symptom contract). Review `docs/traycer/CURRENT-STATE.md` and `DEVICE-ACCEPTANCE.md` before asserting current functionality. Historic GP plan predates this commit.

## Rules for checking boxes

Use `[x]` only when the linked issue/PR, test output, screenshot, owner approval or Play Console evidence exists. `[ ]` means **not verified here**, not necessarily absent. Record proof and date in the release evidence ledger. Never store identity documents, private account details, keystore or credentials in GitHub.

## Gate 0: Account, product and immutable decisions

- [ ] Owner creates/opens personal Google Play Console account; pays applicable registration fee and completes identity/device verification required by Console.
- [ ] Owner verifies developer display name, public contact requirements and approved support email/website. Do not expose personal information without approval.
- [ ] Owner verifies whether personal-account production access requires closed testing; plan for **at least 12 continuously opted-in testers for 14 days** if applicable. Recruit early.
- [ ] Owner selects permanent Android `applicationId` and confirms control of any reverse-domain namespace; document it before first Play upload.
- [ ] Confirm 1.0 is free/ad-free, Canada only, weather index with phone and widget; symptom journal and Wear OS excluded.
- [ ] Decide English (Canada) listing and French localization as a subsequent, tracked deliverable.
- [ ] Confirm public support and privacy policy URLs and hosting owner.

**Gate 0 evidence:** decision record with non-sensitive values and owner approval.

## Gate 1: User-ready application

- [ ] Audit latest main against GP00 and prior N01–N04 evidence; reconcile newly merged expanded widget (#12).
- [ ] Replace any fixed Toronto default dependency with Canadian city search, clear province/country disambiguation, selection persistence and timezone-aware forecast.
- [ ] User can choose/change city without mandatory GPS or account sign-in.
- [ ] App restart, force stop and offline restart retain selected city and show honest freshness.
- [ ] First-run onboarding explains experimental 0–10 index, forecast-vs-symptom distinction, correlation vs causation, no medical prediction.
- [ ] About the Index describes variables, methodology, uncertainty, version and sources.
- [ ] Today displays city, current index, fetched time, current local-day hourly curve, next five local days' index lows/highs.
- [ ] Network errors, invalid geocoding, missing hours, stale/expired cache, timezone and DST do not fabricate values or crash.
- [ ] Current/expanded Glance widget layouts are inspected on device; correct score, location, time, resize, refresh, reboot and tap-through.
- [ ] Phone and widget use one index computation, one location and compatible freshness rules.
- [ ] Accessibility: TalkBack, font scaling, touch targets, colour contrast, no colour-only status, light/dark mode.
- [ ] Review Android 16+ edge-to-edge, notification and background-execution behaviour where applicable; request no unneeded permissions.

**Gate 1 evidence:** green unit tests, emulator screenshots/logcat, real Galaxy S24 FE smoke report; record emulator-only limits.

## Gate 2: Security, privacy and policy

- [ ] Inventory Android permissions, SDKs, outbound requests, IP/location transmission, caches, diagnostics, backups and any analytics/crash reporting.
- [ ] Decide whether geocoded city, forecast cache or preferences leave the device; document retention and deletion accurately.
- [ ] Public HTTPS HTML privacy policy describes actual data processing, provider(s), purpose, storage, retention, contact and rights; owner approves.
- [ ] Link privacy policy from app and Play listing; verify publicly accessible without login.
- [ ] Review Google Play Health apps declaration and health-related content requirements for this experimental joint-weather index.
- [ ] Complete accurate Data safety form; do not claim “no data collected” without a verified data-flow analysis.
- [ ] Complete ads declaration (none), target audience, content rating, app access, and any other Console app-content questionnaires.
- [ ] Ensure store and in-app copy make no diagnosis, treatment or individualized flare prediction claims.
- [ ] Confirm Open-Meteo API licensing, attribution and suitability for planned production usage and request volume.
- [ ] Verify third-party dependencies/licences, dependency security and no hard-coded secrets.
- [ ] Confirm Android backup behaviour and any personal data deletion mechanisms actually match privacy statements.

**Gate 2 evidence:** privacy policy URL, completed declaration matrix and owner sign-off. Agents prepare, owner submits.

## Gate 3: Release engineering and assets

- [ ] Set final `applicationId`, display name and `versionCode`/`versionName`; avoid changing ID after first Play registration.
- [ ] Confirm current Play target API policy and Android build toolchain; compile/target API 37 is configured but needs release verification.
- [ ] Configure release build, appropriate shrinking/obfuscation choices, ProGuard/R8 rules, resource handling and reproducible CI checks.
- [ ] Enable Play App Signing and create protected upload keystore/key with backup and recovery procedure; keep all secrets outside repository.
- [ ] Build signed `.aab` with `./gradlew clean test bundleRelease`; archive checksum and build provenance securely.
- [ ] Test release-like install, upgrade, cold start, offline state and widget (debug APK alone is insufficient).
- [ ] Commit exact owner-approved logo PNGs, adaptive launcher icon, monochrome icon where applicable and splash/theme resources; do not substitute rejected SVG recreations.
- [ ] Prepare high-res icon, feature graphic and genuine phone screenshots at current Play requirements; no invented features.
- [ ] Prepare Canadian English short/full description, release notes, support email and accurate content/category tags.
- [ ] Verify asset copyright, accessibility and consistency with actual signed build.

**Gate 3 evidence:** signed AAB, CI result, screenshot pack, store-copy approval, upload-key recovery record (private).

## Gate 4: Console testing and production access

- [ ] Create app in personal Play Console account and configure free pricing and Canada country availability.
- [ ] Complete Store listing, App content, policy forms and privacy URL.
- [ ] Upload signed AAB to **internal testing** and install from Play on a real phone; test upgrade and widget.
- [ ] Run full regression on physical Galaxy S24 FE and emulator; review crashes, ANRs and compatibility.
- [ ] Fix all release-blocking P0/P1 issues and record final regression evidence.
- [ ] If Console requires it, create closed testing track, recruit 12+ testers, confirm continuous 14-day opt-in and collect genuine feedback.
- [ ] Request production access only after account-specific criteria are met; record Google's decision.
- [ ] Verify release notes, country availability, policy declarations and owner approval immediately before submission.

**Gate 4 evidence:** Console testing/eligibility records, owner-approved release candidate. Never store tester personal details publicly.

## Gate 5: Publish and operate

- [ ] Submit release to production, Canada only; use staged rollout where supported.
- [ ] Confirm Google approval and public Canadian listing; test clean install and widget from Play.
- [ ] Monitor Play vitals, crashes, ANRs, ratings, user support and policy messages.
- [ ] Document release tag, bundle hash, version, rollout decision, known issues and hotfix procedure.
- [ ] Plan next version separately: symptom journal, Wear OS and expanded analytics only after their own gates.

**Definition of published:** publicly available Play listing and install in Canada, correct production version, verified privacy/support links, no known P0/P1 blockers.

## Official reference links to recheck at submission

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/14738291
- https://support.google.com/googleplay/android-developer/answer/16679511
- https://developer.android.com/studio/publish/app-signing

Policies and Console prompts can change. Verify them at the point of action.
