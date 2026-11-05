# Privacy data-flow and permission audit — Android 1.0

**Status:** Preliminary code-backed inventory, 2026-11-05. Not a final Play Data safety determination.
**Evidence:** `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `MainActivity.kt`, `weather/OpenMeteoClient.kt`; original current-state report and release plan.

| Data / flow | Current observed implementation | Open verification |
|---|---|---|
| Weather coordinates/timezone | `OpenMeteoClient.buildUrl` adds latitude, longitude and timezone to HTTPS `api.open-meteo.com/v1/forecast` | Confirm chosen-city behaviour and exact geocoding provider once PUB02 lands |
| Weather measurements | Requests temperature, relative humidity, surface pressure, 6+ forecast days and past hours | Verify Open-Meteo terms, production use and attribution |
| IP and connection metadata | HTTPS request necessarily reaches external provider; provider may observe IP and request metadata | Review provider processing terms and Play Data safety implications |
| Local weather cache | `MainActivity` creates `filesDir/forecast-cache.json` | Verify lifecycle, retention, invalidation and removal |
| Selected city | Planned for 1.0; not evidenced in reviewed MainActivity | Verify storage, backup and city-change cache isolation |
| Android backup | Manifest currently `android:allowBackup="true"` | Decide whether to disable or exclude cached/preferences data; verify Android 12+ data extraction rules |
| Internet permission | Manifest `android.permission.INTERNET` | Inspect merged release manifest for transitive permissions |
| Device GPS/location | No location permission declared in reviewed manifest | Preserve no-mandatory-GPS design |
| Advertising/analytics | No ad/analytics SDK identified in reviewed Gradle dependencies | Scan all dependencies, source, build variants and final AAB |
| Symptom or health reports | No symptom UI in 1.0 scope; symptom contract exists for later release | Verify no collection in final code; exclude browser prototype |
| App accounts/cloud sync | Not in 1.0 scope | Verify final release |
| Crash reporting | No dedicated SDK identified in reviewed dependencies | Confirm Play/OS diagnostics, SDK additions and statements |
| App widget | Existing Glance receiver; widget reads weather/index data | Confirm widget privacy on lock screen and stale data behaviour |

## Actions required before policy publication

1. Inspect **full release manifest and dependency tree**, including transitive libraries, not just Gradle direct dependencies.
2. Verify city search/geocoding endpoint, request payload, local storage and consent/choice.
3. Decide Android backup policy. Current `allowBackup=true` means do not assert exclusively device-local storage without qualification.
4. Confirm all third-party processors and whether provider IP/request handling triggers disclosures. Avoid automatically answering “no collection” in Play Console.
5. Check Open-Meteo commercial licensing, attribution and privacy documentation for actual use.
6. Establish support contact and publicly hosted HTTPS privacy policy. Do not publish personal contact information without approval.
7. Confirm no medical claims in app/store; include Health apps declaration.
8. Record owner sign-off, release commit, and privacy policy version.

## Evidence checklist

- [ ] Final merged manifest
- [ ] Release dependency/SDK inventory
- [ ] Network endpoint list with sample requests
- [ ] Local persistence/backup test
- [ ] Privacy policy owner approval
- [ ] Public HTTPS policy link
- [ ] Google Play Data safety and Health apps declarations checked against actual data flows
