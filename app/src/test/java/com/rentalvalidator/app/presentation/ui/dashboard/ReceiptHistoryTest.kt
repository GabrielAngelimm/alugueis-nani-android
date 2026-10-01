package com.rentalvalidator.app.presentation.ui.dashboard

import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class ReceiptHistoryTest {
    @Test fun `January history uses previous year payments and excludes future namesake months`() {
        val tenant = Tenant("example", "Morador exemplo", 1000.0, 10)
        val payments = listOf(
            Payment(1, tenant.id, 2025, 11, PaymentStatus.PAGO),
            Payment(2, tenant.id, 2026, 11, PaymentStatus.PAGO),
            Payment(3, tenant.id, 2026, 1, PaymentStatus.EM_ANALISE)
        )
        val history = receiptHistorySnapshot(YearMonth.of(2026, 1), listOf(tenant), payments)
        assertEquals(listOf(8, 9, 10, 11, 12, 1), history.months)
        assertEquals(listOf(0f, 0f, 0f, 1000f, 0f, 0f), history.values)
    }
}
