package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.util.CurrencyUtils

@Composable
fun PaymentBottomSheet(item:TenantPaymentItem,onDismiss:()->Unit,onStatusChange:(PaymentStatus)->Unit) {
    NaniSheet("Atualizar pagamento",onDismiss) {
        NaniSheetTenantCard(item.tenant.name, item.tenant.unit.ifBlank { "Geral" })
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            NaniFact("Aluguel mensal",CurrencyUtils.format(item.tenant.amount),Modifier.weight(1f))
            NaniFact("Vencimento","Dia ${item.tenant.dueDay}",Modifier.weight(1f))
        }
        PaymentStatusChoices(item.payment?.status ?: PaymentStatus.PENDENTE) {
            onStatusChange(it);onDismiss()
        }
    }
}

/** One selection pattern shared by manual updates and statement registration. */
@Composable
internal fun PaymentStatusChoices(selected: PaymentStatus, onSelect: (PaymentStatus) -> Unit) {
    Column(Modifier.selectableGroup(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        PaymentStatus.entries.forEach { status ->
            val title = status.displayLabel
            val description = when(status) {
                PaymentStatus.PAGO -> "Pagamento confirmado"
                PaymentStatus.PENDENTE -> "Pagamento ainda não identificado"
                PaymentStatus.EM_ANALISE -> "Aguardando conferência"
            }
            val icon = when(status) {
                PaymentStatus.PAGO -> Icons.Outlined.CheckCircle
                PaymentStatus.PENDENTE -> Icons.Outlined.Schedule
                PaymentStatus.EM_ANALISE -> Icons.Outlined.Search
            }
            val palette = semanticPalette(status.displayKind)
            val active = selected == status
            Surface(shape=RoundedCornerShape(16.dp),
                color=if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border=androidx.compose.foundation.BorderStroke(1.dp,if(active) MaterialTheme.colorScheme.primary.copy(alpha=.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.3f))) {
                Row(Modifier.fillMaxWidth().selectable(active,role=Role.RadioButton,onClick={onSelect(status)})
                    .heightIn(min=76.dp).padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Icon(icon,null,Modifier.size(24.dp),tint=palette.foreground)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(title,style=MaterialTheme.typography.titleSmall)
                        Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(active,onClick=null)
                }
            }
        }
    }
}
