# Daily forecast fetch (minimal online)

## Goal

Fetch Open-Meteo weather at most once per local calendar day unless the user
explicitly refreshes. No background weather work. Pull-to-refresh updates
location only when the saved place came from GPS.

## Network rules

Allowed HTTP forecast pulls:

1. First open of the selected city’s local calendar day (no stamp for today, or
   no usable cache for the saved place).
2. Explicit pull-to-refresh on Today.
3. User picks or changes city.

Not allowed: tab changes, Activity resume, hour/freshness ticks, WorkManager,
widget/watch (cache read only).

## Pull-to-refresh

- `PlaceSource.Gps` → re-resolve GPS, save place, then force forecast fetch.
- `PlaceSource.Search` (default for legacy saves) → force forecast fetch only.
- Successful fetch stamps today’s local date.

## Persistence

- `SelectedCity.placeSource`: `Gps` | `Search` (missing JSON key → `Search`).
- `DailyFetchStampStore`: last successful fetch local date (`yyyy-MM-dd`) for
  gating opens.

## Out of scope

Changing cache freshness windows for UI labels; sayings refresh; city search UX.
