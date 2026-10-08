package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.DialogText
import com.rentalvalidator.app.presentation.design.NaniConfirmDialog
import com.rentalvalidator.app.presentation.viewmodel.RentReminderViewModel
import com.rentalvalidator.app.presentation.viewmodel.TenantsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel

internal enum class TenantViewMode { UNITS, ALL }

/** Which page of the rentals section is open; deeper pages slide in from the right. */
private sealed interface RentalPane {
    val depth: Int
    data object Overview : RentalPane { override val depth = 0 }
    data class UnitPage(val name: String) : RentalPane { override val depth = 1 }
    data class TenantPage(val id: String) : RentalPane { override val depth = 2 }
}

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
    val overviewState = rememberLazyListState()

    val visibleTenants = tenants.filter { tenant ->
        (tenant.name.contains(query, true) || tenant.aliases.any { it.contains(query, true) }) &&
            (filterBank == null || tenant.bank == filterBank) &&
            (filterUnit == null || tenant.unit == filterUnit) &&
            (filterDueDay == null || tenant.dueDay == filterDueDay)
    }

    val pane: RentalPane = when {
        detailTenant != null -> RentalPane.TenantPage(detailTenant.id)
        selectedUnitName != null -> RentalPane.UnitPage(selectedUnitName!!)
        else -> RentalPane.Overview
    }

    AnimatedContent(pane, Modifier.fillMaxSize(), label = "rentals page", transitionSpec = {
        val forward = targetState.depth > initialState.depth
        (slideInHorizontally(AppMotion.PageSlide) { if (forward) it / 5 else -it / 5 } + fadeIn(AppMotion.EnterFade)) togetherWith
            (slideOutHorizontally(AppMotion.PageSlide) { if (forward) -it / 8 else it / 8 } + fadeOut(AppMotion.ExitFade))
    }) { page ->
        when (page) {
            is RentalPane.TenantPage -> {
                val tenant = tenants.firstOrNull { it.id == page.id } ?: return@AnimatedContent
                val reminderVm = reminderViewModel ?: hiltViewModel<RentReminderViewModel>(key = "rent-reminder-${tenant.id}")
                val reminderState by reminderVm.state.collectAsStateWithLifecycle()
                LaunchedEffect(tenant.id) { reminderVm.load(tenant.id) }
                TenantDetails(
                    tenant = tenant,
                    onBack = { detailTenantId = null },
                    onEdit = { formTenant = tenant; formViewOnly = false; showForm = true },
                    onDelete = { deleteTenant = tenant },
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
            is RentalPane.UnitPage -> {
                val unitName = page.name
                // Find real unit or null if legacy
                val realUnit = units.find { it.name == unitName }
                val linkedTenants = tenants.filter { it.unit.ifBlank { "Geral" } == unitName }
                UnitDetails(
                    unitName = unitName,
                    realUnit = realUnit,
                    tenants = linkedTenants,
                    onBack = { selectedUnitName = null },
                    onTenant = { detailTenantId = it.id },
                    onEdit = {
                        when (linkedTenants.size) {
                            0 -> Unit
                            1 -> {
                                formTenant = linkedTenants.first()
                                formInitialUnit = unitName
                                formViewOnly = false
                                showForm = true
                            }
                            else -> showUnitTenantPicker = true
                        }
                    },
                    onAdd = {
                        formTenant = null
                        formInitialUnit = unitName
                        formViewOnly = false
                        showForm = true
                    },
                    onDeleteUnit = {
                        realUnit?.let { deleting -> unitsViewModel.deleteUnit(deleting.id,
                            onSuccess = { selectedUnitName = null }, onError = { showSnackbar(it) }) }
                    },
                    onEditUnit = {
                        formUnit = realUnit
                        formUnitInitialName = realUnit?.name ?: unitName
                        showUnitForm = true
                    }
                )
            }
            RentalPane.Overview -> TenantsOverview(
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
                    if (mode == TenantViewMode.UNITS) { formUnit = null; formUnitInitialName = null; showUnitForm = true }
                    else { formTenant = null; formInitialUnit = null; formViewOnly = false; showForm = true }
                },
                listState = overviewState
            )
        }
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
                    onError = { message -> showSnackbar(message) }
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
                    onError = { message -> showSnackbar(message) }
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
        NaniConfirmDialog(
            title = "Excluir inquilino?",
            onDismiss = { deleteTenant = null },
            confirmLabel = "Excluir",
            onConfirm = { viewModel.deleteTenant(tenant) { deleteTenant = null; detailTenantId = null } },
            text = { DialogText("${tenant.name} e todos os pagamentos vinculados serão removidos permanentemente.") },
            destructive = true,
            icon = Icons.Rounded.DeleteOutline
        )
    }
}
