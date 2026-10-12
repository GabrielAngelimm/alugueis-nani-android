package com.rentalvalidator.app.presentation.design

/**
 * Where the relief of a cover sits, in pixels: the box its mark is drawn in. The box may run past
 * the cover's right edge; the cover clips it, so the mark reads as pressed into a larger sheet.
 */
internal data class ReliefFrame(val left: Float, val top: Float, val width: Float, val height: Float)

/** A cover holds a relief only from this size up, in dp: smaller ones have no room beside the name. */
internal const val ReliefMinWidth = 240f
internal const val ReliefMinHeight = 100f

/** How much of the relief runs off the right edge. */
internal const val ReliefBleed = .3f

/** The relief never comes further in than this fraction of the cover, so the mark and the start of the name sit on clear material. */
internal const val ReliefReach = .5f

/**
 * The box for a relief whose mark is [aspect] times as wide as it is tall, on a [width] by
 * [height] cover. The mark is about as tall as the cover, set a little low, against the right edge
 * with [ReliefBleed] of it running off. A wide mark on a narrow cover is set smaller, so it never
 * comes in past [ReliefReach] of the width.
 */
internal fun reliefFrame(width: Float, height: Float, aspect: Float): ReliefFrame {
    val tall = height * 1.04f
    val shown = 1f - ReliefBleed
    val markHeight = minOf(tall, width * (1f - ReliefReach) / (aspect * shown))
    val markWidth = markHeight * aspect
    return ReliefFrame(left = width - markWidth * shown, top = (height - markHeight) / 2f + height * .07f,
        width = markWidth, height = markHeight)
}
