# Personal index curve (sensitivity sliders)

## Goal

Show a second line on Today’s hourly index curve for a **personal** series
derived from the same weather inputs as `index-v1`, reweighted by user
sensitivity. Journal calibration is deferred.

## Scoring

- Baseline line: unchanged `index-v1` weights `0.50 / 0.25 / 0.25`.
- Personal line: multiply those weights by pressure / humidity / cold
  sensitivity (each **0–2**, default **1**), then renormalize to sum **1**.
- Same sub-score formulas and clamps; no journal input.
- Changing sliders re-scores from cache only (no network).

## UI

- Today curve: baseline + personal stroke + legend (“Weather” / “Yours”).
- About: three sensitivity sliders; reset to defaults.
- Hero score, bands, sayings, five-day outlook: baseline only.

## Persistence

`SensitivityProfileStore` → app-private JSON (`sensitivity-profile.json`).
