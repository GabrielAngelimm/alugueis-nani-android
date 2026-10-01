package com.rentalvalidator.app.presentation.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.rentalvalidator.app.*

private fun selected(destination:NavDestination?,screen:Screen)=destination?.route==screen.route ||
    (screen==Screen.Grid && (destination?.route==Screen.Validator.route || destination?.route?.startsWith("tenantPayments/")==true))
@Composable
fun AppNavigation(currentDestination:NavDestination?,onNavigate:(Screen)->Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=16.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
        val compactLabels = maxWidth < 400.dp && LocalDensity.current.fontScale > 1.2f
        val shape = RoundedCornerShape(28.dp)
        Surface(Modifier.widthIn(max=520.dp).fillMaxWidth().navigationShadow(shape),shape=shape,
            color=MaterialTheme.colorScheme.surface,shadowElevation=0.dp,tonalElevation=0.dp,
            border=surfaceDepthBorder(.45f)) {
            Row(Modifier.selectableGroup().padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                navItems.forEach { screen ->
                    val active=selected(currentDestination,screen)
                    val selectionColor by animateColorAsState(
                        if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        animationSpec=tween(AppMotion.StateDuration),label="navigation selection")
                    val iconColor by animateColorAsState(
                        if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec=tween(AppMotion.StateDuration),label="navigation icon")
                    Surface(Modifier.weight(1f),shape=RoundedCornerShape(22.dp),
                        color=selectionColor) {
                        Column(Modifier.selectable(active,role=Role.Tab,onClick={onNavigate(screen)}).semantics { contentDescription = screen.title }
                            .heightIn(min=56.dp).padding(horizontal=3.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Icon(screen.icon,null,Modifier.size(21.dp),tint=iconColor)
                            Text(if(compactLabels && screen == Screen.Contracts) "Docs" else screen.title,modifier=Modifier.clearAndSetSemantics {},maxLines=1,style=MaterialTheme.typography.labelSmall,color=if(active)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AppNavigationRail(currentDestination:NavDestination?,onNavigate:(Screen)->Unit) {
    NavigationRail(Modifier.fillMaxHeight(),containerColor=MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(24.dp))
        navItems.forEach { screen -> NavigationRailItem(selected=selected(currentDestination,screen),
            onClick={onNavigate(screen)},icon={Icon(screen.icon,null)},label={Text(screen.title)}) }
    }
}
