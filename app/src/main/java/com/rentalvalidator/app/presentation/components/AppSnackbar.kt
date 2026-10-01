package com.rentalvalidator.app.presentation.components
import com.rentalvalidator.app.presentation.theme.AppSize

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val LocalAppSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("AppSnackbarHostState não foi fornecido")
}

@Composable
fun AppSnackbarProvider(
    hostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppSnackbarHostState provides hostState, content = content)
}

@Composable
fun rememberAppSnackbar(): (String) -> Unit {
    val hostState = LocalAppSnackbarHostState.current
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        { message ->
            scope.launch {
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }
}

@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier.padding(horizontal = ScreenHorizontalPadding)) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(AppSize.cardRadius),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary
        )
    }
}
