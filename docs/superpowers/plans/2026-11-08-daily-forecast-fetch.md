# Daily Forecast Fetch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Forecast HTTP at most once per local day unless pull-refresh or city change; remove background WorkManager pulls; GPS re-resolve only for GPS places.

**Architecture:** Gate network in `ForecastRepository.loadToday(forceNetwork)` using `DailyFetchStampStore` + cache match; persist `PlaceSource` on `SelectedCity`; Today pull-to-refresh calls force path; cancel periodic worker on app start.

**Tech Stack:** Kotlin, Compose Material3 `PullToRefreshBox`, JUnit4 unit tests, Canadian spelling in docs/UI.

## Global Constraints

- Canadian spelling in user-facing strings and docs.
- Version bump: feature digit on this change.
- Prefer specific exceptions (no broad `Exception` catch).
- Classes over free functions for new Python N/A; Kotlin follows existing style.
- No background forecast scheduling after this change.

---

### Task 1: PlaceSource + daily stamp + gated loadToday

**Files:**
- Create: `app/src/main/java/com/example/grannysknees/data/PlaceSource.kt`
- Create: `app/src/main/java/com/example/grannysknees/data/DailyFetchStampStore.kt`
- Create: `app/src/test/java/com/example/grannysknees/data/DailyFetchStampStoreTest.kt`
- Modify: `SelectedCity.kt`, `SelectedCityStore.kt`, `ForecastRepository.kt`, `MainActivity.kt`
- Modify/extend: `ForecastRepositoryTest.kt`, city store tests if any
- Modify: `OptionalGpsPlaceSource.kt`, `CitySearchScreen.kt` (tag Gps vs Search)

- [x] Failing tests for stamp gate (same-day second open skips HTTP; force hits HTTP)
- [x] Implement stamp + `loadToday(forceNetwork)`
- [x] Wire stamp file in MainActivity
- [x] Tests pass

### Task 2: Remove background forecast worker

**Files:**
- Modify: `GrannysKneesApplication.kt`, `ForecastRefreshWorker.kt`

- [x] Cancel unique work on start; stop scheduling
- [x] Keep worker class only if needed for cancel, or cancel + leave dormant

### Task 3: Today pull-to-refresh (option C)

**Files:**
- Modify: `TodayScreen.kt` (`TodayRoute` + `TodayScreen`)
- Strings if needed

- [x] Open path uses `forceNetwork = false`
- [x] Pull refreshes GPS-or-weather then presents
- [x] Resume/tabs remain cache-only
- [x] Bump version, assemble, install

---
