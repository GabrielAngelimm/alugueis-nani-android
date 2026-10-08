package com.rentalvalidator.app.presentation.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.StatusTone
import kotlinx.coroutines.launch

enum class StatusKind { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

@Composable
fun statusTone(kind: StatusKind): StatusTone = with(NaniTheme.colors) {
    when (kind) {
        StatusKind.SUCCESS -> paid
        StatusKind.WARNING -> pending
        StatusKind.ERROR -> overdue
        StatusKind.INFO -> review
        StatusKind.NEUTRAL -> neutral
    }
}

/**
 * Status is carried by shape as much as by color, so it survives color blindness and
 * grayscale: a double-ruled stamp for paid, a highlighter swipe for open items, a dashed
 * outline for what is under review, a solid rule for problems.
 * When a mark becomes paid while on screen it lands like a rubber stamp.
 */
@Composable
fun StatusMark(label: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val tone = statusTone(kind)
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(kind) }
    LaunchedEffect(kind) {
        if (kind == StatusKind.SUCCESS && previous != StatusKind.SUCCESS) {
            scale.snapTo(1.55f); alpha.snapTo(0f)
            launch { alpha.animateTo(1f, tween(110)) }
            scale.animateTo(1f, spring(dampingRatio = .42f, stiffness = 520f))
        }
        previous = kind
    }
    val markModifier = when (kind) {
        StatusKind.SUCCESS -> Modifier.drawBehind {
            val outer = 1.5.dp.toPx()
            val inner = .8.dp.toPx()
            val gap = 3.dp.toPx()
            drawRoundRect(tone.fill.copy(alpha = .55f), cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(tone.ink, Offset(outer / 2, outer / 2), Size(size.width - outer, size.height - outer),
                CornerRadius(6.dp.toPx()), style = Stroke(outer))
            drawRoundRect(tone.ink.copy(alpha = .7f), Offset(gap, gap), Size(size.width - gap * 2, size.height - gap * 2),
                CornerRadius(3.5.dp.toPx()), style = Stroke(inner))
        }
        StatusKind.WARNING -> Modifier.drawBehind {
            // A highlighter swipe: slightly taller than the text, with soft ends.
            drawRoundRect(tone.fill, cornerRadius = CornerRadius(4.dp.toPx(), 10.dp.toPx()))
        }
        StatusKind.INFO -> Modifier.drawBehind {
            val stroke = 1.2.dp.toPx()
            drawRoundRect(tone.fill.copy(alpha = .5f), cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(tone.ink, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke),
                CornerRadius(6.dp.toPx()), style = Stroke(stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 2.5.dp.toPx()))))
        }
        StatusKind.ERROR -> Modifier.drawBehind {
            val stroke = 1.2.dp.toPx()
            drawRoundRect(tone.fill, cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(tone.ink, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke),
                CornerRadius(6.dp.toPx()), style = Stroke(stroke))
        }
        StatusKind.NEUTRAL -> Modifier.drawBehind {
            drawRoundRect(tone.fill, cornerRadius = CornerRadius(6.dp.toPx()))
        }
    }
    val textColor = when (kind) {
        StatusKind.WARNING, StatusKind.ERROR -> tone.onFill
        else -> tone.ink
    }
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
                rotationZ = if (kind == StatusKind.SUCCESS) -2.5f else 0f
            }
            .then(markModifier)
            .heightIn(min = 26.dp)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (kind == StatusKind.SUCCESS) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
