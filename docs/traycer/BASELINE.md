# T00 baseline: repository reality check

**Status:** verified (independent Codex review completed 2026-11-04; **owner accepted 2026-11-04** — Batch B approved)  
**Date (UTC):** 2026-11-04T16:02:47Z  
**Branch:** `traycer/grannys-knees-zealous-panda` @ `9b2ee33`  
**Host:** `alien.aeo3.io` (Linux x86_64)  
**Working tree:** `/home/rbrooker/.traycer/worktrees/cognition__grannys-knees/traycer-grannys-knees-zealous-panda-c83ef8810b4d`  
**Owned path for this task:** `docs/traycer/BASELINE.md` only (no application code changed)

## Verdict

There is **no native Android application** in this repository. The only runnable product is a browser HTML prototype (`index.html`). Requirements, architecture and Traycer backlog documents describe the target product; they are not evidence of implementation. Approved PNG branding assets are **not** in the tree. Android SDK tooling exists at `/home/rbrooker/Android/Sdk` on this host, but there is no Gradle project to build and neither `ANDROID_HOME` nor `ANDROID_SDK_ROOT` was set in the independent review process.

## Capability matrix

| Capability | State | Evidence |
| --- | --- | --- |
| Native Android / Kotlin / Compose app | **Not implemented** | `find` for `*.kt`, `*.kts`, `*.gradle`, `AndroidManifest.xml`, `gradlew` → **0 files** |
| Gradle wrapper / AGP / modules | **Not implemented** | No `settings.gradle*`, `build.gradle*`, `libs.versions.toml`, `app/`, `core/` |
| CI (GitHub Actions) | **Not implemented** | No `.github/` directory |
| Unit / UI / instrumented tests | **Not implemented** | No test sources or runners |
| Weather/index contracts + fixtures | **Not implemented** | No `docs/contracts/` |
| Room / DataStore / widgets / Wear OS | **Not implemented** | Absent from tree; described only in `docs/architecture.md` |
| HTML Open-Meteo prototype | **Partial** (browser only) | `index.html`; 0–100 score; daily-mean pressure delta; fixed full-entry symptom form |
| Geocoding + forecast fetch | **Partial** (prototype) | Live `curl` to Open-Meteo geocoding and forecast endpoints returned HTTP 200 for the variables used |
| Symptom journal (product rules) | **Not implemented** | Prototype always writes pain+stiffness; no partial/confirmed-unchanged semantics |
| Append-only observation + sensor snapshot | **Not implemented** | Prototype stores monolithic localStorage entries |
| 0–10 experimental index + hourly low/high | **Not implemented** | Prototype score 0–100; seven-day cards from daily aggregates, not hourly index extrema |
| Approved PNG branding | **Not implemented** | No `*.png` under `assets/`; README states PNGs pending |
| SVG logo recreations | **Present, not approved** | `assets/logo-dark.svg`, `assets/logo-light.svg`; README forbids treating as approved |
| Traycer docs (instructions + backlog) | **Implemented** (docs only) | `docs/traycer/AGENT-INSTRUCTIONS.md`, `EXECUTION-BACKLOG.md` |
| Delivery / architecture docs | **Implemented** (requirements) | `docs/delivery-plan.md`, `docs/architecture.md` both label native app as not built |

## Tracked file inventory (11 files)

```
.gitignore
LICENSE
README.md
assets/README.md
assets/logo-dark.svg
assets/logo-light.svg
docs/architecture.md
docs/delivery-plan.md
docs/traycer/AGENT-INSTRUCTIONS.md
docs/traycer/EXECUTION-BACKLOG.md
index.html
```

`.gitignore` already anticipates Android/Gradle (`local.properties`, `*.apk`, `.gradle/`, etc.) but no Android sources exist yet.

## HTML prototype findings (`index.html`)

| Topic | Observed behaviour | Product gap |
| --- | --- | --- |
| Index scale | `score()` clamps to **0–100** | Required **0–10** |
| Weather inputs | `current=temperature_2m,relative_humidity_2m,surface_pressure`; daily means for outlook | Need **hourly** inputs; daily low/high from hourly **index** scores |
| Pressure trend | Day *i* uses `surface_pressure_mean[i] - mean[i-1]` (forecast daily means); today uses `delta=0` | Architecture forbids using future daily means as observed pressure change |
| Outlook | Seven day cards, single daily index each | Five-day **low/high** index from hourly curve |
| Journal | Always saves pain, stiffness, joints, notes | Only explicitly changed / confirmed-unchanged measurements |
| Storage | `localStorage` key `grannys-knees.entries.v1` | Room + provenance snapshots |
| Export | CSV + JSON download; clear-all | Needed later; prototype export is browser-only |
| Insights | Pearson-like pain/index correlation after ≥5 linked entries | Observational only; OK as prototype behaviour |
| Branding in UI | Text header only; no logo assets referenced | Approved PNGs not committed |

### Open-Meteo variable support (live probe, independently reproduced 2026-11-04)

Commands and results:

```sh
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' 'https://geocoding-api.open-meteo.com/v1/search?name=Toronto&count=10&format=json'
# HTTP 200; 10 results; first result Toronto, Ontario, CA

curl -sS -o /dev/null -w 'HTTP %{http_code}\n' 'https://api.open-meteo.com/v1/forecast?latitude=43.6532&longitude=-79.3832&current=temperature_2m,relative_humidity_2m,surface_pressure&timezone=America%2FToronto'
# HTTP 200; current object contains all three requested variables

curl -sS -o /dev/null -w 'HTTP %{http_code}\n' 'https://api.open-meteo.com/v1/forecast?latitude=43.6532&longitude=-79.3832&daily=temperature_2m_max,relative_humidity_2m_mean,surface_pressure_mean&timezone=America%2FToronto&forecast_days=7'
# HTTP 200; 7 daily points containing all three requested variables

curl -sS -o /dev/null -w 'HTTP %{http_code}\n' 'https://api.open-meteo.com/v1/forecast?latitude=43.6532&longitude=-79.3832&hourly=temperature_2m,relative_humidity_2m,surface_pressure&timezone=America%2FToronto&forecast_days=1'
# HTTP 200; 24 hourly points containing all three requested variables

curl -sS -o /dev/null -w 'HTTP %{http_code}\n' 'https://api.open-meteo.com/v1/forecast?latitude=43.6532&longitude=-79.3832&hourly=pressure_msl,dewpoint_2m&timezone=America%2FToronto&forecast_days=1'
# HTTP 200; 24 hourly points containing both variables (not used by prototype)
```

The values are live and therefore time-dependent; the stable evidence is HTTP success plus the returned field names and point counts.

**Conclusion for T02:** The prototype’s named variables are supported by Open-Meteo. The product problem is **how** they are used (daily means / missing hourly index extrema / pressure provenance), not that the variable names are rejected by the API. T02 must still pin versioned contracts against authoritative docs and fixtures; this probe is not that contract.

## Branding assets

| Asset | Present? | Notes |
| --- | --- | --- |
| `assets/logo-dark.png` | No | Canonical approved (per README) — missing |
| `assets/logo-light.png` | No | Canonical approved (per README) — missing |
| `assets/logo-dark.svg` | Yes (1850 B) | SVG recreation; no `<text>` lettering; aria-label mentions product name |
| `assets/logo-light.svg` | Yes (1844 B) | Same |
| `assets/README.md` | Yes | Still describes SVGs for Android drawables; conflicts with root README “not approved” policy |

**Blocker for branding completeness:** approved PNGs must be committed before app or README can claim final branding. Do not ship SVG recreations as approved artwork.

## Build / test commands attempted

| Command | Result |
| --- | --- |
| `./gradlew test assembleDebug` | **Failed:** `./gradlew: No such file or directory` |
| `find . -path './.git' -prune -o -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.gradle' -o -name 'AndroidManifest.xml' -o -name 'gradlew' \) -print` | **0 files** |
| `find . -path './.git' -prune -o -type f -path './.github/workflows/*' -print` | **0 files** (`.github/` absent) |
| `find . -path './.git' -prune -o -type f \( -path '*/src/test/*' -o -path '*/src/androidTest/*' -o -path './docs/contracts/*' \) -print` | **0 files** |
| `/home/rbrooker/Android/Sdk/emulator/emulator -list-avds` | **Empty** (emulator package installed; no AVDs) |

**Device verification:** not verified on device (no app, no AVD).

## Host environment (relevant to T01)

| Item | Value |
| --- | --- |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | **Unset** in the independent review process |
| SDK root present on disk | `/home/rbrooker/Android/Sdk` |
| Platforms installed | `android-37.0` |
| Build-tools | `36.0.0` |
| Platform-tools / cmdline-tools / emulator | Installed (sdkmanager list) |
| NDK / emulator system images | Not installed |
| `java` | OpenJDK **25.0.4.1** (suse) |
| `gradle` / `kotlin` on PATH | **Not found** (expect Gradle Wrapper in T01) |
| Node / Python | Present (not required for Android MVP) |
| Git remote | `git@github.com:cognition/grannys-knees.git` |

**T01 environmental notes:** SDK packages are present, but T01 must configure SDK discovery (`ANDROID_HOME`, `ANDROID_SDK_ROOT` or an untracked `local.properties`) before Gradle can use them. Java 25 is newer than many AGP toolchains historically tested; T01 must pick a compatible JDK/AGP pairing and report honestly if the toolchain refuses JDK 25. No emulator system image or AVD is installed, so emulator smoke is blocked until an image is installed and an AVD is created.

## Blockers and decisions (for Batch B)

1. **No Android project** — T01 must create the skeleton; nothing to “fix” today.
2. **SDK discovery and emulator setup** — SDK packages exist at a known path, but the environment variables are unset and there is no emulator system image or AVD. T01 can build after configuring SDK discovery; device smoke needs additional setup.
3. **Approved PNGs missing** — branding and README picture element remain incomplete until assets are uploaded (deferred decision in backlog).
4. **Index formula unsettled** — prototype 0–100 heuristic must not be treated as the native 0–10 scorer; T02 defines contracts; T04 implements.
5. **Package / application ID** — still a deferred decision and must be resolved or explicitly chosen by T01.
6. **No CI / no tests** — T01 owns first CI workflow and unit-test runner.
7. **Independent Codex baseline review** — corrections applied to evidence wording; owner accepted T00 on 2026-11-04.

## Explicit non-claims

- No `assembleDebug`, unit tests, UI tests or emulator runs succeeded (none exist to run).
- No Wear OS, widget, Room or scoring module exists.
- Open-Meteo live probe confirms API reachability and variable acceptance for sampled names; it does **not** validate product scoring or timezone edge cases.

## Recommended next batch (after this baseline is accepted)

- **T02** (Claude + Codex review): `docs/contracts/weather-index.md` + fixtures — separate worktree; no production scorer.

## T00 standard output

| Field | Value |
| --- | --- |
| Status | **verified** (owner accepted 2026-11-04; Codex review evidence corrections incorporated) |
| Summary | Repo is docs + HTML prototype only; native Android absent; SDK packages present at a known path but environment discovery unset; approved PNGs missing |
| Changed paths | `docs/traycer/BASELINE.md` |
| Commit/PR | Not created yet (BASELINE still untracked until owner asks to commit) |
| Tests run | Exact Open-Meteo geocoding/forecast `curl` probes (HTTP 200); repository marker searches (0 files); `./gradlew test assembleDebug` confirmed unavailable |
| Tests not run | All Android builds/tests; emulator |
| Demonstration | Open `index.html` in a browser for prototype only |
| Integration gate | N/A for T00 |
| Follow-on | **Batch B in progress:** T01 + T02 parallel worktrees |
