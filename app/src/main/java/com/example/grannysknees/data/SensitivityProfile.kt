package com.example.grannysknees.data

import com.example.grannysknees.index.DeltaSensitivities

/**
 * User sensitivity multipliers for the personal index curve.
 *
 * Each factor is clamped to [MIN]–[MAX]. [DEFAULT] (all 1) matches the shared
 * `index-v2` Weather line. Factors scale the matching 12-hour weather delta
 * before the signal is mapped onto 1–10.
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
         * User-facing real-feel scale on Settings sliders.
         * 0 = no adjustment (standard multiplier); 10 = strongest boost.
         */
        const val DISPLAY_MIN: Float = 0f
        const val DISPLAY_MAX: Float = 10f
        /** Nine steps between 0 and 10 → whole-number stops. */
        const val DISPLAY_STEPS: Int = 9

        val DEFAULT: SensitivityProfile = SensitivityProfile()

        /**
         * Maps a stored multiplier onto the 0–10 scale.
         * Values at or below the default read as 0 (no adjustment).
         */
        fun toDisplayScale(multiplier: Float): Float {
            val safe = multiplier.coerceIn(MIN, MAX)
            if (safe <= DEFAULT_SENSITIVITY) return DISPLAY_MIN
            val span = MAX - DEFAULT_SENSITIVITY
            return ((safe - DEFAULT_SENSITIVITY) / span) * DISPLAY_MAX
        }

        /**
         * Maps a 0–10 rating to a stored multiplier.
         * 0 → default (no adjustment); 10 → maximum boost.
         */
        fun fromDisplayScale(display: Float): Float {
            val rating = display.coerceIn(DISPLAY_MIN, DISPLAY_MAX)
            val span = MAX - DEFAULT_SENSITIVITY
            return DEFAULT_SENSITIVITY + (rating / DISPLAY_MAX) * span
        }
    }
}
