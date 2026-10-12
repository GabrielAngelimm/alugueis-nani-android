package com.rentalvalidator.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Caderneta: the landlady's account book. Ledger-sage paper, ballpoint ink for
 * actions, account-book green for paid rent, rubber-stamp red for overdue and
 * highlighter yellow for what is still open. Screens pick a role, never a hex.
 */

// Paper and ink, light.
internal val Paper = Color(0xFFEDF1E9)
internal val Sheet = Color(0xFFFBFCF9)
internal val SheetWhite = Color(0xFFFFFFFF)
internal val PaperLow = Color(0xFFF4F7F1)
internal val PaperDeep = Color(0xFFE4EAE0)
internal val PaperDeeper = Color(0xFFDAE1D5)
internal val Rule = Color(0xFFD3DBCF)
internal val RuleStrong = Color(0xFF7D887E)
internal val Ink = Color(0xFF1B2330)
internal val InkMuted = Color(0xFF566170)
internal val Pen = Color(0xFF2A43A6)
internal val PenSoft = Color(0xFFE1E7FA)
internal val PenDeep = Color(0xFF172869)
internal val Stamp = Color(0xFFB0372D)
internal val StampSoft = Color(0xFFFAE2DD)
internal val StampDeep = Color(0xFF6B1912)

// Paper and ink, night.
internal val PaperNight = Color(0xFF10151D)
internal val SheetNight = Color(0xFF171E28)
internal val SheetNightLowest = Color(0xFF0C1016)
internal val SheetNightLow = Color(0xFF141A23)
internal val SheetNightContainer = Color(0xFF1C2430)
internal val SheetNightHigh = Color(0xFF232C39)
internal val SheetNightHighest = Color(0xFF2B3544)
internal val RuleNight = Color(0xFF2B3442)
internal val RuleStrongNight = Color(0xFF717C8B)
internal val InkNight = Color(0xFFE8ECF1)
internal val InkMutedNight = Color(0xFFA6AFBD)
internal val PenNight = Color(0xFFB0C0FF)
internal val PenSoftNight = Color(0xFF27357A)
internal val PenDeepNight = Color(0xFF0F1E5C)
internal val StampNight = Color(0xFFFF9E91)
internal val StampSoftNight = Color(0xFF4C1E19)

/** Colors that Material's scheme has no role for: payment marks, plaques and charts. */
@Immutable
data class NaniColors(
    val paid: StatusTone,
    val pending: StatusTone,
    val review: StatusTone,
    val overdue: StatusTone,
    val neutral: StatusTone,
    val plaque: Color,
    val onPlaque: Color,
    /** The cover of a tenant's page: sapphire, lit from the upper right. Text on it uses [onPlaque]. */
    val tenantCover: CoverTone,
    /** The cover of a unit's page: the same material in petrol blue, so a place never reads as a person. */
    val unitCover: CoverTone,
    /** A unit's mark on its cover, built like a monogram: [Pair.first] fills the disc, [Pair.second] draws the icon. */
    val unitMark: Pair<Color, Color>,
    /** Fill of the single strongest action. At night it stays saturated so it never reads as disabled. */
    val action: Color,
    val onAction: Color,
    val rule: Color,
    val track: Color,
    val monograms: List<Pair<Color, Color>>,
    val isDark: Boolean
) {
    /** The lit and the deep corner of each cover, for tints and shadows that follow it. */
    val coverStart: Color get() = tenantCover.lit
    val coverEnd: Color get() = tenantCover.deep
    val unitCoverStart: Color get() = unitCover.lit
    val unitCoverEnd: Color get() = unitCover.deep
}

/**
 * A cover's material: the ground runs from [lit] through [mid] to [deep] across the diagonal, a
 * soft [glow] of light falls from the upper-right corner and a [bounce] of a neighbouring hue
 * rises from the lower left, for depth. [glow] and [bounce] carry their own strength as alpha.
 */
@Immutable
data class CoverTone(val lit: Color, val mid: Color, val deep: Color, val glow: Color, val bounce: Color)

/** A status mark: [ink] for text and strokes, [fill] for its background, [onFill] for text on that fill. */
@Immutable
data class StatusTone(val ink: Color, val fill: Color, val onFill: Color)

internal val LightNaniColors = NaniColors(
    paid = StatusTone(Color(0xFF2B6A4B), Color(0xFFDBEEE1), Color(0xFF17442E)),
    pending = StatusTone(Color(0xFF8A5800), Color(0xFFFBE8A8), Color(0xFF573700)),
    review = StatusTone(Pen, PenSoft, PenDeep),
    overdue = StatusTone(Stamp, StampSoft, StampDeep),
    neutral = StatusTone(InkMuted, PaperDeep, Ink),
    plaque = Color(0xFF233886),
    onPlaque = Color(0xFFF4F6FF),
    tenantCover = CoverTone(Color(0xFF5B74DA), Color(0xFF3B54B6), Color(0xFF243A8C),
        glow = Color(0x6B9DB0F5), bounce = Color(0x4D5E3FB8)),
    unitCover = CoverTone(Color(0xFF3F8597), Color(0xFF2A6375), Color(0xFF173F4F),
        glow = Color(0x6186C3CF), bounce = Color(0x4D1F4C7E)),
    unitMark = Color(0xFFDDEEF1) to Color(0xFF173F4F),
    action = Pen,
    onAction = Color.White,
    rule = Rule,
    track = PaperDeeper,
    monograms = listOf(
        Color(0xFFD9E8DC) to Color(0xFF285640),
        Color(0xFFDDE3F6) to Color(0xFF283C8A),
        Color(0xFFEFE3CF) to Color(0xFF6B4A17),
        Color(0xFFE9DCEA) to Color(0xFF633A60),
        Color(0xFFD7EBF0) to Color(0xFF1D5567),
        Color(0xFFE6E8D2) to Color(0xFF4F5419)
    ),
    isDark = false
)

internal val DarkNaniColors = NaniColors(
    paid = StatusTone(Color(0xFF8AD4AA), Color(0xFF173728), Color(0xFFC0EBD1)),
    pending = StatusTone(Color(0xFFF0C766), Color(0xFF3A3013), Color(0xFFFBE3A5)),
    review = StatusTone(PenNight, PenSoftNight, Color(0xFFDDE4FF)),
    overdue = StatusTone(StampNight, StampSoftNight, Color(0xFFFFDAD4)),
    neutral = StatusTone(InkMutedNight, SheetNightHigh, InkNight),
    plaque = Color(0xFF3A50A8),
    onPlaque = Color(0xFFF4F6FF),
    tenantCover = CoverTone(Color(0xFF4660C8), Color(0xFF2D429C), Color(0xFF172462),
        glow = Color(0x4D8196EA), bounce = Color(0x5752309E)),
    unitCover = CoverTone(Color(0xFF326E80), Color(0xFF1F5263), Color(0xFF0F2E3B),
        glow = Color(0x476FAFBD), bounce = Color(0x57163E6A)),
    unitMark = Color(0xFF0C2530) to Color(0xFFA9CFD7),
    action = Color(0xFF4460D6),
    onAction = Color.White,
    rule = RuleNight,
    track = SheetNightHighest,
    monograms = listOf(
        Color(0xFF1E3428) to Color(0xFFB4DCC2),
        Color(0xFF222C4F) to Color(0xFFC3CFFA),
        Color(0xFF392D1B) to Color(0xFFEBCF9F),
        Color(0xFF35233A) to Color(0xFFE2C1E2),
        Color(0xFF17323A) to Color(0xFFAEDCE8),
        Color(0xFF2E301B) to Color(0xFFD9DCA6)
    ),
    isDark = true
)

val LocalNaniColors = staticCompositionLocalOf { LightNaniColors }

// Stable references kept for non-UI callers (ContractStatusInfo carries a color).
val SuccessGreen = LightNaniColors.paid.ink
val WarningAmber = LightNaniColors.pending.ink
val ErrorRed = Stamp
val NeutralInk = InkMuted
