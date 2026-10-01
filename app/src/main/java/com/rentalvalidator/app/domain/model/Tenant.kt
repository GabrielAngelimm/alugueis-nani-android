package com.rentalvalidator.app.domain.model

data class Tenant(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val bank: String = "",
    val phone: String = "",
    val cpf: String = "",
    val whatsappName: String = "",
    // Legacy text field — kept for backward compatibility during transition phase
    val unit: String = "Geral",
    val aliases: List<String> = emptyList(),
    val contractPath: String = "",
    val contractExpirationDate: String = "",
    val inspectionPath: String = "",
    val dateCreated: String = "",
    // New: stable FK reference to RentalUnit.id (nullable during transition)
    val unitId: String? = null,
)

