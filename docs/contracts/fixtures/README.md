# Weather/index contract fixtures

Deterministic fixtures for the versioned contract in
[`../weather-index.md`](../weather-index.md). They exist so that downstream
implementation tasks (T03 Open-Meteo client, T04 scorer) have a fixed,
reviewable target and so that the documented reference index (`index-v1`) is
provably consistent with worked examples.

**These fixtures are not medical data and the index is experimental, not a
clinical prediction.** See the contract document for the full disclaimer.

## Scope and non-goals

- Fixtures define *inputs* and the *expected derived outputs* for a specific
  contract version. They are not a scorer implementation.
- No production Kotlin is included in this task. T04 implements the scorer and
  must reproduce every `*.expected.json` value from the matching
  `*.input.json` under `index-v1`.

## Timestamps: `unixtime` + IANA conversion

Inputs use `timeformat=unixtime`, so `hourly.time[]` is an array of **integer
Unix epoch seconds** (true UTC instants, one real hour apart). This is required
because Open-Meteo **ISO** mode applies one fixed offset to the whole response
and does **not** repeat/skip local DST labels (confirmed against the live API
2026-11-04). The client converts each epoch with the IANA `timezone`
(`java.time`) to get DST-correct local time and a per-instant offset.

Each `*.expected.json` hour therefore carries:

| Field | Meaning |
| --- | --- |
| `epoch` | UTC epoch seconds (matches the input `time[]` entry) |
| `instantUtc` | the same instant as ISO-8601 `…Z` |
| `local` | DST-correct local wall time **with offset**, e.g. `2026-11-01T01:00:00-05:00` |
| `utcOffsetSeconds` | the offset **for that instant** (differs across DST) |
| `localDate` | local date used for daily low/high membership |

Daily `lowHour`/`highHour` are reported as `{epoch, instantUtc, local}` so the
chosen hour is unambiguous even when the wall label repeats.

## File pairs

| File | Shape | Meaning |
| --- | --- | --- |
| `<case>.input.json` | Open-Meteo `/v1/forecast` response (`timeformat=unixtime`) | Raw provider payload: `hourly.time[]` is epoch seconds; field names/units per Open-Meteo docs. |
| `<case>.expected.json` | Contract-derived result | Per-hour instant/offset fields, sub-scores (4 dp), `pressureTrend3h_hPa`, `indexRaw` (4 dp) / `indexScore` (1 dp) / `indexDisplay` (int), availability status, and daily low/high from the hourly **index** scores. |

`cache-freshness.expected.json` has no `.input.json`: cache freshness is a
repository-layer concern, not part of the Open-Meteo payload.

## Cases

| Case | Rows | Exercises |
| --- | --- | --- |
| `nominal` | 27 (3 lead-in + 24) | Clean EST winter day (`2026-11-10`): cold/calm overnight, afternoon pressure front. Daily **high** `6.9` is **unique** (`highTies: 1`) at local `16:00` where it is a mild 9.5 °C (3-h trend −5.8 hPa), while the coldest hour (`06:00`, −4 °C) scores only `5.3` — proving low/high come from the hourly index, not temperature. |
| `missing-values` | 27 | `surface_pressure` null at local `06:00`–`08:00` and `relative_humidity_2m` null at `12:00`. Null inputs → `indexScore: null` (status `unavailable`), never `0`. Each null pressure also makes its `+3h` row's trend unavailable (`09:00`–`11:00`). Day is `partial` (17/24 available; threshold 18). |
| `pressure-trend-boundary` | 24 (no lead-in) | Series starts at local midnight with no lead-in, so the first three rows have no comparable `t−3h` sample: `pressureTrend3h_hPa` and the index are `unavailable` — never faked as `0`. Day `complete` (21/24). |
| `dst-fall-back` | 28 (3 + 25) | `America/Toronto` `2026-11-01`: **25** local hours; the `01:00` wall label appears twice on different instants/offsets (`-04:00` then `-05:00`). The daily **high** `7.4` lands on the **second** `01:00` (`-05:00`) — demonstrating instant/offset disambiguation of the repeated label. |
| `dst-spring-forward` | 26 (3 + 23) | `America/Toronto` `2026-03-08`: **23** local hours; `02:00` is absent (instant after `01:00-05:00` converts to `03:00-04:00`). Tied extrema (`highTies > 1`) exercise the earliest-epoch tie-break. |
| `cache-freshness` | — | `fresh` / `stale` / `expired` / `no-cache-offline` states evaluated against a fixed `referenceNow`, per the freshness policy. |

## Determinism rules

- All weather values are hand-authored constants; no randomness, no live calls.
- Epoch/instant/offset fields come from a real IANA tz conversion of the input
  epochs; they do not depend on when the test runs.
- `pressureTrend3h_hPa = surface_pressure[i] − surface_pressure[i−3]` by array
  index (each row is one real hour apart, so `i−3` is exactly 3 hours earlier in
  UTC, including across DST). Defined only when both samples are present.
- Rounding is half-up (see contract §9.3–§9.4): `indexRaw` and sub-scores to
  4 dp, `indexScore` to 1 dp, `indexDisplay` to the nearest integer. Daily
  extrema and ties are decided on the 1-dp `indexScore`, and the `lowHour`/
  `highHour` tie-break is earliest epoch.
- Cache fixtures use an explicit `referenceNow` instead of the wall clock.

## How a reviewer/implementer validates

1. Parse `<case>.input.json` with the T03 client (`timeformat=unixtime`; convert
   each epoch with the IANA `timezone`).
2. Apply `index-v1` from `../weather-index.md` with half-up quantisation.
3. Assert the result equals `<case>.expected.json` field for field (exact on the
   canonical quantised values; no float tolerance).

Any intentional change to the formula is a new `indexVersion` and a new set of
`*.expected.json` values, bumped together with `contractVersion`.
