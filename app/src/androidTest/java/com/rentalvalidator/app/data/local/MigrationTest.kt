package com.rentalvalidator.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

/** Opens historical exported schemas, then lets Room validate the production migration chain. */
class MigrationTest {
    @Test fun everyHistoricalSchemaMigratesWithoutLosingTenantsOrPayments() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (version in 1..3) {
            val name = "migration-test-${UUID.randomUUID()}"
            val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
            val schema = InstrumentationRegistry.getInstrumentation().context.assets
                .open("${AppDatabase::class.java.name}/$version.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices") ?: JSONArray()
                    for (index in 0 until indices.length()) {
                        old.execSQL(indices.getJSONObject(index).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
                old.execSQL("INSERT INTO tenants (id,name,amount,dueDay,bank,phone,whatsappName,unit,aliases,contractPath,contractExpirationDate,inspectionPath,dateCreated) VALUES ('tenant','Morador exemplo',1000,10,'','','','Unidade descoberta','[]','','','','2026-01-01')")
                old.execSQL("INSERT INTO payments (tenantId,year,month,status) VALUES ('tenant',2026,9,'PAGO')")
                if (version == 3) {
                    old.execSQL("INSERT INTO rental_units (id,name,type,location,capacity,capacityKind,operationalStatus,notes,createdAt,updatedAt) VALUES ('discovered','Unidade descoberta','OTHER','',1,'TENANTS','ACTIVE','','','')")
                }
                old.version = version
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
            try {
                val tenant = migrated.tenantDao().getTenantById("tenant")!!
                assertEquals("Morador exemplo", tenant.name)
                assertEquals("Unidade descoberta", tenant.unit)
                assertNotNull(tenant.unitId)
                assertEquals("Unidade descoberta", migrated.unitDao().getUnitById(tenant.unitId!!)?.name)
                assertEquals("PAGO", migrated.paymentDao().getPaymentsForTenant("tenant").single().status)
                assertEquals(4, migrated.openHelper.writableDatabase.version)
            } finally { migrated.close(); context.deleteDatabase(name) }
        }
    }
}
