# Experimental 12-hour weather index

This proposal records the formula used in the seven-day Ottawa prototype. The active scorer is described in the [weather and index contract](weather-index.md). The proposed score is a weather heuristic. Its calibration uses weather data alone.

## Hourly calculation

Use Open-Meteo hourly surface pressure (hPa), air temperature at 2 m (°C), and relative humidity at 2 m (%). For each scored hour, subtract each value from exactly 12 elapsed hours earlier from its current value. Use UTC timestamps for matching. The humidity difference is in percentage points.

```text
signal = deltaHumidity - deltaPressure - deltaTemperature
index = clamp(1 + 9 * (signal + 89.4) / 177.8, 1, 10)
```

Falling pressure, falling temperature, and rising humidity all increase the signal. The formula adds the numeric changes directly, so each unit contributes equally in the arithmetic. Provide 12 hours of lead-in and mark windows with missing or invalid hourly inputs unavailable. Score forecast hours with the same trailing window. The formula compares window endpoints, so changes that reverse within 12 hours can cancel.

Clamp before rounding. Round half-up to one decimal for charts or to a whole number for the main display, in each case from the clamped unrounded value.

## Fixed calibration

Open-Meteo ERA5 hourly data for Ottawa (45.42, −75.70) was requested for November 8, 2024 through November 7, 2026 UTC. The archive supplied 17,388 complete rolling windows through November 2, 2026 at 6 p.m. Eastern.

| Raw signal | Index | Window (Eastern time) | Pressure change | Temperature change | Humidity change |
| ---: | ---: | --- | ---: | ---: | ---: |
| −89.4 | 1 | Apr 27, 2025, 5 a.m. to 5 p.m. | +5.4 hPa | +15.0 °C | −69 pp |
| 0 | 5.53 (approx.) | Zero net signal | | | |
| +88.4 | 10 | May 27, 2025, 5 p.m. to May 28, 5 a.m. | −0.6 hPa | −18.8 °C | +69 pp |

Use the fixed anchors for each chart. Values beyond the historical extremes clamp to 1 or 10.
