# Granny's Knees: delivery plan

Status: **Proposed execution baseline**. This plan does not imply that native Android functionality exists.

## Product definition

A playful, experimental, research-informed **weather-based Arthritis Index**, 0–10, with an hourly graph and five-day daily low/high, like a temperature forecast. Optional, privacy-first symptom reports can help users explore correlations. Correlation is not causation; the index is not medical advice or a clinically validated prediction.

**Out of scope:** exercise programmes, fitness goals, rehabilitation, coaching, mandatory accounts, medical diagnosis, treatment advice, cloud symptom storage for MVP, premature AI personalization.

**Identity:** Granny's Knees. Approved branding is the user's supplied black/white PNG pair, **not** the earlier inaccurate SVG recreations. Do not use those SVGs as approved artwork. Approved PNGs are not yet in the repository; see README.

## Technical direction

- Kotlin, Jetpack Compose, Material 3; native Android first.
- Shared pure Kotlin weather/index domain, with deterministic tests.
- Open-Meteo hourly variables, including temperature, relative humidity and surface pressure. Derive daily index low/high from hourly index scores, never from a daily average.
- Repository/cache with explicit data freshness and offline states; Room for local observation history, DataStore for preferences.
- WorkManager for refresh; Glance for Android home-screen widgets; Wear OS Tiles and optional Complications; Wear Data Layer for synchronization.
- Local symptom history by default; explicit permission and provenance for optional sensor measurements.
- Scoring must be labelled experimental and its variables, weights, limits and version documented. No causal inference.

## Release sequence and gates

| Release | Workstreams | Verifiable gate |
| --- | --- | --- |
| R0 Foundation | WS-01 Android foundation, WS-02 weather/index engine | Reproducible debug build, deterministic scoring tests, valid forecast fixture |
| R1 Weather MVP | WS-03 Today/Forecast, WS-04 Android widgets | Phone shows current index, 24-hour chart, five-day low/high; widget agrees with app |
| R2 Journal | WS-05 partial symptom reports, WS-06 snapshots/history | Only changed or explicitly confirmed symptoms saved; snapshot provenance and export verified |
| R3 Wear/release | WS-07 Wear OS, WS-08 QA/release | Watch tile/quick log sync, offline and duplicate handling, accessibility and release checks |

**No calendar commitment** until the initial Android build and integration throughput are measured. Parallel agents shorten isolated tasks, not device testing or integration gates.

## Workstream scope

### WS-01 Android foundation
Gradle/AGP/Kotlin compatibility, Compose application shell, package structure, unit tests, CI, version catalogue, README build instructions, emulator smoke test. Keep module count minimal.

### WS-02 Weather/index engine
Weather API client, city selection/geocoding contract, hourly forecast model, validated parsing and timezone handling, deterministic versioned 0–10 scoring, daily min/max aggregation, cache/freshness, error states. Separate observed weather from forecasts.

### WS-03 Forecast UX
Today index, hourly graph, five-day high/low index, selected city, last refresh, experimental disclaimer, light/dark themes, Granny commentary. Accessible 0–10 labels.

### WS-04 Android widgets
Compact current-index, expanded hourly graph, five-day low/high; Glance; background updates; stale-data display; correct refresh and device restart behaviour.

### WS-05 Symptom journal
Body part, side, symptom, 0–10 rating. Only fields actively changed generate observations. Untouched means **not reported**; zero is an explicit rating. Optional per-field or global confirmation creates distinguishable `confirmed_unchanged` observations. Preserve prior timestamps.

### WS-06 Snapshots/history
At report time, link a timestamped environmental/device snapshot; each reading has source, unit, captured time, and measured/retrieved/unavailable status. No fabricated sensor values. Local history, correction strategy, export/delete, descriptive associations only.

### WS-07 Wear OS
Current index tile, optional complication, quick partial symptom report, phone/watch sync with idempotency and offline retry; no requirement to report all symptoms.

### WS-08 Release quality
Accessibility, permission review, privacy statement, store disclosures, automated tests, emulator and real-device verification, reproducible signed-release process without committing secrets.

## Required product checks

1. Weather-only use is fully functional without creating an account or entering symptoms.
2. Five-day **index** low/high are calculated from hourly index values, not temperature or average index.
3. A left-foot ache update does not create new observations for knees, hands, or other symptoms.
4. Confirmed unchanged values are tagged distinctly from changes; omitted values are not copied.
5. No sensor reading is represented as zero when unavailable.
6. Humour is visible, but medical limitations and correlation-not-causation are accessible.
7. The index and symptom pain ratings are different concepts, even though both use 0–10.
8. No exercise, goals, or coaching feature is introduced.

## Source-of-truth policy

GitHub code and test results establish implementation status. Documents define requirements, not completed capabilities. Each release gate needs a build, automated tests and a reproducible demo.
