package ca.aeo3.grannysknees.data

import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Persists the local calendar date of the last successful forecast HTTP fetch.
 *
 * Used to allow at most one automatic network pull per local day.
 */
internal class DailyFetchStampStore(
    private val file: File,
) {
    /** The stamped local date, or null when never written or unreadable. */
    fun readLocalDate(): LocalDate? {
        if (!file.exists()) return null
        return try {
            val text = file.readText(Charsets.UTF_8).trim()
            if (text.isEmpty()) null else LocalDate.parse(text)
        } catch (_: IOException) {
            null
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** Records a successful fetch for [date] in the place's local calendar. */
    fun writeLocalDate(date: LocalDate) {
        AtomicFileWriter.writeText(file, date.toString())
    }

    /** Clears the stamp so the next open may fetch again. */
    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
