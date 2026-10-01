package com.rentalvalidator.app.presentation.ui.validator

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentalvalidator.app.domain.model.PaymentStatus as StoredPaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.usecase.PaymentStatus
import com.rentalvalidator.app.domain.usecase.ValidationResult
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.NaniSummaryStrip
import com.rentalvalidator.app.presentation.theme.*
import com.rentalvalidator.app.presentation.ui.tenants.TenantFilterBottomSheet
import com.rentalvalidator.app.presentation.viewmodel.ValidatorState
import com.rentalvalidator.app.presentation.viewmodel.ValidatorViewModel
import com.rentalvalidator.app.util.CurrencyUtils
import com.rentalvalidator.app.util.WhatsAppUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


import com.rentalvalidator.app.presentation.design.*
@Composable
internal fun ValidatorIdle(state:ValidatorState.Idle,period:YearMonth,onPeriod:()->Unit,onFile:()->Unit,onClear:()->Unit,onValidate:()->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=12.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal=AppSpace.page)) {
                Text("Confira seus recebimentos",style=MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Escolha o período e o arquivo. Depois, revise os valores identificados antes de registrar os pagamentos.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                val selectionShape = RoundedCornerShape(20.dp)
                Surface(Modifier.fillMaxWidth().premiumShadow(selectionShape), shape=selectionShape,
                    color=MaterialTheme.colorScheme.surface,border=surfaceDepthBorder()) {
                Column(Modifier.padding(horizontal=12.dp,vertical=4.dp)) {
                com.rentalvalidator.app.presentation.design.NaniActionRow("Mês de referência",period.displayNamePtBr(),Icons.Rounded.CalendarMonth,onPeriod)
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                com.rentalvalidator.app.presentation.design.NaniActionRow(if(state.selectedFileName==null)"Selecionar extrato" else "Trocar extrato","Arquivo PDF ou CSV",Icons.Rounded.UploadFile,onFile)
                }
                }
                Spacer(Modifier.height(12.dp))
                state.selectedFileName?.let {name ->
                    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(start=16.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {Text(name,style=MaterialTheme.typography.titleSmall);Text("Pronto para conferir",style=MaterialTheme.typography.bodySmall)}
                            IconButton(onClick=onClear){Icon(Icons.Rounded.Close,"Remover")}
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Validar pagamentos",onValidate,enabled=state.selectedFileName!=null)
            }
        }
    }
}

@Composable
internal fun LoadingValidation() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        AppLoadingState(
            title = "Analisando o extrato",
            message = "Conferindo transações e vínculos de pagamento."
        )
    }
}

@Composable
internal fun ErrorValidation(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ValidationResults(results: List<ValidationResult>, query: String, onQuery: (String) -> Unit, hasFilters: Boolean, onFilters: () -> Unit, onReset: () -> Unit, onRegister: (Tenant, Boolean) -> Unit, onCharge: (ValidationResult, Boolean) -> Unit) {
    val paid = results.count { it.status.isPaid }
    val success = semanticPalette(StatusKind.SUCCESS)
    val warning = semanticPalette(StatusKind.WARNING)
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = FloatingNavigationContentClearance)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(start=24.dp,end=16.dp,top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("Resultado da conferência",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick=onReset){Text("Novo")}
            }
            NaniSummaryStrip("Identificados",paid,Icons.Rounded.CheckCircle,"Pendentes",results.size-paid,Icons.Rounded.Schedule,
                Modifier.padding(horizontal=ScreenHorizontalPadding,vertical=12.dp),StatusKind.SUCCESS,StatusKind.WARNING,tonal=true)
            }
        }
        val grouped = results.groupBy { it.tenant.unit.ifBlank { "Geral" } }
        if (grouped.isEmpty()) {
            item { AppEmptyState(Icons.Rounded.SearchOff, "Nenhum resultado encontrado", "Ajuste a busca ou os filtros para visualizar outros inquilinos.") }
        }
        grouped.forEach { (unit, unitResults) ->
            item { SectionHeader(unit, "${unitResults.size} registros", Icons.Rounded.Apartment, Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp)) }
            items(unitResults, key = { it.tenant.id }) { result -> ValidationResultCard(result, { onRegister(result.tenant, result.status.isPaid) }, { includePenalty -> onCharge(result, includePenalty) }) }
        }
    }
}

@Composable
fun ValidatorUnitHeader(unitName: String, results: List<ValidationResult>, isFirst: Boolean = false) { SectionHeader(unitName, "${results.size} registros", Icons.Rounded.Apartment) }
