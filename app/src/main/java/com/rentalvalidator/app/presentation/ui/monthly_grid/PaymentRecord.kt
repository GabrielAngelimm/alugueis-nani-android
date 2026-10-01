package com.rentalvalidator.app.presentation.ui.monthly_grid

import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.rentalvalidator.app.presentation.components.premiumShadow
import com.rentalvalidator.app.presentation.components.surfaceDepthBorder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.presentation.components.StatusBadge
import com.rentalvalidator.app.presentation.design.NaniTenantIdentity
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.util.CurrencyUtils

@Composable
internal fun PaymentRecord(item:TenantPaymentItem,onClick:()->Unit) {
    val status=item.payment?.status?:PaymentStatus.PENDENTE
    val shape = RoundedCornerShape(20.dp)
    Surface(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page, vertical=6.dp).premiumShadow(shape),
        shape=shape, color=MaterialTheme.colorScheme.surface,
        border=surfaceDepthBorder()) {
    Column(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick).padding(horizontal=16.dp,vertical=14.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val dueOnOwnLine = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.25f
            val dueTrailing: @Composable RowScope.() -> Unit = {
                Spacer(Modifier.width(8.dp))
                DueText(item.tenant.dueDay)
            }
            Column {
                NaniTenantIdentity(item.tenant.name, item.tenant.unit,
                    secondaryTrailing = if (dueOnOwnLine) null else dueTrailing) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (dueOnOwnLine) {
                    Spacer(Modifier.height(8.dp))
                    DueText(item.tenant.dueDay)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
        Spacer(Modifier.height(9.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.25f) {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    PaymentAmount(item)
                    StatusBadge(status.displayLabel,status.displayKind)
                }
            } else {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    PaymentAmount(item,Modifier.weight(1f))
                    StatusBadge(status.displayLabel,status.displayKind)
                }
            }
        }
    }
    }
}

@Composable
private fun DueText(day: Int) {
    Text("Vence dia $day",
        style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),
        color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaymentAmount(item: TenantPaymentItem, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            stringResource(R.string.monthly_rent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            CurrencyUtils.format(item.tenant.amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}


