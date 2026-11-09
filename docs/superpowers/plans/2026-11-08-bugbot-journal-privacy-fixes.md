# Bugbot Journal / Privacy Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the four Bugbot findings: unreachable Privacy, body taps during week playback, stale journal state after reset, and empty-chosen coerce crash.

**Architecture:** Wire About → Privacy navigation; gate `BodyFigure` input with `enabled`; remount Journal/History via `key(dataEpoch)` after wipe; harden autosave when `chosen` is empty.

**Tech Stack:** Kotlin, Compose Navigation, Material3.

## Global Constraints

- Canadian spelling in user-facing strings.
- Version bump (fix digit) for these bug fixes.
- No broad `Exception` catches.

---

### Task 1: Restore Privacy navigation

**Files:** `AboutScreens.kt`, `GrannysKneesApp.kt`, `strings_about.xml`

- [x] Add `onOpenPrivacy` to About; TextButton opens Privacy route
- [x] Navigate from About composable

### Task 2: Disable BodyFigure during playback

**Files:** `BodyFigure.kt`, `JournalScreens.kt`

- [x] Add `enabled` to `BodyFigure`; skip pointer/semantics when false
- [x] Pass `enabled = editing` on Report

### Task 3: Remount journal after reset + empty focus guard

**Files:** `GrannysKneesApp.kt`, `AboutScreens.kt`, `JournalScreens.kt`

- [x] `dataEpoch` + `key()` around Journal/History; About calls `onDataReset`
- [x] Skip autosave coerce when `chosen` is empty; guard `addOrFocus`
- [x] Bump version, assemble, install on Samsung
