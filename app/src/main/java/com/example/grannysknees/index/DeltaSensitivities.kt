package com.example.grannysknees.index

import java.math.BigDecimal

/**
 * Multipliers applied to each 12-hour weather delta before the v2 signal.
 *
 * [BASELINE] leaves the deltas unchanged. Values above 1 amplify that factor;
 * 0 drops it from the signal.
 */
internal data class DeltaSensitivities(
    val pressure: BigDecimal,
    val humidity: BigDecimal,
    val temperature: BigDecimal,
) {
    /** True when every factor is exactly one. */
    fun isBaseline(): Boolean =
        pressure.compareTo(BigDecimal.ONE) == 0 &&
            humidity.compareTo(BigDecimal.ONE) == 0 &&
            temperature.compareTo(BigDecimal.ONE) == 0

    companion object {
        val BASELINE: DeltaSensitivities = DeltaSensitivities(
            pressure = BigDecimal.ONE,
            humidity = BigDecimal.ONE,
            temperature = BigDecimal.ONE,
        )

        /** Builds sensitivities from the About slider range (typically 0–2). */
        fun fromMultipliers(
            pressure: Float,
            humidity: Float,
            temperature: Float,
        ): DeltaSensitivities = DeltaSensitivities(
            pressure = BigDecimal.valueOf(pressure.toDouble()),
            humidity = BigDecimal.valueOf(humidity.toDouble()),
            temperature = BigDecimal.valueOf(temperature.toDouble()),
        )
    }
}
