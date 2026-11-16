package com.aetherealmind.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

/** Persistence for Feel calibration time-points. */
class FeelCalibrationStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `missing file reads empty`() {
        val file = temporaryFolder.newFile("missing-feel.json").also { it.delete() }
        assertTrue(FeelCalibrationStore(file).read().isEmpty())
    }

    @Test
    fun `recordSave skips when weather window is missing`() {
        val store = FeelCalibrationStore(temporaryFolder.newFile("feel.json"))
        store.recordSave(
            observedAt = Instant.parse("2026-11-09T12:00:00Z"),
            observationId = "o1",
            ratings = listOf(4),
            capture = FeelWeatherCapture(
                weatherWindowMean = null,
                deltaPressure = null,
                deltaHumidity = null,
                deltaTemperature = null,
            ),
        )
        assertTrue(store.read().isEmpty())
    }

    @Test
    fun `round trips a usable sample with slope schema`() {
        val store = FeelCalibrationStore(temporaryFolder.newFile("feel-round.json"))
        store.recordSave(
            observedAt = Instant.parse("2026-11-09T12:00:00Z"),
            observationId = "o1",
            ratings = listOf(4, 6),
            capture = FeelWeatherCapture(
                weatherWindowMean = 5.5,
                deltaPressure = -2.0,
                deltaHumidity = 4.0,
                deltaTemperature = -1.0,
            ),
        )
        val samples = store.read()
        assertEquals(1, samples.size)
        assertEquals(listOf(4, 6), samples.single().ratings)
        assertEquals(5.5, samples.single().weatherWindowMean, 0.0001)
        assertEquals(FeelCalibration.DRIVER_SCHEMA, samples.single().driverSchema)
    }

    @Test
    fun `legacy samples without driver schema are dropped on read`() {
        val file = temporaryFolder.newFile("feel-legacy.json")
        file.writeText(
            """
            {
              "samples": [
                {
                  "centreAtEpochMs": 1731153600000,
                  "ratings": [5],
                  "weatherWindowMean": 5.0,
                  "deltaPressure": -2.0,
                  "deltaHumidity": 3.0,
                  "deltaTemperature": -1.0,
                  "observationIds": ["legacy"]
                }
              ]
            }
            """.trimIndent(),
        )
        val store = FeelCalibrationStore(file)
        assertTrue(store.read().isEmpty())
        assertTrue(store.read().isEmpty())
    }
}
