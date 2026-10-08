package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.design.StatusKind

/** Presentation only; the persisted status and financial predicates are unchanged. */
internal val PaymentStatus.displayLabel: String
    get() = when (this) {
        PaymentStatus.PAGO -> "Pago"
        PaymentStatus.PENDENTE -> "Pendente"
        PaymentStatus.EM_ANALISE -> "Em análise"
    }

internal val PaymentStatus.displayKind: StatusKind
    get() = when (this) {
        PaymentStatus.PAGO -> StatusKind.SUCCESS
        PaymentStatus.PENDENTE -> StatusKind.WARNING
        PaymentStatus.EM_ANALISE -> StatusKind.INFO
    }

internal val PaymentStatus.displayDescription: String
    get() = when (this) {
        PaymentStatus.PAGO -> "O aluguel do mês foi recebido"
        PaymentStatus.PENDENTE -> "O pagamento ainda não foi identificado"
        PaymentStatus.EM_ANALISE -> "Há um pagamento aguardando conferência"
    }

internal val PaymentStatus.glyph: ImageVector
    get() = when (this) {
        PaymentStatus.PAGO -> Icons.Rounded.Check
        PaymentStatus.PENDENTE -> Icons.Rounded.Schedule
        PaymentStatus.EM_ANALISE -> Icons.Rounded.HourglassTop
    }
