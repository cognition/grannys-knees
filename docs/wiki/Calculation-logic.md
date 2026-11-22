# How the calculations work

## Overview

Granny's Knees uses hourly Open-Meteo forecast data for:

- air temperature at 2 metres, in °C;
- relative humidity at 2 metres, in %;
- apparent (feels-like) temperature at 2 metres, in °C; and
- surface pressure, in hPa.

The app requests the selected city's IANA timezone and Unix timestamps. Each timestamp is converted into that city's local time, including daylight-saving changes. The phone's timezone does not replace the forecast city's timezone.

The calculation is deliberately a heuristic. It describes how conditions are *changing*, not absolute levels. It does not measure pain or establish that weather caused a symptom. The feed is outdoor only.

## Hourly Weather score (slope index)

For a scored hour `t`, the engine needs:

- the last four hourly temperatures, humidities, and (when present) feels-like values, oldest first; and
- the last thirteen hourly pressures (index 0 is 12 h ago, index 12 is now).

The four-point least-squares slope per hour is:

```text
m = (−3·x0 − x1 + x2 + 3·x3) / 10
```

Then:

```text
mT = slope(temp4)
mH = slope(hum4)
gap_i = max(0, temp_i − feels_i)   # if feels-like is missing, g = 0
g  = slope(gap)
dP12 = pressure13[12] − pressure13[0]
```

The unclamped score is:

```text
S = 5.5254
    − 0.0506 · dP12
    + 0.50  · max(0, −mT)      # cooling
    + 0.125 · max(0,  mT)      # warming (weaker than cooling)
    + 0.25  · mH
    + clamp(0.25 · g, −1, +1)
```

```text
index = clamp(S, 1, 10)
```

Steady weather sits near 5.5. Cooling and rising humidity push the index up. Rising pressure tends to pull it down. The result is rounded half-up to one decimal place for charts and daily ranges, and to a whole number for the large Today display.

The current implementation uses `slope-index/1.0.0` and `index-slope-v1`. Reference maths live in `scripts/slope_index.py` and `docs/contracts/slope-index.md`.

## Missing or invalid data

An hour is unavailable when any required temperature or humidity sample in the contiguous 4-hour slope window is missing, when that slope window’s hours are not contiguous, or when either 12-hour pressure endpoint (`t−12h` or now) is missing or null. Middle hours between the pressure endpoints may be absent or null; they are not used for `dP12`. A missing value is never treated as zero. Missing feels-like alone does not mark the hour unavailable; the gap term is simply dropped (`g = 0`).

Unavailable hours are omitted from calculations and shown as gaps or unavailable states. They do not lower the daily low or increase the daily high.

## Daily outlook

For each local calendar day, the app takes the minimum and maximum of the available hourly scores. If multiple hours share an extreme, the earliest UTC epoch is used as the representative time.

A day is marked complete when at least 18 hours are available. Fewer available hours produce a partial day. A day with no available hours has no index range.

## Personal score

Personal applies influence multipliers to the pressure, humidity, and cool/warm temperature *terms* before they are summed. Influence means how much that weather factor affects the person — not “high versus low” pressure or “hot versus cold”. Higher influence amplifies contributions that raise discomfort and dampens relief from that factor. The default multipliers of 1 leave Weather and Personal identical. Non-baseline multipliers stamp `index-slope-v1-personal`.

## Feel calibration

When Feel tuning is enabled and enough spaced check-ins exist, Feel ratings can gently adjust those influence levels. Feel ratings stay separate from the weather number itself.
