package com.rentalvalidator.app.data.local.dao

import androidx.room.*
import com.rentalvalidator.app.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

data class UnitWithTenantCount(
    @Embedded val unit: UnitEntity,
    val tenantCount: Int
)

@Dao
interface UnitDao {

    @Query("SELECT * FROM rental_units ORDER BY name ASC")
    fun getUnitsFlow(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM rental_units ORDER BY name ASC")
    suspend fun getAllUnits(): List<UnitEntity>

    @Query("SELECT rental_units.*, (SELECT COUNT(*) FROM tenants WHERE unitId = rental_units.id) AS tenantCount FROM rental_units ORDER BY name ASC")
    fun getUnitsWithTenantCountFlow(): Flow<List<UnitWithTenantCount>>

    @Query("SELECT rental_units.*, (SELECT COUNT(*) FROM tenants WHERE unitId = rental_units.id) AS tenantCount FROM rental_units ORDER BY name ASC")
    suspend fun getUnitsWithTenantCounts(): List<UnitWithTenantCount>

    @Query("SELECT * FROM rental_units WHERE id = :id")
    suspend fun getUnitById(id: String): UnitEntity?

    @Query("SELECT * FROM rental_units WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getUnitByName(name: String): UnitEntity?

    @Query("SELECT COUNT(*) FROM tenants WHERE unitId = :unitId")
    suspend fun getTenantCount(unitId: String): Int

    @Query("SELECT COUNT(*) FROM tenants WHERE unitId = :unitId")
    fun getTenantCountFlow(unitId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Update
    suspend fun updateUnit(unit: UnitEntity)

    /** The unit fields and the tenants' legacy display text must change together. */
    @Transaction
    suspend fun updateUnitAndTenantNames(unit: UnitEntity) {
        updateUnit(unit)
        backFillTenantUnitText(unit.id, unit.name)
    }

    @Query("UPDATE rental_units SET operationalStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOperationalStatus(id: String, status: String, updatedAt: String)

    @Query("UPDATE tenants SET unit = :newName WHERE unitId = :unitId")
    suspend fun backFillTenantUnitText(unitId: String, newName: String)

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)

    @Query("DELETE FROM rental_units WHERE id = :id")
    suspend fun deleteUnitById(id: String)

    @Transaction
    suspend fun deleteUnoccupiedUnit(id: String) {
        check(getTenantCount(id) == 0) {
            "Cannot delete unit with linked tenants. Transfer or remove tenants first."
        }
        deleteUnitById(id)
    }

    @Query("DELETE FROM rental_units")
    suspend fun clearAll()
}
