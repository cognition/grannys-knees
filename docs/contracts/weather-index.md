# Weather & index contract

**Contract version:** `weather-index/1.0.0`
**Index version:** `index-v1`
**Status:** Proposed for independent Codex review (T02). Pre-release; never committed or accepted. No production scorer is implemented in this task.
**Owner task:** T02 (Claude), reviewer: Codex.
**Last updated (UTC):** 2026-11-04.
**Review corrections (2026-11-04):** timestamp model switched to `timeformat=unixtime` + IANA conversion after the Codex review and an independent live-API reproduction showed Open-Meteo ISO mode does **not** repeat/skip local DST labels (see §4). Daily-extrema proof, tie-break, missing-value propagation, and cross-language precision rules corrected in the same pass.

This document defines the versioned contract between the Open-Meteo weather
provider, the repository/cache layer, and the experimental Arthritis Index for
Granny's Knees. It fixes variable names, units, timezone and freshness rules,
the pressure-trend definition, missing-value handling, and a versioned 0–10
reference index with its assumptions and uncertainty. Deterministic fixtures
live under [`fixtures/`](fixtures/).

Downstream tasks: T03 (Open-Meteo client and models) and T04 (pure Kotlin
scorer) must satisfy this contract and reproduce the fixtures. This document is
normative for the native Android scorer and fixtures.

---

## 1. Scope and disclaimer

The Arthritis Index is a **playful, experimental, research-informed** weather
sensitivity indicator on a **0–10** scale. It is **not medical advice, not a
diagnosis, and not a clinically validated prediction.** It expresses a
heuristic "weather might feel rough on joints today" signal derived from public
weather forecasts. Correlation is not causation.

This contract covers weather-derived forecast values only. It makes **no causal
or medical claim**. Any wording in UI, docs or fixtures that implies the index
predicts, diagnoses, or treats a medical condition is out of contract.

### 1.1 Two different 0–10 concepts (do not conflate)

| Concept | Source | Meaning | Owner |
| --- | --- | --- | --- |
| **Forecast index** (this contract) | Open-Meteo weather forecast | Heuristic environmental "joint-weather" score, 0 = benign … 10 = most provocative | T02/T03/T04 |
| **Self-reported discomfort** | The user's symptom journal | A person's own pain/stiffness rating for a body part | T10–T14 (separate contract) |

Both happen to use 0–10, but they are distinct quantities, stored separately,
and must never be merged, averaged, or presented as the same number. Observed
self-reports are never used as inputs to the forecast index in this contract;
any later correlation analysis is observational only and lives in a different
task (T17).

**An omitted self-report is "not reported" — never zero.** A symptom the user
did not touch produces no observation; it is not recorded as `0` and is not
carried forward from a previous entry. `0` is a deliberate rating a user can
choose, and is therefore distinct from "not reported". (This mirrors the
observation semantics in `docs/architecture.md`; the symptom-journal contract
owns the full rule. It is restated here only to keep the two 0–10 concepts from
being conflated.)

---

## 2. Provider and endpoint

- **Provider:** Open-Meteo Weather Forecast API.
- **Forecast endpoint:** `https://api.open-meteo.com/v1/forecast`
- **Geocoding endpoint (city selection):** `https://geocoding-api.open-meteo.com/v1/search`
- **Authentication:** none (public, free tier). No API key is stored.
- **Request granularity:** the index requires **hourly** data. Daily
  aggregation endpoints are **not** used to derive index extrema (see §8).

### 2.1 Required forecast request parameters

| Parameter | Value | Notes |
| --- | --- | --- |
| `latitude`, `longitude` | from selected city / consented location | decimal degrees |
| `hourly` | `temperature_2m,relative_humidity_2m,surface_pressure` | see §3 |
| `timezone` | IANA name, e.g. `America/Toronto` | used for geocoded local-day boundaries; see §4 |
| `timeformat` | `unixtime` | **required**; makes `hourly.time[]` UTC epoch seconds so the client can apply DST-correct IANA conversion (see §4) |
| `forecast_days` | ≥ 6 | 5-day outlook plus the current local day |
| `past_hours` or lead-in | ≥ 3 | so the pressure trend (§5) is defined for the first displayed hour; see §5.3 |

Optional/for later consideration (not required by `index-v1`): `pressure_msl`,
`dew_point_2m`, `apparent_temperature`, `precipitation`, `weather_code`
(see §9.4 and §11).

---

## 3. Hourly weather inputs

All variable names below are verified against the authoritative Open-Meteo
documentation (<https://open-meteo.com/en/docs>) as of 2026-11-04.

| Purpose | Open-Meteo hourly variable | Unit (metric) | Required by `index-v1` |
| --- | --- | --- | --- |
| Air temperature at 2 m | `temperature_2m` | °C | Yes |
| Relative humidity at 2 m | `relative_humidity_2m` | % | Yes |
| Surface (station-level) pressure | `surface_pressure` | hPa | Yes |
| Mean sea-level pressure | `pressure_msl` | hPa | No (alt. pressure source) |
| Dew point at 2 m | `dew_point_2m` | °C | No |
| Apparent temperature | `apparent_temperature` | °C | No |
| Precipitation total | `precipitation` | mm | No |
| Weather code | `weather_code` | WMO code | No |

### 3.1 Authoritative-name correction (carry into T03)

The T00 live probe used the legacy spelling `dewpoint_2m`, which Open-Meteo
still accepts as an alias. The **authoritative current name is
`dew_point_2m`** (with underscore). If/when dew point is adopted, the client
must request `dew_point_2m`. All names in this contract are the current
canonical spellings.

### 3.2 Units

The response echoes units under `hourly_units`. The client **must** assert the
received units match the contract (°C, %, hPa) rather than assuming them;
Open-Meteo can return Imperial units if `temperature_unit`/`windspeed_unit`/etc.
are set. This contract fixes metric units; no unit-conversion parameters are
sent.

---

## 4. Timestamps and timezone (DST-correct)

> **Why `unixtime`, not ISO local time.** In ISO mode, Open-Meteo applies a
> **single** `utc_offset_seconds` to the whole response and **ignores DST
> transitions that fall inside the requested range**: a local calendar day that
> actually has 23 or 25 wall-clock hours is still returned as 24 evenly-spaced
> labels, and the fall-back hour is **not** repeated nor the spring-forward hour
> skipped. This was confirmed against the live API on 2026-11-04 (a
> fall-back-day request returned exactly 24 sequential `00:00…23:00` labels).
> Relying on ISO local labels for DST correctness is therefore wrong, and T03
> must not implement that rule.

### 4.1 The model this contract uses

1. Request `timeformat=unixtime`. Then `hourly.time[]` is an array of **integer
   Unix epoch seconds**, each a genuine **UTC instant**, consecutive entries
   exactly **one real hour** apart.
2. The response still carries a single `utc_offset_seconds`, `timezone`, and
   `timezone_abbreviation`. These describe the **request-time** offset only and
   **must not** be applied per-row across a DST boundary.
3. The client converts **each epoch** to the IANA `timezone` using a real
   tz-database implementation (`java.time.Instant` + `ZoneId` →
   `ZonedDateTime`). This yields, per hour: the local wall-clock time **and the
   correct UTC offset for that instant.**
4. A row's **local calendar day** (for daily low/high, §8) is the local date of
   its converted instant — never a hard-coded block of 24.

### 4.2 Daylight-saving transitions

Because membership is by converted local date, a local calendar day has 23, 24,
or 25 hourly rows:

| Case | Day (America/Toronto) | Local hours | Behaviour after IANA conversion |
| --- | --- | --- | --- |
| Spring forward | `2026-03-08` | **23** | The `02:00` wall hour never occurs: the instant after `01:00-05:00` converts to `03:00-04:00`. |
| Fall back | `2026-11-01` | **25** | The `01:00` wall label occurs **twice**, on two different instants, with **different offsets** — first `01:00-04:00` (EDT), then `01:00-05:00` (EST). |

The repeated fall-back hour is disambiguated by **epoch / UTC instant / offset**,
not by the wall label. Every fixture row carries `epoch`, `instantUtc`,
`local`, and per-row `utcOffsetSeconds`, and daily `lowHour`/`highHour` are
reported as `{epoch, instantUtc, local}` so the chosen hour is unambiguous even
when the label repeats. Fixtures `dst-fall-back` (25 rows; daily **high** lands
on the **second** `01:00`, offset `-05:00`) and `dst-spring-forward` (23 rows)
pin this.

---

## 5. Pressure trend (comparable timestamps only)

Rapid changes in barometric pressure — not the absolute level — are the
anecdotally cited weather trigger. The index therefore uses a **short-horizon
pressure change from comparable timestamps.**

### 5.1 Definition

```
pressureTrend3h_hPa[i] = surface_pressure[i] − surface_pressure[i − 3]
```

where indices are positions in the hourly series. Because the series is
instant-based and strictly one real hour apart (§4), position `i − 3` is
**exactly three hours earlier in UTC**, including across DST boundaries — so the
trend is always a true 3-hour change, never distorted by a wall-clock
repeat/skip. It is defined **only** when `i ≥ 3` and both `surface_pressure[i]`
and `surface_pressure[i − 3]` are present (non-null). Otherwise it is
`unavailable`.

### 5.2 Explicitly forbidden

- **Do not** use differences of **daily mean** pressure
  (`surface_pressure_mean[d] − surface_pressure_mean[d−1]`) as a proxy for an
  hourly pressure change; `docs/architecture.md` forbids it.
- **Do not** substitute `0` for an unavailable trend. An unavailable trend
  makes the hour's index `unavailable` (§7); it does not make it calm.

### 5.3 Lead-in requirement

Because the first displayed hour needs its `t − 3h` sample, the client must
fetch at least a 3-hour lead-in before the first hour it intends to display
(via `past_hours` or by including earlier forecast hours). Fixture
`pressure-trend-boundary` shows the honest degraded behaviour when no lead-in
is present: the first three hours are `unavailable`.

---

## 6. Forecast freshness and cache semantics

Freshness is a **repository/cache-layer** concept, not part of the Open-Meteo
payload. The cache wraps a fetched forecast in an envelope:

| Field | Meaning |
| --- | --- |
| `fetchedAt` | UTC instant the forecast was retrieved |
| `providerGenerationtimeMs` | Open-Meteo `generationtime_ms`, for diagnostics |
| `freshForMinutes` | policy: max age considered fresh (default **60**) |
| `staleAfterMinutes` | policy: max age still usable-but-stale (default **180**) |

### 6.1 States

Let `age = now − fetchedAt`.

| State | Condition | Presentation |
| --- | --- | --- |
| `fresh` | `age ≤ freshForMinutes` | Show normally. |
| `stale` | `freshForMinutes < age ≤ staleAfterMinutes` | Usable, but **must** show a stale badge and the `fetchedAt` last-updated time. |
| `expired` | `age > staleAfterMinutes` | Do **not** present index values as current; show last-updated and an explicit expired/offline state. |
| `unavailable` | nothing cached | Empty/offline state; never fabricate values. |

Defaults are a starting policy for review (see §11). Fixture
`cache-freshness` pins all four states against a fixed `referenceNow` so tests
stay clock-independent.

---

## 7. Missing and unavailable values

- Open-Meteo represents a missing hourly value as JSON **`null`** in the value
  array (positionally aligned with `hourly.time[]`). The client must treat
  `null` as missing — **never** as `0`.
- If **any** input required by `index-v1` for an hour (surface pressure, the
  3-hour pressure trend, relative humidity, or temperature) is unavailable,
  that hour's index is `unavailable` (`indexScore: null`, `status:
  "unavailable"`) with machine-readable `unavailableReasons`.
- **Propagation of a single null pressure at position `i`:** it makes row `i`
  unavailable directly (its own pressure input is missing) and makes the
  **3-hour trend** unavailable at exactly **two** rows — `i` (needs
  `pressure[i]`) and `i + 3` (needs `pressure[(i+3) − 3] = pressure[i]`). It does
  **not** affect `i + 1` or `i + 2`. Rows `i + 1` and `i + 2` are only affected
  if they too have a null pressure.
- The `missing-values` fixture stacks **three consecutive** null pressures (local
  `06:00`, `07:00`, `08:00`). Directly unavailable: `06:00`, `07:00`, `08:00`.
  Trend-unavailable at the matching `+3h` rows: `09:00`, `10:00`, `11:00`. A
  separate null humidity at `12:00` makes that row unavailable too. Available
  rows are never padded (17 of 24 → day `partial`).
- Never fabricate, interpolate, or zero-fill a missing weather value to keep a
  score "complete". A gap is reported as a gap.

---

## 8. The 0–10 index and daily low/high

### 8.1 Scale interpretation (experimental)

| Band | Display | Rough meaning (experimental, non-medical) |
| --- | --- | --- |
| 0–2 | Calm | Weather unlikely to be provocative. |
| 3–4 | Mild | Minor change in the air. |
| 5–6 | Noticeable | Changeable weather some people notice. |
| 7–8 | Rough | Marked pressure/humidity/cold movement. |
| 9–10 | Stormy | Strong, fast weather change. |

These are evocative labels, not clinical categories.

### 8.2 Daily low/high from hourly **index** scores

- `daily.low`  = **minimum** of the available hourly `indexScore` across that
  local calendar day.
- `daily.high` = **maximum** of the available hourly `indexScore` across that
  local calendar day.

The outlook is a **five-day low/high of the index**, computed from the hourly
index curve — **not** from temperature, and **not** from a daily-average index
used as a proxy for extrema.

**Worked proof (`nominal` fixture).** The day is cold, humid and calm overnight,
with an afternoon front bringing a sharp pressure fall. The daily **high** is
`6.9` at a single hour, local `2026-11-10T16:00:00-05:00` (uniquely —
`highTies: 1`), where the temperature is a mild **9.5 °C** and the 3-hour
pressure trend is **−5.8 hPa**. The **coldest** hour of the day is `06:00`
(−4.0 °C), whose index is only `5.3`. So the daily high is a **warm** hour driven
by the pressure change, and the coldest hour is **not** the high — the extrema
follow the hourly **index**, not temperature. (An earlier draft mislabelled the
coldest hour and the high was tied; the fixture was redesigned so the high is a
genuinely unique, warm hour clearly separated from the coldest hour. The
tie-break in §8.4 still covers the general tied case.)

### 8.3 Partial days

| Daily `status` | Condition |
| --- | --- |
| `complete` | available hours ≥ **18** (of the day's rows) |
| `partial` | 1 ≤ available hours < 18 (low/high computed from available hours, flagged) |
| `unavailable` | 0 available hours |

The 18-hour threshold is a reviewable default (§11). Low/high are computed over
**available** hours only; they are never padded.

### 8.4 Extrema tie-break (deterministic)

If several hours share the daily minimum (or maximum) `indexScore`, the reported
`lowHour` (or `highHour`) is the one with the **earliest epoch** (earliest UTC
instant). Fixtures expose `lowTies`/`highTies` counts so a reviewer can see when
the rule is exercised — e.g. `dst-spring-forward` has tied extrema resolved this
way, while `nominal` has a unique high (`highTies: 1`). The low/high **values**
are unaffected by the tie-break; only the reported hour is.

---

## 9. Reference index `index-v1`

This is the versioned reference definition that fixtures encode and that T04
must reproduce. It is intentionally simple, transparent, and **experimental**.

### 9.1 Sub-scores (each 0–10)

Let `clamp(x) = max(0, min(10, x))`.

```
pressure    = clamp( |pressureTrend3h_hPa| / 6  × 10 )     # magnitude of 3-h change
humidity    = clamp( (relative_humidity_2m − 40) / 50 × 10 )
temperature = clamp( (20 − temperature_2m) / 20 × 10 )      # colder ⇒ higher
```

### 9.2 Weights and combination

```
indexRaw = 0.50 × pressure + 0.25 × humidity + 0.25 × temperature
```

If any sub-score is `unavailable`, `indexRaw` is `unavailable` (§7).

### 9.3 Rounding and display

- `indexRaw` — retained to 4 decimal places (fixtures) for review.
- `indexScore` — `indexRaw` rounded **half-up to 1 decimal place**. This is the
  value used for daily low/high (§8.2).
- `indexDisplay` — `indexRaw` rounded **half-up to the nearest integer** (0–10),
  for compact UI such as widgets and tiles.

### 9.4 Cross-language precision and equality (normative)

To keep T04 (Kotlin) bit-for-bit comparable with the fixtures:

1. **Intermediate arithmetic** is done in full IEEE-754 **double** precision. No
   rounding happens before the final quantisation steps in §9.3.
2. **Rounding mode** is **half-up** (`RoundingMode.HALF_UP`), applied by scaling
   to the target decimal places on the decimal value — **not** by formatting a
   binary float. (Decimal `7.6667` is what is emitted, independent of the
   binary-float tie-breaking that `HALF_EVEN`/`toString` would introduce.)
3. **Canonical serialisation:** `subScores.{pressure,humidity,temperature}` and
   `indexRaw` are serialised to exactly **4 decimal places**; `indexScore` to
   **1 decimal place**; `indexDisplay` as an **integer**.
4. **Assertions are exact** on these canonical values — `indexScore`,
   `indexDisplay`, each 4-dp sub-score, `pressureTrend3h_hPa` (2 dp),
   `epoch`/`utcOffsetSeconds` (integers). There is **no** floating-point
   tolerance; equivalence is defined after the §9.3/§9.4 quantisation. Daily
   extrema comparisons use the quantised `indexScore`, not raw doubles, so
   min/max and ties are decided on 1-dp values.

Any change to §9.1–§9.4 is a **new `indexVersion`** with regenerated fixtures
(§10).

### 9.5 Stated assumptions and uncertainty

`index-v1` is a **documented heuristic, not a validated model.** Known limits,
to be surfaced in UI/disclaimers and revisited before any scoring is treated as
stable (a deferred decision in `docs/delivery-plan.md`):

1. **Weights are unvalidated.** The 0.50/0.25/0.25 split encodes the common
   anecdote that pressure *change* dominates, but no study in this repo
   justifies the exact numbers.
2. **Magnitude, not direction.** `index-v1` scores the magnitude of pressure
   change; some literature emphasises *falling* pressure specifically. Direction
   weighting is deferred (§11) to keep v1 deterministic and reviewable.
3. **Thresholds are judgement calls.** The 6 hPa / 40–90 % / 0–20 °C anchors are
   plausible but arbitrary; they are versioned so changes are explicit.
4. **Forecast, not observation.** Inputs are *forecast* values. Forecast error
   is not modelled; scores are heuristics about predicted weather, never
   measurements of past weather (`docs/architecture.md`).
5. **Individual variation is enormous.** Sensitivity differs per person; a
   single population heuristic cannot capture it. Hence "experimental".

---

## 10. Versioning policy

- `contractVersion` (`weather-index/MAJOR.MINOR.PATCH`) covers the whole
  contract: variables, timezone/freshness rules, fixture schema.
- `indexVersion` (`index-vN`) covers the scoring formula specifically.
- **Breaking** changes (variable set, formula, rounding, daily-extrema rule)
  bump MAJOR of `contractVersion` and/or `N` of `indexVersion`, and **all**
  `*.expected.json` fixtures are regenerated in the same change.
- Fixtures carry both version strings so a reviewer can detect drift.

---

## 11. Open decisions (for Codex review)

1. **Index weights & thresholds** — confirm or replace the 0.50/0.25/0.25
   weights and the 6 hPa / 40–90 % / 0–20 °C anchors; attach research citations
   before scoring is treated as stable (deferred decision in the delivery plan).
2. **Pressure direction** — should falling pressure score higher than rising for
   the same magnitude? `index-v1` uses magnitude only.
3. **Pressure source** — `surface_pressure` (station-level, elevation-sensitive)
   vs `pressure_msl` (sea-level normalised). `index-v1` uses `surface_pressure`;
   confirm this is the intended trend source.
4. **Trend horizon** — 3 hours is chosen; confirm vs 1 h / 6 h.
5. **Freshness policy** — 60 min fresh / 180 min stale defaults (§6).
6. **Partial-day threshold** — 18 available hours for a `complete` day (§8.3).
7. **Additional inputs** — whether `dew_point_2m`, `apparent_temperature`,
   `precipitation`, or `weather_code` should enter a future `index-v2`.
8. **Display rounding** — integer `indexDisplay` for widgets vs one-decimal
   everywhere; confirm the dual representation is wanted.

---

## 12. Fixtures

See [`fixtures/README.md`](fixtures/README.md). Pairs of
`<case>.input.json` (Open-Meteo-shaped, `timeformat=unixtime`) and
`<case>.expected.json` (contract-derived, with per-row
`epoch`/`instantUtc`/`local`/`utcOffsetSeconds`) cover: `nominal`,
`missing-values`, `pressure-trend-boundary`, `dst-fall-back`,
`dst-spring-forward`, plus `cache-freshness.expected.json`.

---

## 13. References

- Open-Meteo Weather Forecast API documentation — <https://open-meteo.com/en/docs>
  (variable names, units, `utc_offset_seconds`, `timeformat`, `null` for missing
  values; retrieved 2026-11-04).
- Live-API reproduction (2026-11-04): a fall-back-day hourly request in ISO mode
  returned 24 sequential local labels (no repeat); the same request with
  `timeformat=unixtime` returned epoch seconds that convert, under
  `America/Toronto`, to the repeated `01:00-04:00` / `01:00-05:00` pair. This is
  why §4 mandates `unixtime` + IANA conversion.
- `docs/architecture.md` — pressure-trend and forecast-vs-observation rules;
  observation (self-report) semantics.
- `docs/delivery-plan.md` — product definition, 0–10 scale, low/high-from-hourly
  requirement, deferred scoring-formula decision.
