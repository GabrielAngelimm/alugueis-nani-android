package com.rentalvalidator.app.presentation.ui.dashboard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.viewmodel.TenantsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel
import com.rentalvalidator.app.util.ContractStatus
import com.rentalvalidator.app.util.getContractStatus
import java.time.LocalDate

@Composable
fun DashboardScreen(viewModel:TenantsViewModel=hiltViewModel(),unitsViewModel:UnitsViewModel=hiltViewModel(),
    onNavigateToSettings:()->Unit={},onNavigateToContracts:()->Unit={},onNavigateToPayments:()->Unit={},
    onNavigateToValidator:()->Unit={},onNavigateToTenants:()->Unit={}) {
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val payments by viewModel.recentPayments.collectAsStateWithLifecycle()
    val rentalUnits by unitsViewModel.units.collectAsStateWithLifecycle()
    val currentMonth = LocalDate.now().monthValue
    val monthPayments = payments.filter { it.year == LocalDate.now().year && it.month == currentMonth }
    val paidIds = monthPayments.filter { it.status == PaymentStatus.PAGO }.mapTo(hashSetOf()) { it.tenantId }
    val expected = tenants.sumOf { it.amount }
    val received = tenants.filter { it.id in paidIds }.sumOf { it.amount }
    val pending = (expected - received).coerceAtLeast(0.0)
    val unitCount = rentalUnits.size
    val contractAlerts = tenants.count {
        getContractStatus(it.contractExpirationDate, it.contractPath.isNotBlank()).status in
            setOf(ContractStatus.VENCIDO, ContractStatus.VENCE_EM_BREVE)
    }
    val progress = if (expected == 0.0) 0f else (received / expected).toFloat().coerceIn(0f, 1f)
    val history = receiptHistorySnapshot(java.time.YearMonth.now(), tenants, payments)

    OverviewContent(OverviewSnapshot(expected,received,pending,progress,tenants.size,unitCount,contractAlerts,history.months,history.values),
        onNavigateToSettings,onNavigateToPayments,onNavigateToValidator,onNavigateToTenants,onNavigateToContracts)
}
