package com.rentalvalidator.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
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

/** Position of each section along the navigation bar; statement checking sits just right of the monthly view. */
private fun sectionOf(route: String?): Float? = when (route) {
    "dashboard" -> 0f
    "tenants" -> 1f
    "grid" -> 2f
    "validator" -> 2.5f
    "contracts" -> 3f
    else -> null
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int? {
    val from = sectionOf(initialState.destination.route) ?: return null
    val to = sectionOf(targetState.destination.route) ?: return null
    return if (to == from) null else if (to > from) 1 else -1
}

/**
 * Sections share one horizontal axis: moving to a section on the right slides the page in from
 * the right, and back again from the left, so the bar and the content tell the same story.
 */
private val sectionEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    direction()?.let { dir ->
        slideInHorizontally(AppMotion.PageSlide) { dir * it / 8 } + fadeIn(tween(220, delayMillis = 50, easing = AppMotion.Settle))
    } ?: fadeIn(AppMotion.EnterFade)
}

private val sectionExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    direction()?.let { dir ->
        slideOutHorizontally(AppMotion.PageSlide) { -dir * it / 8 } + fadeOut(tween(120, easing = FastOutLinearInEasing))
    } ?: fadeOut(AppMotion.ExitFade)
}

/** Pages reached from a section slide in from the reading direction. */
@Composable
internal fun AppNavGraph(nav: NavHostController, go: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val drillIn = slideInHorizontally(AppMotion.PageSlide) { it / 6 } + fadeIn(AppMotion.EnterFade)
    val drillOut = slideOutHorizontally(AppMotion.PageSlide) { it / 6 } + fadeOut(AppMotion.ExitFade)
    NavHost(nav, AppRoute.Dashboard, modifier,
        enterTransition = sectionEnter, exitTransition = sectionExit,
        popEnterTransition = sectionEnter, popExitTransition = sectionExit) {
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
