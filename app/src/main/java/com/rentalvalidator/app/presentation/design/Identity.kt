package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.NaniSans
import com.rentalvalidator.app.presentation.theme.NaniSerifText
import com.rentalvalidator.app.presentation.theme.NaniTheme
import kotlin.math.abs

internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 })
        .joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }
}

/**
 * A person's monogram. The tint is derived from the name, so the same tenant keeps the
 * same color on every screen and people become recognizable before their names are read.
 * Letters are sized in dp: the circle is a fixed glyph, not running text.
 */
@Composable
fun Monogram(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val tones = NaniTheme.colors.monograms
    val (fill, ink) = tones[abs(name.trim().lowercase().hashCode()) % tones.size]
    val fontSize = with(LocalDensity.current) { (size * .38f).toSp() }
    Box(modifier.size(size).clearAndSetSemantics { }.background(fill, CircleShape), contentAlignment = Alignment.Center) {
        Text(initialsOf(name), style = TextStyle(fontFamily = NaniSans, fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize, letterSpacing = with(LocalDensity.current) { (size * .01f).toSp() }), color = ink)
    }
}

/**
 * A unit's plaque, after the enamel house-number signs on Brazilian and Portuguese
 * façades: deep blue, a fine inner rule and the unit's letters.
 */
@Composable
fun UnitPlaque(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val colors = NaniTheme.colors
    val fontSize = with(LocalDensity.current) { (size * .4f).toSp() }
    val shape = RoundedCornerShape(size * .24f)
    Box(modifier.size(size).clearAndSetSemantics { }.background(colors.plaque, shape), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSizeInset(size).border(1.dp, colors.onPlaque.copy(alpha = .42f), RoundedCornerShape(size * .17f)))
        Text(initialsOf(name), style = TextStyle(fontFamily = NaniSerifText, fontWeight = FontWeight.SemiBold,
            fontSize = fontSize), color = colors.onPlaque)
    }
}

private fun Modifier.matchParentSizeInset(size: Dp) = this.size(size).padding(size * .08f)

/**
 * The people of a unit at a glance: their monograms overlap like portraits on a fridge door,
 * followed by a dashed seat for each free place. Long lists end in a "+N" token instead of
 * running off the card. Purely visual; the sentence beside it carries the same facts for TalkBack.
 */
@Composable
fun ResidentsPile(
    names: List<String>,
    vacancies: Int,
    modifier: Modifier = Modifier,
    occupied: Int = names.size,
    size: Dp = 34.dp,
    ring: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    maxFaces: Int = 4,
    maxSeats: Int = 3
) {
    val colors = MaterialTheme.colorScheme
    val people = maxOf(occupied, names.size)
    val faces = if (people > maxFaces) names.take(maxFaces - 1) else names.take(maxFaces)
    val hiddenFaces = people - faces.size
    val seats = vacancies.coerceAtLeast(0)
    val shownSeats = if (seats > maxSeats) maxSeats - 1 else seats
    val hiddenSeats = seats - shownSeats
    val fontSize = with(LocalDensity.current) { (size * .3f).toSp() }
    val token = @Composable { label: String ->
        Box(Modifier.size(size).background(ring, CircleShape).padding(2.dp).background(colors.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center) {
            Text(label, style = TextStyle(fontFamily = NaniSans, fontWeight = FontWeight.ExtraBold, fontSize = fontSize),
                color = colors.onSurfaceVariant)
        }
    }
    Row(modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(-(size * .18f))) {
        faces.forEach { name ->
            // A ring in the panel's color separates overlapping portraits.
            Box(Modifier.size(size).background(ring, CircleShape).padding(2.dp)) { Monogram(name, size = size - 4.dp) }
        }
        if (hiddenFaces > 0) token("+$hiddenFaces")
        if (seats > 0) Spacer(Modifier.size(width = size * .18f + 6.dp, height = 1.dp))
        repeat(shownSeats) {
            Box(Modifier.size(size).background(ring, CircleShape).padding(2.dp).drawBehind {
                val stroke = 1.5.dp.toPx()
                drawCircle(colors.outline, radius = this.size.minDimension / 2f - stroke / 2f,
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))))
            })
        }
        if (hiddenSeats > 0) token("+$hiddenSeats")
    }
}

/** Who an action sheet is about, repeated at the top so the person never acts on the wrong tenant. */
@Composable
fun NaniSheetTenantCard(name: String, detail: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Monogram(name, size = 42.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
