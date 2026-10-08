package com.rentalvalidator.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace

/** A form reads as a few ledger sheets: a plain heading, then one sheet of related fields. */
@Composable
fun AppFormSection(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.medium)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.padding(top = 2.dp).size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(AppSize.sheetRadius), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
        }
    }
}

/** Save and cancel stay within reach above the keyboard; they stack when type is enlarged. */
@Composable
fun AppFormFooter(onCancel: () -> Unit, onSave: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AppSpace.page, vertical = AppSpace.medium)) {
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
    }
}
