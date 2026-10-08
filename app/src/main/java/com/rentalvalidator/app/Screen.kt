package com.rentalvalidator.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Início", Icons.Outlined.Home, Icons.Filled.Home)
    data object Tenants : Screen("tenants", "Locações", Icons.Outlined.Key, Icons.Filled.Key)
    data object Grid : Screen("grid", "Receber", Icons.Outlined.Payments, Icons.Filled.Payments)
    data object Validator : Screen("validator", "Conferir", Icons.AutoMirrored.Outlined.FactCheck, Icons.AutoMirrored.Filled.FactCheck)
    data object Contracts : Screen("contracts", "Documentos", Icons.Outlined.Folder, Icons.Filled.Folder)
}
val navItems = listOf(Screen.Dashboard, Screen.Tenants, Screen.Grid, Screen.Contracts)
