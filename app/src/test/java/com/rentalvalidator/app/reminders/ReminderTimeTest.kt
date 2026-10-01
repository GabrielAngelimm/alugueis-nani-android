package com.rentalvalidator.app.reminders

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderTimeTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun instant(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test fun daysBeforeUseDueDateAtNine() {
        val result = ReminderTime.next(10, 48, instant("2026-09-01T08:00"), zone)
        assertEquals(LocalDate.of(2026, 9, 10), result.dueDate)
        assertEquals(instant("2026-09-08T09:00"), result.trigger)
    }
    @Test fun hoursCanCrossIntoPreviousDay() {
        assertEquals(instant("2026-09-09T21:00"), ReminderTime.next(10, 12, instant("2026-09-01T08:00"), zone).trigger)
    }
    @Test fun passedLeadTimeSkipsToNextMonthlyOccurrence() {
        assertEquals(instant("2026-10-09T09:00"), ReminderTime.next(10, 24, instant("2026-09-09T09:00"), zone).trigger)
    }
    @Test fun shortAndLeapMonthsClampDueDay() {
        assertEquals(LocalDate.of(2026, 2, 28), ReminderTime.next(31, 24, instant("2026-02-01T08:00"), zone).dueDate)
        assertEquals(LocalDate.of(2028, 2, 29), ReminderTime.next(31, 24, instant("2028-02-01T08:00"), zone).dueDate)
    }
    @Test fun decemberAndLongLeadFindJanuary() {
        val result = ReminderTime.next(1, 72, instant("2026-12-10T08:00"), zone)
        assertEquals(LocalDate.of(2027, 1, 1), result.dueDate)
        assertEquals(instant("2026-12-29T09:00"), result.trigger)
    }
    @Test fun largestOffsetAlwaysReturnsAFutureOccurrence() {
        val now = instant("2026-02-28T23:59")
        assertTrue(ReminderTime.next(1, 672, now, zone).trigger.isAfter(now))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroIsRejected() { ReminderTime.next(10, 0, Instant.now(), zone) }
    @Test(expected = IllegalArgumentException::class) fun hugeOffsetIsRejected() { ReminderTime.next(10, 673, Instant.now(), zone) }
}
