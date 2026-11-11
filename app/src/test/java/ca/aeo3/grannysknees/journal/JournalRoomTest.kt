package ca.aeo3.grannysknees.journal

import androidx.room.Room
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Room round-trip for a partial report and an idempotent event id.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class JournalRoomTest {
    @Test
    fun `a left foot ache does not create a knee row`() {
        val repository = repository()
        repository.save(
            committedAt = Instant.parse("2026-11-05T16:00:00Z"),
            edits = listOf(SeriesEdit("foot", "left", "ache", 6)),
            confirmations = emptyList(),
        )
        val measurements = repository.current().flatMap { it.measurements }
        assertEquals(1, measurements.size)
        assertEquals("foot", measurements.single().bodyPart)
        assertEquals("left", measurements.single().side)
        assertEquals(6, measurements.single().rating)
    }

    @Test
    fun `a repeated event id appends one observation`() {
        val repository = repository()
        repeat(2) {
            repository.save(
                committedAt = Instant.parse("2026-11-05T16:00:00Z"),
                edits = listOf(SeriesEdit("knee", "left", "ache", 6)),
                confirmations = emptyList(),
                eventId = "evt-1",
            )
        }
        assertEquals(1, repository.all().size)
        assertEquals(6, repository.current().single().measurements.single().rating)
    }

    @Test
    fun `an explicit zero survives a room round trip`() {
        val repository = repository()
        repository.save(
            committedAt = Instant.parse("2026-11-05T16:00:00Z"),
            edits = listOf(SeriesEdit("knee", "left", "ache", 0)),
            confirmations = emptyList(),
        )
        assertEquals(0, repository.current().single().measurements.single().rating)
    }

    @Test
    fun `removing one history report keeps the other measurements`() {
        val repository = repository()
        val saved = requireNotNull(
            repository.save(
                committedAt = Instant.parse("2026-11-10T15:00:00Z"),
                edits = listOf(
                    SeriesEdit("wrist", "left", "stiffness", 8),
                    SeriesEdit("knee", "right", "ache", 5),
                ),
                confirmations = emptyList(),
            ),
        )

        repository.deleteMeasurement(
            observationId = saved.id,
            bodyPart = "wrist",
            side = "left",
            symptom = "stiffness",
            deletedAt = Instant.parse("2026-11-10T16:00:00Z"),
        )

        val remaining = repository.current().single().measurements
        assertEquals(1, remaining.size)
        assertEquals("knee", remaining.single().bodyPart)
        assertEquals("right", remaining.single().side)
        assertEquals(5, remaining.single().rating)
    }

    private fun repository(): JournalRepository {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            JournalDatabase::class.java,
        ).allowMainThreadQueries().build()
        return JournalRepository(database.journalDao())
    }
}
