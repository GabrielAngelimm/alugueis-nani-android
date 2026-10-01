package com.rentalvalidator.app.data.local.dao

import androidx.room.*
import com.rentalvalidator.app.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants ORDER BY name ASC")
    fun getTenantsFlow(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants ORDER BY name ASC")
    suspend fun getAllTenants(): List<TenantEntity>

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getTenantById(id: String): TenantEntity?

    @Upsert
    suspend fun insertTenant(tenant: TenantEntity)

    @Upsert
    suspend fun insertTenants(tenants: List<TenantEntity>)

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Delete
    suspend fun deleteTenant(tenant: TenantEntity)

    @Query("DELETE FROM tenants")
    suspend fun clearAll()
}
