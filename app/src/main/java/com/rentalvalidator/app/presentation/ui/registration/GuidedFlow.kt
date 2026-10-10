package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.LedgerRule
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme

/*
 * A guided registration asks one thing at a time. Above the question, a trail of steps says
 * where the person is: what was answered (with the answer itself), the step on screen and what
 * is left. Moving forward checks the step being left; moving back never checks anything and
 * never forgets anything, so answers survive every trip across the trail.
 */

/** How one step reads in the trail. */
@Immutable
internal data class TrailStep(
    val title: String,
    val optional: Boolean = false,
    /** The answer as the trail shows it once the step is behind the person; null shows the title. */
    val answer: String? = null,
    /** True when the review found something to fix in this step. */
    val hasError: Boolean = false
)

/** The step on screen and the furthest step the person has reached; only reached steps can be revisited. */
@Stable
internal class StepperState(private val count: Int) {
    var current by mutableIntStateOf(0)
        private set
    var furthest by mutableIntStateOf(0)
        private set

    val isLast: Boolean get() = current == count - 1

    fun goTo(step: Int) {
        current = step.coerceIn(0, count - 1)
        furthest = maxOf(furthest, current)
    }
}

/** The frame of a guided registration: title, progress, trail, the step itself and its two actions. */
@Composable
internal fun GuidedFlow(
    title: String,
    subtitle: String?,
    steps: List<TrailStep>,
    stepper: StepperState,
    onTrail: (Int) -> Unit,
    onClose: () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable AnimatedContentScope.(step: Int) -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        FlowTopBar(title, subtitle, "Etapa ${stepper.current + 1} de ${steps.size}", onClose)
        FlowProgress(stepper.current, stepper.furthest, steps.size)
        StepTrail(steps, stepper.current, stepper.furthest, onTrail)
        AnimatedContent(stepper.current, Modifier.weight(1f).fillMaxWidth(), label = "guided step", transitionSpec = {
            // Forward pages come from the right, like turning to the next page of the ledger.
            val forward = targetState > initialState
            (slideInHorizontally(AppMotion.PageSlide) { if (forward) it / 4 else -it / 4 } + fadeIn(AppMotion.EnterFade)) togetherWith
                (slideOutHorizontally(AppMotion.PageSlide) { if (forward) -it / 6 else it / 6 } + fadeOut(AppMotion.ExitFade))
        }) { step -> content(step) }
        footer()
    }
}

@Composable
internal fun FlowTopBar(title: String, subtitle: String?, counter: String?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = AppSpace.page, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Fechar") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (counter != null) Text(counter, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** A thin rule that fills as the person moves on; the reached part stays faintly inked when they step back. */
@Composable
private fun FlowProgress(current: Int, furthest: Int, count: Int) {
    val spring = spring<Float>(dampingRatio = .9f, stiffness = Spring.StiffnessLow)
    val now by animateFloatAsState((current + 1f) / count, spring, label = "progress now")
    val reached by animateFloatAsState((furthest + 1f) / count, spring, label = "progress reached")
    val ink = NaniTheme.colors.action
    Box(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).height(4.dp).clip(CircleShape)
        .background(NaniTheme.colors.track).clearAndSetSemantics { }) {
        Box(Modifier.fillMaxWidth(reached).height(4.dp).clip(CircleShape).background(ink.copy(alpha = .28f)))
        Box(Modifier.fillMaxWidth(now).height(4.dp).clip(CircleShape).background(ink))
    }
}

/**
 * The steps as a row of tabs on a ledger's edge. Steps behind the person show their answer and
 * can be opened again; the current one is inked; the ones ahead wait in pencil.
 */
@Composable
private fun StepTrail(steps: List<TrailStep>, current: Int, furthest: Int, onStep: (Int) -> Unit) {
    val list = rememberLazyListState()
    // Keep the step before the current one in view, so the last answer is always readable, unless
    // a long answer would push the current step past the edge: then the current step wins.
    LaunchedEffect(current) {
        list.animateScrollToItem((current - 1).coerceAtLeast(0))
        val info = list.layoutInfo
        val shown = info.visibleItemsInfo.firstOrNull { it.index == current }
        if (shown == null) list.animateScrollToItem(current)
        else (shown.offset + shown.size - (info.viewportEndOffset - info.afterContentPadding))
            .takeIf { it > 0 }?.let { list.animateScrollBy(it.toFloat()) }
    }
    LazyRow(Modifier.fillMaxWidth(), state = list, contentPadding = PaddingValues(horizontal = AppSpace.page, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(steps) { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) Box(Modifier.width(14.dp).height(1.dp).background(
                    if (index <= furthest) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant))
                TrailChip(index, steps.size, step, current = index == current, reached = index <= furthest,
                    onClick = { onStep(index) })
            }
        }
    }
}

@Composable
private fun TrailChip(index: Int, count: Int, step: TrailStep, current: Boolean, reached: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val done = reached && !current
    val fill by animateColorAsState(if (current) colors.primaryContainer else if (done) colors.surface else colors.background,
        tween(AppMotion.StateDuration), label = "trail fill")
    val stroke by animateColorAsState(when {
        current -> colors.primary
        done && step.hasError -> nani.overdue.ink
        else -> colors.outlineVariant
    }, tween(AppMotion.StateDuration), label = "trail stroke")
    val text = if (done) step.answer ?: step.title else step.title
    val state = when {
        current -> "etapa atual"
        step.hasError -> "precisa de correção"
        done -> "concluída"
        else -> "a seguir"
    }
    val spoken = "Etapa ${index + 1} de $count, ${step.title}" + (if (done && step.answer != null) ": ${step.answer}" else "") +
        if (step.optional) ", opcional" else ""
    Row(Modifier.heightIn(min = 36.dp).clip(CircleShape).background(fill).border(BorderStroke(1.dp, stroke), CircleShape)
        .then(if (done) Modifier.clickable(role = Role.Button, onClickLabel = "Voltar a esta etapa", onClick = onClick) else Modifier)
        // The click stays outside the cleared semantics, so a reached step is still announced as a button.
        .clearAndSetSemantics {
            contentDescription = spoken
            stateDescription = state
        }
        .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
        .animateContentSize(tween(AppMotion.LayoutDuration, easing = AppMotion.Settle)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrailDisc(index, current, done, step.hasError)
        Text(text, Modifier.widthIn(max = 168.dp), style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold),
            color = when {
                current -> colors.onPrimaryContainer
                done -> colors.onSurface
                else -> colors.onSurfaceVariant
            }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The step's number, a check once it is behind the person, or an alert when the review sends them back to it. */
@Composable
private fun TrailDisc(index: Int, current: Boolean, done: Boolean, error: Boolean) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val (fill, ink) = when {
        current -> nani.action to nani.onAction
        done && error -> nani.overdue.fill to nani.overdue.ink
        done -> nani.paid.fill to nani.paid.ink
        else -> colors.background to colors.onSurfaceVariant
    }
    Box(Modifier.size(26.dp).clip(CircleShape).background(fill)
        .then(if (!current && !done) Modifier.border(1.dp, colors.outlineVariant, CircleShape) else Modifier),
        contentAlignment = Alignment.Center) {
        when {
            done && error -> Icon(Icons.Rounded.PriorityHigh, null, Modifier.size(16.dp), tint = ink)
            done -> Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = ink)
            else -> Text("${index + 1}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ink)
        }
    }
}

/**
 * One question. A small kicker names the step, the question is written in the ledger's hand, a
 * line explains why it is asked, and the fields follow a beat after the page lands.
 */
@Composable
internal fun AnimatedVisibilityScope.StepPage(
    kicker: String,
    question: String,
    helper: String?,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = AppSpace.page).padding(top = 12.dp, bottom = 32.dp)) {
        StepHeading(kicker, question, helper, optional)
        Column(Modifier.fillMaxWidth().landing(this@StepPage), verticalArrangement = Arrangement.spacedBy(22.dp), content = content)
    }
}

/** The last page: the heading, a preview of the record as it will look, then the answers to confirm. */
@Composable
internal fun AnimatedVisibilityScope.ReviewPage(
    kicker: String,
    question: String,
    helper: String,
    preview: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 32.dp)) {
        Column(Modifier.padding(horizontal = AppSpace.page)) { StepHeading(kicker, question, helper, optional = false) }
        // The preview brings its own gutter, as it does at the top of a detail page.
        Box(Modifier.landing(this@ReviewPage)) { preview() }
        Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).landing(this@ReviewPage, delay = 150),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
internal fun StepHeading(kicker: String, question: String, helper: String?, optional: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.primary)
        if (optional) Text("Opcional", Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(10.dp))
    Text(question, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
    if (helper != null) {
        Spacer(Modifier.height(8.dp))
        Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(28.dp))
}

/** What a page holds rises into place a beat after the page itself, so the question is read first. */
@Composable
private fun Modifier.landing(page: AnimatedVisibilityScope, delay: Int = 90): Modifier {
    val rise = with(LocalDensity.current) { 20.dp.roundToPx() }
    return with(page) { this@landing.animateEnterExit(
        enter = fadeIn(tween(220, delayMillis = delay, easing = AppMotion.Settle)) +
            slideInVertically(tween(360, delayMillis = delay, easing = AppMotion.Settle)) { rise },
        exit = ExitTransition.None) }
}

/**
 * Back on the left, the way forward filling the rest. The forward label says what happens next
 * ("Avançar", "Revisar", "Cadastrar unidade"); both stack on a very narrow window.
 */
@Composable
internal fun FlowFooter(
    backLabel: String,
    onBack: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    busy: Boolean = false,
    nextIcon: ImageVector? = null
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            LedgerRule()
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AppSpace.page, vertical = AppSpace.medium)) {
                val label = if (busy) "Salvando…" else nextLabel
                if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.25f) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                        PrimaryButton(label, onNext, enabled = !busy, icon = nextIcon)
                        SecondaryButton(backLabel, onBack, enabled = !busy)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.medium)) {
                        SecondaryButton(backLabel, onBack, enabled = !busy, fill = false)
                        PrimaryButton(label, onNext, Modifier.weight(1f), enabled = !busy, icon = nextIcon)
                    }
                }
            }
        }
    }
}
