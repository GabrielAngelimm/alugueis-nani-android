package com.rentalvalidator.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.ShadowAmbient

/**
 * Cached, achromatic falloff. No RenderNode elevation or Material tonal overlay:
 * depth remains stable on Android renderers that tint hardware spot shadows.
 */
@Composable
private fun Modifier.surfaceDepth(shape: Shape, elevation: Dp): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val depth = if (dark) elevation + 1.dp else elevation
    val opacity = if (dark) .58f else .13f
    return drawWithCache {
        val blur = depth.toPx() * if (dark) 2.5f else 2f
        val offset = depth.toPx() * if (dark) .65f else .4f
        val layers = if (dark) 20 else 12
        val outlines = (layers downTo 1).map { layer ->
            val spread = blur * layer / layers
            Triple(
                shape.createOutline(Size(size.width + spread * 2, size.height + spread * 2), layoutDirection, this),
                spread,
                opacity * (1f - layer.toFloat() / (layers + 1)) / layers
            )
        }
        val interior = Path().apply {
            when (val outline = shape.createOutline(size, layoutDirection, this@drawWithCache)) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        onDrawBehind {
            clipPath(interior, clipOp = ClipOp.Difference) {
            outlines.forEach { (outline, spread, alpha) ->
                translate(left = -spread, top = offset - spread) {
                    drawOutline(outline, ShadowAmbient.copy(alpha = alpha))
                }
            }
            }
        }
    }
}

@Composable fun Modifier.premiumShadow(shape: Shape): Modifier = surfaceDepth(shape, 2.dp)
@Composable fun Modifier.prominentShadow(shape: Shape): Modifier = surfaceDepth(shape, 6.dp)
@Composable fun Modifier.groupedLiftShadow(shape: Shape, darkTheme: Boolean = false): Modifier =
    surfaceDepth(shape, if (darkTheme) 1.dp else 3.dp)
@Composable fun Modifier.elevatedControlShadow(shape: Shape, darkTheme: Boolean = false): Modifier =
    surfaceDepth(shape, if (darkTheme) 1.dp else 4.dp)
@Composable fun Modifier.floatingShadow(shape: Shape): Modifier = surfaceDepth(shape, 6.dp)
@Composable fun Modifier.navigationShadow(shape: Shape): Modifier = surfaceDepth(shape, 6.dp)

/** Neutral edge light complements black shadows on dark surfaces. */
@Composable
fun surfaceDepthBorder(lightAlpha: Float = .5f): BorderStroke {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < .5f) {
        BorderStroke(1.dp, Brush.verticalGradient(listOf(
            Color.White.copy(alpha=.12f), Color.White.copy(alpha=.045f), Color.Black.copy(alpha=.18f)
        )))
    } else BorderStroke(1.dp, colors.outlineVariant.copy(alpha=lightAlpha))
}
