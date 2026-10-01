package com.rentalvalidator.app.data.local.dao

import androidx.room.*
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE year = :year")
    fun getPaymentsFlow(year: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE year = :year")
    suspend fun getPaymentsByYear(year: Int): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE tenantId = :tenantId")
    suspend fun getPaymentsForTenant(tenantId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE tenantId = :tenantId AND year = :year AND month = :month")
    suspend fun getPayment(tenantId: String, year: Int, month: Int): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Transaction
    suspend fun savePayment(tenantId: String, year: Int, month: Int, status: String) {
        val existing = getPayment(tenantId, year, month)
        insertPayment(existing?.copy(status = status) ?: PaymentEntity(
            tenantId = tenantId, year = year, month = month, status = status
        ))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE tenantId = :tenantId AND year = :year AND month = :month")
    suspend fun deletePayment(tenantId: String, year: Int, month: Int)

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}
