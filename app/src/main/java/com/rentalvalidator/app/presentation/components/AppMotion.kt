package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * One quiet motion language for the operational surfaces in the app.
 * Transitions describe a state change; they never delay work or compete with data.
 */
object AppMotion {
    const val PressDuration = 100
    const val FeedbackDuration = 140
    const val StateDuration = 180
    const val LayoutDuration = 220

    val PressScale: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = Spring.StiffnessMedium
    )
    val QuickFloat: TweenSpec<Float> = tween(durationMillis = FeedbackDuration, easing = FastOutSlowInEasing)
    val StateFloat: TweenSpec<Float> = tween(durationMillis = StateDuration, easing = FastOutSlowInEasing)
    val EnterFade: TweenSpec<Float> = tween(durationMillis = FeedbackDuration, easing = FastOutSlowInEasing)
    val ExitFade: TweenSpec<Float> = tween(durationMillis = 100, easing = FastOutLinearInEasing)
    val ExpandVertically: FiniteAnimationSpec<IntSize> = tween(durationMillis = LayoutDuration, easing = FastOutSlowInEasing)
    val CollapseVertically: FiniteAnimationSpec<IntSize> = tween(durationMillis = 160, easing = FastOutLinearInEasing)
}
