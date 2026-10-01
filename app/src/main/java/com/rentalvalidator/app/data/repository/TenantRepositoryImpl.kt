package com.rentalvalidator.app.data.repository

import com.rentalvalidator.app.data.local.dao.TenantDao
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepositoryImpl @Inject constructor(
    private val tenantDao: TenantDao
) : TenantRepository {

    override fun getTenantsFlow(): Flow<List<Tenant>> {
        return tenantDao.getTenantsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllTenants(): List<Tenant> {
        return tenantDao.getAllTenants().map { it.toDomain() }
    }

    override suspend fun getTenantById(id: String): Tenant? {
        return tenantDao.getTenantById(id)?.toDomain()
    }

    override suspend fun insertTenant(tenant: Tenant) {
        tenantDao.insertTenant(TenantEntity.fromDomain(tenant))
    }

    override suspend fun updateTenant(tenant: Tenant) {
        tenantDao.updateTenant(TenantEntity.fromDomain(tenant))
    }

    override suspend fun deleteTenant(tenant: Tenant) {
        tenantDao.deleteTenant(TenantEntity.fromDomain(tenant))
    }

    override suspend fun insertTenants(tenants: List<Tenant>) {
        tenantDao.insertTenants(tenants.map { TenantEntity.fromDomain(it) })
    }

    override suspend fun clearAll() {
        tenantDao.clearAll()
    }
}
