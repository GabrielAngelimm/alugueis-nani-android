package com.rentalvalidator.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rentalvalidator.app.data.local.dao.PaymentDao
import com.rentalvalidator.app.data.local.dao.TenantDao
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.data.repository.UnitRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var tenantDao: TenantDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var backupDao: BackupDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        tenantDao = db.tenantDao()
        paymentDao = db.paymentDao()
        backupDao = db.backupDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndReadTenant() = runBlocking {
        val tenant = TenantEntity(
            id = "test-uuid",
            name = "Inquilino Teste",
            amount = 1200.00,
            dueDay = 10,
            bank = "Nubank",
            phone = "11999999999",
            cpf = "",
            whatsappName = "Inquilino WhatsApp",
            unit = "Apto 101",
            aliases = listOf("Alias 1", "Alias 2"),
            contractPath = "",
            contractExpirationDate = "2027-01-01",
            inspectionPath = "",
            dateCreated = "2026-05-25"
        )
        tenantDao.insertTenant(tenant)
        val list = tenantDao.getTenantsFlow().first()
        assertEquals(1, list.size)
        assertEquals("Inquilino Teste", list[0].name)
        assertEquals(listOf("Alias 1", "Alias 2"), list[0].aliases)
    }

    @Test
    @Throws(Exception::class)
    fun testPaymentAndCascadeDelete() = runBlocking {
        val tenant = TenantEntity(
            id = "tenant-1",
            name = "Inquilino 1",
            amount = 1000.0,
            dueDay = 5,
            bank = "Itau",
            phone = "",
            cpf = "",
            whatsappName = "",
            unit = "Apto 102",
            aliases = emptyList(),
            contractPath = "",
            contractExpirationDate = "",
            inspectionPath = "",
            dateCreated = ""
        )
        tenantDao.insertTenant(tenant)

        val payment = PaymentEntity(
            tenantId = "tenant-1",
            year = 2026,
            month = 5,
            status = "PAGO"
        )
        paymentDao.insertPayment(payment)

        val paymentsList = paymentDao.getPaymentsByYear(2026)
        assertEquals(1, paymentsList.size)
        assertEquals("PAGO", paymentsList[0].status)

        // Delete tenant and verify cascade delete
        tenantDao.deleteTenant(tenant)
        val paymentsAfterDelete = paymentDao.getPaymentsByYear(2026)
        assertTrue(paymentsAfterDelete.isEmpty())
    }

    @Test
    fun backupRestoreRollsBackWhenAnInsertFails() = runBlocking {
        val originalTenant = tenantEntity(id = "original", name = "Cadastro original")
        tenantDao.insertTenant(originalTenant)

        val replacementTenant = tenantEntity(id = "replacement", name = "Novo cadastro")
        val invalidPayment = PaymentEntity(
            tenantId = "tenant-that-does-not-exist",
            year = 2026,
            month = 8,
            status = "PAGO"
        )

        var failed = false
        try {
            backupDao.replaceAllData(
                units = emptyList(),
                tenants = listOf(replacementTenant),
                payments = listOf(invalidPayment)
            )
        } catch (_: Exception) {
            failed = true
        }

        assertTrue(failed)
        val tenantsAfterFailure = tenantDao.getAllTenants()
        assertEquals(listOf("original"), tenantsAfterFailure.map { it.id })
    }

    private fun tenantEntity(id: String, name: String) = TenantEntity(
        id = id,
        name = name,
        amount = 1000.0,
        dueDay = 10,
        bank = "",
        phone = "",
        cpf = "",
        whatsappName = "",
        unit = "Geral",
        aliases = emptyList(),
        contractPath = "",
        contractExpirationDate = "",
        inspectionPath = "",
        dateCreated = "2026-08-27"
    )

    @Test fun unitOccupancyUpdatesWhenOnlyTenantsChange() = runBlocking {
        val unit = UnitEntity.fromDomain(RentalUnit("unit-test", "Unidade exemplo", UnitType.OTHER))
        db.unitDao().insertUnit(unit)
        val repository = UnitRepositoryImpl(db.unitDao())
        assertEquals(0, repository.getUnitsFlow().first().single().tenantCount)
        val changed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { repository.getUnitsFlow().first { it.single().tenantCount == 1 } }
        }
        tenantDao.insertTenant(tenantEntity("linked", "Morador exemplo").copy(unitId = unit.id, unit = unit.name))
        assertEquals(1, changed.await().single().tenantCount)
        assertEquals(1, repository.getAllUnits().single().tenantCount)
    }

    @Test fun upsertingTenantPreservesItsPaymentHistory() = runBlocking {
        val tenant = tenantEntity("upsert", "Morador exemplo")
        tenantDao.insertTenant(tenant)
        paymentDao.savePayment(tenant.id, 2026, 9, "PAGO")
        val originalId = paymentDao.getPaymentsForTenant(tenant.id).single().id
        tenantDao.insertTenant(tenant.copy(name = "Morador atualizado"))
        assertEquals(originalId, paymentDao.getPaymentsForTenant(tenant.id).single().id)
        paymentDao.savePayment(tenant.id, 2026, 9, "EM_ANALISE")
        assertEquals(originalId, paymentDao.getPaymentsForTenant(tenant.id).single().id)
    }

    @Test fun renamingUnitUpdatesItsLinkedTenantAndDeletingItIsBlocked() = runBlocking {
        val unit = RentalUnit("unit-test", "Unidade exemplo", UnitType.OTHER)
        val repository = UnitRepositoryImpl(db.unitDao())
        repository.insertUnit(unit)
        tenantDao.insertTenant(tenantEntity("linked", "Morador exemplo").copy(unitId = unit.id, unit = unit.name))
        repository.updateUnit(unit.copy(name = "Unidade atualizada", location = "Endereço de exemplo"))
        assertEquals("Unidade atualizada", tenantDao.getTenantById("linked")?.unit)
        assertEquals("Endereço de exemplo", repository.getUnitById(unit.id)?.location)
        assertTrue(runCatching { repository.deleteUnit(unit.id) }.isFailure)
        assertEquals(1, db.unitDao().getAllUnits().size)
    }

    @Test fun duplicateBackupIdsAreRejectedWithoutReplacingOriginalData() = runBlocking {
        val original = tenantEntity("original", "Cadastro original")
        tenantDao.insertTenant(original)
        val duplicate = tenantEntity("duplicate", "Cadastro duplicado")
        assertTrue(runCatching { backupDao.replaceAllData(emptyList(), listOf(duplicate, duplicate), emptyList()) }.isFailure)
        assertEquals(listOf(original), tenantDao.getAllTenants())
    }
}
