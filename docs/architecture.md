# Android and Wear OS architecture

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
