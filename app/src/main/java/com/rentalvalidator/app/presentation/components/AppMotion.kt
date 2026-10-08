package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One quiet motion language. Motion answers what the person did: a sheet opens,
 * a row expands, a rent is stamped as paid. Nothing moves on its own except the
 * single reveal of the month's ruler.
 */
object AppMotion {
    const val PressDuration = 100
    const val FeedbackDuration = 140
    const val StateDuration = 200
    const val LayoutDuration = 240
    const val PageDuration = 260

    /** Decelerates firmly, like a page settling on a desk. */
    val Settle = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val PressScale: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
    val QuickFloat: TweenSpec<Float> = tween(durationMillis = FeedbackDuration, easing = Settle)
    val StateFloat: TweenSpec<Float> = tween(durationMillis = StateDuration, easing = Settle)
    val EnterFade: TweenSpec<Float> = tween(durationMillis = 180, easing = Settle)
    val ExitFade: TweenSpec<Float> = tween(durationMillis = 90, easing = FastOutLinearInEasing)
    val PageSlide: FiniteAnimationSpec<IntOffset> = tween(durationMillis = PageDuration, easing = Settle)
    val ExpandVertically: FiniteAnimationSpec<IntSize> = tween(durationMillis = LayoutDuration, easing = Settle)
    val CollapseVertically: FiniteAnimationSpec<IntSize> = tween(durationMillis = 180, easing = FastOutLinearInEasing)
}
