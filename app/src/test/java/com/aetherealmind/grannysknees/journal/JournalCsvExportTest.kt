package com.aetherealmind.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Flat CSV rows for the Settings history export. */
class JournalCsvExportTest {
    @Test
    fun `empty journal is header only`() {
        val csv = JournalCsvExport.format(emptyList())
        assertEquals(
            "observationId,observedAt,bodyPart,side,symptom,rating,reportKind\n",
            csv,
        )
    }

    @Test
    fun `one measurement becomes one data row`() {
        val observation = JournalObservation(
            id = "obs-1",
            observedAt = Instant.parse("2026-11-15T12:00:00Z"),
            correctsObservationId = null,
            deletedAt = null,
            measurements = listOf(
                JournalMeasurement(
                    bodyPart = "knee",
                    side = "left",
                    symptom = "ache",
                    rating = 6,
                    reportKind = "new",
                    previousObservationId = null,
                    previousRating = null,
                    previousObservedAt = null,
                ),
            ),
        )
        val csv = JournalCsvExport.format(listOf(observation))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("obs-1,2026-11-15T12:00:00Z,knee,left,ache,6,new"))
    }

    @Test
    fun `commas inside fields are quoted`() {
        val observation = JournalObservation(
            id = "id,1",
            observedAt = Instant.parse("2026-11-15T12:00:00Z"),
            correctsObservationId = null,
            deletedAt = null,
            measurements = listOf(
                JournalMeasurement(
                    bodyPart = "back",
                    side = null,
                    symptom = "stiffness",
                    rating = 3,
                    reportKind = "confirm",
                    previousObservationId = null,
                    previousRating = null,
                    previousObservedAt = null,
                ),
            ),
        )
        val csv = JournalCsvExport.format(listOf(observation))
        assertTrue(csv.contains("\"id,1\""))
    }
}
