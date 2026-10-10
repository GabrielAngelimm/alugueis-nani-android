package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniSerifText
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DetailIdentity { TENANT, UNIT }

/**
 * The head of a detail page as one card. Its cover holds who or what the page is about: the
 * mark, the name and the line under it, set on deep blue with a guilloché engraved from the name.
 * Below the cover, the two numbers that define the record share one line with a divider; when
 * either is too wide for its half, both are set smaller by the same amount, so they stay level.
 */
@Composable
fun NaniDetailHero(
    title: String,
    subtitle: String,
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    subtitleIcon: ImageVector? = null,
    identity: DetailIdentity = DetailIdentity.TENANT,
    unitIcon: ImageVector? = null
) {
    val nani = NaniTheme.colors
    val shape = RoundedCornerShape(AppSize.sheetRadius)
    // By day the card lifts off the paper on a shadow tinted with the cover; at night the cover's glow is enough.
    val lift = if (nani.isDark) Modifier else Modifier.shadow(14.dp, shape, ambientColor = nani.coverEnd.copy(alpha = .16f),
        spotColor = nani.coverEnd.copy(alpha = .3f))
    Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 4.dp, bottom = 24.dp).then(lift),
        shape = shape, color = MaterialTheme.colorScheme.surface) {
        Column {
            HeroCover(seed = title) {
                if (identity == DetailIdentity.UNIT) UnitCoverMark(title, unitIcon) else PersonCoverMark(title)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeroTitle(title, nani.onPlaque)
                    Row {
                        if (subtitleIcon != null) {
                            Icon(subtitleIcon, null, Modifier.padding(top = 2.dp).size(16.dp), tint = nani.onPlaque.copy(alpha = .7f))
                            Spacer(Modifier.size(6.dp))
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = nani.onPlaque.copy(alpha = .84f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HeroFacts(firstLabel, firstValue, secondLabel, secondValue)
        }
    }
}

/** The name on a cover: the display serif, a size down from a page title so it can sit beside the mark. */
private val HeroName = TextStyle(fontFamily = com.rentalvalidator.app.presentation.theme.NaniSerif,
    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = (-0.4).sp)
private val HeroNameSizes = listOf(26, 23, 20).map { HeroName.copy(fontSize = it.sp, lineHeight = (it + 5).sp) }

/**
 * The name at the largest size that keeps it within two lines, so short names stay bold and long
 * ones break into whole words instead of leaving a lone "de" on a line. Only the longest names use a third.
 */
@Composable
private fun HeroTitle(title: String, color: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val width = constraints.maxWidth
        val style = remember(title, width, density) {
            HeroNameSizes.firstOrNull {
                measurer.measure(AnnotatedString(title), it, constraints = Constraints(maxWidth = width)).lineCount <= 2
            } ?: HeroNameSizes.last()
        }
        Text(title, Modifier.fillMaxWidth().semantics { heading() }, style = style, color = color,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** Deep blue lit from the upper right, engraved with fine rings that ripple out from the light. */
@Composable
private fun HeroCover(seed: String, content: @Composable RowScope.() -> Unit) {
    val nani = NaniTheme.colors
    val engraving = remember(seed) { engravingOf(seed) }
    // Rings run past the edges by design; the cover keeps them off the facts below.
    Row(Modifier.fillMaxWidth().heightIn(min = 132.dp).clipToBounds().drawWithCache {
        val width = size.width
        val height = size.height
        val center = Offset(width * EngravingCenterX, height * EngravingCenterY)
        // From a little before straight down to a little past straight left: the quarter that covers the cover.
        val angles = List(141) { (.47f + it * .004f) * PI.toFloat() }
        val rings = List(EngravingRings) { index ->
            Path().apply {
                angles.forEachIndexed { step, angle ->
                    val radius = engraving.ringRadius(index, angle, width, height)
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    if (step == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
        }
        val ground = Brush.linearGradient(listOf(nani.coverStart, nani.coverEnd), start = Offset(width, 0f), end = Offset(0f, height))
        val glow = Brush.radialGradient(listOf(Color.White.copy(alpha = .12f), Color.Transparent),
            center = Offset(width * .94f, -height * .15f), radius = height * 1.4f)
        // The rings fade out toward the left, so the mark and the start of the name sit on clear blue.
        val ink = Brush.horizontalGradient(0f to Color.Transparent, .25f to Color.Transparent,
            .65f to nani.onPlaque.copy(alpha = .14f), 1f to nani.onPlaque.copy(alpha = .14f))
        val line = Stroke(.75.dp.toPx())
        onDrawBehind {
            drawRect(ground)
            rings.forEach { drawPath(it, ink, style = line) }
            drawRect(glow)
        }
    }.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content)
}

/** A person on the cover: their own monogram, set in a halo of the cover's light. */
@Composable
private fun PersonCoverMark(name: String) {
    Box(Modifier.size(64.dp).clearAndSetSemantics { }.background(NaniTheme.colors.onPlaque.copy(alpha = .16f), CircleShape)
        .padding(3.dp)) {
        Monogram(name, size = 58.dp)
    }
}

/** A unit on the cover: what it is, drawn in light on a pane of frosted glass. */
@Composable
private fun UnitCoverMark(name: String, icon: ImageVector?) {
    val light = NaniTheme.colors.onPlaque
    val shape = RoundedCornerShape(19.dp)
    Box(Modifier.size(64.dp).clearAndSetSemantics { }.clip(shape)
        .background(Brush.verticalGradient(listOf(light.copy(alpha = .24f), light.copy(alpha = .08f))))
        .border(1.dp, light.copy(alpha = .3f), shape), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, Modifier.size(30.dp), tint = light)
        else Text(initialsOf(name), style = TextStyle(fontFamily = NaniSerifText,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 22.sp), color = light)
    }
}

/** The record's two numbers, side by side under the cover at one shared scale. */
@Composable
private fun HeroFacts(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val gutter = 16.dp
        // Two equal halves around a 1dp divider with a gutter on each side.
        val halfWidth = with(density) { ((maxWidth - gutter * 2 - 1.dp) / 2).toPx() }
        val widest = listOf(firstValue, secondValue).maxOf {
            measurer.measure(AnnotatedString(it), NaniType.moneyLarge, softWrap = false).size.width
        }
        // One scale for both values keeps them level; the floor only matters on very narrow windows.
        val scale = if (widest <= halfWidth) 1f else (halfWidth / widest).coerceAtLeast(.45f)
        val valueStyle = NaniType.moneyLarge.scaled(scale)
        // Labels stay on one line too, at their own shared scale, so a long label never pushes its value down.
        val labelStyle = MaterialTheme.typography.bodySmall
        val widestLabel = listOf(firstLabel, secondLabel).maxOf {
            measurer.measure(AnnotatedString(it), labelStyle, softWrap = false).size.width
        }
        val fittedLabel = labelStyle.scaled(if (widestLabel <= halfWidth) 1f else (halfWidth / widestLabel).coerceAtLeast(.7f))
        // Each value is still fitted to the width it actually receives, so nothing is ever clipped.
        // The divider has a fixed height because fitted text cannot report intrinsic sizes.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gutter)) {
            HeroFact(firstLabel, firstValue, fittedLabel, valueStyle, Modifier.weight(1f))
            VerticalDivider(Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)
            HeroFact(secondLabel, secondValue, fittedLabel, valueStyle, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroFact(label: String, value: String, labelStyle: TextStyle, valueStyle: TextStyle, modifier: Modifier) {
    Column(modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FittingText(label, labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, minScale = .6f)
        FittingText(value, valueStyle, minScale = .4f)
    }
}

/** A titled sheet of related facts or actions on a detail page. */
@Composable
fun NaniDetailCard(
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            SectionTitle(title)
            Spacer(Modifier.height(8.dp))
        }
        LedgerSheet(Modifier.padding(horizontal = AppSpace.page), contentPadding = contentPadding, content = content)
    }
}

/** A navigational row inside a detail card, optionally carrying a status mark. */
@Composable
fun NaniDetailAction(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    divider: Boolean = true,
    status: String? = null
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 76.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium)
                if (status != null) StatusMark(status, StatusKind.INFO)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (divider) LedgerRule(Modifier.padding(start = 70.dp))
}
