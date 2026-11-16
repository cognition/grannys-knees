package com.aetherealmind.grannysknees.sayings

import com.aetherealmind.grannysknees.index.HourIndex
import com.aetherealmind.grannysknees.index.HourInputs
import com.aetherealmind.grannysknees.index.HourStatus
import com.aetherealmind.grannysknees.index.SubScores
import com.aetherealmind.grannysknees.weather.ForecastHour
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Derives Granny Says weather tags from nearby forecast hours. */
class GrannyWeatherTypeTest {
    @Test
    fun `snow rain sunny and humid tags come from nearby hours`() {
        val now = Instant.parse("2026-11-15T18:00:00Z")
        val hours = listOf(
            hour(now.epochSecond, weatherCode = 71, temperatureC = -2.0, humidity = 40),
            hour(now.epochSecond + 3600, weatherCode = 61, temperatureC = 4.0, humidity = 80),
            hour(now.epochSecond + 7200, weatherCode = 0, temperatureC = 8.0, humidity = 50),
        )
        val active = GrannyWeatherType.activeTypes(
            hours = hours,
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.SNOW in active)
        assertTrue(GrannyWeatherType.RAIN in active)
        assertTrue(GrannyWeatherType.SUNNY in active)
        assertTrue(GrannyWeatherType.COLD in active)
        assertTrue(GrannyWeatherType.HUMID in active)
    }

    @Test
    fun `pressure change uses the scored hour delta`() {
        val now = Instant.parse("2026-11-15T18:00:00Z")
        val epoch = now.atZone(ZoneOffset.UTC)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toEpochSecond()
        val scored = HourIndex(
            epoch = epoch,
            instantUtc = Instant.ofEpochSecond(epoch).toString(),
            local = "",
            utcOffsetSeconds = 0,
            localDate = "2026-11-15",
            inputs = HourInputs(null, null, null, null),
            pressureTrend3hHpa = BigDecimal("-2.50"),
            subScores = SubScores(
                pressure = BigDecimal("-2.50"),
                humidity = null,
                temperature = null,
            ),
            indexRaw = BigDecimal("5.0000"),
            indexScore = BigDecimal("5.0"),
            indexDisplay = 5,
            status = HourStatus.AVAILABLE,
            unavailableReasons = emptyList(),
        )
        val active = GrannyWeatherType.activeTypes(
            hours = listOf(hour(epoch, weatherCode = 2, temperatureC = 10.0, humidity = 40)),
            scoredHours = listOf(scored),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.PRESSURE_CHANGE in active)
        assertTrue(GrannyWeatherType.CLOUDY in active)
    }

    @Test
    fun `thunderstorm and icy come from WMO codes`() {
        val now = Instant.parse("2026-11-15T18:00:00Z")
        val active = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(now.epochSecond, weatherCode = 95, temperatureC = 18.0, humidity = 70),
                hour(now.epochSecond + 3600, weatherCode = 66, temperatureC = -1.0, humidity = 90),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.THUNDERSTORM in active)
        assertTrue(GrannyWeatherType.RAIN in active)
        assertTrue(GrannyWeatherType.ICY in active)
        assertTrue(GrannyWeatherType.COLD in active)
    }

    @Test
    fun `hot uses air or apparent temperature`() {
        val now = Instant.parse("2026-07-15T18:00:00Z")
        val byAir = GrannyWeatherType.activeTypes(
            hours = listOf(hour(now.epochSecond, weatherCode = 0, temperatureC = 29.0, humidity = 40)),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.HOT in byAir)

        val byApparent = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(
                    now.epochSecond,
                    weatherCode = 1,
                    temperatureC = 26.0,
                    humidity = 40,
                    apparentC = 30.0,
                ),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.HOT in byApparent)

        val mild = GrannyWeatherType.activeTypes(
            hours = listOf(hour(now.epochSecond, weatherCode = 0, temperatureC = 24.0, humidity = 40)),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertFalse(GrannyWeatherType.HOT in mild)
    }

    @Test
    fun `windy uses wind speed threshold`() {
        val now = Instant.parse("2026-11-15T18:00:00Z")
        val windy = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(
                    now.epochSecond,
                    weatherCode = 2,
                    temperatureC = 12.0,
                    humidity = 40,
                    windKmh = 35.0,
                ),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.WINDY in windy)

        val calm = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(
                    now.epochSecond,
                    weatherCode = 2,
                    temperatureC = 12.0,
                    humidity = 40,
                    windKmh = 10.0,
                ),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertFalse(GrannyWeatherType.WINDY in calm)
    }

    @Test
    fun `cold front is a nearby cooling of three degrees or more`() {
        val now = Instant.parse("2026-11-15T18:00:00Z")
        val front = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(now.epochSecond - 3600, weatherCode = 2, temperatureC = 12.0, humidity = 40),
                hour(now.epochSecond, weatherCode = 3, temperatureC = 8.0, humidity = 50),
                hour(now.epochSecond + 3600, weatherCode = 61, temperatureC = 7.0, humidity = 70),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertTrue(GrannyWeatherType.COLD_FRONT in front)

        val steady = GrannyWeatherType.activeTypes(
            hours = listOf(
                hour(now.epochSecond - 3600, weatherCode = 2, temperatureC = 12.0, humidity = 40),
                hour(now.epochSecond + 3600, weatherCode = 2, temperatureC = 11.0, humidity = 40),
            ),
            scoredHours = emptyList(),
            zoneId = ZoneOffset.UTC,
            referenceNow = now,
        )
        assertFalse(GrannyWeatherType.COLD_FRONT in steady)
    }

    private fun hour(
        epoch: Long,
        weatherCode: Int,
        temperatureC: Double,
        humidity: Int,
        apparentC: Double = temperatureC,
        windKmh: Double? = null,
    ): ForecastHour {
        val instant = Instant.ofEpochSecond(epoch)
        val local = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
        return ForecastHour(
            epoch = epoch,
            instant = instant,
            local = local,
            utcOffsetSeconds = 0,
            localDate = LocalDate.ofInstant(instant, ZoneOffset.UTC),
            temperature2m = temperatureC,
            relativeHumidity2m = humidity,
            apparentTemperature = apparentC,
            surfacePressure = 1010.0,
            weatherCode = weatherCode,
            windSpeed10m = windKmh,
        )
    }
}
