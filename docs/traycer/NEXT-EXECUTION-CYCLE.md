# Next execution cycle: native Android verification and R1 completion

**Status:** Superseded for new work. N01–N04 were merged as pull request #12 (`32d8b9e`). The next cycle is [NEXT-PUBLICATION-CYCLE.md](NEXT-PUBLICATION-CYCLE.md). This file remains the 2026-11-04 handoff.

**Repository reviewed:** `main` at `ed8ba1756134d5ad0ebe3edb0f338f91d87b6c29` (2026-11-04). This is a handoff, **not** evidence of passing builds, CI or emulator tests.

## Why this cycle

Native Android implementation has moved beyond the original T00 baseline: T01–T06 implementation commits exist; T08 current-index Glance widget was added in `6d3b7bd`; T07 24-hour index curve and five-day low/high forecast was added in `ed8ba17`. The historic T00 baseline must remain intact as a dated audit, not be rewritten as the current state.

The next delivery gate is a **device-verified R0/R1 weather experience**, followed by the symptom domain contract. Avoid Wear OS and symptom UI until prerequisites are verified.

## Execution tasks

### N01: Current implementation truth sweep (P0)
**Dependencies:** none. **Suggested owner:** Claude; independent Codex review.
**Owned path:** `docs/traycer/CURRENT-STATE.md` (new).
**Instructions:** Inspect current `main` source, tests, workflows and commits. Assess T01–T09 individually as implemented, partial, unverified or absent. Record file evidence, exact test results, CI status and blockers. Specifically inspect current widget receiver/provider code, forecast integration and the latest outlook. Preserve `docs/traycer/BASELINE.md` as historical evidence. Correct outdated README claims only in a separately reviewed docs change.
**Acceptance:** Every task state has code references and test evidence or explicit “not verified”; no claims derived solely from commit titles.

### N02: Build and emulator acceptance (P0)
**Dependencies:** N01 identifies runnable APK and environment. **Suggested owner:** Cursor/Codex.
**Owned paths:** `docs/traycer/DEVICE-ACCEPTANCE.md` and test fixes in isolated worktree.
**Host:** openSUSE Leap, SDK at `/home/rbrooker/Android/Sdk`. Do not assume an AVD exists; `emulator -list-avds` determines actual names. Confirm KVM, `adb devices -l`, SDK/Gradle toolchain. Build using `ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew test assembleDebug`; record exact output and GitHub Actions checks. Install and launch on an actual emulator or connected Galaxy S24 FE, and capture screenshots/logcat.
**Verify:** Live Open-Meteo response, Today score and timestamp, hourly index graph, five-day index low/high, loading/error/offline/stale states, timezone behaviour and no fabricated missing-hour scores.
**Acceptance:** Reproducible install/run plus evidence, or explicitly blocked with commands and reasons. A Gradle build alone is **not** device acceptance.

### N03: T09 expanded Android widgets (P1)
**Dependencies:** N01 and T07/T08 interface review; coordinate emulator with N02. **Suggested owner:** Codex implementation, Cursor visual/device review.
**Owned paths:** Widget package and tests; avoid concurrent edits to app shell, scoring and Gradle.
**Implement:** Expand the current-index Glance widget to support the agreed hourly and five-day index views, appropriately sized widget layouts, cache freshness, refresh/lifecycle and reboot behaviour. Reuse the forecast repository/scoring domain; do not duplicate calculations. Preserve “unavailable” rather than displaying zero. Verify widget and Today screen agree for the same forecast.
**Acceptance:** Widget tests, emulator screenshots in relevant sizes, update/restart checks, and consistent current/hourly/daily values. **R1 gate** requires N02 and N03 device verification.

### N04: T10 symptom observation contract (P1, independent parallel worktree)
**Dependencies:** N01 confirms no conflicting schema work. **Suggested owner:** Claude, Codex independent reviewer.
**Owned paths:** `docs/contracts/symptom-observations.md` and deterministic fixtures, not Android widget/UI files.
**Define:** Local Room append-only observation records, body part/side/symptom, 0–10 self-report, explicit changed vs confirmed-unchanged vs unreported semantics, timestamp and previous-value handling, correction/deletion/export policy, independent weather/device sensor snapshot with unit/source/capture time/availability. No automatic carry-forward, no inferred zeros, no medical claims.
**Acceptance:** Contract and fixture review passes; partial update, explicit zero, untouched symptoms and unavailable sensors have unambiguous cases. T11 persistence follows acceptance; T12 UI follows T11.

## Scheduling

1. N01 first.
2. N02 and N04 may proceed in parallel after N01, with disjoint files.
3. N03 after widget contract review; reserve emulator access so N02 and N03 do not interfere.
4. Accept R1 only after tests **and** a reproducible device demo.
5. Then T11–T14; defer T15–T18 until their dependencies are met.

## Standard Traycer report

For each task: status (planned / in progress / blocked / implemented-unverified / verified), summary in plain Canadian English, owned paths, commit/PR, exact commands and test results, device screenshots/logs where relevant, known limitations, next action and whether the release gate passed.

## Known decisions and risks

- The native Android build currently declares `com.example.grannysknees` as its temporary application ID; decide the production ID before Play Store release.
- The README still refers to `index.html` as “Current version”, although native implementation has begun.
- Approved dark/light PNG artwork has **not** been committed; the existing SVG recreations are not approved.
- Current index-v1 is experimental, not a medical forecast.
- Android SDK API 37 build configuration and the locally selected emulator image need independent compatibility verification.
