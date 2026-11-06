# Traycer — publication execution backlog (detailed)

**Parent:** [Publication checklist](../release/google-play/PUBLICATION-CHECKLIST.md) · [Release plan](../release/google-play/RELEASE-PLAN.md)
**Scope:** personal publisher, Canada only, free/ad-free, weather-only 1.0. **As-of:** 2026-11-05. All tasks are **unverified**, not asserted unimplemented.

**Baseline update:** `32d8b9e` merged expanded hourly/five-day widget and emulator evidence. GP04 must **audit and harden** existing work, not reimplement T09. Symptom contract was also merged, but symptom functionality is not a 1.0 release dependency.

**PUB05 drafts committed (2026-11-05):** [privacy policy draft](../release/google-play/PRIVACY-POLICY-DRAFT.md), [data-flow audit](../release/google-play/PRIVACY-DATA-FLOWS.md), and [Play declarations worksheet](../release/google-play/POLICY-DECLARATIONS.md). These are **drafts only**, not approved, hosted or submitted. Complete provider, backup, SDK and owner-contact verification before closing PUB05.

## Work packages

| ID | Priority | Depends on | Lead | Deliverable / acceptance |
|---|---|---|---|---|
| PUB00 | P0 | none | Claude audit + Codex reviewer | Reconcile latest main, current-state/device evidence, original GP tasks; traceable status matrix and blocker list. |
| PUB01 | P0 | PUB00 | Owner | Personal Play Console verification, public contact decisions, permanent app ID, tester recruitment plan. |
| PUB02 | P0 | PUB00 | Codex | Canadian city search, saved location, provider timezone and forecast integration; unit/device tests. |
| PUB03 | P0 | PUB02 | Cursor + Codex | First-run onboarding, About Index, loading/offline/empty states and accessibility acceptance. |
| PUB04 | P0 | PUB00,PUB02 | Codex + Cursor | Verify/harden existing Glance current/hourly/five-day widget, refresh/reboot/resize, phone parity; device screenshots. |
| PUB05 | P0 | PUB00 | Claude + owner | Data-flow audit, privacy policy, Health apps/Data safety and Play app-content declaration matrix. |
| PUB06 | P1 | PUB01 | Cursor + owner | Approved branding, launcher icon, listing graphics/screenshots, Canadian English copy. |
| PUB07 | P0 | PUB01,PUB02,PUB03,PUB04 | Codex | Permanent ID, versioning, signing/CI, signed AAB, secret-free build evidence. |
| PUB08 | P0 | PUB03,PUB04,PUB05,PUB06,PUB07 | Codex QA + Cursor | Device regression matrix, release-candidate acceptance, zero open P0/P1 bugs. |
| PUB09 | P0 | PUB05,PUB06,PUB07,PUB08 | Owner + agent support | Internal testing track, Console declarations, closed test if required, production eligibility. |
| PUB10 | P0 | PUB09 | Owner + agent support | Canada-only production submission, approval, Play install, monitoring and release record. |

## Executable task briefs

### PUB00 — Current-state truth sweep
**Files:** `docs/traycer/PUBLICATION-STATUS.md`; no app changes.
1. Read `docs/traycer/CURRENT-STATE.md`, `DEVICE-ACCEPTANCE.md`, merged #12, current source, CI and tests.
2. For each PUB task mark not started / partial / implemented-unverified / verified / blocked; include file references, command output, device evidence.
3. Identify which GP00–GP10 items already exist and avoid duplicate implementation.
4. Identify actual release blockers, separating owner/Console actions from code changes.
**Done:** independent reviewer agrees evidence-backed matrix. **No feature code in this task.**

### PUB01 — Publisher decisions and early external dependencies
**Owner only for account actions.** Agents draft checklist, never impersonate or complete identity verification.
1. Confirm personal account creation, identity verification, public developer display name, approved contact email and privacy hosting.
2. Propose 2–3 app IDs; owner selects final namespace based on controlled domain, not guessed ownership.
3. Confirm closed-test eligibility requirements shown by Console; recruit testers early and privately.
4. Record decisions in `docs/release/google-play/DECISIONS.md` without sensitive identity or account numbers.
**Done:** explicit owner approval and non-sensitive decision record.

### PUB02 — City-selection vertical slice
**Owned paths:** geocoding/location preference and relevant forecast UI/tests; coordinate widget interfaces.
1. Search Canadian municipalities via Open-Meteo geocoding; show city/province/country and handle duplicates.
2. Persist user selection, latitude/longitude/timezone; permit change and restore after process death.
3. Remove fixed Toronto dependency in forecast fetching; avoid mandatory GPS.
4. Cover invalid city, no results, no network, changed timezone, stale cache from previous city and widget refresh.
**Done:** clean install → select city → live index → restart → same city, with tests and device evidence.

### PUB03 — Onboarding and transparency
**Owned paths:** onboarding/About/Today UI and tests.
1. Explain weather-derived estimate, uncertainty, correlation ≠ causation, non-medical nature.
2. Add About the Index with versioned inputs, methodology, sources and privacy link.
3. Ensure first-run state, screen-reader labels, font scaling, dark/light contrast, error/loading/offline messages.
**Done:** first-time tester can complete flow without instructions; accessibility screenshots/checks.

### PUB04 — Widget acceptance, not rebuild
**Owned paths:** existing widget implementation and widget tests.
1. Inspect merged #12 current/hourly/five-day views and existing emulator evidence.
2. Verify selected city, score, freshness and local dates agree with Today.
3. Verify resize, launcher add/remove, tap-through, network failure, stale/expired data, reboot and refresh scheduling.
4. Fix only reproducible defects; no duplicate index engine.
**Done:** documented emulator and real-device widget matrix; existing functionality preserved.

### PUB05 — Policy and data flows
**Owned paths:** `docs/release/google-play/PRIVACY-DATA-FLOWS.md`, `POLICY-DECLARATIONS.md`, privacy policy source.
1. Inventory permissions, outbound APIs, geocoding queries, IP/location, local storage, backups, SDKs, diagnostics and telemetry.
2. Confirm Open-Meteo licensing/attribution and production usage.
3. Draft public HTTPS HTML privacy policy; owner approves and publishes; add app link.
4. Map actual behaviour to Data safety, Health apps declaration, ads, content rating, audience and app access.
5. Review all index language for health/medical claims; never assert entertainment-only exemption.
**Done:** accurate declaration worksheet and live policy URL; owner signs off. Agents do not submit forms.

### PUB06 — Store identity and listing
**Owned paths:** `assets/`, Android launcher resources and `docs/release/google-play/store/`.
1. Require exact approved PNG artwork from owner, not old SVG approximations.
2. Produce adaptive launcher icon, feature graphic, store icon and authentic phone screenshots.
3. Draft Canadian English short/full descriptions and release notes without clinical promises.
4. Check current Google Play dimensions and listing requirements.
**Done:** reviewed asset pack and approved listing copy.

### PUB07 — Signed production bundle
**Owned paths:** Gradle release config, CI, release engineering docs.
1. Apply owner-approved final application ID, versionCode/versionName and compatible SDK toolchain.
2. Configure release signing and Play App Signing; owner keeps keystore and secrets outside repo.
3. Add release bundle build/test in CI without exposing signing secrets.
4. Run `./gradlew clean test bundleRelease`, inspect AAB and checksum; document upgrade path and key recovery.
**Done:** reproducible signed AAB and security review; do not upload until PUB05/PUB06 owner approval.

### PUB08 — Final quality gate
**Owned paths:** `docs/release/google-play/QA-MATRIX.md` and fixes through scoped PRs.
1. Test clean install, upgrade, city search, network/offline, DST, missing hours, stale/expired, UI and widget.
2. Run on configured emulator and physical Samsung S24 FE; report hardware-only gaps explicitly.
3. Test TalkBack, font scale, contrast, orientation, app lifecycle, Play-delivered install and crash/ANR observations.
4. Maintain bug severity and rerun after fixes.
**Done:** all P0/P1 resolved, owner-approved release candidate.

### PUB09 — Console tracks and eligibility
**Owner-controlled Play Console.**
1. Create app, free pricing, Canada distribution, listing and all policy declarations.
2. Upload AAB to internal testing; verify install/upgrade on real device.
3. Run closed testing if required for personal account, including 12+ continuously opted-in testers over 14 days, collect feedback.
4. Request production access after Console eligibility and retain evidence privately.
**Done:** production access confirmed and submission-ready candidate.

### PUB10 — Publish and monitor
**Owner approval required for production.**
1. Confirm release notes, app version, country availability, support/privacy links and no P0/P1 blockers.
2. Submit production release, optionally staged rollout; wait for Google approval.
3. Verify Canadian public listing, install, update and widget behaviour.
4. Capture release tag, version, AAB hash, incident/hotfix plan and monitor Play vitals.
**Done:** publicly installable Canadian listing, monitored launch.

## Batch schedule and conflicts

- **Batch 0:** PUB00 and owner-led PUB01; recruit testers now.
- **Batch 1:** PUB02 and PUB05 parallel; PUB06 asset preparation in separate paths.
- **Batch 2:** PUB03 and PUB04 once location interfaces stabilize.
- **Batch 3:** PUB07 then PUB08; do not confuse debug build with signed release acceptance.
- **Batch 4:** PUB09 external testing and eligibility; PUB10 after owner approval.

Only one integrator edits shared application shell/Gradle at a time. Separate PR per package. Codex independently reviews domain/Gradle changes, Claude reviews policy/docs, Cursor validates UX and device behaviour. Report: actual status, changed files, PR/commit, exact tests, device proof, blockers, next action and gate verdict.

## Non-goals

Do not implement symptom journal, Wear OS, accounts, analytics, ad SDK, medical predictions or extra widgets to meet 1.0 release gate. Any later feature requires a separately approved scope change.
