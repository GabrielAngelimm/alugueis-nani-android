package com.rentalvalidator.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.theme.AppSize

/**
 * Owns adaptive navigation, insets and app-wide feedback, independently of destinations.
 * On phones the content runs to the bottom edge, under the floating bar; a short fade of the
 * paper behind the bar lets lists dissolve beneath it instead of being cut by a hard edge.
 */
@Composable
internal fun NaniScaffold(destination: NavDestination?, go: (Screen) -> Unit, content: @Composable () -> Unit) {
    val wide = LocalConfiguration.current.screenWidthDp.dp >= RailBreakpoint
    val showNavigation = destination?.route != "settings"
    val floating = showNavigation && !wide
    val host = remember { SnackbarHostState() }
    val background = MaterialTheme.colorScheme.background
    val navigationBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clearance = if (floating) navigationBars + FloatingNavigationReserve else 0.dp
    AppSnackbarProvider(hostState = host) {
        // The surface sets the default ink for everything drawn on the page. Without it, text that
        // takes its color from context falls back to black, which disappears on the night theme.
        Surface(Modifier.fillMaxSize(), color = background, contentColor = MaterialTheme.colorScheme.onBackground) {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .then(if (floating) Modifier else Modifier.navigationBarsPadding())
                    .imePadding()) {
                    if (wide && showNavigation) AppNavigationRail(destination, go)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = AppSize.contentMaxWidth).fillMaxSize()) {
                            CompositionLocalProvider(LocalNavigationClearance provides clearance) { content() }
                        }
                    }
                }
                AnimatedVisibility(floating, Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(AppMotion.PageSlide) { it } + fadeIn(AppMotion.EnterFade),
                    exit = slideOutVertically(AppMotion.PageSlide) { it } + fadeOut(AppMotion.ExitFade)) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height(clearance + 28.dp).background(Brush.verticalGradient(
                            0f to background.copy(alpha = 0f), .5f to background.copy(alpha = .78f), 1f to background)))
                        AppNavigation(destination, go)
                    }
                }
                AppSnackbarHost(host, Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = if (floating) clearance else navigationBars + 12.dp))
            }
        }
    }
}
