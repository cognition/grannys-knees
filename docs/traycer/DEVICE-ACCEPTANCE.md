# Device acceptance (N02)

**Status:** Emulator demonstration recorded. **Not** a physical Galaxy S24 FE run, and **not** an R1 pass. **HEAD:** `906691f5156477e221ff0c3745148c2bde0a9f93` plus uncommitted N01/N02 notes on `feature/n01-current-implementation-truth`.

Screenshots are under `docs/traycer/device-evidence/`. `uiautomator dump` could not run: an existing UiAutomation service was already registered (`com.callstack.agentdevice.snapshothelper`). Text below is what is visible in the screenshots, checked against the cache file pulled with `run-as`.

## Environment

| Check | Result |
| --- | --- |
| KVM | `/dev/kvm` is present (`crw-rw-rw-`, group `kvm`). |
| SDK | `ANDROID_HOME=/home/rbrooker/Android/Sdk`. Platform directory `android-37.0` is installed. |
| `adb devices -l` | `emulator-5554` `device` `product:sdk_gphone64_x86_64` `model:sdk_gphone64_x86_64` `transport_id:2`. No physical phone. |
| `emulator -list-avds` with default `~/.android/avd` | Empty. That directory has no AVDs. |
| `ANDROID_AVD_HOME=/home/rbrooker/.config/.android/avd emulator -list-avds` | `Galaxy_S24_FE` and `Medium_Phone`. |
| Running process | `qemu-system-x86_64 -avd Galaxy_S24_FE`, started from Android Studio. `ro.boot.qemu.avd_name=Galaxy_S24_FE`. |
| Image | `system-images/android-35/google_apis/x86_64/`, `ro.build.version.sdk=35`, release 15, 1080×2340, density 320. |
| App under test | `com.example.grannysknees` debug APK, `versionName` 0.3.0. Installed and launched on API 35 even though `compileSdk`/`targetSdk` are 37. |

The AVD is a Galaxy S24 FE **profile**, not the physical phone named in the cycle note.

## Commands that succeeded

```sh
ANDROID_HOME=/home/rbrooker/Android/Sdk ./gradlew assembleDebug --offline
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.example.grannysknees/.MainActivity
```

`assembleDebug --offline` finished `BUILD SUCCESSFUL in 1s`. Install printed `Success`. Logcat: `Displayed com.example.grannysknees/.MainActivity for user 0: +1s118ms` at 18:35:45, and again `+1s63ms` at 18:37:53 after a data clear. No app fatal exception in those launches.

## Live Open-Meteo

First launch wrote `files/forecast-cache.json` (10 275 bytes), pulled with `run-as`. Envelope: Toronto, ON, 43.65, −79.38, `America/Toronto`. Body: 387 hourly rows, `hourly_units` °C / % / hPa, `utc_offset_seconds` −18000, `generationtime_ms` 0.2608299255371094. Fetched at `2026-11-04T18:35:46.457-05:00`.

Today’s payload hours were 15:00–23:00 local, all with temperature, humidity and pressure present. The screen still said **6 of 24 hours scored**. That matches the scorer rule that the first three hours of a series have no pressure trend, so 15:00–17:00 are omitted. The curve starts at 18:00 and the gap copy says breaks are left blank, never filled with zeros.

Screenshot `01-live-today.png` (status clock 6:35):

- Score **4 / 10**, band Mild, caption “A little something in the air.”
- Last updated Nov 4, 6:35 p.m.
- Today’s index: low 3.4 at 19:00, high 4.2 at 22:00
- Five-day index low–high, caption “not temperature”: Thu 5 3.0–4.8, Fri 6 3.2–4.5, Sat 7 3.1–5.0, Sun 8 1.2–4.5, Mon 9 1.4–4.7
- Experimental disclaimer is on screen

A later fetch after clearing app data (`08-after-slow-fetch.png`, `generationtime_ms` 0.43451786041259766, fetched 18:37:54) showed the same current score **4**, with a different curve (low 3.2 at 21:00, high 4.3 at 22:00) and different five-day ranges (Thu 5 2.9–5.1 through Mon 9 1.2–4.1). Two different provider bodies a couple of minutes apart: this is a live response, not a fixture baked into the APK.

## States exercised on the emulator

| State | How | Screenshot | What the screen showed |
| --- | --- | --- | --- |
| Loading | `pm clear`, then `adb emu network speed gprs` and `network delay gprs`, launch, capture at 1.6 s, then `network speed full` and `network delay none` | `02-loading.png` | “Fetching the forecast…” and the Toronto picker note. No score. |
| Fresh live | Normal launch on validated Wi-Fi | `01-live-today.png`, `08-after-slow-fetch.png` | Score, curve, five-day ranges, timestamp. |
| Device zone ≠ forecast zone | `adb shell cmd alarm set-timezone America/Vancouver`, relaunch, then restored `America/Toronto` | `03-timezone-vancouver.png` | Status clock **3:36**. Last updated still **Nov 4, 6:36 p.m.** Hour labels stayed 19:00 and 22:00. Score stayed 4. |
| Offline, empty cache | `cmd connectivity airplane-mode enable`, `pm clear`, launch | `04-offline-empty.png` | “No forecast just yet”. Last updated “not yet”. No index and no outlook. |
| Stale | Airplane still on. Cache from the live body with `fetchedAtEpochMs` set 90 minutes back. Relaunch. | `05-stale.png` | Score 4 still shown. “Stale” and “Showing the last forecast we managed to fetch.” Last updated Nov 4, 5:07 p.m. Outlook still shown. |
| Expired | Same, `fetchedAt` 200 minutes back (policy expires after 180). | `06-expired.png` | “Forecast expired”. Last updated Nov 4, 3:17 p.m. No score and no curve. |
| Missing hour | Airplane on. Fresh `fetchedAt`, `rawBody` = `docs/contracts/fixtures/nominal.input.json` (hours on 9–10 Nov, not the evening of 4 Nov). | `07-invalid-hour.png` | “Index unavailable this hour” and “we won't invent a score.” Today: “No hours scored for today yet.” Thu 5–Mon 9 each say **No index**. |

There is no separate error toast. A failed fetch with an empty cache is the offline card. A failed fetch with a usable cache keeps that cache and, once it is older than 60 minutes, labels it stale. That is the network-error behaviour on screen.

The invalid-hour fixture does contain some 9 Nov hours, but they are the first three hours of that series, which the scorer will not score, and 10 Nov is outside the five local days after 4 Nov. “No index” on every row is the honest result, not a zero.

Airplane mode was turned back off (`cmd connectivity airplane-mode` → `disabled`). Timezone was set back to `America/Toronto`. The final cache is the second live fetch above.

## Not verified

- No physical handset. `adb devices -l` listed only `emulator-5554`.
- Home-screen widget was not placed. AppWidgetService logged the receiver, and a force-stop cancelled its broadcasts. No widget screenshot, resize, or reboot check.
- `Medium_Phone` was not booted.
- Compile/target SDK 37 was not run on an API 37 image. This debug build did install and draw on API 35.

## N02 report

- **Status:** verified for the Today screen on the Galaxy S24 FE emulator profile. R1 gate: no.
- **Summary:** The debug app installs, fetches a real Open-Meteo forecast for Toronto, and shows the current index, local-day curve and five-day index ranges. Empty, stale, expired and missing-hour states stay free of invented scores. Forecast-zone labels stayed on Toronto time while the device clock was on Pacific time.
- **Owned path:** `docs/traycer/DEVICE-ACCEPTANCE.md` and `docs/traycer/device-evidence/`
- **Commit / PR:** not committed
- **Tests:** local `assembleDebug --offline` succeeded. Unit tests were re-run in N01 (74 passed) and not repeated here. No instrumented test source set.
- **Limitations:** Emulator only. Widget not on the home screen. Loading was captured by slowing the emulator network, not by a timed test.
- **Next action:** N03 widget layouts, then place them on this same emulator after Today checks are done. N04 can proceed on the contract files only.
- **Release gate:** R0 phone demonstration on this emulator: yes, with the limits above. R1: no. The widget follow-up is below and still does not close R1, because the hourly size and a reboot were not shown.

## Widget follow-up (N03, same emulator)

After the Today checks, the working tree’s widget was installed and placed from the launcher picker (search “Granny”, Add). `versionName` on that build is `0.3.1.0`.

| Size | Screenshot | What it showed |
| --- | --- | --- |
| Default 2×1, just after Add | `09-widget-current.png` | Toronto, ON, **4 Mild**, “Updated Nov 4, 6:37 p.m.” Same score and timestamp as `08-after-slow-fetch.png`. |
| Resized tall | `10-widget-resized.png` | **4 Mild** plus Thu 5 2.9–5.1, Fri 6 3.1–4.5, Sat 7 3.2–4.6, Sun 8 1.3–3.6, Mon 9 1.2–4.1. Those ranges match the second live Today screen exactly. |

A further drag meant to shorten the widget into the hourly layout opened the app drawer instead. There is no hourly-size screenshot. `WidgetLayoutPicker` unit test covers the width breakpoint. Reboot was not run; the emulator was left on the home screen with the tall widget still placed.

Unit tests after the widget change: **76 tests, 0 failures**, including `WidgetModelLoaderTest` (6). `assembleDebug --offline` succeeded and the APK installed with `Success`.
