package com.rentalvalidator.app.presentation.navigation

import com.rentalvalidator.app.Screen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Stable serial names preserve the existing top-level navigation identity. */
@Serializable
sealed interface AppRoute {
    @Serializable @SerialName("dashboard") data object Dashboard : AppRoute
    @Serializable @SerialName("tenants") data object Tenants : AppRoute
    @Serializable @SerialName("grid") data object Grid : AppRoute
    @Serializable @SerialName("validator") data object Validator : AppRoute
    @Serializable @SerialName("contracts") data object Contracts : AppRoute
    @Serializable @SerialName("settings") data object Settings : AppRoute
    @Serializable @SerialName("tenantPayments")
    data class TenantPayments(val tenantId: String, val period: String = "") : AppRoute
}

internal val Screen.destination: AppRoute
    get() = when (this) {
        Screen.Dashboard -> AppRoute.Dashboard
        Screen.Tenants -> AppRoute.Tenants
        Screen.Grid -> AppRoute.Grid
        Screen.Validator -> AppRoute.Validator
        Screen.Contracts -> AppRoute.Contracts
    }
