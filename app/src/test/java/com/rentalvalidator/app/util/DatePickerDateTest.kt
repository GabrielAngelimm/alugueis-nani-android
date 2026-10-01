package com.rentalvalidator.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

class DatePickerDateTest {
    @Test fun `selected calendar day is independent of device time zone`() {
        val original = TimeZone.getDefault()
        try {
            val selected = Instant.parse("2026-09-30T00:00:00Z").toEpochMilli()
            listOf("America/Sao_Paulo", "Pacific/Honolulu", "Asia/Tokyo").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(LocalDate.of(2026, 9, 30), DateUtils.fromDatePickerUtc(selected))
            }
        } finally { TimeZone.setDefault(original) }
    }
}
