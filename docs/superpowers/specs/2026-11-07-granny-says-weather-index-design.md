# Granny Says on the weather index

## Goal

Show a Granny saying on Today for the current knee-weather index. Prefer wry lines with a weighted pick that grows toward wry on each refresh, and pick again when the score changes.

## Source

- Canonical copy: `assets/granny-says.json`
- Packaged for the app: `app/src/main/assets/granny-says.json` (same contents)
- Format: root `grannySays` array of bands with `index` (1–10) and `sayings` of `{ tone, text }` where tone is `warm`, `wry`, or `spicy`

## Behaviour

- Visible only when Today has an available index. Hidden while loading or offline.
- Score `1`–`10` uses the matching band. Score `0` uses band `1`.
- Missing or empty band: keep the existing band caption; show no saying line.
- First show for a score, or when the score changes: wry 40%, warm 30%, spicy 30%. Then pick uniformly among that tone’s lines.
- Each Refresh: wry weight rises by 10 points (capped at 80%). Remaining weight is split evenly between warm and spicy.
- After the wry cap: wry 80%, warm 10%, spicy 10%.
- Avoid repeating the same text on the next pick when another line exists in the chosen pool (or in the band when falling back).
- Journal body-part asides are unchanged.

## UI

- Under the index caption on Today: the saying text, plus an “Another saying” control.
- Refresh only reshuffles the saying for the current score; it does not refetch weather.

## Testing

- Parse and band lookup for scores 0–10.
- Weight schedule and wry preference across successive refreshes.
- Score-change reset of weights.
- Avoid immediate text repeat when alternatives exist.

## Version

Feature bump on the phone app from `0.3.19.6` to `0.3.20.0`.
