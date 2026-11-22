package com.aetherealmind.grannysknees.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aetherealmind.grannysknees.R

/**
 * One chosen place on the figure.
 * [rating] runs from clear at 0 to red at 10. Aches use a horizontal stack of dashes;
 * stiffness is colour only (fill wash + soft bloom, no ticks).
 */
internal data class FigureMark(
    val part: String,
    val side: String?,
    val rating: Int,
    val ache: Boolean,
)

/**
 * An artist's figure, with the joints joined together.
 * Chosen places are filled from clear to red. The left of the figure is the person's left.
 */
@Composable
internal fun BodyFigure(
    marks: List<FigureMark>,
    onSelect: (part: String, side: String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val outline = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BodyMap.WIDTH / BodyMap.HEIGHT),
    ) {
        Canvas(
            Modifier
                .matchParentSize()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(onSelect) {
                            detectTapGestures { offset ->
                                val spot = BodyMap.spotAt(
                                    x = offset.x / size.width * BodyMap.WIDTH,
                                    y = offset.y / size.height * BodyMap.HEIGHT,
                                )
                                if (spot != null) {
                                    onSelect(spot.part, spot.side)
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            drawFigure(outline, paper, marks)
        }
        BodyMap.spots.forEach { spot ->
            val diameter = maxWidth * (spot.radius * 2f / BodyMap.WIDTH)
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * (spot.x / BodyMap.WIDTH) - diameter / 2,
                        y = maxHeight * (spot.y / BodyMap.HEIGHT) - diameter / 2,
                    )
                    .size(diameter)
                    .semantics {
                        contentDescription = bodySpotName(spot.part, spot.side)
                        if (enabled) {
                            onClick {
                                onSelect(spot.part, spot.side)
                                true
                            }
                        }
                    },
            )
        }
    }
}

private fun DrawScope.drawFigure(
    outline: Color,
    paper: Color,
    marks: List<FigureMark>,
) {
    val scale = size.width / BodyMap.WIDTH
    val stroke = Stroke(width = scale * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val seam = Stroke(width = scale * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val bolts = mutableListOf<BoltSite>()
    fun mark(name: String, place: String? = null): FigureMark? {
        return marks.firstOrNull { it.part == name && it.side == place }
    }

    filledPath(footSteps(left = true), scale, mark("foot", "left"), paper, outline, stroke, bolts, 42f to 152f)
    filledPath(footSteps(left = false), scale, mark("foot", "right"), paper, outline, stroke, bolts, 58f to 152f)
    limb(39.2f, 114f, 46.8f, 148f, 3.8f, scale, mark("ankle", "left"), paper, outline, stroke)
    limb(53.2f, 114f, 60.8f, 148f, 3.8f, scale, mark("ankle", "right"), paper, outline, stroke)
    // Hip bloom sits at the hip joint (near the seam), not mid-thigh.
    limb(37.4f, 78f, 48.2f, 116f, 4.8f, scale, mark("hip", "left"), paper, outline, stroke, bolts, 42.8f to 84f)
    limb(51.8f, 78f, 62.6f, 116f, 4.8f, scale, mark("hip", "right"), paper, outline, stroke, bolts, 57.2f to 84f)
    joint(43f, 114.5f, 4.2f, 3.6f, scale, mark("knee", "left"), paper, outline, stroke, bolts)
    joint(57f, 114.5f, 4.2f, 3.6f, scale, mark("knee", "right"), paper, outline, stroke, bolts)
    joint(43f, 146.2f, 2.8f, 2.3f, scale, mark("ankle", "left"), paper, outline, stroke, bolts)
    joint(57f, 146.2f, 2.8f, 2.3f, scale, mark("ankle", "right"), paper, outline, stroke, bolts)

    limb(24.2f, 58f, 32.4f, 90f, 3.8f, scale, mark("wrist", "left"), paper, outline, stroke)
    limb(67.6f, 58f, 75.8f, 90f, 3.8f, scale, mark("wrist", "right"), paper, outline, stroke)
    limb(23.2f, 34f, 33.2f, 64f, 4.6f, scale, mark("shoulder", "left"), paper, outline, stroke)
    limb(66.8f, 34f, 76.8f, 64f, 4.6f, scale, mark("shoulder", "right"), paper, outline, stroke)
    filledPath(handSteps(left = true), scale, mark("hand", "left"), paper, outline, stroke, bolts, 28.5f to 96f)
    filledPath(handSteps(left = false), scale, mark("hand", "right"), paper, outline, stroke, bolts, 71.5f to 96f)
    joint(27.6f, 62f, 3.8f, 3.2f, scale, mark("elbow", "left"), paper, outline, stroke, bolts)
    joint(72.4f, 62f, 3.8f, 3.2f, scale, mark("elbow", "right"), paper, outline, stroke, bolts)
    joint(28.2f, 89f, 2.6f, 2.2f, scale, mark("wrist", "left"), paper, outline, stroke, bolts)
    joint(71.8f, 89f, 2.6f, 2.2f, scale, mark("wrist", "right"), paper, outline, stroke, bolts)
    joint(34f, 34f, 5.6f, 4.8f, scale, mark("shoulder", "left"), paper, outline, stroke, bolts)
    joint(66f, 34f, 5.6f, 4.8f, scale, mark("shoulder", "right"), paper, outline, stroke, bolts)

    val torso = pathOf(mirrored(trace(torsoSteps())), scale)
    val back = mark("back")
    drawPath(torso, paper)
    if (back != null) {
        drawPath(torso, washOf(back))
        rememberBoltSite(bolts, back, 50f, 62f)
    }
    drawPath(torso, outline, style = stroke)
    cloudAround(
        scale,
        back,
        50f,
        62f,
    ) { grow, colour ->
        drawPath(
            torso,
            colour,
            style = Stroke(width = grow * 2f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    openCurve(chestSeam(), scale, outline, seam)
    openCurve(hipSeam(), scale, outline, seam)
    limb(47f, 19.5f, 53f, 31f, 1.6f, scale, mark("neck"), paper, outline, stroke, bolts, 50f to 25f)
    joint(50f, 12f, 7.6f, 9.2f, scale, mark("head"), paper, outline, stroke, bolts)
    // Whole-body cue for the General overall place (no single joint spot).
    // Ache bolts sit in the upper chest (heart height), centred on the torso.
    mark(OVERALL_PART)?.let { overall ->
        rememberBoltSite(bolts, overall, 50f, 48f)
        overallOvalAura(scale, overall)
    }
    bolts.forEach { site -> drawBoltSite(site, scale) }
}

/** Clear at rest, red at the top of the scale. */
internal fun feelingColour(rating: Int): Color {
    val amount = rating.coerceIn(0, 10) / 10f
    return acheRed.copy(alpha = amount)
}

private val acheRed: Color = Color(0xFFD23A32)

private fun washOf(mark: FigureMark): Color {
    val amount = mark.rating.coerceIn(0, 10) / 10f
    return acheRed.copy(alpha = 0.8f * amount)
}

/**
 * How far the colour leaves the outline.
 * Clear stays inside. The cloud starts as soon as there is some red, then grows
 * past the black line so the ache reads as a bloom outside the figure.
 */
internal fun cloudReach(rating: Int): Float {
    val amount = rating.coerceIn(0, 10) / 10f
    return kotlin.math.sqrt(amount) * 16f
}

/**
 * Ache rate as bolt count / layout, with brightness rising inside each tier.
 *
 * Boundary at 3: the second dash appears at 3 (one for 1–2; two for 3–5).
 * 6–8 adds a third; 9 uses four dashes; 10 is four dots.
 */
internal fun acheBoltLayout(rating: Int): AcheBoltLayout {
    val bounded = rating.coerceIn(0, 10)
    return when (bounded) {
        0 -> AcheBoltLayout.None
        in 1..2 -> AcheBoltLayout.Parallel(
            count = 1,
            brightness = tierBrightness(bounded, tierLow = 1, tierHigh = 2),
        )
        in 3..5 -> AcheBoltLayout.Parallel(
            count = 2,
            brightness = tierBrightness(bounded, tierLow = 3, tierHigh = 5),
        )
        in 6..8 -> AcheBoltLayout.Parallel(
            count = 3,
            brightness = tierBrightness(bounded, tierLow = 6, tierHigh = 8),
        )
        9 -> AcheBoltLayout.Parallel(
            count = 4,
            brightness = 0f,
        )
        else -> AcheBoltLayout.Dots(
            count = 4,
            brightness = 1f,
        )
    }
}

/** 0 at the bottom of a tier, 1 at the top — brightness within that step only. */
internal fun tierBrightness(rating: Int, tierLow: Int, tierHigh: Int): Float {
    if (tierHigh <= tierLow) return 1f
    return ((rating - tierLow).toFloat() / (tierHigh - tierLow).toFloat()).coerceIn(0f, 1f)
}

/** How ache marks are drawn for a rating. */
internal sealed class AcheBoltLayout {
    data object None : AcheBoltLayout()

    data class Parallel(
        val count: Int,
        val brightness: Float,
    ) : AcheBoltLayout()

    /** Top of the scale: a short row of filled dots. */
    data class Dots(
        val count: Int,
        val brightness: Float,
    ) : AcheBoltLayout()
}

/** One joint centre that should show ache bolts. */
private data class BoltSite(
    val x: Float,
    val y: Float,
    val mark: FigureMark,
)

private fun rememberBoltSite(
    bolts: MutableList<BoltSite>,
    mark: FigureMark?,
    x: Float,
    y: Float,
) {
    // Bolts are for aches only; stiffness stays colour-wash only.
    if (mark == null || !mark.ache || mark.rating <= 0) return
    bolts += BoltSite(x, y, mark)
}

private val cloudPuffs: List<Pair<Float, Float>> = listOf(
    0.42f to -0.24f,
    -0.36f to 0.28f,
    0.14f to 0.4f,
    -0.16f to -0.38f,
)

/**
 * Soft oval aura around the whole figure for General overall.
 * One continuous radiated-heat glow (not concentric wave rings): warmer near
 * the body, fading smoothly outward. As rating rises, heat fills inward and
 * the outer bloom grows.
 */
private fun DrawScope.overallOvalAura(scale: Float, mark: FigureMark) {
    val rating = mark.rating.coerceIn(0, 10)
    val reach = cloudReach(rating)
    if (reach < 0.3f) {
        return
    }
    val presence = 0.45f + 0.55f * (rating / 10f)
    val heatAmount = rating / 10f
    val centreX = 50f
    val centreY = 78f
    val bodyRadiusX = 44f
    val bodyRadiusY = 80f
    val outwardBloom = reach * 2.4f + 8f
    val outerRadiusX = bodyRadiusX + 6f + outwardBloom
    val outerRadiusY = bodyRadiusY + 6f + outwardBloom
    // Centre fills with heat as the rating climbs; low ratings stay quieter mid-body.
    val centreHeat = acheRed.copy(alpha = 0.18f * presence + 0.48f * presence * heatAmount)
    val bodyHeat = acheRed.copy(alpha = 0.62f * presence)
    val nearEdge = acheRed.copy(alpha = 0.38f * presence)
    val pivot = Offset(centreX * scale, centreY * scale)
    val outerY = outerRadiusY.coerceAtLeast(1f)
    val outerX = outerRadiusX.coerceAtLeast(1f)
    val radiusPx = outerY * scale
    // Body sits partway out from the centre along the ellipse.
    val bodyStop = (
        (bodyRadiusX / outerX) + (bodyRadiusY / outerY)
        ).times(0.5f).coerceIn(0.2f, 0.75f)
    val nearStop = (bodyStop + (1f - bodyStop) * 0.35f).coerceIn(bodyStop + 0.02f, 0.9f)
    scale(
        scaleX = outerX / outerY,
        scaleY = 1f,
        pivot = pivot,
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to centreHeat,
                    bodyStop to bodyHeat,
                    nearStop to nearEdge,
                    1f to Color.Transparent,
                ),
                center = pivot,
                radius = radiusPx,
            ),
            radius = radiusPx,
            center = pivot,
        )
    }
}

/**
 * Draws the ache bloom after the outline, so the red leaves the figure
 * instead of being covered by the paper fill.
 */
private fun DrawScope.cloudAround(
    scale: Float,
    mark: FigureMark?,
    centreX: Float,
    centreY: Float,
    paint: DrawScope.(grow: Float, colour: Color) -> Unit,
) {
    if (mark == null) {
        return
    }
    // Colour bloom is the stiffness (and shared wash) rate cue; ache bolts
    // are drawn separately. Do not gate this on ache — that left stiffness
    // with almost no visible scale after bolts moved off it.
    val reach = cloudReach(mark.rating)
    if (reach < 0.3f) {
        return
    }
    val presence = 0.45f + 0.55f * (mark.rating.coerceIn(0, 10) / 10f)
    softDisc(centreX, centreY, reach * 1.2f, scale, acheRed.copy(alpha = 0.28f * presence))
    cloudPuffs.forEach { (driftX, driftY) ->
        softDisc(
            centreX + driftX * reach * 0.34f,
            centreY + driftY * reach * 0.34f,
            reach * 1.05f,
            scale,
            acheRed.copy(alpha = 0.2f * presence),
        )
    }
    val layers = 18
    for (layer in layers downTo 1) {
        val fraction = layer / layers.toFloat()
        val fade = 1f - fraction
        paint(reach * fraction, acheRed.copy(alpha = 0.08f * presence * fade * fade))
    }
}

/**
 * A soft ring of colour that peaks away from the centre, so the bloom
 * sits outside the outline rather than under the paper fill.
 */
private fun DrawScope.softDisc(
    centreX: Float,
    centreY: Float,
    radius: Float,
    scale: Float,
    colour: Color,
) {
    val centre = Offset(centreX * scale, centreY * scale)
    val radiusPx = radius * scale
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.35f to colour.copy(alpha = colour.alpha * 0.35f),
                0.65f to colour,
                1f to Color.Transparent,
            ),
            center = centre,
            radius = radiusPx,
        ),
        radius = radiusPx,
        center = centre,
    )
}

private fun DrawScope.joint(
    centreX: Float,
    centreY: Float,
    radiusX: Float,
    radiusY: Float,
    scale: Float,
    selected: FigureMark?,
    paper: Color,
    outline: Color,
    stroke: Stroke,
    bolts: MutableList<BoltSite>,
) {
    val topLeft = Offset((centreX - radiusX) * scale, (centreY - radiusY) * scale)
    val oval = Size(radiusX * 2f * scale, radiusY * 2f * scale)
    drawOval(paper, topLeft, oval)
    if (selected != null) {
        drawOval(washOf(selected), topLeft, oval)
        rememberBoltSite(bolts, selected, centreX, centreY)
    }
    drawOval(outline, topLeft, oval, style = stroke)
    cloudAround(scale, selected, centreX, centreY) { grow, colour ->
        drawOval(
            colour,
            Offset((centreX - radiusX - grow) * scale, (centreY - radiusY - grow) * scale),
            Size((radiusX + grow) * 2f * scale, (radiusY + grow) * 2f * scale),
        )
    }
}

private fun DrawScope.limb(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
    scale: Float,
    selected: FigureMark?,
    paper: Color,
    outline: Color,
    stroke: Stroke,
    bolts: MutableList<BoltSite>? = null,
    boltAt: Pair<Float, Float>? = null,
) {
    val topLeft = Offset(left * scale, top * scale)
    val body = Size((right - left) * scale, (bottom - top) * scale)
    val corner = CornerRadius(radius * scale, radius * scale)
    drawRoundRect(paper, topLeft, body, corner)
    if (selected != null) {
        drawRoundRect(washOf(selected), topLeft, body, corner)
        if (boltAt != null && bolts != null) {
            rememberBoltSite(bolts, selected, boltAt.first, boltAt.second)
        }
    }
    drawRoundRect(outline, topLeft, body, corner, style = stroke)
    val cloudX = boltAt?.first ?: ((left + right) / 2f)
    val cloudY = boltAt?.second ?: ((top + bottom) / 2f)
    cloudAround(
        scale,
        selected,
        cloudX,
        cloudY,
    ) { grow, colour ->
        drawRoundRect(
            colour,
            Offset((left - grow) * scale, (top - grow) * scale),
            Size((right - left + grow * 2f) * scale, (bottom - top + grow * 2f) * scale),
            CornerRadius((radius + grow) * scale, (radius + grow) * scale),
        )
    }
}

private fun DrawScope.filledPath(
    steps: List<Step>,
    scale: Float,
    selected: FigureMark?,
    paper: Color,
    outline: Color,
    stroke: Stroke,
    bolts: MutableList<BoltSite>,
    boltAt: Pair<Float, Float>,
) {
    val shape = pathOf(trace(steps), scale)
    drawPath(shape, paper)
    if (selected != null) {
        drawPath(shape, washOf(selected))
        rememberBoltSite(bolts, selected, boltAt.first, boltAt.second)
    }
    drawPath(shape, outline, style = stroke)
    cloudAround(scale, selected, boltAt.first, boltAt.second) { grow, colour ->
        drawPath(
            shape,
            colour,
            style = Stroke(width = grow * 2f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Deep electric blue, so the ache bolts stay clear of the red wash. */
private val boltBlue: Color = Color(0xFF0026E6)

/** Brighter end of the ache ladder (still distinct from the red wash). */
private val boltBright: Color = Color(0xFF6B8CFF)

/** Overall chest bolts grow with rating; joint bolts keep the default size. */
private fun overallBoltSize(mark: FigureMark): Float {
    if (mark.part != OVERALL_PART) {
        return 1f
    }
    return 1.45f + 0.75f * (mark.rating.coerceIn(0, 10) / 10f)
}

private fun DrawScope.drawBoltSite(site: BoltSite, scale: Float) {
    if (!site.mark.ache) {
        return
    }
    val size = overallBoltSize(site.mark)
    when (val layout = acheBoltLayout(site.mark.rating)) {
        AcheBoltLayout.None -> Unit
        is AcheBoltLayout.Parallel -> {
            val colour = acheBoltColour(layout.brightness)
            // Horizontal stack: short dashes side by side.
            val spread = 2.8f * size
            val start = -(layout.count - 1) / 2f
            for (index in 0 until layout.count) {
                val dx = (start + index) * spread
                drawAcheTick(
                    site.x + dx,
                    site.y,
                    scale,
                    colour,
                    length = size,
                    strokeWidth = 1.45f * size,
                )
            }
        }
        is AcheBoltLayout.Dots -> {
            val colour = acheBoltColour(layout.brightness)
            val spread = 2.9f * size
            val start = -(layout.count - 1) / 2f
            val radius = 1.05f * size
            for (index in 0 until layout.count) {
                val dx = (start + index) * spread
                drawCircle(
                    colour,
                    radius = radius * scale,
                    center = Offset((site.x + dx) * scale, site.y * scale),
                )
            }
        }
    }
}

private fun acheBoltColour(brightness: Float): Color {
    val amount = brightness.coerceIn(0f, 1f)
    return lerp(boltBlue, boltBright, amount).copy(alpha = 0.55f + 0.45f * amount)
}

/**
 * Soft horizontal ache dash — rounded capsule in a side-by-side stack.
 */
private fun DrawScope.drawAcheTick(
    x: Float,
    y: Float,
    scale: Float,
    colour: Color,
    length: Float = 1f,
    strokeWidth: Float = 1.45f,
) {
    val grow = length.coerceAtLeast(0.5f)
    val halfWidth = 1.15f * grow
    drawLine(
        colour,
        start = Offset((x - halfWidth) * scale, y * scale),
        end = Offset((x + halfWidth) * scale, y * scale),
        strokeWidth = scale * strokeWidth * 1.35f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.openCurve(steps: List<Step>, scale: Float, outline: Color, stroke: Stroke) {
    drawPath(pathOf(trace(steps), scale, closePath = false), outline, style = stroke)
}

private fun torsoSteps(): List<Step> {
    return listOf(
        Step(47f, 27f, move = true),
        Step(36f, 36f, 40f, 29f),
        Step(36f, 60f, 34f, 50f),
        Step(36f, 76f, 38f, 68f),
        Step(43f, 90f, 38f, 86f),
        Step(50f, 90f, 47f, 94f),
    )
}

private fun chestSeam(): List<Step> {
    return listOf(Step(40f, 52f, move = true), Step(60f, 52f, 50f, 55f))
}

private fun hipSeam(): List<Step> {
    return listOf(Step(40f, 70f, move = true), Step(60f, 70f, 50f, 73f))
}

private fun handSteps(left: Boolean): List<Step> {
    return mirroredSteps(
        listOf(
            Step(25f, 88f, move = true),
            Step(24.5f, 102f, 23f, 95f),
            Step(32f, 101f, 28f, 106f),
            Step(32f, 90f, 33.5f, 96f),
            Step(25f, 88f, 28f, 87f),
        ),
        left,
    )
}

private fun footSteps(left: Boolean): List<Step> {
    return mirroredSteps(
        listOf(
            Step(38f, 146f, move = true),
            Step(36f, 155f, 35f, 151f),
            Step(48f, 155f, 42f, 159f),
            Step(45f, 146f, 49f, 151f),
            Step(41f, 145f, line = true),
        ),
        left,
    )
}

private fun mirroredSteps(steps: List<Step>, left: Boolean): List<Step> {
    if (left) {
        return steps
    }
    return steps.map { step ->
        step.copy(x = BodyMap.WIDTH - step.x, controlX = BodyMap.WIDTH - step.controlX)
    }
}

/** One piece of the outline. A line is straight. Anything else bends. */
private data class Step(
    val x: Float,
    val y: Float,
    val controlX: Float = x,
    val controlY: Float = y,
    val move: Boolean = false,
    val line: Boolean = false,
)

private fun trace(steps: List<Step>, stepsPerCurve: Int = 14): List<Pair<Float, Float>> {
    val points = ArrayList<Pair<Float, Float>>()
    var current = 0f to 0f
    steps.forEach { step ->
        if (step.move || step.line) {
            current = step.x to step.y
            points += current
        } else {
            val startX = current.first
            val startY = current.second
            for (index in 1..stepsPerCurve) {
                val t = index.toFloat() / stepsPerCurve
                val remaining = 1f - t
                points += (
                    remaining * remaining * startX +
                        2f * remaining * t * step.controlX +
                        t * t * step.x
                    ) to (
                    remaining * remaining * startY +
                        2f * remaining * t * step.controlY +
                        t * t * step.y
                    )
            }
            current = step.x to step.y
        }
    }
    return points
}

private fun mirrored(left: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    val right = left.dropLast(1).asReversed().map { (x, y) -> (BodyMap.WIDTH - x) to y }
    return left + right
}

private fun pathOf(
    points: List<Pair<Float, Float>>,
    scale: Float,
    closePath: Boolean = true,
): Path {
    return Path().apply {
        val first = points.first()
        moveTo(first.first * scale, first.second * scale)
        points.drop(1).forEach { (x, y) -> lineTo(x * scale, y * scale) }
        if (closePath) {
            close()
        }
    }
}
