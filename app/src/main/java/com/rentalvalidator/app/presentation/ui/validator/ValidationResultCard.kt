package com.rentalvalidator.app.presentation.ui.validator
import com.rentalvalidator.app.presentation.theme.AppSpace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.usecase.PaymentStatus
import com.rentalvalidator.app.domain.usecase.ValidationResult
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rentalvalidator.app.presentation.design.NaniFact
import com.rentalvalidator.app.presentation.design.NaniTenantIdentity
@Composable
fun ValidationResultCard(result:ValidationResult,onRegistrar:()->Unit,onCobrar:(Boolean)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    var includePenalty by remember(result.tenant.id, result.amountDue, result.penaltyApplied) { mutableStateOf(false) }
    val isPaid = result.status.isPaid
    val (label, kind) = when (result.status) {
        PaymentStatus.PAGO_NO_PRAZO -> "Pago" to StatusKind.SUCCESS
        PaymentStatus.PAGO_COM_ATRASO -> "Pago com atraso" to StatusKind.INFO
        PaymentStatus.PAGO_A_MAIOR -> {
            if (result.daysLate > 0) "Pago a maior com atraso" to StatusKind.INFO
            else "Pago a maior" to StatusKind.SUCCESS
        }
        PaymentStatus.PARCIAL -> {
            if (result.daysLate > 0) "Parcial em atraso" to StatusKind.WARNING
            else "Parcial" to StatusKind.WARNING
        }
        PaymentStatus.PENDENTE_NO_PRAZO -> "Pendente" to StatusKind.WARNING
        PaymentStatus.PENDENTE_ATRASADO -> "Em atraso" to StatusKind.ERROR
    }


    val shape = RoundedCornerShape(20.dp)
    Surface(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page,vertical=6.dp).premiumShadow(shape),
        shape=shape,color=MaterialTheme.colorScheme.surface,border=surfaceDepthBorder()) {
      Column(Modifier.padding(16.dp)) {
        Column(Modifier.fillMaxWidth().clickable{expanded=!expanded}) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                NaniTenantIdentity(result.tenant.name, result.tenant.unit, Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore,if(expanded)"Recolher ${result.tenant.name}" else "Expandir ${result.tenant.name}",Modifier.rotate(if(expanded)180f else 0f))
            }
            Spacer(Modifier.height(8.dp));StatusBadge(label,kind)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                NaniFact("Devido",CurrencyUtils.format(result.amountDue),Modifier.weight(1f))
                VerticalDivider(Modifier.fillMaxHeight().padding(vertical=10.dp),
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
                NaniFact("Identificado",CurrencyUtils.format(result.amountPaid),Modifier.weight(1f))
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = AppMotion.ExpandVertically,
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = AppMotion.EnterFade),
            exit = shrinkVertically(
                animationSpec = AppMotion.CollapseVertically,
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = AppMotion.ExitFade)
        ) {
            Column(Modifier.padding(top=12.dp)) {
                if (result.penaltyApplied > 0.0) {
                    Row(Modifier.fillMaxWidth().padding(bottom=12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Incluir multa na cobrança",style=MaterialTheme.typography.titleSmall)
                            Text("Opcional · ${CurrencyUtils.format(result.penaltyApplied)}",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked=includePenalty,onCheckedChange={includePenalty=it})
                    }
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))
                Spacer(Modifier.height(12.dp))
                if (result.matchedTransactions.isEmpty()) {
                    Text(
                        "Nenhuma transação associada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Transações encontradas", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    result.matchedTransactions.forEach { transaction ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    transaction.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    transaction.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                CurrencyUtils.format(transaction.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isPaid) {
                    Button(onClick=onRegistrar,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)) {
                        Text("Registrar pagamento")
                    }
                } else {
                    BoxWithConstraints {
                        val stacked = maxWidth < 280.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.25f
                        val registerLabel = if (result.amountPaid > 0) "Salvar parcial" else "Registrar"
                        if(stacked) {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                ResultAction("Cobrar",{onCobrar(includePenalty)},Modifier.fillMaxWidth(),primary=false)
                                ResultAction(registerLabel,onRegistrar,Modifier.fillMaxWidth(),primary=true)
                            }
                        } else {
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                ResultAction("Cobrar",{onCobrar(includePenalty)},Modifier.weight(1f),primary=false)
                                ResultAction(registerLabel,onRegistrar,Modifier.weight(1f),primary=true)
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
private fun ResultAction(label:String,onClick:()->Unit,modifier:Modifier,primary:Boolean) {
    Button(onClick=onClick,modifier=modifier.heightIn(min=48.dp),shape=RoundedCornerShape(12.dp),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=12.dp),
        colors=if(primary) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
            containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)) {
        Text(label,style=MaterialTheme.typography.labelLarge)
    }
}
