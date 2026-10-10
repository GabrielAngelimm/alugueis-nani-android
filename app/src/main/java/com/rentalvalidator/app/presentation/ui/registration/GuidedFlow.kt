package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.SecondaryButton
import com.rentalvalidator.app.presentation.design.LedgerRule
import com.rentalvalidator.app.presentation.design.engravedCover
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniSerif
import com.rentalvalidator.app.presentation.theme.NaniTheme

/*
 * A guided registration asks one thing at a time. Above the question, one step rule says where
 * the person is: the steps behind (checked, and open to a tap), the step on screen, named under
 * the rule, and what is left. Answers are read on the review, not on the rule. Moving forward
 * checks the step being left; moving back never checks anything and never forgets anything, so
 * answers survive every trip along the rule.
 */

/** How one step reads on the step rule. */
@Immutable
internal data class TrailStep(
    val title: String,
    val optional: Boolean = false,
    /** The answer, read out to assistive technology once the step is behind the person. */
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

/**
 * The frame of a guided registration: title, step rule, the record taking shape ([preview], when
 * given), the step itself and its two actions. The preview stands back on the review, which shows
 * the full cover, and on a window too short for it (a small phone with the keyboard up), so the
 * question always keeps its room.
 */
@Composable
internal fun GuidedFlow(
    title: String,
    subtitle: String?,
    steps: List<TrailStep>,
    stepper: StepperState,
    onTrail: (Int) -> Unit,
    onClose: () -> Unit,
    preview: (@Composable () -> Unit)? = null,
    footer: @Composable () -> Unit,
    content: @Composable AnimatedContentScope.(step: Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val roomy = maxHeight >= PreviewRoom
        Column(Modifier.fillMaxSize()) {
            FlowTopBar(title, subtitle, null, onClose)
            StepIndicator(steps, stepper.current, stepper.furthest, compact = keyboardOpen(), onStep = onTrail)
            if (preview != null) AnimatedVisibility(roomy && !stepper.isLast,
                enter = fadeIn(AppMotion.EnterFade) + expandVertically(), exit = fadeOut(AppMotion.ExitFade) + shrinkVertically()) {
                preview()
            }
            AnimatedContent(stepper.current, Modifier.weight(1f).fillMaxWidth(), label = "guided step", transitionSpec = {
                // Forward pages come from the right, like turning to the next page of the ledger.
                val forward = targetState > initialState
                (slideInHorizontally(AppMotion.PageSlide) { if (forward) it / 4 else -it / 4 } + fadeIn(AppMotion.EnterFade)) togetherWith
                    (slideOutHorizontally(AppMotion.PageSlide) { if (forward) -it / 6 else it / 6 } + fadeOut(AppMotion.ExitFade))
            }) { step -> content(step) }
            footer()
        }
    }
}

/** Below this height the preview gives its room to the question. */
private val PreviewRoom = 500.dp

/** The name on a preview: the cover's serif, a size down so it fits one line beside the mark. */
private val PreviewName = TextStyle(fontFamily = NaniSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp,
    lineHeight = 24.sp, letterSpacing = (-0.2).sp)

/**
 * The record taking shape above the questions, the way a payment app draws the card being added:
 * a blank card outlined in pencil, with the [placeholder] and [linePlaceholder] in grey, until the
 * record has a [name]; then the cover itself, engraved from [seed] (ruled steel for a [unit]),
 * filling in answer by answer in [line], on a second line when it needs one. [mark] draws the disc
 * on the left, given how far the cover has come in.
 */
@Composable
internal fun FlowPreview(
    seed: String,
    unit: Boolean,
    name: String,
    placeholder: String,
    line: String?,
    linePlaceholder: String,
    mark: @Composable (cover: Float) -> Unit
) {
    val nani = NaniTheme.colors
    val colors = MaterialTheme.colorScheme
    val cover by animateFloatAsState(if (name.isNotBlank()) 1f else 0f, tween(AppMotion.PageDuration, easing = AppMotion.Settle),
        label = "preview cover")
    val shape = RoundedCornerShape(AppSize.sheetRadius)
    val deep = if (unit) nani.unitCoverEnd else nani.coverEnd
    val lift = if (nani.isDark || cover == 0f) Modifier else Modifier.shadow(10.dp * cover, shape,
        ambientColor = deep.copy(alpha = .14f), spotColor = deep.copy(alpha = .3f))
    val pencil = colors.outline
    Row(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(bottom = 12.dp)
        .then(lift).clip(shape).background(colors.surface)
        .engravedCover(seed, unit, reveal = { cover })
        .drawWithContent {
            drawContent()
            val outline = 1f - cover
            if (outline > 0f) {
                val stroke = 1.2.dp.toPx()
                drawRoundRect(pencil, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke),
                    CornerRadius(AppSize.sheetRadius.toPx()), alpha = .55f * outline,
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
            }
        }
        .semantics(mergeDescendants = true) { }
        .heightIn(min = 76.dp)
        .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        mark(cover)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name.trim().ifBlank { placeholder }, style = PreviewName,
                color = lerp(colors.onSurfaceVariant, nani.onPlaque, cover), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line ?: linePlaceholder, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (line == null) lerp(colors.outline, nani.onPlaque.copy(alpha = .62f), cover)
                else lerp(colors.onSurfaceVariant, nani.onPlaque.copy(alpha = .86f), cover))
        }
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

/** A stop on the step rule takes this much room, so a reached step is easy to tap. */
private val StopTouch = 40.dp

/**
 * Where the person is, in one piece: a rule of numbered stops, inked up to the step on screen, and
 * under it the step's name and how far along it is. Stops behind the person carry a check and open
 * again on a tap; the ones ahead wait in pencil. While the keyboard is up only the rule stays, so
 * the question keeps its room.
 */
@Composable
private fun StepIndicator(steps: List<TrailStep>, current: Int, furthest: Int, compact: Boolean, onStep: (Int) -> Unit) {
    val nani = NaniTheme.colors
    val colors = MaterialTheme.colorScheme
    val last = (steps.size - 1).coerceAtLeast(1)
    val ink by animateFloatAsState(current.toFloat() / last, tween(AppMotion.PageDuration, easing = AppMotion.Settle),
        label = "rule ink")
    val seen by animateFloatAsState(furthest.toFloat() / last, tween(AppMotion.PageDuration, easing = AppMotion.Settle),
        label = "rule seen")
    // The stops sit a half-touch inside the gutter, so the first and last discs line up with the page text.
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page - 8.dp).padding(top = 2.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth().drawBehind {
            val start = StopTouch.toPx() / 2
            val length = size.width - StopTouch.toPx()
            val y = size.height / 2
            val stroke = 2.dp.toPx()
            fun rule(to: Float, color: Color) = drawLine(color, Offset(start, y), Offset(start + length * to, y), stroke, StrokeCap.Round)
            rule(1f, nani.track)
            rule(seen, nani.action.copy(alpha = .3f))
            rule(ink, nani.action)
        }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { index, step ->
                StepStop(index, steps.size, step, current = index == current, reached = index <= furthest) { onStep(index) }
            }
        }
        AnimatedVisibility(!compact, enter = fadeIn(AppMotion.EnterFade) + expandVertically(),
            exit = fadeOut(AppMotion.ExitFade) + shrinkVertically()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedContent(steps[current], Modifier.weight(1f), label = "step name", transitionSpec = {
                    fadeIn(AppMotion.EnterFade) togetherWith fadeOut(AppMotion.ExitFade)
                }) { step ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(step.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (step.optional) Text("Opcional", Modifier.clip(CircleShape).background(colors.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant, maxLines = 1)
                    }
                }
                Text("Etapa ${current + 1} de ${steps.size}", style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StepStop(index: Int, count: Int, step: TrailStep, current: Boolean, reached: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val nani = NaniTheme.colors
    val done = reached && !current
    val size by animateDpAsState(if (current) 30.dp else 24.dp, tween(AppMotion.StateDuration, easing = AppMotion.Settle),
        label = "stop size")
    val halo by animateFloatAsState(if (current) 1f else 0f, tween(AppMotion.StateDuration), label = "stop halo")
    val (fill, ink) = when {
        current -> nani.action to nani.onAction
        done && step.hasError -> nani.overdue.fill to nani.overdue.ink
        done -> nani.action to nani.onAction
        else -> colors.background to colors.onSurfaceVariant
    }
    val state = when {
        current -> "etapa atual"
        step.hasError -> "precisa de correção"
        done -> "concluída"
        else -> "a seguir"
    }
    val spoken = "Etapa ${index + 1} de $count, ${step.title}" + (if (done && step.answer != null) ": ${step.answer}" else "") +
        if (step.optional) ", opcional" else ""
    Box(Modifier.size(StopTouch).clip(CircleShape)
        .then(if (done) Modifier.clickable(role = Role.Button, onClickLabel = "Voltar a esta etapa", onClick = onClick) else Modifier)
        // The click stays outside the cleared semantics, so a reached step is still announced as a button.
        .clearAndSetSemantics {
            contentDescription = spoken
            stateDescription = state
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(StopTouch - 2.dp).graphicsLayer { alpha = halo }.background(nani.action.copy(alpha = .14f), CircleShape))
        Box(Modifier.size(size).background(fill, CircleShape)
            .then(if (!current && !done) Modifier.border(1.5.dp, colors.outlineVariant, CircleShape) else Modifier),
            contentAlignment = Alignment.Center) {
            when {
                done && step.hasError -> Icon(Icons.Rounded.PriorityHigh, null, Modifier.size(14.dp), tint = ink)
                done -> Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = ink)
                else -> Text("${index + 1}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ink)
            }
        }
    }
}

/** Text scrolled under the top of a page fades out instead of being cut by the edge. */
private fun Modifier.fadeUnderTop(scroll: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val edge = minOf(scroll.value.toFloat(), 24.dp.toPx())
        if (edge > 0f) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = edge),
            size = Size(size.width, edge), blendMode = BlendMode.DstIn)
    }

/** True while the soft keyboard covers part of the window; the registration editor lays out behind it. */
@Composable
internal fun keyboardOpen(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/**
 * One question. The step's name sits above, in the step rule, so the page opens on the question
 * itself, written in the ledger's hand, with a line on why it is asked; the fields follow a beat
 * after the page lands. While the keyboard is up the line on why steps aside, so the question and
 * the field being typed in stay on screen together.
 */
@Composable
internal fun AnimatedVisibilityScope.StepPage(
    question: String,
    helper: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().fadeUnderTop(scroll).verticalScroll(scroll)
        .padding(horizontal = AppSpace.page).padding(top = 16.dp, bottom = 32.dp)) {
        StepHeading(null, question, helper, compact = keyboardOpen())
        Column(Modifier.fillMaxWidth().landing(this@StepPage), verticalArrangement = Arrangement.spacedBy(22.dp), content = content)
    }
}

/** The last page: the heading, a preview of the record as it will look, then the answers to confirm. */
@Composable
internal fun AnimatedVisibilityScope.ReviewPage(
    question: String,
    helper: String,
    preview: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().fadeUnderTop(scroll).verticalScroll(scroll).padding(top = 16.dp, bottom = 32.dp)) {
        Column(Modifier.padding(horizontal = AppSpace.page)) { StepHeading(null, question, helper) }
        // The preview brings its own gutter, as it does at the top of a detail page.
        Box(Modifier.landing(this@ReviewPage)) { preview() }
        Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).landing(this@ReviewPage, delay = 150),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** A small [kicker] (where there is no step rule to name the page), the question and why it is asked. */
@Composable
internal fun StepHeading(kicker: String?, question: String, helper: String?, compact: Boolean = false) {
    if (kicker != null) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
    }
    Text(question, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
    AnimatedVisibility(helper != null && !compact, enter = fadeIn(AppMotion.EnterFade) + expandVertically(),
        exit = fadeOut(AppMotion.ExitFade) + shrinkVertically()) {
        Text(helper.orEmpty(), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
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
