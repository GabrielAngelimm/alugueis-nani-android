package com.rentalvalidator.app.domain.usecase

import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
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

    private fun validate(
        transactions: List<Transaction>,
        evaluationDay: Int
    ): ValidationResult = useCase.execute(
        tenants = listOf(tenant),
        transactions = transactions,
        referencePeriod = period,
        evaluationDate = period.atDay(evaluationDay)
    ).single()

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
