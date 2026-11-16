# Maestro smoke flows (L3)

Long journeys for Granny's Knees. Assert **shape and honesty**, never a pinned
live index number. See [`docs/qa/fixture-freshness.md`](../docs/qa/fixture-freshness.md).

## Prerequisites

- Maestro CLI installed (`curl -Ls "https://get.maestro.mobile.dev" | bash`)
- Debug APK installed on emulator or device
- Prefer a **fresh** forecast for happy-path flows (clear app data then launch)

## Run

```bash
./gradlew installDebug
maestro test maestro/flows
```

## Allowed band words

`Calm` | `Mild` | `Noticeable` | `Rough` | `Stormy`

## Flows

| Flow | Covers |
| --- | --- |
| `smoke-first-launch.yaml` | UE-01, UE-09, UE-14 |
| `smoke-city-switch.yaml` | UE-02, UE-04 (shape only) |
| `smoke-dark-large-type.yaml` | DE-05 (manual font scale note in comments) |
