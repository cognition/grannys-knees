# Google Play Console — draft declaration worksheet

**Draft only.** Owner must verify and submit in Play Console; no forms have been submitted. Reassess against final signed AAB at commit `33fe033` or later on `feature/play-store-readiness`.

| Console topic | Preliminary response / instruction | Status |
|---|---|---|
| Developer account | Personal developer account; verify identity and public contact requirements | Owner action |
| Distribution | Canada initially | Approved plan |
| App pricing | Free | Approved plan |
| Ads | No ads planned; verify no ad SDK and complete ads declaration accurately | Needs release audit |
| App access | No login planned; confirm no restricted content or access steps | Needs verification |
| Data safety | **Do not preselect “no data collected”.** The phone sends city search queries, forecast coordinates/timezone, and connection metadata to **Open-Meteo** (forecast + geocoding). Symptom journal entries, saved city, cache, and watch sync payloads stay on the device / Wear data layer (`allowBackup=false`; journal export only via user-initiated share sheet). Declare location-related data only if Console questionnaires require describing optional GPS use (approximate location for city fix). | Blocked pending owner audit on signed AAB |
| Health apps declaration | App concerns weather and joint discomfort and allows optional user symptom notes (local journal); answer health-related questions accurately; do not assume playful language exempts it | Owner/policy review |
| Medical claims | Experimental, non-clinical index; no diagnosis, treatment or individual flare prediction | Copy review |
| Target audience | Adult-oriented positioning proposed; select accurate age ranges in Console | Owner decision |
| Content rating | Complete questionnaire based on actual content (including health-adjacent journaling) | Owner action |
| Privacy policy | Public HTTPS HTML URL, accessible in app and store; must describe journal, watch sync, Open-Meteo, optional location, and backup | Pending publication |
| App category/tags | Select accurate non-misleading category and tags | Owner decision |
| Permissions | **INTERNET** (forecast and geocoding). **ACCESS_COARSE_LOCATION** and **ACCESS_FINE_LOCATION** declared for optional “use this phone's location”; no background location in manifest. Wearable messaging uses Google Play services Wearable APIs (no extra location permission for watch path). | Release audit on merged manifest |
| Location | Default path: manual Canadian city search without GPS. Optional: user-initiated request for device location to set city. Document in Data safety and permission rationale strings. | Needs implementation verification on release build |
| Wear OS | Companion `:wear` module distributed with phone app; data exchanged via Wearable Data Layer between paired devices only | Verify Play/Wear pairing requirements |
| Testing | Personal account may require 12 opted-in testers continuously for 14 days before production access | Confirm Console eligibility |

## Data safety — working notes (not submission text)

Use the signed AAB and `PRIVACY-DATA-FLOWS.md` as source. Typical declarations for this candidate:

| Data type (illustrative) | Collected by developer? | Leaves device? | Notes |
|---|---|---|---|
| Approximate/precise location (optional GPS) | No developer collection | User device → app only when permission granted; coordinates used for forecast requests like manual city | Declare per Console taxonomy if prompted; purpose: app functionality (weather index for chosen place) |
| Search query (city name) | No | HTTPS to Open-Meteo geocoding | Not retained by developer |
| Forecast request (lat/long/timezone) | No | HTTPS to Open-Meteo | Provider may log IP/metadata |
| Health info (symptom journal entries) | No developer collection | Stored locally in `journal.db`; export only via user-initiated share sheet | Encrypted in transit N/A for local DB; declare on-device health-related data handling honestly |
| App activity / diagnostics | No dedicated SDK in Gradle | OS/Play vitals as per platform | Verify release tree |

Android `allowBackup=false` is set (2026-11-10); Google Auto Backup does not copy app private data. Policy and Data safety wording may state local-first storage; user export via share sheet still transfers data the user chooses to share.

## Required sign-offs

- [ ] PUB05 technical reviewer signs off data flows
- [ ] Publisher approves privacy policy and public contact details
- [ ] Policy reviewer checks Health apps and medical-content wording
- [ ] Console declarations match the final AAB, not an older debug build
- [ ] Privacy policy URL works without login and appears in app and listing
- [ ] Evidence recorded privately for any account-specific requirements

## Sources to recheck before submitting

- https://support.google.com/googleplay/android-developer/answer/14738291
- https://support.google.com/googleplay/android-developer/answer/16679511
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/10144311
