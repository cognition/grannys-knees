# Grannys Knees

An experimental arthritis weather-sensitivity index and symptom journal.

## Current version

Open `index.html` in a modern browser. The prototype fetches forecasts from Open-Meteo and saves symptom entries in browser local storage. It supports CSV and JSON export. The index is an unvalidated heuristic and is **not** a clinical flare-up prediction.

## Android direction

The target product is a native Android app, with a home-screen widget and Wear OS tile (the Wear OS equivalent of a glanceable widget).

Proposed stack:
- Kotlin, Jetpack Compose, Material 3
- Room for offline symptom history
- DataStore for preferences
- Retrofit or Ktor for Open-Meteo
- WorkManager for periodic forecast refresh
- Glance App Widget for Android home screen
- Wear OS Tiles API for the watch tile
- Wear Data Layer for phone/watch synchronization where needed

## MVP delivery order

1. Extract and test a shared weather index scoring function; document uncertainty.
2. Build native Android dashboard, city selection, forecast and offline journal.
3. Add Android home-screen widget with index, location and refresh time.
4. Add Wear OS tile displaying index and last sync; optional symptom quick log.
5. Add explicit permissions, accessible UI, retention/export controls and automated tests.

Do not treat index scores as medical advice. Personal correlations are observational. Forecast and symptom histories must be timestamped and distinguished from actual historical weather measurements.
