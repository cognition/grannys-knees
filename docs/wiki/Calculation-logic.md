# How the calculations work

## Overview

Granny's Knees uses hourly Open-Meteo forecast data for:

- air temperature at 2 metres, in °C;
- relative humidity at 2 metres, in %; and
- surface pressure, in hPa.

The app requests the selected city's IANA timezone and Unix timestamps. Each timestamp is converted into that city's local time, including daylight-saving changes. The phone's timezone does not replace the forecast city's timezone.

The calculation is deliberately a heuristic. It describes how provocative the weather is in this model; it does not measure pain or establish that weather caused a symptom.

## Hourly Weather score

For a scored hour `t`, the engine finds the observation exactly 12 elapsed hours earlier, `t − 12h`, using UTC epoch time. It calculates endpoint changes:

```text
deltaHumidity    = humidity(t)    − humidity(t − 12h)
deltaPressure    = pressure(t)    − pressure(t − 12h)
deltaTemperature = temperature(t) − temperature(t − 12h)
```

The raw signal is:

```text
signal = deltaHumidity − deltaPressure − deltaTemperature
```

Therefore:

- rising humidity raises the signal;
- falling pressure raises the signal; and
- falling temperature raises the signal.

The signal is mapped using fixed Ottawa ERA5 calibration anchors:

```text
index = clamp(1 + 9 × (signal + 89.4) / 177.8, 1, 10)
```

Values below the historical floor map to 1.0. Values above the historical ceiling map to 10.0. The result is rounded half-up to one decimal place for charts and daily ranges, and to a whole number for the large Today display.

The current implementation uses `weather-index/2.0.0` and `index-v2`.

## Missing or invalid data

An hour is unavailable when any required current value is missing, any required value from the 12-hour comparison is missing, or the comparison observation does not exist. A missing value is never treated as zero.

Unavailable hours are omitted from calculations and shown as gaps or unavailable states. They do not lower the daily low or increase the daily high.

## Daily outlook

For each local calendar day, the app takes the minimum and maximum of the available hourly scores. If multiple hours share an extreme, the earliest UTC epoch is used as the representative time.

A day is marked complete when at least 18 hours are available. Fewer available hours produce a partial day. A day with no available hours has no index range.

## Personal score

Personal uses the same 12-hour weather deltas, but applies an influence multiplier to each signed contribution before mapping the signal.

At the default multiplier of 1.0, the Personal calculation is identical to Weather. A positive contribution is multiplied by the selected influence. A negative contribution is multiplied by `2 − influence`, so increasing the influence strengthens discomfort signals while reducing the amount of relief that factor can provide.

The Personal profile is clamped to the supported 0–2 influence range. It changes the Personal series only; it does not change the Weather score or the user's symptom ratings.

## Feel calibration

When enabled, a journal save can capture the local weather context around that report. The context uses a small window around the saved time and records the report's rating separately from the Weather score.

Calibration:

1. combines saves within 15 minutes into one time-point;
2. keeps usable time-points that are at least 45 minutes apart;
3. unlocks after 10 counted time-points;
4. compares the report mean with the local Weather window mean; and
5. nudges pressure, humidity, and cold influence suggestions based on the signed weather drivers and rating residual.

The suggestion is blended onto the existing profile with 25% suggestion weight and then clamped. This is a local adjustment to Personal, not a medical model and not a change to the baseline Weather calculation.

## Local pressure option

If enabled, a recent phone barometer reading can influence the nearest current forecast hour only. The reading must be no more than 30 minutes old and within 90 minutes of a forecast hour. The blended pressure is:

```text
blendedPressure = 0.70 × forecastPressure + 0.30 × phonePressure
```

Future hours remain forecast-based. The phone does not use one barometer reading to rewrite the entire forecast.

## Data and calculation boundaries

- Forecast requests go directly from the phone to Open-Meteo.
- Forecast responses are cached in app-private storage.
- The score is calculated on the phone.
- Journal data is stored locally and is not sent to the developer.
- Weather scores and symptom ratings remain separate and are never added together.
