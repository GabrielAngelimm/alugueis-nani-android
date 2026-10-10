package com.rentalvalidator.app.presentation.ui.registration

import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.presentation.design.bankMarkOf
import com.rentalvalidator.app.presentation.ui.tenants.unitTypeIcons
import com.rentalvalidator.app.util.CurrencyUtils
import org.junit.Assert.*
import org.junit.Test

class RegistrationDraftsTest {
    private val house = RentalUnit("u1", "Casa Azul", UnitType.HOUSE)
    private val tenant = TenantDraft(name = "Marina Oliveira", amount = "1250,50", dueDay = 10, phone = "11987654321")

    @Test fun tenantStepsAskOnlyForTheirOwnFields() {
        val empty = TenantDraft()
        assertEquals(setOf(TenantField.NAME), empty.errors(TenantStep.NAME).keys)
        assertTrue(empty.errors(TenantStep.UNIT).isEmpty())
        assertEquals(setOf(TenantField.AMOUNT, TenantField.DUE_DAY), empty.errors(TenantStep.RENT).keys)
        // Only the phone is required on the contact step; WhatsApp name and CPF can wait.
        assertEquals(mapOf(TenantField.PHONE to "Informe o telefone com DDD"), empty.errors(TenantStep.CONTACT))
        assertTrue(empty.errors(TenantStep.STATEMENT).isEmpty())
        assertEquals(setOf(TenantField.NAME, TenantField.AMOUNT, TenantField.DUE_DAY, TenantField.PHONE),
            empty.errors(TenantStep.REVIEW).keys)
        assertEquals(TenantStep.NAME, empty.firstInvalidStep())
        assertNull(tenant.firstInvalidStep())
        assertEquals(TenantStep.CONTACT, tenant.copy(phone = "").firstInvalidStep())
    }

    @Test fun tenantFieldsKeepTheFullFormRules() {
        assertEquals("Informe um valor válido", tenant.copy(amount = "1.234").errors(TenantStep.RENT)[TenantField.AMOUNT])
        assertEquals(setOf(TenantField.DUE_DAY), tenant.copy(dueDay = 32).errors(TenantStep.RENT).keys)
        val contact = tenant.copy(phone = "119", cpf = "11111111111")
        assertEquals(setOf(TenantField.PHONE, TenantField.CPF), contact.errors(TenantStep.CONTACT).keys)
        assertEquals(TenantStep.CONTACT, contact.firstInvalidStep())
        assertNull(tenant.copy(phone = "11987654321", cpf = "52998224725").firstInvalidStep())
    }

    @Test fun tenantRecordIsBuiltLikeTheFullForm() {
        val saved = tenant.copy(name = "  Marina Oliveira ", unit = "Casa Azul", cpf = "52998224725",
            whatsappName = " Mari ", bank = "Nubank", aliases = listOf("Mari Pix", "Mari Pix"))
            .toTenant("t1", listOf(house), "2026-10-10")
        assertEquals("Marina Oliveira", saved.name)
        assertEquals(1250.5, saved.amount, .001)
        assertEquals(10, saved.dueDay)
        assertEquals("Casa Azul", saved.unit)
        assertEquals("u1", saved.unitId)
        assertEquals("(11) 98765-4321", saved.phone)
        assertEquals("52998224725", saved.cpf)
        assertEquals("Mari", saved.whatsappName)
        assertEquals("Nubank", saved.bank)
        assertEquals(listOf("Mari Pix"), saved.aliases)
        assertEquals("2026-10-10", saved.dateCreated)

        val plain = tenant.toTenant("t2", listOf(house), "2026-10-10")
        assertEquals(RentalUnit.GERAL_NAME, plain.unit)
        assertNull(plain.unitId)
        assertEquals("", plain.bank)
        assertEquals("", plain.cpf)
    }

    @Test(expected = IllegalStateException::class)
    fun tenantRecordNeedsEveryRequiredAnswer() {
        TenantDraft(name = "Marina").toTenant("t3", emptyList(), "2026-10-10")
    }

    @Test fun tenantSummariesReadLikeTheAnswers() {
        assertNull(TenantDraft().summary(TenantStep.NAME))
        assertNull(TenantDraft().summary(TenantStep.RENT))
        assertEquals("Marina Oliveira", tenant.summary(TenantStep.NAME))
        assertEquals("${CurrencyUtils.format(1250.5)} · dia 10", tenant.summary(TenantStep.RENT))
        assertEquals("dia 10", tenant.copy(amount = "").summary(TenantStep.RENT))
        assertEquals("Sem telefone", tenant.copy(phone = "").summary(TenantStep.CONTACT))
        assertEquals("(11) 98765-4321", tenant.summary(TenantStep.CONTACT))
        assertEquals("Sem banco", tenant.summary(TenantStep.STATEMENT))
        assertEquals("Nubank · 1 outro nome", tenant.copy(bank = "Nubank", aliases = listOf("A")).summary(TenantStep.STATEMENT))
        assertEquals("2 outros nomes", tenant.copy(aliases = listOf("A", "B")).summary(TenantStep.STATEMENT))
    }

    @Test fun unitStepsAskOnlyForTheirOwnFields() {
        val empty = UnitDraft()
        assertTrue(empty.errors(UnitStep.TYPE).isEmpty())
        assertEquals(setOf(UnitField.NAME, UnitField.LOCATION), empty.errors(UnitStep.IDENTITY).keys)
        assertTrue(empty.errors(UnitStep.CAPACITY).isEmpty())
        assertEquals("Mínimo 1", empty.copy(capacity = "0").errors(UnitStep.CAPACITY)[UnitField.CAPACITY])
        assertEquals("Mínimo 1", empty.copy(capacity = "").errors(UnitStep.CAPACITY)[UnitField.CAPACITY])
        assertTrue(empty.copy(fee = "0").errors(UnitStep.DETAILS).isEmpty())
        assertEquals(setOf(UnitField.FEE), empty.copy(fee = "1,234").errors(UnitStep.DETAILS).keys)
        assertEquals(UnitStep.IDENTITY, empty.firstInvalidStep())
        assertNull(empty.copy(name = "Casa Azul", location = "Rua do Sol, 140").firstInvalidStep())
    }

    @Test fun unitRecordIsBuiltLikeTheFullForm() {
        val saved = UnitDraft(UnitType.KITNET, " Kitnet Centro ", " Rua do Sol, 140 ", "3", CapacityKind.ROOMS,
            OperationalStatus.MAINTENANCE, "250,5", " Portão azul ").toUnit("u2", "2026-10-10T12:00:00Z")
        assertEquals("Kitnet Centro", saved.name)
        assertEquals("Rua do Sol, 140", saved.location)
        assertEquals(UnitType.KITNET, saved.type)
        assertEquals(unitTypeIcons.getValue(UnitType.KITNET).name, saved.iconKey)
        assertNull(saved.photoPath)
        assertEquals(3, saved.capacity)
        assertEquals(CapacityKind.ROOMS, saved.capacityKind)
        assertEquals(OperationalStatus.MAINTENANCE, saved.operationalStatus)
        assertEquals(250.5, saved.condominiumFee!!, .001)
        assertEquals("Portão azul", saved.notes)
        assertEquals("2026-10-10T12:00:00Z", saved.createdAt)
        assertEquals(saved.createdAt, saved.updatedAt)
        assertNull(UnitDraft(name = "Casa Azul", location = "Rua do Sol").toUnit("u3", "now").condominiumFee)
    }

    @Test fun unitSummariesReadLikeTheAnswers() {
        val unit = UnitDraft(UnitType.HOUSE, "Casa Azul", "Rua do Sol", "2")
        assertEquals("Casa", unit.summary(UnitStep.TYPE))
        assertEquals("Casa Azul", unit.summary(UnitStep.IDENTITY))
        assertEquals("2 inquilinos", unit.summary(UnitStep.CAPACITY))
        assertEquals("1 vaga", unit.copy(capacity = "1", capacityKind = CapacityKind.SPACES).summary(UnitStep.CAPACITY))
        assertNull(unit.copy(capacity = "").summary(UnitStep.CAPACITY))
        assertEquals("Ativa", unit.summary(UnitStep.DETAILS))
        assertEquals("Inativa · Cond. ${CurrencyUtils.format(250.0)}",
            unit.copy(status = OperationalStatus.INACTIVE, fee = "250").summary(UnitStep.DETAILS))
    }

    @Test fun entriesKeepWhatTheFieldsAccept() {
        assertEquals("12", capacityInput("12"))
        assertEquals("123", capacityInput("12345"))
        assertNull(capacityInput("1a"))
        assertEquals("1250,5", moneyInput("1250,5"))
        assertEquals("", moneyInput(""))
        assertNull(moneyInput("1,2,3"))
        assertNull(moneyInput("R$"))
    }

    @Test fun everyListedBankShowsItsOwnMark() {
        // A bank offered in the registration without a mark would fall back to bare initials.
        assertTrue(TenantBanks.filter { it != NoBank }.all { bankMarkOf(it) != null })
        assertNull(bankMarkOf(NoBank))
        // No two banks share a coin.
        assertEquals(TenantBanks.size - 1, TenantBanks.mapNotNull { bankMarkOf(it)?.coin }.toSet().size)
    }
}
