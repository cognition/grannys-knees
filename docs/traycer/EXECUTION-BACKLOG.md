# Traycer execution backlog

**How to use:** Each task is an agent-sized unit of work. Start with T00 and T01. Tasks with satisfied prerequisites and disjoint file ownership may run in parallel. All statuses below are **not started** until the repository is inspected and evidence is recorded.

| ID | Priority | Dependencies | Suggested lead | Deliverable and acceptance test |
| --- | --- | --- | --- | --- |
| T00 | P0 | none | Claude + Codex reviewer | Audit repo actual state, buildability, files and blockers. Produce evidence-backed baseline; no implementation claims without checks. |
| T01 | P0 | T00 | Codex | Native Android Gradle/Compose skeleton, build instructions and unit-test runner; `assembleDebug` and tests pass in configured environment. |
| T02 | P0 | T00 | Claude | Define versioned hourly weather, index and cache contracts, scoring uncertainty and fixture cases; independent review accepts contract. |
| T03 | P0 | T01,T02 | Codex | Open-Meteo hourly client and timezone-aware models with parsing, network-error and fixture tests. |
| T04 | P0 | T02 | Codex | Pure Kotlin 0–10 experimental scorer, daily min/max from hourly values, edge-case tests; no medical claim. |
| T05 | P0 | T01,T02 | Cursor/Codex | Compose Today screen with current score, location, timestamp, stale state and disclaimer; emulator smoke test. |
| T06 | P0 | T03,T04,T05 | Codex | Connect real forecast to Today; offline cache and invalid-data behaviour verified. **R0 gate**. |
| T07 | P1 | T06 | Cursor | 24-hour index graph and five-day low/high index view; timezone and extrema tests. |
| T08 | P1 | T06 | Codex | Glance current-index widget and refresh/freshness; match phone score. |
| T09 | P1 | T07,T08 | Codex/Cursor | Expanded hourly and five-day widgets, lifecycle/refresh tests. **R1 gate**. |
| T10 | P1 | T01 | Claude | Define Room observation/symptom/snapshot schema, append-only event semantics, explicit confirmation, migration strategy. |
| T11 | P1 | T10 | Codex | Room persistence with partial updates, confirmed-unchanged state and tests proving untouched values absent. |
| T12 | P1 | T11 | Cursor | Symptom quick-log UI, body part/side/symptom, 0–10 controls, optional confirmation and reset; UI tests. |
| T13 | P1 | T03,T11 | Codex | Linked weather/device snapshot, per-reading provenance and unavailable states; permission tests. |
| T14 | P1 | T11,T13 | Codex | Local history, export/delete and observation timestamps; round-trip tests. **R2 gate**. |
| T15 | P2 | T06 | Claude/Codex | Wear OS tile architecture and current-index tile; independent emulator/device check. |
| T16 | P2 | T12,T15 | Codex | Watch partial symptom entry, offline queue, idempotent sync, duplicate and conflict tests. |
| T17 | P2 | T14 | OpenRouter + Codex reviewer | Descriptive historical associations, sample-size safeguards and correlation disclaimer; test against known fixtures. |
| T18 | P2 | T09,T14,T16 | Cursor/Codex | Accessibility, dark/light approved branding, privacy disclosures, release build, regression suite. **R3 gate**. |

## First execution batch: exact handoffs

### T00: Repository reality check
**Owner:** Claude, independent Codex review.
**Owned paths:** `docs/traycer/BASELINE.md` only.
**Do:** inspect all tracked files; determine if Android source/Gradle/CI exist; run available tests or document why not; identify incorrect SVG branding and missing approved PNG assets; identify HTML-only features and any unsupported Open-Meteo variables.
**Accept:** a concise table of implemented/partial/not implemented, evidence by file, exact commands and results, and blockers. No invented build result.

### T01: Native Android foundation
**Owner:** Codex, after T00.
**Owned paths:** Android build and source tree, `.github/workflows/android.yml`, `docs/traycer/ANDROID-BUILD.md`.
**Do:** choose compatible, pinned AGP/Kotlin/Compose/Gradle versions; create minimal Compose activity and navigation shell; configure unit tests, CI, package name and build instructions. Keep the existing HTML prototype intact.
**Accept:** `./gradlew test assembleDebug` succeeds where Android SDK exists; CI checks succeed; clean checkout build documented. Report environmental blockers honestly.

### T02: Weather/index contracts
**Owner:** Claude, Codex reviewer, may run in parallel with T01 after T00.
**Owned paths:** `docs/contracts/weather-index.md`, `docs/contracts/fixtures/`.
**Do:** specify hourly Open-Meteo inputs, timestamp/timezone, pressure trends, missing values, forecast freshness, index 0–10 interpretation, versioned scoring assumptions, daily low/high from hourly scores, fixture examples. Verify supported API variables against authoritative documentation.
**Accept:** deterministic fixture cases and edge conditions, documented uncertainty, and no unsupported medical claims. No production scorer implementation in this task.

## Integration and scheduling

- **Batch A:** T00 alone, verify baseline.
- **Batch B:** T01 and T02 in parallel, separate owned paths.
- **Batch C:** T03, T04 and T05 after contracts/foundation, avoid shared Gradle and app-shell edits.
- **Batch D:** T06 integration and R0 gate, then T07/T08.
- **Batch E:** T09 R1 gate; T10 may start after T01 if file ownership remains isolated.
- **Batch F:** T11–T14 and R2 gate.
- **Batch G:** T15–T18 and R3 gate.

Never parallelize tasks just because multiple models are available. Avoid integration bottlenecks by assigning one integrator per batch.

## Definition of done
Code exists; build succeeds; applicable unit/UI tests pass; data contract preserved; demonstration reproducible; reviewer signs off; documentation reflects reality; limitations and incomplete hardware checks explicitly disclosed.

## Deferred decisions
- Final package/application ID and Play Store publisher identity.
- Approved PNG assets must be committed before app branding or README references can be considered complete.
- Exact experimental index formula and research citations must be reviewed before treating scoring as stable.
- Sensor scope and permissions need device-availability and privacy review.
- Wear OS testing hardware and supported minimum API levels.
