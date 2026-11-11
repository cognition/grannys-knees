# Privacy Policy — Granny's Knees

**Effective date:** 2026-11-10
**App:** Granny's Knees (Android phone)
**Publisher/data controller:** AEO3 / Cognition
**Privacy contact:** cognition@aeo3.ca
**Policy URL:** https://github.com/cognition/grannys-knees/blob/main/PRIVACY-POLICY.md
>
> **Reviewed against:** release version 0.3.82.2 (1.0 phone scope; widget and Wear deferred).

## Privacy promise and important distinction

Granny's Knees is designed so that the **developer does not receive or collect** your symptom journal, saved city, or forecast cache. The app has no developer-operated account, advertising tracker or analytics service in the planned 1.0 release. The phone contacts the weather provider directly, not through a developer-operated backend.

**This is not a claim that no information ever leaves the device:** weather and geocoding requests send city search terms or selected coordinates and timezone to Open-Meteo, which can also observe connection metadata such as an IP address. The developer does not receive those requests.

Symptom notes, journal history, and most preferences remain on your phone. They are not uploaded to the developer. You may export journal content yourself through the Android share sheet.

## 1. What Granny's Knees does

Granny's Knees displays an experimental weather-based joint-discomfort index, an hourly outlook and a five-day outlook on your phone. You may optionally record symptom-related notes in an on-device journal. The index is derived from weather measurements and forecasts. It is not a diagnosis, treatment recommendation or personalized prediction of pain or arthritis symptoms.

The first Android release is intended to be free, without advertising, and initially available in Canada. It does not include user accounts, developer-operated cloud synchronization, a home-screen widget listing, a Wear OS companion, or personalized health profiles managed by the developer. Widget and Wear features are planned for later releases and will update this policy when they ship.

## 2. Information the app uses

**Weather location.** To show a forecast, the app uses the location associated with your chosen city, or—if you explicitly choose and grant permission—your device's current location to help select a place. Forecast requests include latitude, longitude and timezone. Your chosen city is stored in app-private storage (`selected-city.json`).

**City search.** When you search for a Canadian city by name, the app sends your search text to Open-Meteo's geocoding service over HTTPS.

**Weather forecasts.** The app requests temperature, relative humidity and surface pressure data from Open-Meteo. The weather-derived index is calculated within the app from those forecasts.

**Symptom journal (on-device).** If you choose to use the journal, you may enter symptom-related notes and related fields defined in the app. These entries are stored locally in a Room database (`journal.db`) on your phone. The developer does not receive them. You can delete journal data in the app or remove the app from your device.

**On-device data.** The app stores a weather forecast cache in app-private storage (`forecast-cache.json`), onboarding/preferences as applicable, and the journal database described above.

**Network information.** When the app connects to an external weather service, that provider may receive technical connection information, such as an IP address, in addition to the location coordinates and request parameters. Consult the provider's privacy information for its handling of those requests.

## 3. Why information is used

The app uses location information to retrieve the correct forecast; weather data to calculate and display the index and outlook; and local storage to show your city, cache, journal and freshness. It does not use your weather index or journal as evidence of a medical condition, and the developer does not use your journal for profiling or marketing.

## 4. External services

The reviewed app sends HTTPS requests directly to **Open-Meteo**:

- Forecast: `api.open-meteo.com`
- Geocoding (Canadian city search): `geocoding-api.open-meteo.com`

Requests include search text or forecast coordinates, timezone and weather-variable names as needed.

Open-Meteo privacy information: https://open-meteo.com/en/terms

The release contains no analytics, crash-reporting, advertising or other developer-operated third-party SDK/service beyond the weather services described above.

## 5. Sharing and selling

The app does not include advertising or a developer-operated account system. It sends the weather and geocoding request information described above to Open-Meteo to deliver forecasts. Journal data is not sold or shared by the developer because the developer does not receive it.

## 6. Storage, retention and deletion

The app maintains local forecast cache, city selection, journal data and related files in app-private storage. Cache entries expire according to in-app logic; you can clear journal data through in-app controls. The release candidate sets `android:allowBackup="false"`, so Google Auto Backup does not copy this app's private data. Device-to-device setup flows may still be governed by the operating system; describe only what the app opts into.

You can generally remove locally stored app data through Android's app storage controls or by uninstalling the app.

## 7. Permissions

The reviewed Android manifest includes:

- **INTERNET** — forecast and geocoding requests.
- **ACCESS_COARSE_LOCATION** and **ACCESS_FINE_LOCATION** — only used if you choose to use your phone's location to help set your city; city search works without granting these permissions. The manifest does not request background location.

## 8. Security

The forecast client uses HTTPS for requests to Open-Meteo. Local data is stored in the application's private files directory and local database. No app can guarantee absolute security.

## 9. Children

Granny's Knees is intended for a general adult audience interested in weather-related joint discomfort. The publisher will complete the applicable Google Play target-audience and child-directed-content declarations accurately in Play Console.

## 10. Your choices and questions

You can choose which city the app uses, whether to grant location permission, whether to use the journal, and you can remove the app and its locally stored information through Android's app controls. For privacy questions, contact **cognition@aeo3.ca**.

The app includes an in-app **Your privacy** summary for convenience; the authoritative policy is the published HTTPS page linked from the store listing.

## 11. Changes to this policy

We may update this policy when the app changes, including if future versions introduce cloud sync, analytics or additional providers. Material changes will be reflected in the published policy and, where appropriate, within the app. The effective date above will identify the applicable version.

---

**Publication approval:** AEO3 / Cognition — 2026-11-10 — release 0.3.82.2
**Data-flow verification:** Codex review — commit `88c7da2`
