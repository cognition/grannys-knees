package ca.aeo3.grannysknees.wear

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.LayoutElementBuilders
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Current index from the phone. This tile does not score weather itself.
 */
class IndexTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val label = readPayload()
        val text = LayoutElementBuilders.Text.Builder()
            .setText(label)
            .build()
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(text).build()
        val entry = TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build()
        val timeline = TimelineBuilders.Timeline.Builder().addTimelineEntry(entry).build()
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCE_VERSION)
            .setTimeline(timeline)
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCE_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }

    private fun readPayload(): String {
        return try {
            val items = Tasks.await(
                Wearable.getDataClient(this).dataItems,
                2,
                TimeUnit.SECONDS,
            )
            var label = "Open the phone"
            for (item in items) {
                if (item.uri.path == INDEX_PATH) {
                    val stored = DataMapItem.fromDataItem(item)
                        .dataMap
                        .getString(PAYLOAD_KEY)
                    if (!stored.isNullOrBlank()) label = stored
                }
            }
            items.release()
            label
        } catch (_: IllegalStateException) {
            "Open the phone"
        } catch (_: ExecutionException) {
            "Open the phone"
        } catch (_: TimeoutException) {
            "Open the phone"
        }
    }

    private companion object {
        const val RESOURCE_VERSION: String = "1"
        const val INDEX_PATH: String = "/grannys-knees/index"
        const val PAYLOAD_KEY: String = "payload"
    }
}

/**
 * Sends one ache report to the phone. The event id makes a retry idempotent.
 */
object WatchQuickLog {
    /** Body for one left-knee ache. Retries must reuse [eventId]. */
    fun message(eventId: String): ByteArray =
        "$eventId\tknee\tleft\tache\t6".toByteArray(Charsets.UTF_8)

    fun sendKneeAche(client: MessageClient, nodeId: String, eventId: String = UUID.randomUUID().toString()) {
        client.sendMessage(nodeId, "/grannys-knees/log", message(eventId))
    }
}
