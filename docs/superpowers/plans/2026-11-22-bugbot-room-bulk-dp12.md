# Bugbot Room / Bulk Delete / dP12 Window Fixes

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the three Bugbot findings on `release/rc2`: shared Room journal DB, transactional bulk measurement deletes, and dP12 scoring that only requires pressure endpoints (not a contiguous 13-hour map).

**Architecture:** Hold one `JournalDatabase` on `GrannysKneesApplication` and resolve `JournalRepository` from it in Journal / History / Settings. Move bulk removes into `JournalRepository` wrapped in `RoomDatabase.runInTransaction` so a mid-batch failure rolls back. Replace contiguous pressure `window(13)` with an endpoint lookup for dP12 while keeping the 4-hour slope window contiguous.

**Tech Stack:** Kotlin, Jetpack Room, Compose, Robolectric/JUnit, slope-index contract.

## Global Constraints

- Canadian spelling in user-facing strings.
- Version bump: fix digit only (`rc2.3.23` → `rc2.3.24`, `versionCode` 20323 → 20324).
- No broad `Exception` catches (Pylint W0718 analogue).
- TDD: failing test first for each behaviour change.
- Do not change CSV FileProvider / TLS hardening from the security commit.

## File map

| File | Responsibility |
|------|----------------|
| `GrannysKneesApplication.kt` | Lazy singleton `JournalDatabase` + factory for `JournalRepository` |
| `JournalRepository.kt` | Accept DB (or dao+db); add transactional `deleteMeasurements` |
| `JournalScreens.kt` | Use app singleton; call batch delete API |
| `AboutScreens.kt` | Use app singleton for Settings wipe/export |
| `JournalRoomTest.kt` | Tests for singleton wiring helper + transactional bulk delete |
| `WeatherIndexScorer.kt` | Pressure endpoints only for dP12 |
| `WeatherIndexScorerTest.kt` | Missing middle *epoch* still scores when endpoints exist |
| `docs/wiki/Calculation-logic.md` | Align missing-data wording with endpoint-only dP12 |
| `app/build.gradle.kts` | Version bump |

---

### Task 1: Shared Room journal database

**Files:** `GrannysKneesApplication.kt`, `JournalScreens.kt`, `AboutScreens.kt`, `JournalRepository.kt` (ctor if needed), `JournalRoomTest.kt`

- [x] Add `journalDatabase` lazy property on `GrannysKneesApplication` building `"journal.db"` once
- [x] Add `journalRepository(): JournalRepository` using that DB’s DAO
- [x] Replace the three `Room.databaseBuilder(...).build()` call sites with `(context.applicationContext as GrannysKneesApplication).journalRepository()`
- [x] Keep optional/test injection: screens may still accept an injected repository where useful; default path must use the singleton
- [x] Update `JournalRoomTest.repository()` to pass through `JournalDatabase` if the repository ctor changes
- [x] Verify unit tests still pass for journal Room tests

### Task 2: Transactional bulk measurement delete

**Files:** `JournalRepository.kt`, `JournalScreens.kt`, `JournalRoomTest.kt`

- [x] Write failing test: two measurements; second delete would fail (stale id) — assert first measurement still present after failed batch (transaction rolled back)
- [x] Add `deleteMeasurements(targets, deletedAt)` (or similar) on `JournalRepository` that runs inside `database.runInTransaction { }`, tracks live observation ids like HistoryScreen today, and throws a dedicated non-broad error (e.g. `IllegalStateException` or small typed exception) on any null delete so Room rolls back
- [x] Change `HistoryScreen.removeMeasurements` to call the batch API once; on failure show existing bulk/single failure strings without partial UI success
- [x] Run `JournalRoomTest` and confirm green

### Task 3: dP12 endpoint lookup without contiguous middle epochs

**Files:** `WeatherIndexScorer.kt`, `WeatherIndexScorerTest.kt`, `docs/wiki/Calculation-logic.md`

- [x] Write failing test: 13-hour span with a *missing* middle epoch key (not just null pressure); endpoints have pressure; temps/humidity present for slope hours — expect `AVAILABLE` and dP12 = last − first
- [x] Replace `pressureWindow = window(..., PRESSURE_POINTS, ...)` with endpoint fetch: `byEpoch[end]` and `byEpoch[end - 12h]`; set `pressuresReady` from those two samples’ `surfacePressureHpa`
- [x] Update `buildReasons` so `REASON_PRESSURE_WINDOW` means missing endpoint samples, not missing middle hours; keep null-endpoint reasons
- [x] Update wiki “Missing or invalid data” paragraph to match endpoint-only dP12 (slope window still contiguous)
- [x] Run `WeatherIndexScorerTest`

### Task 4: Version bump and verify

**Files:** `app/build.gradle.kts`

- [x] Bump to `rc2.3.24` / `20324`
- [x] Run `./gradlew :app:testDebugUnitTest`
- [x] Mark plan checkboxes done in this file

## Out of scope

- SQLCipher / encrypting the journal at rest
- Certificate pinning for Open-Meteo
- Changing append-only tombstone semantics of a single `deleteMeasurement`
