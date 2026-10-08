package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit

/**
 * A single line that must never be cut, such as a large amount. It keeps its style while it
 * fits and otherwise steps the size down, never below [minScale] of the original.
 */
@Composable
fun FittingText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified, minScale: Float = .55f) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val available = with(density) { maxWidth.toPx() }
        // Font scale is part of the key: the same sp style measures differently when type is enlarged.
        val fitted = remember(text, style, available, density.fontScale) {
            var scale = 1f
            while (scale > minScale &&
                measurer.measure(AnnotatedString(text), style.scaled(scale), softWrap = false).size.width > available) {
                scale -= .05f
            }
            style.scaled(scale)
        }
        Text(text, style = fitted, color = color, maxLines = 1, softWrap = false)
    }
}

/**
 * Scale at which every word of each label fits [maxWidth] on its own line, so short labels
 * wrap between words but never break inside one. 1 when everything already fits.
 */
@Composable
fun rememberWordFitScale(labels: List<String>, style: TextStyle, maxWidth: androidx.compose.ui.unit.Dp, minScale: Float = .7f): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val available = with(density) { maxWidth.toPx() }
    return remember(labels, style, available, density.fontScale) {
        val widest = labels.flatMap { it.split(' ') }.filter { it.isNotBlank() }
            .maxOfOrNull { measurer.measure(AnnotatedString(it), style, softWrap = false).size.width } ?: 0
        if (widest <= available || widest == 0) 1f else (available / widest).coerceAtLeast(minScale)
    }
}

internal fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale >= 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight.scaledOrSame(scale))

private fun TextUnit.scaledOrSame(scale: Float): TextUnit = if (this == TextUnit.Unspecified) this else this * scale
