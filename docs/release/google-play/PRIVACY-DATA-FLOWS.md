# Data flows for Granny's Knees

**Status:** Draft for the owner. This is not a public privacy policy and it is not a Play Data safety submission.

## What the app does

The phone shows an experimental 0–10 weather index for a Canadian city the user picks, or for an optional foreground GPS fix if the user allows location. Symptom reports are optional and stay on the device.

## Permissions

- `INTERNET`, for Open-Meteo forecast and geocoding.
- `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`, only after the user taps "Use this phone's location". Declining them leaves city search working. There is no background location permission.

## What leaves the device

- City search text, sent to `geocoding-api.open-meteo.com` with `countryCode=CA`.
- The chosen latitude, longitude, and IANA timezone, sent to `api.open-meteo.com` for the hourly forecast.
- If a watch is paired, the current index text and a partial symptom message cross the Wear data layer between this phone and the watch app. They are not sent to a Granny's Knees server. There is no account.

## What stays on the device

- `selected-city.json`, the saved city.
- `forecast-cache.json`, the last forecast body.
- `journal.db`, append-only symptom observations, sensor snapshots, and watch event ids.
- `onboarding-seen`, a flag that the first explanation was shown.
- `watch-index.txt`, the last index text prepared for the watch.

Export shares the current journal as JSON through the Android share sheet. Delete everything removes the local journal tables. There is no cloud copy.

## What is not collected

No advertising SDK, no account, and no crash-reporting SDK is declared in the Gradle files. The forecast index is not stored as a symptom, and a symptom rating is not sent to Open-Meteo.

Approved logo PNGs are not in the repository yet. Launcher art must wait for those files. The SVG recreations are not the store identity.

## Still for the owner

Public support address, privacy-policy host, permanent application ID, and whether the personal Play account needs 12 testers for 14 days.
