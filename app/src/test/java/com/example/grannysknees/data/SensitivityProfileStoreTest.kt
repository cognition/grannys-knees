package com.example.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Persistence for personal index sensitivity sliders.
 */
class SensitivityProfileStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `missing file reads as defaults`() {
        val file = temporaryFolder.newFile("missing-sensitivity.json").also { it.delete() }
        val store = SensitivityProfileStore(file)

        assertTrue(store.read().isDefault())
        assertTrue(store.readFeelCalibrationEnabled())
    }

    @Test
    fun `round trips a custom profile`() {
        val store = SensitivityProfileStore(temporaryFolder.newFile("sensitivity.json"))
        val profile = SensitivityProfile(pressure = 1.5f, humidity = 0.5f, cold = 2f)

        store.write(profile)

        assertEquals(profile, store.read())
        assertTrue(store.readFeelCalibrationEnabled())
    }

    @Test
    fun `missing feelCalibrationEnabled key defaults to on`() {
        val file = temporaryFolder.newFile("legacy-sensitivity.json")
        file.writeText(
            """{"pressure":1.0,"humidity":1.0,"cold":1.0}""",
            Charsets.UTF_8,
        )
        val store = SensitivityProfileStore(file)

        assertTrue(store.readFeelCalibrationEnabled())
    }

    @Test
    fun `explicit feel opt-out persists`() {
        val store = SensitivityProfileStore(temporaryFolder.newFile("feel-opt.json"))
        store.writeFeelCalibrationEnabled(false)

        assertEquals(false, store.readFeelCalibrationEnabled())
        assertTrue(store.read().isDefault())
    }
}
