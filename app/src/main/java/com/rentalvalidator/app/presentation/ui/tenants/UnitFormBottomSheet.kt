package com.rentalvalidator.app.presentation.ui.tenants
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.theme.AppSpace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.presentation.components.AppFormSection
import com.rentalvalidator.app.presentation.components.AppFormFooter
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.ModernDropdownMenu
import com.rentalvalidator.app.presentation.components.ModernTextField
import com.rentalvalidator.app.presentation.components.premiumShadow
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var condFeeText by remember(unit) { mutableStateOf(unit?.condominiumFee?.toString().orEmpty()) }
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

    com.rentalvalidator.app.presentation.design.NaniEditor(onDismiss = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(Modifier.fillMaxSize().imePadding()) {
                UnitFormHeader(
                    isNew = isNew && !isLegacyUnit,
                    unitName = unit?.name ?: initialName,
                    onDismiss = onDismiss
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppSpace.page, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.spacious)
                ) {
                    // --- Identificação ---
                    Text("* Obrigatório. Os demais campos são opcionais ou já possuem uma opção selecionada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppFormSection(
                        title = "Identificação",
                        subtitle = "Nome e tipo da unidade",
                        icon = Icons.Rounded.Domain
                    ) {
                        ModernTextField(
                            name,
                            { name = it; nameError = null },
                            "Nome da unidade",
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            isError = nameError != null,
                            errorMessage = nameError,
                            required = true, focusRequester = nameFocus
                        )
                        ModernDropdownMenu(
                            "Tipo",
                            unitTypeLabels.values.toList(),
                            unitTypeLabels[type] ?: "Outro",
                            { selected ->
                                type = unitTypeLabels.entries.find { it.value == selected }?.key ?: UnitType.OTHER
                            }
                        )
                    }

                    // --- Localização e capacidade ---
                    AppFormSection(
                        title = "Localização e capacidade",
                        subtitle = "Endereço e limite de ocupação",
                        icon = Icons.Rounded.LocationOn
                    ) {
                        ModernTextField(
                            location,
                            { location = it; locationError = null },
                            "Endereço",
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            isError = locationError != null,
                            errorMessage = locationError,
                            required = true, focusRequester = locationFocus
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ModernTextField(
                                capacityText,
                                { value ->
                                    if (value.matches(Regex("^\\d*$"))) {
                                        capacityText = value.take(3)
                                        capacityError = null
                                    }
                                },
                                "Capacidade",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                isError = capacityError != null,
                                errorMessage = capacityError,
                            required = true, focusRequester = capacityFocus
                            )
                            ModernDropdownMenu(
                                "Tipo de capacidade",
                                capacityKindLabels.values.toList(),
                            capacityKindLabels[capacityKind] ?: "Inquilinos",
                                { selected ->
                                    capacityKind = capacityKindLabels.entries.find { it.value == selected }?.key ?: CapacityKind.TENANTS
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- Status operacional ---
                    AppFormSection(
                        title = "Situação",
                        subtitle = "Status operacional da unidade",
                        icon = Icons.Rounded.ToggleOn
                    ) {
                        ModernDropdownMenu(
                            "Status",
                            statusLabels.values.toList(),
                            statusLabels[status] ?: "Ativa",
                            { selected ->
                                status = statusLabels.entries.find { it.value == selected }?.key ?: OperationalStatus.ACTIVE
                            }
                        )
                    }

                    // --- Informações adicionais (opcionais) ---
                    AppFormSection(
                        title = "Informações adicionais",
                        subtitle = "Condomínio e observações (opcionais)",
                        icon = Icons.Rounded.Notes
                    ) {
                        ModernTextField(
                            condFeeText,
                            { value ->
                                if (value.matches(Regex("^\\d*[.,]?\\d*$"))) { condFeeText = value; feeError = null }
                            },
                            "Condomínio mensal (R\$)",
                            helperText = "Opcional · exemplo: 250,00",
                            isError = feeError != null, errorMessage = feeError, focusRequester = feeFocus,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        ModernTextField(
                            notes,
                            { notes = it },
                            "Observações",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = false,
                            maxLines = 4
                        )
                    }

                    if (!isNew && onDelete != null && unit != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Excluir unidade")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
                AppFormFooter(onCancel = onDismiss, onSave = saveAction)
            }
        }
    }

    if (showDeleteConfirm && unit != null && onDelete != null) {
        val hasTenants = unit.tenantCount > 0
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Box(
                    Modifier.size(52.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(25.dp))
                }
            },
            title = { Text(if (hasTenants) "Unidade com inquilinos" else "Excluir unidade?") },
            text = {
                Column {
                    Text(
                        if (hasTenants) {
                            "A unidade '${unit.name}' possui ${unit.tenantCount} inquilino(s) vinculado(s). Transfira ou remova esses cadastros antes de excluir a unidade."
                        } else {
                            "Deseja excluir permanentemente a unidade '${unit.name}'?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasTenants) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (hasTenants) {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Entendi") }
                } else {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(unit)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text("Excluir") }
                }
            },
            dismissButton = {
                if (!hasTenants) {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
                }
            }
        )
    }
}

