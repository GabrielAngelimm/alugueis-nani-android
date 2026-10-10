package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.design.DialogText
import com.rentalvalidator.app.presentation.design.NaniConfirmDialog
import com.rentalvalidator.app.presentation.design.NaniEditor
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal enum class RegistrationKind { TENANT, UNIT }

/**
 * How the registration was opened: from the "+" of the rentals page, which asks what to register
 * first, or straight into one kind, as an empty list or a unit's page does. [unit] places a new
 * tenant in that unit from the start.
 */
@Immutable
internal data class RegistrationRequest(val kind: RegistrationKind? = null, val unit: String? = null)

/**
 * The guided registration of a tenant or a unit, in a full-screen editor. Back steps back one
 * question; closing with answers given asks before throwing them away. The drafts of both kinds
 * live here, so going back to the choice and changing one's mind keeps what was typed.
 */
@Composable
internal fun RegistrationEditor(
    request: RegistrationRequest,
    units: List<RentalUnit>,
    onDismiss: () -> Unit,
    isUnitNameTaken: suspend (String) -> Boolean,
    onSaveTenant: (Tenant, onError: (String) -> Unit) -> Unit,
    onSaveUnit: (RentalUnit, onError: (String) -> Unit) -> Unit
) {
    var kind by remember { mutableStateOf(request.kind) }
    val tenant = remember { TenantFlowState(TenantDraft(unit = request.unit?.ifBlank { null } ?: RentalUnit.GERAL_NAME)) }
    val unit = remember { UnitFlowState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val close = { if (tenant.hasAnswers || unit.hasAnswers) confirmDiscard = true else onDismiss() }
    // The first question goes back to the choice when there was one; otherwise it is the way out.
    val chose = request.kind == null
    val firstBack = { if (chose) kind = null else close() }
    val firstBackLabel = if (chose) "Voltar" else "Cancelar"
    val back = {
        when (kind) {
            null -> close()
            RegistrationKind.TENANT -> tenant.stepper.current.let { if (it == 0) firstBack() else tenant.stepper.goTo(it - 1) }
            RegistrationKind.UNIT -> unit.stepper.current.let { if (it == 0) firstBack() else unit.stepper.goTo(it - 1) }
        }
    }

    NaniEditor(onDismiss = back) {
        AnimatedContent(kind, Modifier.fillMaxSize(), label = "registration kind", transitionSpec = {
            val forward = initialState == null
            (slideInHorizontally(AppMotion.PageSlide) { if (forward) it / 4 else -it / 4 } + fadeIn(AppMotion.EnterFade)) togetherWith
                (slideOutHorizontally(AppMotion.PageSlide) { if (forward) -it / 6 else it / 6 } + fadeOut(AppMotion.ExitFade))
        }) { shown ->
            when (shown) {
                null -> RegistrationChooser(hasUnits = units.isNotEmpty(), onClose = close, onChoose = { kind = it })
                RegistrationKind.TENANT -> TenantRegistration(tenant, units, firstBackLabel, firstBack, close) { draft ->
                    tenant.saving = true
                    tenant.saveError = null
                    onSaveTenant(draft.toTenant(UUID.randomUUID().toString(), units, LocalDate.now().toString())) { message ->
                        tenant.saving = false
                        tenant.saveError = message
                    }
                }
                RegistrationKind.UNIT -> UnitRegistration(unit, firstBackLabel, firstBack, close, isUnitNameTaken) { draft ->
                    unit.saving = true
                    unit.saveError = null
                    onSaveUnit(draft.toUnit(UUID.randomUUID().toString(), Instant.now().toString())) { message ->
                        unit.saving = false
                        unit.saveError = message
                    }
                }
            }
        }
    }

    if (confirmDiscard) NaniConfirmDialog(
        title = "Descartar cadastro?",
        onDismiss = { confirmDiscard = false },
        confirmLabel = "Descartar",
        onConfirm = { confirmDiscard = false; onDismiss() },
        text = { DialogText("As respostas preenchidas até aqui serão perdidas.") },
        dismissLabel = "Continuar",
        destructive = true,
        icon = Icons.Rounded.DeleteOutline
    )
}

/** The first page behind the "+": two covers, one per kind of record, each saying what it will ask. */
@Composable
private fun RegistrationChooser(hasUnits: Boolean, onClose: () -> Unit, onChoose: (RegistrationKind) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        FlowTopBar("Novo cadastro", null, null, onClose)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(horizontal = AppSpace.page).padding(top = 12.dp, bottom = 32.dp)) {
            StepHeading("Locações", "O que você quer cadastrar?",
                "Uma pergunta por vez. No final, você confere tudo antes de salvar.")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                KindCard("Inquilino", "A pessoa que aluga: nome, unidade, valor do aluguel, vencimento e contato.",
                    "6 etapas, com revisão", Icons.Rounded.Person, unit = false) { onChoose(RegistrationKind.TENANT) }
                KindCard("Unidade", "O imóvel: tipo, nome, endereço, capacidade e situação.",
                    "5 etapas, com revisão", Icons.Rounded.Home, unit = true) { onChoose(RegistrationKind.UNIT) }
                if (!hasUnits) FlowHint(Icons.Rounded.Lightbulb,
                    "Ainda não há unidades. Cadastrando a unidade primeiro, o inquilino já tem onde morar.")
            }
        }
    }
}

/**
 * A kind of record as a small passbook: a strip of the cover that opens its detail page (blue for a
 * person, steel for a [unit]), with the kind's glyph lit on it, beside what the registration asks
 * and how long it is.
 */
@Composable
private fun KindCard(title: String, description: String, length: String, glyph: ImageVector, unit: Boolean, onClick: () -> Unit) {
    val nani = NaniTheme.colors
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(AppSize.sheetRadius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, AppMotion.PressScale, label = "kind press")
    val (lit, deep) = if (unit) nani.unitCoverStart to nani.unitCoverEnd else nani.coverStart to nani.coverEnd
    val lift = if (nani.isDark) Modifier else Modifier.shadow(10.dp, shape, ambientColor = deep.copy(alpha = .12f),
        spotColor = deep.copy(alpha = .22f))
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.then(lift),
        shape = shape, color = colors.surface, interactionSource = interaction) {
        Row(Modifier.height(IntrinsicSize.Min).heightIn(min = 128.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(96.dp).fillMaxHeight().drawBehind {
                drawRect(Brush.linearGradient(listOf(lit, deep), start = Offset(size.width, 0f),
                    end = Offset(0f, size.height)))
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .14f), Color.Transparent),
                    center = Offset(size.width, 0f), radius = size.height))
            }.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(nani.onPlaque.copy(alpha = .16f)).padding(3.dp)
                    .clip(CircleShape).background(nani.onPlaque.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Icon(glyph, null, Modifier.size(28.dp), tint = nani.onPlaque)
                }
            }
            Column(Modifier.weight(1f).padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(length, Modifier.clip(CircleShape).background(colors.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.padding(end = 12.dp).size(22.dp),
                tint = colors.onSurfaceVariant)
        }
    }
}
