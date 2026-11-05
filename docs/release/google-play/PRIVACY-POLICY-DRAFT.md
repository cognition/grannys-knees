# Privacy Policy — Granny's Knees

**DRAFT FOR OWNER REVIEW — NOT YET PUBLISHED OR EFFECTIVE**
**Proposed effective date:** [DATE OF PUBLICATION]
**App:** Granny's Knees (Android)
**Publisher/data controller:** [PERSONAL DEVELOPER LEGAL NAME OR APPROVED PUBLIC IDENTITY]
**Privacy contact:** [SUPPORT EMAIL]
**Policy URL:** [PUBLIC HTTPS URL]

> **Release hold:** Replace all brackets, verify actual release build and third-party data flows, obtain publisher approval, then publish as an accessible public HTML page. This Markdown draft is not itself a live Play Store privacy policy.

## 1. What Granny's Knees does

Granny's Knees displays an experimental weather-based joint-discomfort index, an hourly outlook and a five-day outlook. It also offers Android home-screen widgets. The index is derived from weather measurements and forecasts. It is not a diagnosis, treatment recommendation or personalized prediction of pain or arthritis symptoms.

The first Android release is intended to be free, without advertising, and initially available in Canada. It does not include user accounts, symptom journalling, Wear OS synchronization or personalized health profiles.

## 2. Information the app uses

**Weather location.** To show a forecast, the app uses the location associated with your chosen city. Forecast requests include latitude, longitude and timezone. City selection is planned for release 1.0; its final storage and search implementation must be verified before this policy is published.

**Weather forecasts.** The app requests temperature, relative humidity and surface pressure data from Open-Meteo. The weather-derived index is calculated within the app from those forecasts.

**On-device data.** The reviewed Android code stores a weather forecast cache in app-private storage (`forecast-cache.json`). A saved city preference is planned for 1.0; its retention and Android backup behaviour must be verified.

**Network information.** When the app connects to an external weather service, that provider may receive technical connection information, such as an IP address, in addition to the location coordinates and request parameters. Consult the provider's privacy information for its handling of those requests.

**No symptom entries in 1.0.** The planned 1.0 Android release does not ask you to record pain, stiffness, diagnoses or other personal health observations. An older browser prototype and future features are outside the scope of this Android policy.

## 3. Why information is used

The app uses selected-location information to retrieve the correct forecast; weather data to calculate and display the index and outlook; and local cache/preferences to support display, freshness indications and use after a network interruption. It does not use your weather index as evidence of a medical condition.

## 4. External services

The reviewed app sends HTTPS forecast requests directly to **Open-Meteo** (`api.open-meteo.com`). Requests include forecast coordinates, timezone and weather-variable names. The release implementation may add a geocoding endpoint for Canadian city search; its provider and privacy terms must be identified before publication.

Open-Meteo privacy information: https://open-meteo.com/en/terms

**[VERIFY BEFORE RELEASE]** Confirm whether any analytics, crash reporting, advertising, geocoding or other third-party SDK/service has been added. If so, describe it here and in Google Play's Data safety declaration.

## 5. Sharing and selling

The planned app does not include advertising or an account system. It sends the weather request information described above to external service providers to deliver the forecast. **[VERIFY]** Whether any additional transfer, collection, sharing or sale occurs must be determined from the final release build and provider contracts before this statement is finalized.

## 6. Storage, retention and deletion

The reviewed Android implementation maintains a local forecast cache in app-private storage. **[VERIFY]** The exact cache expiry and deletion behaviour, the persistence of the selected city, and whether Android backup can retain or transfer any app data. Android currently declares `allowBackup=true`; therefore this policy must not claim that data can never be backed up or leave the device.

You can generally remove locally stored app data through Android's app storage controls or by uninstalling the app, subject to Android backup/restore behaviour. **[VERIFY]** Any additional deletion process needed for the final build.

## 7. Permissions

The reviewed Android manifest requests the **Internet** permission for forecast retrieval. It does not currently declare a device location permission. The intended city-search release should not require precise device location. **[VERIFY]** The final merged manifest and all SDK permissions before publication.

## 8. Security

The reviewed forecast client uses HTTPS for requests to Open-Meteo. Local data is stored in the application's private files directory. No app can guarantee absolute security. **[VERIFY]** The final build's network configuration, backup settings, dependencies and any telemetry before release.

## 9. Children

Granny's Knees is intended for a general adult audience interested in weather-related joint discomfort. **[DECISION REQUIRED]** Select the accurate Play Console target age groups and confirm whether any child-directed requirements apply. Do not publish this section as a claim that children cannot access the app without reviewing the intended audience and implementation.

## 10. Your choices and questions

You can choose which city the app uses once city selection is implemented, and you can remove the app and its locally stored information through Android's app controls. For privacy questions, contact **[SUPPORT EMAIL]**.

## 11. Changes to this policy

We may update this policy when the app changes, including if future versions introduce symptom tracking, analytics or additional providers. Material changes will be reflected in the published policy and, where appropriate, within the app. The effective date above will identify the applicable version.

---

**Publication approval:** [OWNER NAME / DATE / RELEASE VERSION]
**Data-flow verification:** [REVIEWER / COMMIT SHA]
