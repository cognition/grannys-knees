package com.example.grannysknees.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The report figure turns over at 3:00 a.m. local time, not at midnight.
 */
class ReportDummyTest {
    private val toronto: ZoneId = ZoneId.of("America/Toronto")

    @Test
    fun `just before 3 am still belongs to the previous reporting day`() {
        val at = Instant.parse("2026-11-06T07:59:00Z")
        assertEquals(LocalDate.of(2026, 11, 5), ReportDay.reportingDay(at, toronto))
    }

    @Test
    fun `3 am starts the new reporting day`() {
        val at = Instant.parse("2026-11-06T08:00:00Z")
        assertEquals(LocalDate.of(2026, 11, 6), ReportDay.reportingDay(at, toronto))
    }

    @Test
    fun `the next boundary from before 3 am is that morning`() {
        val at = Instant.parse("2026-11-06T07:00:00Z")
        assertEquals(Instant.parse("2026-11-06T08:00:00Z"), ReportDay.nextBoundary(at, toronto))
    }

    @Test
    fun `an instant on the boundary waits until the next morning`() {
        val at = Instant.parse("2026-11-06T08:00:00Z")
        assertEquals(Instant.parse("2026-11-07T08:00:00Z"), ReportDay.nextBoundary(at, toronto))
    }

    @Test
    fun `a draft from earlier the same reporting day is kept`() {
        val file = Files.createTempFile("report-dummy", ".json").toFile()
        val store = ReportDummyStore(file)
        val savedAt = Instant.parse("2026-11-06T07:30:00Z")
        store.write(
            ReportDummy(
                places = listOf(ChosenPlace("shoulder", "right", "stiffness", 4)),
                focus = 0,
                savedAt = savedAt,
            ),
        )
        val read = store.read(Instant.parse("2026-11-06T07:59:00Z"), toronto)
        assertEquals("shoulder", read.places.single().part)
        assertEquals(4, read.places.single().rating)
    }

    @Test
    fun `a draft from the previous reporting day is dropped at 3 am`() {
        val file = Files.createTempFile("report-dummy", ".json").toFile()
        val store = ReportDummyStore(file)
        store.write(
            ReportDummy(
                places = listOf(ChosenPlace("shoulder", "right", "stiffness", 4)),
                focus = 0,
                savedAt = Instant.parse("2026-11-06T07:30:00Z"),
            ),
        )
        val read = store.read(Instant.parse("2026-11-06T08:00:00Z"), toronto)
        assertEquals("knee", read.places.single().part)
        assertEquals("left", read.places.single().side)
        assertEquals(0, read.places.single().rating)
        assertFalse(ReportDay.sameReportingDay(read.savedAt, Instant.parse("2026-11-06T07:30:00Z"), toronto))
        assertTrue(ReportDay.sameReportingDay(read.savedAt, Instant.parse("2026-11-06T08:00:00Z"), toronto))
    }

    @Test
    fun `same as yesterday brings back the latest place from before 3 am`() {
        val now = Instant.parse("2026-11-06T15:00:00Z")
        val places = placesFromYesterday(
            observations = listOf(
                report("older", "2026-11-05T15:00:00Z", ChosenPlace("knee", "left", "ache", 2)),
                report("later", "2026-11-05T22:00:00Z", ChosenPlace("knee", "left", "ache", 6), ChosenPlace("hip", "right", "stiffness", 3)),
                report("today", "2026-11-06T16:00:00Z", ChosenPlace("elbow", "left", "ache", 9)),
            ),
            now = now,
            zone = toronto,
        )
        assertEquals(listOf("knee", "hip"), places.map { place -> place.part })
        assertEquals(6, places.first().rating)
        assertEquals(3, places.last().rating)
    }

    @Test
    fun `a report saved today is not yesterday`() {
        val places = placesFromYesterday(
            observations = listOf(
                report("today", "2026-11-06T16:00:00Z", ChosenPlace("elbow", "left", "ache", 9)),
            ),
            now = Instant.parse("2026-11-06T18:00:00Z"),
            zone = toronto,
        )
        assertTrue(places.isEmpty())
    }

    @Test
    fun `a correction made today does not hide yesterday`() {
        val yesterday = report("knee-report", "2026-11-05T20:00:00Z", ChosenPlace("knee", "left", "ache", 7))
        val correctionToday = yesterday.copy(
            id = "correction",
            observedAt = Instant.parse("2026-11-06T16:00:00Z"),
            correctsObservationId = "knee-report",
            measurements = yesterday.measurements.map { measurement -> measurement.copy(rating = 1) },
        )
        val places = placesFromYesterday(
            observations = listOf(yesterday, correctionToday),
            now = Instant.parse("2026-11-06T18:00:00Z"),
            zone = toronto,
        )
        assertEquals(7, places.single().rating)
        assertEquals("knee", places.single().part)
    }

    @Test
    fun `the past week plays oldest day first and keeps later corrections off earlier days`() {
        val now = Instant.parse("2026-11-06T18:00:00Z")
        val earlier = report("knee-report", "2026-11-04T16:00:00Z", ChosenPlace("knee", "left", "ache", 4))
        val correction = earlier.copy(
            id = "correction",
            observedAt = Instant.parse("2026-11-05T16:00:00Z"),
            correctsObservationId = "knee-report",
            measurements = earlier.measurements.map { measurement -> measurement.copy(rating = 8) },
        )
        val frames = weekFrames(listOf(earlier, correction), now, toronto)
        assertEquals(WEEK_LENGTH, frames.size)
        assertEquals(LocalDate.of(2026, 10, 31), frames.first().day)
        assertEquals(LocalDate.of(2026, 11, 6), frames.last().day)
        val beforeCorrection = frames.first { frame -> frame.day == LocalDate.of(2026, 11, 4) }
        val afterCorrection = frames.first { frame -> frame.day == LocalDate.of(2026, 11, 5) }
        assertEquals(4, beforeCorrection.places.single().rating)
        assertEquals(8, afterCorrection.places.single().rating)
        assertEquals("Wed 4 Nov", beforeCorrection.label)
    }

    private fun report(id: String, at: String, vararg places: ChosenPlace): JournalObservation {
        return JournalObservation(
            id = id,
            observedAt = Instant.parse(at),
            correctsObservationId = null,
            deletedAt = null,
            measurements = places.map { place ->
                JournalMeasurement(
                    bodyPart = place.part,
                    side = place.side,
                    symptom = place.symptom,
                    rating = place.rating,
                    reportKind = JournalCommitter.CHANGED,
                    previousObservationId = null,
                    previousRating = null,
                    previousObservedAt = null,
                )
            },
        )
    }
}
