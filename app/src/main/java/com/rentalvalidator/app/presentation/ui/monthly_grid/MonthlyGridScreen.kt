package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.navigation.FinanceNavigation
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyGridScreen(viewModel: MonthlyGridViewModel = hiltViewModel(), onFinanceNavigate: (com.rentalvalidator.app.Screen) -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showSnackbar = rememberAppSnackbar()
    LaunchedEffect(state.error) {
        state.error?.let { showSnackbar(it); viewModel.clearError() }
    }
    var selected by remember { mutableStateOf<TenantPaymentItem?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val period = YearMonth.of(state.year, state.month)
    val scoped = state.items
    val visible = scoped.filter { (it.tenant.name.contains(query, true) || it.tenant.unit.contains(query, true)) &&
        (filter == 0 || if (filter == 1) it.payment?.status != PaymentStatus.PAGO else it.payment?.status == PaymentStatus.PAGO) }
    if (showSearch) NaniSearchDialog(query, { query = it }, { showSearch = false })
    Column(Modifier.fillMaxSize()) {
        FinanceNavigation("grid", onFinanceNavigate) {
            NaniSearchButton(query) { showSearch = true }
            IconButton(onClick = { showFilters = true }) {
                BadgedBox(badge = { if (filter != 0) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                    Icon(Icons.Rounded.Tune, "Filtrar recebimentos")
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalNavigationClearance.current)) {
            stickyHeader {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    MonthSwitcher("${getMonthName(state.month)} ${state.year}", viewModel::previousMonth, viewModel::nextMonth) { showPicker = true }
                    MonthTally(scoped, period, Modifier.padding(horizontal = AppSpace.page))
                    NaniTabs(listOf(stringResource(R.string.all), stringResource(R.string.pending), stringResource(R.string.paid)),
                        filter, { filter = it }, Modifier.padding(horizontal = AppSpace.page).padding(top = 14.dp, bottom = 12.dp))
                }
            }
            if (state.isLoading) item { AppLoadingState("Atualizando pagamentos", "Organizando o período selecionado.") }
            if (!state.isLoading && visible.isEmpty()) item {
                AppEmptyState(Icons.Rounded.Payments, "Nenhum pagamento encontrado",
                    if (scoped.isEmpty()) "Cadastre inquilinos em Locações para acompanhar os aluguéis do mês."
                    else "Nenhum inquilino corresponde à busca ou ao filtro escolhido.")
            }
            itemsIndexed(visible, key = { _, item -> item.tenant.id }) { index, item ->
                PaymentRecord(item, period, ledgerPosition(index, visible.size)) { selected = item }
            }
        }
    }
    if (showFilters) NaniSheet("Filtrar recebimentos", { showFilters = false }) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(stringResource(R.string.all) to "Todos os inquilinos do mês",
                stringResource(R.string.pending) to "Pendentes e em análise",
                stringResource(R.string.paid) to "Somente aluguéis recebidos").forEachIndexed { index, (label, description) ->
                NaniChoice(label, description, selected = filter == index, onClick = { filter = index; showFilters = false })
            }
        }
    }
    if (showPicker) MonthYearPickerBottomSheet(period, { showPicker = false }) {
        viewModel.changeMonth(it.monthValue, it.year); showPicker = false
    }
    selected?.let { item ->
        PaymentBottomSheet(item, { selected = null }, period.monthInSentence() + " de " + period.year) {
            viewModel.updatePaymentStatus(item.tenant.id, it)
        }
    }
}

/** The month under review. Arrows step through months; the title opens a picker to jump. */
@Composable
private fun MonthSwitcher(title: String, onPrevious: () -> Unit, onNext: () -> Unit, onPick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") }
        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = "Escolher mês", onClick = onPick)
            .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês") }
    }
}

/** Compact account of the month: amount in, amount expected, and the ruler of tenants. */
@Composable
private fun MonthTally(items: List<TenantPaymentItem>, period: YearMonth, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val states = items.map { dueState(it.payment?.status, period, it.tenant.dueDay, today) }
    // The ring counts tenants, like the legend beside it; the amount line carries the money.
    val counts = dueCounts(states)
    val stats = dueStats(counts)
    val percent = percentOf(counts.paid, counts.total)
    val received = items.zip(states).filter { it.second == DueState.PAID }.sumOf { it.first.tenant.amount }
    LedgerSheet(modifier) {
        Row(Modifier.padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusRing(ringOf(stats), "${counts.paid} de ${counts.total} aluguéis pagos", size = 64.dp, stroke = 7.dp) {
                RingLabel(if (counts.total > 0) "$percent%" else "—", 64.dp)
            }
            Spacer(Modifier.width(14.dp))
            TallyText("Recebido", received, items.sumOf { it.tenant.amount }, stats, Modifier.weight(1f))
        }
    }
}

/**
 * The words beside a ring: the amount in, what was expected and a legend whose markers, counts
 * and labels are the ring's own. Pieces flow whole, so a narrow screen wraps them without splitting a value.
 */
@Composable
internal fun TallyText(label: String, amount: Double, expected: Double, stats: List<StatCount>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FittingText(CurrencyUtils.format(amount), NaniType.moneyLarge)
        Text("de ${CurrencyUtils.format(expected)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        RingLegend(stats)
    }
}

@Composable
fun getMonthName(month: Int) = stringArrayResource(R.array.month_names).getOrNull(month - 1).orEmpty()
