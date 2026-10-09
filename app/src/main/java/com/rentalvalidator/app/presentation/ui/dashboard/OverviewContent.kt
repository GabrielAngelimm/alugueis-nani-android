package com.rentalvalidator.app.presentation.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.LocalNavigationClearance
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** One tenant's line on the month page. */
data class OverviewEntry(
    val tenantId: String, val name: String, val unit: String,
    val amount: Double, val dueDay: Int, val status: PaymentStatus?
)

data class OverviewSnapshot(
    val expected: Double, val received: Double, val pending: Double, val progress: Float,
    val tenantCount: Int, val unitCount: Int, val contractAlerts: Int,
    val months: List<Int>, val values: List<Float>,
    val period: YearMonth = YearMonth.now(), val today: LocalDate = LocalDate.now(),
    val entries: List<OverviewEntry> = emptyList()
)

/** Presentation only: all financial totals and navigation come from the existing route. */
@Composable
fun OverviewContent(
    data: OverviewSnapshot,
    onSettings: () -> Unit,
    onPayments: () -> Unit,
    onStatement: () -> Unit,
    onTenants: () -> Unit,
    onContracts: () -> Unit,
    onTenantPayments: (String) -> Unit = {}
) {
    val currentHour = rememberDashboardHour()
    val states = remember(data.entries, data.period, data.today) {
        data.entries.map { it to dueState(it.status, data.period, it.dueDay, data.today) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp + LocalNavigationClearance.current)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = AppSpace.page, end = 8.dp, top = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(dashboardGreetingResource(currentHour)), Modifier.semantics { heading() },
                        style = MaterialTheme.typography.displaySmall)
                    Text(data.today.weekdayLabel(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Configurações") }
            }
        }
        item { MonthLedger(data, states.map { it.second }, onPayments) }
        item {
            SectionTitle("Histórico de recebimentos", Modifier.padding(top = AppSpace.section),
                detail = "Estimado pelo aluguel atual de cada inquilino")
            Spacer(Modifier.height(10.dp))
            ReceiptHistory(data.months, data.values)
        }
        val open = states.filter { it.second != DueState.PAID }
            .sortedWith(compareBy({ it.second.ordinalForAttention() }, { dueDateIn(data.period, it.first.dueDay) }))
        if (data.tenantCount > 0) item {
            SectionTitle("Quem falta pagar", Modifier.padding(top = AppSpace.section),
                detail = if (open.isEmpty()) null else "${open.size} de ${data.tenantCount} em ${data.period.monthInSentence()}")
            Spacer(Modifier.height(10.dp))
            OpenRents(open.take(5), open.size, data.period, onTenantPayments, onPayments)
        }
        item {
            SectionTitle("Atalhos", Modifier.padding(top = AppSpace.section))
            Spacer(Modifier.height(10.dp))
            LedgerSheet(Modifier.padding(horizontal = AppSpace.page)) {
                NaniActionRow("Conferir um extrato", "Encontre os pagamentos em um PDF ou CSV do banco",
                    Icons.AutoMirrored.Rounded.FactCheck, onStatement)
                LedgerRule(Modifier.padding(start = 70.dp))
                NaniActionRow("Gerenciar locações",
                    "${data.unitCount} ${if (data.unitCount == 1) "unidade" else "unidades"} e ${data.tenantCount} ${if (data.tenantCount == 1) "inquilino" else "inquilinos"}",
                    Icons.Rounded.Cottage, onTenants)
                if (data.contractAlerts > 0) {
                    LedgerRule(Modifier.padding(start = 70.dp))
                    NaniActionRow("Revisar contratos",
                        "${data.contractAlerts} ${if (data.contractAlerts == 1) "contrato vencido ou perto do vencimento" else "contratos vencidos ou perto do vencimento"}",
                        Icons.Rounded.EventBusy, onContracts)
                }
            }
        }
    }
}

private fun DueState.ordinalForAttention() = when (this) {
    DueState.OVERDUE -> 0
    DueState.DUE_TODAY -> 1
    DueState.UPCOMING -> 2
    DueState.REVIEW -> 3
    DueState.PAID -> 4
}

/** The month as an account-book page: what came in, against what was expected, tenant by tenant. */
@Composable
private fun MonthLedger(data: OverviewSnapshot, states: List<DueState>, onPayments: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val month = data.period.monthInSentence()
    // One breakdown feeds the ring, its percentage and the counters, so they always agree.
    val counts = dueCounts(states)
    val stats = dueStats(counts)
    val percent = percentOf(counts.paid, counts.total)
    LedgerSheet(Modifier.padding(horizontal = AppSpace.page)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recebido em $month", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FittingText(CurrencyUtils.format(data.received), NaniType.moneyHero)
                    Text("de ${CurrencyUtils.format(data.expected)} previstos", style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.width(14.dp))
                StatusRing(
                    segments = ringOf(stats),
                    description = if (counts.total > 0) "${counts.paid} de ${counts.total} aluguéis pagos em $month, " +
                        "${counts.upcoming} a vencer e ${counts.overdue} em atraso" else "Nenhum aluguel previsto",
                    size = 92.dp, stroke = 10.dp
                ) { RingLabel(if (counts.total > 0) "$percent%" else "—", 92.dp, caption = "pagos") }
            }
            Spacer(Modifier.height(18.dp))
            if (data.tenantCount == 0) {
                Text("Cadastre inquilinos em Locações para acompanhar o mês.", style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            } else {
                StatTiles(stats)
            }
        }
        LedgerRule()
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("A receber", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(CurrencyUtils.format(data.pending), style = NaniType.moneyRow)
        }
        LedgerRule()
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onPayments).heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Acompanhar recebimentos", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = colors.primary)
        }
    }
}

@Composable
private fun OpenRents(
    entries: List<Pair<OverviewEntry, DueState>>,
    total: Int,
    period: YearMonth,
    onTenant: (String) -> Unit,
    onAll: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    LedgerSheet(Modifier.padding(horizontal = AppSpace.page)) {
        if (entries.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatusMark("Pago", StatusKind.SUCCESS)
                Text("Todos os aluguéis de ${period.monthInSentence()} foram recebidos.", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium)
            }
            return@LedgerSheet
        }
        entries.forEachIndexed { index, (entry, state) ->
            if (index > 0) LedgerRule(Modifier.padding(start = 66.dp))
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onTenant(entry.tenantId) }
                .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Monogram(entry.name, size = 36.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (state == DueState.REVIEW) "Aguardando conferência" else dueSentence(state, entry.dueDay, period),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state == DueState.OVERDUE) NaniTheme.colors.overdue.ink else colors.onSurfaceVariant)
                }
                Text(CurrencyUtils.format(entry.amount), style = NaniType.moneyRow, maxLines = 1, softWrap = false)
            }
        }
        if (total > entries.size) {
            LedgerRule()
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAll).heightIn(min = 52.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ver todos os $total", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = colors.primary)
            }
        }
    }
}

@Composable
private fun rememberDashboardHour(): Int {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hour = LocalTime.now().hour
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return hour
}

internal fun dashboardGreetingResource(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

internal fun monthName(month: Int): String = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)[month - 1]
