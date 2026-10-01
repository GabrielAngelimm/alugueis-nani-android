package com.rentalvalidator.app.presentation.ui.tenants
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.theme.AppSpace
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rentalvalidator.app.domain.model.*
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.util.*

@Composable
internal fun TenantDetails(
    tenant: Tenant, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onPayments: () -> Unit, onReminder: () -> Unit, onContact: () -> Unit,
    reminderLeadHours: Int? = null
) {
    val reminderTiming = reminderLeadHours?.let { hours ->
        when {
            hours == 24 -> "1 dia antes do vencimento"
            hours % 24 == 0 -> "${hours / 24} dias antes do vencimento"
            hours == 1 -> "1 hora antes do vencimento"
            else -> "$hours horas antes do vencimento"
        }
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            NaniHeader(stringResource(R.string.tenant), onBack = onBack) {
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Excluir inquilino", tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar inquilino") }
            }
            NaniDetailHero(
                title = tenant.name,
                subtitle = tenant.unit.ifBlank { "Geral" },
                firstLabel = stringResource(R.string.monthly_rent), firstValue = CurrencyUtils.format(tenant.amount),
                secondLabel = stringResource(R.string.due_date), secondValue = "Dia ${tenant.dueDay}",
                subtitleIcon = Icons.Outlined.Apartment
            )
        }
        item {
            NaniDetailCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                NaniDetailAction(stringResource(R.string.payments), "Acompanhar recebimentos", Icons.Outlined.AccountBalanceWallet, onPayments)
                NaniDetailAction(
                    stringResource(R.string.reminder),
                    reminderTiming ?: "Agendar aviso antes do vencimento",
                    Icons.Outlined.NotificationsNone,
                    onReminder,
                    status = if (reminderTiming != null) "Agendado" else null
                )
                NaniDetailAction(stringResource(R.string.contact), PhoneUtils.formatPhone(tenant.phone).ifBlank { "Telefone não informado" },
                    Icons.Outlined.Phone, onContact, divider = false)
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            NaniDetailCard(stringResource(R.string.registration), headerIcon = Icons.Outlined.Badge) {
                CopyableFact("Telefone", PhoneUtils.formatPhone(tenant.phone).ifBlank { "Não informado" })
                CopyableFact("CPF", CpfUtils.format(tenant.cpf).ifBlank { "Não informado" })
                CopyableFact("Banco", tenant.bank.ifBlank { "Não informado" })
                CopyableFact("Apelidos", tenant.aliases.joinToString().ifBlank { "Não informado" })
                CopyableFact("Nome no WhatsApp", tenant.whatsappName.ifBlank { "Não informado" }, divider = false)
            }
            Spacer(Modifier.height(20.dp))
            NaniDetailCard(stringResource(R.string.contract), headerIcon = Icons.Outlined.Description) {
                CopyableFact("Vencimento do contrato", displayDate(tenant.contractExpirationDate, "Não informado"))
                CopyableFact(stringResource(R.string.contract), if (tenant.contractPath.isBlank()) "Não anexado" else "Anexado", copyEnabled = false)
                CopyableFact("Vistoria", if (tenant.inspectionPath.isBlank()) "Não anexada" else "Anexada", divider = false, copyEnabled = false)
            }

        }
    }
}

@Composable
internal fun UnitDetails(
    unitName: String, realUnit: RentalUnit?, tenants: List<Tenant>, onBack: () -> Unit,
    onTenant: (Tenant) -> Unit, onEdit: () -> Unit, onAdd: () -> Unit, onEditUnit: () -> Unit, onDeleteUnit: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Excluir unidade?") },
        text = { Text(if (tenants.isNotEmpty()) "Transfira ou remova os inquilinos vinculados antes de excluir esta unidade." else "A unidade $unitName será excluída permanentemente.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; if (tenants.isEmpty()) onDeleteUnit() }) { Text(if (tenants.isEmpty()) "Excluir" else "Entendi") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            NaniHeader(stringResource(R.string.unit), onBack = onBack) {
                if (realUnit != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteOutline, "Excluir unidade", tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = onEditUnit) { Icon(Icons.Outlined.Edit, "Editar detalhes da unidade") }
            }
            NaniDetailHero(
                title = unitName,
                subtitle = realUnit?.location?.ifBlank { "Localização não informada" } ?: "Agrupamento de inquilinos",
                firstLabel = stringResource(R.string.tenants), firstValue = tenants.size.toString(),
                secondLabel = "Capacidade", secondValue = realUnit?.let {
                    "${it.capacity} ${when (it.capacityKind) {
                        CapacityKind.TENANTS -> if (it.capacity == 1) "inquilino" else "inquilinos"
                        CapacityKind.ROOMS -> if (it.capacity == 1) "quarto" else "quartos"
                        CapacityKind.SPACES -> if (it.capacity == 1) "vaga" else "vagas"
                    }}"
                } ?: "Não informada",
                compact = true,
                subtitleIcon = Icons.Outlined.LocationOn,
                identity = DetailIdentity.UNIT
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tenants), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Outlined.PersonAdd, "Adicionar inquilino") }
            }
            if (tenants.isEmpty()) {
                Column(Modifier.padding(horizontal = AppSpace.page, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nenhum inquilino nesta unidade.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onAdd) { Text("Adicionar inquilino") }
                }
            }
        }
        items(tenants, key = { it.id }) { TenantRecord(it, onTenant) }
        item {
            Spacer(Modifier.height(24.dp))
            NaniDetailCard("Sobre a unidade", headerIcon = Icons.Outlined.HomeWork) {
                CopyableFact("Endereço", realUnit?.location?.ifBlank { "Não informado" } ?: "Não informado")
                CopyableFact("Ocupação", realUnit?.occupancyStatus?.displayName() ?: "Não informada", copyEnabled = false)
                CopyableFact("Tipo", realUnit?.type?.displayName() ?: "Não informado")
                CopyableFact("Situação", when (realUnit?.operationalStatus) {
                    OperationalStatus.ACTIVE -> "Ativa"
                    OperationalStatus.INACTIVE -> "Inativa"
                    OperationalStatus.MAINTENANCE -> "Em manutenção"
                    null -> "Não informado"
                })
                CopyableFact("Condomínio", realUnit?.condominiumFee?.let { CurrencyUtils.format(it) } ?: "Não informado")
                CopyableFact("Observações", realUnit?.notes?.ifBlank { "Não informado" } ?: "Não informado")
                CopyableFact("Documentos", "${tenants.count { it.contractPath.isNotBlank() }} contratos · ${tenants.count { it.inspectionPath.isNotBlank() }} vistorias", divider = false)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitTenantPicker(unit:String,tenants:List<Tenant>,onDismiss:()->Unit,onTenant:(Tenant)->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Text("Editar inquilino",Modifier.padding(horizontal=AppSpace.page),style=MaterialTheme.typography.titleLarge)
        LazyColumn {items(tenants,key={it.id}) {TenantRecord(it,onTenant)}}
    }
}
@Composable
private fun CopyableFact(label: String, value: String, divider: Boolean = true, copyEnabled: Boolean = true) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val showSnackbar = rememberAppSnackbar()
    val canCopy = copyEnabled && value.isNotBlank() && value != "Não informado"
    val copyValue = {
        if (canCopy) {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(label, value))
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            showSnackbar("$label copiado")
        }
    }
    val copyModifier = if (canCopy) {
        Modifier
            .pointerInput(label, value) {
                detectTapGestures(onLongPress = { copyValue() })
            }
            .semantics {
                onLongClick(label = "Copiar $label") {
                    copyValue()
                    true
                }
            }
    } else {
        Modifier
    }

    val colors = MaterialTheme.colorScheme
    val valueColor = if (value == "Não informado" || value == "Não informada" || value == "Não anexado" || value == "Não anexada") {
        colors.onSurfaceVariant
    } else colors.onSurface
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(copyModifier.fillMaxWidth().padding(vertical = 15.dp)) {
        // Long content and enlarged type keep the full width; values are never truncated.
        val stacked = maxWidth < 290.dp || fontScale > 1.15f || value.length > 32 || label.length > 22
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = valueColor)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(label, Modifier.weight(.42f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(value, Modifier.weight(.58f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = valueColor, textAlign = TextAlign.End)
            }
        }
    }
    if (divider) HorizontalDivider(color = colors.outlineVariant.copy(alpha = .35f))
}
