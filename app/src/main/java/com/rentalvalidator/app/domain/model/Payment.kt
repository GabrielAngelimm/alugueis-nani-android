package com.rentalvalidator.app.domain.model

enum class PaymentStatus {
    PAGO, PENDENTE, EM_ANALISE
}

data class Payment(
    val id: Long = 0,
    val tenantId: String,
    val year: Int,
    val month: Int,
    val status: PaymentStatus
)
