package com.rentalvalidator.app.presentation.ui.tenants

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.R
import com.rentalvalidator.app.domain.model.*
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.LocalNavigationClearance
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TenantsOverview(tenants: List<Tenant>, units: List<RentalUnit>, mode: TenantViewMode,
    query: String, hasFilters: Boolean, onQuery: (String) -> Unit, onMode: (TenantViewMode) -> Unit,
    onUnit: (String) -> Unit, onTenant: (Tenant) -> Unit, onFilters: () -> Unit, onAdd: () -> Unit,
    listState: LazyListState = rememberLazyListState()) {
    var showSearch by remember { mutableStateOf(false) }
    if (showSearch) NaniSearchDialog(query, onQuery, { showSearch = false })
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp + LocalNavigationClearance.current)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                NaniHeader(stringResource(R.string.rentals)) {
                    NaniSearchButton(query) { showSearch = true }
                    IconButton(onClick = onFilters) {
                        BadgedBox(badge = { if (hasFilters) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                            Icon(Icons.Rounded.Tune, "Filtros")
                        }
                    }
                    FilledIconButton(onClick = onAdd, Modifier.padding(start = 4.dp, end = 8.dp),
                        shape = RoundedCornerShape(AppSize.controlRadius),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = NaniTheme.colors.action,
                            contentColor = NaniTheme.colors.onAction)) {
                        Icon(Icons.Rounded.Add, if (mode == TenantViewMode.UNITS) "Adicionar unidade" else "Adicionar inquilino")
                    }
                }
                NaniTabs(listOf("Unidades", "Inquilinos"), if (mode == TenantViewMode.UNITS) 0 else 1,
                    { onMode(if (it == 0) TenantViewMode.UNITS else TenantViewMode.ALL) },
                    Modifier.padding(horizontal = AppSpace.page).padding(top = 4.dp, bottom = 14.dp))
            }
        }
        if (mode == TenantViewMode.UNITS) {
            val grouped = tenants.groupBy { it.unit.ifBlank { "Geral" } }
            val names = (units.map { it.name } + grouped.keys).distinct().sorted()
                .filter { query.isBlank() && !hasFilters || grouped[it].orEmpty().isNotEmpty() }
            if (names.isEmpty()) item {
                if (query.isNotBlank() || hasFilters) AppEmptyState(Icons.Rounded.SearchOff, "Nenhuma locação encontrada",
                    "Nenhuma unidade tem inquilinos que correspondam à busca ou aos filtros.")
                else AppEmptyState(Icons.Rounded.MeetingRoom, "Nenhuma unidade cadastrada",
                    "Cadastre a casa, apartamento ou quarto para organizar os inquilinos por endereço.",
                    actionLabel = "Adicionar unidade", onAction = onAdd)
            }
            items(names, key = { it }) { name -> UnitRecord(name, units.find { it.name == name }, grouped[name].orEmpty(), onUnit, onTenant) }
        } else {
            if (tenants.isEmpty()) item {
                if (query.isNotBlank() || hasFilters) AppEmptyState(Icons.Rounded.SearchOff, "Nenhum inquilino encontrado",
                    "Ajuste a busca ou os filtros para ver outros inquilinos.")
                else AppEmptyState(Icons.Rounded.Cottage, "Nenhum inquilino cadastrado",
                    "Cadastre um inquilino com o valor do aluguel e o dia de vencimento.",
                    actionLabel = "Adicionar inquilino", onAction = onAdd)
            }
            itemsIndexed(tenants, key = { _, tenant -> tenant.id }) { index, tenant ->
                TenantRecord(tenant, onTenant, ledgerPosition(index, tenants.size))
            }
        }
    }
}

/** A tenant as a line of the ledger, used in lists and inside unit pages. */
@Composable
internal fun TenantLine(tenant: Tenant, onClick: () -> Unit, showUnit: Boolean = true) {
    val stacked = LocalDensity.current.fontScale > 1.3f
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 72.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Monogram(tenant.name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tenant.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (showUnit) tenant.unit.ifBlank { "Geral" } else "Vence dia ${tenant.dueDay}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (stacked) Text(CurrencyUtils.format(tenant.amount), style = NaniType.moneyRow)
        }
        if (!stacked) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(CurrencyUtils.format(tenant.amount), style = NaniType.moneyRow, maxLines = 1, softWrap = false)
            if (showUnit) Text("dia ${tenant.dueDay}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TenantRecord(tenant: Tenant, onTenant: (Tenant) -> Unit, position: LedgerPosition = LedgerPosition.Single, showUnit: Boolean = true) {
    LedgerSlice(position, ruleIndent = 70.dp) { TenantLine(tenant, { onTenant(tenant) }, showUnit) }
}

internal fun CapacityKind.noun(count: Int) = when (this) {
    CapacityKind.TENANTS -> if (count == 1) "inquilino" else "inquilinos"
    CapacityKind.ROOMS -> if (count == 1) "quarto" else "quartos"
    CapacityKind.SPACES -> if (count == 1) "vaga" else "vagas"
}

/** "1 lugar livre", "2 quartos livres", "Capacidade completa". */
internal fun RentalUnit.vacancySentence(): String {
    val free = capacity - tenantCount
    val noun = when (capacityKind) {
        CapacityKind.TENANTS -> if (free == 1) "lugar" else "lugares"
        CapacityKind.ROOMS -> if (free == 1) "quarto" else "quartos"
        CapacityKind.SPACES -> if (free == 1) "vaga" else "vagas"
    }
    return when {
        free > 0 -> "$free $noun ${if (free == 1) "livre" else "livres"}"
        free == 0 -> "Capacidade completa"
        else -> "${-free} acima da capacidade"
    }
}

/**
 * How full a unit is: a ring with the occupied share, the count beside it and how many places
 * are free. The ring takes the ink of the occupancy mark, so "full" and "has vacancies" read alike everywhere.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OccupancyPanel(unit: RentalUnit, modifier: Modifier = Modifier, ringSize: Dp = 52.dp) {
    val colors = MaterialTheme.colorScheme
    val kind = if (unit.tenantCount > unit.capacity) StatusKind.ERROR else unit.occupancyStatus.badgeKind()
    val ink = if (kind == StatusKind.NEUTRAL) colors.outline else ringColor(kind)
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerLow)
        .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusRing(listOf(RingSegment(shareOf(unit.tenantCount.toDouble(), unit.capacity.toDouble()), ink)),
            "${unit.tenantCount} de ${unit.capacity} ${unit.capacityKind.noun(unit.capacity)} ocupados",
            size = ringSize, stroke = 6.dp) { RingLabel("${unit.tenantCount}/${unit.capacity}", ringSize) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${unit.tenantCount} de ${unit.capacity} ${unit.capacityKind.noun(unit.capacity)}", style = MaterialTheme.typography.titleSmall)
            // The mark rides with the sentence and drops to its own line before it would be cut.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(unit.vacancySentence(), Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
                StatusMark(unit.occupancyStatus.displayName(), unit.occupancyStatus.badgeKind())
            }
        }
    }
}

@Composable
private fun UnitRecord(name: String, unit: RentalUnit?, tenants: List<Tenant>, onUnit: (String) -> Unit, onTenant: (Tenant) -> Unit) {
    var expanded by rememberSaveable(name) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, AppMotion.StateFloat, label = "unit expand")
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 6.dp),
        shape = RoundedCornerShape(AppSize.sheetRadius), color = colors.surface) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onUnit(name) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                UnitPlaque(name, size = 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(unit?.type?.displayName() ?: "Agrupamento de inquilinos",
                        unit?.location?.takeIf { it.isNotBlank() }).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp), tint = colors.onSurfaceVariant)
            }
            if (unit != null) OccupancyPanel(unit, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            LedgerRule()
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(pluralStringResource(R.plurals.tenant_count, tenants.size, tenants.size), Modifier.weight(1f).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Rounded.ExpandMore, if (expanded) "Recolher inquilinos" else "Mostrar inquilinos", Modifier.rotate(rotation))
                }
            }
            AnimatedVisibility(expanded,
                enter = expandVertically(AppMotion.ExpandVertically, expandFrom = Alignment.Top) + fadeIn(AppMotion.EnterFade),
                exit = shrinkVertically(AppMotion.CollapseVertically, shrinkTowards = Alignment.Top) + fadeOut(AppMotion.ExitFade)) {
                Column(Modifier.background(colors.surfaceContainerLow)) {
                    LedgerRule()
                    if (tenants.isEmpty()) {
                        Text("Nenhum inquilino nesta unidade.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                    } else tenants.forEachIndexed { index, tenant ->
                        if (index > 0) LedgerRule(Modifier.padding(start = 70.dp))
                        TenantLine(tenant, { onTenant(tenant) }, showUnit = false)
                    }
                }
            }
        }
    }
}

internal fun UnitType.displayName() = when (this) {
    UnitType.APARTMENT -> "Apartamento"; UnitType.HOUSE -> "Casa"; UnitType.COMMERCIAL_ROOM -> "Sala comercial"
    UnitType.BUILDING -> "Prédio"; UnitType.KITNET -> "Kitnet"; UnitType.OTHER -> "Outro"
}

internal fun OccupancyStatus.displayName() = when (this) {
    OccupancyStatus.AVAILABLE -> "Disponível"; OccupancyStatus.OCCUPIED -> "Ocupada"
    OccupancyStatus.PARTIALLY_OCCUPIED -> "Ocupação parcial"; OccupancyStatus.MAINTENANCE -> "Em manutenção"
    OccupancyStatus.INACTIVE -> "Inativa"
}

internal fun OccupancyStatus.badgeKind() = when (this) {
    OccupancyStatus.OCCUPIED -> StatusKind.SUCCESS
    OccupancyStatus.PARTIALLY_OCCUPIED -> StatusKind.WARNING
    OccupancyStatus.MAINTENANCE -> StatusKind.INFO
    OccupancyStatus.AVAILABLE, OccupancyStatus.INACTIVE -> StatusKind.NEUTRAL
}
