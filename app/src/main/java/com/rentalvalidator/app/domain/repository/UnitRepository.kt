package com.rentalvalidator.app.domain.repository

import com.rentalvalidator.app.domain.model.RentalUnit
import kotlinx.coroutines.flow.Flow

interface UnitRepository {
    /** Observe all units, each with its current tenant count. */
    fun getUnitsFlow(): Flow<List<RentalUnit>>

    /** One-shot query of all units with tenant counts. */
    suspend fun getAllUnits(): List<RentalUnit>

    /** Find unit by its stable ID. Returns null if not found. */
    suspend fun getUnitById(id: String): RentalUnit?

    /** Find unit by name (case-insensitive). Returns null if not found. */
    suspend fun getUnitByName(name: String): RentalUnit?

    /** Returns true when a unit with the given name already exists (excluding [excludeId]). */
    suspend fun nameExists(name: String, excludeId: String? = null): Boolean

    /** Get the number of tenants linked to a unit. */
    suspend fun getTenantCount(unitId: String): Int

    /** Insert a new unit. Returns true on success. */
    suspend fun insertUnit(unit: RentalUnit): Boolean

    /** Update unit data (name, type, etc.). If the name changed, the tenant legacy text is back-filled. */
    suspend fun updateUnit(unit: RentalUnit)

    /** Change the operational status (ACTIVE / INACTIVE / MAINTENANCE). */
    suspend fun updateOperationalStatus(unitId: String, status: com.rentalvalidator.app.domain.model.OperationalStatus)

    /**
     * Permanently deletes a unit.
     * @throws IllegalStateException if the unit still has linked tenants.
     */
    suspend fun deleteUnit(unitId: String)

    /** Insert or ignore a list of units (used during backup restore). */
    suspend fun insertUnits(units: List<RentalUnit>)

    /** Clear all units (used only during full backup restore, called before insertUnits). */
    suspend fun clearAll()
}
