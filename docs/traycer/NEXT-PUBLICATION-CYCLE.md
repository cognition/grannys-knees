# Next execution cycle: Canadian city forecast

**Status:** Proposed / not executed. **Base:** `origin/main` at `2fe7262ae6ccb8401d14bf98d61ffb42d883850f` (2026-11-05). This is a handoff, **not** evidence of passing builds, CI, or emulator tests.

Parent decisions, already approved and not open for redesign in this cycle:

- [Release plan](../release/google-play/RELEASE-PLAN.md)
- [Publication checklist](../release/google-play/PUBLICATION-CHECKLIST.md)
- [Publication backlog](PUBLICATION-BACKLOG.md) (PUB00–PUB10)
- Weather contract §2 geocoding endpoint in [weather-index.md](../contracts/weather-index.md)

Checked against the live geocoding API on 2026-11-05: `GET https://geocoding-api.open-meteo.com/v1/search?name=London&count=5&language=en&format=json&countryCode=CA` returns `results[]` with `name`, `latitude`, `longitude`, `timezone`, `country_code`, `admin1` (Ontario for London). An empty search omits `results`.

## Why this cycle

`32d8b9e` put the hourly/five-day widget, emulator notes, and the symptom contract on GitHub. The publication track says 1.0 is a free, ad-free, Canada-only weather index. The app still requests a fixed Toronto forecast (`DefaultLocation`, `43.65`, `-79.38`) until something else is chosen. Play upload, a new application ID, onboarding, and store artwork wait on owner decisions and are not this cycle.

## Start from a clean published tree

Implement on a new branch from `origin/main` (`2fe7262`). Do not implement on `feature/brand-logo-colours`.

That branch has uncommitted work which this cycle does **not** take:

- Foreground GPS (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `AndroidForecastPlaceSource`). The release plan says 1.0 must not require a location permission. Leave that work uncommitted.
- Launcher and chart colours generated from `assets/logo-dark.svg` and `assets/logo-light.svg`. Those SVGs are unapproved recreations. Do not treat them as the Play icon or store art.

`docs/traycer/BASELINE.md` stays the dated T00 audit. Do not rewrite it.

## In this cycle

| ID | Publication item | Outcome |
| --- | --- | --- |
| C01 | PUB00 | Evidence matrix for `2fe7262`. No app code. |
| C02 | PUB02 | Search and save a Canadian city. Forecast and widget use that city. |
| C03 | PUB04, known defect only | Hourly widget shows the current hour and the hours after it. Emulator proof for that size, plus reboot if the emulator can be rebooted. |
| C04 | PUB05, inventory only | Data-flow note for the city-search app. No privacy-policy URL and no Play form. |

## Not in this cycle

- PUB01: Play Console account, public contact, permanent `applicationId`. Leave `com.example.grannysknees` unchanged.
- PUB03: first-run onboarding and About the Index.
- PUB06–PUB10: approved PNGs, signing, AAB, closed testing, production.
- Symptom Room/UI, Wear OS, accounts, ads, analytics.
- GPS as a location source. A saved city is the only 1.0 location.
- A second scoring implementation. Keep `WeatherIndexScorer`.

## C01: Publication truth sweep (P0)

**Dependencies:** none. **Suggested owner:** Claude; independent Codex review.
**Owned path:** `docs/traycer/PUBLICATION-STATUS.md` (new). Do not edit application source.

Inspect `origin/main` at `2fe7262`, not a dirty worktree. For PUB00–PUB10 and for T07, T08, and T09, mark each item `not started`, `partial`, `implemented-unverified`, `verified`, or `blocked`. Cite files, the command that was run, and the result. Separate owner/Console blockers from code blockers.

Re-run unit tests from a clean checkout of `2fe7262`:

```sh
ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew :app:testDebugUnitTest --offline
```

Record the test count from the XML under `app/build/test-results/testDebugUnitTest/`. A Gradle `FROM-CACHE` line is not a new run; say so if the task was not re-executed. Do not claim device acceptance from `DEVICE-ACCEPTANCE.md` without saying it was an API 35 Galaxy S24 FE **emulator profile**, and that the hourly widget size and reboot were not shown.

**Acceptance:** A reviewer can tell what is already on `main` and what this cycle still has to build. No feature code in C01.

## C02: Canadian city selection (P0)

**Dependencies:** C01. **Suggested owner:** Codex implementation, Cursor emulator check.
**Branch:** `feature/pub02-canadian-city` from `origin/main`.

### Behaviour

1. First launch does not fetch Toronto. The Today screen asks for a Canadian city.
2. The user types a name. The app calls the contract geocoding URL with `language=en`, `format=json`, `count=10`, and `countryCode=CA`. No API key.
3. Each row shows `name`, `admin1`, and `Canada`, so London, Ontario is distinct from any other London the API returns. Rows without `latitude`, `longitude`, `timezone`, `name`, or `admin1` are dropped, not shown as blanks.
4. Choosing a row saves it and requests the forecast with that latitude, longitude, and IANA `timezone`. The label is `Name, Admin1` (for example `London, Ontario`).
5. The choice survives process death. Restart shows the same city and does not ask again.
6. The user can change the city. The forecast request uses the new coordinates.
7. If the saved cache belongs to a different latitude/longitude than the saved city, the UI shows the saved city and an unavailable forecast. It must not show the previous city's score under the new name.
8. Network or parse failure while searching shows an error and an empty result list. It does not invent cities. A failed refresh keeps a cache only when that cache is for the **same** city.
9. The widget reads the saved city and the same cache. It does not search and it does not request location permission.
10. Copy is Canadian English. The index remains an experimental weather index, not a medical reading.

### Files

- Create: `app/src/main/java/com/example/grannysknees/weather/OpenMeteoGeocodingClient.kt` — builds the search URL and parses `results`. Use the existing `HttpTransport`. Missing `results` means no matches.
- Create: `app/src/main/java/com/example/grannysknees/data/SelectedCity.kt` — `label`, `latitude`, `longitude`, `timezone`, `admin1`, `countryCode`.
- Create: `app/src/main/java/com/example/grannysknees/data/SelectedCityStore.kt` — one JSON file beside the forecast cache, same read/write style as `ForecastCacheStore`. No DataStore dependency.
- Modify: `ForecastRepository.kt` — `loadToday` uses the saved city. Refuse to present a cache whose coordinates do not match the saved city.
- Modify: `TodayScreen.kt` / `TodayRoute` — search, results, and change-city. Remove the copy that says the Toronto location is temporary.
- Modify: `DefaultLocation.kt` — keep the Toronto constants only as an explicit sample for tests and previews, not as the silent production place.
- Modify: `strings.xml` — search field, no-results, search failure, change city.
- Test: `app/src/test/java/com/example/grannysknees/weather/OpenMeteoGeocodingClientTest.kt` — London, Ontario is kept; a result missing `timezone` is dropped; a body without `results` is an empty list; `countryCode=CA` is on the URL.
- Test: extend `ForecastRepositoryTest.kt` — a saved Ottawa city (`45.4215`, `-75.6972`, `America/Toronto`) is the request; a Toronto cache is not presented for that city.
- Version: `versionName` `0.3.2.0`, `versionCode` `5` (feature bump from `0.3.1.0` / `4` on `origin/main`).

Do not add location permissions to `AndroidManifest.xml`.

### Emulator check

Host SDK: `ANDROID_HOME=/home/rbrooker/Android/Sdk`. AVD home, when the default `~/.android/avd` is empty: `ANDROID_AVD_HOME=/home/rbrooker/.config/.android/avd`. Device seen previously: `emulator-5554`, AVD `Galaxy_S24_FE`, API 35. That profile is not a physical phone.

```sh
ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew :app:testDebugUnitTest assembleDebug --offline
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.example.grannysknees/.MainActivity
```

Show, with screenshots: the search prompt before any forecast; results for `London` that include Ontario; the live index after selection; the same city after force-stop and relaunch; a second city whose score is not the previous city's. `uiautomator dump` has failed on this emulator because `com.callstack.agentdevice.snapshothelper` holds UiAutomation. Use `screencap` if dump fails.

**Acceptance:** Unit tests pass, and the emulator shows a chosen Canadian city end to end. Gradle alone is not acceptance.

## C03: Hourly widget window (P1)

**Dependencies:** none for the code; emulator time after C02 if both touch the same running emulator. **Suggested owner:** Codex, then Cursor for the screenshot.
**Owned paths:** `WidgetUiModel.kt`, `GrannysKneesWidget.kt`, `WidgetModelLoaderTest.kt`, `strings_widget.xml`. Do not change scoring or the forecast client.

Codex review on pull request #12 (`discussion_r4482759701`) is confirmed in the source. `hoursFrom()` copies every scored local-day point from midnight. `HourlyContent` draws them in a non-scrolling `Column`. `WidgetLayoutPicker` uses the hourly layout only when width is at least `240.dp` and height is below `220.dp`. At `220.dp` the layout becomes five-day, so making the cell taller never reveals the clipped hours. Later in the day the visible rows can be morning hours only.

Change the hourly layout so it shows the current local hour when that hour has a score, then the following scored hours, in order, and stops at the number of rows that fit the hourly cell. Do not draw a chart. Do not list hours before the current hour in this layout. If the current hour has no score, start at the next scored hour. An empty list still uses `widget_no_hours`. The five-day layout stays the five local days after today. The compact layout stays the current score only.

Bump the fix iteration after C02 has landed: `versionName` `0.3.2.1`, `versionCode` `6`. If C03 merges before C02, bump from `0.3.1.0` to `0.3.1.1` and `versionCode` `5` instead.

**Tests:** a day whose scored hours run `00:00` through `23:00`, with "now" at `18:00`, does not put `00:00` in the hourly model. The current hour is first. A missing current hour starts at the next scored hour. The five-day rows are unchanged.

**Emulator:** place the widget, resize to the wide short cell, and screenshot it. The visible hours must not be only morning hours when the device clock is afternoon or evening. Then reboot the emulator and confirm the widget still shows the same city and score. If reboot would disrupt other work on that emulator, write that blocker in the evidence file and do not call R1 accepted. Add the screenshot under `docs/traycer/device-evidence/` and a short note in `DEVICE-ACCEPTANCE.md` that says what was and was not shown.

**Acceptance:** The unit test fails on the old midnight-first list and passes on the window. One emulator screenshot shows the hourly cell. R1 is accepted only if reboot was actually shown.

## C04: Data-flow inventory (P1)

**Dependencies:** C02's behaviour is settled, so the note describes the search that will ship. **Suggested owner:** Claude. **Owned path:** `docs/release/google-play/PRIVACY-DATA-FLOWS.md` (new). No application code.

Record, in Canadian English:

- Permissions in the manifest after C02. Location permissions must be absent.
- Outbound calls: Open-Meteo forecast and Open-Meteo geocoding. No account, no ad SDK, no crash SDK unless the Gradle files show one.
- What leaves the device: city query text, and the selected latitude, longitude, and timezone on the forecast request. Say that Open-Meteo is the recipient.
- What stays on the device: `SelectedCityStore` file and `forecast-cache.json`.
- What this document does not do: it is not a public privacy policy, not a Data safety submission, and not a Health apps declaration.

**Acceptance:** An owner can review the note without being told that "no data is collected."

## Scheduling

1. C01 first, from `origin/main`.
2. C02 and C04 may proceed together once C02's request and storage shapes above are accepted. C04 does not edit Kotlin.
3. C03 may proceed in parallel with C02 only while it stays inside the widget files. Integrate so the widget displays the saved city from C02 before the emulator screenshot.
4. One branch per task. Do not commit directly to `main`.

## Owner decisions still required before a later cycle

These block PUB01 and everything after PUB06. This cycle must not invent the answers.

- Permanent application ID, using a domain the owner controls.
- Whether the personal Play account must run 12 opted-in testers for 14 days.
- Public support address and where the privacy policy will be hosted.
- Approved dark and light PNG artwork. The SVGs in `assets/` are not that artwork.

## Standard report

For each task: status (`planned`, `in progress`, `blocked`, `implemented-unverified`, `verified`), summary in Canadian English, owned paths, commit or pull request, exact commands and results, screenshots where this cycle asks for them, limitations, and whether the task's acceptance line passed.
