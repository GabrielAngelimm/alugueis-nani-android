package com.rentalvalidator.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rentalvalidator.app.domain.model.Payment
import com.rentalvalidator.app.domain.model.PaymentStatus

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tenantId"]), Index(value = ["tenantId", "year", "month"], unique = true)]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val year: Int,
    val month: Int,
    val status: String
) {
    fun toDomain(): Payment {
        return Payment(
            id = id,
            tenantId = tenantId,
            year = year,
            month = month,
            status = PaymentStatus.valueOf(status)
        )
    }

    companion object {
        fun fromDomain(payment: Payment): PaymentEntity {
            return PaymentEntity(
                id = payment.id,
                tenantId = payment.tenantId,
                year = payment.year,
                month = payment.month,
                status = payment.status.name
            )
        }
    }
}
