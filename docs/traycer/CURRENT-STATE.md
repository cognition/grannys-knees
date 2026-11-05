# Current implementation truth

**Status:** Evidence sweep for N01, written against commit `906691f5156477e221ff0c3745148c2bde0a9f93`. Later the same working tree gained N02 device notes and an uncommitted T09 widget change. Those are recorded in [DEVICE-ACCEPTANCE.md](DEVICE-ACCEPTANCE.md), not in the task table below. `docs/traycer/BASELINE.md` stays the historical T00 audit and is not rewritten here.

This file records what the source, tests and GitHub Actions runs show. It is not a device acceptance report. Live Open-Meteo, emulator and Galaxy S24 FE checks are N02.

## How this was checked

| Check | Command or source | Result |
| --- | --- | --- |
| Local unit tests | `ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew :app:testDebugUnitTest --rerun-tasks --offline` | `BUILD SUCCESSFUL in 8s`. XML under `app/build/test-results/testDebugUnitTest/`: **74 tests, 0 failures, 0 errors, 0 skipped**. |
| Local debug APK | Not run in this sweep | **Not verified** locally this session. |
| GitHub Actions on this SHA | Run [41933917175](https://github.com/cognition/grannys-knees/actions/runs/41933917175), push of `906691f`, workflow `.github/workflows/android.yml` | `conclusion: success`. The workflow runs `./gradlew test assembleDebug --stacktrace` after installing `platforms;android-37.0` and `build-tools;36.0.0`. |
| Instrumented / emulator / logcat | Not run | **Not verified.** No `androidTest` source set exists. |
| Host | OpenJDK 25.0.4.1; `ANDROID_HOME=/home/rbrooker/Android/Sdk` | SDK directory is present. AVD names and KVM were not listed in N01. |

Per-class unit-test counts from the XML reports:

| Class | Tests |
| --- | --- |
| `CacheFreshnessTest` | 4 |
| `ForecastCacheStoreTest` | 2 |
| `ForecastRepositoryTest` | 12 |
| `OutlookMapperTest` | 9 |
| `OutlookRepositoryTest` | 5 |
| `PresentationScheduleTest` | 4 |
| `WeatherIndexScorerTest` | 7 |
| `AppDestinationTest` | 3 |
| `TodayScoreBandTest` | 3 |
| `TodayUiStateTest` | 5 |
| `OpenMeteoClientTest` | 8 |
| `OpenMeteoParserTest` | 8 |
| `WidgetModelLoaderTest` | 4 |

## Task assessment

Vocabulary: **implemented** (code matches the task and cited tests passed), **partial** (some of the task is in tree, the rest is missing or only untested), **unverified** (present, but this sweep did not run the accepting check), **absent** (no implementation).

| ID | Assessment | Report status | Why |
| --- | --- | --- | --- |
| T01 | implemented | verified for Gradle/CI; device launch unverified | App module, wrapper and workflow exist. CI on this SHA ran `test assembleDebug` successfully. Local unit tests were re-run. No emulator launch. |
| T02 | implemented | verified for in-repo fixtures; live Open-Meteo variable list not re-checked | Contract and fixtures are in tree and consumed by passing scorer tests. This sweep did not re-read Open-Meteo documentation. |
| T03 | implemented | implemented-unverified for a live response | Parser, client and fixture tests pass. No live HTTP call in this sweep. |
| T04 | implemented | verified by unit tests | Pure Kotlin scorer and daily low/high. Seven fixture tests passed. |
| T05 | partial | implemented-unverified | Compose Today screen and state copy exist. State helpers have unit tests. No Compose UI test and no emulator smoke. |
| T06 | partial | implemented-unverified | Repository wires fetch, file cache, score and Today. Twelve repository tests passed. **R0 device gate has not passed.** |
| T07 | partial | implemented-unverified | Outlook mapper and Today section exist. Mapper and repository outlook tests passed. No device graph or five-day screenshot. |
| T08 | partial | implemented-unverified | One compact current-index Glance widget. Loader tests match Today at a fixed instant. No on-device widget, reboot or resize check. |
| T09 | absent | planned | No hourly or five-day widget. R1 cannot pass. |
| T10–T14 | absent | planned | No Room schema, symptom contract, persistence or journal UI. |
| T15–T18 | absent | planned | No Wear OS module. |

## T01 — Android foundation

Evidence:

- `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
- `.github/workflows/android.yml`
- `app/src/main/java/com/example/grannysknees/MainActivity.kt`
- Application id and namespace: `com.example.grannysknees` (`app/build.gradle.kts`)
- `versionName` `0.3.0`, `versionCode` 3, `minSdk` 26, `compileSdk`/`targetSdk` 37
- Toolchain pins: Gradle wrapper 9.3.1, AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2026.09.00, Glance 1.2.0 (from `docs/traycer/ANDROID-BUILD.md` and `gradle/libs.versions.toml`)

`docs/traycer/ANDROID-BUILD.md` is stale. It still says the project has no weather client, scoring, persistence or widget. Those pieces are now in the tree. README “Current version” still points only at `index.html`. Those doc corrections are a separate change, not part of this sweep.

## T02 — Weather and index contract

Evidence:

- `docs/contracts/weather-index.md`
- `docs/contracts/fixtures/README.md`
- Fixtures: `nominal`, `missing-values`, `pressure-trend-boundary`, `dst-fall-back`, `dst-spring-forward` (each `.input.json` and `.expected.json`), plus `cache-freshness.expected.json`

`WeatherIndexScorerTest` loads those cases and compares canonical numbers with `BigDecimal`, without a floating-point tolerance.

## T03 — Open-Meteo client

Evidence:

- `app/src/main/java/com/example/grannysknees/weather/OpenMeteoClient.kt`
- `OpenMeteoModels.kt`, `OpenMeteoParser.kt`
- `OpenMeteoClientTest` (8) and `OpenMeteoParserTest` (8)
- Manifest permission `android.permission.INTERNET`

The client requests `temperature_2m`, `relative_humidity_2m` and `surface_pressure`, with `forecast_days >= 6`. Parser tests cover nulls preserved as null, unit mismatch, missing variables, malformed JSON, and 23-hour / 25-hour civil days. Transport failures are a distinct network result in `OpenMeteoClientTest` (`fetch never fabricates values when the transport fails`). A live Open-Meteo body was **not** fetched.

## T04 — index-v1 scorer

Evidence:

- `app/src/main/java/com/example/grannysknees/index/WeatherIndexScorer.kt`
- `WeatherIndexModels.kt` (`DailyIndex` is the hourly `indexScore` low/high for a local date)
- `WeatherIndexScorerTest` (7), including clamp, first-three-hours unavailable, and the five fixtures above

The scorer is experimental domain code. Today copy in `strings.xml` calls the index a playful indicator, not medical advice. No symptom rating is an input to this scorer.

## T05 — Today screen

Evidence:

- `app/src/main/java/com/example/grannysknees/ui/today/TodayScreen.kt`
- `TodayUiState.kt`, `TodayScoreBand.kt`, `TodayStateProvider.kt`
- `app/src/main/res/values/strings.xml`
- `TodayUiStateTest` (5), `TodayScoreBandTest` (3), `AppDestinationTest` (3)

Rendered states in `TodayScreen`:

- `Loading` — progress and “Fetching the forecast…”
- `Available` — score, band, caption, last-updated; stale badge when freshness is stale
- `Unavailable` / `EMPTY` — “No forecast just yet”
- `Unavailable` / `EXPIRED` — “Forecast expired”
- `Unavailable` / `INVALID_HOUR` — “Index unavailable this hour”
- Disclaimer — “Experimental — just for curiosity”

There is no `androidx.compose.ui:ui-test` dependency and no screenshot. Emulator smoke is **not verified**.

## T06 — Forecast wired to Today (R0 code, gate open)

Evidence:

- `ForecastRepository.kt` — `loadToday()` fetches, then `presentFromCache()`; `IOException` and `OpenMeteoParseException` keep the previous cache
- `ForecastCacheStore.kt`, `ForecastCacheEnvelope.kt`, `CacheFreshness.kt`, `PresentationSchedule.kt`, `TodayMapper.kt`, `WeatherToScoringAdapter.kt`
- Cache file name `forecast-cache.json`, opened from `MainActivity` and from `WidgetModelLoader`
- Fixed location `DefaultLocation`: Toronto, ON, 43.65, -79.38, `America/Toronto`
- `ForecastRepositoryTest` (12), `CacheFreshnessTest` (4), `ForecastCacheStoreTest` (2), `PresentationScheduleTest` (4)

Behaviour covered by those tests, still without a device:

- Successful fetch presents the current local hour’s score
- Network failure can still show a stale cache
- Expired cache hides the index
- Empty cache is unavailable, not a zero
- A missing current hour does not invent a score
- Hour rollover and the 60-minute / 180-minute freshness boundaries update the presented state
- Resume-style `presentFromCache()` re-reads the clock

`TodayRoute` shows `Loading` until `loadToday()` returns, then re-presents on `ON_RESUME` and on the presentation delay. That UI path has no test.

**R0 gate: not passed.** Acceptance still needs a reproducible install and a live forecast on a device or emulator (N02).

## T07 — Outlook on Today

Evidence:

- `OutlookMapper.kt` — current local calendar day curve; unavailable hours omitted; five rows are the next local days that have a daily index; extrema come from `DailyIndex`
- `OutlookUiState.kt`, `ui/outlook/OutlookSection.kt`
- `ForecastRepository.presentOutlookFromCache()`
- `OutlookMapperTest` (9), `OutlookRepositoryTest` (5)
- Copy in `app/src/main/res/values/strings_outlook.xml`

Mapper tests cover a 25-hour fall-back day, a 23-hour spring-forward day, tied extrema, omitted unavailable hours, a partial day, “local calendar day rather than a rolling 24 hours”, forecast-zone labels, and five-day rows that start tomorrow and skip days with no index.

`OutlookRepositoryTest` hides the outlook for expired, empty and unparseable caches, and still shows it for a stale cache.

The curve and five-day cards are Compose in `OutlookSection` / `FiveDayCard`. Drawing them on a screen is **not verified**.

## T08 — Current-index Glance widget

Evidence:

- Receiver: `GrannysKneesWidgetReceiver` in `widget/GrannysKneesWidget.kt`, registered in `AndroidManifest.xml` for `android.appwidget.action.APPWIDGET_UPDATE` only
- Provider: `GrannysKneesWidget` (`GlanceAppWidget`) loads `WidgetModelLoader` once per `provideGlance`, then `WidgetContentRenderer`
- Metadata: `res/xml/grannys_knees_widget_info.xml` and `res/xml-v31/grannys_knees_widget_info.xml`
  - `minWidth` 168dp, `minHeight` 72dp, `resizeMode` horizontal, `updatePeriodMillis` 1800000 (30 minutes)
  - API 31 file also sets `targetCellWidth` 2 and `targetCellHeight` 1
- `WidgetModelLoader` calls `ForecastRepository.presentFromCache` on `forecast-cache.json`. It does not call the network and does not score on its own
- `WidgetModelMapper.fromToday` copies an `Available` score through, and maps `Unavailable` and `Loading` to a score-free model
- `WidgetModelLoaderTest` (4): fresh and stale models equal the phone presentation at a fixed instant; expired and empty models expose no index
- Widget strings in `res/values/strings_widget.xml` (`widget_no_forecast`, stale badge, band labels)

Not in the tree, so not claimed:

- No second widget provider for the hourly curve or the five-day low/high
- No `WorkManager` class or dependency
- No `BOOT_COMPLETED` receiver. Whether the system re-sends `APPWIDGET_UPDATE` after reboot was not tested
- No widget screenshot, resize check or process-death check
- The widget cannot refresh the cache by itself. If Today has never fetched, the widget stays on the unavailable copy

## T09 — Expanded widgets

**Absent.** Search of `app/src/main` shows a single Glance receiver and a single app-widget XML description. Hourly and five-day presentation exist only inside the Today screen (`OutlookSection`), not as home-screen widgets.

R1 stays open until N02 device evidence and an N03 widget that reuses `ForecastRepository` / `OutlookMapper` without a second scorer.

## Symptom domain and Wear OS

No Kotlin type, Gradle module or Room schema mentions symptom observations. `docs/architecture.md` still describes the native journal and widgets as requirements rather than the current app; that sentence is now wrong for the weather widget and right for symptoms and Wear OS. No Wear module exists.

N04 may add `docs/contracts/symptom-observations.md` without colliding with widget or Today files. No symptom schema work is in progress in this tree.

## Docs that disagree with this tree

Leave these for a separate docs review. They were not edited in N01.

| File | Claim that is now wrong |
| --- | --- |
| `README.md` “Current version” | Only `index.html` is described as the current version. |
| `docs/traycer/ANDROID-BUILD.md` opening paragraph | Says there is no weather client, scoring, persistence or widget. |
| `docs/architecture.md` closing paragraph | Says native widgets are not yet implemented. The current-index widget is in tree. Journal, snapshots and Wear OS still are not. |
| `docs/traycer/EXECUTION-BACKLOG.md` | Header still says task statuses are “not started” until inspected. This file is that inspection for T01–T09. |
| `docs/traycer/BASELINE.md` | Historical T00 snapshot, including “no widget”. Keep it dated. Do not edit it to match today. |

Approved PNG artwork is still not in `assets/` (README asset status). SVG files there remain unapproved recreations.

## Blockers before R1

1. No install, launch, live Open-Meteo response, screenshot or logcat (N02).
2. T09 widget layouts are absent (N03).
3. Widget and Today agreement is unit-tested only through `WidgetModelMapper`, not on a home screen.
4. Application id remains the temporary `com.example.grannysknees`.

## N01 report

- **Status:** verified for this document’s evidence. The release gate did not pass.
- **Summary:** T01–T04 are in tree with passing unit tests. T05–T08 are in tree with passing JVM tests and no device proof. T09 and the symptom/Wear tasks are absent. CI on `906691f` succeeded.
- **Owned path:** `docs/traycer/CURRENT-STATE.md`
- **Commit / PR:** not committed in this sweep
- **Tests:** local `:app:testDebugUnitTest --rerun-tasks --offline` — 74 passed. `assembleDebug` not re-run locally. CI assemble on this SHA succeeded.
- **Limitations:** Gradle was invoked `--offline`. No emulator, no live forecast, no widget placement.
- **Next action:** N02 device acceptance, and N04 symptom contract on disjoint paths. N03 after that widget interface above is the baseline to extend.
- **Release gate:** no
