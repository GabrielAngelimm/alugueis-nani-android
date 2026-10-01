package com.rentalvalidator.app.data.repository

import com.rentalvalidator.app.data.local.dao.UnitDao
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.repository.UnitRepository
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitRepositoryImpl @Inject constructor(
    private val unitDao: UnitDao
) : UnitRepository {

    override fun getUnitsFlow(): Flow<List<RentalUnit>> {
        return unitDao.getUnitsWithTenantCountFlow().map { rows ->
            rows.map { it.unit.toDomain(tenantCount = it.tenantCount) }
        }
    }

    override suspend fun getAllUnits(): List<RentalUnit> {
        return unitDao.getUnitsWithTenantCounts().map { it.unit.toDomain(tenantCount = it.tenantCount) }
    }

    override suspend fun getUnitById(id: String): RentalUnit? {
        val entity = unitDao.getUnitById(id) ?: return null
        val count = unitDao.getTenantCount(id)
        return entity.toDomain(tenantCount = count)
    }

    override suspend fun getUnitByName(name: String): RentalUnit? {
        val entity = unitDao.getUnitByName(name) ?: return null
        val count = unitDao.getTenantCount(entity.id)
        return entity.toDomain(tenantCount = count)
    }

    override suspend fun nameExists(name: String, excludeId: String?): Boolean {
        val existing = unitDao.getUnitByName(name) ?: return false
        return if (excludeId != null) existing.id != excludeId else true
    }

    override suspend fun getTenantCount(unitId: String): Int {
        return unitDao.getTenantCount(unitId)
    }

    override suspend fun insertUnit(unit: RentalUnit): Boolean {
        val entity = UnitEntity.fromDomain(unit)
        val rowId = unitDao.insertUnit(entity)
        return rowId != -1L
    }

    override suspend fun updateUnit(unit: RentalUnit) {
        unitDao.getUnitById(unit.id) ?: return
        val updatedAt = Instant.now().toString()
        val updatedEntity = UnitEntity.fromDomain(unit.copy(updatedAt = updatedAt))
        unitDao.updateUnitAndTenantNames(updatedEntity)
    }

    override suspend fun updateOperationalStatus(unitId: String, status: OperationalStatus) {
        unitDao.updateOperationalStatus(unitId, status.stableCode, Instant.now().toString())
    }

    override suspend fun deleteUnit(unitId: String) {
        unitDao.deleteUnoccupiedUnit(unitId)
    }

    override suspend fun insertUnits(units: List<RentalUnit>) {
        unitDao.insertUnits(units.map { UnitEntity.fromDomain(it) })
    }

    override suspend fun clearAll() {
        unitDao.clearAll()
    }
}
