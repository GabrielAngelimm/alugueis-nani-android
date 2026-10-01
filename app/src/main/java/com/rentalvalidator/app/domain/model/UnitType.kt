package com.rentalvalidator.app.domain.model

enum class UnitType(val stableCode: String) {
    APARTMENT("APARTMENT"),
    HOUSE("HOUSE"),
    COMMERCIAL_ROOM("COMMERCIAL_ROOM"),
    BUILDING("BUILDING"),
    KITNET("KITNET"),
    OTHER("OTHER");

    companion object {
        fun fromCode(code: String): UnitType =
            entries.firstOrNull { it.stableCode == code } ?: OTHER
    }
}
