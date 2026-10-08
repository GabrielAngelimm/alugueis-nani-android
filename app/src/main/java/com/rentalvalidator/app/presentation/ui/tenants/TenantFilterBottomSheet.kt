package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.NaniDropdownField
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.NaniSheet

@Composable
fun TenantFilterBottomSheet(
    initialBank: String?,
    initialUnit: String?,
    initialDueDay: Int?,
    unitNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onApply: (String?, String?, Int?) -> Unit
) {
    var bank by remember { mutableStateOf(initialBank ?: "Todos") }
    var unit by remember { mutableStateOf(initialUnit ?: "Todas") }
    var day by remember { mutableStateOf(initialDueDay?.toString() ?: "Todos") }
    val banks = listOf("Todos", "Nenhum", "Itaú", "Santander", "Nubank", "Mercado Pago", "PagSeguro", "Bradesco", "Banco do Brasil", "Inter", "Caixa", "Sicredi")
    val units = listOf("Todas") + unitNames.ifEmpty { listOf("Geral") }
    NaniSheet("Filtrar inquilinos", onDismiss, actions = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Limpar", { bank = "Todos"; unit = "Todas"; day = "Todos" }, Modifier.weight(1f))
            PrimaryButton("Aplicar", { onApply(bank.takeUnless { it == "Todos" }, unit.takeUnless { it == "Todas" }, day.toIntOrNull()) }, Modifier.weight(1f))
        }
    }) {
        NaniDropdownField("Banco", banks, bank, { bank = it })
        NaniDropdownField("Unidade", units, unit, { unit = it })
        NaniDropdownField("Dia do vencimento", listOf("Todos") + (1..31).map(Int::toString), day, { day = it })
    }
}
