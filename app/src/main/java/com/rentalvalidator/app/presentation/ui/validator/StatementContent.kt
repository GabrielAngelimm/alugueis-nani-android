package com.rentalvalidator.app.presentation.ui.validator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.usecase.PaymentStatus
import com.rentalvalidator.app.domain.usecase.ValidationResult
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.LocalNavigationClearance
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.ui.monthly_grid.TallyText
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniSerifText
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.presentation.viewmodel.ValidatorState
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.YearMonth

/** The three steps of checking a statement, in the order they are done. */
@Composable
internal fun ValidatorIdle(state: ValidatorState.Idle, period: YearMonth, onPeriod: () -> Unit, onFile: () -> Unit, onClear: () -> Unit, onValidate: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalNavigationClearance.current)) {
        item {
            Column(Modifier.padding(horizontal = AppSpace.page).padding(top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Compare o extrato com os aluguéis", style = MaterialTheme.typography.headlineSmall)
                Text("Escolha o mês e o arquivo do banco. Os valores encontrados aparecem para você revisar antes de registrar qualquer pagamento.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LedgerSheet(Modifier.padding(horizontal = AppSpace.page)) {
                StepRow(1, "Mês de referência", period.longLabel(), onPeriod)
                LedgerRule(Modifier.padding(start = 70.dp))
                StepRow(2, if (state.selectedFileName == null) "Selecionar extrato" else "Trocar extrato", "Arquivo PDF ou CSV", onFile)
            }
        }
        state.selectedFileName?.let { name ->
            item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f)) {
                    Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Pronto para conferir", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
                        }
                        IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, "Remover", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = AppSpace.page).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PrimaryButton("Validar pagamentos", onValidate, enabled = state.selectedFileName != null)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("O arquivo é lido somente neste aparelho. Nada é enviado para a internet.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 72.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape).clearAndSetSemantics { },
            contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.titleLarge.copy(fontFamily = NaniSerifText))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LoadingValidation() {
    Box(Modifier.fillMaxSize()) {
        AppLoadingState(title = "Analisando o extrato", message = "Conferindo transações e vínculos de pagamento.")
    }
}

@Composable
internal fun ErrorValidation(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AppErrorState(
            title = "Não foi possível validar",
            message = if (message.contains("ENOENT") || message.contains("FileNotFoundException") ||
                message.contains("Permission denied", ignoreCase = true)) {
                "Não foi possível abrir o arquivo. Selecione o extrato novamente."
            } else message,
            actionLabel = "Selecionar outro extrato",
            onAction = onRetry
        )
    }
}

/** Presentation of a statement result: the label carries the nuance, the mark carries the state. */
internal fun ValidationResult.markLabel(): Pair<String, StatusKind> = when (status) {
    PaymentStatus.PAGO_NO_PRAZO -> "Pago" to StatusKind.SUCCESS
    PaymentStatus.PAGO_COM_ATRASO -> "Pago com atraso" to StatusKind.SUCCESS
    PaymentStatus.PAGO_A_MAIOR -> (if (daysLate > 0) "Pago a maior com atraso" else "Pago a maior") to StatusKind.SUCCESS
    PaymentStatus.PARCIAL -> (if (daysLate > 0) "Parcial em atraso" else "Parcial") to
        (if (daysLate > 0) StatusKind.ERROR else StatusKind.WARNING)
    PaymentStatus.PENDENTE_NO_PRAZO -> "Pendente" to StatusKind.WARNING
    PaymentStatus.PENDENTE_ATRASADO -> "Em atraso" to StatusKind.ERROR
}

/** Results grouped by the mark their cards show: paid in any form, still open, or late. */
@Composable
internal fun statementStats(results: List<ValidationResult>): List<StatCount> {
    val byKind = results.groupingBy { it.markLabel().second }.eachCount()
    val paid = byKind[StatusKind.SUCCESS] ?: 0
    val open = byKind[StatusKind.WARNING] ?: 0
    return listOf(
        StatCount(paid, if (paid == 1) "pago" else "pagos", ringColor(StatusKind.SUCCESS)),
        StatCount(open, if (open == 1) "pendente" else "pendentes", ringColor(StatusKind.WARNING)),
        StatCount(byKind[StatusKind.ERROR] ?: 0, "em atraso", ringColor(StatusKind.ERROR), emphasize = true)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ValidationResults(results: List<ValidationResult>, period: YearMonth, onReset: () -> Unit,
    onRegister: (Tenant, Boolean) -> Unit, onCharge: (ValidationResult, Boolean) -> Unit) {
    val paid = results.count { it.status.isPaid }
    val identified = results.sumOf { it.amountPaid }
    val due = results.sumOf { it.amountDue }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalNavigationClearance.current)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AppSpace.page).padding(top = 10.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Resultado da conferência", style = MaterialTheme.typography.titleMedium)
                        Text(period.longLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onReset) { Text("Novo", style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.height(10.dp))
                // Counted per tenant from the same mark each result card shows, so ring, legend and cards agree.
                val stats = statementStats(results)
                val percent = percentOf(paid, results.size)
                LedgerSheet {
                    Row(Modifier.padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusRing(ringOf(stats), "$paid de ${results.size} pagamentos identificados no extrato",
                            size = 64.dp, stroke = 7.dp) { RingLabel(if (results.isNotEmpty()) "$percent%" else "—", 64.dp) }
                        Spacer(Modifier.width(14.dp))
                        TallyText("Identificado no extrato", identified, due, stats, Modifier.weight(1f))
                    }
                }
            }
        }
        val grouped = results.groupBy { it.tenant.unit.ifBlank { "Geral" } }
        if (grouped.isEmpty()) {
            item { AppEmptyState(Icons.Rounded.SearchOff, "Nenhum resultado encontrado", "Ajuste a busca ou os filtros para ver outros inquilinos.") }
        }
        grouped.forEach { (unit, unitResults) ->
            item(key = "unit-$unit") {
                SectionTitle(unit, Modifier.padding(top = 12.dp, bottom = 8.dp),
                    detail = "${unitResults.size} ${if (unitResults.size == 1) "inquilino" else "inquilinos"}")
            }
            itemsIndexed(unitResults, key = { _, result -> result.tenant.id }) { _, result ->
                ValidationResultCard(result, { onRegister(result.tenant, result.status.isPaid) }, { includePenalty -> onCharge(result, includePenalty) })
            }
        }
    }
}
