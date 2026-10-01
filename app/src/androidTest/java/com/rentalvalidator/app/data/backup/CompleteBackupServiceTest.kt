package com.rentalvalidator.app.data.backup

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rentalvalidator.app.data.local.AppDatabase
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.dao.BackupSnapshot
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.reminders.RentReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Exercises the same SAF URI boundary used by Settings, with a private in-memory database. */
@RunWith(AndroidJUnit4::class)
class CompleteBackupServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val runId = UUID.randomUUID().toString()
    private val tenantId = "backup-tenant-$runId"
    private val unitId = "backup-unit-$runId"
    private val files = mutableSetOf<File>()
    private lateinit var db: AppDatabase
    private lateinit var preferences: PreferencesManager
    private lateinit var scheduler: RentReminderScheduler
    private lateinit var service: CompleteBackupService
    private lateinit var originalTheme: AppTheme
    private var originalPenalty = 20.0
    private var originalInterest = 0.0

    @Before fun setUp() = runBlocking {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .adoptShellPermissionIdentity("android.permission.POST_NOTIFICATIONS")
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        preferences = PreferencesManager(context)
        originalTheme = preferences.themeModeFlow.first()
        preferences.penaltyConfigFlow.first().also {
            originalPenalty = it.penaltyPct
            originalInterest = it.dailyInterestPct
        }
        val isolatedReminders = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) =
                context.getSharedPreferences("backup-test-reminders-$runId", mode)
        }
        scheduler = RentReminderScheduler(isolatedReminders, db.tenantDao())
        service = CompleteBackupService(context, db.backupDao(), preferences, scheduler)
    }

    @After fun tearDown() = runBlocking {
        if (::scheduler.isInitialized) {
            scheduler.remove(tenantId)
            context.getSharedPreferences("backup-test-reminders-$runId", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
        if (::preferences.isInitialized) {
            preferences.setThemeMode(originalTheme)
            preferences.updatePenaltyConfig(originalPenalty, originalInterest)
        }
        if (::db.isInitialized) db.close()
        files.forEach { it.delete() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.dropShellPermissionIdentity()
    }

    @Test fun fullArchiveRestoresRecordsAttachmentsSettingsAndReminder() = runBlocking {
        val photoBytes = "photo-$runId".toByteArray()
        val contractBytes = "%PDF-1.4\ncontract-$runId".toByteArray()
        val inspectionBytes = "%PDF-1.4\ninspection-$runId".toByteArray()
        val photo = fixtureFile("unit_photos", "original-$runId.jpg", photoBytes)
        val contract = fixtureFile("contracts", "original-$runId.pdf", contractBytes)
        val inspection = fixtureFile("inspections", "original-$runId.pdf", inspectionBytes)
        val unit = fixtureUnit(photo.absolutePath)
        val tenant = fixtureTenant(contract.absolutePath, inspection.absolutePath)
        db.unitDao().insertUnit(unit)
        db.tenantDao().insertTenant(tenant)
        db.paymentDao().insertPayment(PaymentEntity(tenantId = tenantId, year = 2025, month = 12, status = "PAGO"))
        db.paymentDao().insertPayment(PaymentEntity(tenantId = tenantId, year = 2026, month = 1, status = "PENDENTE"))
        preferences.setThemeMode(AppTheme.DARK)
        preferences.updatePenaltyConfig(12.5, 0.4)
        scheduler.save(tenantId, 48)

        val archive = archiveFile()
        val exported = service.export(archiveUri(archive))
        assertEquals(1, exported.units)
        assertEquals(1, exported.tenants)
        assertEquals(2, exported.payments)
        assertEquals(3, exported.documents)
        assertEquals(1, exported.reminders)
        assertTrue(exported.includesSettings)
        assertTrue(exported.includesReminders)
        assertTrue(archive.length() > 0)
        assertEquals(exported, service.inspect(archiveUri(archive)))

        db.backupDao().replaceAllData(emptyList(), listOf(fixtureTenant("", "").copy(name = "Changed")), emptyList())
        preferences.setThemeMode(AppTheme.LIGHT)
        preferences.updatePenaltyConfig(3.0, 0.0)
        scheduler.remove(tenantId)
        photo.delete()
        contract.delete()
        inspection.delete()

        val restored = service.restore(archiveUri(archive))
        assertEquals(exported, restored)
        val restoredUnit = db.unitDao().getUnitById(unitId)!!
        val restoredTenant = db.tenantDao().getTenantById(tenantId)!!
        assertEquals(unit.copy(photoPath = restoredUnit.photoPath), restoredUnit)
        assertEquals(tenant.copy(contractPath = restoredTenant.contractPath,
            inspectionPath = restoredTenant.inspectionPath), restoredTenant)
        assertNotEquals(photo.absolutePath, restoredUnit.photoPath)
        assertNotEquals(contract.absolutePath, restoredTenant.contractPath)
        assertArrayEquals(photoBytes, restoredUnit.photoPath!!.let(::File).readBytes())
        assertArrayEquals(contractBytes, File(restoredTenant.contractPath).readBytes())
        assertArrayEquals(inspectionBytes, File(restoredTenant.inspectionPath).readBytes())
        files += File(restoredUnit.photoPath!!)
        files += File(restoredTenant.contractPath)
        files += File(restoredTenant.inspectionPath)
        assertEquals(setOf(2025 to 12, 2026 to 1),
            db.paymentDao().getPaymentsForTenant(tenantId).map { it.year to it.month }.toSet())
        assertEquals(AppTheme.DARK, preferences.themeModeFlow.first())
        assertEquals(12.5, preferences.penaltyConfigFlow.first().penaltyPct, 0.0)
        assertEquals(0.4, preferences.penaltyConfigFlow.first().dailyInterestPct, 0.0)
        assertEquals(48, scheduler.get(tenantId)?.leadHours)
        assertEquals(tenant.dueDay, scheduler.get(tenantId)?.dueDay)
    }

    @Test fun damagedArchiveDoesNotChangeExistingState() = runBlocking {
        val originalTenant = fixtureTenant("", "")
        db.unitDao().insertUnit(fixtureUnit(null))
        db.tenantDao().insertTenant(originalTenant)
        preferences.setThemeMode(AppTheme.DARK)
        preferences.updatePenaltyConfig(15.0, 0.2)
        scheduler.save(tenantId, 24)

        val archive = archiveFile()
        service.export(archiveUri(archive))
        val completeBytes = archive.readBytes()
        assertTrue(completeBytes.size > 16)
        archive.writeBytes(completeBytes.copyOf(8))

        val beforeUnits = db.unitDao().getAllUnits()
        val beforeTenants = db.tenantDao().getAllTenants()
        val beforeReminder = scheduler.get(tenantId)
        val failed = runCatching { service.restore(archiveUri(archive)) }.isFailure
        assertTrue("Truncated archives must be rejected", failed)
        assertEquals(beforeUnits, db.unitDao().getAllUnits())
        assertEquals(beforeTenants, db.tenantDao().getAllTenants())
        assertEquals(AppTheme.DARK, preferences.themeModeFlow.first())
        assertEquals(15.0, preferences.penaltyConfigFlow.first().penaltyPct, 0.0)
        assertEquals(0.2, preferences.penaltyConfigFlow.first().dailyInterestPct, 0.0)
        assertEquals(beforeReminder, scheduler.get(tenantId))
    }

    @Test fun missingReferencedAttachmentPreventsIncompleteExport() = runBlocking {
        db.unitDao().insertUnit(fixtureUnit(null))
        val missing = File(context.getExternalFilesDir(null), "contracts/missing-$runId.pdf")
        missing.delete()
        db.tenantDao().insertTenant(fixtureTenant(missing.absolutePath, ""))
        val archive = archiveFile()

        assertTrue(runCatching { service.export(archiveUri(archive)) }.isFailure)
        assertEquals(0L, archive.length())
        assertEquals(missing.absolutePath, db.tenantDao().getTenantById(tenantId)?.contractPath)
    }

    @Test fun backupWithoutSettingsOrRemindersKeepsCurrentDeviceChoices() = runBlocking {
        db.unitDao().insertUnit(fixtureUnit(null))
        val originalTenant = fixtureTenant("", "")
        db.tenantDao().insertTenant(originalTenant)
        preferences.setThemeMode(AppTheme.DARK)
        preferences.updatePenaltyConfig(11.0, 0.1)
        scheduler.save(tenantId, 24)
        val archive = archiveFile()

        val summary = service.export(archiveUri(archive), BackupOptions(
            includeDocuments = false, includeSettings = false, includeReminders = false
        ))
        assertFalse(summary.includesSettings)
        assertFalse(summary.includesReminders)
        assertEquals(0, summary.documents)
        assertEquals(0, summary.reminders)

        db.tenantDao().updateTenant(originalTenant.copy(name = "Temporary name"))
        preferences.setThemeMode(AppTheme.LIGHT)
        preferences.updatePenaltyConfig(7.0, 0.3)
        scheduler.save(tenantId, 72)
        service.restore(archiveUri(archive))

        assertEquals(originalTenant, db.tenantDao().getTenantById(tenantId))
        assertEquals(AppTheme.LIGHT, preferences.themeModeFlow.first())
        assertEquals(7.0, preferences.penaltyConfigFlow.first().penaltyPct, 0.0)
        assertEquals(0.3, preferences.penaltyConfigFlow.first().dailyInterestPct, 0.0)
        assertEquals(72, scheduler.get(tenantId)?.leadHours)
    }

    private fun fixtureUnit(photoPath: String?) = UnitEntity.fromDomain(RentalUnit(
        id = unitId,
        name = "Casa backup $runId",
        type = UnitType.HOUSE,
        photoPath = photoPath,
        location = "Rua das Acácias, 42",
        capacity = 3,
        condominiumFee = 123.45,
        notes = "Portão azul",
        createdAt = "2025-10-01T12:00:00Z",
        updatedAt = "2026-09-01T12:00:00Z"
    ))

    @Test fun cancellationAfterDatabaseReplacementRestoresPreviousState() = runBlocking {
        db.unitDao().insertUnit(fixtureUnit(null))
        val archived = fixtureTenant("", "")
        db.tenantDao().insertTenant(archived)
        val archive = archiveFile()
        service.export(archiveUri(archive), BackupOptions(includeDocuments = false))
        val current = archived.copy(name = "Cadastro atual")
        db.tenantDao().updateTenant(current)
        preferences.setThemeMode(AppTheme.LIGHT)
        preferences.updatePenaltyConfig(7.0, 0.0)
        val written = CompletableDeferred<Unit>()
        val proceed = CompletableDeferred<Unit>()
        val actual = db.backupDao()
        val intercept = object : BackupDao() {
            var replacements = 0
            override suspend fun snapshot(): BackupSnapshot = actual.snapshot()
            override suspend fun replaceAllData(units: List<UnitEntity>, tenants: List<TenantEntity>, payments: List<PaymentEntity>) {
                actual.replaceAllData(units, tenants, payments)
                if (++replacements == 1) { written.complete(Unit); proceed.await() }
            }
            override suspend fun readUnits(): List<UnitEntity> = error("unused")
            override suspend fun readTenants(): List<TenantEntity> = error("unused")
            override suspend fun readPayments(): List<PaymentEntity> = error("unused")
            override suspend fun clearPayments() = error("unused")
            override suspend fun clearTenants() = error("unused")
            override suspend fun clearUnits() = error("unused")
            override suspend fun insertUnits(units: List<UnitEntity>) = error("unused")
            override suspend fun insertTenants(tenants: List<TenantEntity>) = error("unused")
            override suspend fun insertPayments(payments: List<PaymentEntity>) = error("unused")
        }
        val restoring = launch { CompleteBackupService(context, intercept, preferences, scheduler).restore(archiveUri(archive)) }
        try {
            withTimeout(5_000) { written.await() }
            restoring.cancel()
        } finally { proceed.complete(Unit) }
        withTimeout(5_000) { restoring.join() }
        assertEquals(current, db.tenantDao().getTenantById(tenantId))
        assertEquals(AppTheme.LIGHT, preferences.themeModeFlow.first())
        assertEquals(7.0, preferences.penaltyConfigFlow.first().penaltyPct, 0.0)
    }

    private fun fixtureTenant(contractPath: String, inspectionPath: String) = TenantEntity(
        id = tenantId,
        name = "Marina Costa",
        amount = 2_450.75,
        dueDay = 17,
        bank = "Nubank",
        phone = "11987654321",
        cpf = "12345678909",
        whatsappName = "Marina",
        unit = "Casa backup $runId",
        aliases = listOf("Marina C.", "M. Costa"),
        contractPath = contractPath,
        contractExpirationDate = "2028-12-31",
        inspectionPath = inspectionPath,
        dateCreated = "2025-10-01",
        unitId = unitId
    )

    private fun fixtureFile(folder: String, name: String, bytes: ByteArray): File =
        File(context.getExternalFilesDir(null), "$folder/$name").also {
            it.parentFile!!.mkdirs()
            it.writeBytes(bytes)
            files += it
        }

    private fun archiveFile(): File = fixtureFile("contracts", "backup-test-$runId.nani", byteArrayOf())

    private fun archiveUri(file: File): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
}
