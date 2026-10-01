package com.rentalvalidator.app.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class ReminderOccurrence(val dueDate: LocalDate, val trigger: Instant)

/** Calendar months, including February and due days 29–31. No financial rules are changed. */
object ReminderTime {
    fun next(dueDay: Int, leadHours: Int, now: Instant, zone: ZoneId): ReminderOccurrence {
        require(dueDay in 1..31)
        require(leadHours in 1..672)
        val month = YearMonth.from(now.atZone(zone))
        return (0L..3L).map { offset ->
            val period = month.plusMonths(offset)
            val due = period.atDay(minOf(dueDay, period.lengthOfMonth()))
            ReminderOccurrence(due, due.atTime(LocalTime.of(9, 0)).atZone(zone).minusHours(leadHours.toLong()).toInstant())
        }.first { it.trigger.isAfter(now) }
    }
}
