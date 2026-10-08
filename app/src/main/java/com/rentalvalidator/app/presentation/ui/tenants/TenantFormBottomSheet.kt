package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.AppFormFooter
import com.rentalvalidator.app.presentation.components.AppFormSection
import com.rentalvalidator.app.presentation.components.NaniDropdownField
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.design.NaniEditor
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.util.CpfUtils
import java.time.LocalDate
import java.util.UUID

@Composable
fun TenantFormBottomSheet(
    tenant: Tenant? = null,
    initialIsViewOnly: Boolean = false,
    initialUnit: String? = null,
    unitOptions: List<RentalUnit> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Tenant) -> Unit
) {
    var name by remember(tenant) { mutableStateOf(tenant?.name.orEmpty()) }
    var amount by remember(tenant) { mutableStateOf(tenant?.amount?.takeIf { it > 0 }?.let(::moneyInputText).orEmpty()) }
    var dueDay by remember(tenant) { mutableStateOf(tenant?.dueDay?.toString().orEmpty()) }
    var unit by remember(tenant, initialUnit) {
        mutableStateOf(tenant?.unit?.ifBlank { "Geral" } ?: initialUnit?.ifBlank { "Geral" } ?: "Geral")
    }
    var bank by remember(tenant) { mutableStateOf(tenant?.bank?.ifBlank { "Nenhum" } ?: "Nenhum") }
    var phone by remember(tenant) { mutableStateOf(tenant?.phone?.filter(Char::isDigit).orEmpty()) }
    var cpf by remember(tenant) { mutableStateOf(CpfUtils.digits(tenant?.cpf.orEmpty())) }
    var whatsappName by remember(tenant) { mutableStateOf(tenant?.whatsappName.orEmpty()) }
    var aliasInput by remember { mutableStateOf("") }
    var aliases by remember(tenant) { mutableStateOf(tenant?.aliases ?: emptyList()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var dueError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val phoneFocusRequester = remember { FocusRequester() }
    var cpfError by remember { mutableStateOf<String?>(null) }
    val nameFocusRequester = remember { FocusRequester() }
    val cpfFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }
    val dueDayFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Build the unit options list: use real units from the database, supplemented by context
    val unitNames = (
        unitOptions.map { it.name } +
            listOfNotNull(initialUnit, tenant?.unit)
        ).filter { it.isNotBlank() }.distinct()
    val banks = listOf("Nenhum", "Itaú", "Santander", "Nubank", "Mercado Pago", "PagSeguro", "Bradesco", "Banco do Brasil", "Inter", "Caixa", "Sicredi")


    val saveAction: () -> Unit = saveAction@{
        val nextNameError = if (name.isBlank()) "Informe o nome" else null
        val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
        val nextAmountError = if (!validMoney(amount)) "Informe um valor válido" else null
        val parsedDay = dueDay.toIntOrNull()
        val nextDueError = if (parsedDay == null || parsedDay !in 1..31) "Use um dia de 1 a 31" else null
        val nextCpfError = if (!validOptionalCpf(cpf)) "Informe um CPF válido com 11 dígitos ou deixe em branco" else null

        val nextPhoneError = if (!validOptionalPhone(phone)) "Informe DDD + 8 dígitos (fixo) ou 9 dígitos começando com 9 (celular)" else null
        phoneError = nextPhoneError
        nameError = nextNameError
        amountError = nextAmountError
        dueError = nextDueError
        cpfError = nextCpfError

        val firstInvalidField = when {
            nextNameError != null -> nameFocusRequester
            nextPhoneError != null -> phoneFocusRequester
            nextCpfError != null -> cpfFocusRequester
            nextAmountError != null -> amountFocusRequester
            nextDueError != null -> dueDayFocusRequester
            else -> null
        }
        if (firstInvalidField != null) {
            firstInvalidField.requestFocus()
            keyboardController?.show()
            return@saveAction
        }

        val selectedUnitId = unitOptions
            .firstOrNull { it.name.equals(unit, ignoreCase = true) }
            ?.id
            ?: tenant?.unitId?.takeIf { tenant.unit.equals(unit, ignoreCase = true) }

        val saved = tenant?.copy(
            name = name.trim(), amount = parsedAmount!!, dueDay = parsedDay!!, unit = unit,
            bank = bank.takeUnless { it == "Nenhum" }.orEmpty(), phone = formatPhoneForDB(phone),
            cpf = CpfUtils.digits(cpf), whatsappName = whatsappName.trim(), aliases = aliases.distinct(),
            unitId = selectedUnitId
        ) ?: Tenant(
            id = UUID.randomUUID().toString(), name = name.trim(), amount = parsedAmount!!, dueDay = parsedDay!!,
            unit = unit, bank = bank.takeUnless { it == "Nenhum" }.orEmpty(), phone = formatPhoneForDB(phone),
            cpf = CpfUtils.digits(cpf), whatsappName = whatsappName.trim(), aliases = aliases.distinct(),
            dateCreated = LocalDate.now().toString(), unitId = selectedUnitId
        )
        onSave(saved)
    }

    NaniEditor(onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize().imePadding()) {
            EditorHeader(if (tenant == null) "Novo inquilino" else "Editar inquilino", tenant?.name, onDismiss)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpace.page).padding(top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpace.section)
            ) {
                Text("Campos com * são obrigatórios.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppFormSection(title = "Dados pessoais", subtitle = "Identificação e contato", icon = Icons.Rounded.Person) {
                    NaniTextField(name, { name = it; nameError = null }, "Nome completo", required = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        isError = nameError != null, errorMessage = nameError, focusRequester = nameFocusRequester)
                    NaniTextField(phone, { phone = it.filter(Char::isDigit); phoneError = null }, "Telefone",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        visualTransformation = PhoneVisualTransformation(), helperText = "DDD e número",
                        isError = phoneError != null, errorMessage = phoneError, focusRequester = phoneFocusRequester)
                    NaniTextField(cpf, { cpf = CpfUtils.digits(it); cpfError = null }, "CPF (opcional)", helperText = "11 dígitos",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CpfVisualTransformation(),
                        isError = cpfError != null, errorMessage = cpfError, focusRequester = cpfFocusRequester)
                    NaniTextField(whatsappName, { whatsappName = it }, "Nome no WhatsApp", helperText = "Como a pessoa é chamada nas mensagens",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                }
                AppFormSection(title = "Locação e cobrança", subtitle = "Unidade, banco e condições do aluguel", icon = Icons.Rounded.MeetingRoom) {
                    NaniDropdownField("Unidade", unitNames, unit, { unit = it })
                    NaniDropdownField("Banco", banks, bank, { bank = it })
                    NaniTextField(amount, { value ->
                            if (value.matches(Regex("^\\d*[.,]?\\d*$"))) { amount = value; amountError = null }
                        }, "Aluguel mensal", required = true, helperText = "Exemplo: 1250,00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { dueDayFocusRequester.requestFocus() }),
                        isError = amountError != null, errorMessage = amountError, focusRequester = amountFocusRequester)
                    NaniTextField(dueDay, { dueDay = it.filter(Char::isDigit).take(2); dueError = null }, "Dia de vencimento",
                        required = true, helperText = "De 1 a 31",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveAction() }),
                        isError = dueError != null, errorMessage = dueError, focusRequester = dueDayFocusRequester)
                }
                TenantAliasesSection(aliasInput, aliases, { aliasInput = it },
                    onAdd = { aliases = aliases + aliasInput.trim(); aliasInput = "" },
                    onRemove = { alias -> aliases = aliases - alias })
            }
            AppFormFooter(onCancel = onDismiss, onSave = saveAction)
        }
    }
}
