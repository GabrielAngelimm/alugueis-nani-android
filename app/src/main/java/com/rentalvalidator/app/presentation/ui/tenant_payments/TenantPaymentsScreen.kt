package com.rentalvalidator.app.presentation.ui.tenant_payments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.LocalAppSnackbarHostState
import com.rentalvalidator.app.presentation.components.rememberAppSnackbar
import com.rentalvalidator.app.presentation.design.AppEmptyState
import com.rentalvalidator.app.presentation.design.AppLoadingState
import com.rentalvalidator.app.presentation.design.NaniHeader
import com.rentalvalidator.app.presentation.design.longLabel
import com.rentalvalidator.app.presentation.design.monthInSentence
import com.rentalvalidator.app.presentation.ui.monthly_grid.PaymentBottomSheet
import com.rentalvalidator.app.presentation.ui.monthly_grid.TenantPaymentItem
import com.rentalvalidator.app.presentation.ui.monthly_grid.displayLabel
import com.rentalvalidator.app.util.WhatsAppUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * A tenant's payments, reached from their detail page, from "Quem falta pagar" and from a rent
 * reminder. [initialPeriod] ("2026-10") opens on that month; otherwise the page opens on today's.
 */
@Composable
fun TenantPaymentsScreen(
    tenantId: String,
    initialPeriod: String? = null,
    onBack: (() -> Unit)? = null,
    viewModel: TenantPaymentsViewModel = hiltViewModel()
) {
    LaunchedEffect(tenantId) { viewModel.open(tenantId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requested = remember(initialPeriod) { initialPeriod?.let { runCatching { YearMonth.parse(it) }.getOrNull() } }
    val context = LocalContext.current
    val snackbars = LocalAppSnackbarHostState.current
    val showSnackbar = rememberAppSnackbar()
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) { state.error?.let { showSnackbar(it); viewModel.clearError() } }

    // The page belongs to the tenant asked for; until their record arrives it shows the loading sheet.
    val current = state.takeIf { it.tenantId == tenantId }
    val tenant = current?.tenant
    Column(Modifier.fillMaxSize()) {
        NaniHeader(stringResource(R.string.payments), tenant?.name ?: "Inquilino", onBack = onBack)
        when {
            current == null || current.isLoading -> AppLoadingState("Abrindo pagamentos", "Reunindo os meses deste inquilino.")
            tenant == null -> AppEmptyState(Icons.Rounded.Payments, "Nenhum pagamento encontrado",
                "Este inquilino não está mais cadastrado" +
                    (requested?.let { ", então não há pagamentos de ${it.monthInSentence()} de ${it.year} para mostrar." } ?: "."))
            else -> TenantPaymentsContent(tenant, current.statuses, requested, LocalDate.now(),
                onMarkPaid = { period ->
                    val previous = current.statuses[period]
                    viewModel.setStatus(period, PaymentStatus.PAGO)
                    // One tap now settles a month, so it can be taken back right away.
                    scope.launch {
                        snackbars.currentSnackbarData?.dismiss()
                        val result = snackbars.showSnackbar("${period.longLabel()} marcado como pago", actionLabel = "Desfazer",
                            duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.restore(period, previous)
                    }
                },
                onChangeStatus = { editing = it.toString() },
                onMessage = if (tenant.phone.isBlank()) null else { period, monthState ->
                    val message = if (monthState == MonthState.PAID) receiptMessage(tenant, period)
                        else WhatsAppUtils.createReminderMessage(tenant, period.monthInSentence())
                    WhatsAppUtils.openWhatsApp(context, tenant.phone, message)
                })
        }
    }
    val editingPeriod = editing?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    if (tenant != null && editingPeriod != null) {
        val stored = current?.statuses?.get(editingPeriod)
        PaymentBottomSheet(
            TenantPaymentItem(tenant, stored?.let { Payment(tenantId = tenant.id, year = editingPeriod.year, month = editingPeriod.monthValue, status = it) }),
            { editing = null }, editingPeriod.monthInSentence() + " de " + editingPeriod.year
        ) { status ->
            viewModel.setStatus(editingPeriod, status)
            showSnackbar("${editingPeriod.longLabel()}: ${status.displayLabel.lowercase()}")
        }
    }
}
