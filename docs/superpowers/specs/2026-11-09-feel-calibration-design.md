# Feel + Adjustment personal real-feel

## Goal

Show a Personal (“real-feel”) weather curve only when the user has **Settings
adjustments**, **Feel-based tuning**, or both. Feel ratings never replace the
Standard weather index.

## Terms

- **Adj** — pressure / humidity / cold sliders. Used when any multiplier ≠ 1.
- **Feel opt-in** — switch on Feel screen and Settings (same preference). Default
  **on**. Missing preference = on. Opt-out does not clear sliders or reports.
- **Personal** — `index-v2-personal` series on Today / outlook.

## Decision matrix

| Adj used | Feel data | Opt-in | Personal | Feel reports |
| --- | --- | --- | --- | --- |
| No | No | any | None | — |
| No | Yes | Off | None | Record only |
| Yes | No / Off | any / Off | Adj only | Record only when opted out |
| No | Yes | On | Feel tunes DEFAULT base after 10 consecutive | Calibrate |
| Yes | Yes | On | Feel tunes Adj base after 10 consecutive | Calibrate |

## Time-points

1. Saves within **15 minutes** (local zone) collapse to one usable time-point.
2. Pain = mean and sample stddev of **reported** body-part ratings only.
3. Weather = mean of AVAILABLE Standard `indexScore` in **[T−3h, T+3h]**; keep
   nearest-hour 12h deltas. No AVAILABLE hours → not usable (journal still saves).
4. **Consecutive** unlock: ≥10 counted points, each **≥45 minutes** after the
   previous counted point. Gaps of 15–45 minutes do not advance the count.

## Tuning

When opted in and unlocked:

`personal = clamp(0.75 * base + 0.25 * suggested, 0…2)`

Base is Adj if used, else DEFAULT `(1,1,1)`. Slider-displayed Adj stays separate
so opting Feel off returns to Adj-only (or none).

## Copy

How calculated must mention: 10 consecutive points, 15-minute same-moment,
≥45-minute spacing, hourly-on-the-hour tip (extremes help), and that you cannot
currently backdate a Feel report for earlier in the day.
