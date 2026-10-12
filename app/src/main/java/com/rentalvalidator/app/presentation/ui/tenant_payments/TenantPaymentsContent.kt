package com.rentalvalidator.app.presentation.ui.tenant_payments

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.LocalNavigationClearance
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * A tenant's payments: the year card on top (what came in, a verdict and the twelve months) and,
 * under it, the chosen month as a coupon. The coupons slide sideways from month to month and
 * across years; the month tabs and the year arrows follow, and tapping a tab brings its coupon.
 */
@Composable
fun TenantPaymentsContent(
    tenant: Tenant,
    statuses: Map<YearMonth, PaymentStatus>,
    requested: YearMonth?,
    today: LocalDate,
    onMarkPaid: (YearMonth) -> Unit,
    onChangeStatus: (YearMonth) -> Unit,
    onMessage: ((YearMonth, MonthState) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val since = remember(tenant.dateCreated) { tenancyStart(tenant.dateCreated) }
    val range = remember(since, statuses.keys, requested, today) { monthRange(since, statuses.keys, requested, today) }
    var selected by rememberSaveable { mutableStateOf(requested ?: YearMonth.from(today)) }
    fun stateOf(period: YearMonth) = monthState(statuses[period], period, tenant.dueDay, today, since)
    val firstName = tenant.name.trim().substringBefore(' ').ifBlank { "o inquilino" }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var stubHeight by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // The pager counts months from the range's first January; should that move, it starts over on the same month.
    key(range.first) {
        val pager = rememberPagerState(initialPage = range.pageOf(selected)) { range.pageCount }
        val strip = rememberLazyListState(initialFirstVisibleItemIndex = (selected.monthValue - 3).coerceAtLeast(0))
        LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { selected = range.monthAt(it) } }
        LaunchedEffect(requested) { requested?.let { if (range.monthAt(pager.currentPage) != it) pager.scrollToPage(range.pageOf(it)) } }
        var centered by remember { mutableStateOf(false) }
        LaunchedEffect(selected) { strip.centerOn(selected.monthValue - 1, animate = centered); centered = true }

        val year = selected.year
        val months = remember(year, statuses, tenant.dueDay, today, since) {
            (1..12).map { YearMonth.of(year, it) }.map { it to stateOf(it) }
        }
        val summary = remember(months, tenant.amount) { yearSummary(months.map { it.second }, tenant.amount) }
        fun go(period: YearMonth) {
            selected = period
            scope.launch { pager.animateScrollToPage(range.pageOf(period)) }
        }

        Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = 24.dp + LocalNavigationClearance.current)) {
            YearCard(tenant.name, year, today, summary, months, selected, strip,
                canGoBack = range.contains(year - 1), canGoForward = range.contains(year + 1),
                onPreviousYear = { go(selected.minusYears(1)) }, onNextYear = { go(selected.plusYears(1)) }, onMonth = ::go)
            Spacer(Modifier.height(16.dp))
            // Neighbouring coupons show a sliver at the edges, so the row reads as something to slide.
            HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = AppSpace.page),
                pageSpacing = 8.dp, beyondViewportPageCount = 1, verticalAlignment = Alignment.Top,
                key = { range.monthAt(it).toString() }) { page ->
                val period = range.monthAt(page)
                val state = stateOf(period)
                MonthSlip(period, state, tenant.amount, tenant.dueDay, today, firstName,
                    onMarkPaid = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onMarkPaid(period) },
                    onChangeStatus = { onChangeStatus(period) },
                    onMessage = onMessage?.let { message -> { message(period, state) } },
                    stubMinHeight = with(density) { stubHeight.toDp() },
                    onStubMeasured = { if (it > stubHeight) stubHeight = it })
            }
            Text("Os valores seguem o aluguel atual de ${CurrencyUtils.format(tenant.amount)}, que vence todo dia ${tenant.dueDay}.",
                Modifier.padding(horizontal = AppSpace.page + 4.dp).padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Scrolls the month row so tab [index] sits in the middle, or as near as the row's ends allow. */
private suspend fun LazyListState.centerOn(index: Int, animate: Boolean) {
    var item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        scrollToItem(index)
        item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    }
    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val delta = (item.offset + item.size / 2 - center).toFloat()
    if (animate) animateScrollBy(delta) else scrollBy(delta)
}
