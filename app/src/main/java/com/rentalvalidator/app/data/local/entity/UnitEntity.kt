package com.rentalvalidator.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.UnitType

@Entity(
    tableName = "rental_units",
    indices = [Index(value = ["name"], unique = true)]
)
data class UnitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,             // UnitType.stableCode
    val iconKey: String?,
    val photoPath: String?,
    val location: String,
    val capacity: Int,
    val capacityKind: String,     // CapacityKind.stableCode
    val operationalStatus: String, // OperationalStatus.stableCode
    val condominiumFee: Double?,
    val notes: String,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(tenantCount: Int = 0): RentalUnit = RentalUnit(
        id = id,
        name = name,
        type = UnitType.fromCode(type),
        iconKey = iconKey,
        photoPath = photoPath,
        location = location,
        capacity = capacity,
        capacityKind = CapacityKind.fromCode(capacityKind),
        operationalStatus = OperationalStatus.fromCode(operationalStatus),
        condominiumFee = condominiumFee,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tenantCount = tenantCount
    )

    companion object {
        fun fromDomain(unit: RentalUnit): UnitEntity = UnitEntity(
            id = unit.id,
            name = unit.name,
            type = unit.type.stableCode,
            iconKey = unit.iconKey,
            photoPath = unit.photoPath,
            location = unit.location,
            capacity = unit.capacity,
            capacityKind = unit.capacityKind.stableCode,
            operationalStatus = unit.operationalStatus.stableCode,
            condominiumFee = unit.condominiumFee,
            notes = unit.notes,
            createdAt = unit.createdAt,
            updatedAt = unit.updatedAt
        )
    }
}
