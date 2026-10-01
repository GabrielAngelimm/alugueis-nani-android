package com.rentalvalidator.app.presentation.ui.tenants

import org.junit.Assert.*
import org.junit.Test

class FormValidationTest {
    @Test fun optionalFieldsAndPhoneLengths() {
        assertTrue(validOptionalPhone(""))
        assertTrue(validOptionalPhone("1131234567"))
        assertTrue(validOptionalPhone("11987654321"))
        assertFalse(validOptionalPhone("1198765"))
        assertFalse(validOptionalPhone("119876543210"))
        assertFalse(validOptionalPhone("11887654321"))
    }
    @Test fun cpfChecksBothDigits() {
        assertTrue(validOptionalCpf(""))
        assertTrue(validOptionalCpf("52998224725"))
        assertFalse(validOptionalCpf("52998224724"))
        assertFalse(validOptionalCpf("11111111111"))
        assertFalse(validOptionalCpf("123"))
    }
    @Test fun monetaryBoundsAndPrecision() {
        assertTrue(validMoney("1250,50"))
        assertTrue(validMoney("0", allowZero = true))
        listOf("", "0", "-1", "NaN", "Infinity", "1.234", ".").forEach { assertFalse(it, validMoney(it)) }
    }
}
