package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
 * Identity first, then the two numbers that define the record. The facts sit side by
 * side only when both actually fit at the person's font scale; otherwise they stack,
 * so an amount is never cut or wrapped.
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
    identity: DetailIdentity = DetailIdentity.TENANT
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 4.dp, bottom = 24.dp)) {
        if (identity == DetailIdentity.UNIT) UnitPlaque(title, size = if (compact) 52.dp else 60.dp)
        else Monogram(title, size = if (compact) 52.dp else 60.dp)
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
                val valueStyle = NaniType.moneyLarge
                val labelStyle = MaterialTheme.typography.bodySmall
                val halfWidth = with(density) { ((maxWidth - 33.dp) / 2).toPx() }
                // Measure the real strings at the current font scale rather than guessing by breakpoint.
                val fitsSideBySide = listOf(firstValue, secondValue).all {
                    measurer.measure(AnnotatedString(it), valueStyle, softWrap = false).size.width <= halfWidth
                } && listOf(firstLabel, secondLabel).all {
                    measurer.measure(AnnotatedString(it), labelStyle, softWrap = false).size.width <= halfWidth
                }
                if (fitsSideBySide) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NaniFact(firstLabel, firstValue, Modifier.weight(1f), valueStyle)
                        VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        NaniFact(secondLabel, secondValue, Modifier.weight(1f), valueStyle)
                    }
                } else {
                    Column {
                        NaniFact(firstLabel, firstValue, Modifier.fillMaxWidth(), valueStyle)
                        LedgerRule()
                        NaniFact(secondLabel, secondValue, Modifier.fillMaxWidth(), valueStyle)
                    }
                }
            }
        }
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
