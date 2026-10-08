package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace

/*
 * Lists are ledger sheets, not stacks of cards: one surface per group, entries
 * separated by ruled lines. A lazy list renders each entry as its own slice of the
 * sheet, so the paper reads continuous while rows stay virtualized.
 */

enum class LedgerPosition { Single, First, Middle, Last }

fun ledgerPosition(index: Int, count: Int): LedgerPosition = when {
    count <= 1 -> LedgerPosition.Single
    index == 0 -> LedgerPosition.First
    index == count - 1 -> LedgerPosition.Last
    else -> LedgerPosition.Middle
}

fun ledgerShape(position: LedgerPosition, radius: Dp = AppSize.sheetRadius): Shape = when (position) {
    LedgerPosition.Single -> RoundedCornerShape(radius)
    LedgerPosition.First -> RoundedCornerShape(topStart = radius, topEnd = radius)
    LedgerPosition.Middle -> RoundedCornerShape(0.dp)
    LedgerPosition.Last -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
}

/** One slice of a ledger sheet inside a lazy list, with the rule above it when it is not the first. */
@Composable
fun LedgerSlice(
    position: LedgerPosition,
    modifier: Modifier = Modifier,
    ruleIndent: Dp = AppSpace.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = AppSpace.page)
            .clip(ledgerShape(position)).background(MaterialTheme.colorScheme.surface)
    ) {
        if (position == LedgerPosition.Middle || position == LedgerPosition.Last) LedgerRule(Modifier.padding(start = ruleIndent))
        content()
    }
}

/** A self-contained sheet for grouped content that is not virtualized. */
@Composable
fun LedgerSheet(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(AppSize.sheetRadius), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** The ruled line of the ledger. */
@Composable
fun LedgerRule(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** A heading that introduces a group of entries. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().padding(horizontal = AppSpace.page).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

/** A labelled value: the label whispers, the value is written in the account-book voice. */
@Composable
fun NaniFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = com.rentalvalidator.app.presentation.theme.NaniType.moneyRow
) {
    Column(modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = valueStyle, maxLines = 1, softWrap = false)
    }
}

/** The glyph tile used in front of navigational and action rows. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer) {
    Box(modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(21.dp), tint = tint)
    }
}

/** A row that takes the person somewhere or opens a step. */
@Composable
fun NaniActionRow(
    title: String,
    description: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 68.dp).padding(horizontal = AppSpace.large, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
        else {
            Spacer(Modifier.width(2.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
