# Acceptance matrix (L4)

Named device/UX checks for Granny's Knees. Complements automated L1–L3.
Historical evidence: [`docs/traycer/DEVICE-ACCEPTANCE.md`](../traycer/DEVICE-ACCEPTANCE.md).

**Freshness rule:** When a row needs live weather, fetch **new** data for that run.
Do not reuse yesterday’s screenshots or a stale `forecast-cache.json` as proof of
a fresh path. Exercise stale/expired as **separate** rows with a deliberately aged
cache.

| ID | Scenario | Pass criteria | Track |
| --- | --- | --- | --- |
| ACC-01 | First launch onboarding | Explains index; Continue → city search | Phone |
| ACC-02 | Search Canadian city (new fetch) | City persists; Today shows band from allowed set; last updated recent | Phone |
| ACC-03 | Decline location | City search still usable | Phone |
| ACC-04 | Offline empty cache | “No forecast just yet”; no score; no outlook | Phone |
| ACC-05 | Stale cache (60–180 min) | Score may show; **Stale** labelling visible | Phone |
| ACC-06 | Expired cache (>180 min) | “Forecast expired”; **no** score; **no** curve as current | Phone |
| ACC-07 | Missing/invalid hour fixture | Invalid-hour copy; no invented zero | Phone |
| ACC-08 | Refresh after stale | New fetch; last updated advances; stale clears when fresh | Phone |
| ACC-09 | Partial journal | Only reported part stored; History matches | Phone |
| ACC-10 | Export / reset | Share sheet JSON; reset clears journal | Phone |
| ACC-11 | About / privacy | Experimental + not medical advice visible | Phone |
| ACC-12 | Dark theme + font scale 1.3 | Score + curve readable | Phone |
| ACC-13 | Lake teal visual | Light teal / soft navy; Rough coral only; no cream/purple | Phone |
| ACC-14 | TalkBack | Core Today + journal actions announced | Phone |
| ACC-15 | Physical handset smoke | Install, fetch, journal once | Phone |
| ACC-W01 | Widget place / resize / reboot | Matches phone ranges; survives reboot | Widget deferred |
| ACC-R01 | Wear tile + quick log | Tile agrees; one journal row | Wear deferred |

## Allowed Today band labels

`Calm`, `Mild`, `Noticeable`, `Rough`, `Stormy` — use for live assertions instead
of numeric scores.

## Evidence

Store new screenshots under `docs/qa/evidence/<yyyy-mm-dd>/` with the ACC id in
the filename. Do not overwrite prior release evidence.
