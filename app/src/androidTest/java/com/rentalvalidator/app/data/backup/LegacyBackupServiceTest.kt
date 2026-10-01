package com.rentalvalidator.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rentalvalidator.app.data.local.AppDatabase
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LegacyBackupServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val service = LegacyBackupService(context, db.backupDao())
    private val file = File(context.getExternalFilesDir(null), "contracts/legacy-test-${UUID.randomUUID()}.json")
        .apply { parentFile!!.mkdirs() }
    private val uri: Uri get() = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    @After fun close() { db.close(); file.delete() }

    @Test fun versionFourRoundTripPreservesRecordsAndAllPaymentYears() = runBlocking {
        val unit = UnitEntity.fromDomain(RentalUnit("unit", "Unidade exemplo", UnitType.OTHER))
        val tenant = tenant().copy(unitId = unit.id)
        val payments = listOf(PaymentEntity(1, tenant.id, 2025, 12, "PAGO"), PaymentEntity(2, tenant.id, 2026, 1, "PENDENTE"))
        db.backupDao().replaceAllData(listOf(unit), listOf(tenant), payments)
        service.export(uri)
        assertEquals(4, JSONObject(file.readText()).getInt("version"))
        db.backupDao().replaceAllData(emptyList(), emptyList(), emptyList())
        service.restore(uri)
        assertEquals(listOf(unit), db.unitDao().getAllUnits())
        assertEquals(listOf(tenant), db.tenantDao().getAllTenants())
        assertEquals(setOf(2025 to 12, 2026 to 1), db.paymentDao().getPaymentsForTenant(tenant.id).map { it.year to it.month }.toSet())
    }

    @Test fun oldBackupDiscoversUnitsAndKeepsExistingStatuses() = runBlocking {
        file.writeText("""{"version":1,"tenants":[{"id":"tenant","name":"Morador exemplo","amount":1000,"dueDay":10,"unit":"Unidade exemplo","payments":{"2026":{"09":"PAGO"}}}]}""")
        service.restore(uri)
        val tenant = db.tenantDao().getTenantById("tenant")!!
        assertEquals("Unidade exemplo", db.unitDao().getAllUnits().single().name)
        assertEquals(db.unitDao().getAllUnits().single().id, tenant.unitId)
        assertEquals("PAGO", db.paymentDao().getPaymentsForTenant(tenant.id).single().status)
    }

    @Test fun invalidMonthOrDuplicateTenantDoesNotReplaceExistingRecords() = runBlocking {
        val original = tenant()
        db.tenantDao().insertTenant(original)
        listOf(
            """{"version":1,"tenants":[{"id":"other","name":"Outro exemplo","amount":1000,"dueDay":10,"payments":{"2026":{"13":"PAGO"}}}]}""",
            """{"version":1,"tenants":[{"id":"same","name":"Exemplo A","amount":1000,"dueDay":10},{"id":"same","name":"Exemplo B","amount":1000,"dueDay":10}]}"""
        ).forEach { payload ->
            file.writeText(payload)
            assertTrue(runCatching { service.restore(uri) }.isFailure)
            assertEquals(listOf(original), db.tenantDao().getAllTenants())
        }
    }

    private fun tenant() = TenantEntity("tenant", "Morador exemplo", 1000.0, 10, "", "", "", "", "Unidade exemplo", emptyList(), "", "", "", "2026-01-01")
}
