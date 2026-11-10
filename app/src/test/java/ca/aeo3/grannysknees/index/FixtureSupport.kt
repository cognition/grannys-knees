package ca.aeo3.grannysknees.index

import java.io.File
import java.math.BigDecimal

/**
 * Loads the normative contract fixtures from `docs/contracts/fixtures/` and
 * provides exact, scale-independent value comparisons for the scorer tests.
 *
 * The scorer input is built from the Open-Meteo-shaped `<case>.input.json`
 * (`timeformat=unixtime`); the expected output is read from
 * `<case>.expected.json` as a generic tree and compared field for field.
 */
object FixtureSupport {

    private const val FIXTURE_RELATIVE_PATH = "docs/contracts/fixtures"

    /** Locate `docs/contracts/fixtures` by walking up from the test working dir. */
    fun fixturesDir(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, FIXTURE_RELATIVE_PATH)
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("Could not locate '$FIXTURE_RELATIVE_PATH' from ${System.getProperty("user.dir")}")
    }

    @Suppress("UNCHECKED_CAST")
    fun loadExpected(caseName: String): Map<String, Any?> {
        val file = File(fixturesDir(), "$caseName.expected.json")
        return MiniJson.parse(file.readText(Charsets.UTF_8)) as Map<String, Any?>
    }

    /** Build a [ScoringRequest] from an Open-Meteo-shaped `<case>.input.json`. */
    @Suppress("UNCHECKED_CAST")
    fun loadRequest(caseName: String): ScoringRequest {
        val file = File(fixturesDir(), "$caseName.input.json")
        val root = MiniJson.parse(file.readText(Charsets.UTF_8)) as Map<String, Any?>
        val zoneId = root["timezone"] as String
        val hourly = root["hourly"] as Map<String, Any?>

        val times = hourly["time"] as List<Any?>
        val temperature = hourly["temperature_2m"] as List<Any?>
        val humidity = hourly["relative_humidity_2m"] as List<Any?>
        val pressure = hourly["surface_pressure"] as List<Any?>

        val hours = times.indices.map { i ->
            HourlyObservation(
                epoch = (times[i] as BigDecimal).longValueExact(),
                surfacePressureHpa = (pressure[i] as BigDecimal?)?.toDouble(),
                relativeHumidityPct = (humidity[i] as BigDecimal?)?.toDouble(),
                temperatureC = (temperature[i] as BigDecimal?)?.toDouble(),
            )
        }
        return ScoringRequest(zoneId = zoneId, hours = hours)
    }

    /** Normalise any JSON/Kotlin numeric (or null) into a [BigDecimal] for value compare. */
    fun toBigDecimal(value: Any?): BigDecimal? = when (value) {
        null -> null
        is BigDecimal -> value
        is Int -> BigDecimal(value)
        is Long -> BigDecimal(value)
        is Double -> BigDecimal.valueOf(value)
        else -> error("Not a number: $value (${value::class})")
    }
}
