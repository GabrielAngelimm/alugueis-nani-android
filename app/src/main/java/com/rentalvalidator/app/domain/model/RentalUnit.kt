package com.rentalvalidator.app.domain.model

/**
 * Domain model for a rental unit (apartment, house, etc.).
 *
 * The [occupancyStatus] is derived from [tenantCount] and [capacity]:
 * - INACTIVE / MAINTENANCE: driven by [operationalStatus], displayed with priority
 * - AVAILABLE: tenantCount == 0
 * - PARTIALLY_OCCUPIED: 0 < tenantCount < capacity
 * - OCCUPIED: tenantCount >= capacity
 */
data class RentalUnit(
    val id: String,
    val name: String,
    val type: UnitType,
    /** Key referencing a built-in icon from the icon set. Null when a photo is used instead. */
    val iconKey: String? = null,
    /** Path to a resized photo stored in the app's private storage. */
    val photoPath: String? = null,
    val location: String = "",
    val capacity: Int = 1,
    val capacityKind: CapacityKind = CapacityKind.TENANTS,
    val operationalStatus: OperationalStatus = OperationalStatus.ACTIVE,
    val condominiumFee: Double? = null,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    /** Current number of linked tenants — populated by the repository. */
    val tenantCount: Int = 0,
) {
    /** Computed occupancy status based on tenant count and capacity. */
    val occupancyStatus: OccupancyStatus
        get() = when {
            operationalStatus == OperationalStatus.INACTIVE -> OccupancyStatus.INACTIVE
            operationalStatus == OperationalStatus.MAINTENANCE -> OccupancyStatus.MAINTENANCE
            tenantCount == 0 -> OccupancyStatus.AVAILABLE
            tenantCount >= capacity -> OccupancyStatus.OCCUPIED
            else -> OccupancyStatus.PARTIALLY_OCCUPIED
        }

    /** Returns true when new tenant links should be blocked. */
    val acceptsNewTenants: Boolean
        get() = operationalStatus == OperationalStatus.ACTIVE

    companion object {
        /** The well-known legacy unit used when no unit is set on a tenant. */
        const val GERAL_NAME = "Geral"

        /** Only a generic fallback is shipped; migrations discover existing unit names from tenants. */
        val LEGACY_FIXED_UNITS = listOf(GERAL_NAME)
    }
}

enum class OccupancyStatus {
    AVAILABLE,
    PARTIALLY_OCCUPIED,
    OCCUPIED,
    INACTIVE,
    MAINTENANCE
}
