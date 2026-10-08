package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.util.CurrencyUtils

@Composable
fun PaymentBottomSheet(item: TenantPaymentItem, onDismiss: () -> Unit, periodLabel: String = "", onStatusChange: (PaymentStatus) -> Unit) {
    NaniSheet("Atualizar pagamento", onDismiss) {
        NaniSheetTenantCard(item.tenant.name, item.tenant.unit.ifBlank { "Geral" })
        LedgerSheet(contentPadding = PaddingValues(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NaniFact("Aluguel mensal", CurrencyUtils.format(item.tenant.amount), Modifier.weight(1f))
                NaniFact("Vencimento", "Dia ${item.tenant.dueDay}", Modifier.weight(1f))
            }
        }
        Text(if (periodLabel.isBlank()) "Situação do pagamento" else "Situação em $periodLabel",
            style = MaterialTheme.typography.titleMedium)
        PaymentStatusChoices(item.payment?.status ?: PaymentStatus.PENDENTE) {
            onStatusChange(it); onDismiss()
        }
    }
}

/** One selection pattern shared by manual updates and statement registration. */
@Composable
internal fun PaymentStatusChoices(selected: PaymentStatus, onSelect: (PaymentStatus) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentStatus.entries.forEach { status ->
            val tone = statusTone(status.displayKind)
            NaniChoice(status.displayLabel, status.displayDescription, selected = selected == status,
                onClick = {
                    if (status == PaymentStatus.PAGO) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(status)
                },
                leading = {
                    Box(Modifier.size(36.dp).background(tone.fill, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(status.glyph, null, Modifier.size(20.dp), tint = tone.ink)
                    }
                })
        }
    }
}
