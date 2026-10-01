package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.design.NaniSheet
import com.rentalvalidator.app.presentation.components.ModernDropdownMenu
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Limpar", { bank = "Todos"; unit = "Todas"; day = "Todos" }, Modifier.weight(1f), buttonHeight = 58.dp)
                PrimaryButton("Aplicar", { onApply(bank.takeUnless { it == "Todos" }, unit.takeUnless { it == "Todas" }, day.toIntOrNull()) }, Modifier.weight(1f), buttonHeight = 58.dp)
            }
    }) {
            ModernDropdownMenu("Banco", banks, bank, { bank = it }); ModernDropdownMenu("Unidade", units, unit, { unit = it }); ModernDropdownMenu("Dia do vencimento", listOf("Todos") + (1..31).map(Int::toString), day, { day = it })
    }
}
