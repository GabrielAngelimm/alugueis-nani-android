package com.rentalvalidator.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Nani: neutral mineral surfaces, indigo actions, independent semantic states.
val BrandPrimary = Color(0xFF525DD8)
val BrandPrimaryDark = Color(0xFF343D9B)
val BrandSoft = Color(0xFFECEEFC)
val BrandSoftDark = Color(0xFF2D3353)

// Data accents. They are intentionally softened for fills and reserved for
// related information, so a varied dashboard still reads as one product.
val AccentSky = Color(0xFF287F9A)
val AccentSkySoft = Color(0xFFE7F3F8)
val AccentViolet = Color(0xFF525DD8)
val AccentVioletSoft = Color(0xFFECEEFC)
val AccentMint = Color(0xFF347A61)
val AccentMintSoft = Color(0xFFEAF4EF)
val AccentAmber = Color(0xFF925127)
val AccentAmberSoft = Color(0xFFFAEBDC)
val AccentCoral = Color(0xFFA85F5A)
val AccentCoralSoft = Color(0xFFF8ECEB)

// Mineral canvas with opaque white content surfaces and restrained tonal depth.
val AppBackground = Color(0xFFF7F8FA)
val AppSurface = Color(0xFFFFFFFF)
val AppSurfaceElevated = Color(0xFFFFFFFF)
val AppSurfaceMuted = Color(0xFFEDF0F4)
// Tenant records use a slightly denser surface and a tonal header so every
// person reads as one deliberate object instead of blending into the list.
val TenantCardSurface = Color(0xFFFFFFFF)
// Deliberately just one quiet step darker than the reference header so the
// tenant identity remains slightly more prominent without becoming a band.
val TenantCardHeaderSurface = Color(0xFFF7F6F3)
val TenantCardBorder = Color(0xFFDCE0E7)
val TenantCardDivider = Color(0xFFE5E8EE)
val AppBorder = Color(0xFFDCE0E7)
val AppControlBorder = Color(0xFF858D9B)
val AppSearchField = Color(0xFFFFFFFF)
val AppInputField = Color(0xFFF0F2F6)
val AppTextPrimary = Color(0xFF202631)
val AppTextSecondary = Color(0xFF606775)
val AppTextTertiary = Color(0xFF697181)
// Icons describe content before they describe an action. Keep their resting
// state neutral; reserve blue for primary interaction and selected controls.
val AppIconNeutral = Color(0xFF606775)

// Semantic roles. Screens should choose a role, not a decorative color.
val ActionPrimary = BrandPrimary
val SelectionActive = BrandPrimary
val SemanticSuccess = AccentMint
val SemanticAttention = AccentAmber
val SemanticAlert = AccentCoral
val SemanticInformation = AccentViolet
val TenantIdentityBlue = Color(0xFF5D78B6)
val TenantIdentityGreen = Color(0xFF4B8A78)
val TenantIdentityCoral = Color(0xFFA76D72)
val TenantIdentityAmber = Color(0xFF9A784A)

// Achromatic depth: no colored light in shadows.
val ShadowAmbient = Color.Black
val ShadowSpot = Color.Black
val FloatingGlass = Color(0xF2FFFFFF)

// Dark surfaces
val AppBackgroundDark = Color(0xFF111317)
val AppSurfaceDark = Color(0xFF1B1E24)
val AppSurfaceMutedDark = Color(0xFF272B34)
val AppSurfaceElevatedDark = Color(0xFF323743)
val AppBorderDark = Color(0xFF565E6D)
val AppSearchFieldDark = Color(0xFF272B34)
val AppInputFieldDark = Color(0xFF242830)
val AppTextPrimaryDark = Color(0xFFF2F3F7)
val AppTextSecondaryDark = Color(0xFFB4BAC8)
val TenantCardSurfaceDark = Color(0xFF1B1E24)
val TenantCardHeaderSurfaceDark = Color(0xFF242830)
val TenantCardBorderDark = Color(0xFF565E6D)
val TenantCardDividerDark = Color(0xFF323743)

// Semantic colors
val SuccessGreen = AccentMint
val SuccessSoft = AccentMintSoft
val WarningAmber = AccentAmber
val WarningSoft = AccentAmberSoft
val StatusOrange = WarningAmber
val ErrorRed = Color(0xFFB64440)
val ErrorSoft = Color(0xFFFFEEEE)
val InfoBlue = AccentViolet
val InfoSoft = AccentVioletSoft

// Compatibility aliases retained while legacy screens are migrated.
val md_theme_light_background = AppBackground
val md_theme_light_surface = AppSurface
val md_theme_light_surfaceVariant = AppSurfaceMuted
val md_theme_light_primary = BrandPrimary
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = BrandSoft
val md_theme_light_onPrimaryContainer = BrandPrimaryDark
val md_theme_light_secondary = AppTextSecondary
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = AppSurfaceMuted
val md_theme_light_onSecondaryContainer = AppTextPrimary
val md_theme_light_error = ErrorRed
val md_theme_light_errorContainer = ErrorSoft
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = ErrorRed
val md_theme_light_onBackground = AppTextPrimary
val md_theme_light_onSurface = AppTextPrimary
val md_theme_light_onSurfaceVariant = AppTextSecondary
val md_theme_light_outline = AppBorder

val md_theme_dark_background = AppBackgroundDark
val md_theme_dark_surface = AppSurfaceDark
val md_theme_dark_surfaceVariant = AppSurfaceMutedDark
val md_theme_dark_primary = Color(0xFFA7AFFA)
val md_theme_dark_onPrimary = Color(0xFF20274D)
val md_theme_dark_primaryContainer = BrandSoftDark
val md_theme_dark_onPrimaryContainer = Color(0xFFE3E6FF)
val md_theme_dark_secondary = AppTextSecondaryDark
val md_theme_dark_onSecondary = AppTextPrimary
val md_theme_dark_secondaryContainer = AppSurfaceMutedDark
val md_theme_dark_onSecondaryContainer = AppTextPrimaryDark
val md_theme_dark_error = Color(0xFFFF8B83)
val md_theme_dark_errorContainer = Color(0xFF5B2928)
val md_theme_dark_onError = Color(0xFF35100F)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD5)
val md_theme_dark_onBackground = AppTextPrimaryDark
val md_theme_dark_onSurface = AppTextPrimaryDark
val md_theme_dark_onSurfaceVariant = AppTextSecondaryDark
val md_theme_dark_outline = AppBorderDark

val GlobalBackgroundBrush = Brush.verticalGradient(listOf(AppBackground, AppBackground))
val GradientBlue = listOf(BrandPrimary, Color(0xFF26B8E8))
val UnitHeroGradient = listOf(
    Color(0xFFFAFBFF),
    Color(0xFFF4F6FF),
    Color(0xFFF0F4FA)
)
val GradientTeal = listOf(InfoBlue, Color(0xFF8FA7F0))
val GradientLavender = listOf(InfoBlue, BrandPrimary)
val GradientAmber = listOf(WarningAmber, Color(0xFFFFB940))
val GradientCardSurface = listOf(AppSurface, AppSurface)
val GradientVioletBlue = GradientBlue
val GradientPurpleCyan = GradientBlue
val GradientPinkPurple = GradientTeal
val GradientDeepPurple = GradientLavender
val GradientCyan = GradientTeal

val LightBlueAccent = BrandPrimary
val InfoCyan = InfoBlue
val GlowPrimary = BrandSoft
val GlowCyan = InfoSoft
val GlowPurple = BrandSoft
val GlassSurface = AppSurface
val GlassBorder = AppBorder
val GlassHighlight = Color.White

