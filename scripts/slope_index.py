"""Granny's Knees slope index: reference implementation.

Inputs are hourly readings, oldest first. All weights are provisional and
live in CONFIG so they can be tuned per user or per experiment.
"""
from dataclasses import dataclass


@dataclass(frozen=True)
class Config:
    baseline: float = 5.5254          # RC1 zero-change score
    pressure_w: float = 9 / 177.8     # 0.0506 per hPa of 12 h net change
    cool_w: float = 0.50              # per degC/h of cooling
    warm_w: float = 0.125             # per degC/h of warming
    humid_w: float = 0.25             # per percentage point/h
    gap_w: float = 0.25               # per degC/h of feels-like gap growth
    gap_clamp: float = 1.0            # gap term limited to +/- this
    deadband_t: float = 0.0           # |slope| below this treated as 0 (degC/h)
    deadband_h: float = 0.0           # same for humidity (pp/h)
    deadband_g: float = 0.0           # same for gap slope (degC/h)
    lo: float = 1.0
    hi: float = 10.0


def clamp(x, lo, hi):
    return max(lo, min(hi, x))


def slope4(x):
    """Least-squares slope per hour through 4 hourly points, oldest first."""
    x0, x1, x2, x3 = x
    return (-3 * x0 - x1 + x2 + 3 * x3) / 10


def dead(v, band):
    return 0.0 if abs(v) < band else v


def score(temp4, hum4, feels4, pressure13, cfg=Config()):
    """temp4, hum4, feels4: last 4 hourly values (oldest first).
    pressure13: last 13 hourly pressures in hPa (oldest first, index 0 is 12 h ago).
    Returns the index clamped to [lo, hi]."""
    m_t = dead(slope4(temp4), cfg.deadband_t)
    m_h = dead(slope4(hum4), cfg.deadband_h)
    gap = [max(0.0, t - f) for t, f in zip(temp4, feels4)]
    g = dead(slope4(gap), cfg.deadband_g)
    dp12 = pressure13[-1] - pressure13[0]

    s = cfg.baseline
    s -= cfg.pressure_w * dp12
    s += cfg.cool_w * max(0.0, -m_t)
    s += cfg.warm_w * max(0.0, m_t)
    s += cfg.humid_w * m_h
    s += clamp(cfg.gap_w * g, -cfg.gap_clamp, cfg.gap_clamp)
    return clamp(s, cfg.lo, cfg.hi)


def accel(slope_now, slope_1h_ago):
    """Optional second derivative (rate of the rate of change). Not in the score."""
    return slope_now - slope_1h_ago


if __name__ == "__main__":
    flat_p = [1013.0] * 13
    steady = [20, 20, 20, 20]
    # 1 steady
    assert round(score(steady, [50] * 4, steady, flat_p), 2) == 5.53
    # 2 cooling 2 C/h, humidity +3 pp/h
    t = [26, 24, 22, 20]
    h = [41, 44, 47, 50]
    assert round(score(t, h, t, flat_p), 2) == 7.28
    # 3 warming 2 C/h, humidity -3 pp/h
    t = [14, 16, 18, 20]
    h = [59, 56, 53, 50]
    assert round(score(t, h, t, flat_p), 2) == 5.03
    # 4 furnace stops: air flat, feels-like falling so gap grows 1.5 C/h
    t = [20, 20, 20, 20]
    gap = [0, 1.5, 3.0, 4.5]
    f = [20 - g for g in gap]
    assert abs(slope4(gap) - 1.5) < 1e-9
    assert round(score(t, [50] * 4, f, flat_p), 2) == 5.90
    # pressure: +10 hPa over 12 h lowers score by 0.506
    p = [1013 + i * 10 / 12 for i in range(13)]
    assert round(score(steady, [50] * 4, steady, p), 3) == round(5.5254 - 0.506, 3)
    print("ok", round(score(steady, [50] * 4, steady, p), 3))
