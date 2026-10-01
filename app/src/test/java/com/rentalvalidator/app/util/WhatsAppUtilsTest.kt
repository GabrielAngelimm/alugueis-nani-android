package com.rentalvalidator.app.util

import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.usecase.PaymentStatus
import com.rentalvalidator.app.domain.usecase.ValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppUtilsTest {
    private val result = ValidationResult(
        tenant = Tenant("tenant-1", "Gabriel Costa", 900.0, 10),
        status = PaymentStatus.PARCIAL,
        amountDue = 900.0,
        amountPaid = 100.0,
        penaltyApplied = 180.0
    )

    @Test fun `ordinary billing does not include a late fee`() {
        val message = WhatsAppUtils.createBillingMessage(result, "setembro")

        assertFalse(message.contains("Multa:"))
        assertFalse(message.contains("Total:"))
    }

    @Test fun `optional late fee uses remaining rent and only appears when selected`() {
        val message = WhatsAppUtils.createBillingMessage(result, "setembro", includePenalty = true)

        assertTrue(message.contains("Valor restante do aluguel: ${CurrencyUtils.format(800.0)}"))
        assertTrue(message.contains("Multa: ${CurrencyUtils.format(180.0)}"))
        assertTrue(message.contains("Total: ${CurrencyUtils.format(980.0)}"))
    }
}
