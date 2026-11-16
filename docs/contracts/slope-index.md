# Granny's Knees: Slope Index (handoff spec)

Implement a weather-sensitivity score from 1 to 10 for each hour. It scores how conditions are *changing right now*, not their absolute level. `slope_index.py` is the reference implementation and its self-tests are the acceptance tests. Port it to the app's language and keep the maths and constants identical.

## Idea in one paragraph

Skin thermoreceptors respond to temperature level and to its rate of change, so the same 19 °C feels warm when it is rising or steady and cool when it is falling. The index therefore starts from a neutral baseline and adds terms for the **slope** (first derivative, change per hour) of temperature, humidity and a feels-like gap, plus the existing 12-hour pressure change. Cooling counts more than warming. "Acceleration" (second derivative, the change in slope) is an optional experiment, not part of the score.

## Inputs (hourly, oldest first, same station and timezone, no gaps)

| Name | Needed | Unit | Notes |
|---|---|---|---|
| `temp4` | last 4 hourly air temperatures | °C | ending at the scored hour |
| `hum4` | last 4 hourly relative humidities | % | slope is in percentage points per hour |
| `feels4` | last 4 hourly feels-like temperatures | °C | from the weather feed |
| `pressure13` | last 13 hourly pressures | hPa | index 0 is 12 h ago, index 12 is now |

## Algorithm

1. **Slope** of any 4 points `x0..x3` (least-squares line, oldest to newest), per hour:
   `m = (-3*x0 - x1 + x2 + 3*x3) / 10`
2. `mT = slope(temp4)`, `mH = slope(hum4)`.
3. **Feels-like gap** per hour: `gap_i = max(0, temp_i - feels_i)`. Then `g = slope(gap)`. This is heat loss growing faster than air temperature shows (the "furnace stopped" case).
4. `dP12 = pressure13[12] - pressure13[0]` (hPa).
5. Score before clamping:
   ```
   S = 5.5254
       - 0.0506 * dP12
       + 0.50  * max(0, -mT)      # cooling
       + 0.125 * max(0,  mT)      # warming (4x weaker than cooling)
       + 0.25  * mH
       + clamp(0.25 * g, -1, +1)
   ```
6. `index = clamp(S, 1, 10)`.

`5.5254` is the zero-change baseline inherited from the earlier RC1 model, and `0.0506 = 9/177.8` is RC1's pressure weight.

## Constants to expose as config (not hard-coded)

`baseline, pressure_w, cool_w, warm_w, humid_w, gap_w, gap_clamp`, and three deadbands (`deadband_t`, `deadband_h`, `deadband_g`, default 0). A deadband zeroes a slope whose absolute value is below it. Candidate values to test are 0.25 to 0.5 °C/h. All weights are provisional and are to be tuned, ideally per user.

## Acceptance tests (all pressure flat, 1013 hPa)

| Scenario | mT | mH | g | Expected |
|---|---|---|---|---|
| Steady | 0 | 0 | 0 | 5.53 |
| Cooling with rising humidity | -2 | +3 | 0 | 7.28 |
| Warming with drying | +2 | -3 | 0 | 5.03 |
| Furnace stops, air flat | 0 | 0 | +1.5 | 5.90 |
| Steady air, pressure up 10 hPa over 12 h | 0 | 0 | 0 | 5.019 |

Run `python3 slope_index.py` to see them pass.

## Edge cases to handle

- **Missing hours:** if any of the 4 points (or either pressure endpoint) is missing, do not score. Return null and show last known value as stale.
- **Units:** humidity slope is percentage points per hour, not a ratio. Pressure must be hPa (convert from kPa or inHg upstream).
- **Feels-like absent:** if the feed has no feels-like, set `g = 0`. The gap term then drops out.
- **Outdoor data only:** the feed is outdoor, so indoor heating cycles are invisible. Do not claim otherwise in the UI.
- **Pressure sign is an open question.** The evidence disagrees on whether rising or falling pressure raises pain, so keep `pressure_w` configurable and its sign flippable without a code change.

## Optional experiment: acceleration

`accel = slope_now - slope_1h_ago` (units per hour squared). It measures whether a cooling is speeding up or easing off. It is **not** in the score, because no study supports a second-derivative term and it amplifies noise in hourly data. Implement it behind a feature flag, log it alongside the score, and only add a weight if it improves separation in testing.

## Logging for later validation

Store per hour: raw inputs, `mT, mH, g, dP12`, the score, config version, and (if the user logs it) pain rating with a timestamp. The goal is to compare variants (level-only, RC1, slope-only, slope plus small level term, each with and without acceleration) on bad days versus matched control days, using held-out weeks or users so tuning does not flatter the result.

## Do not

- Change constants silently. Version the config and log which version produced each score.
- Tune and evaluate on the same data.
- Present the score as medical advice. It is a weather-change index, unvalidated against symptoms so far.
