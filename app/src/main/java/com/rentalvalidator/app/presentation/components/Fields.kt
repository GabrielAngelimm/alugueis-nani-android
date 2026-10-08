package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** A field reads like an entry on a ruled line: a filled box resting on its rule. */
private val FieldShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 2.dp)

@Composable
private fun naniFieldColors(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceContainerLowest,
        unfocusedContainerColor = colors.surfaceContainer,
        disabledContainerColor = colors.surfaceContainerHigh,
        errorContainerColor = colors.surfaceContainerLowest,
        focusedIndicatorColor = colors.primary,
        unfocusedIndicatorColor = colors.outline,
        errorIndicatorColor = colors.error,
        cursorColor = colors.primary,
        focusedTrailingIconColor = colors.onSurfaceVariant,
        unfocusedTrailingIconColor = colors.onSurfaceVariant,
        focusedPlaceholderColor = colors.onSurfaceVariant.copy(alpha = .8f),
        unfocusedPlaceholderColor = colors.onSurfaceVariant.copy(alpha = .8f)
    )
}

@Composable
private fun FieldLabel(text: String, focused: Boolean, isError: Boolean) {
    val color by animateColorAsState(
        when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }, tween(AppMotion.FeedbackDuration), label = "field label")
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(start = 2.dp))
}

/**
 * The visible label doubles as the accessibility name, so assistive technology and
 * tests address a field by what the person reads above it.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NaniTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    required: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, isError) {
        if (focused) { delay(200); bringIntoView.bringIntoView() }
    }
    Column(modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)) {
        FieldLabel(if (required) "$label *" else label, focused, isError)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = label }
                .onFocusChanged { focused = it.isFocused }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            shape = FieldShape,
            placeholder = helperText?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
            supportingText = if (isError && errorMessage != null) ({ Text(errorMessage, style = MaterialTheme.typography.bodySmall) }) else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            isError = isError,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = naniFieldColors()
        )
    }
}

/** A closed list of choices presented with the same anatomy as a text field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaniDropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, expanded, isError = false)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!readOnly) expanded = it }) {
            TextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !readOnly)
                    .semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = {
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(if (expanded) 180f else 0f))
                },
                shape = FieldShape,
                colors = naniFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 340.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                options.forEach { option ->
                    val chosen = option == selectedOption
                    DropdownMenuItem(
                        text = {
                            Text(option, style = MaterialTheme.typography.bodyLarge,
                                color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        },
                        onClick = { onOptionSelected(option); expanded = false },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { selected = chosen },
                        trailingIcon = if (chosen) ({ Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }) else null
                    )
                }
            }
        }
    }
}
