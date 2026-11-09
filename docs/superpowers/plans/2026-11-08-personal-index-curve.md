# Personal Index Curve Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dual Today curve (baseline + personal) with pressure/humidity/cold sensitivity sliders.

**Architecture:** `SensitivityProfile` → renormalized `IndexWeights`; scorer accepts weights; outlook maps both hour series onto `DayCurve`; About edits profile; Today re-presents from cache on profile change.

**Tech Stack:** Kotlin, Compose Material3 Slider, JUnit4, Canadian spelling.

## Global Constraints

- Canadian spelling in user-facing strings.
- Feature version bump for this change.
- Journal ratings never feed the scorer in this slice.
- Baseline `index-v1` fixtures must still pass with default weights.

---

### Task 1: Profile + weighted scorer

- [x] `SensitivityProfile`, store, `IndexWeights`, scorer overload
- [x] Unit tests

### Task 2: Outlook dual series + chart

- [x] `DayCurve.personalSegments`, mapper, draw second line + legend

### Task 3: About sliders + wire-through

- [x] About UI, MainActivity/App wiring, version bump, install
