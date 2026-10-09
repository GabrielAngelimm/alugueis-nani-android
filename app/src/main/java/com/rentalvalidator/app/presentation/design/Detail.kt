package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniType

enum class DetailIdentity { TENANT, UNIT }

/**
 * Identity first, then the two numbers that define the record, always side by side on one
 * line with a divider between them. When either value is too wide for its half, both are
 * set smaller by the same amount, so they stay level, whole and on a single line.
 */
@Composable
fun NaniDetailHero(
    title: String,
    subtitle: String,
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    status: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    subtitleIcon: ImageVector? = null,
    identity: DetailIdentity = DetailIdentity.TENANT,
    unitIcon: ImageVector? = null
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 4.dp, bottom = 24.dp)) {
        val markSize = if (compact) 52.dp else 60.dp
        when {
            identity == DetailIdentity.UNIT && unitIcon != null -> UnitTile(unitIcon, size = markSize)
            identity == DetailIdentity.UNIT -> UnitPlaque(title, size = markSize)
            else -> Monogram(title, size = markSize)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.headlineLarge,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (subtitleIcon != null) {
                Icon(subtitleIcon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(6.dp))
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (status != null) {
            Spacer(Modifier.height(12.dp))
            status()
        }
        Spacer(Modifier.height(20.dp))
        LedgerSheet {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
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
                // Each value is still fitted to the width it actually receives, so nothing is ever clipped.
                // The divider has a fixed height because fitted text cannot report intrinsic sizes.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    HeroFact(firstLabel, firstValue, valueStyle, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    HeroFact(secondLabel, secondValue, valueStyle, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroFact(label: String, value: String, valueStyle: androidx.compose.ui.text.TextStyle, modifier: Modifier) {
    Column(modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
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
