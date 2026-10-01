package com.rentalvalidator.app.presentation.ui.dashboard

import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OverviewSnapshot(
    val expected: Double, val received: Double, val pending: Double, val progress: Float,
    val tenantCount: Int, val unitCount: Int, val contractAlerts: Int,
    val months: List<Int>, val values: List<Float>
)

/** Presentation only: all financial totals and navigation come from the existing route. */
@Composable
fun OverviewContent(
    data: OverviewSnapshot,
    onSettings: () -> Unit,
    onPayments: () -> Unit,
    onStatement: () -> Unit,
    onTenants: () -> Unit,
    onContracts: () -> Unit
) {
    val currentHour = rememberDashboardHour()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 32.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(dashboardGreetingResource(currentHour)),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Configurações") }
            }
        }
        item {
            NaniSection(stringResource(R.string.month_overview), topPadding = 16.dp) {
                MonthlyOverview(data, onPayments)
            }
        }
        item {
            NaniSection("Histórico de recebimentos", topPadding = 20.dp) {
                ReceiptHistory(data.months, data.values)
            }
        }
        item {
            NaniSection("Seu próximo passo") {
                NaniActionRow("Conferir um extrato", "Identifique os pagamentos em PDF ou CSV", Icons.AutoMirrored.Outlined.FactCheck, onStatement)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NaniActionRow("Gerenciar locações",
                    "${data.unitCount} ${if (data.unitCount == 1) "unidade" else "unidades"} · ${data.tenantCount} ${if (data.tenantCount == 1) "inquilino" else "inquilinos"}",
                    Icons.Outlined.Apartment, onTenants)
                if (data.contractAlerts > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NaniActionRow("Revisar contratos", "${data.contractAlerts} com vencimento próximo ou vencidos", Icons.Outlined.EventBusy, onContracts)
                }
            }
        }
    }
}

@Composable
private fun MonthlyOverview(data: OverviewSnapshot, onPayments: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp), color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .55f))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().background(colors.primaryContainer)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(18.dp), tint = colors.onPrimaryContainer)
                    Text("Recebido no mês", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale.forLanguageTag("pt-BR"))),
                    style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                }
                Text(CurrencyUtils.format(data.received), Modifier.padding(top = 6.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                LinearProgressIndicator(
                    progress = { data.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = colors.primary, trackColor = colors.onPrimaryContainer.copy(alpha = .1f),
                    gapSize = 0.dp, drawStopIndicator = {}
                )
                Text("${(data.progress * 100).toInt()}% do previsto recebido", Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
            }
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NaniFact("A receber", CurrencyUtils.format(data.pending), Modifier.weight(1f))
                VerticalDivider(color = colors.outlineVariant)
                NaniFact("Previsto", CurrencyUtils.format(data.expected), Modifier.weight(1f))
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant.copy(alpha = .65f))
            TextButton(
                onClick = onPayments,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "Acompanhar recebimentos",
                    Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.East, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun rememberDashboardHour(): Int {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hour = LocalTime.now().hour
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return hour
}

internal fun dashboardGreetingResource(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

internal fun monthName(month: Int): String = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)[month - 1]
