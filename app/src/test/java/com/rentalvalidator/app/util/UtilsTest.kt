package com.rentalvalidator.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UtilsTest {

    @Test
    fun testTextNormalizer() {
        assertEquals("joao da silva", TextNormalizer.normalize(" João  da Silva "))
        assertEquals("carlos", TextNormalizer.normalize("Cárlós"))
        assertEquals("", TextNormalizer.normalize(null))
    }

    @Test
    fun testPhoneUtils() {
        assertEquals("11999999999", PhoneUtils.getDigits("(11) 99999-9999"))
        assertEquals("(11) 99999-9999", PhoneUtils.formatPhone("11999999999"))
        assertEquals("(11) 8888-8888", PhoneUtils.formatPhone("1188888888"))
        assertEquals("invalid", PhoneUtils.formatPhone("invalid"))
    }

    @Test
    fun testCurrencyUtils() {
        assertEquals(1500.50, CurrencyUtils.parse("1.500,50"), 0.0)
        assertEquals(1500.50, CurrencyUtils.parse("R$ 1.500,50"), 0.0)
        assertEquals(-100.0, CurrencyUtils.parse("-100,00"), 0.0)
        assertEquals(0.0, CurrencyUtils.parse(null), 0.0)
    }

    @Test
    fun testDateUtils() {
        val currentYear = LocalDate.now().year
        assertEquals(LocalDate.of(2026, 5, 12), DateUtils.parseDate("12/05/2026"))
        assertEquals(LocalDate.of(currentYear, 5, 12), DateUtils.parseDate("12/05"))
        assertEquals(LocalDate.of(2024, 5, 12), DateUtils.parseDate("12/05", referenceYear = 2024))
        assertEquals(LocalDate.of(2026, 5, 12), DateUtils.parseDate("2026-05-12"))
        assertEquals("12/05/2026", DateUtils.formatDate(LocalDate.of(2026, 5, 12)))
    }
}
