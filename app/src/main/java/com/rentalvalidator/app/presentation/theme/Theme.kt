package com.rentalvalidator.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Pen,
    onPrimary = Color.White,
    primaryContainer = PenSoft,
    onPrimaryContainer = PenDeep,
    inversePrimary = PenNight,
    secondary = InkMuted,
    onSecondary = Color.White,
    secondaryContainer = PaperDeep,
    onSecondaryContainer = Ink,
    tertiary = LightNaniColors.paid.ink,
    onTertiary = Color.White,
    tertiaryContainer = LightNaniColors.paid.fill,
    onTertiaryContainer = LightNaniColors.paid.onFill,
    background = Paper,
    onBackground = Ink,
    surface = Sheet,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkMuted,
    surfaceTint = Pen,
    inverseSurface = Color(0xFF242C38),
    inverseOnSurface = Color(0xFFEDF1E9),
    error = Stamp,
    onError = Color.White,
    errorContainer = StampSoft,
    onErrorContainer = StampDeep,
    outline = RuleStrong,
    outlineVariant = Rule,
    scrim = Color.Black,
    surfaceBright = SheetWhite,
    surfaceDim = PaperDeeper,
    surfaceContainerLowest = SheetWhite,
    surfaceContainerLow = PaperLow,
    surfaceContainer = Paper,
    surfaceContainerHigh = PaperDeep,
    surfaceContainerHighest = PaperDeeper
)

private val DarkColors = darkColorScheme(
    primary = PenNight,
    onPrimary = PenDeepNight,
    primaryContainer = PenSoftNight,
    onPrimaryContainer = Color(0xFFDDE4FF),
    inversePrimary = Pen,
    secondary = InkMutedNight,
    onSecondary = PaperNight,
    secondaryContainer = SheetNightHigh,
    onSecondaryContainer = InkNight,
    tertiary = DarkNaniColors.paid.ink,
    onTertiary = Color(0xFF0B2818),
    tertiaryContainer = DarkNaniColors.paid.fill,
    onTertiaryContainer = DarkNaniColors.paid.onFill,
    background = PaperNight,
    onBackground = InkNight,
    surface = SheetNight,
    onSurface = InkNight,
    surfaceVariant = SheetNightHigh,
    onSurfaceVariant = InkMutedNight,
    surfaceTint = PenNight,
    inverseSurface = InkNight,
    inverseOnSurface = SheetNightContainer,
    error = StampNight,
    onError = Color(0xFF4A0C06),
    errorContainer = StampSoftNight,
    onErrorContainer = Color(0xFFFFDAD4),
    outline = RuleStrongNight,
    outlineVariant = RuleNight,
    scrim = Color.Black,
    surfaceBright = SheetNightHighest,
    surfaceDim = PaperNight,
    surfaceContainerLowest = SheetNightLowest,
    surfaceContainerLow = SheetNightLow,
    surfaceContainer = SheetNightContainer,
    surfaceContainerHigh = SheetNightHigh,
    surfaceContainerHighest = SheetNightHighest
)

@Composable
fun RentalValidatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        SideEffect {
            // Bars are transparent through enableEdgeToEdge; only their icon contrast follows the app theme.
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    CompositionLocalProvider(LocalNaniColors provides if (darkTheme) DarkNaniColors else LightNaniColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** Extended palette for roles Material does not define. */
object NaniTheme {
    val colors: NaniColors
        @Composable @ReadOnlyComposable get() = LocalNaniColors.current
}
