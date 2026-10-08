package com.rentalvalidator.app.presentation.design

import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")
private val MonthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val MonthOnly = DateTimeFormatter.ofPattern("MMMM", PtBr)
private val WeekdayDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)

/** Display only: persisted dates retain their existing ISO representation. */
fun displayDate(value: String, empty: String): String =
    DateUtils.formatDate(DateUtils.parseDate(value)).ifBlank { value.ifBlank { empty } }

private fun String.capitalized() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(PtBr) else it.toString() }

/** "Outubro de 2026". */
fun YearMonth.longLabel(): String = format(MonthYear).capitalized()

/** "outubro", for use inside a sentence. */
fun YearMonth.monthInSentence(): String = format(MonthOnly)

/** "Quarta-feira, 7 de outubro". */
fun LocalDate.weekdayLabel(): String = format(WeekdayDate).capitalized()

/** The due date inside [period]; a day the month lacks moves to its last day, as the rules do. */
fun dueDateIn(period: YearMonth, dueDay: Int): LocalDate = period.atDay(dueDay.coerceIn(1, period.lengthOfMonth()))

/** How a month's rent reads today. Derived for display; the stored status is never changed by it. */
enum class DueState { PAID, REVIEW, OVERDUE, DUE_TODAY, UPCOMING }

fun dueState(status: PaymentStatus?, period: YearMonth, dueDay: Int, today: LocalDate): DueState {
    if (status == PaymentStatus.PAGO) return DueState.PAID
    if (status == PaymentStatus.EM_ANALISE) return DueState.REVIEW
    val due = dueDateIn(period, dueDay)
    return when {
        today.isAfter(due) -> DueState.OVERDUE
        today.isEqual(due) -> DueState.DUE_TODAY
        else -> DueState.UPCOMING
    }
}

/** Short sentence for a due date in context: "Venceu dia 10", "Vence hoje", "Vence dia 20". */
fun dueSentence(state: DueState, dueDay: Int, period: YearMonth): String {
    val day = dueDateIn(period, dueDay).dayOfMonth
    return when (state) {
        DueState.OVERDUE -> "Venceu dia $day"
        DueState.DUE_TODAY -> "Vence hoje"
        else -> "Vence dia $day"
    }
}

fun DueState.kind(): StatusKind = when (this) {
    DueState.PAID -> StatusKind.SUCCESS
    DueState.REVIEW -> StatusKind.INFO
    DueState.OVERDUE -> StatusKind.ERROR
    DueState.DUE_TODAY, DueState.UPCOMING -> StatusKind.WARNING
}
