package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.presentation.components.AppFormFooter
import com.rentalvalidator.app.presentation.components.AppFormSection
import com.rentalvalidator.app.presentation.components.NaniDropdownField
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.DialogText
import com.rentalvalidator.app.presentation.design.NaniConfirmDialog
import com.rentalvalidator.app.presentation.design.NaniEditor
import com.rentalvalidator.app.presentation.theme.AppSpace
import java.time.Instant
import java.util.UUID

private val unitTypeLabels = mapOf(
    UnitType.APARTMENT to "Apartamento",
    UnitType.HOUSE to "Casa",
    UnitType.COMMERCIAL_ROOM to "Sala comercial",
    UnitType.BUILDING to "Prédio",
    UnitType.KITNET to "Kitnet",
    UnitType.OTHER to "Outro"
)
private val unitTypeIcons = mapOf(
    UnitType.APARTMENT to Icons.Rounded.Apartment,
    UnitType.HOUSE to Icons.Rounded.Home,
    UnitType.COMMERCIAL_ROOM to Icons.Rounded.Store,
    UnitType.BUILDING to Icons.Rounded.Domain,
    UnitType.KITNET to Icons.Rounded.MeetingRoom,
    UnitType.OTHER to Icons.Rounded.OtherHouses
)
private val capacityKindLabels = mapOf(
    CapacityKind.TENANTS to "Inquilinos",
    CapacityKind.ROOMS to "Quartos",
    CapacityKind.SPACES to "Espaços"
)
private val statusLabels = mapOf(
    OperationalStatus.ACTIVE to "Ativa",
    OperationalStatus.INACTIVE to "Inativa",
    OperationalStatus.MAINTENANCE to "Em manutenção"
)

@Composable
fun UnitFormBottomSheet(
    unit: RentalUnit? = null,
    initialName: String? = null,
    onDismiss: () -> Unit,
    onSave: (RentalUnit, Boolean) -> Unit,   // (unit, isNew)
    onDelete: ((RentalUnit) -> Unit)? = null
) {
    val isNew = unit == null
    // "Geral" and other legacy unit names do not yet have a RentalUnit record.
    // They are created on save, but the user is still editing an existing unit
    // from their point of view, so the sheet should use the edit affordance.
    val isLegacyUnit = isNew && !initialName.isNullOrBlank()
    var name by remember(unit, initialName) { mutableStateOf(unit?.name ?: initialName.orEmpty()) }
    var type by remember(unit) { mutableStateOf(unit?.type ?: UnitType.OTHER) }
    var location by remember(unit) { mutableStateOf(unit?.location.orEmpty()) }
    var capacityText by remember(unit) { mutableStateOf(unit?.capacity?.toString() ?: "1") }
    var capacityKind by remember(unit) { mutableStateOf(unit?.capacityKind ?: CapacityKind.TENANTS) }
    var status by remember(unit) { mutableStateOf(unit?.operationalStatus ?: OperationalStatus.ACTIVE) }
    var condFeeText by remember(unit) { mutableStateOf(unit?.condominiumFee?.let(::moneyInputText).orEmpty()) }
    var notes by remember(unit) { mutableStateOf(unit?.notes.orEmpty()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var capacityError by remember { mutableStateOf<String?>(null) }

    val nameFocus = remember { FocusRequester() }
    val locationFocus = remember { FocusRequester() }
    val capacityFocus = remember { FocusRequester() }
    val feeFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var feeError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val saveAction: () -> Unit = saveAction@{
        val nextNameError = if (name.isBlank()) "Informe o nome da unidade" else null
        val nextLocationError = if (location.isBlank()) "Informe o endereço" else null
        val parsedCapacity = capacityText.toIntOrNull()
        val nextCapacityError = if (parsedCapacity == null || parsedCapacity < 1) "Mínimo 1" else null

        nameError = nextNameError
        locationError = nextLocationError
        capacityError = nextCapacityError
        feeError = if (condFeeText.isNotBlank() && !validMoney(condFeeText, allowZero = true)) "Informe um valor a partir de zero, com até 2 casas decimais" else null
        val invalid = when {
            nextNameError != null -> nameFocus
            nextLocationError != null -> locationFocus
            nextCapacityError != null -> capacityFocus
            feeError != null -> feeFocus
            else -> null
        }
        if (invalid != null) { invalid.requestFocus(); keyboard?.show(); return@saveAction }

        val now = Instant.now().toString()
        val saved = RentalUnit(
            id = unit?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            iconKey = unitTypeIcons[type]?.name,
            photoPath = unit?.photoPath,
            location = location.trim(),
            capacity = parsedCapacity!!,
            capacityKind = capacityKind,
            operationalStatus = status,
            condominiumFee = condFeeText.replace(',', '.').toDoubleOrNull(),
            notes = notes.trim(),
            createdAt = unit?.createdAt ?: now,
            updatedAt = now
        )
        onSave(saved, isNew)
    }

    NaniEditor(onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize().imePadding()) {
            EditorHeader(if (isNew && !isLegacyUnit) "Nova unidade" else "Editar unidade", unit?.name ?: initialName, onDismiss)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpace.page).padding(top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpace.section)
            ) {
                Text("Campos com * são obrigatórios.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppFormSection(title = "Identificação", subtitle = "Nome e tipo da unidade", icon = Icons.Rounded.Domain) {
                    NaniTextField(name, { name = it; nameError = null }, "Nome da unidade",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        isError = nameError != null, errorMessage = nameError, required = true, focusRequester = nameFocus)
                    NaniDropdownField("Tipo", unitTypeLabels.values.toList(), unitTypeLabels[type] ?: "Outro", { selected ->
                        type = unitTypeLabels.entries.find { it.value == selected }?.key ?: UnitType.OTHER
                    })
                }
                AppFormSection(title = "Localização e capacidade", subtitle = "Endereço e limite de ocupação", icon = Icons.Rounded.LocationOn) {
                    NaniTextField(location, { location = it; locationError = null }, "Endereço",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        isError = locationError != null, errorMessage = locationError, required = true, focusRequester = locationFocus)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        NaniTextField(capacityText, { value ->
                                if (value.matches(Regex("^\\d*$"))) { capacityText = value.take(3); capacityError = null }
                            }, "Capacidade", modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            isError = capacityError != null, errorMessage = capacityError, required = true, focusRequester = capacityFocus)
                        NaniDropdownField("Tipo de capacidade", capacityKindLabels.values.toList(),
                            capacityKindLabels[capacityKind] ?: "Inquilinos", { selected ->
                                capacityKind = capacityKindLabels.entries.find { it.value == selected }?.key ?: CapacityKind.TENANTS
                            }, modifier = Modifier.weight(1f))
                    }
                }
                AppFormSection(title = "Situação", subtitle = "Se a unidade está recebendo inquilinos", icon = Icons.Rounded.ToggleOn) {
                    NaniDropdownField("Status", statusLabels.values.toList(), statusLabels[status] ?: "Ativa", { selected ->
                        status = statusLabels.entries.find { it.value == selected }?.key ?: OperationalStatus.ACTIVE
                    })
                }
                AppFormSection(title = "Informações adicionais", subtitle = "Condomínio e observações, se houver", icon = Icons.AutoMirrored.Rounded.Notes) {
                    NaniTextField(condFeeText, { value ->
                            if (value.matches(Regex("^\\d*[.,]?\\d*$"))) { condFeeText = value; feeError = null }
                        }, "Condomínio mensal (R\$)", helperText = "Exemplo: 250,00",
                        isError = feeError != null, errorMessage = feeError, focusRequester = feeFocus,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    NaniTextField(notes, { notes = it }, "Observações",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = false, maxLines = 4)
                }
                if (unit != null && onDelete != null) {
                    SecondaryButton("Excluir unidade", { showDeleteConfirm = true }, icon = Icons.Rounded.DeleteOutline)
                }
            }
            AppFormFooter(onCancel = onDismiss, onSave = saveAction)
        }
    }

    if (showDeleteConfirm && unit != null && onDelete != null) {
        val hasTenants = unit.tenantCount > 0
        NaniConfirmDialog(
            title = if (hasTenants) "Unidade com inquilinos" else "Excluir unidade?",
            onDismiss = { showDeleteConfirm = false },
            confirmLabel = if (hasTenants) "Entendi" else "Excluir",
            onConfirm = { showDeleteConfirm = false; if (!hasTenants) onDelete(unit) },
            text = {
                DialogText(if (hasTenants) {
                    "A unidade ${unit.name} tem ${unit.tenantCount} ${if (unit.tenantCount == 1) "inquilino vinculado" else "inquilinos vinculados"}. Transfira ou remova esses cadastros antes de excluir a unidade."
                } else "A unidade ${unit.name} será excluída permanentemente.")
            },
            dismissLabel = if (hasTenants) null else "Cancelar",
            destructive = !hasTenants,
            icon = Icons.Rounded.DeleteOutline
        )
    }
}
