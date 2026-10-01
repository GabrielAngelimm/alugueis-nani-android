package com.rentalvalidator.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CpfUtilsTest {
    @Test
    fun format_appliesBrazilianCpfMask() {
        assertEquals("123.456.789-01", CpfUtils.format("12345678901"))
    }

    @Test
    fun digits_removesFormattingAndLimitsLength() {
        assertEquals("12345678901", CpfUtils.digits("123.456.789-01 extra"))
    }

    @Test
    fun optionalCpf_acceptsBlankOrElevenDigits() {
        assertTrue(CpfUtils.isCompleteOrBlank(""))
        assertTrue(CpfUtils.isCompleteOrBlank("123.456.789-01"))
        assertFalse(CpfUtils.isCompleteOrBlank("123"))
    }
}
