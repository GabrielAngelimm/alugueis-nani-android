package com.rentalvalidator.app.presentation.ui.contracts

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.NaniDropdownField
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.NaniSheet
import com.rentalvalidator.app.util.ContractStatus

@Composable
fun ContractFilterBottomSheet(
    initialStatus: ContractStatus?,
    initialUnit: String?,
    availableUnits: List<String>,
    onDismiss: () -> Unit,
    onApply: (ContractStatus?, String?) -> Unit
) {
    val statuses = linkedMapOf("Todos" to null, "Vigente" to ContractStatus.VIGENTE, "Vence em breve" to ContractStatus.VENCE_EM_BREVE,
        "Vencido" to ContractStatus.VENCIDO, "Data pendente" to ContractStatus.DATA_PENDENTE, "Sem contrato" to ContractStatus.SEM_CONTRATO)
    var status by remember { mutableStateOf(statuses.entries.firstOrNull { it.value == initialStatus }?.key ?: "Todos") }
    var unit by remember { mutableStateOf(initialUnit ?: "Todas") }
    val units = listOf("Todas") + availableUnits.filter { it.isNotBlank() }.distinct()
    NaniSheet("Filtrar contratos", onDismiss, actions = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Limpar", { status = "Todos"; unit = "Todas" }, Modifier.weight(1f))
            PrimaryButton("Aplicar", { onApply(statuses[status], unit.takeUnless { it == "Todas" }) }, Modifier.weight(1f))
        }
    }) {
        NaniDropdownField("Status", statuses.keys.toList(), status, { status = it })
        NaniDropdownField("Unidade", units, unit, { unit = it })
    }
}
