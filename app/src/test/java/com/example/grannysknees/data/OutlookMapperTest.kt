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
 * Outlook projection for `index-v2`: DST-correct local-day labelling, honest
 * gaps, and extrema that match the live scorer (not archived v1 fixtures).
 */
class OutlookMapperTest {
    private val toronto: ZoneId = ZoneId.of("America/Toronto")

    @Test
    fun `fall-back day spans 25 hours and labels available repeated hours by offset`() {
        val result = score("dst-fall-back")
        val secondOneAm = localEpoch("2026-11-01T01:00", ZoneOffset.ofHours(-5))

        val curve = toOutlook(result, Instant.ofEpochSecond(secondOneAm + 1_800)).today
        val dayHours = result.hours.filter { it.localDate == "2026-11-01" }
        val available = dayHours.filter { it.status == HourStatus.AVAILABLE }

        assertEquals(LocalDate.of(2026, 11, 1), curve.date)
        assertEquals(25, curve.hoursInDay)
        assertEquals(available.size, curve.points.size)
        assertTrue(curve.points.size < curve.hoursInDay)
        val oneAmLabels = curve.points.map { it.timeLabel }.filter { it.startsWith("01:00") }
        assertTrue(oneAmLabels.all { it.contains("(") })
        assertEquals(2.5f / 25f, curve.nowFraction!!, FRACTION_TOLERANCE)
        assertTick(curve, "06", 7f / 25f)
        assertTick(curve, "12", 13f / 25f)
        assertTick(curve, "24", 1f)
    }

    @Test
    fun `fall-back daily extrema match the live scorer`() {
        val result = score("dst-fall-back")
        val daily = result.daily.single { it.date == "2026-11-01" }

        val range = toOutlook(result, Instant.ofEpochSecond(1_793_520_000L)).today.range!!

        assertEquals(daily.highHour.epoch, range.highEpoch)
        assertEquals(daily.lowHour.epoch, range.lowEpoch)
        assertEquals(0, daily.low.compareTo(range.low))
        assertEquals(0, daily.high.compareTo(range.high))
    }

    @Test
    fun `spring-forward day spans 23 hours with no 02_00 label`() {
        val result = score("dst-spring-forward")
        val noon = localEpoch("2026-03-08T12:00", ZoneOffset.ofHours(-4))

        val curve = toOutlook(result, Instant.ofEpochSecond(noon)).today
        val available = result.hours.filter {
            it.localDate == "2026-03-08" && it.status == HourStatus.AVAILABLE
        }

        assertEquals(23, curve.hoursInDay)
        assertEquals(available.size, curve.points.size)
        val labels = curve.points.map { it.timeLabel }
        assertFalse(labels.any { it.startsWith("02:") })
        assertTick(curve, "06", 5f / 23f)
        assertEquals(11f / 23f, curve.nowFraction!!, FRACTION_TOLERANCE)
    }

    @Test
    fun `tied extrema resolve to the earliest epoch from the live scorer`() {
        val result = score("dst-spring-forward")
        val daily = result.daily.single { it.date == "2026-03-08" }

        val curve = toOutlook(result, Instant.ofEpochSecond(1_772_960_400L)).today
        val range = curve.range!!

        assertEquals(0, daily.low.compareTo(range.low))
        assertEquals(0, daily.high.compareTo(range.high))
        val lowTied = curve.points.filter { it.score.compareTo(range.low) == 0 }
        val highTied = curve.points.filter { it.score.compareTo(range.high) == 0 }
        assertEquals(lowTied.minOf { it.epoch }, range.lowEpoch)
        assertEquals(highTied.minOf { it.epoch }, range.highEpoch)
        assertEquals(daily.lowHour.epoch, range.lowEpoch)
        assertEquals(daily.highHour.epoch, range.highEpoch)
    }

    @Test
    fun `unavailable hours are omitted and split the curve rather than zero-filled`() {
        val result = score("missing-values")
        val noon = localEpoch("2026-11-10T12:00", ZoneOffset.ofHours(-5))

        val curve = toOutlook(result, Instant.ofEpochSecond(noon)).today
        val dayHours = result.hours.filter { it.localDate == "2026-11-10" }
        val unavailableEpochs = dayHours
            .filter { it.status == HourStatus.UNAVAILABLE }
            .map { it.epoch }
            .toSet()
        val available = dayHours.filter { it.status == HourStatus.AVAILABLE }

        assertEquals(24, curve.hoursInDay)
        assertEquals(available.size, curve.points.size)
        assertTrue(unavailableEpochs.isNotEmpty())
        assertTrue(curve.points.none { it.epoch in unavailableEpochs })
        assertTrue(curve.segments.isNotEmpty())
        val scorerScores = result.hours.associate { it.epoch to it.indexScore }
        curve.points.forEach { point -> assertEquals(scorerScores[point.epoch], point.score) }
        assertNull(curve.currentHourEpoch)
        assertNotNull(curve.nowFraction)
    }

    @Test
    fun `partial day keeps the scorer status and available-hour counts`() {
        val result = score("missing-values")
        val daily = result.daily.single { it.date == "2026-11-10" }

        val range = toOutlook(result, Instant.ofEpochSecond(1_794_344_400L)).today.range!!

        assertEquals(daily.status, range.status)
        assertEquals(daily.status == DailyStatus.PARTIAL, range.isPartial)
        assertEquals(daily.hoursAvailable, range.hoursAvailable)
        assertEquals(daily.hoursTotal, range.hoursTotal)
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
        val available = result.hours.filter {
            it.localDate == "2026-11-10" && it.status == HourStatus.AVAILABLE
        }

        assertEquals(late.points, early.points)
        assertEquals(available.size, late.points.size)
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
        val tuesdayDaily = result.daily.single { it.date == "2026-11-10" }

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
        assertEquals(0, tuesdayDaily.low.compareTo(tuesday.range!!.low))
        assertEquals(0, tuesdayDaily.high.compareTo(tuesday.range!!.high))
        assertEquals(tuesdayDaily.highHour.epoch, tuesday.range!!.highEpoch)
        assertFalse(tuesday.isUnknown)
        assertNotNull(tuesday.startScore)
        assertNotNull(tuesday.endScore)
        assertEquals(tuesday.hourScores.first(), tuesday.startScore)
        assertEquals(tuesday.hourScores.last(), tuesday.endScore)
        assertTrue(outlook.upcoming.drop(1).all { it.isUnknown })
        assertTrue(outlook.upcoming.drop(1).all { it.range == null })
        assertTrue(outlook.upcoming.all { it.personalRange == null })
    }

    @Test
    fun `ribbon trajectory fills missing hours between known anchors`() {
        val known = mapOf(
            0 to BigDecimal("2.0"),
            6 to BigDecimal("8.0"),
            12 to BigDecimal("4.0"),
        )

        val filled = OutlookMapper.fillRibbonTrajectory(slotCount = 13, knownBySlot = known)

        assertEquals(13, filled.size)
        assertEquals(0, BigDecimal("2.0").compareTo(filled[0]))
        assertEquals(0, BigDecimal("8.0").compareTo(filled[6]))
        assertEquals(0, BigDecimal("4.0").compareTo(filled[12]))
        // Midway 0→6: average on the trajectory.
        assertEquals(0, BigDecimal("5.0").compareTo(filled[3]))
        // Midway 6→12.
        assertEquals(0, BigDecimal("6.0").compareTo(filled[9]))
        // Trailing hold after last known anchor.
        // slotCount 13 means indices 0..12; 12 is last known — no trailing.
    }

    @Test
    fun `ribbon trajectory holds edges and ignores empty days`() {
        assertTrue(OutlookMapper.fillRibbonTrajectory(24, emptyMap()).isEmpty())

        val known = mapOf(6 to BigDecimal("3.0"), 18 to BigDecimal("9.0"))
        val filled = OutlookMapper.fillRibbonTrajectory(24, known)

        assertEquals(24, filled.size)
        assertEquals(0, BigDecimal("3.0").compareTo(filled[0]))
        assertEquals(0, BigDecimal("3.0").compareTo(filled[5]))
        assertEquals(0, BigDecimal("6.0").compareTo(filled[12]))
        assertEquals(0, BigDecimal("9.0").compareTo(filled[18]))
        assertEquals(0, BigDecimal("9.0").compareTo(filled[23]))
    }

    @Test
    fun `five-day ribbon scores cover every local hour with trajectory fill`() {
        val result = score("nominal")
        val reference = Instant.ofEpochSecond(localEpoch("2026-11-09T22:00", ZoneOffset.ofHours(-5)))
        val outlook = toOutlook(result, reference)
        val tuesday = outlook.upcoming.first()
        val startEpoch = tuesday.date.atStartOfDay(toronto).toEpochSecond()
        val endEpoch = tuesday.date.plusDays(1).atStartOfDay(toronto).toEpochSecond()
        val hoursInDay = ((endEpoch - startEpoch) / 3_600L).toInt()
        val availableCount = result.hours.count {
            it.epoch in startEpoch until endEpoch &&
                it.status == HourStatus.AVAILABLE &&
                it.indexScore != null
        }

        assertTrue(availableCount > 0)
        assertEquals(hoursInDay, tuesday.hourScores.size)
        assertEquals(0, tuesday.startScore!!.compareTo(tuesday.hourScores.first()))
        assertEquals(0, tuesday.endScore!!.compareTo(tuesday.hourScores.last()))
    }

    @Test
    fun `five-day personal high and low come from the personal scorer`() {
        val request = IndexFixtures.loadRequest("nominal")
        val weather = WeatherIndexScorer.score(request)
        val personal = WeatherIndexScorer.score(
            request,
            SensitivityProfile(pressure = 1.5f, humidity = 0.5f, cold = 2f)
                .toDeltaSensitivities(),
        )
        val reference = Instant.ofEpochSecond(
            localEpoch("2026-11-09T22:00", ZoneOffset.ofHours(-5)),
        )
        val personalTuesday = personal.daily.single { it.date == "2026-11-10" }

        val outlook = OutlookMapper.toOutlook(
            indexResult = weather,
            zoneId = toronto,
            referenceNow = reference,
            personalResult = personal,
        )

        val tuesday = outlook.upcoming.first()
        assertNotNull(tuesday.personalRange)
        assertEquals(0, personalTuesday.high.compareTo(tuesday.personalRange!!.high))
        assertEquals(0, personalTuesday.low.compareTo(tuesday.personalRange!!.low))
        assertTrue(tuesday.hasPersonalRibbon)
        assertEquals(tuesday.hourScores.size, tuesday.personalHourScores.size)
        assertTrue(outlook.upcoming.drop(1).all { it.personalRange == null })
        assertTrue(outlook.upcoming.drop(1).all { it.personalHourScores.isEmpty() })
    }

    private fun score(case: String): IndexResult =
        WeatherIndexScorer.score(IndexFixtures.loadRequest(case))

    private fun toOutlook(result: IndexResult, referenceNow: Instant): OutlookUiState.Available =
        OutlookMapper.toOutlook(result, toronto, referenceNow)

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
