# Granny's Knees: Android architecture

## Modules (1.0 shipping)

- `app`: Android Compose phone UI, navigation, Open-Meteo client, index scoring, on-device journal (Room).

## Deferred modules (later iterations)

- Home-screen Glance widget (previously under `app` … `/widget`).
- Wear OS companion (previously `:wear` plus phone watch sync).

## Data flow

Open-Meteo forecast -> repository/cache -> scoring -> Today / outlook screens.
Symptom form -> Room -> history and export.

## Privacy

Store journal entries on-device by default. No health data to third-party services. Weather requests need only location coordinates or selected city, not symptom data. Ask consent before using device location; support manual city selection.

## Forecast caveats

Forecast-based scores are heuristic, not clinical predictions. Pressure deltas must use real comparable timestamps; do not use future daily mean differences as a proxy for observed pressure change. Never silently equate forecasts with historical observations.

## Release criteria (1.0 phone)

Offline journal works; API failures show stale-data state; accessible large text and touch targets; timezone correctness; export/delete works; tests for scoring and storage.

## Observation semantics (required)

Use append-only, timestamped symptom events. Each event includes the exact changed measurements only, plus explicitly confirmed unchanged measurements if the user selects confirmation. A measurement has `bodyPart`, optional `side`, `symptom`, `rating` (integer 0–10), `observedAt`, and `reportKind` (`changed` or `confirmed_unchanged`). An omitted symptom means **not reported**, never zero and never implicitly carried forward.

A separate sensor snapshot is linked to the event by ID. Each reading stores value, unit, source, capturedAt, and state (measured/retrieved/unavailable); do not store an unavailable value as zero. Sensor inputs may include weather temperature, humidity, barometric pressure and trend, and consented Health Connect measurements such as heart rate, activity and sleep. Device sensor availability varies. The snapshot is contextual, not diagnostic.

## Delivery status

The committed Android phone app implements the 0–10 weather index, Canadian city selection, on-device symptom journal and privacy summary. Widget and Wear companions are deferred and stripped from the 1.0 AAB. Verify behaviour against the current release checklist before Play upload.
