package com.rentalvalidator.app.domain.usecase

import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import com.rentalvalidator.app.util.TextNormalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Collections
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

/**
 * A credit whose description names two or more tenants equally well. It counts for nobody until
 * the owner says whose it is; [index] is its position in the transactions that were checked.
 */
data class AmbiguousCredit(
    val index: Int,
    val transaction: Transaction,
    val candidates: List<Tenant>
)

data class StatementCheck(
    val results: List<ValidationResult>,
    val ambiguous: List<AmbiguousCredit> = emptyList()
)

class ValidatePaymentsUseCase @Inject constructor() {

    fun execute(
        tenants: List<Tenant>,
        transactions: List<Transaction>,
        referencePeriod: YearMonth = YearMonth.now(),
        evaluationDate: LocalDate = LocalDate.now(),
        penaltyFeePercentage: Double = 20.0, // Unique fee of 20% for late
        penaltyInterestPerDayPercentage: Double = 0.0 // No daily interest
    ): List<ValidationResult> = check(tenants, transactions, referencePeriod, evaluationDate,
        penaltyFeePercentage, penaltyInterestPerDayPercentage).results

    /**
     * Matches the month's credits to tenants and computes each tenant's result. A credit goes to
     * at most one tenant: the one whose name or alias appears in its description as whole words,
     * the most specific such name winning. Equally good names leave the credit [AmbiguousCredit]
     * until [assignments] (transaction index to tenant id) settles it.
     */
    fun check(
        tenants: List<Tenant>,
        transactions: List<Transaction>,
        referencePeriod: YearMonth = YearMonth.now(),
        evaluationDate: LocalDate = LocalDate.now(),
        penaltyFeePercentage: Double = 20.0,
        penaltyInterestPerDayPercentage: Double = 0.0,
        assignments: Map<Int, String> = emptyMap()
    ): StatementCheck {
        val results = mutableListOf<ValidationResult>()

        val targetMonth = referencePeriod.monthValue
        val targetYear = referencePeriod.year
        val credits = transactions.withIndex().filter { (_, credit) ->
            credit.date.monthValue == targetMonth &&
                credit.date.year == targetYear &&
                credit.type == TransactionType.CREDIT &&
                credit.amount > 0.0
        }

        val names = tenants.associate { it.id to statementNames(it) }
        val received = mutableMapOf<String, MutableList<Transaction>>()
        val ambiguous = mutableListOf<AmbiguousCredit>()
        for ((index, transaction) in credits) {
            val owner = assignments[index]?.takeIf { id -> tenants.any { it.id == id } }
            if (owner != null) {
                received.getOrPut(owner) { mutableListOf() } += transaction
                continue
            }
            val words = words(transaction.description)
            val scores = tenants.mapNotNull { tenant ->
                names.getValue(tenant.id).filter { Collections.indexOfSubList(words, it) >= 0 }
                    .maxOfOrNull { specificity(it) }?.let { tenant to it }
            }
            val best = scores.maxOfOrNull { it.second } ?: continue
            val winners = scores.filter { it.second == best }.map { it.first }
            if (winners.size == 1) received.getOrPut(winners.single().id) { mutableListOf() } += transaction
            else ambiguous += AmbiguousCredit(index, transaction, winners)
        }

        for (tenant in tenants) {
            val matched = received[tenant.id].orEmpty()
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
        
        return StatementCheck(results, ambiguous)
    }

    /** The tenant's name and aliases as word lists; names with no letters or digits match nothing. */
    private fun statementNames(tenant: Tenant): List<List<String>> =
        (tenant.aliases + tenant.name).map { words(it) }.filter { it.isNotEmpty() }.distinct()

    /** More words, then more letters, make a name more specific: "Ana Souza Lima" beats "Ana Souza". */
    private fun specificity(name: List<String>): Int = name.size * 1_000 + name.sumOf { it.length }

    private fun words(text: String): List<String> =
        TextNormalizer.normalize(text).split(NON_WORD).filter { it.isNotEmpty() }

    private companion object {
        const val PAYMENT_TOLERANCE = 1.0
        val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    }
}
