package com.rentalvalidator.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Início", Icons.Outlined.SpaceDashboard, Icons.Rounded.SpaceDashboard)
    data object Tenants : Screen("tenants", "Locações", Icons.Outlined.Cottage, Icons.Rounded.Cottage)
    data object Grid : Screen("grid", "Receber", Icons.Outlined.Payments, Icons.Rounded.Payments)
    data object Validator : Screen("validator", "Conferir", Icons.AutoMirrored.Outlined.FactCheck, Icons.AutoMirrored.Rounded.FactCheck)
    data object Contracts : Screen("contracts", "Docs", Icons.Outlined.Folder, Icons.Rounded.Folder)
}
val navItems = listOf(Screen.Dashboard, Screen.Tenants, Screen.Grid, Screen.Contracts)
