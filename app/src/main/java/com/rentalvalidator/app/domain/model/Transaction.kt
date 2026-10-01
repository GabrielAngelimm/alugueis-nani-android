package com.rentalvalidator.app.domain.model

import java.time.LocalDate

enum class TransactionType {
    CREDIT, DEBIT
}

data class Transaction(
    val date: LocalDate,
    val description: String,
    val amount: Double,
    val type: TransactionType
)
