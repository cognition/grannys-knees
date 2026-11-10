package ca.aeo3.grannysknees.sayings

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Fills display placeholders in Granny saying text.
 *
 * Catalog lines may include `{weekday}` for the current local day name.
 */
internal object SayingPlaceholders {
    private const val WEEKDAY: String = "{weekday}"

    /**
     * Returns [text] with known placeholders replaced for [today].
     *
     * Day names use Canadian English by default (for example "Wednesday").
     */
    fun render(
        text: String,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.CANADA,
    ): String {
        if (!text.contains(WEEKDAY)) {
            return text
        }
        val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        return text.replace(WEEKDAY, weekday)
    }
}
