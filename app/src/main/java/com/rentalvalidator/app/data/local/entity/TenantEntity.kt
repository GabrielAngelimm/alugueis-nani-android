package com.rentalvalidator.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rentalvalidator.app.domain.model.Tenant

// Note: @ForeignKey is intentionally omitted for unitId.
// Room validates FK constraints against sqlite_master's CREATE TABLE DDL.
// Because unitId was added via ALTER TABLE ADD COLUMN (MIGRATION_2_3), the DDL
// stored in sqlite_master does NOT contain the FK clause, which would cause a
// schema validation mismatch at runtime. Referential integrity is enforced at
// the repository layer: UnitRepositoryImpl.deleteUnit() throws when tenant count > 0.
@Entity(
    tableName = "tenants",
    indices = [Index(value = ["unitId"])]
)
data class TenantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val bank: String,
    val phone: String,
    @ColumnInfo(defaultValue = "''") val cpf: String,
    val whatsappName: String,
    // Legacy text field — kept for backward compatibility during transition phase
    val unit: String,
    val aliases: List<String>,
    val contractPath: String,
    val contractExpirationDate: String,
    val inspectionPath: String,
    val dateCreated: String,
    // New: stable FK reference to rental_units.id (nullable during transition)
    @ColumnInfo(defaultValue = "NULL") val unitId: String? = null,
) {
    fun toDomain(): Tenant {
        return Tenant(
            id = id,
            name = name,
            amount = amount,
            dueDay = dueDay,
            bank = bank,
            phone = phone,
            cpf = cpf,
            whatsappName = whatsappName,
            unit = unit,
            aliases = aliases,
            contractPath = contractPath,
            contractExpirationDate = contractExpirationDate,
            inspectionPath = inspectionPath,
            dateCreated = dateCreated,
            unitId = unitId
        )
    }

    companion object {
        fun fromDomain(tenant: Tenant): TenantEntity {
            return TenantEntity(
                id = tenant.id,
                name = tenant.name,
                amount = tenant.amount,
                dueDay = tenant.dueDay,
                bank = tenant.bank,
                phone = tenant.phone,
                cpf = tenant.cpf,
                whatsappName = tenant.whatsappName,
                unit = tenant.unit,
                aliases = tenant.aliases,
                contractPath = tenant.contractPath,
                contractExpirationDate = tenant.contractExpirationDate,
                inspectionPath = tenant.inspectionPath,
                dateCreated = tenant.dateCreated,
                unitId = tenant.unitId
            )
        }
    }
}

