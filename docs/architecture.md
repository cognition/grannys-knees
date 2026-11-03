# Granny's Knees: Android and Wear OS architecture

## Modules (planned)
- `app`: Android Compose UI and navigation.
- `core:model`: domain types and scoring.
- `core:data`: Room, DataStore and Open-Meteo repository.
- `widget`: Glance home-screen widget.
- `wear`: Wear OS tile and optional quick symptom entry.

## Data flow

Open-Meteo forecast -> repository/cache -> scoring -> dashboard / Android widget / Wear tile.
Symptom form -> Room -> insights and optional watch sync.

## Privacy

Store journal entries on-device by default. No health data to third-party services. Weather requests need only location coordinates or selected city, not symptom data. Ask consent before using device location; support manual city selection.

## Forecast caveats

Forecast-based scores are heuristic, not clinical predictions. Pressure deltas must use real comparable timestamps; do not use future daily mean differences as a proxy for observed pressure change. Never silently equate forecasts with historical observations.

## Release criteria

Offline journal works; API failures show stale-data state; widgets display last-updated timestamp; accessible large text and touch targets; timezone correctness; export/delete works; tests for scoring and storage.

## Observation semantics (required)

Use append-only, timestamped symptom events. Each event includes the exact changed measurements only, plus explicitly confirmed unchanged measurements if the user selects confirmation. A measurement has `bodyPart`, optional `side`, `symptom`, `rating` (integer 0–10), `observedAt`, and `reportKind` (`changed` or `confirmed_unchanged`). An omitted symptom means **not reported**, never zero and never implicitly carried forward.

A separate sensor snapshot is linked to the event by ID. Each reading stores value, unit, source, capturedAt, and state (measured/retrieved/unavailable); do not store an unavailable value as zero. Sensor inputs may include weather temperature, humidity, barometric pressure and trend, and consented Health Connect or Wear OS measurements such as heart rate, activity and sleep. Device sensor availability varies. The snapshot is contextual, not diagnostic.

## Widget specifications

- Android current widget: current 0–10 index and last refreshed timestamp.
- Android day widget: hourly 0–10 curve for the full local day.
- Android five-day widget: daily **low/high index** calculated from hourly forecasts, clearly distinguished from temperature.
- Wear OS tile: current score, short outlook, and quick symptom-entry action; optional complication for the current score.
- All layouts must show stale-data states and avoid presenting the index as a clinically validated prediction.

## Delivery status

The current committed app is a browser prototype with an older 0–100 index and full-entry symptom form. The native 0–10 index, partial observation persistence, sensor snapshots and widgets described here are **requirements, not yet implemented**.
