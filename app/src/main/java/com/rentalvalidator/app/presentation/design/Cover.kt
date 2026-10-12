package com.rentalvalidator.app.presentation.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.NaniTheme
import kotlin.math.abs
import kotlin.random.Random

/**
 * Paints a cover behind what it holds, at any size, as light on a material rather than a print:
 * sapphire for a person, petrol blue for a place ([unit]). The ground deepens across the
 * diagonal, a bright neighbouring hue blooms from the upper-right corner like a light source, a
 * second hue rises softly from below (violet under the sapphire, sea green under the petrol), the
 * left side settles into the deep tone so the name always reads, and a fine grain gives the
 * surface a touch, the way a metal card looks in the hand. There are no lines or marks: the cover
 * is told apart by its light and color alone, and the record's own mark sits on top of it.
 *
 * [reveal] fades the whole cover in, for a card that becomes a cover when chosen; it is read
 * while drawing, so the fade never recomposes. A [quiet] cover only glows faintly with the
 * cover's color from the corner, over whatever the card is, for a card that should sit calmly in
 * the page. [seed] names the record the cover stands for; every record of a kind gets the same
 * material, so the cover never varies with it.
 */
@Composable
internal fun Modifier.engravedCover(
    seed: String,
    unit: Boolean,
    reveal: () -> Float = { 1f },
    quiet: Boolean = false
): Modifier {
    val nani = NaniTheme.colors
    val tone = if (unit) nani.unitCover else nani.tenantCover
    val dark = nani.isDark
    return drawWithCache {
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
        val ground = Brush.linearGradient(0f to tone.mid, 1f to tone.deep, start = Offset(width, 0f), end = Offset(0f, height))
        val depth = Brush.radialGradient(listOf(tone.deep.copy(alpha = .7f), Color.Transparent),
            center = Offset(-width * .05f, height * .45f), radius = maxOf(width * .5f, height * 1.2f))
        val bounce = Brush.radialGradient(listOf(tone.bounce, Color.Transparent),
            center = Offset(width * .78f, height * 1.35f), radius = maxOf(width * .7f, height * 1.5f))
        val glow = Brush.radialGradient(0f to tone.glow, .5f to tone.glow.copy(alpha = tone.glow.alpha * .4f), 1f to Color.Transparent,
            center = Offset(width * .9f, -height * .1f), radius = maxOf(width * .72f, height * 1.4f))
        val grain = ShaderBrush(ImageShader(CoverGrain, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind {
            val alpha = reveal().coerceIn(0f, 1f)
            if (alpha <= 0f) return@onDrawBehind
            drawRect(ground, alpha = alpha)
            drawRect(depth, alpha = alpha)
            drawRect(bounce, alpha = alpha)
            drawRect(glow, alpha = alpha)
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
