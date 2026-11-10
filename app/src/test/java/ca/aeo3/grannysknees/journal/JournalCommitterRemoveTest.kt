package ca.aeo3.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Single-measurement removal from a multi-report observation. */
class JournalCommitterRemoveTest {
    @Test
    fun `removing one measurement keeps the rest of the set`() {
        val target = observation(
            id = "obs-1",
            observedAt = Instant.parse("2026-11-10T15:00:00Z"),
            measurements = listOf(
                measurement("wrist", "left", "stiffness", 8),
                measurement("knee", "right", "ache", 5),
            ),
        )

        val result = requireNotNull(
            JournalCommitter.removeMeasurement(
                target = target,
                bodyPart = "wrist",
                side = "left",
                symptom = "stiffness",
                deletedAt = Instant.parse("2026-11-10T16:00:00Z"),
                newId = "obs-2",
            ),
        )

        assertNull(result.deletedAt)
        assertEquals("obs-1", result.correctsObservationId)
        assertEquals(1, result.measurements.size)
        assertEquals("knee", result.measurements.single().bodyPart)
        assertEquals(
            Instant.parse("2026-11-10T15:00:00Z"),
            result.measurements.single().previousObservedAt,
        )
        assertEquals(
            listOf(result),
            JournalCommitter.current(listOf(target, result)),
        )
    }

    @Test
    fun `removing the last measurement tombstones the observation`() {
        val target = observation(
            id = "obs-1",
            observedAt = Instant.parse("2026-11-10T15:00:00Z"),
            measurements = listOf(measurement("wrist", "left", "stiffness", 8)),
        )

        val result = requireNotNull(
            JournalCommitter.removeMeasurement(
                target = target,
                bodyPart = "wrist",
                side = "left",
                symptom = "stiffness",
                deletedAt = Instant.parse("2026-11-10T16:00:00Z"),
                newId = "obs-2",
            ),
        )

        assertNotNull(result.deletedAt)
        assertTrue(result.measurements.isEmpty())
        assertTrue(JournalCommitter.current(listOf(target, result)).isEmpty())
    }

    @Test
    fun `removing an unknown series does nothing`() {
        val target = observation(
            id = "obs-1",
            observedAt = Instant.parse("2026-11-10T15:00:00Z"),
            measurements = listOf(measurement("knee", "left", "ache", 4)),
        )

        assertNull(
            JournalCommitter.removeMeasurement(
                target = target,
                bodyPart = "wrist",
                side = "left",
                symptom = "stiffness",
                deletedAt = Instant.parse("2026-11-10T16:00:00Z"),
                newId = "obs-2",
            ),
        )
    }

    private fun observation(
        id: String,
        observedAt: Instant,
        measurements: List<JournalMeasurement>,
    ): JournalObservation = JournalObservation(
        id = id,
        observedAt = observedAt,
        correctsObservationId = null,
        deletedAt = null,
        measurements = measurements,
    )

    private fun measurement(
        bodyPart: String,
        side: String?,
        symptom: String,
        rating: Int,
    ): JournalMeasurement = JournalMeasurement(
        bodyPart = bodyPart,
        side = side,
        symptom = symptom,
        rating = rating,
        reportKind = JournalCommitter.CHANGED,
        previousObservationId = null,
        previousRating = null,
        previousObservedAt = null,
    )
}
