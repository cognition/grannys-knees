# Google Play — screenshot capture (local only)

**Updated:** 2026-11-10. **Status:** Guardrails for release engineering; not a mandate to automate captures in CI.

## Release rule

**Release and production builds must not ship debug-only screenshot export code.** In particular, do not merge `ScreenshotTodayActivity`, `PrimaryScreenshotMatrix`, or companion debug manifests/tests under `com.aetherealmind.grannysknees.screenshot`. Those paths existed only as local scaffolding and broke release compilation when left untracked on a developer machine.

Use owner-approved screenshots from a **signed release-like build** on a real device or emulator, matching [PUBLICATION-CHECKLIST.md](./PUBLICATION-CHECKLIST.md) Gate 3.

## What Git ignores

The root [`.gitignore`](../../../.gitignore) blocks accidental commits of:

| Pattern | Purpose |
|--------|---------|
| `app/src/**/grannysknees/screenshot/` (main, debug, androidTest) | Ad-hoc capture activities, matrices, and debug wiring |
| `scripts/capture_primary_screenshots.py` | Optional local automation; review before ever tracking |
| `samples/primary/` | Generated PNG output from local capture runs |

Committed store assets (when ready) should live under an explicit, reviewed path—for example `docs/release/google-play/assets/`—not under ephemeral `samples/primary/`.

## Local workflow (recommended)

1. Build and install a release-like variant (`bundleRelease` or `assembleRelease` smoke-tested per checklist).
2. Capture genuine UI on device or emulator; avoid mock data that misrepresents product behaviour.
3. Save working files outside the ignored scaffold, or under `samples/primary/` locally only (never required for compile).
4. Before opening a merge request, run `./gradlew clean test assembleRelease` (or project CI equivalent) to confirm no `screenshot` package sources are present.

## Future automated screenshot tests

Instrumented or Paparazzi-style tests may be added later using normal Android test conventions. They must not reintroduce debug-exported activities into `main` or `release` source sets. Update this document and `.gitignore` if the team adopts a tracked test suite with different paths.

## Optional capture script

If you maintain `scripts/capture_primary_screenshots.py` locally, treat it as **untracked helper tooling** unless the team explicitly reviews and commits a maintained version. Do not copy broken Kotlin screenshot sources from another checkout into `main` or release branches without the same review.
