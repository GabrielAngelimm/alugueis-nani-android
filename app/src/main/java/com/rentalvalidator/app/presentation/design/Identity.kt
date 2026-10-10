package com.rentalvalidator.app.presentation.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.R
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
 * [tone] picks the color instead, for a name still being typed. Letters are sized in dp: the
 * circle is a fixed glyph, not running text.
 */
@Composable
fun Monogram(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, tone: String = name) {
    val tones = NaniTheme.colors.monograms
    val (fill, ink) = tones[abs(tone.trim().lowercase().hashCode()) % tones.size]
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

/**
 * How a bank is told apart at a glance: its official mark on a round [coin], the way its app icon
 * looks, and on a white [seal] for a card filled with the bank's color. [fill] is that color and
 * [ink] what reads on it.
 */
@Immutable
internal data class BankMark(@DrawableRes val coin: Int, @DrawableRes val seal: Int, val fill: Color, val ink: Color)

/**
 * The banks a rent usually arrives through. The marks are the banks' own logos, only there to
 * name the bank; the colors are the ones on their cards and apps, the same by day and by night.
 */
private val BankMarks = mapOf(
    "Nubank" to BankMark(R.drawable.bank_nubank, R.drawable.bank_nubank_seal, Color(0xFF820AD1), Color.White),
    "Itaú" to BankMark(R.drawable.bank_itau, R.drawable.bank_itau_seal, Color(0xFFEC7000), Color.White),
    "Santander" to BankMark(R.drawable.bank_santander, R.drawable.bank_santander_seal, Color(0xFFEC0000), Color.White),
    "Mercado Pago" to BankMark(R.drawable.bank_mercado_pago, R.drawable.bank_mercado_pago, Color(0xFF009EE3), Color.White),
    "PagSeguro" to BankMark(R.drawable.bank_pagseguro, R.drawable.bank_pagseguro, Color(0xFFFFE72D), Color(0xFF151515)),
    "Bradesco" to BankMark(R.drawable.bank_bradesco, R.drawable.bank_bradesco_seal, Color(0xFFCC092F), Color.White),
    "Banco do Brasil" to BankMark(R.drawable.bank_banco_do_brasil, R.drawable.bank_banco_do_brasil_seal,
        Color(0xFFFFEF38), Color(0xFF003DA4)),
    "Inter" to BankMark(R.drawable.bank_inter, R.drawable.bank_inter_seal, Color(0xFFFF7A00), Color.White),
    "Caixa" to BankMark(R.drawable.bank_caixa, R.drawable.bank_caixa, Color(0xFF005CA9), Color.White),
    "Sicredi" to BankMark(R.drawable.bank_sicredi, R.drawable.bank_sicredi, Color(0xFF3FA110), Color.White)
)

/** The bank's mark, or null for a bank without one (and for "no bank"). */
internal fun bankMarkOf(bank: String): BankMark? = BankMarks[bank.trim()]

/**
 * A bank as a round coin with its official mark, the way a payment app lists banks. [turned] runs
 * from the bank's coin to its white seal, for a card that takes on the bank's color. A bank
 * without a mark gets its initials on a quiet coin; [none] is the empty coin of "no bank", drawn
 * in [ink].
 */
@Composable
fun BankCoin(
    bank: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    none: Boolean = false,
    turned: Float = 0f,
    ink: Color = Color.Unspecified
) {
    val colors = MaterialTheme.colorScheme
    val mark = bankMarkOf(bank)
    val coin = Modifier.size(size).clearAndSetSemantics { }
    when {
        none -> {
            val line = ink.takeOrElse { colors.outline }
            Box(modifier.then(coin).border(1.5.dp, line.copy(alpha = .6f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(size * .34f, 2.dp).background(line, CircleShape))
            }
        }
        mark != null -> Box(modifier.then(coin).clip(CircleShape)) {
            // A bank whose mark is already drawn on white keeps one face.
            val seal = if (mark.seal == mark.coin) 0f else turned.coerceIn(0f, 1f)
            if (seal < 1f) Image(painterResource(mark.coin), null, Modifier.matchParentSize())
            if (seal > 0f) Image(painterResource(mark.seal), null, Modifier.matchParentSize(), alpha = seal)
        }
        else -> Box(modifier.then(coin).background(colors.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
            val letters = initialsOf(bank)
            Text(letters, style = TextStyle(fontFamily = NaniSans, fontWeight = FontWeight.ExtraBold,
                fontSize = with(LocalDensity.current) { (size * if (letters.length > 1) .36f else .44f).toSp() }),
                color = ink.takeOrElse { colors.onSurfaceVariant }, maxLines = 1)
        }
    }
}

private fun Modifier.matchParentSizeInset(size: Dp) = this.size(size).padding(size * .08f)

/**
 * A unit shown by what it is (house, apartment, building...) rather than by letters. The tile is
 * the same tinted glass as the navigation lens, light from above and a fine rim, and round like
 * the monograms of the people who live there.
 */
@Composable
fun UnitTile(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val colors = MaterialTheme.colorScheme
    val dark = NaniTheme.colors.isDark
    val shape = CircleShape
    Box(modifier.size(size).clearAndSetSemantics { }.clip(shape)
        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
            androidx.compose.ui.graphics.lerp(colors.primaryContainer, androidx.compose.ui.graphics.Color.White, if (dark) .06f else .2f),
            colors.primaryContainer)))
        .border(1.dp, colors.primary.copy(alpha = if (dark) .26f else .2f), shape),
        contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(icon, null, Modifier.size(size * .48f), tint = colors.onPrimaryContainer)
    }
}

/**
 * The people of a unit at a glance: their monograms overlap like portraits on a fridge door.
 * Only people who live there are shown, at most [maxFaces] and no more than fit the width the
 * pile is given; the rest are counted in a quiet "+N" pill. [face] lets a caller attach each
 * portrait to a transition, keyed by its position.
 */
@Composable
fun ResidentsPile(
    names: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    ring: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    maxFaces: Int = 4,
    face: @Composable (index: Int) -> Modifier = { Modifier }
) {
    val colors = MaterialTheme.colorScheme
    val overlap = size * .18f
    val pillGap = 8.dp
    val pillPadding = 9.dp
    val countStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        // A narrow row drops portraits from the end, and the pill counts them instead.
        fun widthOf(shown: Int): Dp {
            val hidden = names.size - shown
            val pill = if (hidden == 0) 0.dp else pillGap + pillPadding * 2 +
                with(density) { measurer.measure("+$hidden", countStyle, maxLines = 1).size.width.toDp() }
            return size * shown - overlap * (shown - 1) + pill
        }
        val most = minOf(names.size, maxFaces)
        val shown = if (!constraints.hasBoundedWidth) most
            else (most downTo 1).firstOrNull { widthOf(it) <= maxWidth } ?: minOf(most, 1)
        val faces = names.take(shown)
        val hidden = names.size - faces.size
        val spoken = faces.joinToString(", ") + if (hidden > 0) " e mais $hidden" else ""
        Row(Modifier.clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(-overlap)) {
            faces.forEachIndexed { index, name ->
                // The ring sits on the portrait's own edge, so a portrait keeps one size whether it is
                // stacked here or standing alone in a list, and can travel between the two.
                Monogram(name, face(index).border(2.dp, ring, CircleShape), size = size)
            }
            if (hidden > 0) {
                Text("+$hidden", Modifier.padding(start = overlap + pillGap).clip(CircleShape)
                    .background(colors.surfaceContainerHigh).padding(horizontal = pillPadding, vertical = 3.dp),
                    style = countStyle, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
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
