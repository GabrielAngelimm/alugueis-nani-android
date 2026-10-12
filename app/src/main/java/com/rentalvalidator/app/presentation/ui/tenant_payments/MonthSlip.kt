package com.rentalvalidator.app.presentation.ui.tenant_payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.FittingText
import com.rentalvalidator.app.presentation.design.StatusMark
import com.rentalvalidator.app.presentation.design.dueDateIn
import com.rentalvalidator.app.presentation.design.longLabel
import com.rentalvalidator.app.presentation.design.statusTone
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One month as a coupon of a rent booklet: the month, its due date, its mark and the rent above
 * a perforated fold; below it, what the month needs and the actions that settle it. The mark is
 * the same stamp, highlighter or outline used across the app, and lands like a stamp when the
 * month is marked as paid.
 */
@Composable
internal fun MonthSlip(
    period: YearMonth,
    state: MonthState,
    rent: Double,
    dueDay: Int,
    today: LocalDate,
    firstName: String,
    onMarkPaid: () -> Unit,
    onChangeStatus: () -> Unit,
    onMessage: (() -> Unit)?,
    stubMinHeight: Dp,
    onStubMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().clip(CouponShape)
            .background(colors.surface).padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp)) {
            // The mark sits beside the month only, so the due date below it keeps the full width on one line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(period.longLabel(), Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface)
                Spacer(Modifier.width(12.dp))
                StatusMark(state.label, state.kind)
            }
            Text("Vencimento · ${dueLabel(dueDateIn(period, dueDay))}", Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            FittingText(CurrencyUtils.format(rent), NaniType.moneyLarge.copy(fontSize = NaniType.moneyLarge.fontSize * 1.25f,
                lineHeight = NaniType.moneyLarge.lineHeight * 1.2f), color = colors.onSurface)
            Text("Aluguel do mês", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        // Every coupon's stub takes the height of the tallest one seen, so coupons side by side always match.
        Column(Modifier.fillMaxWidth().onSizeChanged { onStubMeasured(it.height) }.heightIn(min = stubMinHeight)
            .clip(StubShape).background(colors.surface).perforation(SlipNotch)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            SlipSentence(state, period, dueDay, today, firstName)
            SlipActions(state, onMarkPaid, onChangeStatus, onMessage, Modifier.padding(top = 14.dp))
        }
    }
}

private val SlipNotch = 10.dp
private val CouponShape = SlipShape(AppSize.sheetRadius, SlipNotch, notchBottom = true)
private val StubShape = SlipShape(AppSize.sheetRadius, SlipNotch, notchTop = true)

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")
private val DueDay = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)
private val DayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)

/** "segunda, 5 de outubro": the weekday without its "-feira", so the line stays short. */
private fun dueLabel(date: LocalDate): String = date.format(DueDay).replace("-feira", "")

/** What the month needs, in a sentence led by the month's own mark. */
@Composable
private fun SlipSentence(state: MonthState, period: YearMonth, dueDay: Int, today: LocalDate, firstName: String) {
    val tone = statusTone(state.kind)
    val days = daysUntilDue(period, dueDay, today)
    val text = when (state) {
        MonthState.PAID -> "Aluguel recebido."
        MonthState.REVIEW -> "Aguardando conferência."
        MonthState.OVERDUE -> when {
            days == -1L -> "Venceu ontem."
            days >= -45 -> "Venceu há ${-days} dias."
            else -> "Em aberto desde ${dueDateIn(period, dueDay).format(DayMonth)}."
        }
        MonthState.DUE_TODAY -> "Vence hoje."
        MonthState.UPCOMING -> if (days == 1L) "Vence amanhã." else "Vence em $days dias."
        MonthState.FUTURE -> "Ainda não venceu."
        MonthState.UNRECORDED -> "Nenhum pagamento registrado."
        MonthState.BEFORE_TENANCY -> "Antes do cadastro de $firstName."
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MonthGlyph(state, size = 22.dp)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = if (state == MonthState.OVERDUE) tone.ink else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One strong action per month, the one the month asks for, with quieter ones under it. "Mudar
 * situação" is always there, so any month can still be set to any state, as before. [onMessage]
 * opens WhatsApp: a charge for a late month, a confirmation of receipt for a paid one.
 */
@Composable
private fun SlipActions(state: MonthState, onMarkPaid: () -> Unit, onChangeStatus: () -> Unit, onMessage: (() -> Unit)?,
    modifier: Modifier = Modifier) {
    val chargeable = state == MonthState.OVERDUE || state == MonthState.DUE_TODAY
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (state) {
            MonthState.PAID -> SecondaryButton("Mudar situação", onChangeStatus, icon = Icons.Rounded.Edit)
            MonthState.FUTURE, MonthState.BEFORE_TENANCY -> SecondaryButton("Marcar como pago", onMarkPaid, icon = Icons.Rounded.Check)
            MonthState.REVIEW -> PrimaryButton("Confirmar pagamento", onMarkPaid, icon = Icons.Rounded.Check)
            else -> PrimaryButton("Marcar como pago", onMarkPaid, icon = Icons.Rounded.Check)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onMessage != null && state == MonthState.PAID) QuietAction("Confirmar no WhatsApp", Icons.AutoMirrored.Rounded.Chat, onMessage)
            if (onMessage != null && chargeable) QuietAction("Cobrar", Icons.AutoMirrored.Rounded.Chat, onMessage)
            if (state != MonthState.PAID) QuietAction("Mudar situação", Icons.Rounded.Edit, onChangeStatus)
        }
    }
}

@Composable
private fun QuietAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick, Modifier.heightIn(min = 48.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** The fold between coupon and stub: a dashed rule between the two notches, on the stub's top edge. */
@Composable
private fun Modifier.perforation(notch: Dp): Modifier {
    val rule = MaterialTheme.colorScheme.outlineVariant
    return drawBehind {
        val inset = notch.toPx() + 8.dp.toPx()
        drawLine(rule, Offset(inset, 0f), Offset(size.width - inset, 0f), strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
    }
}

/**
 * A rounded sheet whose top or bottom corners are bitten by a round notch instead, so two of them
 * stacked read as one coupon folded along a perforation.
 */
internal class SlipShape(private val corner: Dp, private val notch: Dp, private val notchTop: Boolean = false,
    private val notchBottom: Boolean = false) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { corner.toPx() }.coerceAtMost(size.minDimension / 2)
        val n = with(density) { notch.toPx() }.coerceAtMost(size.minDimension / 2)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (notchTop) {
                moveTo(0f, n)
                arcTo(Rect(-n, -n, n, n), 90f, -90f, false)
                lineTo(w - n, 0f)
                arcTo(Rect(w - n, -n, w + n, n), 180f, -90f, false)
            } else {
                moveTo(0f, c)
                arcTo(Rect(0f, 0f, 2 * c, 2 * c), 180f, 90f, false)
                lineTo(w - c, 0f)
                arcTo(Rect(w - 2 * c, 0f, w, 2 * c), 270f, 90f, false)
            }
            if (notchBottom) {
                lineTo(w, h - n)
                arcTo(Rect(w - n, h - n, w + n, h + n), 270f, -90f, false)
                lineTo(n, h)
                arcTo(Rect(-n, h - n, n, h + n), 0f, -90f, false)
            } else {
                lineTo(w, h - c)
                arcTo(Rect(w - 2 * c, h - 2 * c, w, h), 0f, 90f, false)
                lineTo(c, h)
                arcTo(Rect(0f, h - 2 * c, 2 * c, h), 90f, 90f, false)
            }
            close()
        }
        return Outline.Generic(path)
    }
}
