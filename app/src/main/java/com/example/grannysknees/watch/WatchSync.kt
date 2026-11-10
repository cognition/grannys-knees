package com.example.grannysknees.watch

import android.content.Context
import com.example.grannysknees.journal.JournalRepository
import com.example.grannysknees.journal.SeriesEdit
import com.example.grannysknees.ui.today.TodayUiState
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import androidx.room.Room
import com.example.grannysknees.journal.JournalDatabase
import java.io.File
import java.time.Instant

/** Paths shared with the watch module. */
object WatchPaths {
    const val INDEX: String = "/grannys-knees/index"
    const val LOG: String = "/grannys-knees/log"
    const val PAYLOAD_KEY: String = "payload"
}

/**
 * Publishes the current index for the watch tile.
 *
 * The phone cache stays the source of the score. A phone without the Wear
 * API still writes the local payload file.
 */
internal class WatchIndexPublisher(
    private val context: Context,
) {
    /** Sends [state] to the local payload file and, when available, the data layer. */
    fun publish(state: TodayUiState) {
        val payload = watchIndexText(state)
        File(context.filesDir, "watch-index.txt").writeText(payload, Charsets.UTF_8)
        try {
            val request = PutDataMapRequest.create(WatchPaths.INDEX).run {
                dataMap.putString(WatchPaths.PAYLOAD_KEY, payload)
                dataMap.putLong("updatedAt", System.currentTimeMillis())
                asPutDataRequest().setUrgent()
            }
            Wearable.getDataClient(context).putDataItem(request)
        } catch (_: IllegalStateException) {
            // Play services is missing. The local file remains.
        } catch (_: SecurityException) {
            // The data layer refused the write.
        }
    }
}

/**
 * Accepts one partial report from the watch. A repeated event id is ignored.
 */
class WatchLogService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchPaths.LOG) return
        val report = parseWatchLog(messageEvent.data) ?: return
        val repository = JournalRepository(
            Room.databaseBuilder(applicationContext, JournalDatabase::class.java, "journal.db")
                .build()
                .journalDao(),
        )
        repository.save(
            committedAt = Instant.now(),
            edits = listOf(
                SeriesEdit(
                    bodyPart = report.bodyPart,
                    side = report.side,
                    symptom = report.symptom,
                    rating = report.rating,
                ),
            ),
            confirmations = emptyList(),
            eventId = report.eventId,
        )
    }
}

/** Text the watch tile shows. The watch does not calculate this score. */
internal fun watchIndexText(state: TodayUiState): String = when (state) {
    is TodayUiState.Available -> {
        val personal = state.personalScoreLabel
        val scoreLine = if (personal != null && personal != state.scoreLabel) {
            "${state.scoreLabel} / $personal"
        } else {
            state.scoreLabel
        }
        "$scoreLine\n${state.location}\n${state.lastUpdatedLabel}"
    }
    else -> "No index\n${state.location}"
}

/** One partial report from the watch. */
internal data class WatchLogReport(
    val eventId: String,
    val bodyPart: String,
    val side: String?,
    val symptom: String,
    val rating: Int,
)

/** Parses a watch quick-log body. A short or non-numeric body is ignored. */
internal fun parseWatchLog(body: ByteArray): WatchLogReport? {
    val parts = String(body, Charsets.UTF_8).split('\t')
    if (parts.size < 5 || parts[0].isBlank()) return null
    val rating = parts[4].toIntOrNull() ?: return null
    return WatchLogReport(
        eventId = parts[0],
        bodyPart = parts[1],
        side = parts[2].ifBlank { null },
        symptom = parts[3],
        rating = rating,
    )
}
