package com.rentalvalidator.app.presentation.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.theme.AppSpace

/** An empty view says what is missing and offers the next step. */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 28.dp)
        .semantics { liveRegion = LiveRegionMode.Polite }) {
        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(actionLabel, onAction, fill = false)
        }
    }
}

/** Ruled placeholder lines stand in for the entries being prepared. */
@Composable
fun AppLoadingState(title: String, message: String, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "loading").animateFloat(
        .45f, .9f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "loading pulse")
    Column(modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 20.dp)
        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().alpha(pulse), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { index ->
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(if (index == 1) .5f else .65f).height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp)))
                        Box(Modifier.fillMaxWidth(.35f).height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(5.dp)))
                    }
                }
            }
        }
    }
}

/** A failure explains what happened and offers one way forward. */
@Composable
fun AppErrorState(title: String, message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 28.dp)
        .semantics { liveRegion = LiveRegionMode.Assertive }) {
        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        PrimaryButton(actionLabel, onAction)
    }
}
