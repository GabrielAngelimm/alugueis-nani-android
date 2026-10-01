package com.rentalvalidator.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity

data class BackupSnapshot(
    val units: List<UnitEntity>,
    val tenants: List<TenantEntity>,
    val payments: List<PaymentEntity>
)

@Dao
abstract class BackupDao {

    @Transaction
    open suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        readUnits(), readTenants(), readPayments()
    )

    @Query("SELECT * FROM rental_units ORDER BY id")
    protected abstract suspend fun readUnits(): List<UnitEntity>

    @Query("SELECT * FROM tenants ORDER BY id")
    protected abstract suspend fun readTenants(): List<TenantEntity>

    @Query("SELECT * FROM payments ORDER BY id")
    protected abstract suspend fun readPayments(): List<PaymentEntity>

    @Transaction
    open suspend fun replaceAllData(
        units: List<UnitEntity>,
        tenants: List<TenantEntity>,
        payments: List<PaymentEntity>
    ) {
        clearPayments()
        clearTenants()
        clearUnits()
        insertUnits(units)
        insertTenants(tenants)
        insertPayments(payments)
    }

    @Query("DELETE FROM payments")
    protected abstract suspend fun clearPayments()

    @Query("DELETE FROM tenants")
    protected abstract suspend fun clearTenants()

    @Query("DELETE FROM rental_units")
    protected abstract suspend fun clearUnits()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertUnits(units: List<UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertTenants(tenants: List<TenantEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPayments(payments: List<PaymentEntity>)
}
