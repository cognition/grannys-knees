package com.example.grannysknees.data

import com.example.grannysknees.index.DailyStatus
import com.example.grannysknees.index.HourStatus
import com.example.grannysknees.index.IndexResult
import com.example.grannysknees.index.WeatherIndexScorer
import com.example.grannysknees.ui.outlook.DayCurve
import com.example.grannysknees.ui.outlook.OutlookUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone
import com.example.grannysknees.index.FixtureSupport as IndexFixtures

/**
 * Outlook projection against the contract fixtures: DST-correct local-day
 * labelling, extrema/tie-break parity with the scorer, and honest gaps.
 */
class OutlookMapperTest {
    private val toronto: ZoneId = ZoneId.of("America/Toronto")

    @Test
    fun `fall-back day spans 25 hours and labels the repeated hour by offset`() {
        val result = score("dst-fall-back")
        val secondOneAm = localEpoch("2026-11-01T01:00", ZoneOffset.ofHours(-5))

        val curve = toOutlook(result, Instant.ofEpochSecond(secondOneAm + 1_800)).today

        assertEquals(LocalDate.of(2026, 11, 1), curve.date)
        assertEquals(25, curve.hoursInDay)
        assertEquals(25, curve.points.size)
        assertEquals(1, curve.segments.size)
        val oneAmLabels = curve.points.map { it.timeLabel }.filter { it.startsWith("01:00") }
        assertEquals(listOf("01:00 (-04:00)", "01:00 (-05:00)"), oneAmLabels)
        assertEquals(
            listOf(-14_400, -18_000),
            curve.points.filter { it.timeLabel.startsWith("01:00") }.map { it.utcOffsetSeconds },
        )
        // Truncating 01:30 EST must pick the second 01:00, not the EDT one.
        assertEquals(secondOneAm, curve.currentHourEpoch)
        assertEquals(2.5f / 25f, curve.nowFraction!!, FRACTION_TOLERANCE)
        assertTick(curve, "06", 7f / 25f)
        assertTick(curve, "12", 13f / 25f)
        assertTick(curve, "24", 1f)
    }

    @Test
    fun `fall-back daily high reports the second 01_00 like the fixture`() {
        val result = score("dst-fall-back")
        val expected = expectedDaily("dst-fall-back", "2026-11-01")

        val range = toOutlook(result, Instant.ofEpochSecond(1_793_520_000L)).today.range!!

        assertEquals(epochOf(expected["highHour"]), range.highEpoch)
        assertEquals("01:00 (-05:00)", range.highTimeLabel)
        assertEquals(epochOf(expected["lowHour"]), range.lowEpoch)
        assertEquals("03:00", range.lowTimeLabel)
    }

    @Test
    fun `spring-forward day spans 23 hours with no 02_00 label`() {
        val result = score("dst-spring-forward")
        val noon = localEpoch("2026-03-08T12:00", ZoneOffset.ofHours(-4))

        val curve = toOutlook(result, Instant.ofEpochSecond(noon)).today

        assertEquals(23, curve.hoursInDay)
        assertEquals(23, curve.points.size)
        val labels = curve.points.map { it.timeLabel }
        assertFalse(labels.any { it.startsWith("02:") })
        val oneAm = curve.points.single { it.timeLabel == "01:00" }
        val threeAm = curve.points.single { it.timeLabel == "03:00" }
        assertEquals(3_600L, threeAm.epoch - oneAm.epoch)
        assertEquals(-18_000, oneAm.utcOffsetSeconds)
        assertEquals(-14_400, threeAm.utcOffsetSeconds)
        assertTick(curve, "06", 5f / 23f)
        assertEquals(11f / 23f, curve.nowFraction!!, FRACTION_TOLERANCE)
    }

    @Test
    fun `tied extrema resolve to the earliest epoch and match the scorer fixture`() {
        val result = score("dst-spring-forward")
        val expected = expectedDaily("dst-spring-forward", "2026-03-08")

        val curve = toOutlook(result, Instant.ofEpochSecond(1_772_960_400L)).today
        val range = curve.range!!

        assertDecimalEquals(expected["low"], range.low)
        assertDecimalEquals(expected["high"], range.high)
        val lowTied = curve.points.filter { it.score.compareTo(range.low) == 0 }
        val highTied = curve.points.filter { it.score.compareTo(range.high) == 0 }
        assertEquals(intOf(expected["lowTies"]), lowTied.size)
        assertEquals(intOf(expected["highTies"]), highTied.size)
        assertTrue("fixture should exercise the tie-break", highTied.size > 1)
        assertEquals(lowTied.minOf { it.epoch }, range.lowEpoch)
        assertEquals(highTied.minOf { it.epoch }, range.highEpoch)
        assertEquals(epochOf(expected["lowHour"]), range.lowEpoch)
        assertEquals(epochOf(expected["highHour"]), range.highEpoch)
        assertEquals("05:00", range.highTimeLabel)
    }

    @Test
    fun `unavailable hours are omitted and split the curve rather than zero-filled`() {
        val result = score("missing-values")
        val noon = localEpoch("2026-11-10T12:00", ZoneOffset.ofHours(-5))

        val curve = toOutlook(result, Instant.ofEpochSecond(noon)).today

        assertEquals(24, curve.hoursInDay)
        assertEquals(17, curve.points.size)
        val unavailableEpochs = result.hours
            .filter { it.localDate == "2026-11-10" && it.status == HourStatus.UNAVAILABLE }
            .map { it.epoch }
            .toSet()
        assertEquals(7, unavailableEpochs.size)
        assertTrue(curve.points.none { it.epoch in unavailableEpochs })
        assertEquals(2, curve.segments.size)
        assertEquals("05:00", curve.segments[0].last().timeLabel)
        assertEquals("13:00", curve.segments[1].first().timeLabel)
        val scorerScores = result.hours.associate { it.epoch to it.indexScore }
        curve.points.forEach { point -> assertEquals(scorerScores[point.epoch], point.score) }
        // Noon itself is unavailable, so no current-hour marker is drawn.
        assertNull(curve.currentHourEpoch)
        assertNotNull(curve.nowFraction)
    }

    @Test
    fun `partial day keeps the scorer status and available-hour counts`() {
        val result = score("missing-values")

        val range = toOutlook(result, Instant.ofEpochSecond(1_794_344_400L)).today.range!!

        assertEquals(DailyStatus.PARTIAL, range.status)
        assertTrue(range.isPartial)
        assertEquals(17, range.hoursAvailable)
        assertEquals(24, range.hoursTotal)
    }

    @Test
    fun `curve is the local calendar day, not a rolling 24 hours`() {
        val result = score("nominal")
        val lateEvening = localEpoch("2026-11-10T23:30", ZoneOffset.ofHours(-5))
        val earlyMorning = localEpoch("2026-11-10T00:30", ZoneOffset.ofHours(-5))
        val dayStart = localEpoch("2026-11-10T00:00", ZoneOffset.ofHours(-5))
        val dayEnd = localEpoch("2026-11-11T00:00", ZoneOffset.ofHours(-5))

        val late = toOutlook(result, Instant.ofEpochSecond(lateEvening)).today
        val early = toOutlook(result, Instant.ofEpochSecond(earlyMorning)).today

        assertEquals(late.points, early.points)
        assertEquals(24, late.points.size)
        assertTrue(late.points.all { it.epoch in dayStart until dayEnd })
        assertEquals(23.5f / 24f, late.nowFraction!!, FRACTION_TOLERANCE)
        assertEquals(0.5f / 24f, early.nowFraction!!, FRACTION_TOLERANCE)
    }

    @Test
    fun `labels follow the forecast zone regardless of the device zone`() {
        val result = score("dst-fall-back")
        val reference = Instant.ofEpochSecond(1_793_520_000L)
        val original = TimeZone.getDefault()
        val inToronto = try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            toOutlook(result, reference)
        } finally {
            TimeZone.setDefault(original)
        }
        val inTokyo = try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            toOutlook(result, reference)
        } finally {
            TimeZone.setDefault(original)
        }

        assertEquals(inToronto, inTokyo)
    }

    @Test
    fun `five-day rows start tomorrow and never invent days without an index`() {
        val result = score("nominal")
        // 22:00 on Nov 9: only lead-in hours exist today, none scored.
        val reference = Instant.ofEpochSecond(localEpoch("2026-11-09T22:00", ZoneOffset.ofHours(-5)))
        val expected = expectedDaily("nominal", "2026-11-10")

        val outlook = toOutlook(result, reference)

        assertTrue(outlook.today.points.isEmpty())
        assertNull(outlook.today.range)
        assertEquals(OutlookMapper.UPCOMING_DAY_COUNT, outlook.upcoming.size)
        assertEquals(
            (1L..5L).map { LocalDate.of(2026, 11, 9).plusDays(it) },
            outlook.upcoming.map { it.date },
        )
        val tuesday = outlook.upcoming.first()
        assertTrue(tuesday.dayLabel, tuesday.dayLabel.startsWith("Tue"))
        assertDecimalEquals(expected["low"], tuesday.range!!.low)
        assertDecimalEquals(expected["high"], tuesday.range!!.high)
        assertEquals(epochOf(expected["highHour"]), tuesday.range!!.highEpoch)
        assertEquals("16:00", tuesday.range!!.highTimeLabel)
        assertTrue(outlook.upcoming.drop(1).all { it.range == null })
    }

    private fun score(case: String): IndexResult =
        WeatherIndexScorer.score(IndexFixtures.loadRequest(case))

    private fun toOutlook(result: IndexResult, referenceNow: Instant): OutlookUiState.Available =
        OutlookMapper.toOutlook(result, toronto, referenceNow)

    @Suppress("UNCHECKED_CAST")
    private fun expectedDaily(case: String, date: String): Map<String, Any?> =
        (IndexFixtures.loadExpected(case)["daily"] as List<Map<String, Any?>>)
            .single { it["date"] == date }

    @Suppress("UNCHECKED_CAST")
    private fun epochOf(hourRef: Any?): Long =
        IndexFixtures.toBigDecimal((hourRef as Map<String, Any?>)["epoch"])!!.longValueExact()

    private fun intOf(value: Any?): Int = IndexFixtures.toBigDecimal(value)!!.intValueExact()

    private fun assertDecimalEquals(expected: Any?, actual: BigDecimal) {
        assertEquals(0, IndexFixtures.toBigDecimal(expected)!!.compareTo(actual))
    }

    private fun localEpoch(local: String, offset: ZoneOffset): Long {
        val zoned = ZonedDateTime.ofLocal(LocalDateTime.parse(local), toronto, offset)
        check(zoned.offset == offset) { "$local is not valid at $offset in $toronto" }
        return zoned.toEpochSecond()
    }

    private fun assertTick(curve: DayCurve, label: String, fraction: Float) {
        val tick = curve.axisTicks.single { it.label == label }
        assertEquals("tick $label", fraction, tick.fraction, FRACTION_TOLERANCE)
    }

    private companion object {
        const val FRACTION_TOLERANCE = 1e-5f
    }
}
