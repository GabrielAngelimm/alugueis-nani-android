package com.rentalvalidator.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route:String,val title:String,val icon:ImageVector) {
    data object Dashboard:Screen("dashboard","Início",Icons.Outlined.SpaceDashboard)
    data object Tenants:Screen("tenants","Locações",Icons.Outlined.Apartment)
    data object Grid:Screen("grid","Receber",Icons.Outlined.AccountBalanceWallet)
    data object Validator:Screen("validator","Conferir",Icons.Outlined.FactCheck)
    data object Contracts:Screen("contracts","Documentos",Icons.Outlined.FolderOpen)
}
val navItems=listOf(Screen.Dashboard,Screen.Tenants,Screen.Grid,Screen.Contracts)
