package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.design.BankCoin
import com.rentalvalidator.app.presentation.design.LedgerRule
import com.rentalvalidator.app.presentation.design.bankMarkOf
import com.rentalvalidator.app.presentation.design.engravedCover
import com.rentalvalidator.app.presentation.design.rememberWordFitScale
import com.rentalvalidator.app.presentation.design.scaled
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

/**
 * Choices that each stand for a place, as compact cards two to a row (three on a wide screen): the
 * kinds of property, or the units a tenant can live in. Each card carries its mark beside its name,
 * small enough for a whole set to fit on one screen. At rest a card is a pale wash of steel; the
 * chosen one fills in as a small unit cover, the ruled steel blue of the page the unit opens on, so
 * the answer already looks like what it makes. [labelStyle] sets the names: a kind reads as a
 * label, a unit's name in the ledger's hand.
 */
@Composable
internal fun <T> CoverChoices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    glyph: (T) -> ImageVector,
    labelStyle: TextStyle = MaterialTheme.typography.titleSmall,
    onSelect: (T) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = if (maxWidth < 560.dp) 2 else 3
        val gap = 10.dp
        // Every card's names share one size, eased down just enough that no word breaks on a narrow phone.
        val text = (maxWidth - gap * (perRow - 1)) / perRow - CoverPadding * 2 - CoverMarkSize - CoverGap
        val fit = rememberWordFitScale(options.map(label), labelStyle, text)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            options.chunked(perRow).forEach { row ->
                // Cards in a row share one height, so a name that wraps never leaves a neighbour short.
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { option ->
                        CoverCard(label(option), glyph(option), labelStyle.scaled(fit), option == selected, { onSelect(option) },
                            Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CoverCard(label: String, glyph: ImageVector, labelStyle: TextStyle, selected: Boolean, onClick: () -> Unit,
                      modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val shape = RoundedCornerShape(AppSize.controlRadius + 2.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) .97f else 1f, AppMotion.PressScale, label = "cover press")
    val cover by animateFloatAsState(if (selected) 1f else 0f, tween(AppMotion.StateDuration, easing = AppMotion.Settle),
        label = "cover chosen")
    val badge by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow),
        label = "cover badge")
    // At rest the card is already tinted with the steel it turns into, so choosing it reads as the color filling in.
    val rest = lerp(colors.surface, nani.unitCoverStart, if (nani.isDark) .16f else .09f)
    // Only the chosen card stands off the page, and only by day; at night the cover's own light is enough.
    val lift = if (nani.isDark || cover == 0f) Modifier else Modifier.shadow(8.dp * cover, shape,
        ambientColor = nani.unitCoverEnd.copy(alpha = .14f), spotColor = nani.unitCoverEnd.copy(alpha = .3f))
    Row(modifier
        .graphicsLayer { scaleX = press; scaleY = press }
        .then(lift)
        .clip(shape)
        .background(rest)
        .engravedCover(label, unit = true, reveal = { cover })
        .border(1.dp, nani.unitCoverStart.copy(alpha = (if (nani.isDark) .34f else .2f) * (1f - cover)), shape)
        .selectable(selected, interaction, LocalIndication.current, role = Role.RadioButton, onClick = onClick)
        .heightIn(min = 64.dp)
        .padding(CoverPadding),
        verticalAlignment = Alignment.CenterVertically) {
        CoverMark(glyph, cover, size = CoverMarkSize, rest = if (nani.isDark) colors.surfaceContainerHighest else colors.surface,
            check = badge)
        Spacer(Modifier.width(CoverGap))
        Text(label, Modifier.weight(1f), style = labelStyle, color = lerp(colors.onSurface, nani.onPlaque, cover),
            maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private val CoverPadding = 12.dp
private val CoverMarkSize = 36.dp
private val CoverGap = 10.dp

/**
 * A place's mark, built like the unit's mark on its cover: the icon on a disc that turns steel and
 * is ringed in light once chosen. [rest] is the disc before that; [check] springs a small check
 * onto its corner.
 */
@Composable
internal fun CoverMark(
    glyph: ImageVector,
    cover: Float,
    size: Dp = 48.dp,
    rest: Color = NaniTheme.colors.unitMark.first,
    check: Float = 0f
) {
    val nani = NaniTheme.colors
    val (fill, ink) = nani.unitMark
    Box(Modifier.size(size)) {
        Box(Modifier.matchParentSize().background(nani.onPlaque.copy(alpha = .18f * cover), CircleShape).padding(3.dp)
            .background(lerp(rest, fill, cover), CircleShape), contentAlignment = Alignment.Center) {
            Icon(glyph, null, Modifier.size(size * .48f), tint = ink)
        }
        if (check > 0f) Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp)) {
            ChosenBadge(check, nani.onPlaque, nani.unitCoverEnd, size = 17.dp)
        }
    }
}

/** The check on a chosen card, springing in on its corner; [progress] runs from hidden to shown. */
@Composable
private fun ChosenBadge(progress: Float, fill: Color, ink: Color, size: Dp = 24.dp) {
    Box(Modifier.size(size).graphicsLayer { alpha = progress.coerceIn(0f, 1f); scaleX = progress; scaleY = progress }
        .background(fill, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, null, Modifier.size(size * .66f), tint = ink)
    }
}

/**
 * The bank a rent arrives through, as cards three to a row on a phone: each bank's official mark
 * on a coin over its name, the way a payment app lists banks. The chosen card takes on the bank's
 * own color, like a card picked out of a wallet, and its coin turns over to the white seal; "no
 * bank" turns ink-dark instead. No card changes size when chosen, so nothing moves while the
 * person taps through them.
 */
@Composable
internal fun BankChoices(banks: List<String>, selected: String, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val columns = ((maxWidth + gap) / (96.dp + gap)).toInt().coerceIn(2, 4)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            banks.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { bank -> BankCard(bank, bank == selected, { onSelect(bank) }, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BankCard(bank: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val none = bank == NoBank
    val mark = bankMarkOf(bank)
    val fill = mark?.fill ?: colors.inverseSurface
    val ink = mark?.ink ?: colors.inverseOnSurface
    val shape = RoundedCornerShape(AppSize.sheetRadius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) .96f else 1f, AppMotion.PressScale, label = "bank press")
    val chosen by animateFloatAsState(if (selected) 1f else 0f, tween(AppMotion.StateDuration, easing = AppMotion.Settle),
        label = "bank chosen")
    val badge by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow),
        label = "bank badge")
    val lift = if (nani.isDark || chosen == 0f) Modifier else Modifier.shadow(8.dp * chosen, shape,
        ambientColor = fill.copy(alpha = .18f), spotColor = fill.copy(alpha = .36f))
    // At rest the card is paper a shade off white, so the bank's own mark is what stands out.
    val rest = if (nani.isDark) colors.surfaceContainer else colors.surfaceContainerLow
    Box(modifier.graphicsLayer { scaleX = press; scaleY = press }.then(lift)) {
        Column(Modifier.fillMaxSize().clip(shape).background(rest)
            .drawBehind {
                if (chosen > 0f) {
                    // The bank's color, lit from the upper right like every cover in the app.
                    drawRect(Brush.linearGradient(listOf(lerp(fill, Color.White, .14f), fill), start = Offset(size.width, 0f),
                        end = Offset(0f, size.height)), alpha = chosen)
                    drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .16f), Color.Transparent),
                        center = Offset(size.width, 0f), radius = size.height), alpha = chosen)
                }
            }
            .border(1.dp, colors.outlineVariant.copy(alpha = 1f - chosen), shape)
            .selectable(selected, interaction, LocalIndication.current, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
            // Coins line up across a row; a name that wraps hangs below its coin instead of pushing it up.
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            // Once chosen the coin turns over to the bank's white seal, in a ring of light.
            Box(Modifier.size(48.dp).background(ink.copy(alpha = .22f * chosen), CircleShape), contentAlignment = Alignment.Center) {
                if (none) BankCoin(bank, size = 40.dp, none = true, ink = lerp(colors.outline, ink, chosen))
                else BankCoin(bank, size = 40.dp, turned = chosen)
            }
            Spacer(Modifier.height(8.dp))
            Text(bank, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = lerp(colors.onSurface, ink, chosen))
        }
        Box(Modifier.align(Alignment.TopEnd).padding(7.dp)) { ChosenBadge(badge, ink, fill, size = 20.dp) }
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
