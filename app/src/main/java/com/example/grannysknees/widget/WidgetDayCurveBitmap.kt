package com.example.grannysknees.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.math.BigDecimal

/**
 * Draws the full local-day index curve into one bitmap so a short widget cell
 * can show every scored hour, including the current one.
 *
 * Gaps between hours stay blank. Missing hours are not drawn as zero.
 */
internal object WidgetDayCurveBitmap {
    private const val WIDTH: Int = 640
    private const val HEIGHT: Int = 160
    private const val HOUR_GAP: Float = 1.5f / 24f

    /** Curve for [model], or null when the day has no scored hour. */
    fun draw(model: WidgetUiModel.Available): Bitmap? {
        if (model.hours.isEmpty()) return null
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF07884F.toInt()
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            strokeWidth = 3f
        }
        var previous: WidgetHourScore? = null
        model.hours.forEach { hour ->
            val prior = previous
            if (prior != null && hour.fraction - prior.fraction <= HOUR_GAP) {
                canvas.drawLine(
                    x(prior.fraction),
                    y(prior.score),
                    x(hour.fraction),
                    y(hour.score),
                    line,
                )
            }
            previous = hour
        }
        val now = model.nowFraction
        if (now != null) {
            canvas.drawLine(x(now), 8f, x(now), HEIGHT - 8f, mark)
        }
        return bitmap
    }

    private fun x(fraction: Float): Float = fraction.coerceIn(0f, 1f) * (WIDTH - 16) + 8f

    private fun y(score: BigDecimal): Float {
        val unit = score.toFloat().coerceIn(0f, 10f) / 10f
        return (HEIGHT - 12) - unit * (HEIGHT - 24)
    }
}
