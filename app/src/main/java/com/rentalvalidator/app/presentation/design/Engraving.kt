package com.rentalvalidator.app.presentation.design

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The guilloché printed on a detail cover, like the fine security lines of a savings passbook.
 * A tenant's cover carries concentric rings that ripple out from the lit corner, like a
 * fingerprint; a unit's carries straight courses that run across the light, like the ruled
 * ground of a deed. Both are derived from a name, so every tenant and unit keeps its own
 * engraving on every visit. Every value is a fraction of the cover, so the pattern holds at any size.
 */
@Immutable
internal data class Engraving(
    /** How many ripples a ring would carry around a whole turn. */
    val waves: Float,
    /** Depth of a ripple, as a fraction of the cover's height. */
    val amplitude: Float,
    /** Where the ripples start, in radians. */
    val phase: Float,
    /** How far each ring's ripples turn ahead of the ring inside it, in radians. */
    val twist: Float
)

internal const val EngravingRings = 12

/** Each gap between rings is this much wider than the one before, so the rings open up away from the light. */
private const val RingGrowth = 1.06f

/** The rings' center, as fractions of the cover: just past the upper-right corner, where the light comes from. */
internal const val EngravingCenterX = 1.04f
internal const val EngravingCenterY = -.18f

internal fun engravingOf(seed: String): Engraving {
    val random = Random(seed.trim().lowercase().hashCode())
    return Engraving(
        waves = 4f + random.nextFloat() * 2f,
        amplitude = .016f + random.nextFloat() * .012f,
        phase = random.nextFloat() * (2 * PI).toFloat(),
        twist = .05f + random.nextFloat() * .25f
    )
}

/**
 * Radius of ring [index] at [angle] (radians, clockwise from three o'clock) on a [width] by
 * [height] cover. The innermost ring hugs the corner and the outermost reaches toward the middle,
 * but never so far on a wide window that it runs under the name. Each ring ripples gently around
 * its circle; the ripples turn a little from ring to ring, so the lines flow without crossing.
 */
internal fun Engraving.ringRadius(index: Int, angle: Float, width: Float, height: Float): Float {
    val first = height * .3f
    val last = min(width * .72f, height * 2.6f)
    val opened = (RingGrowth.pow(index) - 1f) / (RingGrowth.pow(EngravingRings - 1) - 1f)
    val ripple = amplitude * height * sin(waves * angle + phase + index * twist)
    return first + (last - first) * opened + ripple
}

/** Gap between two courses of a unit's engraving, as a fraction of the cover's height. */
private const val CourseGap = .12f

/** How far a course climbs for each step to the left, so the courses run across the light instead of toward it. */
private const val CourseSlope = .55f

/** Where the ink of an engraving starts, as a fraction of the cover's width; the mark sits to the left of it. */
internal const val EngravingInkFrom = .25f

/** How many courses it takes to rule a [width] by [height] cover from the lit corner down to where the ink starts. */
internal fun engravingCourses(width: Float, height: Float): Int =
    ((height + width * (1f - EngravingInkFrom) * CourseSlope) / (height * CourseGap)).toInt() + 2

/**
 * Height of course [index] at [x] on a [width] by [height] cover. The first course leaves the
 * right edge at the top corner and each one after it a fixed gap lower; all of them climb at the
 * same slope toward the left. Each course ripples gently along its length, a little ahead of the
 * one above it, so the courses flow without crossing.
 */
internal fun Engraving.courseY(index: Int, x: Float, width: Float, height: Float): Float {
    val ripple = amplitude * height * sin(waves * .7f * x / height + phase + index * twist)
    return height * CourseGap * index - (width - x) * CourseSlope + ripple
}
