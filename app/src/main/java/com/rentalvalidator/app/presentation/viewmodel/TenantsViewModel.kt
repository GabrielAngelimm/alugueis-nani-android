package com.rentalvalidator.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.repository.PaymentRepository
import com.rentalvalidator.app.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class TenantsViewModel @Inject constructor(
    private val repository: TenantRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    private val errorChannel = Channel<String>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()

    val tenants: StateFlow<List<Tenant>> = repository.getTenantsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentYearPayments: StateFlow<List<Payment>> = paymentRepository.getPaymentsFlow(java.time.LocalDate.now().year)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentPayments: StateFlow<List<Payment>> = combine(
        currentYearPayments,
        paymentRepository.getPaymentsFlow(java.time.LocalDate.now().year - 1)
    ) { current, previous -> current + previous }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTenant(tenant: Tenant, onSuccess: () -> Unit = {}, onError: ((String) -> Unit)? = null) =
        perform(onSuccess, onError) { repository.insertTenant(tenant) }
    fun updateTenant(tenant: Tenant, onSuccess: () -> Unit = {}) = perform(onSuccess) { repository.updateTenant(tenant) }
    fun deleteTenant(tenant: Tenant, onSuccess: () -> Unit = {}) = perform(onSuccess) { repository.deleteTenant(tenant) }

    /** Failures go to [onError] when the caller shows them itself, such as an open editor; otherwise to [errors]. */
    private fun perform(onSuccess: () -> Unit, onError: ((String) -> Unit)? = null, operation: suspend () -> Unit) = viewModelScope.launch {
        try { operation(); onSuccess() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            val message = failure.message ?: "Não foi possível salvar o cadastro."
            if (onError != null) onError(message) else errorChannel.send(message)
        }
    }
}
