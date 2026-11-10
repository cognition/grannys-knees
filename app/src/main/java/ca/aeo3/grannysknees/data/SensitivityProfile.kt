package ca.aeo3.grannysknees.data

import ca.aeo3.grannysknees.index.DeltaSensitivities

/**
 * How much each weather factor influences the personal real-feel curve.
 *
 * This is influence (does pressure / humidity / temperature affect you?), not
 * a high-vs-low or hot-vs-cold preference. Each factor is clamped to
 * [MIN]–[MAX]. [DEFAULT] (all 1) matches the shared `index-v2` Weather line.
 */
internal data class SensitivityProfile(
    val pressure: Float = DEFAULT_SENSITIVITY,
    val humidity: Float = DEFAULT_SENSITIVITY,
    val cold: Float = DEFAULT_SENSITIVITY,
) {
    /** Profile with each factor clamped into the allowed range. */
    fun clamped(): SensitivityProfile = SensitivityProfile(
        pressure = pressure.coerceIn(MIN, MAX),
        humidity = humidity.coerceIn(MIN, MAX),
        cold = cold.coerceIn(MIN, MAX),
    )

    /** Delta multipliers for the personal `index-v2` series. */
    fun toDeltaSensitivities(): DeltaSensitivities {
        val safe = clamped()
        return DeltaSensitivities.fromMultipliers(
            pressure = safe.pressure,
            humidity = safe.humidity,
            temperature = safe.cold,
        )
    }

    /** True when every factor is the neutral default. */
    fun isDefault(): Boolean {
        val safe = clamped()
        return nearDefault(safe.pressure) &&
            nearDefault(safe.humidity) &&
            nearDefault(safe.cold)
    }

    private fun nearDefault(value: Float): Boolean =
        kotlin.math.abs(value - DEFAULT_SENSITIVITY) <= DEFAULT_EPSILON

    companion object {
        const val MIN: Float = 0f
        const val MAX: Float = 2f
        const val DEFAULT_SENSITIVITY: Float = 1f
        private const val DEFAULT_EPSILON: Float = 0.0001f

        /**
         * User-facing influence scale on Settings sliders.
         * 0 = no influence adjustment (standard); 10 = strongest influence.
         */
        const val DISPLAY_MIN: Float = 0f
        const val DISPLAY_MAX: Float = 10f
        /** Nine steps between 0 and 10 → whole-number stops. */
        const val DISPLAY_STEPS: Int = 9

        val DEFAULT: SensitivityProfile = SensitivityProfile()

        /**
         * Maps a stored multiplier onto the 0–10 influence scale.
         * Values at or below the default read as 0 (no influence).
         */
        fun toDisplayScale(multiplier: Float): Float {
            val safe = multiplier.coerceIn(MIN, MAX)
            if (safe <= DEFAULT_SENSITIVITY) return DISPLAY_MIN
            val span = MAX - DEFAULT_SENSITIVITY
            return ((safe - DEFAULT_SENSITIVITY) / span) * DISPLAY_MAX
        }

        /**
         * Maps a 0–10 influence rating to a stored multiplier.
         * 0 → default (no influence); 10 → strongest influence.
         */
        fun fromDisplayScale(display: Float): Float {
            val rating = display.coerceIn(DISPLAY_MIN, DISPLAY_MAX)
            val span = MAX - DEFAULT_SENSITIVITY
            return DEFAULT_SENSITIVITY + (rating / DISPLAY_MAX) * span
        }
    }
}
