package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.design.LedgerRule
import com.rentalvalidator.app.presentation.design.UnitTile
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType

/** The label over a control that is not a text field, in the same voice as a field's label. */
@Composable
internal fun ControlLabel(text: String, required: Boolean = false, isError: Boolean = false) {
    Text(if (required) "$text *" else text, Modifier.padding(start = 2.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A message under a control, read out when it appears. */
@Composable
internal fun ControlMessage(text: String, isError: Boolean) {
    Text(text, Modifier.padding(start = 2.dp, top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The due day as a page of a month: 31 days in weeks of seven. Choosing one inks it; days past
 * the 28th explain that short months fall due on their last day, as the rest of the app counts them.
 */
@Composable
internal fun DueDayPicker(selected: Int?, onSelect: (Int) -> Unit, error: String?) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        ControlLabel("Dia de vencimento", required = true, isError = error != null)
        Surface(shape = RoundedCornerShape(AppSize.sheetRadius), color = colors.surface,
            border = if (error != null) BorderStroke(1.dp, colors.error) else null) {
            Column(Modifier.fillMaxWidth().padding(8.dp).selectableGroup()
                .semantics { contentDescription = "Dia de vencimento"; if (error != null) error(error) },
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..31).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day -> DayCell(day, day == selected, { onSelect(day) }, Modifier.weight(1f)) }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        when {
            error != null -> ControlMessage(error, isError = true)
            selected != null -> ControlMessage("Vence todo dia $selected." +
                if (selected > 28) " Nos meses mais curtos, vence no último dia do mês." else "", isError = false)
        }
    }
}

@Composable
private fun DayCell(day: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val nani = NaniTheme.colors
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) nani.action else colors.surface, tween(AppMotion.StateDuration), label = "day fill")
    val ink by animateColorAsState(if (selected) nani.onAction else colors.onSurface, tween(AppMotion.StateDuration), label = "day ink")
    // The chosen day settles in with a small spring, like a pen tapping the page.
    val scale by animateFloatAsState(if (selected) 1f else .94f, spring(dampingRatio = .5f, stiffness = Spring.StiffnessMediumLow),
        label = "day scale")
    Box(modifier.height(44.dp).scale(if (selected) scale else 1f).clip(RoundedCornerShape(12.dp)).background(fill)
        .selectable(selected, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = "Dia $day" }, contentAlignment = Alignment.Center) {
        Text("$day", style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = ink)
    }
}

/** A grid of large choices with a glyph each, such as the kinds of property. Two or three to a row. */
@Composable
internal fun <T> GlyphChoices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    glyph: (T) -> ImageVector,
    onSelect: (T) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = if (maxWidth < 330.dp) 2 else 3
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { option ->
                        GlyphChoice(label(option), glyph(option), option == selected, { onSelect(option) }, Modifier.weight(1f))
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun GlyphChoice(label: String, glyph: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) colors.primaryContainer.copy(alpha = .55f) else colors.surface,
        tween(AppMotion.StateDuration), label = "glyph fill")
    val stroke by animateColorAsState(if (selected) colors.primary else colors.outlineVariant,
        tween(AppMotion.StateDuration), label = "glyph stroke")
    val lift by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow),
        label = "glyph lift")
    Box(modifier.heightIn(min = 112.dp).clip(RoundedCornerShape(AppSize.sheetRadius)).background(fill)
        .border(BorderStroke(if (selected) 1.5.dp else 1.dp, stroke), RoundedCornerShape(AppSize.sheetRadius))
        .selectable(selected, role = Role.RadioButton, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UnitTile(glyph, Modifier.scale(lift), size = 48.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant)
        }
        androidx.compose.animation.AnimatedVisibility(selected, Modifier.align(Alignment.TopEnd).padding(8.dp),
            enter = scaleIn(spring(dampingRatio = .6f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Box(Modifier.size(22.dp).background(NaniTheme.colors.action, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = NaniTheme.colors.onAction)
            }
        }
    }
}

/** Short exclusive choices that wrap like chips, such as the bank a rent arrives through. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipChoices(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val chosen = option == selected
            val colors = MaterialTheme.colorScheme
            val fill by animateColorAsState(if (chosen) colors.primaryContainer else colors.surface, tween(AppMotion.StateDuration),
                label = "chip fill")
            Row(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(fill)
                .border(1.dp, if (chosen) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
                .selectable(chosen, role = Role.RadioButton) { onSelect(option) }
                .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimatedVisibility(chosen, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = colors.primary)
                }
                Text(option, style = MaterialTheme.typography.labelLarge,
                    color = if (chosen) colors.onPrimaryContainer else colors.onSurface)
            }
        }
    }
}

/**
 * A count set with minus and plus, written large in the ledger's hand. The number can also be
 * typed; the field keeps the full form's rules (digits only, three at most).
 */
@Composable
internal fun CountStepper(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    unit: String,
    error: String?,
    focusRequester: FocusRequester,
    onDone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val count = value.toIntOrNull() ?: 0
    Column(Modifier.fillMaxWidth()) {
        ControlLabel(label, required = true, isError = error != null)
        Surface(shape = RoundedCornerShape(AppSize.sheetRadius), color = colors.surface,
            border = if (error != null) BorderStroke(1.dp, colors.error) else null) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                StepperButton(Icons.Rounded.Remove, "Diminuir ${label.lowercase()}", enabled = count > 1) { onValue((count - 1).coerceAtLeast(1).toString()) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicTextField(value, { typed -> capacityInput(typed)?.let(onValue) },
                        Modifier.fillMaxWidth().focusRequester(focusRequester).semantics {
                            contentDescription = label
                            if (error != null) error(error)
                        },
                        textStyle = NaniType.moneyHero.copy(color = colors.onSurface, textAlign = TextAlign.Center),
                        singleLine = true, cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onDone() }))
                    Text(unit, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                StepperButton(Icons.Rounded.Add, "Aumentar ${label.lowercase()}", enabled = count < 999) { onValue((count + 1).coerceAtMost(999).toString()) }
            }
        }
        if (error != null) ControlMessage(error, isError = true)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(AppSize.controlRadius),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Icon(icon, description)
    }
}

/** A quiet note that explains a consequence, such as where a tenant goes when there are no units yet. */
@Composable
internal fun FlowHint(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(AppSize.controlRadius))
        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A failure to save, kept on the page where the person can act on it rather than behind the editor. */
@Composable
internal fun FlowError(text: String) {
    val nani = NaniTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(AppSize.controlRadius)).background(nani.overdue.fill)
        .padding(14.dp).semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(20.dp), tint = nani.overdue.ink)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = nani.overdue.onFill)
    }
}

/** The review as one ledger sheet: a ruled line per step, each opening its step again. */
@Composable
internal fun ReviewSheet(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(AppSize.sheetRadius), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
internal fun ReviewLine(
    icon: ImageVector,
    label: String,
    lines: List<String>,
    onEdit: () -> Unit,
    error: String? = null,
    divider: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Editar $label", onClick = onEdit)
        .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(if (error != null) nani.overdue.fill else colors.surfaceContainer), contentAlignment = Alignment.Center) {
            Icon(if (error != null) Icons.Rounded.ErrorOutline else icon, null, Modifier.size(21.dp),
                tint = if (error != null) nani.overdue.ink else colors.onSurface)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            lines.forEachIndexed { index, line ->
                Text(line, style = if (index == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (index == 0) colors.onSurface else colors.onSurfaceVariant)
            }
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        Spacer(Modifier.width(2.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp), tint = colors.onSurfaceVariant)
    }
    if (divider) LedgerRule(Modifier.padding(start = 70.dp))
}
