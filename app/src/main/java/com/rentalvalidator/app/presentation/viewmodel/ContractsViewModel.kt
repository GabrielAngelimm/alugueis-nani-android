package com.rentalvalidator.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.rentalvalidator.app.data.files.DocumentStorage
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class ContractsViewModel @Inject constructor(
    private val repository: TenantRepository,
    private val documents: DocumentStorage
) : ViewModel() {
    private val errorChannel = Channel<String>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()

    val tenants: StateFlow<List<Tenant>> = repository.getTenantsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateExpirationDate(tenantId: String, date: String) {
        perform {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                repository.updateTenant(tenant.copy(contractExpirationDate = date))
            }
        }
    }

    fun updateTenantField(tenantId: String, field: String, value: String) {
        perform {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updatedTenant = when (field) {
                    "contractPath" -> tenant.copy(contractPath = value)
                    "inspectionPath" -> tenant.copy(inspectionPath = value)
                    "contractExpirationDate" -> tenant.copy(contractExpirationDate = value)
                    else -> tenant
                }
                repository.updateTenant(updatedTenant)
            }
        }
    }

    fun attach(tenantId: String, field: String, uri: Uri, onSuccess: () -> Unit) = perform {
        val tenant = repository.getTenantById(tenantId) ?: error("Este inquilino não está mais cadastrado.")
        val path = documents.copy(uri, field)
        try {
            val updated = when (field) {
                "contractPath" -> tenant.copy(contractPath = path)
                "inspectionPath" -> tenant.copy(inspectionPath = path)
                else -> error("Tipo de documento inválido.")
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { repository.updateTenant(updated) }
        } catch (failure: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                runCatching { documents.remove(path, field) }.onFailure(failure::addSuppressed)
            }
            throw failure
        }
        onSuccess()
    }

    fun remove(tenantId: String, field: String) = perform {
        val tenant = repository.getTenantById(tenantId) ?: error("Este inquilino não está mais cadastrado.")
        val path = when (field) {
            "contractPath" -> tenant.contractPath
            "inspectionPath" -> tenant.inspectionPath
            else -> error("Tipo de documento inválido.")
        }
        val updated = if (field == "contractPath") tenant.copy(contractPath = "") else tenant.copy(inspectionPath = "")
        // Clear the reference first: a failed database write must not destroy the referenced file.
        repository.updateTenant(updated)
        if (repository.getAllTenants().none { it.contractPath == path || it.inspectionPath == path }) {
            documents.remove(path, field)
        }
    }

    private fun perform(operation: suspend () -> Unit) = viewModelScope.launch {
        try { operation() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { errorChannel.send(failure.message ?: "Não foi possível atualizar o documento.") }
    }
}
