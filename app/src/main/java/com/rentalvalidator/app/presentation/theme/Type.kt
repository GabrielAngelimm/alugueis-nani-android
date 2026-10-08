package com.rentalvalidator.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.R

/*
 * Two voices. Fraunces, softened and set at its optical size, writes the things
 * a person would write by hand in an account book: titles, names and amounts.
 * Manrope with tabular figures carries every control, label and sentence.
 */

@OptIn(ExperimentalTextApi::class)
val NaniSans = FontFamily(listOf(400, 500, 600, 700, 800).map { weight ->
    Font(R.font.manrope, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
})

@OptIn(ExperimentalTextApi::class)
private fun fraunces(opticalSize: Float) = FontFamily(listOf(500, 600, 700).map { weight ->
    Font(R.font.fraunces, FontWeight(weight), variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
        FontVariation.Setting("SOFT", 70f),
        FontVariation.Setting("WONK", 0f)
    ))
})

/** Display cut, for 26sp and larger. */
val NaniSerif = fraunces(72f)

/** Text cut, for names and amounts between 16 and 24sp. */
val NaniSerifText = fraunces(20f)

private val Trimmed = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun sans(size: Int, height: Int, weight: Int = 400, tracking: Double = 0.0) = TextStyle(
    fontFamily = NaniSans, fontSize = size.sp, lineHeight = height.sp, fontWeight = FontWeight(weight),
    letterSpacing = tracking.sp, fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false), lineHeightStyle = Trimmed
)

private fun serif(family: FontFamily, size: Int, height: Int, weight: Int, tracking: Double) = TextStyle(
    fontFamily = family, fontSize = size.sp, lineHeight = height.sp, fontWeight = FontWeight(weight),
    letterSpacing = tracking.sp, platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = Trimmed
)

val Typography = Typography(
    displayLarge = serif(NaniSerif, 52, 58, 600, -1.2),
    displayMedium = serif(NaniSerif, 42, 48, 600, -0.9),
    displaySmall = serif(NaniSerif, 34, 40, 600, -0.6),
    headlineLarge = serif(NaniSerif, 30, 36, 600, -0.5),
    headlineMedium = serif(NaniSerif, 27, 33, 600, -0.4),
    headlineSmall = serif(NaniSerifText, 22, 28, 600, -0.2),
    titleLarge = serif(NaniSerifText, 20, 26, 600, -0.1),
    titleMedium = sans(17, 23, 700),
    titleSmall = sans(15, 20, 700),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(15, 21),
    bodySmall = sans(13, 18, 500),
    labelLarge = sans(15, 20, 600, 0.1),
    labelMedium = sans(13, 17, 600, 0.1),
    labelSmall = sans(12, 16, 600, 0.2)
)

/** Amounts are written in the account-book voice at three sizes. */
object NaniType {
    val moneyHero: TextStyle = Typography.displayMedium
    val moneyLarge: TextStyle = serif(NaniSerifText, 24, 30, 600, -0.3)
    val moneyRow: TextStyle = serif(NaniSerifText, 18, 24, 600, -0.1)
}
