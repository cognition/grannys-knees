package com.aetherealmind.grannysknees.index

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Personal influence multipliers for the slope-index Personal curve.
 */
class IndexWeightsTest {
    @Test
    fun `default multipliers are baseline`() {
        val sensitivities = DeltaSensitivities.fromMultipliers(1f, 1f, 1f)

        assertTrue(sensitivities.isBaseline())
    }

    @Test
    fun `amplified pressure is not baseline`() {
        val sensitivities = DeltaSensitivities.fromMultipliers(2f, 1f, 1f)

        assertFalse(sensitivities.isBaseline())
    }
}
