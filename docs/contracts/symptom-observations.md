# Symptom observation contract

**Contract version:** `symptom-observations/1.0.0`
**Status:** Proposed for independent review (T10 / N04). No Room database and no symptom UI are implemented with this document.
**Owner task:** T10. **Persistence:** T11. **UI:** T12, only after T11.
**Last updated (UTC):** 2026-11-04.

This document defines local, append-only symptom observations and the sensor snapshot linked to them. Deterministic cases live under [`fixtures/symptom-observations/`](fixtures/symptom-observations/).

The HTML prototype (`index.html`) stores one pain value and one stiffness value per day in `localStorage`. That store is **not** normative. Do not migrate it field-for-field.

## 1. Disclaimer

A symptom rating is the user’s own report. It is not a diagnosis, not a treatment recommendation, and not the weather index. The forecast index (`weather-index` / `index-v1`) and a self-report both use 0–10 and must never be added, averaged, or labelled as the same quantity. Nothing in this contract claims that weather caused a symptom.

Weather-only use of the app must stay possible with zero observation rows.

## 2. Records

Two local tables, both append-only. Names are the logical Room entities for T11. This task does not create the database.

### 2.1 `SymptomObservation`

| Field | Type | Rule |
| --- | --- | --- |
| `id` | string (UUID) | Assigned when the row is appended. Never reused. |
| `observedAt` | instant (UTC epoch milliseconds) | When the user committed this report. Not the time of an earlier rating. |
| `measurements` | list of `SymptomMeasurement` | Only fields the user changed or explicitly confirmed. Empty list is forbidden; if there is nothing to record, write **no** row. |
| `correctsObservationId` | string or null | Set only when this row corrects an earlier row. The earlier row stays in the table. |
| `deletedAt` | instant or null | Null on a normal report. A deletion marker sets this and has an empty `measurements` list. See §6. |

### 2.2 `SymptomMeasurement`

| Field | Type | Rule |
| --- | --- | --- |
| `bodyPart` | enum §3 | Required. |
| `side` | `left`, `right`, or null | Null for a midline part (neck, back) or when the user did not choose a side. |
| `symptom` | `ache` or `stiffness` | Required. Pain and ache are the same symptom in this version: `ache`. |
| `rating` | integer 0–10 | Required when `reportKind` is `changed` or `confirmed_unchanged`. Never a float. |
| `reportKind` | `changed` or `confirmed_unchanged` | Required. There is no `unreported` value, because unreported fields are omitted. |
| `previousObservationId` | string or null | The prior row that last reported this same body part, side and symptom, if one exists. |
| `previousRating` | integer 0–10 or null | Copied from that prior measurement when `previousObservationId` is set. Null when this is the first report of that series. |
| `previousObservedAt` | instant or null | Copied from the prior observation’s `observedAt`. Writing a new row does not change the prior timestamp. |

A series key is `(bodyPart, side, symptom)`. `side: null` and `side: left` are different series.

### 2.3 `SensorSnapshot`

One snapshot may be linked to one observation. It is a separate object, not columns on the measurement.

| Field | Type | Rule |
| --- | --- | --- |
| `id` | string (UUID) | New for every snapshot. |
| `observationId` | string | The observation this context belongs to. |
| `capturedAt` | instant | When the snapshot was taken, which may differ from `observedAt` by a few seconds. |
| `readings` | list of `SensorReading` | May be empty if the user saved a report and every sensor was skipped. Empty is not the same as a fabricated zero. |

### 2.4 `SensorReading`

| Field | Type | Rule |
| --- | --- | --- |
| `quantity` | enum §3 | What was sampled. |
| `state` | `measured`, `retrieved`, or `unavailable` | `measured` = this device’s sensor. `retrieved` = a reading copied from the current weather cache or another provided source. `unavailable` = no value. |
| `value` | number or null | Null if and only if `state` is `unavailable`. |
| `unit` | string or null | Null when unavailable. Otherwise the unit in §3. |
| `source` | string | For example `phone-barometer`, `open-meteo-cache`, `health-connect`. Required even when unavailable, so the gap has a provenance. |
| `capturedAt` | instant | When this reading was taken. A cached weather value uses the cache `fetchedAt`, not “now”, if that is the time the value was actually retrieved. |

An unavailable reading stores `"value": null`. It must not store `0`.

## 3. Allowed values

**Body parts:** `head`, `neck`, `shoulder`, `elbow`, `wrist`, `hand`, `back`, `hip`, `knee`, `ankle`, `foot`, `other`.

**Sides:** `left`, `right`, or omitted/null. Neck and back should be stored with `side: null`.

**Symptoms:** `ache`, `stiffness`.

**Ratings:** integers `0` through `10` inclusive. `0` means the user explicitly reported none. It does not mean “skipped”.

**Sensor quantities and units:**

| `quantity` | Unit when present | Typical source |
| --- | --- | --- |
| `temperature` | `°C` | `open-meteo-cache` (`retrieved`) or a device sensor (`measured`) |
| `relative_humidity` | `%` | `open-meteo-cache` |
| `surface_pressure` | `hPa` | `open-meteo-cache` or `phone-barometer` |
| `pressure_trend` | `hPa` | Derived trend from the weather cache, or `unavailable` |
| `heart_rate` | `bpm` | Health Connect / watch, only with consent |
| `steps` | `count` | Health Connect / watch, only with consent |
| `sleep_minutes` | `min` | Health Connect / watch, only with consent |

No other quantity is valid in version `1.0.0`. Heart rate, steps and sleep are omitted entirely unless the user has granted that source. Omitted is not `unavailable`. `unavailable` is recorded only when a snapshot was attempted and the source could not supply a value.

The forecast index is not a sensor quantity.

## 4. Changed, confirmed unchanged, unreported

| User action | What is stored |
| --- | --- |
| Leaves a field untouched, confirmation off | Nothing for that series. Not zero. Not a copy of the last rating. |
| Sets a rating, including a change from a previous number | One measurement, `reportKind: changed`, `rating` set to the new integer. |
| Sets a rating to `0` on purpose | One measurement, `reportKind: changed`, `rating: 0`. |
| Ticks confirm on a field they did not edit | One measurement, `reportKind: confirmed_unchanged`, `rating` equal to `previousRating`. |
| Confirms a field that has no previous rating | Reject the confirm. Do not invent a previous value and do not write a row for that field. |
| Saves the form with no changed fields and no successful confirms | No observation, and no snapshot. |

A partial save writes only the series the user touched. A left-foot ache does not create rows for knees, hands, or the other symptom on that foot.

Confirming unchanged does not rewrite the previous row and does not move `previousObservedAt`. The new row’s `observedAt` is the confirmation time. UI that shows “last reported” must keep showing the original timestamp for the value, and may additionally show that it was confirmed later.

There is no carry-forward job. Opening the app the next day does not insert observations.

## 5. Previous-value handling

When appending `changed` or `confirmed_unchanged` for a series:

1. Find the latest observation that is not a deletion marker and that contains that series, in `observedAt` order.
2. If none, `previousObservationId`, `previousRating` and `previousObservedAt` are null.
3. If one exists, copy its id, rating and `observedAt` into those three fields.
4. Never update the older row’s rating or timestamp.

`previousRating` is context for the journal. It is not an input to `index-v1`.

## 6. Correction, deletion, export

**Correction.** Append a new observation with `correctsObservationId` set to the row being corrected, new `observedAt`, and the replacement measurements. The original row remains. Readers that want the current journal skip any observation that is referenced by a later correction, and skip deletion markers. They do not delete history from the file.

**Deletion of one report.** Append a marker with `deletedAt` set, `correctsObservationId` set to the target, and `measurements: []`. Do not erase the original row. Exports of the “current journal” omit the marker and the row it deletes. A full data export for the user’s own archive may include tombstones; the default share/export used by the product omits them.

**Delete everything.** Remove the local observation and snapshot tables from the device. This is the only destructive wipe. It does not contact a server. This version has no account and no cloud copy.

Snapshots attached to a corrected or deleted observation are kept with the original id so provenance is not rewritten. A wipe removes them with the tables.

## 7. Snapshot rules

Take a snapshot only when an observation row is actually written. A cancelled or empty save writes neither.

Each reading’s `state` is independent. Temperature can be `retrieved` while the barometer is `unavailable`. The unavailable reading is still listed, with a null value, because the snapshot tried. A source the user never allowed is left out of the list.

Do not copy the forecast index into the snapshot. Do not copy symptom ratings into the weather cache.

## 8. Room notes for T11 (not built here)

- Entities match §2. Measurements can be a child table keyed by `observationId` plus series key. A snapshot is a parent with child readings.
- The schema version starts at 1. Later migrations must not rewrite historical ratings in place.
- Queries that build “today’s report” return only series present in non-superseded, non-deleted observations. A missing series is absence, not a default `0`.
- Tests must prove: partial update, explicit zero, untouched series absent, unavailable sensor value null. Fixtures in this folder are the expected JSON for those cases.

## 9. Fixtures

| File | Case |
| --- | --- |
| `partial-update.json` | Left foot ache set to 6. Knees and stiffness absent. |
| `explicit-zero.json` | Left knee ache set to 0. Distinct from an omitted field. |
| `untouched-symptoms.json` | Save with no edits. `observations` is empty. |
| `confirmed-unchanged.json` | Right knee stiffness confirmed at the previous 4, without moving the old timestamp. |
| `unavailable-sensors.json` | Pressure attempted and unavailable (`value` null). Temperature retrieved. No zero filling. |

Reject any implementation that passes these cases by inserting zeros for gaps.
