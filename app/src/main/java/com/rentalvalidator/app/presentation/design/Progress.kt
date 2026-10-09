package com.rentalvalidator.app.presentation.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.theme.NaniSans
import com.rentalvalidator.app.presentation.theme.NaniSerifText
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import kotlin.math.cos
import kotlin.math.sin

/** A share of a ring, as a fraction of the whole, painted in [color]. */
data class RingSegment(val fraction: Float, val color: Color)

/** The ink a status takes inside rings and counters. */
@Composable
fun ringColor(kind: StatusKind): Color = statusTone(kind).ink

/**
 * The share of the month (or of a unit, or of the contracts) as a segmented ring. Arcs are
 * laid clockwise from twelve o'clock in the order given, separated by a small gap and drawn
 * with round ends; what is not covered remains the quiet track. It sweeps in once and then
 * eases to new values, never clipping an arc mid-way.
 */
@Composable
fun StatusRing(
    segments: List<RingSegment>,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    stroke: Dp = 10.dp,
    center: (@Composable BoxScope.() -> Unit)? = null
) {
    val track = NaniTheme.colors.track
    val inspection = LocalInspectionMode.current
    val reveal = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = AppMotion.Settle)) }
    val fractions = segments.mapIndexed { index, segment ->
        animateFloatAsState(segment.fraction.coerceIn(0f, 1f),
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow), label = "ring segment $index").value
    }
    Box(modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val radius = diameter / 2f
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx))
            // Round ends extend each arc by half the stroke; subtract them so neighbours keep a clean gap.
            val capDegrees = Math.toDegrees((strokePx / 2f / radius).toDouble()).toFloat()
            val gapDegrees = Math.toDegrees((3.dp.toPx() / radius).toDouble()).toFloat()
            val visible = segments.indices.filter { fractions[it] > .0005f }
            var start = -90f
            visible.forEach { index ->
                val sweep = 360f * fractions[index] * reveal.value
                val color = segments[index].color
                when {
                    visible.size == 1 && sweep >= 359.5f ->
                        drawArc(color, start, 360f, false, topLeft, arcSize, style = Stroke(strokePx))
                    sweep - 2 * capDegrees - gapDegrees > 0f ->
                        drawArc(color, start + capDegrees + gapDegrees / 2f, sweep - 2 * capDegrees - gapDegrees, false,
                            topLeft, arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
                    sweep > 0f -> {
                        // Too small for an arc: a dot keeps the share visible without overlapping its neighbours.
                        val angle = Math.toRadians((start + sweep / 2f).toDouble())
                        val centerPoint = Offset(this.size.width / 2f + radius * cos(angle).toFloat(),
                            this.size.height / 2f + radius * sin(angle).toFloat())
                        drawCircle(color, strokePx / 2f * (sweep / (2 * capDegrees + gapDegrees)).coerceIn(.6f, 1f), centerPoint)
                    }
                }
                start += sweep
            }
        }
        if (center != null) Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center, content = center)
    }
}

/**
 * The figure inside a ring. Sized from the ring, not from the font scale, so it always fits;
 * the same information is available as text or in the ring's description.
 */
@Composable
fun RingLabel(value: String, ringSize: Dp, caption: String? = null) {
    val density = LocalDensity.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = TextStyle(fontFamily = NaniSerifText, fontWeight = FontWeight.SemiBold,
            fontSize = with(density) { (ringSize * .22f).toSp() }, lineHeight = with(density) { (ringSize * .26f).toSp() }),
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
        if (caption != null && ringSize >= 72.dp) Text(caption, style = TextStyle(fontFamily = NaniSans, fontWeight = FontWeight.SemiBold,
            fontSize = with(density) { (ringSize * .12f).toSp() }), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
    }
}

/** One counter of a group: how many items share a state. [hollow] marks a state that holds nothing yet. */
data class StatCount(val count: Int, val label: String, val color: Color, val hollow: Boolean = false, val emphasize: Boolean = false)

@Composable
private fun StatDot(stat: StatCount, size: Dp = 10.dp) {
    val outline = MaterialTheme.colorScheme.outline
    Box(Modifier.size(size).clearAndSetSemantics { }.then(
        if (stat.hollow) Modifier.border(2.dp, outline, CircleShape) else Modifier.background(stat.color, CircleShape)))
}

/**
 * Explicit counts beside a ring: the numbers people act on (paid, due, late), each with the
 * same marker the ring uses. Zeros are kept quiet; a late count is raised in its own ink.
 */
@Composable
fun StatTiles(stats: List<StatCount>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerLow)) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 12.dp), color = colors.outlineVariant)
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { }.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatDot(stat)
                    Text(stat.count.toString(), style = NaniType.moneyLarge, maxLines = 1,
                        color = when {
                            stat.count == 0 -> colors.onSurfaceVariant
                            stat.emphasize -> stat.color
                            else -> colors.onSurface
                        })
                }
                Text(stat.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

/** A compact legend for rings where the counts are secondary. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RingLegend(stats: List<StatCount>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.filter { it.count > 0 }.forEach { stat ->
            Row(Modifier.semantics(mergeDescendants = true) { }, verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatDot(stat, 9.dp)
                Text("${stat.count} ${stat.label}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Fractions of a total, guarding against an empty total. */
fun shareOf(part: Double, total: Double): Float = if (total <= 0.0) 0f else (part / total).toFloat().coerceIn(0f, 1f)
