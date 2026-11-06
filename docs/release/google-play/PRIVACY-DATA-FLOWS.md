# Privacy data-flow and permission audit — Android

**Status:** Preliminary inventory, updated 2026-11-05 for the city, journal, and watch code. Not a public privacy policy and not a Play Data safety submission.
**Evidence:** `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `MainActivity.kt`, `weather/OpenMeteoClient.kt`, `weather/OpenMeteoGeocodingClient.kt`, `data/SelectedCityStore.kt`, `journal/`, `watch/WatchSync.kt`.

| Data / flow | Current observed implementation | Open verification |
|---|---|---|
| City search | `OpenMeteoGeocodingClient` sends the typed name to HTTPS `geocoding-api.open-meteo.com/v1/search` with `countryCode=CA` | Confirm Open-Meteo terms, production use, and attribution |
| Weather coordinates/timezone | The saved city's latitude, longitude, and IANA timezone go to HTTPS `api.open-meteo.com/v1/forecast` | Review provider processing terms and Play Data safety implications |
| Weather measurements | Requests temperature, relative humidity, surface pressure, 6+ forecast days and past hours | Same as the forecast request |
| IP and connection metadata | HTTPS requests reach Open-Meteo; the provider may observe IP and request metadata | Do not answer "no data collected" in Play Console |
| Saved city | `filesDir/selected-city.json` | Decide backup behaviour. `android:allowBackup="true"` is still set |
| Local weather cache | `filesDir/forecast-cache.json`. A cache for another city is not shown as the current score | Verify retention and removal |
| Optional GPS | `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` only after "Use this phone's location". Declining them leaves city search working. No background location permission | Confirm the merged release manifest has no background location permission |
| Symptom journal | Room `journal.db` on the device. Export uses the Android share sheet. Delete everything clears the local tables. No account and no cloud copy | Confirm backup does not copy the journal unless the owner wants that |
| Watch | The phone writes the current index for the tile and accepts one partial report. A repeated event id is ignored. Traffic stays on the Wear data layer | A paired watch was not available in this pass |
| Advertising/analytics | No ad or analytics SDK is declared in the app Gradle file | Scan the release dependency tree and the signed AAB |
| Crash reporting | No dedicated SDK is declared | Confirm Play and OS diagnostics before a Data safety answer |
| App widget | Glance reads the same cache as Today and does not call the network itself | Wide short cell was not captured on the emulator |

## What stays on the device

- `selected-city.json`
- `forecast-cache.json`
- `journal.db`
- `onboarding-seen`
- `watch-index.txt`

## Product privacy principle (owner clarification)

**The requirement is no collection by the Granny's Knees developer**, not zero outbound traffic. The phone is allowed to contact an external weather provider directly to retrieve a forecast. The weather provider receives requested coordinates/timezone and technical network metadata; that traffic is not sent to the developer. No developer-operated accounts, backend collection, advertising or analytics are planned. All future symptom records must stay on the phone or paired watch. Validate this against the signed build before making public guarantees.

A new in-app **Your privacy** page is linked from Today, with plain-language descriptions and a backup caveat. The page is informational, not a substitute for a publicly hosted full policy. Android currently sets `allowBackup=true`, so local-only backup claims remain blocked pending a technical decision.

## Actions required before policy publication

1. Inspect the full release manifest and dependency tree, including transitive libraries.
2. Decide the Android backup policy. `allowBackup=true` means the app must not claim that storage is exclusively on the device.
3. Confirm Open-Meteo licensing, attribution, and privacy documentation.
4. Establish a support contact and a publicly hosted HTTPS privacy policy. Do not publish personal contact information without approval.
5. Confirm there are no medical claims, and complete the Health apps declaration if Console asks for it.
6. Record owner sign-off. Permanent application ID, privacy-policy host, and the 12-tester question are still owner decisions.

## Evidence checklist

- [ ] Final merged manifest
- [ ] Release dependency/SDK inventory
- [ ] Network endpoint list with sample requests
- [ ] Local persistence/backup test
- [ ] Privacy policy owner approval
- [ ] Public HTTPS policy link
- [ ] Google Play Data safety and Health apps declarations checked against these flows
