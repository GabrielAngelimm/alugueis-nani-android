package com.rentalvalidator.app.presentation.ui.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.viewmodel.ContractsViewModel
import com.rentalvalidator.app.presentation.viewmodel.UnitsViewModel
import com.rentalvalidator.app.util.ContractStatus
import com.rentalvalidator.app.util.DateUtils
import com.rentalvalidator.app.util.getContractStatus
import java.io.File
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun ContractStatus.label() = when (this) {
    ContractStatus.VIGENTE -> "Vigente"
    ContractStatus.VENCE_EM_BREVE -> "Vence em breve"
    ContractStatus.VENCIDO -> "Vencido"
    ContractStatus.DATA_PENDENTE -> "Data pendente"
    ContractStatus.SEM_CONTRATO -> "Sem contrato"
}

private fun ContractStatus.kind() = when (this) {
    ContractStatus.VIGENTE -> StatusKind.SUCCESS
    ContractStatus.VENCE_EM_BREVE, ContractStatus.DATA_PENDENTE -> StatusKind.WARNING
    ContractStatus.VENCIDO -> StatusKind.ERROR
    ContractStatus.SEM_CONTRATO -> StatusKind.NEUTRAL
}

@OptIn(ExperimentalFoundationApi::class)
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
    val statuses = tenants.associate { it.id to getContractStatus(it.contractExpirationDate, it.contractPath.isNotBlank()).status }
    val visible = tenants.filter {
        it.name.contains(query, true) && (unitFilter == null || it.unit == unitFilter) &&
            (statusFilter == null || statuses[it.id] == statusFilter)
    }
    val availableUnitNames = (units.map { it.name } + tenants.map { it.unit.ifBlank { "Geral" } })
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    if (showSearch) NaniSearchDialog(query, { query = it }, { showSearch = false })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalNavigationClearance.current)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                NaniHeader(stringResource(R.string.documents)) {
                    NaniSearchButton(query) { showSearch = true }
                    IconButton(onClick = { showFilters = true }) {
                        BadgedBox(badge = { if (statusFilter != null || unitFilter != null) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                            Icon(Icons.Rounded.Tune, "Filtros")
                        }
                    }
                }
                if (tenants.isNotEmpty()) {
                    val all = tenants.map { statuses[it.id] ?: ContractStatus.SEM_CONTRATO }
                    val attached = all.count { it != ContractStatus.SEM_CONTRATO }
                    val current = all.count { it == ContractStatus.VIGENTE }
                    val review = all.count { it == ContractStatus.VENCE_EM_BREVE || it == ContractStatus.DATA_PENDENTE }
                    val expired = all.count { it == ContractStatus.VENCIDO }
                    // Legend and ring come from one list; "sem contrato" counts toward the whole but stays as track.
                    val stats = listOf(
                        StatCount(current, if (current == 1) "vigente" else "vigentes", ringColor(StatusKind.SUCCESS)),
                        StatCount(review, "a revisar", ringColor(StatusKind.WARNING)),
                        StatCount(expired, if (expired == 1) "vencido" else "vencidos", ringColor(StatusKind.ERROR)),
                        StatCount(all.size - attached, "sem contrato", ringColor(StatusKind.NEUTRAL), hollow = true)
                    )
                    LedgerSheet(Modifier.padding(horizontal = AppSpace.page).padding(bottom = 12.dp)) {
                        Row(Modifier.padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusRing(ringOf(stats), "$attached de ${tenants.size} inquilinos com contrato anexado",
                                size = 64.dp, stroke = 7.dp) { RingLabel("$attached/${tenants.size}", 64.dp) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$attached de ${tenants.size} com contrato anexado", style = MaterialTheme.typography.titleSmall)
                                RingLegend(stats)
                            }
                        }
                    }
                }
            }
        }
        if (visible.isEmpty()) item {
            AppEmptyState(
                icon = Icons.Rounded.Description,
                title = if (tenants.isEmpty()) "Nenhum contrato para acompanhar" else "Nenhum contrato encontrado",
                message = if (tenants.isEmpty()) "Quando houver inquilinos cadastrados, os contratos e vistorias aparecerão aqui."
                else "Ajuste a busca ou os filtros para ver outros registros."
            )
        }
        visible.groupBy { it.unit.ifBlank { "Geral" } }.forEach { (unit, unitTenants) ->
            item(key = "unit-$unit") {
                SectionTitle(unit, Modifier.padding(top = 8.dp, bottom = 8.dp),
                    detail = "${unitTenants.size} ${if (unitTenants.size == 1) "inquilino" else "inquilinos"}")
            }
            items(unitTenants, key = { it.id }) { tenant ->
                ContractCard(
                    tenant = tenant,
                    onDate = { viewModel.updateExpirationDate(tenant.id, it) },
                    onAttachContract = { pickerTenant = tenant.id; pickerType = "contract"; picker.launch(arrayOf("application/pdf")) },
                    onAttachInspection = { pickerTenant = tenant.id; pickerType = "inspection"; picker.launch(arrayOf("application/pdf")) },
                    onOpenContract = { openFile(context, tenant.contractPath, showSnackbar) },
                    onOpenInspection = { openFile(context, tenant.inspectionPath, showSnackbar) },
                    onRemoveContract = { viewModel.remove(tenant.id, "contractPath") },
                    onRemoveInspection = { viewModel.remove(tenant.id, "inspectionPath") }
                )
            }
        }
    }
    if (showFilters) ContractFilterBottomSheet(statusFilter, unitFilter, availableUnitNames, { showFilters = false }) { status, unit ->
        statusFilter = status; unitFilter = unit; showFilters = false
    }
}

/** A tenant's dossier: contract dates, the contract and the inspection report. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractCard(tenant: Tenant, onDate: (String) -> Unit, onAttachContract: () -> Unit, onAttachInspection: () -> Unit,
    onOpenContract: () -> Unit, onOpenInspection: () -> Unit, onRemoveContract: () -> Unit, onRemoveInspection: () -> Unit) {
    val info = getContractStatus(tenant.contractExpirationDate, tenant.contractPath.isNotBlank())
    var showDatePicker by remember { mutableStateOf(false) }
    var deleteType by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, AppMotion.StateFloat, label = "dossier expand")
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 5.dp),
        shape = RoundedCornerShape(AppSize.sheetRadius), color = colors.surface) {
        Column {
            Row(Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expandido" else "Recolhido" }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Monogram(tenant.name)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(tenant.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (tenant.contractExpirationDate.isBlank()) "Vencimento não informado"
                        else "Vence em ${displayDate(tenant.contractExpirationDate, "Não informado")}",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    StatusMark(info.status.label(), info.status.kind())
                }
                Icon(Icons.Rounded.ExpandMore, "Documentos de ${tenant.name}", Modifier.rotate(rotation), tint = colors.onSurfaceVariant)
            }
            AnimatedVisibility(expanded,
                enter = expandVertically(AppMotion.ExpandVertically, expandFrom = Alignment.Top) + fadeIn(AppMotion.EnterFade),
                exit = shrinkVertically(AppMotion.CollapseVertically, shrinkTowards = Alignment.Top) + fadeOut(AppMotion.ExitFade)) {
                Column(Modifier.padding(bottom = 6.dp)) {
                    LedgerRule()
                    NaniActionRow("Vencimento do contrato", displayDate(tenant.contractExpirationDate, "Adicionar data"),
                        Icons.Rounded.EditCalendar, { showDatePicker = true })
                    LedgerRule(Modifier.padding(start = 70.dp))
                    DocumentRow("Contrato", tenant.contractPath.isNotBlank(), Icons.Rounded.Description, onAttachContract, onOpenContract) { deleteType = "Contrato" }
                    LedgerRule(Modifier.padding(start = 70.dp))
                    DocumentRow("Vistoria", tenant.inspectionPath.isNotBlank(), Icons.AutoMirrored.Rounded.FactCheck, onAttachInspection, onOpenInspection) { deleteType = "Vistoria" }
                }
            }
        }
    }
    if (showDatePicker) {
        val initial = DateUtils.parseDate(tenant.contractExpirationDate)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis -> onDate(DateUtils.fromDatePickerUtc(millis).format(DateTimeFormatter.ISO_DATE)) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
            shape = RoundedCornerShape(AppSize.modalRadius),
            colors = DatePickerDefaults.colors(containerColor = colors.surface)
        ) { DatePicker(state, colors = DatePickerDefaults.colors(containerColor = colors.surface)) }
    }
    deleteType?.let { type ->
        NaniConfirmDialog(
            title = "Remover ${type.lowercase()}?",
            onDismiss = { deleteType = null },
            confirmLabel = stringResource(R.string.remove),
            onConfirm = { if (type == "Contrato") onRemoveContract() else onRemoveInspection(); deleteType = null },
            text = { DialogText("O arquivo será removido do aplicativo. A cópia que você tiver em outro lugar não é afetada.") },
            destructive = true,
            icon = Icons.Rounded.DeleteOutline
        )
    }
}

@Composable
private fun DocumentRow(label: String, hasFile: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAttach: () -> Unit, onOpen: () -> Unit, onRemove: () -> Unit) {
    val attached = statusTone(StatusKind.SUCCESS)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon, tint = if (hasFile) attached.ink else MaterialTheme.colorScheme.onSurfaceVariant,
            container = if (hasFile) attached.fill else MaterialTheme.colorScheme.surfaceContainer)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(if (hasFile) "PDF anexado" else "Nenhum arquivo", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = if (hasFile) onOpen else onAttach, shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Text(if (hasFile) "Abrir" else "Anexar", style = MaterialTheme.typography.labelLarge)
        }
        if (hasFile) IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.DeleteOutline, "Remover $label", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
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
