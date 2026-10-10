package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.design.DetailIdentity
import com.rentalvalidator.app.presentation.design.NaniChoice
import com.rentalvalidator.app.presentation.design.NaniDetailHero
import com.rentalvalidator.app.presentation.design.NaniTabs
import com.rentalvalidator.app.presentation.ui.tenants.capacityKindLabels
import com.rentalvalidator.app.presentation.ui.tenants.displayName
import com.rentalvalidator.app.presentation.ui.tenants.glyph
import com.rentalvalidator.app.presentation.ui.tenants.noun
import com.rentalvalidator.app.presentation.ui.tenants.statusLabels
import com.rentalvalidator.app.presentation.ui.tenants.validMoney
import com.rentalvalidator.app.util.CurrencyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything a unit's registration holds while it is open. */
@Stable
internal class UnitFlowState {
    val start = UnitDraft()
    var draft by mutableStateOf(start)
    val stepper = StepperState(UnitStep.entries.size)
    var shown by mutableStateOf<Map<UnitField, String>>(emptyMap())
    var focus by mutableStateOf<UnitField?>(null)
    /** True while the name is being checked against the registered units. */
    var checking by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var saveError by mutableStateOf<String?>(null)

    val step: UnitStep get() = UnitStep.entries[stepper.current]
    val hasAnswers: Boolean get() = draft != start

    fun edit(field: UnitField?, change: UnitDraft.() -> UnitDraft) {
        draft = draft.change()
        if (field != null) shown = shown - field
        saveError = null
    }

    private fun reject(errors: Map<UnitField, String>) {
        shown = shown + errors
        focus = errors.keys.first()
    }

    /**
     * Leaves the current step forward when it has nothing to fix. The name step also asks whether
     * another unit already has the name, so the person learns it where they can change it.
     */
    suspend fun advance(isNameTaken: suspend (String) -> Boolean, onSave: (UnitDraft) -> Unit) {
        if (step == UnitStep.REVIEW) {
            val invalid = draft.firstInvalidStep()
            if (invalid == null) onSave(draft) else {
                shown = draft.errors(UnitStep.REVIEW)
                stepper.goTo(invalid.ordinal)
                focus = draft.errors(invalid).keys.first()
            }
            return
        }
        val errors = draft.errors(step)
        if (errors.isNotEmpty()) return reject(errors)
        if (step == UnitStep.IDENTITY) {
            checking = true
            val taken = try { isNameTaken(draft.name.trim()) } finally { checking = false }
            if (taken) return reject(mapOf(UnitField.NAME to DuplicateUnitName))
        }
        stepper.goTo(if (stepper.furthest == UnitStep.REVIEW.ordinal) UnitStep.REVIEW.ordinal else stepper.current + 1)
    }

    fun open(target: UnitStep) {
        if (target.ordinal > stepper.current && step != UnitStep.REVIEW) {
            val errors = draft.errors(step)
            if (errors.isNotEmpty()) return reject(errors)
        }
        stepper.goTo(target.ordinal)
    }

    fun nextLabel(): String = when {
        step == UnitStep.REVIEW -> "Cadastrar unidade"
        stepper.furthest == UnitStep.REVIEW.ordinal || step.ordinal == UnitStep.REVIEW.ordinal - 1 -> "Revisar"
        else -> "Avançar"
    }
}

/** The message the units' repository check gives, repeated where the name is typed. */
internal const val DuplicateUnitName = "Já existe uma unidade com este nome."

private fun UnitField.step(): UnitStep = when (this) {
    UnitField.NAME, UnitField.LOCATION -> UnitStep.IDENTITY
    UnitField.CAPACITY -> UnitStep.CAPACITY
    UnitField.FEE -> UnitStep.DETAILS
}

/** "esta casa" and "nesta casa": how a question points at the property once its kind is known. */
private fun UnitType.thisOne(): Pair<String, String> = when (this) {
    UnitType.APARTMENT -> "este apartamento" to "neste apartamento"
    UnitType.HOUSE -> "esta casa" to "nesta casa"
    UnitType.COMMERCIAL_ROOM -> "esta sala" to "nesta sala"
    UnitType.BUILDING -> "este prédio" to "neste prédio"
    UnitType.KITNET -> "esta kitnet" to "nesta kitnet"
    UnitType.OTHER -> "esta unidade" to "nesta unidade"
}

/** The kinds in the order they are usually let: homes first, then whole buildings and shops. */
private val TypeOrder = listOf(UnitType.HOUSE, UnitType.APARTMENT, UnitType.KITNET, UnitType.BUILDING,
    UnitType.COMMERCIAL_ROOM, UnitType.OTHER)

/** What each kind covers, in a line under its name, so the choice is made without guessing. */
private fun UnitType.hint(): String = when (this) {
    UnitType.HOUSE -> "Com entrada própria"
    UnitType.APARTMENT -> "Em prédio ou condomínio"
    UnitType.KITNET -> "Compacta, de um só cômodo"
    UnitType.BUILDING -> "Várias unidades num só endereço"
    UnitType.COMMERCIAL_ROOM -> "Loja, escritório ou consultório"
    UnitType.OTHER -> "Quarto, vaga, galpão e outros"
}

private val statusNotes = mapOf(
    OperationalStatus.ACTIVE to "Recebendo inquilinos",
    OperationalStatus.INACTIVE to "Fora de uso por enquanto",
    OperationalStatus.MAINTENANCE to "Em obra ou reparo"
)

@Composable
internal fun UnitRegistration(
    flow: UnitFlowState,
    firstBackLabel: String,
    onFirstBack: () -> Unit,
    onClose: () -> Unit,
    isNameTaken: suspend (String) -> Boolean,
    onSave: (UnitDraft) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val requesters = remember { UnitField.entries.associateWith { FocusRequester() } }
    val notes = remember { FocusRequester() }
    LaunchedEffect(flow.stepper.current) { focusManager.clearFocus() }
    LaunchedEffect(flow.focus, flow.stepper.current) {
        val field = flow.focus ?: return@LaunchedEffect
        delay(120)
        if (runCatching { requesters.getValue(field).requestFocus() }.isSuccess) keyboard?.show()
        flow.focus = null
    }
    val advance: () -> Unit = { if (!flow.checking) scope.launch { flow.advance(isNameTaken, onSave) } }
    val draft = flow.draft
    val (thisOne, inThisOne) = draft.type.thisOne()
    val steps = UnitStep.entries.map { step ->
        TrailStep(step.title, step.optional, draft.summary(step), hasError = flow.shown.keys.any { it.step() == step })
    }
    GuidedFlow(
        title = "Nova unidade",
        subtitle = null,
        steps = steps,
        stepper = flow.stepper,
        onTrail = { flow.open(UnitStep.entries[it]) },
        onClose = onClose,
        footer = {
            FlowFooter(
                backLabel = if (flow.stepper.current == 0) firstBackLabel else "Voltar",
                onBack = { if (flow.stepper.current == 0) onFirstBack() else flow.stepper.goTo(flow.stepper.current - 1) },
                nextLabel = flow.nextLabel(),
                onNext = advance,
                busy = flow.saving,
                nextIcon = if (flow.step == UnitStep.REVIEW) Icons.Rounded.Check else null
            )
        }
    ) { index ->
        val error = { field: UnitField -> flow.shown[field] }
        when (UnitStep.entries[index]) {
            UnitStep.TYPE -> StepPage("Que tipo de imóvel é?",
                "O tipo dá o ícone da unidade, que aparece nas listas e na página dela.") {
                TypeCards(TypeOrder, draft.type, { it.displayName() }, { it.hint() }, { it.glyph() }) { type ->
                    flow.edit(null) { copy(type = type) }
                }
            }
            UnitStep.IDENTITY -> StepPage("Como se chama $thisOne e onde fica?",
                "O nome aparece nas listas e nos filtros; o endereço, na página da unidade.") {
                val name = requesters.getValue(UnitField.NAME)
                LaunchedEffect(Unit) { if (flow.draft.name.isEmpty()) { delay(AppMotion.PageDuration.toLong()); runCatching { name.requestFocus() } } }
                NaniTextField(draft.name, { flow.edit(UnitField.NAME) { copy(name = it) } }, "Nome da unidade", required = true,
                    helperText = "Exemplo: Casa Azul, Apto 302",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { requesters.getValue(UnitField.LOCATION).requestFocus() } }),
                    isError = error(UnitField.NAME) != null, errorMessage = error(UnitField.NAME), focusRequester = name)
                NaniTextField(draft.location, { flow.edit(UnitField.LOCATION) { copy(location = it) } }, "Endereço", required = true,
                    helperText = "Rua, número e bairro",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { advance() }),
                    isError = error(UnitField.LOCATION) != null, errorMessage = error(UnitField.LOCATION),
                    focusRequester = requesters.getValue(UnitField.LOCATION))
            }
            UnitStep.CAPACITY -> StepPage("Quanto cabe $inThisOne?",
                "A capacidade define a ocupação mostrada na lista, como 2 de 4.") {
                Column {
                    ControlLabel("Contar por")
                    NaniTabs(CapacityKind.entries.map { capacityKindLabels.getValue(it) }, draft.capacityKind.ordinal,
                        { flow.edit(null) { copy(capacityKind = CapacityKind.entries[it]) } }, role = Role.RadioButton)
                }
                CountStepper(draft.capacity, { flow.edit(UnitField.CAPACITY) { copy(capacity = it) } }, "Capacidade",
                    unit = draft.capacityKind.noun(draft.capacityCount), error = error(UnitField.CAPACITY),
                    focusRequester = requesters.getValue(UnitField.CAPACITY), onDone = { focusManager.clearFocus() })
            }
            UnitStep.DETAILS -> StepPage("Mais algum detalhe?",
                "Tudo aqui é opcional. A situação diz se a unidade está recebendo inquilinos.", optional = true) {
                Column {
                    ControlLabel("Situação")
                    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperationalStatus.entries.forEach { status ->
                            NaniChoice(statusLabels.getValue(status), statusNotes[status], selected = draft.status == status,
                                onClick = { flow.edit(null) { copy(status = status) } })
                        }
                    }
                }
                NaniTextField(draft.fee, { typed -> moneyInput(typed)?.let { flow.edit(UnitField.FEE) { copy(fee = it) } } },
                    "Condomínio mensal (R\$)", helperText = "Exemplo: 250,00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { notes.requestFocus() } }),
                    isError = error(UnitField.FEE) != null, errorMessage = error(UnitField.FEE),
                    focusRequester = requesters.getValue(UnitField.FEE))
                NaniTextField(draft.notes, { flow.edit(null) { copy(notes = it) } }, "Observações",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false, maxLines = 4, focusRequester = notes)
            }
            UnitStep.REVIEW -> UnitReview(flow)
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.UnitReview(flow: UnitFlowState) {
    val draft = flow.draft
    val error = { step: UnitStep -> flow.shown.entries.firstOrNull { it.key.step() == step }?.value }
    val fee = draft.fee.takeIf { it.isNotBlank() && validMoney(it, allowZero = true) }
        ?.let { CurrencyUtils.format(it.replace(',', '.').toDouble()) }
    val capacity = draft.capacityCount.takeIf { it > 0 }?.let { "$it ${draft.capacityKind.noun(it)}" }
    ReviewPage("Confira a unidade", "Toque em uma resposta para corrigir. Nada é salvo antes de você confirmar.",
        preview = {
            NaniDetailHero(
                title = draft.name.trim().ifBlank { "Sem nome" },
                subtitle = draft.location.trim().ifBlank { "Endereço não informado" },
                firstLabel = "Capacidade", firstValue = capacity ?: "—",
                secondLabel = "Situação", secondValue = statusLabels.getValue(draft.status),
                subtitleIcon = Icons.Rounded.LocationOn,
                identity = DetailIdentity.UNIT,
                unitIcon = draft.type.glyph()
            )
        }) {
        ReviewSheet {
            ReviewLine(draft.type.glyph(), "Tipo", listOf(draft.type.displayName()), { flow.open(UnitStep.TYPE) })
            ReviewLine(Icons.Rounded.LocationOn, "Nome e endereço", listOf(draft.name.trim().ifBlank { "Nome não informado" },
                draft.location.trim().ifBlank { "Endereço não informado" }), { flow.open(UnitStep.IDENTITY) }, error(UnitStep.IDENTITY))
            ReviewLine(Icons.Rounded.Groups, "Capacidade", listOf(capacity ?: "Não informada",
                "Contada em ${capacityKindLabels.getValue(draft.capacityKind).lowercase()}"),
                { flow.open(UnitStep.CAPACITY) }, error(UnitStep.CAPACITY))
            ReviewLine(Icons.AutoMirrored.Rounded.Notes, "Detalhes", listOfNotNull(statusLabels.getValue(draft.status),
                fee?.let { "Condomínio de $it por mês" },
                draft.notes.trim().ifBlank { null }), { flow.open(UnitStep.DETAILS) }, error(UnitStep.DETAILS), divider = false)
        }
        flow.saveError?.let { FlowError(it) }
    }
}
