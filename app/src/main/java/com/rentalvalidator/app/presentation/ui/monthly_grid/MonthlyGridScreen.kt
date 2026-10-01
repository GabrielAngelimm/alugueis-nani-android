package com.rentalvalidator.app.presentation.ui.monthly_grid
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.presentation.theme.AppSpace
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.util.CurrencyUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyGridScreen(viewModel:MonthlyGridViewModel=hiltViewModel(), tenantId:String?=null, initialPeriod:String?=null,
    onBack:(()->Unit)?=null, onFinanceNavigate:(com.rentalvalidator.app.Screen)->Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showSnackbar = rememberAppSnackbar()
    LaunchedEffect(state.error) {
        state.error?.let { showSnackbar(it); viewModel.clearError() }
    }
    var selected by remember{mutableStateOf<TenantPaymentItem?>(null)}
    var query by remember{mutableStateOf("")}
    var filter by remember{mutableIntStateOf(0)}
    var showFilters by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    LaunchedEffect(initialPeriod) {
        initialPeriod?.let { value -> runCatching { java.time.YearMonth.parse(value) }.getOrNull()?.let { viewModel.changeMonth(it.monthValue,it.year) } }
    }
    val scoped = state.items.filter { tenantId == null || it.tenant.id == tenantId }
    val visible=scoped.filter { (it.tenant.name.contains(query,true)||it.tenant.unit.contains(query,true)) &&
        (filter==0 || if(filter==1)it.payment?.status!=PaymentStatus.PAGO else it.payment?.status==PaymentStatus.PAGO) }
    val paid=scoped.count{it.payment?.status==PaymentStatus.PAGO}
    if(showSearch) NaniSearchDialog(query,{query=it},{showSearch=false})
    Column(Modifier.fillMaxSize()) {
        if (tenantId != null) NaniHeader(stringResource(R.string.payments), scoped.firstOrNull()?.tenant?.name ?: "Inquilino", onBack = onBack)
        else com.rentalvalidator.app.presentation.navigation.FinanceNavigation("grid",onFinanceNavigate) {
            NaniSearchButton(query) {showSearch=true}
            IconButton(onClick={showFilters=true}) {BadgedBox(badge={if(filter!=0) Badge()}) {Icon(Icons.Outlined.Tune,"Filtrar recebimentos")}}
        }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=12.dp)) {
        stickyHeader {
          Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=viewModel::previousMonth){Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"Mês anterior")}
                Text("${getMonthName(state.month)} ${state.year}", Modifier.weight(1f),
                    style=MaterialTheme.typography.titleLarge, textAlign=androidx.compose.ui.text.style.TextAlign.Center)

                IconButton(onClick=viewModel::nextMonth){Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"Próximo mês")}
            }
            NaniSummaryStrip("Pagos",paid,Icons.Outlined.CheckCircle,"Pendentes",scoped.size-paid,Icons.Outlined.Schedule,
                Modifier.padding(horizontal=AppSpace.page,vertical=12.dp),StatusKind.SUCCESS,StatusKind.WARNING,tonal=true)
            NaniTabs(listOf("Todos","Pendentes","Pagos"),filter,{filter=it},Modifier.padding(horizontal=AppSpace.page,vertical=6.dp))
        }
        }
        if(state.isLoading) item { AppLoadingState("Atualizando pagamentos","Organizando o período selecionado.") }
        if(!state.isLoading&&visible.isEmpty())item {AppEmptyState(Icons.Outlined.AccountBalanceWallet,"Nenhum pagamento encontrado","Ajuste os filtros ou cadastre um inquilino.")}
        items(visible,key={it.tenant.id}){item -> PaymentRecord(item){selected=item}}
    }
    }
    if (showFilters) NaniSheet("Filtrar recebimentos", {showFilters=false}) {
        Column(Modifier.selectableGroup(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(stringResource(R.string.all), stringResource(R.string.pending), stringResource(R.string.paid)).forEachIndexed { index, label ->
                NaniChoice(label, selected=filter==index, onClick={filter=index;showFilters=false})
            }
        }
    }
    selected?.let { item -> PaymentBottomSheet(item,{selected=null}){viewModel.updatePaymentStatus(item.tenant.id,it)} }
}
@Composable
fun getMonthName(month: Int) = androidx.compose.ui.res.stringArrayResource(com.rentalvalidator.app.R.array.month_names).getOrNull(month - 1).orEmpty()
