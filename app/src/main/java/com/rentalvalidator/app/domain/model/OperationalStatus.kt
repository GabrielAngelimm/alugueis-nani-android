package com.rentalvalidator.app.domain.model

enum class OperationalStatus(val stableCode: String) {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    MAINTENANCE("MAINTENANCE");

    companion object {
        fun fromCode(code: String): OperationalStatus =
            entries.firstOrNull { it.stableCode == code } ?: ACTIVE
    }
}
