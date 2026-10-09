package com.rentalvalidator.app.presentation.ui.tenants

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.*
import com.rentalvalidator.app.presentation.components.rememberAppSnackbar
import com.rentalvalidator.app.presentation.components.LocalNavigationClearance
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp + LocalNavigationClearance.current)) {
        item {
            NaniHeader(stringResource(R.string.tenant), onBack = onBack) {
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Excluir inquilino", tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Editar inquilino") }
            }
            NaniDetailHero(
                title = tenant.name,
                subtitle = tenant.unit.ifBlank { "Geral" },
                firstLabel = stringResource(R.string.monthly_rent), firstValue = CurrencyUtils.format(tenant.amount),
                secondLabel = stringResource(R.string.due_date), secondValue = "Dia ${tenant.dueDay}",
                subtitleIcon = Icons.Rounded.MeetingRoom
            )
        }
        item {
            NaniDetailCard(contentPadding = PaddingValues(0.dp)) {
                NaniDetailAction(stringResource(R.string.payments), "Ver e atualizar os meses deste inquilino", Icons.Rounded.Payments, onPayments)
                NaniDetailAction(stringResource(R.string.reminder), reminderTiming ?: "Receber um aviso antes do vencimento",
                    Icons.Rounded.NotificationsNone, onReminder, status = if (reminderTiming != null) "Agendado" else null)
                NaniDetailAction(stringResource(R.string.contact), PhoneUtils.formatPhone(tenant.phone).ifBlank { "Telefone não informado" },
                    Icons.Rounded.Phone, onContact, divider = false)
            }
        }
        item {
            Spacer(Modifier.height(AppSpace.section))
            NaniDetailCard(stringResource(R.string.registration)) {
                CopyableFact("Telefone", PhoneUtils.formatPhone(tenant.phone).ifBlank { "Não informado" })
                CopyableFact("CPF", CpfUtils.format(tenant.cpf).ifBlank { "Não informado" })
                CopyableFact("Banco", tenant.bank.ifBlank { "Não informado" })
                CopyableFact("Apelidos", tenant.aliases.joinToString().ifBlank { "Não informado" })
                CopyableFact("Nome no WhatsApp", tenant.whatsappName.ifBlank { "Não informado" }, divider = false)
            }
            Text("Toque e segure um dado para copiá-lo.", Modifier.padding(horizontal = AppSpace.page + 4.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Spacer(Modifier.height(AppSpace.large))
            NaniDetailCard(stringResource(R.string.contract)) {
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
    if (confirmDelete) NaniConfirmDialog(
        title = "Excluir unidade?",
        onDismiss = { confirmDelete = false },
        confirmLabel = if (tenants.isEmpty()) "Excluir" else "Entendi",
        onConfirm = { confirmDelete = false; if (tenants.isEmpty()) onDeleteUnit() },
        text = {
            DialogText(if (tenants.isNotEmpty()) "Transfira ou remova os inquilinos vinculados antes de excluir esta unidade."
                else "A unidade $unitName será excluída permanentemente.")
        },
        dismissLabel = if (tenants.isEmpty()) "Cancelar" else null,
        destructive = tenants.isEmpty(),
        icon = Icons.Rounded.DeleteOutline
    )
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp + LocalNavigationClearance.current)) {
        item {
            NaniHeader(stringResource(R.string.unit), onBack = onBack) {
                if (realUnit != null) IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, "Excluir unidade", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onEditUnit) { Icon(Icons.Rounded.Edit, "Editar detalhes da unidade") }
            }
            NaniDetailHero(
                title = unitName,
                subtitle = realUnit?.location?.ifBlank { "Endereço não informado" } ?: "Agrupamento de inquilinos",
                // What the unit brings in and how many tenants it holds against its capacity.
                firstLabel = "Aluguéis por mês", firstValue = CurrencyUtils.format(tenants.sumOf { it.amount }),
                secondLabel = stringResource(R.string.tenants),
                secondValue = realUnit?.let { "${it.tenantCount} de ${it.capacity}" } ?: tenants.size.toString(),
                compact = true,
                subtitleIcon = Icons.Rounded.LocationOn,
                identity = DetailIdentity.UNIT,
                unitIcon = realUnit?.type?.glyph() ?: UnitGroupGlyph
            )
        }
        item {
            SectionTitle(stringResource(R.string.tenants), Modifier.padding(bottom = 8.dp)) {
                FilledTonalIconButton(onClick = onAdd, shape = RoundedCornerShape(AppSize.controlRadius)) {
                    Icon(Icons.Rounded.PersonAdd, "Adicionar inquilino")
                }
            }
            if (tenants.isEmpty()) {
                LedgerSheet(Modifier.padding(horizontal = AppSpace.page), contentPadding = PaddingValues(20.dp)) {
                    Text("Nenhum inquilino nesta unidade.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onAdd, contentPadding = PaddingValues(0.dp)) { Text("Adicionar inquilino") }
                }
            }
        }
        itemsIndexed(tenants, key = { _, tenant -> tenant.id }) { index, tenant ->
            TenantRecord(tenant, onTenant, ledgerPosition(index, tenants.size), showUnit = false)
        }
        item {
            Spacer(Modifier.height(AppSpace.section))
            NaniDetailCard("Sobre a unidade") {
                CopyableFact("Endereço", realUnit?.location?.ifBlank { "Não informado" } ?: "Não informado")
                CopyableFact("Tipo", realUnit?.type?.displayName() ?: "Não informado")
                CopyableFact("Situação", when (realUnit?.operationalStatus) {
                    OperationalStatus.ACTIVE -> "Ativa"
                    OperationalStatus.INACTIVE -> "Inativa"
                    OperationalStatus.MAINTENANCE -> "Em manutenção"
                    null -> "Não informado"
                })
                // Most units are houses without a condominium fee, so the line appears only when one is recorded.
                realUnit?.condominiumFee?.let { CopyableFact("Condomínio", CurrencyUtils.format(it)) }
                CopyableFact("Observações", realUnit?.notes?.ifBlank { "Não informado" } ?: "Não informado")
                val contracts = tenants.count { it.contractPath.isNotBlank() }
                val inspections = tenants.count { it.inspectionPath.isNotBlank() }
                CopyableFact("Documentos", "${contracts} ${if (contracts == 1) "contrato" else "contratos"} e $inspections ${if (inspections == 1) "vistoria" else "vistorias"}",
                    divider = false)
            }
        }
    }
}

@Composable
internal fun UnitTenantPicker(unit: String, tenants: List<Tenant>, onDismiss: () -> Unit, onTenant: (Tenant) -> Unit) {
    NaniSheet("Editar inquilino", onDismiss) {
        Text("Escolha quem editar em $unit.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LedgerSheet {
            tenants.forEachIndexed { index, tenant ->
                if (index > 0) LedgerRule(Modifier.padding(start = 70.dp))
                TenantLine(tenant, { onTenant(tenant) }, showUnit = false)
            }
        }
    }
}

private val EmptyValues = setOf("Não informado", "Não informada", "Não anexado", "Não anexada")

/** A fact on a detail page. Long-press copies it, with a confirmation and haptic feedback. */
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
            .pointerInput(label, value) { detectTapGestures(onLongPress = { copyValue() }) }
            .semantics { onLongClick(label = "Copiar $label") { copyValue(); true } }
    } else Modifier

    val colors = MaterialTheme.colorScheme
    val valueColor = if (value in EmptyValues) colors.onSurfaceVariant else colors.onSurface
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(copyModifier.fillMaxWidth().padding(vertical = 14.dp)) {
        // Long content and enlarged type keep the full width; values are never truncated.
        val stacked = maxWidth < 290.dp || fontScale > 1.15f || value.length > 32 || label.length > 22
        val valueStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (value in EmptyValues) FontWeight.Normal else FontWeight.SemiBold)
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(value, style = valueStyle, color = valueColor)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(label, Modifier.weight(.42f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(value, Modifier.weight(.58f), style = valueStyle, color = valueColor, textAlign = TextAlign.End)
            }
        }
    }
    if (divider) LedgerRule()
}
