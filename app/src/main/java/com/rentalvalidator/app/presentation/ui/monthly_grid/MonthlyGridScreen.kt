package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.navigation.FinanceNavigation
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyGridScreen(viewModel: MonthlyGridViewModel = hiltViewModel(), tenantId: String? = null, initialPeriod: String? = null,
    onBack: (() -> Unit)? = null, onFinanceNavigate: (com.rentalvalidator.app.Screen) -> Unit = {}) {
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
    LaunchedEffect(initialPeriod) {
        initialPeriod?.let { value -> runCatching { YearMonth.parse(value) }.getOrNull()?.let { viewModel.changeMonth(it.monthValue, it.year) } }
    }
    val period = YearMonth.of(state.year, state.month)
    val scoped = state.items.filter { tenantId == null || it.tenant.id == tenantId }
    val visible = scoped.filter { (it.tenant.name.contains(query, true) || it.tenant.unit.contains(query, true)) &&
        (filter == 0 || if (filter == 1) it.payment?.status != PaymentStatus.PAGO else it.payment?.status == PaymentStatus.PAGO) }
    val scopedTenant = scoped.firstOrNull()?.tenant
    if (showSearch) NaniSearchDialog(query, { query = it }, { showSearch = false })
    Column(Modifier.fillMaxSize()) {
        if (tenantId != null) NaniHeader(stringResource(R.string.payments), scopedTenant?.name ?: "Inquilino", onBack = onBack)
        else FinanceNavigation("grid", onFinanceNavigate) {
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
                    if (tenantId == null) {
                        MonthTally(scoped, period, Modifier.padding(horizontal = AppSpace.page))
                        NaniTabs(listOf(stringResource(R.string.all), stringResource(R.string.pending), stringResource(R.string.paid)),
                            filter, { filter = it }, Modifier.padding(horizontal = AppSpace.page).padding(top = 14.dp, bottom = 12.dp))
                    } else Spacer(Modifier.height(8.dp))
                }
            }
            if (tenantId != null && scopedTenant != null) item {
                YearOfPayments(scopedTenant, state.year, state.month, state.yearStatuses[scopedTenant.id].orEmpty()) { month ->
                    viewModel.changeMonth(month, state.year)
                }
            }
            if (state.isLoading) item { AppLoadingState("Atualizando pagamentos", "Organizando o período selecionado.") }
            if (!state.isLoading && visible.isEmpty()) item {
                AppEmptyState(Icons.Rounded.Payments, "Nenhum pagamento encontrado",
                    if (tenantId != null) "Este inquilino não está mais cadastrado ou não tem aluguel neste período."
                    else if (scoped.isEmpty()) "Cadastre inquilinos em Locações para acompanhar os aluguéis do mês."
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
    val paidCount = states.count { it == DueState.PAID }
    val amountIn = { state: DueState -> items.zip(states).filter { it.second == state }.sumOf { it.first.tenant.amount } }
    val received = amountIn(DueState.PAID)
    val expected = items.sumOf { it.tenant.amount }
    val overdue = states.count { it == DueState.OVERDUE }
    val percent = (shareOf(received, expected) * 100).toInt()
    LedgerSheet(modifier) {
        Row(Modifier.padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusRing(
                segments = listOf(
                    RingSegment(shareOf(received, expected), ringColor(StatusKind.SUCCESS)),
                    RingSegment(shareOf(amountIn(DueState.REVIEW), expected), ringColor(StatusKind.INFO)),
                    RingSegment(shareOf(amountIn(DueState.OVERDUE), expected), ringColor(StatusKind.ERROR))
                ),
                description = "$percent% do previsto recebido",
                size = 64.dp, stroke = 7.dp
            ) { RingLabel(if (expected > 0) "$percent%" else "—", 64.dp) }
            Spacer(Modifier.width(14.dp))
            TallyText("Recebido", received, expected, "$paidCount de ${items.size} pagos", overdue, Modifier.weight(1f))
        }
    }
}

/**
 * The words beside a month's ring: the amount in, what was expected and the counts. The
 * secondary facts flow as whole pieces, so enlarged type wraps them without splitting a value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TallyText(label: String, amount: Double, expected: Double, paidLine: String, late: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FittingText(CurrencyUtils.format(amount), NaniType.moneyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("de ${CurrencyUtils.format(expected)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = false)
            Text(paidLine, style = MaterialTheme.typography.labelMedium, color = NaniTheme.colors.paid.ink, softWrap = false)
            if (late > 0) Text("$late em atraso", style = MaterialTheme.typography.labelMedium,
                color = NaniTheme.colors.overdue.ink, softWrap = false)
        }
    }
}

/**
 * A tenant's year at a glance: twelve months marked paid, under review or open. Tapping a
 * month opens it below. Months before the tenant's registration are left blank.
 */
@Composable
private fun YearOfPayments(tenant: Tenant, year: Int, selectedMonth: Int, statuses: Map<Int, PaymentStatus>, onMonth: (Int) -> Unit) {
    val names = stringArrayResource(R.array.month_names)
    val today = LocalDate.now()
    val since = runCatching { YearMonth.from(LocalDate.parse(tenant.dateCreated)) }.getOrNull()
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pagamentos em $year", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text("${statuses.count { it.value == PaymentStatus.PAGO }} de 12 pagos", style = MaterialTheme.typography.labelMedium,
                color = nani.paid.ink)
        }
        Spacer(Modifier.height(10.dp))
        LedgerSheet(contentPadding = PaddingValues(8.dp)) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..12).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { month ->
                            val period = YearMonth.of(year, month)
                            val status = statuses[month]
                            val beforeTenant = since != null && period.isBefore(since) && status == null
                            // Only what was recorded is shown; a past month with no record is not claimed as late.
                            val current = period == YearMonth.from(today)
                            val state = if (beforeTenant || (status == null && !current)) null
                                else dueState(status, period, tenant.dueDay, today)
                            val tone = state?.let { statusTone(it.kind()) }
                            val active = month == selectedMonth
                            val description = when (state) {
                                null -> "sem registro"
                                DueState.PAID -> "pago"
                                DueState.REVIEW -> "em análise"
                                DueState.OVERDUE -> "em atraso"
                                else -> "a vencer"
                            }
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(tone?.fill?.copy(alpha = if (state == DueState.UPCOMING || state == DueState.DUE_TODAY) .55f else 1f)
                                        ?: colors.surfaceContainerLow)
                                    .then(if (active) Modifier.border(2.dp, colors.primary, RoundedCornerShape(12.dp)) else Modifier)
                                    .selectable(active, role = Role.Tab, onClick = { onMonth(month) })
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "${names[month - 1]}: $description"
                                        if (active) stateDescription = "Exibido abaixo"
                                    }
                                    .heightIn(min = 56.dp).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(names[month - 1].take(3), style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold),
                                    color = if (state == null) colors.onSurfaceVariant else colors.onSurface)
                                val glyph = when (state) {
                                    DueState.PAID -> Icons.Rounded.Check
                                    DueState.REVIEW -> Icons.Rounded.HourglassTop
                                    DueState.OVERDUE -> Icons.Rounded.PriorityHigh
                                    else -> null
                                }
                                if (glyph != null) Icon(glyph, null, Modifier.size(16.dp), tint = tone!!.ink)
                                else Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(width = 10.dp, height = 2.dp).background(colors.outlineVariant))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getMonthName(month: Int) = stringArrayResource(R.array.month_names).getOrNull(month - 1).orEmpty()
