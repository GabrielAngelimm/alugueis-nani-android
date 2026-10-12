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
    fun currencyParsingReadsInternationalNotationToo() {
        // Some banks export CSVs with a dot as the decimal separator; reading it as thousands made 1450.00 worth R$ 145.000.
        assertEquals(1450.0, CurrencyUtils.parse("1450.00"), 0.0)
        assertEquals(1450.5, CurrencyUtils.parse("1,450.50"), 0.0)
        assertEquals(-12.0, CurrencyUtils.parse("-12.00"), 0.0)
        assertEquals(99.9, CurrencyUtils.parse("99.9"), 0.0)
        // A dot or comma followed by three digits still groups thousands.
        assertEquals(1500.0, CurrencyUtils.parse("1.500"), 0.0)
        assertEquals(1234567.0, CurrencyUtils.parse("1.234.567"), 0.0)
        assertEquals(1250.5, CurrencyUtils.parse("1250,5"), 0.0)
        // A trailing minus, as some statements print debits, is still negative.
        assertEquals(-80.0, CurrencyUtils.parse("80,00-"), 0.0)
        assertEquals(0.0, CurrencyUtils.parse("R$"), 0.0)
        assertEquals(0.0, CurrencyUtils.parse(""), 0.0)
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
