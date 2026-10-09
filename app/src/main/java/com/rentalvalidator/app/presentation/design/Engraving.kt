package com.rentalvalidator.app.presentation.design

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The guilloché printed on a detail cover, like the fine security lines of a savings passbook.
 * It is derived from a name, so every tenant and unit keeps its own engraving on every visit.
 * Every value is a fraction of the cover, so the pattern holds at any size.
 */
@Immutable
internal data class Engraving(
    /** Length of one wave, as a fraction of the cover's width. */
    val wavelength: Float,
    /** Height of a wave crest, as a fraction of the cover's height. */
    val amplitude: Float,
    /** Where the first wave starts, in radians. */
    val phase: Float,
    /** How far each strand's wave runs ahead of the one above it, in radians. */
    val twist: Float,
    /** Rise across the whole width, as a fraction of the height; negative climbs to the right. */
    val tilt: Float
)

internal const val EngravingStrands = 12

internal fun engravingOf(seed: String): Engraving {
    val random = Random(seed.trim().lowercase().hashCode())
    return Engraving(
        wavelength = .55f + random.nextFloat() * .35f,
        amplitude = .12f + random.nextFloat() * .1f,
        phase = random.nextFloat() * (2 * PI).toFloat(),
        twist = .2f + random.nextFloat() * .22f,
        tilt = -.18f + random.nextFloat() * .36f
    )
}

/**
 * Height of strand [index] at [x] on a [width] by [height] cover. Strands are spread evenly a
 * little past both edges so the weave never ends inside the cover. The [crossing] set twists the
 * other way, and where the two sets meet they form the woven net of a printed guilloché.
 */
internal fun Engraving.strandY(index: Int, x: Float, width: Float, height: Float, crossing: Boolean = false): Float {
    val base = height * (-.1f + 1.2f * (index + .5f) / EngravingStrands)
    val turn = if (crossing) -index * twist else index * twist
    val wave = amplitude * height * sin(2 * PI.toFloat() * x / (wavelength * width) + phase + turn)
    return base + wave + tilt * height * (x / width - .5f)
}
