package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.rentalvalidator.app.Screen
import com.rentalvalidator.app.navItems
import com.rentalvalidator.app.presentation.design.rememberWordFitScale
import com.rentalvalidator.app.presentation.design.scaled

private fun selected(destination: NavDestination?, screen: Screen) = destination?.route == screen.route ||
    (screen == Screen.Grid && (destination?.route == Screen.Validator.route || destination?.route?.startsWith("tenantPayments/") == true))

/**
 * Bottom navigation as the index tabs of a notebook: the open section is marked by a
 * short ink tab on the top edge and a filled glyph; the bar itself stays flat paper.
 */
@Composable
fun AppNavigation(currentDestination: NavDestination?, onNavigate: (Screen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                // Labels are measured against the real item width: with enlarged type "Documentos"
                // becomes "Docs" before any label is allowed to shrink, and none is ever clipped.
                val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                val itemWidth = (minOf(maxWidth, 560.dp) - 16.dp) / navItems.size - 6.dp
                val fullFit = rememberWordFitScale(navItems.map { it.title }, labelStyle, itemWidth, minScale = .1f)
                val compactLabels = fullFit < .9f
                val fit = rememberWordFitScale(navItems.map { if (compactLabels && it == Screen.Contracts) "Docs" else it.title },
                    labelStyle, itemWidth)
                Row(Modifier.widthIn(max = 560.dp).fillMaxWidth().selectableGroup().padding(horizontal = 8.dp)) {
                    navItems.forEach { screen ->
                        val active = selected(currentDestination, screen)
                        val tint by animateColorAsState(if (active) colors.primary else colors.onSurfaceVariant,
                            tween(AppMotion.StateDuration), label = "navigation tint")
                        val marker by animateDpAsState(if (active) 28.dp else 0.dp,
                            tween(AppMotion.StateDuration, easing = AppMotion.Settle), label = "navigation marker")
                        Column(
                            Modifier.weight(1f).selectable(active, role = Role.Tab, onClick = { onNavigate(screen) })
                                .semantics { contentDescription = screen.title }.heightIn(min = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.width(marker).height(3.dp)
                                .background(colors.primary, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)))
                            Spacer(Modifier.height(9.dp))
                            Icon(if (active) screen.selectedIcon else screen.icon, null, Modifier.size(24.dp), tint = tint)
                            Spacer(Modifier.height(4.dp))
                            Text(if (compactLabels && screen == Screen.Contracts) "Docs" else screen.title,
                                Modifier.clearAndSetSemantics {}.padding(bottom = 8.dp), maxLines = 1,
                                style = labelStyle.copy(
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold).scaled(fit),
                                color = tint)
                        }
                    }
                }
            }
        }
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
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
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
