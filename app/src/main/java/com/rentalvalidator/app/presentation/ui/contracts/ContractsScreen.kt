package com.rentalvalidator.app.presentation.ui.contracts

import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.*
import com.rentalvalidator.app.presentation.viewmodel.ContractsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel
import com.rentalvalidator.app.util.ContractStatus
import com.rentalvalidator.app.util.getContractStatus
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContractsScreen(
    viewModel: ContractsViewModel = hiltViewModel(),
    unitsViewModel: UnitsViewModel = hiltViewModel()
) {
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val units by unitsViewModel.units.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showSnackbar = rememberAppSnackbar()
    LaunchedEffect(viewModel) { viewModel.errors.collect { showSnackbar(it) } }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<ContractStatus?>(null) }
    var unitFilter by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var pickerTenant by remember { mutableStateOf<String?>(null) }
    var pickerType by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val tenantId = pickerTenant; val type = pickerType
        if (uri != null && tenantId != null && type != null) {
            viewModel.attach(tenantId, if (type == "contract") "contractPath" else "inspectionPath", uri) { showSnackbar("Documento anexado") }
        }
        pickerTenant = null; pickerType = null
    }
    val visible = tenants.filter {
        it.name.contains(query, true) && (unitFilter == null || it.unit == unitFilter) &&
            (statusFilter == null || getContractStatus(it.contractExpirationDate, it.contractPath.isNotBlank()).status == statusFilter)
    }
    val availableUnitNames = (units.map { it.name } + tenants.map { it.unit.ifBlank { "Geral" } })
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    if(showSearch) NaniSearchDialog(query,{query=it},{showSearch=false})
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = FloatingNavigationContentClearance)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            AppScreenHeader(stringResource(R.string.documents), action = { Row { NaniSearchButton(query) {showSearch=true}; IconButton(onClick = { showFilters = true }) { BadgedBox(badge = { if (statusFilter != null || unitFilter != null) Badge() }) { Icon(Icons.Rounded.Tune, "Filtros") } } } })
            NaniSummaryStrip("Com contrato",tenants.count{it.contractPath.isNotBlank()},Icons.Rounded.TaskAlt,
                "Sem contrato",tenants.count{it.contractPath.isBlank()},Icons.Rounded.Description,
                Modifier.padding(horizontal=AppSpace.page,vertical=12.dp),
                firstKind = StatusKind.SUCCESS, secondKind = StatusKind.NEUTRAL, tonal = true)
            }
        }
        if (visible.isEmpty()) item {
            AppEmptyState(
                icon = Icons.Rounded.Description,
                title = if (tenants.isEmpty()) "Nenhum contrato para acompanhar" else "Nenhum contrato encontrado",
                message = if (tenants.isEmpty()) {
                    "Quando houver inquilinos cadastrados, os contratos e vistorias aparecerão aqui."
                } else {
                    "Ajuste a busca ou os filtros para visualizar outros registros."
                }
            )
        }
        visible.groupBy { it.unit.ifBlank { "Geral" } }.forEach { (unit, unitTenants) ->
            item { SectionHeader(unit, "${unitTenants.size} inquilinos", Icons.Rounded.Apartment, Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp)) }
            items(unitTenants, key = { it.id }) { tenant ->
                ContractCard(
                    tenant = tenant,
                    onDate = { viewModel.updateExpirationDate(tenant.id, it) },
                    onAttachContract = { pickerTenant = tenant.id; pickerType = "contract"; picker.launch(arrayOf("application/pdf")) },
                    onAttachInspection = { pickerTenant = tenant.id; pickerType = "inspection"; picker.launch(arrayOf("application/pdf")) },
                    onOpenContract = { openFile(context, tenant.contractPath, showSnackbar) }, onOpenInspection = { openFile(context, tenant.inspectionPath, showSnackbar) },
                    onRemoveContract = { viewModel.remove(tenant.id, "contractPath") },
                    onRemoveInspection = { viewModel.remove(tenant.id, "inspectionPath") }
                )
            }
        }
    }
    if (showFilters) ContractFilterBottomSheet(statusFilter, unitFilter, availableUnitNames, { showFilters = false }) { status, unit -> statusFilter = status; unitFilter = unit; showFilters = false }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ContractCard(tenant: Tenant, onDate: (String) -> Unit, onAttachContract: () -> Unit, onAttachInspection: () -> Unit, onOpenContract: () -> Unit, onOpenInspection: () -> Unit, onRemoveContract: () -> Unit, onRemoveInspection: () -> Unit) {
    val info = getContractStatus(tenant.contractExpirationDate, tenant.contractPath.isNotBlank())
    var showDatePicker by remember { mutableStateOf(false) }
    var deleteType by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Surface(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page,vertical=6.dp).premiumShadow(shape),
        shape=shape,color=MaterialTheme.colorScheme.surface,border=surfaceDepthBorder()) {
        Column {
            Column(Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expandido" else "Recolhido" }
                .clickable(role = Role.Button) {expanded=!expanded}
                .background(Brush.linearGradient(listOf(lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, .16f), MaterialTheme.colorScheme.surface)))
                .padding(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    InitialsAvatar(tenant.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(tenant.name, style=MaterialTheme.typography.titleMedium)
                        Text(
                            if (tenant.contractExpirationDate.isBlank()) "Vencimento não informado"
                            else "Vence em ${displayDate(tenant.contractExpirationDate, "Não informado")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(5.dp))
                        StatusBadge(info.label, when (info.status) {
                            ContractStatus.VIGENTE -> StatusKind.SUCCESS
                            ContractStatus.VENCE_EM_BREVE, ContractStatus.DATA_PENDENTE -> StatusKind.WARNING
                            ContractStatus.VENCIDO -> StatusKind.ERROR
                            ContractStatus.SEM_CONTRATO -> StatusKind.NEUTRAL
                        })
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(if(expanded)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,"Documentos de ${tenant.name}")
                }
            }
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column(Modifier.padding(horizontal=18.dp).padding(bottom=12.dp)) {
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    com.rentalvalidator.app.presentation.design.NaniActionRow("Vencimento do contrato",com.rentalvalidator.app.presentation.design.displayDate(tenant.contractExpirationDate,"Adicionar data"),Icons.Rounded.CalendarMonth,{showDatePicker=true})
                    DocumentButton("Contrato",tenant.contractPath.isNotBlank(),Icons.Rounded.Description,Modifier.fillMaxWidth(),onAttachContract,onOpenContract){deleteType="Contrato"}
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
                    DocumentButton("Vistoria",tenant.inspectionPath.isNotBlank(),Icons.Rounded.PhotoCamera,Modifier.fillMaxWidth(),onAttachInspection,onOpenInspection){deleteType="Vistoria"}
                }
            }
        }
    }
    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { millis -> onDate(com.rentalvalidator.app.util.DateUtils.fromDatePickerUtc(millis).format(DateTimeFormatter.ISO_DATE)) }; showDatePicker = false }) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state) }
    }
    deleteType?.let { type -> AlertDialog(onDismissRequest = { deleteType = null }, title = { Text("Remover $type?") }, text = { Text("O arquivo será removido do aplicativo.") }, confirmButton = { TextButton(onClick = { if (type == "Contrato") onRemoveContract() else onRemoveInspection(); deleteType = null }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { deleteType = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable
private fun DocumentButton(label: String, hasFile: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onAttach: () -> Unit, onOpen: () -> Unit, onRemove: () -> Unit) {
    val attachedColors = semanticPalette(StatusKind.SUCCESS)
    Row(modifier.heightIn(min = 76.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(
            if (hasFile) attachedColors.background else MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (hasFile) attachedColors.foreground else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(if (hasFile) "Anexado" else "Sem arquivo", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(
            onClick = if (hasFile) onOpen else onAttach,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) { Text(if (hasFile) "Abrir" else "Adicionar", style = MaterialTheme.typography.labelLarge) }
        if (hasFile) IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Delete, "Remover $label", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}


private fun openFile(context: Context, path: String, showSnackbar: (String) -> Unit) {
    try {
        val file = File(path); if (!file.exists()) { showSnackbar("Arquivo não encontrado"); return }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Abrir com"))
    } catch (_: Exception) { showSnackbar("Nenhum leitor de PDF encontrado") }
}

