package com.rentalvalidator.app.domain.usecase

import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import com.rentalvalidator.app.util.TextNormalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

enum class PaymentStatus {
    PAGO_NO_PRAZO,
    PAGO_COM_ATRASO,
    PAGO_A_MAIOR,
    PARCIAL,
    PENDENTE_NO_PRAZO,
    PENDENTE_ATRASADO;

    val isPaid: Boolean
        get() = this == PAGO_NO_PRAZO || this == PAGO_COM_ATRASO || this == PAGO_A_MAIOR
}

data class ValidationResult(
    val tenant: Tenant,
    val status: PaymentStatus,
    val amountDue: Double,
    val amountPaid: Double = 0.0,
    val daysLate: Long = 0L,
    val penaltyApplied: Double = 0.0,
    val matchedTransactions: List<Transaction> = emptyList()
)

class ValidatePaymentsUseCase @Inject constructor() {

    fun execute(
        tenants: List<Tenant>,
        transactions: List<Transaction>,
        referencePeriod: YearMonth = YearMonth.now(),
        evaluationDate: LocalDate = LocalDate.now(),
        penaltyFeePercentage: Double = 20.0, // Unique fee of 20% for late
        penaltyInterestPerDayPercentage: Double = 0.0 // No daily interest
    ): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        val targetMonth = referencePeriod.monthValue
        val targetYear = referencePeriod.year
        val filteredTransactions = transactions.filter {
            it.date.monthValue == targetMonth &&
                it.date.year == targetYear &&
                it.type == TransactionType.CREDIT &&
                it.amount > 0.0
        }

        for (tenant in tenants) {
            val matched = findMatchesForTenant(tenant, filteredTransactions)
            val totalPaid = matched.sumOf { it.amount }

            val dueDateDay = tenant.dueDay.coerceAtMost(referencePeriod.lengthOfMonth())
            val dueDate = referencePeriod.atDay(dueDateDay)
            val difference = totalPaid - tenant.amount
            val withinTolerance = totalPaid > 0.0 && abs(difference) < PAYMENT_TOLERANCE
            val latestPaymentDate = matched.maxOfOrNull { it.date }

            val status = when {
                totalPaid <= 0.0 -> {
                    if (evaluationDate.isAfter(dueDate)) PaymentStatus.PENDENTE_ATRASADO
                    else PaymentStatus.PENDENTE_NO_PRAZO
                }
                withinTolerance -> {
                    if (latestPaymentDate?.isAfter(dueDate) == true) PaymentStatus.PAGO_COM_ATRASO
                    else PaymentStatus.PAGO_NO_PRAZO
                }
                difference > 0.0 -> PaymentStatus.PAGO_A_MAIOR
                else -> PaymentStatus.PARCIAL
            }

            var penaltyApplied = 0.0
            var daysLate = 0L

            if (status.isPaid) {
                if (latestPaymentDate?.isAfter(dueDate) == true) {
                    daysLate = ChronoUnit.DAYS.between(dueDate, latestPaymentDate)
                }
            } else if (evaluationDate.isAfter(dueDate)) {
                daysLate = ChronoUnit.DAYS.between(dueDate, evaluationDate)
                penaltyApplied = tenant.amount * (penaltyFeePercentage / 100)
            }
            
            results.add(
                ValidationResult(
                    tenant = tenant,
                    status = status,
                    // The statement's due amount is the registered rent. A late fee is
                    // calculated separately so the owner can opt into charging it.
                    amountDue = tenant.amount,
                    amountPaid = totalPaid,
                    daysLate = daysLate,
                    penaltyApplied = penaltyApplied,
                    matchedTransactions = matched
                )
            )
        }
        
        return results
    }

    private fun findMatchesForTenant(tenant: Tenant, transactions: List<Transaction>): List<Transaction> {
        val matched = mutableListOf<Transaction>()
        
        val tenantAliases = (tenant.aliases + tenant.name).map { TextNormalizer.normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()

        for (transaction in transactions) {
            val normalizedDesc = TextNormalizer.normalize(transaction.description)
            
            for (alias in tenantAliases) {
                if (normalizedDesc.contains(alias)) {
                    matched.add(transaction)
                    break // Next transaction if matched
                }
            }
        }
        
        return matched
    }

    private companion object {
        const val PAYMENT_TOLERANCE = 1.0
    }
}
