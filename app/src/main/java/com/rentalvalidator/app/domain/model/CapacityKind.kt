package com.rentalvalidator.app.domain.model

enum class CapacityKind(val stableCode: String) {
    TENANTS("TENANTS"),
    ROOMS("ROOMS"),
    SPACES("SPACES");

    companion object {
        fun fromCode(code: String): CapacityKind =
            entries.firstOrNull { it.stableCode == code } ?: TENANTS
    }
}
