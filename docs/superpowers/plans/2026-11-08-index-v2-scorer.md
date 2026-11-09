# Index-v2 Scorer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Replace live Weather scoring with experimental 12-hour delta index (`weather-index-v2.md`); update About, fixtures, and Open-Meteo lead-in.

**Architecture:** Scorer compares each hour to t−12h (UTC epochs): `signal = ΔRH − ΔP − ΔT`, then `clamp(1 + 9*(signal+89.4)/177.8, 1, 10)`. Personal line multiplies each Δ by sensitivity before the same map. Fetch `past_hours ≥ 12`.

**Tech Stack:** Kotlin BigDecimal scorer, Open-Meteo, JUnit fixtures, Canadian About copy.

## Global Constraints

- Canadian spelling in UI.
- Feature version bump.
- Journal still never feeds the scorer.
- First 12 hours (no t−12 sample) stay UNAVAILABLE.

---

### Task 1: Contract + scorer + fixtures

- [x] Sync `docs/contracts/weather-index-v2.md`
- [x] Rewrite `WeatherIndexScorer` for v2; adapt personal sensitivities
- [x] Update fixtures / tests; `past_hours` ≥ 12

### Task 2: About + version + install

- [x] Update calculation blurb for 12-hour method
- [x] Bump version, assemble, install Samsung
