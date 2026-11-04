# Granny's Knees

An experimental arthritis weather-sensitivity index and symptom journal.

## Personality and positioning

**Your joints. Your weather. Your excuse.**

Granny's Knees is a weather-informed joint discomfort index, **not** a fitness, rehabilitation, exercise-coaching or goal-tracking app. Its purpose is to offer a playful extra consideration when planning a day, explore possible weather/symptom relationships, and give users something to point to when they'd rather skip moving a sofa.

### A little science. A little superstition. A perfectly good excuse.

Granny's Knees uses weather data, research and your own observations to explore whether changes in the weather line up with changes in how your joints feel.

**Correlation doesn't mean causation.**

Your arthritis is yours. The weather doesn't magically give it to you.

Think of it like rain. You're wet because you went outside in the rain, not because you were wet and somehow made it rain.

The science is complicated, the evidence is mixed, and everybody's joints have their own opinions.

This is a fun, experimental weather index, **not medical advice, a diagnosis or a clinically validated prediction.**

But if the index says 9 out of 10 and someone wants you to help them move a sofa?

Well, now you've got something to point at.

**Granny says: “Not today.”**

### Where this appears

- **First launch:** Explain the experimental nature, mixed evidence, correlation versus causation and entertainment-first intent.
- **Daily forecast:** Show a short, humorous observation appropriate to the index, alongside a visible reminder: “Weather-based estimate, not a medical prediction. Your joints may disagree.”
- **About the Index:** Explain the weather variables, scoring approach, limitations, assumptions and research sources transparently.
- **Editorial guardrail:** Humour must not imply the index is clinically validated or that weather causes arthritis. Individual symptom observations and weather-based estimates remain distinct.

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
