package com.rentalvalidator.app.data.repository

import com.rentalvalidator.app.data.local.dao.PaymentDao
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao
) : PaymentRepository {

    override fun getPaymentsFlow(year: Int): Flow<List<Payment>> {
        return paymentDao.getPaymentsFlow(year).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPaymentsByYear(year: Int): List<Payment> {
        return paymentDao.getPaymentsByYear(year).map { it.toDomain() }
    }

    override suspend fun getPaymentsForTenant(tenantId: String): List<Payment> {
        return paymentDao.getPaymentsForTenant(tenantId).map { it.toDomain() }
    }

    override suspend fun getPayment(tenantId: String, year: Int, month: Int): Payment? {
        return paymentDao.getPayment(tenantId, year, month)?.toDomain()
    }

    override suspend fun savePayment(tenantId: String, year: Int, month: Int, status: PaymentStatus) {
        paymentDao.savePayment(tenantId, year, month, status.name)
    }

    override suspend fun deletePayment(tenantId: String, year: Int, month: Int) {
        paymentDao.deletePayment(tenantId, year, month)
    }

    override suspend fun insertPayments(payments: List<Payment>) {
        paymentDao.insertPayments(payments.map { PaymentEntity.fromDomain(it) })
    }

    override suspend fun clearAll() {
        paymentDao.clearAll()
    }
}
