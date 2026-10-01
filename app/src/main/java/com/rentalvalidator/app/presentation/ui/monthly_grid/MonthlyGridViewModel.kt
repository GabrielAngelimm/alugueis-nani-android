package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.repository.PaymentRepository
import com.rentalvalidator.app.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TenantPaymentItem(
    val tenant: Tenant,
    val payment: Payment?
)

data class MonthlyGridState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val items: List<TenantPaymentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MonthlyGridViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(MonthlyGridState())
    val state: StateFlow<MonthlyGridState> = _state.asStateFlow()
    private var requestedPeriod = YearMonth.of(_state.value.year, _state.value.month)
    private var hasLoadedOnce = false

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        val period = requestedPeriod
        loadJob = viewModelScope.launch {
            if (!hasLoadedOnce) _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Keep the displayed period and its rows together until the next
                // period is ready. Rapid taps only publish the latest request.
                combine(
                    tenantRepository.getTenantsFlow(),
                    paymentRepository.getPaymentsFlow(period.year)
                ) { tenants, allPayments ->
                    val monthPayments = allPayments.filter { it.month == period.monthValue }
                    
                    val items = tenants.map { tenant ->
                        val payment = monthPayments.find { it.tenantId == tenant.id }
                        TenantPaymentItem(tenant, payment)
                    }
                    items.sortedBy { it.tenant.unit }
                }.collect { items ->
                    hasLoadedOnce = true
                    _state.update { it.copy(year = period.year, month = period.monthValue,
                        items = items, isLoading = false, error = null) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun changeMonth(newMonth: Int, newYear: Int) {
        requestedPeriod = YearMonth.of(newYear, newMonth)
        loadData()
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    fun previousMonth() {
        val previous = requestedPeriod.minusMonths(1)
        changeMonth(previous.monthValue, previous.year)
    }

    fun nextMonth() {
        val next = requestedPeriod.plusMonths(1)
        changeMonth(next.monthValue, next.year)
    }

    fun updatePaymentStatus(tenantId: String, status: PaymentStatus) {
        viewModelScope.launch {
            try {
                paymentRepository.savePayment(
                    tenantId = tenantId,
                    year = _state.value.year,
                    month = _state.value.month,
                    status = status
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(error = "Erro ao salvar: ${e.message}") }
            }
        }
    }
}
