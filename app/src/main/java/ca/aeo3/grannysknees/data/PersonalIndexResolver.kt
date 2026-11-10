package ca.aeo3.grannysknees.data

/**
 * Resolves whether Personal real-feel runs and which multipliers to use.
 *
 * Slider-displayed [adj] stays the Adj base; Feel opt-in never overwrites it.
 */
internal object PersonalIndexResolver {

    /**
     * Effective Personal profile, or `null` when Personal must stay hidden.
     *
     * @param adj slider / stored adjustment profile
     * @param feelOptIn Feel→real-feel switch (default on)
     * @param samples persisted Feel calibration time-points
     */
    fun effectiveProfile(
        adj: SensitivityProfile,
        feelOptIn: Boolean,
        samples: List<FeelCalibrationSample>,
    ): SensitivityProfile? {
        val adjUsed = !adj.isDefault()
        val feelReady = feelOptIn && FeelCalibration.isUnlocked(samples)
        return when {
            adjUsed && feelReady -> {
                val suggested = FeelCalibration.suggest(samples)
                FeelCalibration.blend(adj.clamped(), suggested)
            }
            adjUsed -> adj.clamped()
            feelReady -> {
                val suggested = FeelCalibration.suggest(samples)
                FeelCalibration.blend(SensitivityProfile.DEFAULT, suggested)
            }
            else -> null
        }
    }
}
