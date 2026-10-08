package com.rentalvalidator.app.presentation.design

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.theme.AppSize

/** Search entry point; a dot marks that a query is narrowing the list. */
@Composable
fun NaniSearchButton(query: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (query.isNotBlank()) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
            Icon(Icons.Rounded.Search, stringResource(R.string.search_tenant))
        }
    }
}

/**
 * Search drops from the top of the screen, where the person's eyes already are.
 * Dismissing keeps the entered query active; clearing is explicit.
 */
@Composable
fun NaniSearchDialog(query: String, onQuery: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setGravity(Gravity.TOP) }
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            shape = RoundedCornerShape(AppSize.sheetRadius), color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.search_tenant), Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Fechar busca") }
                }
                Row(Modifier.padding(end = 8.dp)) {
                    NaniTextField(query, onQuery, "Nome do inquilino", focusRequester = focus,
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { onQuery(""); focus.requestFocus(); keyboard?.show() }) {
                                Icon(Icons.Rounded.Close, "Limpar busca")
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onDismiss() }))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.view_results), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos {}; focus.requestFocus(); keyboard?.show() }
    }
}
