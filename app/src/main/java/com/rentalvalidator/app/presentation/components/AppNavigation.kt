package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.navItems
import com.rentalvalidator.app.presentation.design.rememberWordFitScale
import com.rentalvalidator.app.presentation.design.scaled
import com.rentalvalidator.app.presentation.theme.NaniTheme

private fun selected(destination: NavDestination?, screen: Screen) = destination?.route == screen.route ||
    (screen == Screen.Grid && (destination?.route == Screen.Validator.route || destination?.route?.startsWith("tenantPayments/") == true))

private val ItemHeight = 60.dp
private val BarPadding = 6.dp
private val BarShape = RoundedCornerShape(30.dp)
private val IndicatorShape = RoundedCornerShape(24.dp)

/** Height the floating bar takes above the system navigation, including its outer margins. */
val FloatingNavigationReserve: Dp = ItemHeight + BarPadding * 2 + 8.dp + 12.dp

/**
 * Space a scrolling page leaves after its last entry so nothing ends hidden under the
 * floating navigation. Zero wherever the bar is not shown.
 */
val LocalNavigationClearance = compositionLocalOf { 0.dp }

/**
 * Navigation as a floating capsule. A tinted lens slides under the open section and
 * stretches on the way, leading edge first, like a drop of ink finding its place; the
 * section's glyph fills in with a small lift. Shadows are tinted with the theme's ink rather
 * than pure black, and at night a fine light edge replaces the shadow that would not show.
 */
@Composable
fun AppNavigation(currentDestination: NavDestination?, onNavigate: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val haptics = LocalHapticFeedback.current
    val selectedIndex = navItems.indexOfFirst { selected(currentDestination, it) }
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter) {
        val barWidth = minOf(maxWidth, 520.dp)
        val itemWidth = (barWidth - BarPadding * 2) / navItems.size
        // Labels are measured against the real item width, so a very narrow phone shrinks them
        // slightly instead of clipping a word.
        val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
        val fit = rememberWordFitScale(navItems.map { it.title }, labelStyle, itemWidth - 8.dp)
        val shadowInk = if (nani.isDark) Color.Black else colors.onSurface
        val edge = if (nani.isDark) Brush.verticalGradient(listOf(Color.White.copy(alpha = .14f), Color.White.copy(alpha = .03f)))
            else Brush.verticalGradient(listOf(Color.White, colors.outlineVariant.copy(alpha = .7f)))
        Box(
            Modifier.width(barWidth)
                // Two shadows, as a real object casts: a wide, soft ambient pool and a short, denser contact shadow.
                .shadow(elevation = 30.dp, shape = BarShape, clip = false,
                    ambientColor = shadowInk.copy(alpha = .08f), spotColor = shadowInk.copy(alpha = .18f))
                .shadow(elevation = 5.dp, shape = BarShape, clip = false,
                    ambientColor = shadowInk.copy(alpha = .06f), spotColor = shadowInk.copy(alpha = .16f))
                .clip(BarShape)
                .background(if (nani.isDark) colors.surfaceContainerHigh else colors.surfaceContainerLowest)
                .border(1.dp, edge, BarShape)
                .padding(BarPadding)
        ) {
            if (selectedIndex >= 0) StretchingLens(selectedIndex, itemWidth, colors.primaryContainer,
                lerp(colors.primaryContainer, Color.White, if (nani.isDark) .07f else .45f), colors.primary.copy(alpha = if (nani.isDark) .22f else .12f))
            Row(Modifier.selectableGroup()) {
                navItems.forEachIndexed { index, screen ->
                    val active = index == selectedIndex
                    NavigationItem(screen, active, labelStyle.copy(
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold).scaled(fit), Modifier.width(itemWidth)) {
                        if (!active) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(screen)
                    }
                }
            }
        }
    }
}

/**
 * The lens under the open section. Each edge has its own spring, so the lens stretches toward its
 * target; a faint light from above and a hairline rim give it the look of tinted glass.
 */
@Composable
private fun StretchingLens(index: Int, itemWidth: Dp, color: Color, highlight: Color, rim: Color) {
    val previous = remember { intArrayOf(index) }
    val forward = index >= previous[0]
    SideEffect { previous[0] = index }
    val fast = spring<Dp>(dampingRatio = .78f, stiffness = 620f)
    val slow = spring<Dp>(dampingRatio = .82f, stiffness = 260f)
    val start by animateDpAsState(itemWidth * index, if (forward) slow else fast, label = "lens start")
    val end by animateDpAsState(itemWidth * (index + 1), if (forward) fast else slow, label = "lens end")
    Box(Modifier.offset(x = start + 3.dp).width((end - start - 6.dp).coerceAtLeast(0.dp)).height(ItemHeight)
        .clip(IndicatorShape).background(Brush.verticalGradient(listOf(highlight, color)))
        .border(1.dp, rim, IndicatorShape))
}

@Composable
private fun NavigationItem(screen: Screen, active: Boolean, labelStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tween(AppMotion.StateDuration), label = "navigation tint")
    val lift = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(active) {
        if (active && !first[0]) {
            lift.snapTo(.78f)
            lift.animateTo(1f, spring(dampingRatio = .42f, stiffness = 520f))
        }
        first[0] = false
    }
    Column(
        modifier.height(ItemHeight).clip(IndicatorShape)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = screen.title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(active, animationSpec = tween(AppMotion.StateDuration), label = "navigation glyph") { filled ->
            Icon(if (filled) screen.selectedIcon else screen.icon, null,
                Modifier.size(24.dp).graphicsLayer { scaleX = lift.value; scaleY = lift.value }, tint = tint)
        }
        Spacer(Modifier.height(3.dp))
        Text(screen.title,
            Modifier.clearAndSetSemantics {}, maxLines = 1, style = labelStyle, color = tint)
    }
}

/** The same sections for wide windows, as a rail against the left edge. */
@Composable
fun AppNavigationRail(currentDestination: NavDestination?, onNavigate: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row {
        NavigationRail(Modifier.fillMaxHeight(), containerColor = colors.surface) {
            Spacer(Modifier.height(24.dp))
            navItems.forEach { screen ->
                val active = selected(currentDestination, screen)
                NavigationRailItem(
                    selected = active,
                    onClick = { onNavigate(screen) },
                    icon = { Icon(if (active) screen.selectedIcon else screen.icon, null) },
                    label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.onPrimaryContainer,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        VerticalDivider(thickness = 1.dp, color = colors.outlineVariant)
    }
}

/** Width at which the bottom bar becomes a rail. */
internal val RailBreakpoint = 600.dp
