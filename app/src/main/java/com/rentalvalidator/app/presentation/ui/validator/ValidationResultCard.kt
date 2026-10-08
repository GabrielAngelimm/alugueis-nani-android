package com.rentalvalidator.app.presentation.ui.validator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.usecase.ValidationResult
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.TonalButton
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.format.DateTimeFormatter

private val TransactionDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * One tenant's result. Collapsed it answers "was it paid and how much"; expanded it shows
 * the evidence (matched transactions) and the two follow-ups: charge or register.
 */
@Composable
fun ValidationResultCard(result: ValidationResult, onRegistrar: () -> Unit, onCobrar: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var includePenalty by remember(result.tenant.id, result.amountDue, result.penaltyApplied) { mutableStateOf(false) }
    val isPaid = result.status.isPaid
    val (label, kind) = result.markLabel()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, AppMotion.StateFloat, label = "expand")
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page, vertical = 5.dp),
        shape = RoundedCornerShape(AppSize.sheetRadius), color = colors.surface) {
        Column {
            Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Monogram(result.tenant.name)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(result.tenant.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Vence dia ${result.tenant.dueDay}", style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.ExpandMore, if (expanded) "Recolher ${result.tenant.name}" else "Expandir ${result.tenant.name}",
                        Modifier.rotate(rotation), tint = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                StatusMark(label, kind)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NaniFact("Devido", CurrencyUtils.format(result.amountDue), Modifier.weight(1f))
                    VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 12.dp), color = colors.outlineVariant)
                    NaniFact("Identificado", CurrencyUtils.format(result.amountPaid), Modifier.weight(1f))
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(AppMotion.ExpandVertically, expandFrom = Alignment.Top) + fadeIn(AppMotion.EnterFade),
                exit = shrinkVertically(AppMotion.CollapseVertically, shrinkTowards = Alignment.Top) + fadeOut(AppMotion.ExitFade)
            ) {
                Column(Modifier.padding(bottom = 16.dp)) {
                    LedgerRule()
                    if (result.penaltyApplied > 0.0) {
                        Row(Modifier.fillMaxWidth()
                            .toggleable(includePenalty, role = Role.Switch, onValueChange = { includePenalty = it })
                            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Incluir multa na cobrança", style = MaterialTheme.typography.titleSmall)
                                Text("Opcional, ${CurrencyUtils.format(result.penaltyApplied)}", style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant)
                            }
                            Switch(checked = includePenalty, onCheckedChange = null)
                        }
                        LedgerRule(Modifier.padding(horizontal = 16.dp))
                    }
                    Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                        if (result.matchedTransactions.isEmpty()) {
                            Text("Nenhuma transação do extrato corresponde a este inquilino.", style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant)
                        } else {
                            Text("Transações encontradas", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerLow) {
                                Column(Modifier.padding(horizontal = 14.dp)) {
                                    result.matchedTransactions.forEachIndexed { i, transaction ->
                                        if (i > 0) LedgerRule()
                                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text(transaction.description, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis)
                                                Text(transaction.date.format(TransactionDate), style = MaterialTheme.typography.labelSmall,
                                                    color = colors.onSurfaceVariant)
                                            }
                                            Text(CurrencyUtils.format(transaction.amount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (isPaid) {
                            PrimaryButton("Registrar pagamento", onRegistrar)
                        } else {
                            val registerLabel = if (result.amountPaid > 0) "Salvar parcial" else "Registrar"
                            BoxWithConstraints {
                                val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.25f
                                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TonalButton("Cobrar", { onCobrar(includePenalty) }, icon = Icons.AutoMirrored.Rounded.Chat)
                                    PrimaryButton(registerLabel, onRegistrar)
                                } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TonalButton("Cobrar", { onCobrar(includePenalty) }, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.Chat)
                                    PrimaryButton(registerLabel, onRegistrar, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
