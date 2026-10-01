package com.rentalvalidator.app.domain.repository

import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPaymentsFlow(year: Int): Flow<List<Payment>>
    suspend fun getPaymentsByYear(year: Int): List<Payment>
    suspend fun getPaymentsForTenant(tenantId: String): List<Payment>
    suspend fun getPayment(tenantId: String, year: Int, month: Int): Payment?
    suspend fun savePayment(tenantId: String, year: Int, month: Int, status: PaymentStatus)
    suspend fun deletePayment(tenantId: String, year: Int, month: Int)
    suspend fun insertPayments(payments: List<Payment>)
    suspend fun clearAll()
}
