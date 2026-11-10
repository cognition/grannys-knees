package ca.aeo3.grannysknees.sayings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Weekday and other display substitutions in Granny sayings. */
class SayingPlaceholdersTest {

    @Test
    fun `weekday placeholder becomes the local day name`() {
        val text = "You call it atmospheric pressure. My knees call it {weekday}."
        assertEquals(
            "You call it atmospheric pressure. My knees call it Wednesday.",
            SayingPlaceholders.render(
                text = text,
                today = LocalDate.of(2026, 11, 11),
                locale = Locale.CANADA,
            ),
        )
        assertEquals(
            "You call it atmospheric pressure. My knees call it Monday.",
            SayingPlaceholders.render(
                text = text,
                today = LocalDate.of(2026, 11, 9),
                locale = Locale.CANADA,
            ),
        )
    }

    @Test
    fun `text without placeholders is unchanged`() {
        val text = "Nothing I can't handle. I've met Mondays."
        assertEquals(
            text,
            SayingPlaceholders.render(
                text = text,
                today = LocalDate.of(2026, 11, 11),
                locale = Locale.CANADA,
            ),
        )
    }
}
