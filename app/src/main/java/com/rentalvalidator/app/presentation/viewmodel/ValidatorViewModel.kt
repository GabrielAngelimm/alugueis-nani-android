package com.rentalvalidator.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import com.rentalvalidator.app.domain.repository.TenantRepository
import com.rentalvalidator.app.domain.usecase.AmbiguousCredit
import com.rentalvalidator.app.domain.usecase.ValidatePaymentsUseCase
import com.rentalvalidator.app.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed class ValidatorState {
    data class Idle(val selectedUri: Uri? = null, val selectedFileName: String? = null) : ValidatorState()
    object Loading : ValidatorState()
    /** [ambiguous] holds the credits that name more than one tenant and count for nobody until assigned. */
    data class Success(val results: List<ValidationResult>, val ambiguous: List<AmbiguousCredit> = emptyList()) : ValidatorState()
    data class Error(val message: String) : ValidatorState()
}

@HiltViewModel
class ValidatorViewModel @Inject constructor(
    private val useCase: ValidatePaymentsUseCase,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: com.rentalvalidator.app.domain.repository.PaymentRepository,
    private val preferencesManager: PreferencesManager,
    private val statementReader: com.rentalvalidator.app.data.importer.StatementReader
) : ViewModel() {
    private val errorChannel = Channel<String>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow<ValidatorState>(ValidatorState.Idle())
    val uiState: StateFlow<ValidatorState> = _uiState.asStateFlow()

    private val _referencePeriod = MutableStateFlow(YearMonth.now())
    val referencePeriod: StateFlow<YearMonth> = _referencePeriod.asStateFlow()
    private var validationJob: Job? = null
    /** Whose each ambiguous credit is, by its index in the checked statement, as the owner settled it. */
    private val assignments = MutableStateFlow<Map<Int, String>>(emptyMap())

    fun updateReferencePeriod(yearMonth: YearMonth) {
        _referencePeriod.value = yearMonth
    }

    fun selectFile(uri: Uri, fileName: String) {
        validationJob?.cancel()
        _uiState.value = ValidatorState.Idle(selectedUri = uri, selectedFileName = fileName)
    }

    fun clearSelectedFile() {
        validationJob?.cancel()
        _uiState.value = ValidatorState.Idle()
    }

    fun validateSelectedFile() {
        val currentState = _uiState.value as? ValidatorState.Idle
        val uri = currentState?.selectedUri ?: return
        
        validationJob?.cancel()
        assignments.value = emptyMap()
        _uiState.value = ValidatorState.Loading
        validationJob = viewModelScope.launch {
            try {
                val referencePeriod = _referencePeriod.value
                val transactions = statementReader.read(uri, referencePeriod.year)

                
                if (transactions.isEmpty()) {
                    _uiState.value = ValidatorState.Error("Nenhuma transação encontrada no arquivo.")
                    return@launch
                }

                // Keep the inspected file in memory and refresh its results when a tenant or
                // penalty setting is edited, or a credit is assigned, without rereading SAF content.
                combine(tenantRepository.getTenantsFlow(), preferencesManager.penaltyConfigFlow, assignments) { tenants, penaltyConfig, assigned ->
                    useCase.check(
                        tenants = tenants,
                        transactions = transactions,
                        referencePeriod = referencePeriod,
                        evaluationDate = LocalDate.now(),
                        penaltyFeePercentage = penaltyConfig.penaltyPct,
                        penaltyInterestPerDayPercentage = penaltyConfig.dailyInterestPct,
                        assignments = assigned
                    )
                }.collect { check ->
                    _uiState.value = ValidatorState.Success(check.results.sortedBy { it.tenant.name }, check.ambiguous)
                }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.value = ValidatorState.Error(e.message ?: "Erro ao processar arquivo")
            }
        }
    }
    
    /** Counts an ambiguous credit for the tenant the owner picked; the results refresh on their own. */
    fun assignCredit(index: Int, tenantId: String) {
        assignments.update { it + (index to tenantId) }
    }

    fun reset() {
        validationJob?.cancel()
        _uiState.value = ValidatorState.Idle()
    }

    fun registerPayment(tenantId: String, year: Int, month: Int, status: com.rentalvalidator.app.domain.model.PaymentStatus, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                paymentRepository.savePayment(tenantId, year, month, status)
                onSuccess()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { errorChannel.send(failure.message ?: "Não foi possível registrar o pagamento.") }
        }
    }
}
