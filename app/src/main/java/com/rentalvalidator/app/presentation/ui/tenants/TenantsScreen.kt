package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.theme.*
import com.rentalvalidator.app.presentation.viewmodel.RentReminderViewModel
import com.rentalvalidator.app.presentation.viewmodel.TenantsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OccupancyStatus
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.util.CurrencyUtils
import com.rentalvalidator.app.util.CpfUtils
import com.rentalvalidator.app.util.PhoneUtils
import com.rentalvalidator.app.util.WhatsAppUtils
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal enum class TenantViewMode { UNITS, ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantsScreen(
    viewModel: TenantsViewModel = hiltViewModel(),
    unitsViewModel: UnitsViewModel = hiltViewModel(),
    onNavigateToAddTenant: () -> Unit,
    onNavigateToEditTenant: (String) -> Unit,
    onNavigateToPayments: () -> Unit = {},
    onNavigateToTenantPayments: ((String) -> Unit)? = null,
    reminderViewModel: RentReminderViewModel? = null
) {
    val context = LocalContext.current
    val showSnackbar = rememberAppSnackbar()
    LaunchedEffect(viewModel) { viewModel.errors.collect { showSnackbar(it) } }
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val units by unitsViewModel.units.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(TenantViewMode.UNITS) }
    var query by remember { mutableStateOf("") }
    
    // selectedUnitName keeps the selected unit text identifier to find the right RentalUnit
    var selectedUnitName by rememberSaveable { mutableStateOf<String?>(null) }
    
    var detailTenantId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailTenant = tenants.firstOrNull { it.id == detailTenantId }
    var showReminder by rememberSaveable { mutableStateOf(false) }
    var showContact by rememberSaveable { mutableStateOf(false) }
    var formTenant by remember { mutableStateOf<Tenant?>(null) }
    var formInitialUnit by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var formViewOnly by remember { mutableStateOf(false) }
    var deleteTenant by remember { mutableStateOf<Tenant?>(null) }
    
    var formUnit by remember { mutableStateOf<RentalUnit?>(null) }
    var formUnitInitialName by remember { mutableStateOf<String?>(null) }
    var showUnitForm by remember { mutableStateOf(false) }
    
    var showFilters by remember { mutableStateOf(false) }
    var filterBank by remember { mutableStateOf<String?>(null) }
    var filterUnit by remember { mutableStateOf<String?>(null) }
    var filterDueDay by remember { mutableStateOf<Int?>(null) }
    var showUnitTenantPicker by remember { mutableStateOf(false) }

    val visibleTenants = tenants.filter { tenant ->
        (tenant.name.contains(query, true) || tenant.aliases.any { it.contains(query, true) }) &&
            (filterBank == null || tenant.bank == filterBank) &&
            (filterUnit == null || tenant.unit == filterUnit) &&
            (filterDueDay == null || tenant.dueDay == filterDueDay)
    }

    when {
        detailTenant != null -> {
            val tenant = detailTenant!!
            val reminderVm = reminderViewModel ?: hiltViewModel<RentReminderViewModel>(key = "rent-reminder-${tenant.id}")
            val reminderState by reminderVm.state.collectAsStateWithLifecycle()
            LaunchedEffect(tenant.id) { reminderVm.load(tenant.id) }
            TenantDetails(
                tenant = tenant,
                onBack = { detailTenantId = null },
                onEdit = { formTenant = detailTenant; formViewOnly = false; showForm = true },
                onDelete = { deleteTenant = detailTenant },
                onPayments = { onNavigateToTenantPayments?.invoke(tenant.id) ?: onNavigateToPayments() },
                onReminder = { showReminder = true },
                onContact = { showContact = true },
                reminderLeadHours = reminderState.reminder?.leadHours
            )
            if (showReminder) TenantReminderSheet(tenant, { showReminder = false }, vm = reminderVm)
            if (showContact) TenantContactSheet(tenant, { showContact = false }, {
                formTenant = tenant; formViewOnly = false; showForm = true
            })
        }
        selectedUnitName != null -> {
            // Find real unit or null if legacy
            val realUnit = units.find { it.name == selectedUnitName }
            UnitDetails(
                unitName = selectedUnitName!!,
                realUnit = realUnit,
                tenants = tenants.filter { it.unit.ifBlank { "Geral" } == selectedUnitName },
                onBack = { selectedUnitName = null },
                onTenant = { detailTenantId = it.id },
                onEdit = {
                    val linkedTenants = tenants.filter { it.unit.ifBlank { "Geral" } == selectedUnitName }
                    when (linkedTenants.size) {
                        0 -> Unit
                        1 -> {
                            formTenant = linkedTenants.first()
                            formInitialUnit = selectedUnitName
                            formViewOnly = false
                            showForm = true
                        }
                        else -> showUnitTenantPicker = true
                    }
                },
                onAdd = {
                    formTenant = null
                    formInitialUnit = selectedUnitName
                    formViewOnly = false
                    showForm = true
                },
                onDeleteUnit = {
                    realUnit?.let { deleting -> unitsViewModel.deleteUnit(deleting.id,
                        onSuccess = { selectedUnitName = null }, onError = { showSnackbar(it) }) }
                },
                onEditUnit = {
                    formUnit = realUnit
                    formUnitInitialName = realUnit?.name ?: selectedUnitName
                    showUnitForm = true
                }
            )
        }
        else -> TenantsOverview(
            tenants = visibleTenants,
            units = units,
            mode = mode,
            query = query,
            hasFilters = filterBank != null || filterUnit != null || filterDueDay != null,
            onQuery = { query = it },
            onMode = { mode = it },
            onUnit = { selectedUnitName = it },
            onTenant = { detailTenantId = it.id },
            onFilters = { showFilters = true },
            onAdd = {
                if(mode==TenantViewMode.UNITS) {formUnit=null;formUnitInitialName=null;showUnitForm=true}
                else {formTenant=null;formInitialUnit=null;formViewOnly=false;showForm=true}
            }
        )
    }

    if (showForm) {
        TenantFormBottomSheet(
            tenant = formTenant,
            initialIsViewOnly = formViewOnly,
            initialUnit = formInitialUnit,
            unitOptions = units,
            onDismiss = { showForm = false; formInitialUnit = null },
            onSave = { saved ->
                val onSuccess = {
                    if (formTenant != null && selectedUnitName != null && saved.unit.ifBlank { "Geral" } != selectedUnitName) {
                        selectedUnitName = saved.unit.ifBlank { "Geral" }
                    }
                    showForm = false
                    formInitialUnit = null
                }
                if (formTenant == null) viewModel.addTenant(saved, onSuccess) else viewModel.updateTenant(saved, onSuccess)
            }
        )
    }
    
    if (showUnitForm) {
        UnitFormBottomSheet(
            unit = formUnit,
            initialName = formUnitInitialName,
            onDismiss = { showUnitForm = false; formUnit = null; formUnitInitialName = null },
            onSave = { saved, isNew ->
                unitsViewModel.saveUnit(
                    unit = saved,
                    isNew = isNew,
                    onSuccess = {
                        val previousName = formUnit?.name ?: formUnitInitialName
                        if (isNew && previousName != null) {
                            tenants
                                .filter { it.unit.ifBlank { RentalUnit.GERAL_NAME } == previousName }
                                .forEach { linkedTenant ->
                                    viewModel.updateTenant(linkedTenant.copy(unit = saved.name, unitId = saved.id))
                                }
                        }
                        if (selectedUnitName == previousName) {
                            selectedUnitName = saved.name
                        }
                        showUnitForm = false
                        formUnit = null
                        formUnitInitialName = null
                    },
                    onError = { message ->
                        showSnackbar(message)
                    }
                )
            },
            onDelete = { unitToDelete ->
                unitsViewModel.deleteUnit(
                    unitId = unitToDelete.id,
                    onSuccess = {
                        if (selectedUnitName == unitToDelete.name) {
                            selectedUnitName = null // go back to overview
                        }
                        showUnitForm = false
                        formUnit = null
                        formUnitInitialName = null
                    },
                    onError = { message ->
                        showSnackbar(message)
                    }
                )
            }
        )
    }

    if (showUnitTenantPicker) {
        UnitTenantPicker(
            unit = selectedUnitName.orEmpty(),
            tenants = tenants.filter { it.unit.ifBlank { "Geral" } == selectedUnitName },
            onDismiss = { showUnitTenantPicker = false },
            onTenant = {
                formTenant = it
                formInitialUnit = selectedUnitName
                formViewOnly = false
                showUnitTenantPicker = false
                showForm = true
            }
        )
    }
    if (showFilters) {
        TenantFilterBottomSheet(filterBank, filterUnit, filterDueDay, units.map { it.name }, { showFilters = false }) { bank, unit, day ->
            filterBank = bank; filterUnit = unit; filterDueDay = day; showFilters = false
        }
    }
    deleteTenant?.let { tenant ->
        AlertDialog(
            onDismissRequest = { deleteTenant = null },
            icon = {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(25.dp)
                    )
                }
            },
            title = { Text("Excluir inquilino?") },
            text = {
                Text(
                    "${tenant.name} e todos os pagamentos vinculados serão removidos permanentemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTenant(tenant) { deleteTenant = null; detailTenantId = null } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteTenant = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

