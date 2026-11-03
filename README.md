# Granny's Knees

An experimental arthritis weather-sensitivity index and symptom journal.

## Current version

Open `index.html` in a modern browser. The prototype fetches forecasts from Open-Meteo and saves symptom entries in browser local storage. It supports CSV and JSON export. The index is an unvalidated heuristic and is **not** a clinical flare-up prediction.

## Product requirements

The public-facing product name is **Granny's Knees**. The Git repository remains `grannys-knees`.

- Weather-sensitivity index is **0 to 10**, lower means more favourable conditions. The playful endpoints are 0, “go play football and climb a mountain,” and 10, “stay in bed or take a hot bath.” These are metaphors, not medical activity recommendations.
- Android widget offers a current score, hourly all-day graph, and next five days, showing **daily low and high index**, like temperature lows and highs. Daily extrema are calculated from hourly values, not averages.
- Symptom self-report uses 0–10 pain/ache and stiffness scales by symptom and body part, optionally including side. Only explicitly edited fields generate new observations.
- Unedited symptoms remain **not reported** at that timestamp, not zero, not unchanged, and not copied forward. An opt-in checkbox may explicitly confirm unchanged values, preferably with per-symptom confirmation as well.
- At report time, take a timestamped snapshot of available environmental data and permitted phone/watch sensors, with provenance, units, acquisition time and availability status. Never fabricate unavailable readings.
- Show most recently reported values separately from observations at the current time, with their original timestamps.
- Android app, Android home-screen widget, Wear OS tile and optional watch-face complication share these domain rules.

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
