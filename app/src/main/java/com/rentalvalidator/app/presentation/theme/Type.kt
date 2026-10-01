package com.rentalvalidator.app.presentation.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.R

@OptIn(ExperimentalTextApi::class)
val NaniSans = FontFamily(*listOf(400,500,600,700).map {
    Font(R.font.manrope, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
}.toTypedArray())
@OptIn(ExperimentalTextApi::class)
val NaniDisplay = FontFamily(Font(R.font.fraunces, FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("opsz", 32f))))
private fun type(size:Int, height:Int, weight:FontWeight=FontWeight.Normal) = TextStyle(
    fontFamily=NaniSans, fontSize=size.sp, lineHeight=height.sp, fontWeight=weight,
    fontFeatureSettings="tnum", platformStyle=PlatformTextStyle(includeFontPadding=false))
val Typography = Typography(
    displayLarge=type(44,50,FontWeight.Medium),
    displayMedium=type(36,42,FontWeight.Medium),
    displaySmall=type(30,36,FontWeight.Medium),
    headlineLarge=type(32,39,FontWeight.SemiBold),
    headlineMedium=type(26,33,FontWeight.SemiBold),
    headlineSmall=type(24,31,FontWeight.SemiBold),
    titleLarge=type(20,27,FontWeight.SemiBold), titleMedium=type(16,23,FontWeight.Bold),
    titleSmall=type(14,21,FontWeight.Bold), bodyLarge=type(16,24), bodyMedium=type(14,21),
    bodySmall=type(12,18), labelLarge=type(14,20,FontWeight.SemiBold),
    labelMedium=type(12,18,FontWeight.SemiBold),labelSmall=type(11,16,FontWeight.SemiBold))
