package com.rentalvalidator.app.presentation.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.util.CurrencyUtils

internal data class ReceiptHistorySnapshot(val months: List<Int>, val values: List<Float>)

internal fun receiptHistorySnapshot(
    reference: java.time.YearMonth,
    tenants: List<com.rentalvalidator.app.domain.model.Tenant>,
    payments: List<com.rentalvalidator.app.domain.model.Payment>
): ReceiptHistorySnapshot {
    val amounts = tenants.associate { it.id to it.amount }
    val periods = (5L downTo 0L).map(reference::minusMonths)
    return ReceiptHistorySnapshot(periods.map { it.monthValue }, periods.map { period ->
        payments.filter { it.year == period.year && it.month == period.monthValue && it.status == com.rentalvalidator.app.domain.model.PaymentStatus.PAGO }
            .sumOf { amounts[it.tenantId] ?: 0.0 }.toFloat()
    })
}

/** A finite, selectable chart. Dragging is horizontal so page scrolling remains vertical. */
@Composable
internal fun ReceiptHistory(months: List<Int>, values: List<Float>) {
    val entries = months.zip(values)
    if (entries.isEmpty()) {
        Text("Os recebimentos registrados aparecerão aqui.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by remember(months) { mutableIntStateOf(entries.lastIndex) }
    val index = selected.coerceIn(entries.indices)
    val maximum = entries.maxOf { it.second }.coerceAtLeast(1f)
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                    Text(monthName(entries[index].first), style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant)
                    Text(CurrencyUtils.format(entries[index].second.toDouble()),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Text("${entries.size} meses", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(156.dp).testTag("receipt-history-chart").selectableGroup()
                    .semantics {
                        stateDescription = "${monthName(entries[index].first)}: ${CurrencyUtils.format(entries[index].second.toDouble())}"
                        customActions = listOf(
                            CustomAccessibilityAction("Mês anterior") {
                                if (selected > 0) { selected--; true } else false
                            },
                            CustomAccessibilityAction("Mês seguinte") {
                                if (selected < entries.lastIndex) { selected++; true } else false
                            }
                        )
                    }
                    .pointerInput(entries.size) {
                        fun selectAt(x: Float) {
                            selected = (x / (size.width.toFloat() / entries.size)).toInt().coerceIn(entries.indices)
                        }
                        detectHorizontalDragGestures(
                            onDragStart = { selectAt(it.x) },
                            onHorizontalDrag = { change, _ -> selectAt(change.position.x); change.consume() }
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEachIndexed { i, (month, value) ->
                    val selectedColor by animateColorAsState(
                        if (i == index) colors.primary else colors.primaryContainer, label = "Receipt selection"
                    )
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected = i == index, role = Role.Tab, onClick = { selected = i })
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${monthName(month)}: ${CurrencyUtils.format(value.toDouble())}"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.width(28.dp).fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant.copy(alpha = .45f)))
                            if (value > 0f) {
                                Box(Modifier.width(28.dp)
                                    .fillMaxHeight((value / maximum).coerceIn(.025f, 1f))
                                    .clip(RoundedCornerShape(14.dp)).background(selectedColor))
                            }
                        }
                        Text(monthName(month).take(3), Modifier.padding(top = 12.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == index) colors.primary else colors.onSurfaceVariant,
                            fontWeight = if (i == index) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            if (entries.all { it.second == 0f }) {
                Text("Os recebimentos registrados aparecerão aqui.", Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = colors.outlineVariant.copy(alpha = .65f))
            Text("Total no período: ${CurrencyUtils.format(entries.sumOf { it.second.toDouble() })}",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
