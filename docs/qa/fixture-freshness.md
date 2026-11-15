# Fixture and live-data freshness

Prevents false greens from stale caches and wall-clock drift.

## Policy defaults

From [`CachePolicy`](../../app/src/main/java/com/aetherealmind/grannysknees/data/CacheFreshness.kt):

| Age since `fetchedAt` | Status | UI must |
| --- | --- | --- |
| ≤ 60 minutes | FRESH | Show index normally |
| ≤ 180 minutes | STALE | Show index **and** stale labelling |
| > 180 minutes | EXPIRED | Hide index as current |
| No cache | UNAVAILABLE / EMPTY | Offline empty copy |

## JVM / instrumented (L1 / L2)

1. Always pass an explicit `referenceNow` (or fake clock). Never rely on
   `Instant.now()` for freshness assertions.
2. Align the clock to the fixture’s local calendar day when selecting “current
   hour”. A November 2026 fixture plus a 2027 wall clock yields INVALID_HOUR —
   that is a clock bug in the test, not a product win.
3. Scenario tags for contract fixtures: `nominal`, `missing-values`,
   `pressure-trend-boundary`, `dst-fall-back`, `dst-spring-forward`. See
   [`docs/contracts/fixtures/README.md`](../contracts/fixtures/README.md).
4. Anti–false-green: for EXPIRED and INVALID_HOUR, assert the UI state is
   `Unavailable` (or equivalent copy), **not** merely that a score is in 0–10.

## Live pulls (L3 / L4)

1. Prefer a fresh network fetch (or cleared cache then fetch) for “happy path”
   evidence.
2. Assert: selected city name, band ∈ allowed set, last-updated looks current
   after refresh, gaps not filled with zero, disclaimer present.
3. **Never** commit an expected numeric index from a live run into automation.
4. Stale/expired demos must age `fetchedAt` on purpose (or freeze time), not
   reuse an accidentally old cache and call it “fresh”.

## Regenerating fixtures

Allowed when Open-Meteo schema or contract math changes. Regeneration must keep
the same scenario intent. Update this note and the fixture README when cases
change.
