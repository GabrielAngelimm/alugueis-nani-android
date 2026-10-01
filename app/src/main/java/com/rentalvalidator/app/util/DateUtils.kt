package com.rentalvalidator.app.util

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    /** Material DatePicker represents a calendar day as midnight UTC. */
    fun fromDatePickerUtc(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private val ddmmyyyyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val ddmmFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parseDate(dateStr: String?, referenceYear: Int = LocalDate.now().year): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        
        val trimmed = dateStr.trim()
        
        return try {
            if (trimmed.length == 5 && trimmed.contains("/")) {
                val parsed = java.time.MonthDay.parse(trimmed, ddmmFormatter)
                parsed.atYear(referenceYear)
            } else if (trimmed.contains("-")) {
                LocalDate.parse(trimmed, yyyymmddFormatter)
            } else {
                LocalDate.parse(trimmed, ddmmyyyyFormatter)
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun formatDate(date: LocalDate?): String {
        if (date == null) return ""
        return date.format(ddmmyyyyFormatter)
    }
}
