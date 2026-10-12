package com.rentalvalidator.app.presentation.ui.tenant_payments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.design.FittingText
import com.rentalvalidator.app.presentation.design.StatusKind
import com.rentalvalidator.app.presentation.design.coverRim
import com.rentalvalidator.app.presentation.design.engravedCover
import com.rentalvalidator.app.presentation.design.longLabel
import com.rentalvalidator.app.presentation.design.statusTone
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

/**
 * The tenant's year as one card, like the cover of a passbook with its months ruled below. The
 * cover is the tenant's own (the one on their detail page) and carries what the year amounts to:
 * what came in, how many months are paid and a verdict. Under it, the twelve months run in a row
 * that slides sideways; the chosen one is inked, and each carries its state as a shape.
 */
@Composable
internal fun YearCard(
    tenantName: String,
    year: Int,
    today: LocalDate,
    summary: YearSummary,
    months: List<Pair<YearMonth, MonthState>>,
    selected: YearMonth,
    stripState: LazyListState,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonth: (YearMonth) -> Unit
) {
    val nani = NaniTheme.colors
    val shape = RoundedCornerShape(AppSize.sheetRadius)
    // As on a detail page: by day the card lifts on a shadow tinted with the cover; at night the cover is enough.
    val lift = if (nani.isDark) Modifier else Modifier.shadow(14.dp, shape, ambientColor = nani.coverEnd.copy(alpha = .16f),
        spotColor = nani.coverEnd.copy(alpha = .3f))
    Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).then(lift), shape = shape,
        color = MaterialTheme.colorScheme.surface) {
        Column {
            // The cover's edge catches the light along the top, as on the tenant's own page.
            Column(Modifier.fillMaxWidth().engravedCover(tenantName, unit = false).coverRim(AppSize.sheetRadius)
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recebido no ano", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        color = nani.onPlaque.copy(alpha = .84f))
                    YearSwitcher(year, canGoBack, canGoForward, onPreviousYear, onNextYear)
                }
                AnimatedContent(summary.received to year, label = "year amount",
                    transitionSpec = {
                        val forward = targetState.second >= initialState.second
                        (slideInVertically(tween(AppMotion.LayoutDuration, easing = AppMotion.Settle)) { if (forward) it / 3 else -it / 3 } +
                            fadeIn(AppMotion.EnterFade)) togetherWith fadeOut(AppMotion.ExitFade)
                    }) { (amount, _) ->
                    FittingText(CurrencyUtils.format(amount), NaniType.moneyHero, Modifier.padding(end = 12.dp),
                        color = nani.onPlaque, minScale = .5f)
                }
                Text(paidLine(summary, year, today), style = MaterialTheme.typography.bodyMedium,
                    color = nani.onPlaque.copy(alpha = .84f))
                Spacer(Modifier.height(16.dp))
                VerdictPill(summary, year, today)
            }
            MonthStrip(months, selected, today, stripState, onMonth)
        }
    }
}

/** "10 de 10 meses pagos": months still to come are never counted against the tenant. */
private fun paidLine(summary: YearSummary, year: Int, today: LocalDate): String = when {
    summary.counted == 0 && year > today.year -> "Nenhum mês venceu ainda"
    summary.counted == 0 -> "Nenhum mês a cobrar"
    else -> "${summary.paid} de ${summary.counted} ${if (summary.counted == 1) "mês pago" else "meses pagos"}" +
        if (year == today.year) " até agora" else ""
}

/** The year on the cover, between arrows that step to the years around it. */
@Composable
private fun YearSwitcher(year: Int, canGoBack: Boolean, canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val ink = NaniTheme.colors.onPlaque
    Row(Modifier.clip(CircleShape).background(ink.copy(alpha = .12f)), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Ano anterior", tint = ink.copy(alpha = if (canGoBack) 1f else .35f))
        }
        AnimatedContent(year, label = "year",
            transitionSpec = { fadeIn(AppMotion.EnterFade) togetherWith fadeOut(AppMotion.ExitFade) }) { shown ->
            Text(shown.toString(), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium,
                color = ink)
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo ano", tint = ink.copy(alpha = if (canGoForward) 1f else .35f))
        }
    }
}

/** The year's verdict, most urgent first, in a capsule of the cover's light with the state's own mark. */
@Composable
private fun VerdictPill(summary: YearSummary, year: Int, today: LocalDate) {
    val verdict = summary.verdict()
    val (kind, glyph) = when (verdict) {
        YearVerdict.OVERDUE -> StatusKind.ERROR to Icons.Rounded.PriorityHigh
        YearVerdict.REVIEW -> StatusKind.INFO to Icons.Rounded.HourglassTop
        YearVerdict.UNRECORDED -> StatusKind.NEUTRAL to Icons.Rounded.Remove
        YearVerdict.UP_TO_DATE -> StatusKind.SUCCESS to Icons.Rounded.Check
        YearVerdict.NOT_STARTED -> StatusKind.NEUTRAL to Icons.Rounded.Schedule
    }
    val text = when (verdict) {
        YearVerdict.OVERDUE -> "${months(summary.overdue)} em atraso · ${CurrencyUtils.format(summary.overdueAmount)}"
        YearVerdict.REVIEW -> if (summary.review == 1) "1 pagamento em análise" else "${summary.review} pagamentos em análise"
        YearVerdict.UNRECORDED -> "${months(summary.unrecorded)} sem registro"
        YearVerdict.UP_TO_DATE -> "Tudo em dia"
        YearVerdict.NOT_STARTED -> if (year > today.year) "Ano ainda não começou" else "Antes do cadastro"
    }
    val tone = statusTone(kind)
    val ink = NaniTheme.colors.onPlaque
    Row(Modifier.clip(CircleShape).background(ink.copy(alpha = .14f)).semantics(mergeDescendants = true) { }
        .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(24.dp).background(tone.fill, CircleShape), contentAlignment = Alignment.Center) {
            Icon(glyph, null, Modifier.size(15.dp), tint = tone.ink)
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

private fun months(count: Int) = if (count == 1) "1 mês" else "$count meses"

private val ShortMonths = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

/**
 * The year's months in a row that slides sideways, like the month tabs of a card statement. The
 * chosen month is set in pen; the current one carries a dot, as in the month picker.
 */
@Composable
private fun MonthStrip(months: List<Pair<YearMonth, MonthState>>, selected: YearMonth, today: LocalDate, state: LazyListState,
    onMonth: (YearMonth) -> Unit) {
    LazyRow(Modifier.fillMaxWidth().fadedEdges(state).selectableGroup(), state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(months, key = { it.first.monthValue }) { (period, monthState) ->
            MonthTab(period, monthState, period == selected, period == YearMonth.from(today)) { onMonth(period) }
        }
    }
}

/** Softens whichever end of the row still has months beyond it, so the row reads as one that slides. */
private fun Modifier.fadedEdges(state: LazyListState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val edge = 28.dp.toPx()
        if (state.canScrollBackward) drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, endX = edge),
            size = Size(edge, size.height), blendMode = BlendMode.DstIn)
        if (state.canScrollForward) drawRect(Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent,
            startX = size.width - edge, endX = size.width), topLeft = Offset(size.width - edge, 0f),
            size = Size(edge, size.height), blendMode = BlendMode.DstIn)
    }

@Composable
private fun MonthTab(period: YearMonth, state: MonthState, selected: Boolean, current: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val fill by animateColorAsState(if (selected) nani.action else Color.Transparent, tween(AppMotion.StateDuration), label = "month tab")
    val quiet = state == MonthState.FUTURE || state == MonthState.BEFORE_TENANCY
    Column(
        Modifier.width(MonthTabWidth).clip(RoundedCornerShape(18.dp)).background(fill)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${period.longLabel()}: ${state.label.lowercase()}" + if (current) ", mês atual" else ""
                if (selected) stateDescription = "Exibido abaixo"
            }
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(ShortMonths[period.monthValue - 1], style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = when {
                selected -> nani.onAction
                quiet -> colors.onSurfaceVariant
                else -> colors.onSurface
            })
        Spacer(Modifier.height(8.dp))
        MonthGlyph(state, onInk = selected)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(4.dp).clearAndSetSemantics { }.background(when {
            !current -> Color.Transparent
            selected -> nani.onAction
            else -> colors.primary
        }, CircleShape))
    }
}

internal val MonthTabWidth = 56.dp

/**
 * A month's state as a small mark that keeps the app's shapes: a filled seal for paid, a
 * highlighter disc for what is open, a dashed ring for review, a solid ring for late, a hollow
 * ring for what has not come and a dash where nothing was recorded.
 */
@Composable
internal fun MonthGlyph(state: MonthState, onInk: Boolean = false, size: Dp = 26.dp) {
    val tone = statusTone(state.kind)
    val colors = MaterialTheme.colorScheme
    val hollow = if (onInk) NaniTheme.colors.onAction.copy(alpha = .6f) else colors.outlineVariant
    val ring: Modifier = when (state) {
        MonthState.REVIEW -> Modifier.drawBehind {
            val stroke = 1.3.dp.toPx()
            drawCircle(tone.ink, radius = this.size.minDimension / 2 - stroke / 2, style = Stroke(stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))))
        }
        MonthState.OVERDUE -> Modifier.border(1.3.dp, tone.ink, CircleShape)
        MonthState.FUTURE -> Modifier.border(1.3.dp, hollow, CircleShape)
        else -> Modifier
    }
    val fill = when (state) {
        MonthState.PAID, MonthState.OVERDUE, MonthState.DUE_TODAY, MonthState.UPCOMING -> tone.fill
        MonthState.REVIEW -> tone.fill.copy(alpha = if (onInk) 1f else .6f)
        else -> Color.Transparent
    }
    Box(Modifier.size(size).clearAndSetSemantics { }.background(fill, CircleShape).then(ring), contentAlignment = Alignment.Center) {
        when (val glyph = state.glyph) {
            null -> if (state == MonthState.UNRECORDED || state == MonthState.BEFORE_TENANCY) Box(Modifier.drawBehind {
                drawLine(hollow.copy(alpha = if (state == MonthState.BEFORE_TENANCY) .5f else 1f),
                    Offset(this.size.width * .32f, this.size.height / 2), Offset(this.size.width * .68f, this.size.height / 2),
                    strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }.size(size))
            else -> Icon(glyph, null, Modifier.size(size * .6f), tint = tone.ink)
        }
    }
}

internal val MonthState.kind: StatusKind
    get() = when (this) {
        MonthState.PAID -> StatusKind.SUCCESS
        MonthState.REVIEW -> StatusKind.INFO
        MonthState.OVERDUE -> StatusKind.ERROR
        MonthState.DUE_TODAY, MonthState.UPCOMING -> StatusKind.WARNING
        MonthState.FUTURE, MonthState.UNRECORDED, MonthState.BEFORE_TENANCY -> StatusKind.NEUTRAL
    }

internal val MonthState.label: String
    get() = when (this) {
        MonthState.PAID -> "Pago"
        MonthState.REVIEW -> "Em análise"
        MonthState.OVERDUE -> "Em atraso"
        MonthState.DUE_TODAY -> "Vence hoje"
        MonthState.UPCOMING, MonthState.FUTURE -> "A vencer"
        MonthState.UNRECORDED -> "Sem registro"
        MonthState.BEFORE_TENANCY -> "Antes do cadastro"
    }

internal val MonthState.glyph: ImageVector?
    get() = when (this) {
        MonthState.PAID -> Icons.Rounded.Check
        MonthState.REVIEW -> Icons.Rounded.HourglassTop
        MonthState.OVERDUE -> Icons.Rounded.PriorityHigh
        MonthState.DUE_TODAY, MonthState.UPCOMING -> Icons.Rounded.Schedule
        MonthState.FUTURE, MonthState.UNRECORDED, MonthState.BEFORE_TENANCY -> null
    }
