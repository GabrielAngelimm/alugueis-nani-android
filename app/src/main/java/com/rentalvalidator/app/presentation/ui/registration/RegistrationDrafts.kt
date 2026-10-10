package com.rentalvalidator.app.presentation.ui.registration

import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.presentation.ui.tenants.formatPhoneForDB
import com.rentalvalidator.app.presentation.ui.tenants.noun
import com.rentalvalidator.app.presentation.ui.tenants.statusLabels
import com.rentalvalidator.app.presentation.ui.tenants.unitTypeIcons
import com.rentalvalidator.app.presentation.ui.tenants.unitTypeLabels
import com.rentalvalidator.app.presentation.ui.tenants.validMoney
import com.rentalvalidator.app.presentation.ui.tenants.validOptionalCpf
import com.rentalvalidator.app.presentation.ui.tenants.validOptionalPhone
import com.rentalvalidator.app.util.CpfUtils
import com.rentalvalidator.app.util.CurrencyUtils

/*
 * What a guided registration has gathered so far. Answers are kept exactly as typed, so stepping
 * back and forth never loses or reformats them; they only become a record on the last step, by the
 * same rules the full forms apply.
 */

/** The bank choice that stores no bank. */
internal const val NoBank = "Nenhum"

/** Banks offered for a tenant's transfers, in the order the full form lists them. */
internal val TenantBanks = listOf(NoBank, "Itaú", "Santander", "Nubank", "Mercado Pago", "PagSeguro", "Bradesco",
    "Banco do Brasil", "Inter", "Caixa", "Sicredi")

internal enum class TenantStep(val title: String, val optional: Boolean = false) {
    NAME("Nome"), UNIT("Unidade"), RENT("Aluguel"), CONTACT("Contato"),
    STATEMENT("Extrato", optional = true), REVIEW("Revisão")
}

internal enum class TenantField { NAME, AMOUNT, DUE_DAY, PHONE, CPF }

internal data class TenantDraft(
    val name: String = "",
    val unit: String = RentalUnit.GERAL_NAME,
    val amount: String = "",
    val dueDay: Int? = null,
    /** Digits only; the field draws the mask. */
    val phone: String = "",
    /** Digits only; the field draws the mask. */
    val cpf: String = "",
    val whatsappName: String = "",
    val bank: String = NoBank,
    val aliases: List<String> = emptyList()
) {
    /** What the step's fields need before the person can leave it, field by field. */
    fun errors(step: TenantStep): Map<TenantField, String> = buildMap {
        when (step) {
            TenantStep.NAME -> if (name.isBlank()) put(TenantField.NAME, "Informe o nome")
            TenantStep.UNIT -> Unit
            TenantStep.RENT -> {
                if (!validMoney(amount)) put(TenantField.AMOUNT, "Informe um valor válido")
                if (dueDay == null || dueDay !in 1..31) put(TenantField.DUE_DAY, "Escolha o dia de vencimento, de 1 a 31")
            }
            TenantStep.CONTACT -> {
                // A new tenant always has a phone; the full form still saves without one, for older records.
                if (phone.isBlank()) put(TenantField.PHONE, "Informe o telefone com DDD")
                else if (!validOptionalPhone(phone)) put(TenantField.PHONE, "Informe DDD + 8 dígitos (fixo) ou 9 dígitos começando com 9 (celular)")
                if (!validOptionalCpf(cpf)) put(TenantField.CPF, "Informe um CPF válido com 11 dígitos ou deixe em branco")
            }
            TenantStep.STATEMENT -> Unit
            TenantStep.REVIEW -> TenantStep.entries.filter { it != TenantStep.REVIEW }.forEach { putAll(errors(it)) }
        }
    }

    /** The first step, in order, that still has something to fix; null when the draft can be saved. */
    fun firstInvalidStep(): TenantStep? = TenantStep.entries.firstOrNull { it != TenantStep.REVIEW && errors(it).isNotEmpty() }

    /** The person's first name, used to make the questions personal. */
    val firstName: String get() = name.trim().substringBefore(' ')

    /**
     * The record this draft becomes. The unit is linked to its registered record by name, as the
     * full form does, and a name with no record stays as text, like "Geral".
     */
    fun toTenant(id: String, units: List<RentalUnit>, createdOn: String): Tenant {
        check(firstInvalidStep() == null) { "The draft still has fields to fix" }
        return Tenant(
            id = id, name = name.trim(), amount = amount.replace(',', '.').toDouble(), dueDay = dueDay!!,
            unit = unit, bank = bank.takeUnless { it == NoBank }.orEmpty(), phone = formatPhoneForDB(phone),
            cpf = CpfUtils.digits(cpf), whatsappName = whatsappName.trim(), aliases = aliases.distinct(),
            dateCreated = createdOn, unitId = units.firstOrNull { it.name.equals(unit, ignoreCase = true) }?.id
        )
    }
}

internal enum class UnitStep(val title: String, val optional: Boolean = false) {
    TYPE("Tipo"), IDENTITY("Nome e endereço"), CAPACITY("Capacidade"), DETAILS("Detalhes", optional = true), REVIEW("Revisão")
}

internal enum class UnitField { NAME, LOCATION, CAPACITY, FEE }

internal data class UnitDraft(
    val type: UnitType = UnitType.OTHER,
    val name: String = "",
    val location: String = "",
    val capacity: String = "1",
    val capacityKind: CapacityKind = CapacityKind.TENANTS,
    val status: OperationalStatus = OperationalStatus.ACTIVE,
    val fee: String = "",
    val notes: String = ""
) {
    fun errors(step: UnitStep): Map<UnitField, String> = buildMap {
        when (step) {
            UnitStep.TYPE -> Unit
            UnitStep.IDENTITY -> {
                if (name.isBlank()) put(UnitField.NAME, "Informe o nome da unidade")
                if (location.isBlank()) put(UnitField.LOCATION, "Informe o endereço")
            }
            UnitStep.CAPACITY -> if ((capacity.toIntOrNull() ?: 0) < 1) put(UnitField.CAPACITY, "Mínimo 1")
            UnitStep.DETAILS -> if (fee.isNotBlank() && !validMoney(fee, allowZero = true))
                put(UnitField.FEE, "Informe um valor a partir de zero, com até 2 casas decimais")
            UnitStep.REVIEW -> UnitStep.entries.filter { it != UnitStep.REVIEW }.forEach { putAll(errors(it)) }
        }
    }

    fun firstInvalidStep(): UnitStep? = UnitStep.entries.firstOrNull { it != UnitStep.REVIEW && errors(it).isNotEmpty() }

    /** Capacity as a number, for the stepper; an empty or invalid entry counts as zero. */
    val capacityCount: Int get() = capacity.toIntOrNull() ?: 0

    /** The record this draft becomes, by the same rules as the full form. */
    fun toUnit(id: String, now: String): RentalUnit {
        check(firstInvalidStep() == null) { "The draft still has fields to fix" }
        return RentalUnit(
            id = id, name = name.trim(), type = type, iconKey = unitTypeIcons[type]?.name, photoPath = null,
            location = location.trim(), capacity = capacity.toInt(), capacityKind = capacityKind, operationalStatus = status,
            condominiumFee = fee.replace(',', '.').toDoubleOrNull(), notes = notes.trim(), createdAt = now, updatedAt = now
        )
    }
}

/** Keeps a capacity entry to the digits the full form accepts. */
internal fun capacityInput(value: String): String? = value.takeIf { it.matches(Regex("^\\d*$")) }?.take(3)

/** Keeps a money entry to what the full form accepts while typing: digits and one decimal separator. */
internal fun moneyInput(value: String): String? = value.takeIf { it.matches(Regex("^\\d*[.,]?\\d*$")) }

/** How a tenant's step reads once answered, as the step rule reads it out to assistive technology. */
internal fun TenantDraft.summary(step: TenantStep): String? = when (step) {
    TenantStep.NAME -> name.trim().ifBlank { null }
    TenantStep.UNIT -> unit
    TenantStep.RENT -> listOfNotNull(amount.takeIf { validMoney(it) }?.let { CurrencyUtils.format(it.replace(',', '.').toDouble()) },
        dueDay?.let { "dia $it" }).joinToString(" · ").ifBlank { null }
    TenantStep.CONTACT -> phone.takeIf { it.isNotBlank() }?.let(::formatPhoneForDB) ?: "Sem telefone"
    TenantStep.STATEMENT -> listOfNotNull(bank.takeUnless { it == NoBank },
        aliases.size.takeIf { it > 0 }?.let { if (it == 1) "1 outro nome" else "$it outros nomes" })
        .joinToString(" · ").ifBlank { "Sem banco" }
    TenantStep.REVIEW -> null
}

/** How a unit's step reads once answered. */
internal fun UnitDraft.summary(step: UnitStep): String? = when (step) {
    UnitStep.TYPE -> unitTypeLabels[type]
    UnitStep.IDENTITY -> name.trim().ifBlank { null }
    UnitStep.CAPACITY -> capacityCount.takeIf { it > 0 }?.let { "$it ${capacityKind.noun(it)}" }
    UnitStep.DETAILS -> listOfNotNull(statusLabels[status],
        fee.takeIf { validMoney(it, allowZero = true) }?.let { "Cond. ${CurrencyUtils.format(it.replace(',', '.').toDouble())}" })
        .joinToString(" · ")
    UnitStep.REVIEW -> null
}
