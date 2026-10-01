package com.rentalvalidator.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSpace

/** Shared form rhythm: a plain section heading and one surface containing related fields. */
@Composable
fun AppFormSection(title: String, subtitle: String, icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(AppSpace.xs))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(AppSpace.medium))
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.fillMaxWidth().padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)

    }
}

@Composable
fun AppFormFooter(onCancel: () -> Unit, onSave: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
        .navigationBarsPadding().padding(horizontal = AppSpace.large, vertical = AppSpace.medium)) {
        if (maxWidth < 300.dp || fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                PrimaryButton("Salvar", onSave)
                SecondaryButton("Cancelar", onCancel)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.medium)) {
                SecondaryButton("Cancelar", onCancel, Modifier.weight(1f))
                PrimaryButton("Salvar", onSave, Modifier.weight(1f))
            }
        }
    }
}
