package com.rentalvalidator.app.presentation.ui.tenant_payments

import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.util.CurrencyUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PaymentYearTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val october = YearMonth.of(2026, 10)

    private fun state(status: PaymentStatus?, period: YearMonth, dueDay: Int = 10, since: YearMonth? = null) =
        monthState(status, period, dueDay, today, since)

    @Test fun aRecordedStatusWinsOverTheCalendar() {
        assertEquals(MonthState.PAID, state(PaymentStatus.PAGO, YearMonth.of(2027, 3)))
        assertEquals(MonthState.PAID, state(PaymentStatus.PAGO, YearMonth.of(2020, 1), since = october))
        assertEquals(MonthState.REVIEW, state(PaymentStatus.EM_ANALISE, YearMonth.of(2026, 2)))
    }

    @Test fun theCurrentMonthFollowsItsDueDate() {
        assertEquals(MonthState.OVERDUE, state(null, october, dueDay = 10))
        assertEquals(MonthState.DUE_TODAY, state(null, october, dueDay = 12))
        assertEquals(MonthState.UPCOMING, state(null, october, dueDay = 20))
        assertEquals(MonthState.OVERDUE, state(PaymentStatus.PENDENTE, october, dueDay = 10))
    }

    @Test fun aPastMonthIsLateOnlyWhenRecordedAsPending() {
        assertEquals(MonthState.UNRECORDED, state(null, YearMonth.of(2026, 9)))
        assertEquals(MonthState.OVERDUE, state(PaymentStatus.PENDENTE, YearMonth.of(2026, 9)))
    }

    @Test fun monthsBeforeRegistrationAndAheadReadAsSuch() {
        assertEquals(MonthState.BEFORE_TENANCY, state(null, YearMonth.of(2026, 2), since = YearMonth.of(2026, 3)))
        assertEquals(MonthState.UNRECORDED, state(null, YearMonth.of(2026, 3), since = YearMonth.of(2026, 3)))
        assertEquals(MonthState.FUTURE, state(null, YearMonth.of(2026, 11)))
        assertEquals(MonthState.FUTURE, state(PaymentStatus.PENDENTE, YearMonth.of(2027, 1)))
    }

    @Test fun aDueDayPastTheMonthsEndFallsOnItsLastDay() {
        val february = LocalDate.of(2026, 2, 28)
        assertEquals(MonthState.DUE_TODAY, monthState(null, YearMonth.of(2026, 2), 31, february, null))
        assertEquals(0L, daysUntilDue(YearMonth.of(2026, 2), 31, february))
        assertEquals(-2L, daysUntilDue(october, 10, today))
    }

    @Test fun registrationReadsFromADateOrATimestamp() {
        assertEquals(YearMonth.of(2025, 3), tenancyStart("2025-03-14"))
        assertEquals(YearMonth.of(2025, 3), tenancyStart("2025-03-14T09:30:00"))
        assertNull(tenancyStart(""))
        assertNull(tenancyStart("ontem"))
    }

    @Test fun theRangeSpansWholeYearsUpToNextYear() {
        val range = monthRange(null, emptyList(), null, today)
        assertEquals(YearMonth.of(2026, 1), range.first)
        assertEquals(24, range.pageCount)
        assertEquals(october, range.monthAt(range.pageOf(october)))
        assertTrue(range.contains(2027))
        assertFalse(range.contains(2025))
    }

    @Test fun theRangeReachesRecordsRegistrationAndTheMonthAskedFor() {
        val range = monthRange(YearMonth.of(2024, 6), listOf(YearMonth.of(2029, 2)), YearMonth.of(2023, 5), today)
        assertEquals(YearMonth.of(2023, 1), range.first)
        assertTrue(range.contains(2029))
        assertFalse(range.contains(2030))
        assertEquals(YearMonth.of(2023, 5), range.monthAt(range.pageOf(YearMonth.of(2023, 5))))
        // A month outside the range lands on its nearest end instead of failing.
        assertEquals(0, range.pageOf(YearMonth.of(2010, 1)))
        assertEquals(range.pageCount - 1, range.pageOf(YearMonth.of(2040, 1)))
    }

    @Test fun aYearCountsOnlyMonthsThatCameDue() {
        val states = listOf(MonthState.PAID, MonthState.PAID, MonthState.REVIEW, MonthState.UNRECORDED, MonthState.OVERDUE,
            MonthState.UPCOMING, MonthState.FUTURE, MonthState.BEFORE_TENANCY)
        val summary = yearSummary(states, 1000.0)
        assertEquals(2, summary.paid)
        assertEquals(5, summary.counted)
        assertEquals(2000.0, summary.received, 0.0)
        assertEquals(1000.0, summary.overdueAmount, 0.0)
    }

    @Test fun theVerdictNamesTheMostUrgentThing() {
        fun verdict(vararg states: MonthState) = yearSummary(states.toList(), 1000.0).verdict()
        assertEquals(YearVerdict.OVERDUE, verdict(MonthState.OVERDUE, MonthState.REVIEW, MonthState.UNRECORDED))
        assertEquals(YearVerdict.REVIEW, verdict(MonthState.REVIEW, MonthState.UNRECORDED))
        assertEquals(YearVerdict.UNRECORDED, verdict(MonthState.PAID, MonthState.UNRECORDED))
        assertEquals(YearVerdict.UP_TO_DATE, verdict(MonthState.PAID, MonthState.UPCOMING, MonthState.FUTURE))
        assertEquals(YearVerdict.NOT_STARTED, verdict(MonthState.FUTURE, MonthState.BEFORE_TENANCY))
    }

    @Test fun theReceiptGreetsByTheWhatsAppNameAndStatesTheMonth() {
        val tenant = Tenant("t", "Lívia Martins", 1800.0, 5)
        val message = receiptMessage(tenant, october)
        assertTrue(message.startsWith("Oi, Lívia, tudo bem?"))
        assertTrue(message.contains("outubro de 2026"))
        assertTrue(message.contains(CurrencyUtils.format(1800.0)))
        assertTrue(message.endsWith("#mensagemautomática"))
        assertTrue(receiptMessage(tenant.copy(whatsappName = "Livinha"), october).startsWith("Oi, Livinha,"))
    }
}
