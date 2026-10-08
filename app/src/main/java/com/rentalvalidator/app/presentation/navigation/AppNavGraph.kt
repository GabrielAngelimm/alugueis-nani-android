package com.rentalvalidator.app.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.ui.contracts.ContractsScreen
import com.rentalvalidator.app.presentation.ui.dashboard.DashboardScreen
import com.rentalvalidator.app.presentation.ui.monthly_grid.MonthlyGridScreen
import com.rentalvalidator.app.presentation.ui.settings.SettingsScreen
import com.rentalvalidator.app.presentation.ui.tenants.TenantsScreen
import com.rentalvalidator.app.presentation.ui.validator.ValidatorScreen

/** Sections cross-fade; pages reached from a section slide in from the reading direction. */
@Composable
internal fun AppNavGraph(nav: NavHostController, go: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val drillIn = slideInHorizontally(AppMotion.PageSlide) { it / 6 } + fadeIn(AppMotion.EnterFade)
    val drillOut = slideOutHorizontally(AppMotion.PageSlide) { it / 6 } + fadeOut(AppMotion.ExitFade)
    NavHost(nav, AppRoute.Dashboard, modifier,
        enterTransition = { fadeIn(AppMotion.EnterFade) },
        exitTransition = { fadeOut(AppMotion.ExitFade) }) {
        composable<AppRoute.Dashboard> {
            DashboardScreen(
                onNavigateToSettings = { nav.navigate(AppRoute.Settings) },
                onNavigateToContracts = { go(Screen.Contracts) },
                onNavigateToPayments = { go(Screen.Grid) },
                onNavigateToValidator = { go(Screen.Validator) },
                onNavigateToTenants = { go(Screen.Tenants) },
                onNavigateToTenantPayments = { nav.navigate(AppRoute.TenantPayments(it)) })
        }
        composable<AppRoute.Tenants> {
            TenantsScreen(onNavigateToAddTenant = {}, onNavigateToEditTenant = {},
                onNavigateToTenantPayments = { nav.navigate(AppRoute.TenantPayments(it)) })
        }
        composable<AppRoute.Grid> { MonthlyGridScreen(onFinanceNavigate = go) }
        composable<AppRoute.TenantPayments>(
            enterTransition = { drillIn }, popExitTransition = { drillOut }
        ) { entry ->
            val destination = entry.toRoute<AppRoute.TenantPayments>()
            MonthlyGridScreen(tenantId = destination.tenantId, initialPeriod = destination.period,
                onBack = { nav.popBackStack() }, onFinanceNavigate = go)
        }
        composable<AppRoute.Validator> { ValidatorScreen(onFinanceNavigate = go) }
        composable<AppRoute.Contracts> { ContractsScreen() }
        composable<AppRoute.Settings>(
            enterTransition = { drillIn }, popExitTransition = { drillOut }
        ) { SettingsScreen(onNavigateBack = { nav.popBackStack() }) }
    }
}
