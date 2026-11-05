# Traycer — Google Play 1.0 implementation backlog

**Parent plan:** [Google Play publication plan](../../release/google-play/RELEASE-PLAN.md)
**Status:** Planned, not executed. **Scope:** Personal publisher, Canada only, free/ad-free, weather-only 1.0.
**Coordination:** One integration owner. Agents use independent branches/worktrees and PRs. No feature marked verified from documentation alone.

| ID | Priority | Dependencies | Suggested lead | Acceptance evidence |
| --- | --- | --- | --- | --- |
| GP00 | P0 | none | Claude audit, Codex reviewer | Truth sweep current main and N01–N04; code/test/CI/device matrix, exact blockers. |
| GP01 | P0 | GP00 | Owner + Codex | Confirm personal publisher requirements, final application ID, account ownership/contact exposure; no invented values. |
| GP02 | P0 | GP00 | Codex | Canadian city search, saved choice, geocoded forecast, no mandatory GPS, error tests; remove fixed Toronto dependency. |
| GP03 | P0 | GP00, GP02 | Cursor + Codex | First-run onboarding, About the Index, honest offline/loading states, accessible phone UX. |
| GP04 | P0 | GP00, GP02 | Codex | Basic current-index Glance widget refresh/reboot/resize, same score and freshness as Today; device evidence. |
| GP05 | P0 | GP00 | Claude policy research, owner review | Public HTML privacy policy draft, in-app link, Health apps/Data safety declaration evidence, permissions audit; never submit declarations automatically. |
| GP06 | P1 | GP01 | Cursor | Approved PNG assets, adaptive icon, feature graphic, screenshots, Canadian English store copy; review actual assets. |
| GP07 | P0 | GP01, GP02, GP03, GP04 | Codex | Final application ID, signing plan, release AAB and CI release checks; secret-free repo and reproducible build. |
| GP08 | P0 | GP03, GP04, GP05, GP06, GP07 | Codex + Cursor | Emulator and physical S24 FE regression, accessibility, network/DST/stale/upgrade cases; no P0/P1 bugs. |
| GP09 | P0 | GP05, GP06, GP07, GP08 | Owner, agent assists | Play Console app/listing, declarations, internal track, 12 opted-in closed testers/14 days **if applicable**, feedback log and production eligibility. |
| GP10 | P0 | GP09 | Owner, agent assists | Canada-only production submission, approval verification, staged rollout and monitoring/runbook. |

## Task handoffs and ownership

### GP00 — Evidence-first audit
Inspect source, workflows, merged PRs and any local worktrees the agent can actually access. Reconcile original T00–T09 with N01–N04; do not infer no work from main alone. Produce `docs/traycer/CURRENT-STATE.md` with exact commands, observed outcomes and open issues. **Gate:** integration owner approves factual baseline.

### GP01 — Publisher and immutable identifiers
Owner must decide developer display name, personal-account identity verification, publicly shown contacts, and final app ID. Verify ownership of any proposed domain. Review Play Console account-specific requirements and fees before payment. **Gate:** signed-off decisions; do not change application ID blindly.

### GP02 — Location vertical slice
Build geocoding client and UI for city search; save explicit selection using DataStore or suitably minimal storage. Reuse weather repository and scorer; no parallel score implementation. Tests for duplicate city names, province labels, timezone, invalid city, no connection, app restart, widget consistency. **Gate:** fresh install in Canada can select city and get real forecast.

### GP03 — First-use and transparency
Onboarding clearly explains experimental nature, correlation not causation and non-medical purpose. Add About screen with index inputs, version, uncertainty and sources. Accessible labels, font scaling, dark/light and appropriate empty/error states. **Gate:** first-time tester can understand app without guidance.

### GP04 — Widget release slice
Audit existing T08 widget and fix lifecycle, stale display, tap-through, refresh, and consistency. Basic current-index widget is required; expanded hourly/five-day widget (T09) explicitly deferred if not necessary for 1.0. **Gate:** phone and widget agree; reboot/update tested.

### GP05 — Policy evidence
Inventory every data flow, network destination, permission, SDK and any telemetry. Draft policy for public non-PDF HTTPS page and in-app link. Map actual behaviour to Data safety, Health apps declaration, ads, target audience, content rating and app access. **Gate:** owner approves and published policy URL works. No unsupported “no data collected” claim.

### GP06 — Brand and listing
Use only owner-approved exact PNGs, not existing SVG approximations. Produce adaptive icon, high-resolution icon, feature graphic, screenshots captured from release-like app and accurate short/full descriptions. Validate current Play asset specifications. **Gate:** assets and copy reviewed by owner.

### GP07 — Release engineering
Use permanent app ID and monotonic versionCode; protect upload keystore outside repo; enable Play App Signing. Document build, signed `.aab`, artifact provenance and recovery. Test `./gradlew test bundleRelease` and installable test build. **Gate:** release artifact and CI evidence, no secret leakage.

### GP08 — Device acceptance
Test on configured Android emulator and real Galaxy S24 FE. Cover new install, upgrade, city selection, offline/stale/expired, bad provider data, DST, widget lifecycle, background/foreground, accessibility and rotation. Record screenshots and logs. **Gate:** regression checklist signed off; P0/P1 defects closed.

### GP09 — Testing and Console readiness
Owner creates/configures Play Console personal account and reviews identity/public disclosures. Prepare internal test, recruit testers early, run closed test per Console requirement (new personal accounts generally 12 continuous opted-in testers for 14 days). Log feedback and changes; apply for production access only after eligibility. **Gate:** Console confirms production eligibility.

### GP10 — Publish and operate
Owner reviews final declarations, release notes, country availability Canada only and submits. Verify approval and public availability. Monitor crashes/ANRs, reviews, policy notices and emergency fixes. **Gate:** public listing verified.

## Parallel work plan

- Batch A: GP00, then GP01 owner decisions; start tester recruitment and Play Console verification early.
- Batch B: GP02 and GP05 parallel; GP06 can prepare assets independently after owner approval.
- Batch C: GP03 and GP04 after GP02 interfaces are stable.
- Batch D: GP07 then GP08; GP09 begins internal track when compliant AAB is ready.
- Batch E: GP09 closed test/eligibility; GP10 only after approval.

**Do not treat T10–T18 as release blockers.** Symptom journalling, extended widgets, Wear OS and correlation analysis are post-1.0 roadmap items unless a compliance or functional defect requires a change.

## Standard output from every agent

Plain-language status, changed files, commit/PR, test commands and results, screenshots/device evidence if relevant, blockers, policy implications, reviewer verdict and exact next action. Keep plans and actual implementation status separate.
