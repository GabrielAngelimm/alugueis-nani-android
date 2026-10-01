package com.rentalvalidator.app.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.ui.dashboard.DashboardScreen
import com.rentalvalidator.app.presentation.ui.tenants.TenantsScreen
import com.rentalvalidator.app.presentation.ui.monthly_grid.MonthlyGridScreen
import com.rentalvalidator.app.presentation.ui.validator.ValidatorScreen
import com.rentalvalidator.app.presentation.ui.contracts.ContractsScreen
import com.rentalvalidator.app.presentation.ui.settings.SettingsScreen

@Composable
internal fun AppNavGraph(nav: NavHostController, go: (Screen) -> Unit, modifier: Modifier = Modifier) {
    NavHost(nav, AppRoute.Dashboard, modifier,
        enterTransition = { fadeIn(AppMotion.EnterFade) },
        exitTransition = { fadeOut(AppMotion.ExitFade) }) {
        composable<AppRoute.Dashboard> {
            DashboardScreen(
                onNavigateToSettings = { nav.navigate(AppRoute.Settings) },
                onNavigateToContracts = { go(Screen.Contracts) },
                onNavigateToPayments = { go(Screen.Grid) },
                onNavigateToValidator = { go(Screen.Validator) },
                onNavigateToTenants = { go(Screen.Tenants) })
        }
        composable<AppRoute.Tenants> {
            TenantsScreen(onNavigateToAddTenant = {}, onNavigateToEditTenant = {},
                onNavigateToTenantPayments = { nav.navigate(AppRoute.TenantPayments(it)) })
        }
        composable<AppRoute.Grid> { MonthlyGridScreen(onFinanceNavigate = go) }
        composable<AppRoute.TenantPayments> { entry ->
            val destination = entry.toRoute<AppRoute.TenantPayments>()
            MonthlyGridScreen(tenantId = destination.tenantId, initialPeriod = destination.period,
                onBack = { nav.popBackStack() }, onFinanceNavigate = go)
        }
        composable<AppRoute.Validator> { ValidatorScreen(onFinanceNavigate = go) }
        composable<AppRoute.Contracts> { ContractsScreen() }
        composable<AppRoute.Settings> { SettingsScreen(onNavigateBack = { nav.popBackStack() }) }
    }
}
