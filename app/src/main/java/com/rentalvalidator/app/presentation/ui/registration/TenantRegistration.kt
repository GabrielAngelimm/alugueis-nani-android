package com.rentalvalidator.app.presentation.ui.registration

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.design.DetailIdentity
import com.rentalvalidator.app.presentation.design.NaniChoice
import com.rentalvalidator.app.presentation.design.NaniDetailHero
import com.rentalvalidator.app.presentation.design.UnitTile
import com.rentalvalidator.app.presentation.ui.tenants.CpfVisualTransformation
import com.rentalvalidator.app.presentation.ui.tenants.PhoneVisualTransformation
import com.rentalvalidator.app.presentation.ui.tenants.TenantAliasesEditor
import com.rentalvalidator.app.presentation.ui.tenants.UnitGroupGlyph
import com.rentalvalidator.app.presentation.ui.tenants.formatPhoneForDB
import com.rentalvalidator.app.presentation.ui.tenants.glyph
import com.rentalvalidator.app.presentation.ui.tenants.noun
import com.rentalvalidator.app.presentation.ui.tenants.validMoney
import com.rentalvalidator.app.util.CpfUtils
import com.rentalvalidator.app.util.CurrencyUtils
import kotlinx.coroutines.delay

/** Everything a tenant's registration holds while it is open, so stepping away from a step keeps its answers. */
@Stable
internal class TenantFlowState(val start: TenantDraft) {
    var draft by mutableStateOf(start)
    val stepper = StepperState(TenantStep.entries.size)
    /** Errors the person has been shown, field by field; editing a field clears its own. */
    var shown by mutableStateOf<Map<TenantField, String>>(emptyMap())
    /** A statement name typed but not yet added. */
    var aliasInput by mutableStateOf("")
    /** The field to focus once its step is on screen. */
    var focus by mutableStateOf<TenantField?>(null)
    var saving by mutableStateOf(false)
    var saveError by mutableStateOf<String?>(null)

    val step: TenantStep get() = TenantStep.entries[stepper.current]
    val hasAnswers: Boolean get() = draft != start || aliasInput.isNotBlank()

    fun edit(field: TenantField?, change: TenantDraft.() -> TenantDraft) {
        draft = draft.change()
        if (field != null) shown = shown - field
        saveError = null
    }

    /** Adds the typed statement name, once, and empties the entry. */
    fun addAlias() {
        val alias = aliasInput.trim()
        if (alias.isNotEmpty() && alias !in draft.aliases) edit(null) { copy(aliases = aliases + alias) }
        aliasInput = ""
    }

    /** Leaves the current step forward when it has nothing to fix; otherwise shows what to fix. */
    fun advance(onSave: (TenantDraft) -> Unit) {
        if (step == TenantStep.STATEMENT) addAlias()
        if (step == TenantStep.REVIEW) {
            val invalid = draft.firstInvalidStep()
            if (invalid == null) onSave(draft) else {
                shown = draft.errors(TenantStep.REVIEW)
                stepper.goTo(invalid.ordinal)
                focus = draft.errors(invalid).keys.first()
            }
            return
        }
        val errors = draft.errors(step)
        if (errors.isNotEmpty()) {
            shown = shown + errors
            focus = errors.keys.first()
            return
        }
        // Once the review has been reached, a corrected step goes straight back to it.
        stepper.goTo(if (stepper.furthest == TenantStep.REVIEW.ordinal) TenantStep.REVIEW.ordinal else stepper.current + 1)
    }

    /** Opens a step from the trail or the review. Going forward still needs the step on screen to be complete. */
    fun open(target: TenantStep) {
        if (target.ordinal > stepper.current && step != TenantStep.REVIEW) {
            val errors = draft.errors(step)
            if (errors.isNotEmpty()) { shown = shown + errors; focus = errors.keys.first(); return }
        }
        stepper.goTo(target.ordinal)
    }

    fun nextLabel(): String = when {
        step == TenantStep.REVIEW -> "Cadastrar inquilino"
        stepper.furthest == TenantStep.REVIEW.ordinal || step.ordinal == TenantStep.REVIEW.ordinal - 1 -> "Revisar"
        else -> "Avançar"
    }
}

private fun TenantField.step(): TenantStep = when (this) {
    TenantField.NAME -> TenantStep.NAME
    TenantField.AMOUNT, TenantField.DUE_DAY -> TenantStep.RENT
    TenantField.PHONE, TenantField.CPF -> TenantStep.CONTACT
}

@Composable
internal fun TenantRegistration(
    flow: TenantFlowState,
    units: List<RentalUnit>,
    firstBackLabel: String,
    onFirstBack: () -> Unit,
    onClose: () -> Unit,
    onSave: (TenantDraft) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val requesters = remember { TenantField.entries.associateWith { FocusRequester() } }
    val whatsapp = remember { FocusRequester() }
    // A new page starts without a focused field, so the keyboard only rises where there is something to type.
    LaunchedEffect(flow.stepper.current) { focusManager.clearFocus() }
    LaunchedEffect(flow.focus, flow.stepper.current) {
        val field = flow.focus ?: return@LaunchedEffect
        delay(120)
        if (field != TenantField.DUE_DAY && runCatching { requesters.getValue(field).requestFocus() }.isSuccess) keyboard?.show()
        flow.focus = null
    }
    val advance = { flow.advance(onSave) }
    val draft = flow.draft
    val who = draft.firstName.ifBlank { "o inquilino" }
    val steps = TenantStep.entries.map { step ->
        TrailStep(step.title, step.optional, draft.summary(step), hasError = flow.shown.keys.any { it.step() == step })
    }
    GuidedFlow(
        title = "Novo inquilino",
        subtitle = null,
        steps = steps,
        stepper = flow.stepper,
        onTrail = { flow.open(TenantStep.entries[it]) },
        onClose = onClose,
        footer = {
            FlowFooter(
                backLabel = if (flow.stepper.current == 0) firstBackLabel else "Voltar",
                onBack = { if (flow.stepper.current == 0) onFirstBack() else flow.stepper.goTo(flow.stepper.current - 1) },
                nextLabel = flow.nextLabel(),
                onNext = advance,
                busy = flow.saving,
                nextIcon = if (flow.step == TenantStep.REVIEW) Icons.Rounded.Check else null
            )
        }
    ) { index ->
        val error = { field: TenantField -> flow.shown[field] }
        when (TenantStep.entries[index]) {
            TenantStep.NAME -> NamePage(flow, requesters.getValue(TenantField.NAME), advance, error(TenantField.NAME))
            TenantStep.UNIT -> UnitPage(flow, who, units)
            TenantStep.RENT -> StepPage("Aluguel", "Quanto e quando $who paga?",
                "O valor mensal e o dia do mês em que o aluguel vence.") {
                val amount = requesters.getValue(TenantField.AMOUNT)
                LaunchedEffect(Unit) { if (flow.draft.amount.isEmpty()) { delay(AppMotion.PageDuration.toLong()); runCatching { amount.requestFocus() } } }
                NaniTextField(draft.amount, { typed -> moneyInput(typed)?.let { flow.edit(TenantField.AMOUNT) { copy(amount = it) } } },
                    "Aluguel mensal", required = true, helperText = "Exemplo: 1250,00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    isError = error(TenantField.AMOUNT) != null, errorMessage = error(TenantField.AMOUNT), focusRequester = amount)
                DueDayPicker(draft.dueDay, { day -> focusManager.clearFocus(); flow.edit(TenantField.DUE_DAY) { copy(dueDay = day) } },
                    error(TenantField.DUE_DAY))
            }
            TenantStep.CONTACT -> StepPage("Contato", "Como falar com $who?",
                "O telefone é usado nas cobranças e nos lembretes pelo WhatsApp. Tudo aqui pode ficar para depois.", optional = true) {
                NaniTextField(draft.phone, { flow.edit(TenantField.PHONE) { copy(phone = it.filter(Char::isDigit).take(11)) } }, "Telefone",
                    helperText = "DDD e número", visualTransformation = PhoneVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { whatsapp.requestFocus() } }),
                    isError = error(TenantField.PHONE) != null, errorMessage = error(TenantField.PHONE),
                    focusRequester = requesters.getValue(TenantField.PHONE))
                NaniTextField(draft.whatsappName, { flow.edit(null) { copy(whatsappName = it) } }, "Nome no WhatsApp",
                    helperText = if (draft.firstName.isBlank()) "Como a pessoa é chamada nas mensagens" else "Se ficar em branco: ${draft.firstName}",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { runCatching { requesters.getValue(TenantField.CPF).requestFocus() } }),
                    focusRequester = whatsapp)
                NaniTextField(draft.cpf, { flow.edit(TenantField.CPF) { copy(cpf = CpfUtils.digits(it)) } }, "CPF", helperText = "11 dígitos",
                    visualTransformation = CpfVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { advance() }),
                    isError = error(TenantField.CPF) != null, errorMessage = error(TenantField.CPF),
                    focusRequester = requesters.getValue(TenantField.CPF))
            }
            TenantStep.STATEMENT -> StepPage("Extrato", "Como o pagamento chega?",
                "O banco filtra a conferência do extrato. Outros nomes ajudam a reconhecer transferências feitas por outra pessoa.",
                optional = true) {
                Column {
                    ControlLabel("Banco")
                    ChipChoices(TenantBanks, draft.bank) { bank -> flow.edit(null) { copy(bank = bank) } }
                }
                Column {
                    ControlLabel("Outros nomes no extrato")
                    TenantAliasesEditor(flow.aliasInput, draft.aliases, { flow.aliasInput = it }, onAdd = flow::addAlias,
                        onRemove = { alias -> flow.edit(null) { copy(aliases = aliases - alias) } })
                }
            }
            TenantStep.REVIEW -> TenantReview(flow, units)
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.NamePage(flow: TenantFlowState, focus: FocusRequester, advance: () -> Unit, error: String?) {
    StepPage("Nome", "Qual é o nome do inquilino?",
        "Use o nome completo. Ele aparece nas listas e ajuda a reconhecer os pagamentos no extrato.") {
        LaunchedEffect(Unit) { if (flow.draft.name.isEmpty()) { delay(AppMotion.PageDuration.toLong()); runCatching { focus.requestFocus() } } }
        NaniTextField(flow.draft.name, { flow.edit(TenantField.NAME) { copy(name = it) } }, "Nome completo", required = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { advance() }),
            isError = error != null, errorMessage = error, focusRequester = focus)
    }
}

/** A place the tenant can live in: a registered unit, the unit the registration was opened from, or Geral. */
private data class UnitOption(val name: String, val detail: String, val unit: RentalUnit?)

private fun unitOptions(units: List<RentalUnit>, chosen: String): List<UnitOption> {
    val registered = units.sortedBy { it.name.lowercase() }.map { unit ->
        val occupancy = "${unit.tenantCount} de ${unit.capacity} ${unit.capacityKind.noun(unit.capacity)}"
        val state = when {
            unit.operationalStatus == OperationalStatus.INACTIVE -> "Inativa"
            unit.operationalStatus == OperationalStatus.MAINTENANCE -> "Em manutenção"
            unit.tenantCount >= unit.capacity -> "Lotada"
            else -> null
        }
        UnitOption(unit.name, listOfNotNull(unit.location.ifBlank { null }, occupancy, state).joinToString(" · "), unit)
    }
    val names = registered.map { it.name.lowercase() }
    val extra = listOf(chosen, RentalUnit.GERAL_NAME).distinct().filter { it.isNotBlank() && it.lowercase() !in names }.map {
        UnitOption(it, if (it == RentalUnit.GERAL_NAME) "Sem unidade definida" else "Agrupamento de inquilinos", null)
    }
    return registered + extra
}

@Composable
private fun AnimatedVisibilityScope.UnitPage(flow: TenantFlowState, who: String, units: List<RentalUnit>) {
    val options = remember(units) { unitOptions(units, flow.start.unit) }
    StepPage("Unidade", "Em qual unidade $who mora?",
        "A unidade organiza os inquilinos por endereço e mostra a ocupação de cada imóvel.") {
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                NaniChoice(option.name, option.detail, selected = flow.draft.unit == option.name,
                    onClick = { flow.edit(null) { copy(unit = option.name) } },
                    leading = { UnitTile(option.unit?.type?.glyph() ?: UnitGroupGlyph, size = 44.dp) })
            }
        }
        if (units.isEmpty()) FlowHint(Icons.Rounded.Info,
            "Ainda não há unidades cadastradas. O inquilino fica em Geral e pode mudar de unidade depois, na edição do cadastro.")
    }
}

@Composable
private fun AnimatedVisibilityScope.TenantReview(flow: TenantFlowState, units: List<RentalUnit>) {
    val draft = flow.draft
    val error = { step: TenantStep -> flow.shown.entries.firstOrNull { it.key.step() == step }?.value }
    val amount = draft.amount.takeIf { validMoney(it) }?.let { CurrencyUtils.format(it.replace(',', '.').toDouble()) }
    val unit = units.firstOrNull { it.name.equals(draft.unit, ignoreCase = true) }
    ReviewPage("Revisão", "Confira o cadastro", "Toque em uma resposta para corrigir. Nada é salvo antes de você confirmar.",
        preview = {
            NaniDetailHero(
                title = draft.name.trim().ifBlank { "Sem nome" },
                subtitle = draft.unit,
                firstLabel = "Aluguel mensal", firstValue = amount ?: "—",
                secondLabel = "Vencimento", secondValue = draft.dueDay?.let { "Dia $it" } ?: "—",
                subtitleIcon = Icons.Rounded.MeetingRoom,
                identity = DetailIdentity.TENANT
            )
        }) {
        ReviewSheet {
            ReviewLine(Icons.Rounded.Person, "Nome", listOf(draft.name.trim().ifBlank { "Não informado" }),
                { flow.open(TenantStep.NAME) }, error(TenantStep.NAME))
            ReviewLine(unit?.type?.glyph() ?: UnitGroupGlyph, "Unidade",
                listOfNotNull(draft.unit, unit?.location?.ifBlank { null }), { flow.open(TenantStep.UNIT) })
            ReviewLine(Icons.Rounded.Payments, "Aluguel", listOfNotNull(amount?.let { "$it por mês" } ?: "Valor não informado",
                draft.dueDay?.let { "Vence todo dia $it" }), { flow.open(TenantStep.RENT) }, error(TenantStep.RENT))
            ReviewLine(Icons.Rounded.Phone, "Contato", listOfNotNull(
                draft.phone.takeIf { it.isNotBlank() }?.let(::formatPhoneForDB),
                draft.whatsappName.trim().takeIf { it.isNotBlank() }?.let { "WhatsApp: $it" },
                draft.cpf.takeIf { it.isNotBlank() }?.let { "CPF ${CpfUtils.format(it)}" }
            ).ifEmpty { listOf("Não informado") }, { flow.open(TenantStep.CONTACT) }, error(TenantStep.CONTACT))
            ReviewLine(Icons.AutoMirrored.Rounded.ReceiptLong, "Extrato", listOfNotNull(
                draft.bank.takeUnless { it == NoBank } ?: "Sem banco",
                draft.aliases.takeIf { it.isNotEmpty() }?.let { "Outros nomes: ${it.joinToString()}" }
            ), { flow.open(TenantStep.STATEMENT) }, divider = false)
        }
        flow.saveError?.let { FlowError(it) }
    }
}
