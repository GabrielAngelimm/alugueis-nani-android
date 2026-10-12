package com.rentalvalidator.app.domain.usecase

import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ValidatePaymentsUseCaseTest {

    private val useCase = ValidatePaymentsUseCase()
    private val tenant = Tenant(
        id = "tenant-1",
        name = "João Teste",
        amount = 1_000.0,
        dueDay = 10
    )
    private val period = YearMonth.of(2026, 8)

    @Test
    fun `empty normalized names do not match every transaction`() {
        val result = useCase.execute(
            tenants = listOf(tenant.copy(name = "---", aliases = listOf(" "))),
            transactions = listOf(transaction(1_000.0, TransactionType.CREDIT, day = 5)),
            referencePeriod = period,
            evaluationDate = period.atDay(5)
        ).single()
        assertEquals(0.0, result.amountPaid, 0.001)
        assertEquals(PaymentStatus.PENDENTE_NO_PRAZO, result.status)
    }

    @Test
    fun `ignores debits when calculating the identified payment`() {
        val transactions = listOf(
            transaction(1_000.0, TransactionType.CREDIT, day = 5),
            transaction(-1_000.0, TransactionType.DEBIT, day = 6)
        )

        val result = validate(transactions, evaluationDay = 6)

        assertEquals(PaymentStatus.PAGO_NO_PRAZO, result.status)
        assertEquals(1_000.0, result.amountPaid, 0.001)
        assertEquals(1, result.matchedTransactions.size)
    }

    @Test
    fun `accepts a difference smaller than one real as paid`() {
        val result = validate(
            listOf(transaction(999.50, TransactionType.CREDIT, day = 5)),
            evaluationDay = 5
        )

        assertEquals(PaymentStatus.PAGO_NO_PRAZO, result.status)
    }

    @Test
    fun `distinguishes partial payment from overpayment`() {
        val partial = validate(
            listOf(transaction(600.0, TransactionType.CREDIT, day = 5)),
            evaluationDay = 5
        )
        val overpayment = validate(
            listOf(transaction(1_200.0, TransactionType.CREDIT, day = 5)),
            evaluationDay = 5
        )

        assertEquals(PaymentStatus.PARCIAL, partial.status)
        assertEquals(PaymentStatus.PAGO_A_MAIOR, overpayment.status)
    }

    @Test
    fun `uses the evaluation date to identify overdue payments`() {
        val result = validate(emptyList(), evaluationDay = 15)

        assertEquals(PaymentStatus.PENDENTE_ATRASADO, result.status)
        assertEquals(5L, result.daysLate)
        assertEquals(200.0, result.penaltyApplied, 0.001)
        assertEquals(1_000.0, result.amountDue, 0.001)
    }

    @Test
    fun `keeps registered rent as due while calculating an optional late fee`() {
        val result = useCase.execute(
            tenants = listOf(tenant.copy(amount = 900.0)),
            transactions = emptyList(),
            referencePeriod = period,
            evaluationDate = period.atDay(15),
            penaltyFeePercentage = 20.0
        ).single()

        assertEquals(900.0, result.amountDue, 0.001)
        assertEquals(180.0, result.penaltyApplied, 0.001)
        assertEquals(0.0, result.amountPaid, 0.001)
    }

    @Test
    fun `a name inside a longer name does not take its credit`() {
        val mariana = tenant.copy(id = "mariana", name = "Mariana Souza", amount = 1_400.0)
        val ana = tenant.copy(id = "ana", name = "Ana Souza", amount = 1_400.0)
        val results = useCase.execute(
            tenants = listOf(mariana, ana),
            transactions = listOf(credit("PIX RECEBIDO - MARIANA SOUZA", 1_400.0)),
            referencePeriod = period,
            evaluationDate = period.atDay(5)
        ).associateBy { it.tenant.id }
        assertEquals(1_400.0, results.getValue("mariana").amountPaid, 0.001)
        assertEquals(0.0, results.getValue("ana").amountPaid, 0.001)
        assertEquals(PaymentStatus.PENDENTE_NO_PRAZO, results.getValue("ana").status)
    }

    @Test
    fun `a credit counts once, for the most specific name`() {
        val lima = tenant.copy(id = "lima", name = "Carla Souza Lima", amount = 900.0)
        val souza = tenant.copy(id = "souza", name = "Pedro Alves", aliases = listOf("Souza"), amount = 900.0)
        val check = useCase.check(
            tenants = listOf(lima, souza),
            transactions = listOf(credit("Pix Carla Souza Lima", 900.0)),
            referencePeriod = period,
            evaluationDate = period.atDay(5)
        )
        val results = check.results.associateBy { it.tenant.id }
        assertEquals(900.0, results.getValue("lima").amountPaid, 0.001)
        assertEquals(0.0, results.getValue("souza").amountPaid, 0.001)
        assertTrue(check.ambiguous.isEmpty())
        // The whole identified amount never exceeds what the statement received.
        assertEquals(900.0, check.results.sumOf { it.amountPaid }, 0.001)
    }

    @Test
    fun `aliases match whole words, with or without accents`() {
        val withAlias = tenant.copy(aliases = listOf("Jô Pix"))
        val results = useCase.execute(
            tenants = listOf(withAlias),
            transactions = listOf(credit("TED JO PIX LTDA", 400.0), credit("PIX JOPIXEL", 600.0)),
            referencePeriod = period,
            evaluationDate = period.atDay(5)
        ).single()
        assertEquals(400.0, results.amountPaid, 0.001)
    }

    @Test
    fun `an equally good match for two tenants waits for the owner`() {
        val mother = tenant.copy(id = "mother", name = "Ana Souza", amount = 1_400.0)
        val daughter = tenant.copy(id = "daughter", name = "Ana Souza", amount = 800.0)
        val transactions = listOf(credit("Pix Ana Souza", 800.0), credit("Pix João Teste", 1_000.0))
        val pending = useCase.check(listOf(mother, daughter, tenant), transactions, period, period.atDay(5))
        val ambiguous = pending.ambiguous.single()
        assertEquals(0, ambiguous.index)
        assertEquals(setOf("mother", "daughter"), ambiguous.candidates.map { it.id }.toSet())
        assertEquals(0.0, pending.results.filter { it.tenant.name == "Ana Souza" }.sumOf { it.amountPaid }, 0.001)
        assertEquals(1_000.0, pending.results.single { it.tenant.id == tenant.id }.amountPaid, 0.001)

        val settled = useCase.check(listOf(mother, daughter, tenant), transactions, period, period.atDay(5),
            assignments = mapOf(0 to "daughter"))
        assertTrue(settled.ambiguous.isEmpty())
        val daughterResult = settled.results.single { it.tenant.id == "daughter" }
        assertEquals(PaymentStatus.PAGO_NO_PRAZO, daughterResult.status)
        assertEquals(listOf(transactions[0]), daughterResult.matchedTransactions)
        assertEquals(0.0, settled.results.single { it.tenant.id == "mother" }.amountPaid, 0.001)
    }

    @Test
    fun `an assignment to a tenant who is gone is ignored`() {
        val check = useCase.check(listOf(tenant), listOf(credit("Pix João Teste", 1_000.0)), period, period.atDay(5),
            assignments = mapOf(0 to "removed"))
        assertEquals(1_000.0, check.results.single().amountPaid, 0.001)
    }

    private fun validate(
        transactions: List<Transaction>,
        evaluationDay: Int
    ): ValidationResult = useCase.execute(
        tenants = listOf(tenant),
        transactions = transactions,
        referencePeriod = period,
        evaluationDate = period.atDay(evaluationDay)
    ).single()

    private fun credit(description: String, amount: Double, day: Int = 5) = Transaction(
        date = LocalDate.of(period.year, period.monthValue, day),
        description = description,
        amount = amount,
        type = TransactionType.CREDIT
    )

    private fun transaction(
        amount: Double,
        type: TransactionType,
        day: Int
    ) = Transaction(
        date = LocalDate.of(period.year, period.monthValue, day),
        description = "PIX recebido João Teste",
        amount = amount,
        type = type
    )
}
