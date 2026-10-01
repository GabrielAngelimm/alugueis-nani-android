package com.rentalvalidator.app.presentation.ui.monthly_grid

import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.StatusKind

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
