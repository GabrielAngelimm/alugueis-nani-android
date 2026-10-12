package com.rentalvalidator.app.presentation.ui.tenant_payments

import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.design.dueDateIn
import com.rentalvalidator.app.presentation.design.monthInSentence
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/*
 * The rules behind a tenant's payments page, kept apart from the drawing so they can be tested.
 * Everything here is derived for display: the stored status of a month is never changed by it.
 */

/** How one month of a tenant's rent reads on the payments page. */
enum class MonthState {
    PAID, REVIEW, OVERDUE, DUE_TODAY,
    /** This month's rent, still before its due date. */
    UPCOMING,
    /** A month that has not arrived yet. */
    FUTURE,
    /** A month gone by with nothing recorded. It is not claimed as late: the app may simply not have been used then. */
    UNRECORDED,
    /** A month before the tenant was registered, with nothing recorded. */
    BEFORE_TENANCY
}

/**
 * Reads one month. Only the current month, or a month recorded as pending, can be late; a past
 * month with no record stays unrecorded, as on the year view this page replaces.
 */
fun monthState(status: PaymentStatus?, period: YearMonth, dueDay: Int, today: LocalDate, since: YearMonth?): MonthState {
    if (status == PaymentStatus.PAGO) return MonthState.PAID
    if (status == PaymentStatus.EM_ANALISE) return MonthState.REVIEW
    val current = YearMonth.from(today)
    if (status == null) {
        if (since != null && period.isBefore(since)) return MonthState.BEFORE_TENANCY
        if (period.isBefore(current)) return MonthState.UNRECORDED
    }
    if (period.isAfter(current)) return MonthState.FUTURE
    val due = dueDateIn(period, dueDay)
    return when {
        today.isAfter(due) -> MonthState.OVERDUE
        today.isEqual(due) -> MonthState.DUE_TODAY
        else -> MonthState.UPCOMING
    }
}

/** The month the tenant was registered in, when the record carries a readable date. */
fun tenancyStart(dateCreated: String): YearMonth? =
    runCatching { YearMonth.from(LocalDate.parse(dateCreated.take(10))) }.getOrNull()

/** The months a payments page can show: whole years, from the first that matters to the year after this one. */
data class MonthRange(val first: YearMonth, val years: Int) {
    val pageCount: Int get() = years * 12
    fun monthAt(page: Int): YearMonth = first.plusMonths(page.toLong())
    fun pageOf(period: YearMonth): Int = ChronoUnit.MONTHS.between(first, period).toInt().coerceIn(0, pageCount - 1)
    fun contains(year: Int): Boolean = year in first.year until first.year + years
}

/**
 * From the earliest of the registration, the first record, the month asked for and today, to the
 * latest of the last record, the month asked for and next year. Next year is always there so an
 * advance payment can be recorded.
 */
fun monthRange(since: YearMonth?, recorded: Collection<YearMonth>, requested: YearMonth?, today: LocalDate): MonthRange {
    val first = listOfNotNull(since?.year, recorded.minOfOrNull { it.year }, requested?.year, today.year).min()
    val last = listOfNotNull(recorded.maxOfOrNull { it.year }, requested?.year, today.year + 1).max()
    return MonthRange(YearMonth.of(first, 1), last - first + 1)
}

/**
 * A year in numbers. [counted] holds the months whose rent has come due (paid, under review, late
 * or gone by unrecorded) plus any paid ahead, so "10 de 10" never counts months still to come.
 * Amounts follow the current rent, since a payment stores no value of its own.
 */
data class YearSummary(
    val paid: Int,
    val counted: Int,
    val overdue: Int,
    val review: Int,
    val unrecorded: Int,
    val received: Double,
    val overdueAmount: Double
)

fun yearSummary(states: List<MonthState>, rent: Double): YearSummary {
    val paid = states.count { it == MonthState.PAID }
    val overdue = states.count { it == MonthState.OVERDUE }
    val review = states.count { it == MonthState.REVIEW }
    val unrecorded = states.count { it == MonthState.UNRECORDED }
    return YearSummary(paid, paid + overdue + review + unrecorded, overdue, review, unrecorded,
        received = paid * rent, overdueAmount = overdue * rent)
}

/** The one-line verdict on a year, most urgent first. */
enum class YearVerdict { OVERDUE, REVIEW, UNRECORDED, UP_TO_DATE, NOT_STARTED }

fun YearSummary.verdict(): YearVerdict = when {
    overdue > 0 -> YearVerdict.OVERDUE
    review > 0 -> YearVerdict.REVIEW
    unrecorded > 0 -> YearVerdict.UNRECORDED
    counted > 0 -> YearVerdict.UP_TO_DATE
    else -> YearVerdict.NOT_STARTED
}

/** Whole days from [today] to the month's due date: negative once it has passed. */
fun daysUntilDue(period: YearMonth, dueDay: Int, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, dueDateIn(period, dueDay))

/**
 * The WhatsApp note that confirms a paid month, in the same voice as the app's rent reminder:
 * "Oi, Lívia, tudo bem? Confirmamos o recebimento do aluguel de outubro de 2026, no valor de R$ 1.800,00. Obrigado!"
 */
fun receiptMessage(tenant: Tenant, period: YearMonth): String {
    val name = tenant.whatsappName.trim().ifBlank { tenant.name.trim().substringBefore(' ').ifBlank { "Inquilino" } }
    return "Oi, $name, tudo bem? Confirmamos o recebimento do aluguel de ${period.monthInSentence()} de ${period.year}, " +
        "no valor de ${CurrencyUtils.format(tenant.amount)}. Obrigado!\n#mensagemautomática"
}
