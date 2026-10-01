package com.rentalvalidator.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavDestination
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.theme.AppSize

/** Owns adaptive navigation, insets and app-wide feedback, independently of destinations. */
@Composable
internal fun NaniScaffold(destination: NavDestination?, go: (Screen) -> Unit, content: @Composable () -> Unit) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val showNavigation = destination?.route != "settings"
    val host = remember { SnackbarHostState() }
    AppSnackbarProvider(hostState = host) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { AppSnackbarHost(host) },
            bottomBar = { if (showNavigation && !wide) AppNavigation(destination, go) }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide && showNavigation) AppNavigationRail(destination, go)
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = AppSize.contentMaxWidth).fillMaxSize()) { content() }
                }
            }
        }
    }
}
