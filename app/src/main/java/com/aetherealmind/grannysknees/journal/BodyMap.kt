package com.aetherealmind.grannysknees.journal

import kotlin.math.hypot

/**
 * One tappable place on the front-view body.
 *
 * [x] and [y] use a 100 by 160 figure. The left of the screen is the
 * person's left, as in a mirror.
 */
internal data class BodySpot(
    val part: String,
    val side: String?,
    val x: Float,
    val y: Float,
    val radius: Float,
    val highlightRx: Float = radius,
    val highlightRy: Float = radius,
)

/** Places on the body that match the How you feel parts. */
internal object BodyMap {
    const val WIDTH: Float = 100f
    const val HEIGHT: Float = 160f

    val spots: List<BodySpot> = listOf(
        BodySpot("head", null, 50f, 12f, 9f),
        BodySpot("neck", null, 50f, 26f, 4.5f),
        BodySpot("shoulder", "left", 34f, 34f, 6f),
        BodySpot("shoulder", "right", 66f, 34f, 6f),
        BodySpot("shoulder", "left", 28.2f, 48f, 10f),
        BodySpot("shoulder", "right", 71.8f, 48f, 10f),
        BodySpot("elbow", "left", 27.6f, 62f, 4.2f),
        BodySpot("elbow", "right", 72.4f, 62f, 4.2f),
        BodySpot("wrist", "left", 28.2f, 80f, 12f),
        BodySpot("wrist", "right", 71.8f, 80f, 12f),
        BodySpot("hand", "left", 28.5f, 96f, 7f),
        BodySpot("hand", "right", 71.5f, 96f, 7f),
        BodySpot("back", null, 50f, 62f, 26f),
        BodySpot("hip", "left", 42.8f, 97f, 15f),
        BodySpot("hip", "right", 57.2f, 97f, 15f),
        BodySpot("knee", "left", 43f, 114.5f, 4.5f),
        BodySpot("knee", "right", 57f, 114.5f, 4.5f),
        BodySpot("ankle", "left", 43f, 132f, 15f),
        BodySpot("ankle", "right", 57f, 132f, 15f),
        BodySpot("foot", "left", 42f, 153f, 6f),
        BodySpot("foot", "right", 58f, 153f, 6f),
    )

    /**
     * The spot under [x], [y], preferring the smaller area when two overlap.
     */
    fun spotAt(x: Float, y: Float): BodySpot? {
        return spots
            .filter { spot -> hypot(x - spot.x, y - spot.y) <= spot.radius }
            .minByOrNull { it.radius }
    }
}

/** Spoken name for a body spot, such as "Left knee" or "Head". */
internal fun bodySpotName(part: String, side: String?): String {
    return quietReportTitle(side, part, "x").removeSuffix(" x")
}
