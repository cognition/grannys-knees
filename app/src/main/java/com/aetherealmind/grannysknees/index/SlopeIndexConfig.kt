package com.aetherealmind.grannysknees.index

/**
 * Versioned weights for the slope-index scorer (`docs/contracts/slope-index.md`).
 *
 * Defaults match `scripts/slope_index.py`. Do not change constants silently —
 * bump [configVersion] when tuning.
 */
internal data class SlopeIndexConfig(
    val baseline: Double = 5.5254,
    val pressureW: Double = 9.0 / 177.8,
    val coolW: Double = 0.50,
    val warmW: Double = 0.125,
    val humidW: Double = 0.25,
    val gapW: Double = 0.25,
    val gapClamp: Double = 1.0,
    val deadbandT: Double = 0.0,
    val deadbandH: Double = 0.0,
    val deadbandG: Double = 0.0,
    val lo: Double = 1.0,
    val hi: Double = 10.0,
    val configVersion: String = CONFIG_VERSION,
) {
    companion object {
        const val CONFIG_VERSION: String = "slope-config/1.0.0"

        val DEFAULT: SlopeIndexConfig = SlopeIndexConfig()
    }
}
