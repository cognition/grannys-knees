# Expectation catalogue

Source of truth for test IDs. Spec:
[`docs/superpowers/specs/2026-11-15-expectation-test-suite-design.md`](../superpowers/specs/2026-11-15-expectation-test-suite-design.md).

**Layers:** L1 JVM · L2 Compose UI Test · L3 Maestro · L4 Acceptance  
**Tracks:** Phone (active) · Widget (deferred) · Wear (deferred)

## Phone — user (UE)

| ID | Expectation | Layer | Status |
| --- | --- | --- | --- |
| UE-01 | First launch explains index → city search | L3, L4 | Active |
| UE-02 | Canadian city search + persist; change later | L2, L3 | Active |
| UE-03 | Location optional; decline still allows search | L2, L4 | Active |
| UE-04 | Today: city, local time, index, band, last updated | L2, L3 | Active |
| UE-05 | Local-day curve; unscored hours blank, never zero | L1, L2 | Active |
| UE-06 | Five-day index low–high from hourly scores | L1, L2 | Active |
| UE-07 | Missing / stale / expired / offline explained; no invented score | L1, L2, L4 | Active |
| UE-08 | Refresh fetches; auto-fetch respects city local day | L1, L2 | Active |
| UE-09 | Weather-only use needs no account / journal | L3 | Active |
| UE-10 | Partial journal: only reported parts saved | L1, L2 | Active |
| UE-11 | Rating 0–10; unchanged confirmation distinct | L1, L2 | Active |
| UE-12 | History / delete one / export / reset all | L2, L4 | Active |
| UE-13 | Personal/Feel opt-in; 10 spaced points; Standard unchanged | L1, L2 | Active |
| UE-14 | About: experimental, not medical advice | L3, L4 | Active |
| UE-15 | Index and journal ratings never combined | L1, L2 | Active |

## Phone — design (DE)

| ID | Expectation | Layer | Status |
| --- | --- | --- | --- |
| DE-01 | Lake teal light / soft navy dark; no cream paper; no purple | L1, L4 | Active |
| DE-02 | Friendly sans; large index; readable body | L4 | Active |
| DE-03 | Soft radii; Rough = coral secondary only | L4 | Active |
| DE-04 | Primary CTAs / selected nav teal; asides muted | L4 | Active |
| DE-05 | Dark + large type keeps score + curve readable | L3, L4 | Active |

## Privacy / honesty (PE)

| ID | Expectation | Layer | Status |
| --- | --- | --- | --- |
| PE-01 | Journal never uploaded; no account | L4 | Active |
| PE-02 | Forecast request excludes symptom payload | L1 | Active |
| PE-03 | Unavailable hour/sensor ≠ zero | L1, L2 | Active |

## Widget (deferred)

| ID | Expectation | Layer | Status |
| --- | --- | --- | --- |
| UE-W01 | Widget score/ranges match phone for same cache | L1, L4 | Deferred |
| UE-W02 | Resize layouts (compact / five-day / hourly) | L1, L4 | Deferred |
| UE-W03 | Stale labelling when cache ageing | L4 | Deferred |
| UE-W04 | Survives reboot | L4 | Deferred |
| DE-W01 | Widget colours align lake teal | L4 | Deferred |

## Wear (deferred)

| ID | Expectation | Layer | Status |
| --- | --- | --- | --- |
| UE-R01 | Tile shows current score from phone cache | L1, L4 | Deferred |
| UE-R02 | Quick log appends one journal row | L1, L4 | Deferred |
| UE-R03 | Duplicate event id is safe (idempotent) | L1 | Deferred |
