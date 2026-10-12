package com.rentalvalidator.app.presentation.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.NaniSerif
import com.rentalvalidator.app.presentation.theme.NaniTheme
import kotlin.math.abs
import kotlin.random.Random

/**
 * Paints a cover behind what it holds, at any size, as a material rather than a print: sapphire
 * for a person, petrol blue for a place ([unit]). The ground deepens across the diagonal, a soft
 * light falls from the upper-right corner and a neighbouring hue rises from the lower left, a
 * faint sheen crosses the light and a fine grain gives the surface a touch, the way a metal card
 * looks in the hand. A cover that stands for the record itself asks for a [relief]: when there is
 * room beside the name, the record's mark is pressed into the material against the right edge,
 * like the logo on a premium card, the person's initial from [seed] in the ledger's serif or a
 * place's [glyph]. It is lit on its lower lip and shadowed on its upper one, so it reads as
 * carved, not printed. Covers that only lend their material to something else go without it.
 *
 * [reveal] fades the whole cover in, for a card that becomes a cover when chosen; it is read
 * while drawing, so the fade never recomposes. A [quiet] cover only glows faintly with the
 * cover's color from the corner, over whatever the card is, for a card that should sit calmly in
 * the page. The relief runs past the edge by design, so the cover clips to its bounds; a caller
 * clips it to a shape before.
 */
@Composable
internal fun Modifier.engravedCover(
    seed: String,
    unit: Boolean,
    reveal: () -> Float = { 1f },
    quiet: Boolean = false,
    relief: Boolean = false,
    glyph: ImageVector? = null
): Modifier {
    val nani = NaniTheme.colors
    val tone = if (unit) nani.unitCover else nani.tenantCover
    val dark = nani.isDark
    val painter = glyph?.let { rememberVectorPainter(it) }
    val measurer = rememberTextMeasurer()
    val initial = remember(seed) { initialsOf(seed).take(1) }
    return clipToBounds().drawWithCache {
        val width = size.width
        val height = size.height
        if (quiet) {
            val wash = Brush.radialGradient(listOf(tone.lit.copy(alpha = if (dark) .2f else .11f), Color.Transparent),
                center = Offset(width, 0f), radius = maxOf(width * .7f, height * 1.8f))
            return@drawWithCache onDrawBehind {
                val alpha = reveal().coerceIn(0f, 1f)
                if (alpha > 0f) drawRect(wash, alpha = alpha)
            }
        }
        val ground = Brush.linearGradient(0f to tone.lit, .45f to tone.mid, 1f to tone.deep,
            start = Offset(width, 0f), end = Offset(width * .15f, height * 1.25f))
        val bounce = Brush.radialGradient(listOf(tone.bounce, Color.Transparent),
            center = Offset(width * .08f, height * 1.15f), radius = maxOf(width * .6f, height))
        val glow = Brush.radialGradient(0f to tone.glow, .5f to tone.glow.copy(alpha = tone.glow.alpha * .35f), 1f to Color.Transparent,
            center = Offset(width * .92f, -height * .12f), radius = maxOf(width * .75f, height * 1.3f))
        val sheen = Brush.linearGradient(0f to Color.Transparent, .4f to Color.Transparent, .5f to Color.White.copy(alpha = .045f),
            .6f to Color.Transparent, 1f to Color.Transparent, start = Offset(width * .4f, 0f), end = Offset(width * 1.05f, height))
        val grain = ShaderBrush(ImageShader(CoverGrain, TileMode.Repeated, TileMode.Repeated))
        val roomy = width >= ReliefMinWidth.dp.toPx() && height >= ReliefMinHeight.dp.toPx()
        val mark = when {
            !relief || !roomy -> null
            painter != null -> reliefFrame(width, height, aspect = 1f).let { frame ->
                Relief(frame) { dy, color ->
                    translate(frame.left, frame.top + dy) {
                        with(painter) { draw(Size(frame.width, frame.height), colorFilter = ColorFilter.tint(color)) }
                    }
                }
            }
            else -> {
                val text = measurer.measure(initial, TextStyle(fontFamily = NaniSerif, fontWeight = FontWeight.SemiBold,
                    fontSize = height.toSp()))
                val frame = reliefFrame(width, height, aspect = text.size.width.toFloat() / text.size.height)
                val scale = frame.height / text.size.height
                Relief(frame) { dy, color ->
                    translate(frame.left, frame.top + dy) { scale(scale, pivot = Offset.Zero) { drawText(text, color) } }
                }
            }
        }
        // The relief fades away from the light, so it surfaces under the lit corner and sinks back into the ground.
        val fade = mark?.frame?.let { frame ->
            Brush.linearGradient(0f to Color.Black, .45f to Color.Black.copy(alpha = .75f), 1f to Color.Black.copy(alpha = .08f),
                start = Offset(frame.left + frame.width, frame.top), end = Offset(frame.left, frame.top + frame.height))
        }
        val lip = 1.2.dp.toPx()
        onDrawBehind {
            val alpha = reveal().coerceIn(0f, 1f)
            if (alpha <= 0f) return@onDrawBehind
            drawRect(ground, alpha = alpha)
            drawRect(bounce, alpha = alpha)
            drawRect(glow, alpha = alpha)
            if (mark != null && fade != null) {
                // Drawn three times, a hair apart: the shadowed upper lip, the lit lower lip and the pressed face.
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                mark.press(this, -lip, Color.Black.copy(alpha = .13f * alpha))
                mark.press(this, lip, Color.White.copy(alpha = (if (dark) .08f else .11f) * alpha))
                mark.press(this, 0f, tone.deep.copy(alpha = (if (dark) .26f else .22f) * alpha))
                drawRect(fade, blendMode = BlendMode.DstIn)
                drawContext.canvas.restore()
            }
            drawRect(sheen, alpha = alpha)
            drawRect(grain, alpha = CoverGrainStrength * alpha)
        }
    }
}

/**
 * The edge of a cover catching the light: a fine line along the top that fades down the sides,
 * following a shape with [corner] corners. [reveal] fades it in with the cover.
 */
@Composable
internal fun Modifier.coverRim(corner: Dp, reveal: () -> Float = { 1f }): Modifier {
    val strength = if (NaniTheme.colors.isDark) .24f else .3f
    return drawWithCache {
        val stroke = 1.dp.toPx()
        val rim = Brush.verticalGradient(0f to Color.White.copy(alpha = strength), .5f to Color.White.copy(alpha = .04f),
            1f to Color.Transparent)
        val radius = CornerRadius((corner.toPx() - stroke / 2).coerceAtLeast(0f))
        onDrawWithContent {
            drawContent()
            val alpha = reveal().coerceIn(0f, 1f)
            if (alpha > 0f) drawRoundRect(rim, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke),
                radius, alpha = alpha, style = Stroke(stroke))
        }
    }
}

/** A mark pressed into a cover: the box it sits in, and how to draw it [press]ed at a vertical offset in a color. */
private class Relief(val frame: ReliefFrame, val press: DrawScope.(dy: Float, color: Color) -> Unit)

/** How strongly the grain shows on a cover. */
private const val CoverGrainStrength = .045f

/**
 * A tile of fine grain, light and dark specks around the middle, drawn once and repeated across
 * every cover. It keeps the gradients from banding and gives the material its touch.
 */
private val CoverGrain: ImageBitmap by lazy {
    val side = 128
    val levels = 6
    val random = Random(7)
    val light = List(levels) { mutableListOf<Offset>() }
    val dark = List(levels) { mutableListOf<Offset>() }
    for (y in 0 until side) for (x in 0 until side) {
        val value = random.nextFloat() * 2f - 1f
        val level = (abs(value) * levels).toInt().coerceAtMost(levels - 1)
        (if (value > 0f) light else dark)[level].add(Offset(x + .5f, y + .5f))
    }
    ImageBitmap(side, side).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { strokeWidth = 1f; strokeCap = StrokeCap.Square }
        for (level in 0 until levels) {
            val alpha = (level + 1f) / levels
            paint.color = Color.White.copy(alpha = alpha)
            canvas.drawPoints(PointMode.Points, light[level], paint)
            paint.color = Color.Black.copy(alpha = alpha)
            canvas.drawPoints(PointMode.Points, dark[level], paint)
        }
    }
}
