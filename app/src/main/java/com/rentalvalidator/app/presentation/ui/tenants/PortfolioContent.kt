package com.rentalvalidator.app.presentation.ui.tenants
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.theme.AppSpace
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.*
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.util.CurrencyUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TenantsOverview(tenants:List<Tenant>,units:List<RentalUnit>,mode:TenantViewMode,
    query:String,hasFilters:Boolean,onQuery:(String)->Unit,onMode:(TenantViewMode)->Unit,
    onUnit:(String)->Unit,onTenant:(Tenant)->Unit,onFilters:()->Unit,onAdd:()->Unit) {
    var showSearch by remember { mutableStateOf(false) }
    if(showSearch) NaniSearchDialog(query,onQuery,{showSearch=false})
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=12.dp)) {
        stickyHeader {
          Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            NaniHeader(stringResource(R.string.rentals)) {
                Row {
                    NaniSearchButton(query) {showSearch=true}
                    IconButton(onClick=onFilters) {BadgedBox(badge={if(hasFilters)Badge()}){Icon(Icons.Outlined.Tune,"Filtros")}}
                    FilledTonalIconButton(onClick=onAdd){Icon(Icons.Outlined.Add,if(mode==TenantViewMode.UNITS)"Adicionar unidade" else "Adicionar inquilino")}
                }
            }
            NaniTabs(listOf("Unidades","Inquilinos"),if(mode==TenantViewMode.UNITS)0 else 1,
                {onMode(if(it==0)TenantViewMode.UNITS else TenantViewMode.ALL)},Modifier.padding(horizontal=AppSpace.page,vertical=16.dp))
        }
        }
        if(mode==TenantViewMode.UNITS) {
            val grouped=tenants.groupBy{it.unit.ifBlank{"Geral"}}
            val names=(units.map{it.name}+grouped.keys).distinct().sorted().filter { query.isBlank()&&!hasFilters || grouped[it].orEmpty().isNotEmpty() }
            if(names.isEmpty()) item {if(query.isNotBlank()||hasFilters)AppEmptyState(Icons.Outlined.SearchOff,"Nenhuma locação encontrada","Ajuste a busca ou os filtros.") else PortfolioEmpty(true,onAdd)}
            items(names,key={it}){name->UnitRecord(name,units.find{it.name==name},grouped[name].orEmpty(),onUnit,onTenant)}
        } else {
            if(tenants.isEmpty())item {if(query.isNotBlank()||hasFilters)AppEmptyState(Icons.Outlined.SearchOff,"Nenhum inquilino encontrado","Ajuste a busca ou os filtros.") else PortfolioEmpty(false,onAdd)}
            items(tenants,key={it.id}) {TenantRecord(it,onTenant)}
        }
    }
}
@Composable
internal fun TenantRecord(tenant:Tenant,onTenant:(Tenant)->Unit, embedded: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (embedded) 14.dp else 20.dp)
    Surface(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = if (embedded) 12.dp else AppSpace.page, vertical = if (embedded) 5.dp else 6.dp)
            .premiumShadow(shape),
        shape = shape,
        color = colors.surface,
        border = surfaceDepthBorder(if (embedded) .35f else .5f)
    ) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button){onTenant(tenant)}
        .padding(horizontal=16.dp, vertical=if (embedded) 12.dp else 14.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            InitialsAvatar(tenant.name)
            NaniTenantIdentity(tenant.name, tenant.unit, Modifier.weight(1f)) {
                Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(11.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = .4f))
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val amount = CurrencyUtils.format(tenant.amount)
            val compact = maxWidth < 270.dp || LocalDensity.current.fontScale > 1.35f
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(amount, style=MaterialTheme.typography.titleSmall.copy(fontWeight=FontWeight.SemiBold))
                    Text("Vence dia ${tenant.dueDay}", style=MaterialTheme.typography.bodySmall, color=colors.onSurfaceVariant)
                }
            } else {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(amount,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall.copy(fontWeight=FontWeight.SemiBold),
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("Vence dia ${tenant.dueDay}",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                }
            }
        }
    }
    }
}
@Composable
private fun UnitRecord(name:String,unit:RentalUnit?,tenants:List<Tenant>,onUnit:(String)->Unit,onTenant:(Tenant)->Unit) {
    var expanded by rememberSaveable(name){mutableStateOf(false)}
    val shape = RoundedCornerShape(20.dp)
    Surface(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page,vertical=6.dp).premiumShadow(shape),shape=shape,
        color=MaterialTheme.colorScheme.surface,border=surfaceDepthBorder()) {
        Column {
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button){onUnit(name)}
                .background(Brush.linearGradient(listOf(lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, .25f), MaterialTheme.colorScheme.surface)))
                .padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Apartment, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(unit?.type?.displayName() ?: "Agrupamento", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (unit != null) StatusBadge(unit.occupancyStatus.displayName(), unit.occupancyStatus.badgeKind(), Modifier.widthIn(max = 155.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.SemiBold))
                    Icon(Icons.Outlined.NorthEast, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!unit?.location.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(unit!!.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 18.dp), color=MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(androidx.compose.ui.res.pluralStringResource(com.rentalvalidator.app.R.plurals.tenant_count, tenants.size, tenants.size), style = MaterialTheme.typography.titleSmall)
                    unit?.let { Text("Capacidade: ${it.capacity} ${when (it.capacityKind) {
                        CapacityKind.TENANTS -> if (it.capacity == 1) "inquilino" else "inquilinos"
                        CapacityKind.ROOMS -> if (it.capacity == 1) "quarto" else "quartos"
                        CapacityKind.SPACES -> if (it.capacity == 1) "vaga" else "vagas"
                    }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Recolher inquilinos" else "Mostrar inquilinos") }
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .5f))) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                    if (tenants.isEmpty()) {
                        Text("Nenhum inquilino nesta unidade.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        tenants.forEach { TenantRecord(it, onTenant, embedded = true) }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
@Composable private fun PortfolioEmpty(units:Boolean,onAdd:()->Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(if(units)"O começo de uma carteira organizada." else "Cada locação começa com uma pessoa.",style=MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text("Cadastre ${if(units)"uma unidade" else "um inquilino"} ou ajuste a busca para continuar.",style=MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp));PrimaryButton(if(units)"Adicionar unidade" else "Adicionar inquilino",onAdd)
    }
}
internal fun UnitType.displayName()=when(this){UnitType.APARTMENT->"Apartamento";UnitType.HOUSE->"Casa";UnitType.COMMERCIAL_ROOM->"Sala comercial";UnitType.BUILDING->"Prédio";UnitType.KITNET->"Kitnet";UnitType.OTHER->"Outro"}
internal fun OccupancyStatus.displayName()=when(this){OccupancyStatus.AVAILABLE->"Disponível";OccupancyStatus.OCCUPIED->"Ocupada";OccupancyStatus.PARTIALLY_OCCUPIED->"Ocupação parcial";OccupancyStatus.MAINTENANCE->"Em manutenção";OccupancyStatus.INACTIVE->"Inativa"}

internal fun OccupancyStatus.badgeKind() = when (this) {
    OccupancyStatus.OCCUPIED -> StatusKind.SUCCESS
    OccupancyStatus.MAINTENANCE, OccupancyStatus.PARTIALLY_OCCUPIED -> StatusKind.WARNING
    OccupancyStatus.INACTIVE -> StatusKind.ERROR
    else -> StatusKind.NEUTRAL
}
