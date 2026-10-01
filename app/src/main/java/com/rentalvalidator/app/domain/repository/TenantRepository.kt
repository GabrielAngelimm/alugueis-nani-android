package com.rentalvalidator.app.domain.repository

import com.rentalvalidator.app.domain.model.Tenant
import kotlinx.coroutines.flow.Flow

interface TenantRepository {
    fun getTenantsFlow(): Flow<List<Tenant>>
    suspend fun getAllTenants(): List<Tenant>
    suspend fun getTenantById(id: String): Tenant?
    suspend fun insertTenant(tenant: Tenant)
    suspend fun updateTenant(tenant: Tenant)
    suspend fun deleteTenant(tenant: Tenant)
    suspend fun insertTenants(tenants: List<Tenant>)
    suspend fun clearAll()
}
