package com.rentalvalidator.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UnitOperationResult {
    data object Success : UnitOperationResult()
    data class Error(val message: String) : UnitOperationResult()
}

@HiltViewModel
class UnitsViewModel @Inject constructor(
    private val repository: UnitRepository
) : ViewModel() {

    val units: StateFlow<List<RentalUnit>> = repository.getUnitsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _operationResult = MutableStateFlow<UnitOperationResult?>(null)
    val operationResult: StateFlow<UnitOperationResult?> = _operationResult.asStateFlow()

    fun clearResult() { _operationResult.value = null }

    fun saveUnit(
        unit: RentalUnit,
        isNew: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Check name uniqueness
                val excludeId = if (isNew) null else unit.id
                if (repository.nameExists(unit.name.trim(), excludeId = excludeId)) {
                    onError("Já existe uma unidade com este nome.")
                    return@launch
                }
                if (isNew) {
                    val success = repository.insertUnit(unit)
                    if (!success) {
                        onError("Não foi possível salvar a unidade. Tente novamente.")
                        return@launch
                    }
                } else {
                    repository.updateUnit(unit)
                }
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Erro inesperado.")
            }
        }
    }

    fun deactivateUnit(
        unitId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateOperationalStatus(unitId, OperationalStatus.INACTIVE)
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Erro ao desativar unidade.")
            }
        }
    }

    fun reactivateUnit(
        unitId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateOperationalStatus(unitId, OperationalStatus.ACTIVE)
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Erro ao reativar unidade.")
            }
        }
    }

    fun deleteUnit(
        unitId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteUnit(unitId)
                onSuccess()
            } catch (e: IllegalStateException) {
                onError("Esta unidade possui inquilinas vinculadas e não pode ser excluída.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Erro ao excluir unidade.")
            }
        }
    }

    suspend fun nameExists(name: String, excludeId: String? = null): Boolean =
        repository.nameExists(name, excludeId)

    suspend fun getTenantCount(unitId: String): Int =
        repository.getTenantCount(unitId)
}
