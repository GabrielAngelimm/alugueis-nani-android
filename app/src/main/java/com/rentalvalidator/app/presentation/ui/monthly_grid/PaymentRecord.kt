package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

/**
 * One line of the month's ledger: who, where and when it is due on the left; the amount
 * and its mark on the right. Tapping the line updates the mark.
 */
@Composable
internal fun PaymentRecord(item: TenantPaymentItem, period: YearMonth, position: LedgerPosition, onClick: () -> Unit) {
    val status = item.payment?.status ?: PaymentStatus.PENDENTE
    val state = dueState(item.payment?.status, period, item.tenant.dueDay, LocalDate.now())
    val stacked = LocalDensity.current.fontScale > 1.3f
    LedgerSlice(position, ruleIndent = 70.dp) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 76.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Monogram(item.tenant.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.tenant.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.tenant.unit.ifBlank { "Geral" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state != DueState.PAID) Text(dueSentence(state, item.tenant.dueDay, period), style = MaterialTheme.typography.bodySmall,
                    color = if (state == DueState.OVERDUE) NaniTheme.colors.overdue.ink else MaterialTheme.colorScheme.onSurfaceVariant)
                if (stacked) {
                    Spacer(Modifier.height(6.dp))
                    Text(CurrencyUtils.format(item.tenant.amount), style = NaniType.moneyRow)
                    Spacer(Modifier.height(4.dp))
                    StatusMark(status.displayLabel, status.displayKind)
                }
            }
            if (!stacked) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(CurrencyUtils.format(item.tenant.amount), style = NaniType.moneyRow, maxLines = 1, softWrap = false)
                StatusMark(status.displayLabel, status.displayKind)
            }
        }
    }
}
