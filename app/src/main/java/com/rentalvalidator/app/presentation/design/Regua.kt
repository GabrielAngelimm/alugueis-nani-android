package com.rentalvalidator.app.presentation.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.theme.NaniTheme

/** One tenant in the month's ruler; [weight] is the rent, so larger rents take more room. */
data class ReguaPart(val weight: Float, val kind: StatusKind)

/**
 * The régua: the month as a ruler of rents. Every tenant is a segment proportional to
 * the rent they owe, filled when received. It answers "how much of the month is in"
 * and "who is missing" in one glance. It is drawn once with a single left-to-right reveal.
 */
@Composable
fun Regua(parts: List<ReguaPart>, description: String, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val colors = NaniTheme.colors
    val inspection = LocalInspectionMode.current
    val reveal = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(760, easing = AppMotion.Settle)) }
    val total = parts.sumOf { it.weight.toDouble().coerceAtLeast(0.0) }.toFloat()
    Canvas(modifier.fillMaxWidth().height(height).clipToBounds().semantics { contentDescription = description }) {
        val gap = 3.dp.toPx()
        val radius = CornerRadius(size.height / 2, size.height / 2)
        if (parts.isEmpty() || total <= 0f) {
            drawRoundRect(colors.track, cornerRadius = radius)
            return@Canvas
        }
        val usable = size.width - gap * (parts.size - 1)
        var x = 0f
        val bounds = parts.map { part ->
            val width = (usable * part.weight.coerceAtLeast(0f) / total).coerceAtLeast(2.dp.toPx())
            (x to width).also { x += width + gap }
        }
        bounds.forEach { (start, width) ->
            drawRoundRect(colors.track, Offset(start, 0f), Size(width, size.height), radius)
        }
        clipRect(right = size.width * reveal.value) {
            parts.zip(bounds).forEach { (part, bound) ->
                val fill = when (part.kind) {
                    StatusKind.SUCCESS -> colors.paid.ink
                    StatusKind.INFO -> colors.review.ink.copy(alpha = .7f)
                    StatusKind.ERROR -> colors.overdue.ink.copy(alpha = .38f)
                    else -> null
                }
                if (fill != null) drawRoundRect(fill, Offset(bound.first, 0f), Size(bound.second, size.height), radius)
            }
        }
    }
}

/** Legend entry for the ruler: a sample of the segment followed by its count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReguaLegend(entries: List<Triple<StatusKind, Int, String>>, modifier: Modifier = Modifier) {
    val colors = NaniTheme.colors
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.filter { it.second > 0 }.forEach { (kind, count, label) ->
            Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(width = 14.dp, height = 8.dp).clearAndSetSemantics {}) {
                    Canvas(Modifier.size(width = 14.dp, height = 8.dp)) {
                        val fill = when (kind) {
                            StatusKind.SUCCESS -> colors.paid.ink
                            StatusKind.INFO -> colors.review.ink.copy(alpha = .7f)
                            StatusKind.ERROR -> colors.overdue.ink.copy(alpha = .38f)
                            else -> colors.track
                        }
                        drawRoundRect(fill, cornerRadius = CornerRadius(size.height / 2, size.height / 2))
                    }
                }
                Text("$count $label", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
