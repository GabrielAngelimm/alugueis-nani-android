package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSpace

/** Header of a full-screen editor: closing discards, so it is an X rather than a back arrow. */
@Composable
internal fun EditorHeader(title: String, subtitle: String?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = AppSpace.page, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Fechar") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
