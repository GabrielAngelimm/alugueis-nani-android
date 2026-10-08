package com.rentalvalidator.app.presentation.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.design.LedgerRule
import com.rentalvalidator.app.presentation.design.LedgerSheet
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
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

/**
 * Six months as ledger columns. A month is chosen by tapping or by dragging along the chart;
 * the drag is horizontal so the page keeps scrolling vertically.
 */
@Composable
internal fun ReceiptHistory(months: List<Int>, values: List<Float>) {
    val entries = months.zip(values)
    val colors = MaterialTheme.colorScheme
    if (entries.isEmpty()) {
        Text("Os recebimentos registrados aparecerão aqui.", Modifier.padding(horizontal = AppSpace.page),
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        return
    }
    var selected by remember(months) { mutableIntStateOf(entries.lastIndex) }
    val index = selected.coerceIn(entries.indices)
    val maximum = entries.maxOf { it.second }.coerceAtLeast(1f)
    val paid = NaniTheme.colors.paid
    LedgerSheet(Modifier.padding(horizontal = AppSpace.page)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                    Text(monthName(entries[index].first), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(CurrencyUtils.format(entries[index].second.toDouble()), style = NaniType.moneyLarge)
                }
                Text("${entries.size} meses", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().height(150.dp).testTag("receipt-history-chart").selectableGroup()
                    .semantics {
                        stateDescription = "${monthName(entries[index].first)}: ${CurrencyUtils.format(entries[index].second.toDouble())}"
                        customActions = listOf(
                            CustomAccessibilityAction("Mês anterior") { if (selected > 0) { selected--; true } else false },
                            CustomAccessibilityAction("Mês seguinte") { if (selected < entries.lastIndex) { selected++; true } else false }
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entries.forEachIndexed { i, (month, value) ->
                    val active = i == index
                    val barColor by animateColorAsState(if (active) paid.ink else paid.ink.copy(alpha = .32f),
                        tween(AppMotion.StateDuration), label = "receipt bar")
                    val share by animateFloatAsState((value / maximum).coerceIn(0f, 1f), tween(AppMotion.LayoutDuration), label = "receipt share")
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected = active, role = Role.Tab, onClick = { selected = i })
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${monthName(month)}: ${CurrencyUtils.format(value.toDouble())}"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.width(26.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceContainer))
                            if (value > 0f) Box(Modifier.width(26.dp).fillMaxHeight(share.coerceAtLeast(.03f))
                                .clip(RoundedCornerShape(8.dp)).background(barColor))
                        }
                        Text(monthName(month).take(3), Modifier.padding(top = 10.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold),
                            color = if (active) colors.onSurface else colors.onSurfaceVariant)
                    }
                }
            }
            if (entries.all { it.second == 0f }) {
                Text("Os recebimentos registrados aparecerão aqui.", Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
        }
        LedgerRule()
        Text("Total no período: ${CurrencyUtils.format(entries.sumOf { it.second.toDouble() })}",
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
