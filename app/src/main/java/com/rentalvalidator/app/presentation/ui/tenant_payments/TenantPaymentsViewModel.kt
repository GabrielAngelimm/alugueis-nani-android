package com.rentalvalidator.app.presentation.ui.tenant_payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.repository.PaymentRepository
import com.rentalvalidator.app.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** One tenant and every month recorded for them, across all years. */
data class TenantPaymentsState(
    val tenantId: String? = null,
    val tenant: Tenant? = null,
    val statuses: Map<YearMonth, PaymentStatus> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TenantPaymentsViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val tenantId = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<TenantPaymentsState> = tenantId.flatMapLatest { id ->
        if (id == null) emptyFlow()
        else combine(
            tenantRepository.getTenantsFlow().map { tenants -> tenants.find { it.id == id } },
            paymentRepository.getTenantPaymentsFlow(id)
        ) { tenant, payments ->
            TenantPaymentsState(id, tenant, payments.associate { YearMonth.of(it.year, it.month) to it.status }, isLoading = false)
        }.onStart { emit(TenantPaymentsState(id)) }
            .catch { failure -> emit(TenantPaymentsState(id, isLoading = false, error = failure.message)) }
    }.combine(error) { state, message -> if (message != null) state.copy(error = message) else state }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TenantPaymentsState())

    /** Shows [id]. Opening the same tenant again keeps what is on screen. */
    fun open(id: String) { tenantId.value = id }

    fun setStatus(period: YearMonth, status: PaymentStatus) = write {
        paymentRepository.savePayment(it, period.year, period.monthValue, status)
    }

    /** Puts a month back as it was before an action: its old status, or no record at all. */
    fun restore(period: YearMonth, previous: PaymentStatus?) = write {
        if (previous == null) paymentRepository.deletePayment(it, period.year, period.monthValue)
        else paymentRepository.savePayment(it, period.year, period.monthValue, previous)
    }

    fun clearError() { error.update { null } }

    private fun write(block: suspend (String) -> Unit) {
        val id = tenantId.value ?: return
        viewModelScope.launch {
            try {
                block(id)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                error.value = "Erro ao salvar: ${e.message}"
            }
        }
    }
}
