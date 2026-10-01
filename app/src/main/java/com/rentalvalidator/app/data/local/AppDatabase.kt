package com.rentalvalidator.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rentalvalidator.app.data.local.converters.Converters
import com.rentalvalidator.app.data.local.dao.PaymentDao
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.dao.TenantDao
import com.rentalvalidator.app.data.local.dao.UnitDao
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.RentalUnit

@Database(
    entities = [TenantEntity::class, PaymentEntity::class, UnitEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun paymentDao(): PaymentDao
    abstract fun unitDao(): UnitDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val DATABASE_NAME = "rental_validator_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tenants ADD COLUMN cpf TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration 2→3:
         * 1. Creates the rental_units table.
         * 2. Inserts the public fallback unit (Geral).
         * 3. Discovers any additional unit names from existing tenants and inserts them.
         * 4. Treats blank unit as "Geral" (consolidated, no duplicates).
         * 5. Adds nullable unitId column to tenants.
         * 6. Creates index on tenants.unitId.
         * 7. Back-fills unitId by matching the unit text to rental_units.name (case-insensitive).
         *
         * No data is destroyed. No IDs are changed. The legacy `unit` text column is preserved.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = java.time.Instant.now().toString()

                // Step 1 – Create rental_units table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rental_units` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `iconKey` TEXT,
                        `photoPath` TEXT,
                        `location` TEXT NOT NULL,
                        `capacity` INTEGER NOT NULL,
                        `capacityKind` TEXT NOT NULL,
                        `operationalStatus` TEXT NOT NULL,
                        `condominiumFee` REAL,
                        `notes` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rental_units_name` ON `rental_units` (`name`)")

                // Step 2 – Insert the fallback; existing unit names are discovered below.
                val fixedUnits = RentalUnit.LEGACY_FIXED_UNITS
                fixedUnits.forEach { unitName ->
                    val id = java.util.UUID.nameUUIDFromBytes(unitName.toByteArray()).toString()
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `rental_units`
                        (`id`,`name`,`type`,`iconKey`,`photoPath`,`location`,`capacity`,`capacityKind`,`operationalStatus`,`condominiumFee`,`notes`,`createdAt`,`updatedAt`)
                        VALUES (?,?,?,NULL,NULL,'',1,'TENANTS','ACTIVE',NULL,'',?,?)
                        """.trimIndent(),
                        arrayOf(id, unitName, "OTHER", now, now)
                    )
                }

                // Step 3 – Discover extra unit names from existing tenants
                val cursor = db.query("SELECT DISTINCT `unit` FROM `tenants`")
                val discoveredNames = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(0) ?: ""
                    val normalized = raw.trim().ifBlank { RentalUnit.GERAL_NAME }
                    discoveredNames.add(normalized)
                }
                cursor.close()

                // Deduplicate against fixed list (case-insensitive), then insert extras
                val fixedLower = fixedUnits.map { it.lowercase() }.toSet()
                discoveredNames
                    .distinctBy { it.lowercase() }
                    .filter { it.lowercase() !in fixedLower }
                    .forEach { unitName ->
                        val id = java.util.UUID.nameUUIDFromBytes(unitName.toByteArray()).toString()
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `rental_units`
                            (`id`,`name`,`type`,`iconKey`,`photoPath`,`location`,`capacity`,`capacityKind`,`operationalStatus`,`condominiumFee`,`notes`,`createdAt`,`updatedAt`)
                            VALUES (?,?,?,NULL,NULL,'',1,'TENANTS','ACTIVE',NULL,'',?,?)
                            """.trimIndent(),
                            arrayOf(id, unitName, "OTHER", now, now)
                        )
                    }

                // Step 5 – Add unitId column to tenants (nullable)
                db.execSQL("ALTER TABLE `tenants` ADD COLUMN `unitId` TEXT DEFAULT NULL")

                // Step 6 – Create index on tenants.unitId
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tenants_unitId` ON `tenants` (`unitId`)")

                // Step 7 – Back-fill unitId by matching unit text to rental_units.name (case-insensitive)
                // Blank unit → Geral
                db.execSQL(
                    """
                    UPDATE `tenants`
                    SET `unitId` = (
                        SELECT `id` FROM `rental_units`
                        WHERE LOWER(`name`) = LOWER(
                            CASE WHEN TRIM(`tenants`.`unit`) = '' THEN '${RentalUnit.GERAL_NAME}'
                            ELSE TRIM(`tenants`.`unit`) END
                        )
                        LIMIT 1
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 3→4: repairs tenants created after the unit migration whose
         * legacy unit text was saved but unitId remained null.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE `tenants`
                    SET `unitId` = (
                        SELECT `id` FROM `rental_units`
                        WHERE LOWER(`name`) = LOWER(TRIM(`tenants`.`unit`))
                        LIMIT 1
                    )
                    WHERE `unitId` IS NULL
                      AND TRIM(`unit`) <> ''
                    """.trimIndent()
                )
            }
        }
    }
}
