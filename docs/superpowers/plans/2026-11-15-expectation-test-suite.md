# Expectation Test Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a user- and design-expectation test suite for Granny's Knees with freshness honesty, phone automation now, and deferred widget/Wear tracks.

**Architecture:** Catalogue (UE/DE/PE) maps to L1 JVM, L2 Compose UI Test, L3 Maestro, L4 acceptance. L1/L2 use fixed clocks and fixtures; live paths assert shape, not pinned scores.

**Tech Stack:** JUnit4, Robolectric (existing), AndroidX Compose UI Test, Maestro YAML, Gradle CI.

## Global Constraints

- Package: `com.aetherealmind.grannysknees`
- Cache: fresh ≤60m, stale ≤180m, then expired ([`CachePolicy`](../../../app/src/main/java/com/aetherealmind/grannysknees/data/CacheFreshness.kt))
- Canadian spelling in user-facing docs
- Feature version bump when suite scaffolding ships
- Widget/Wear scenarios marked DEFERRED; must not break CI
- Never pin live Open-Meteo scores as expected values

---

### Task 1: Catalogue and acceptance docs

**Files:**
- Create: `docs/qa/expectation-catalogue.md`
- Create: `docs/qa/acceptance-matrix.md`
- Create: `docs/qa/fixture-freshness.md`
- Modify: `docs/contracts/fixtures/README.md` (link freshness policy)

- [ ] **Step 1:** Write catalogue with UE/DE/PE/W/R IDs and primary layers
- [ ] **Step 2:** Write acceptance matrix evolving DEVICE-ACCEPTANCE checks
- [ ] **Step 3:** Document fixture clock alignment and live-pull rules

### Task 2: L1 freshness anti–false-green

**Files:**
- Create: `app/src/test/java/com/aetherealmind/grannysknees/data/TodayMapperFreshnessTest.kt`
- Create: `app/src/test/java/com/aetherealmind/grannysknees/ui/theme/LakeTealTokenTest.kt`

- [ ] **Step 1:** TodayMapper tests: FRESH shows score; STALE shows score+stale; EXPIRED hides score; INVALID_HOUR hides score even if another hour scored
- [ ] **Step 2:** Theme token test: Material light primary = lake teal `#0E7C86`; dark primary = mint `#7FE0E8`; secondary rough coral family (via Compose + Robolectric or androidTest)
- [ ] **Step 3:** Run `./gradlew test`

### Task 3: L2 Compose UI Test scaffolding

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/androidTest/java/.../TodayHonestyInstrumentedTest.kt`
- Create: `app/src/androidTest/java/.../JournalPartialReportInstrumentedTest.kt` (when harness ready)

- [ ] **Step 1:** Add `androidTestImplementation` Compose UI test + Espresso + JUnit
- [ ] **Step 2:** Today honesty: compose `TodayScreen` with SampleTodayStates; assert stale badge/copy; expired title; offline title; fresh shows score without Stale
- [ ] **Step 3:** Run on emulator when available: `./gradlew connectedDebugAndroidTest`

### Task 4: L3 Maestro smokes

**Files:**
- Create: `maestro/README.md`
- Create: `maestro/flows/smoke-first-launch.yaml`
- Create: `maestro/flows/smoke-city-switch.yaml`
- Create: `maestro/flows/smoke-dark-large-type.yaml`

- [ ] **Step 1:** Flows assert allowed band words / city text / disclaimer; no exact score
- [ ] **Step 2:** Document `maestro test maestro/flows`

### Task 5: CI and version

**Files:**
- Modify: `.github/workflows/android.yml` (document optional instrumented; keep unit tests required)
- Modify: `app/build.gradle.kts` versionName / versionCode

- [ ] **Step 1:** Bump feature version
- [ ] **Step 2:** Ensure `./gradlew test assembleDebug` stays green

---
