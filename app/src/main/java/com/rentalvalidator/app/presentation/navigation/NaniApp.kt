package com.rentalvalidator.app.presentation.navigation
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.theme.AppSpace
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.presentation.design.*

@Composable
fun FinanceNavigation(route:String,onNavigate:(Screen)->Unit, actions: @Composable RowScope.()->Unit = {}) {
    NaniHeader(stringResource(R.string.receipts), action = { Row(content = actions) })
    NaniTabs(listOf(stringResource(R.string.monthly), stringResource(R.string.check_statement)),if(route=="validator")1 else 0,
        {onNavigate(if(it==0)Screen.Grid else Screen.Validator)},Modifier.padding(horizontal=AppSpace.page))
}
@Composable
fun NaniApp(openTenantId:String?=null, openPeriod:String?=null, onOpenConsumed:()->Unit = {}) {
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    LaunchedEffect(openTenantId,openPeriod,entry != null) {
        if (entry != null) openTenantId?.let {
            nav.navigate(AppRoute.TenantPayments(it, openPeriod.orEmpty())) { launchSingleTop=true }
            onOpenConsumed()
        }
    }
    val go:(Screen)->Unit={ screen -> nav.navigate(screen.destination) {
        popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true
    }}
    NaniScaffold(entry?.destination, go) {
        AppNavGraph(nav, go, Modifier.fillMaxSize())
    }
}
