# Symptom observation fixtures

Cases for contract `symptom-observations/1.0.0`. Each file has an `input` and an `expected` outcome. Identifiers such as `obs-partial-1` are stable labels for tests; a Room implementation may use its own UUIDs as long as the links (`previousObservationId`, `observationId`, `correctsObservationId`) stay consistent inside a case.

| File | What must be true |
| --- | --- |
| `partial-update.json` | One new left-foot ache. No knee, hand, or stiffness row. |
| `explicit-zero.json` | Rating `0` is stored as `changed`. The previous 5 and its timestamp stay on the old row. |
| `untouched-symptoms.json` | Nothing is appended. |
| `confirmed-unchanged.json` | A new `confirmed_unchanged` row points at the old 4. The old `observedAt` does not move. |
| `unavailable-sensors.json` | Unavailable pressure has `value: null`. Temperature keeps its cache time and unit. |

T11 should load these files in tests. This folder does not add a Kotlin parser.
