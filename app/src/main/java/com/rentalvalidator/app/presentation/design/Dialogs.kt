package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSize

/**
 * Confirmation for consequential actions. The confirm label names the action ("Excluir",
 * "Restaurar"), never "OK". [confirmLabel] null shows a single acknowledgement button.
 */
@Composable
fun NaniConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    text: @Composable () -> Unit,
    dismissLabel: String? = "Cancelar",
    destructive: Boolean = false,
    icon: ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppSize.modalRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        icon = icon?.let { glyph -> {
            Box(Modifier.size(52.dp).background(
                if (destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                CircleShape), contentAlignment = Alignment.Center) {
                Icon(glyph, null, Modifier.size(25.dp),
                    tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        } },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = text,
        confirmButton = {
            if (confirmLabel != null) TextButton(onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor =
                    if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            if (dismissLabel != null) TextButton(onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                Text(dismissLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/** Body copy for a confirmation dialog. */
@Composable
fun DialogText(text: String, isWarning: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium,
        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}
