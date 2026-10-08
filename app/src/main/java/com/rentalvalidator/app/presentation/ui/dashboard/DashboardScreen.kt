package com.rentalvalidator.app.presentation.ui.dashboard

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.viewmodel.TenantsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel
import com.rentalvalidator.app.util.ContractStatus
import com.rentalvalidator.app.util.getContractStatus
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DashboardScreen(viewModel: TenantsViewModel = hiltViewModel(), unitsViewModel: UnitsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}, onNavigateToContracts: () -> Unit = {}, onNavigateToPayments: () -> Unit = {},
    onNavigateToValidator: () -> Unit = {}, onNavigateToTenants: () -> Unit = {},
    onNavigateToTenantPayments: (String) -> Unit = {}) {
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val payments by viewModel.recentPayments.collectAsStateWithLifecycle()
    val rentalUnits by unitsViewModel.units.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val period = YearMonth.from(today)
    val monthPayments = payments.filter { it.year == today.year && it.month == today.monthValue }
    val paidIds = monthPayments.filter { it.status == PaymentStatus.PAGO }.mapTo(hashSetOf()) { it.tenantId }
    val expected = tenants.sumOf { it.amount }
    val received = tenants.filter { it.id in paidIds }.sumOf { it.amount }
    val pending = (expected - received).coerceAtLeast(0.0)
    val contractAlerts = tenants.count {
        getContractStatus(it.contractExpirationDate, it.contractPath.isNotBlank()).status in
            setOf(ContractStatus.VENCIDO, ContractStatus.VENCE_EM_BREVE)
    }
    val progress = if (expected == 0.0) 0f else (received / expected).toFloat().coerceIn(0f, 1f)
    val history = receiptHistorySnapshot(period, tenants, payments)
    // Presentation only: the stored status per tenant for the current month, in display order.
    val statusById = monthPayments.associate { it.tenantId to it.status }
    val entries = tenants.sortedWith(compareBy({ it.dueDay }, { it.name })).map {
        OverviewEntry(it.id, it.name, it.unit.ifBlank { "Geral" }, it.amount, it.dueDay, statusById[it.id])
    }

    OverviewContent(
        OverviewSnapshot(expected, received, pending, progress, tenants.size, rentalUnits.size, contractAlerts,
            history.months, history.values, period, today, entries),
        onNavigateToSettings, onNavigateToPayments, onNavigateToValidator, onNavigateToTenants, onNavigateToContracts,
        onNavigateToTenantPayments)
}
