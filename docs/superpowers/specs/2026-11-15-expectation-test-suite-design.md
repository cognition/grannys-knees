# Granny's Knees expectation test suite

## Goal

A robust test suite driven by **user expectations** and **design expectations**,
not by accidental coverage of whatever the cache last showed. Automate contract
and privacy-critical paths; keep a named acceptance matrix for UX, design, and
device quirks. Catalogue covers phone, widget, and Wear; implement phone now;
widget/Wear stay deferred tracks until those modules return.

## Decisions

| Decision | Choice |
| --- | --- |
| Gate | Hybrid: CI automation + named acceptance |
| Surface catalogue | Phone + widget + Wear |
| UI drivers | Compose UI Test (honesty) + Maestro (smoke journeys) |
| Delivery | Phone-first pyramid; companions deferred |
| Freshness | Controlled clocks + fixtures for L1/L2; live pulls assert shape, not pinned scores |

## Architecture

```text
Expectation sources → UE/DE/PE catalogue → L1 JVM / L2 Compose / L3 Maestro / L4 Acceptance
                                              ↓
                                    Phone NOW | Widget/Wear DEFERRED
```

### Layers

| Layer | Owns | Anti–false-green rule |
| --- | --- | --- |
| L1 JVM | Scoring, cache freshness, journal semantics, Feel unlock, mappers | Fixed `referenceNow`; fixture scenario metadata; never assert “score == 4” from an old live capture |
| L2 Compose UI Test | Today honesty states, journal partial save, city decline, export/reset surfaces | Inject `TodayUiState` / fakes; assert state copy and presence/absence of score, not live Open-Meteo numbers |
| L3 Maestro | Few end-to-end smokes | After refresh, assert city, band from allowed set, recent update label, no invented zeros; **do not** pin exact index |
| L4 Acceptance | Device, TalkBack, lake-teal visual, widget/Wear | Pull **new** forecast data when exercising live; treat stale/expired as first-class scenarios |

### Freshness honesty (mandatory)

1. Separate clocks from weather. L1/L2 inject controllable time and cache age.
2. Live pulls (L3/L4) assert **shape and honesty**, never a numeric score from a prior run.
3. Contract fixtures stay deterministic; each case has a scenario id. Tests set the
   clock to the fixture’s local day — wall clock must not silently mismatch.
4. Every freshness scenario asserts a failure mode if the app kept a last-good
   score after expiry (expired → no score; missing hour → invalid-hour copy;
   offline-empty → no outlook; stale → score allowed **and** stale labelling).
5. At least one refresh path proves `last updated` advances and stale clears when
   a newer envelope arrives (mock or live).

## Expectation catalogue

Canonical IDs live in [`docs/qa/expectation-catalogue.md`](../../qa/expectation-catalogue.md).
Acceptance rows live in [`docs/qa/acceptance-matrix.md`](../../qa/acceptance-matrix.md).

## Non-goals

- Pixel-perfect screenshot goldens as a CI gate.
- Restoring widget/Wear code as part of this suite work.
- Replacing existing JVM unit tests; this suite extends and organises them.

## CI

- PR: `./gradlew test assembleDebug` (existing) plus any new JVM tests.
- Instrumented (`connectedDebugAndroidTest`) and Maestro: local / release gate;
  optional CI job when an emulator runner is available.
- Deferred-track scenarios must not fail CI while modules are absent.

## Version

Shipping the suite scaffolding is a feature increment on the phone app
(`versionName` feature segment bump).
