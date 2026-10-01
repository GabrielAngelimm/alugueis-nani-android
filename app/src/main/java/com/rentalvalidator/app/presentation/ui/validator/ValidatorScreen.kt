package com.rentalvalidator.app.presentation.ui.validator

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import com.rentalvalidator.app.presentation.design.*
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

private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")
private val monthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBrLocale)

internal fun YearMonth.displayNamePtBr(): String =
    format(monthYearFormatter).replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(ptBrLocale) else character.toString()
    }

@Composable
fun ValidatorScreen(viewModel: ValidatorViewModel = hiltViewModel(), onFinanceNavigate:(com.rentalvalidator.app.Screen)->Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val period by viewModel.referencePeriod.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showSnackbar = rememberAppSnackbar()
    LaunchedEffect(viewModel) { viewModel.errors.collect { showSnackbar(it) } }
    var showPeriodPicker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var filterBank by remember { mutableStateOf<String?>(null) }
    var filterUnit by remember { mutableStateOf<String?>(null) }
    var filterDueDay by remember { mutableStateOf<Int?>(null) }
    var registerTenant by remember { mutableStateOf<Tenant?>(null) }
    var defaultStatus by remember { mutableStateOf(StoredPaymentStatus.PAGO) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            var name = "Extrato selecionado"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) }
            }
            viewModel.selectFile(uri, name)
        }
    }

    if(showSearch) NaniSearchDialog(query,{query=it},{showSearch=false})
    Column(Modifier.fillMaxSize()) {
        com.rentalvalidator.app.presentation.navigation.FinanceNavigation("validator",onFinanceNavigate) {
            if(state is ValidatorState.Success) {
                NaniSearchButton(query) {showSearch=true}
                IconButton(onClick={showFilters=true}) {BadgedBox(badge={if(filterBank!=null || filterUnit!=null || filterDueDay!=null) Badge()}) {Icon(Icons.Rounded.Tune,"Filtros")}}
            }
        }
        Box(Modifier.weight(1f)) {
    when (val current = state) {
        is ValidatorState.Idle -> ValidatorIdle(current, period, { showPeriodPicker = true }, { picker.launch("*/*") }, viewModel::clearSelectedFile, viewModel::validateSelectedFile)
        ValidatorState.Loading -> LoadingValidation()
        is ValidatorState.Error -> ErrorValidation(current.message, viewModel::reset)
        is ValidatorState.Success -> {
            val visible = current.results.filter { result ->
                (result.tenant.name.contains(query, true) || result.tenant.aliases.any { it.contains(query, true) }) &&
                    (filterBank == null || result.tenant.bank == filterBank) &&
                    (filterUnit == null || result.tenant.unit == filterUnit) &&
                    (filterDueDay == null || result.tenant.dueDay == filterDueDay)
            }
            ValidationResults(
                visible, query, { query = it },
                hasFilters = filterBank != null || filterUnit != null || filterDueDay != null,
                onFilters = { showFilters = true }, onReset = viewModel::reset,
                onRegister = { tenant, paid -> registerTenant = tenant; defaultStatus = if (paid) StoredPaymentStatus.PAGO else StoredPaymentStatus.PENDENTE },
                onCharge = { result, includePenalty ->
                    if (result.tenant.phone.isBlank()) showSnackbar("Inquilino sem telefone cadastrado")
                    else {
                        val monthName = period.month.getDisplayName(TextStyle.FULL, ptBrLocale)
                        WhatsAppUtils.openWhatsApp(
                            context,
                            result.tenant.phone,
                            WhatsAppUtils.createBillingMessage(result, monthName, includePenalty)
                        )
                    }
                }
            )
        }
    }

        }
    }
    if (showPeriodPicker) MonthYearPickerBottomSheet(period, { showPeriodPicker = false }) { viewModel.updateReferencePeriod(it); showPeriodPicker = false }
    if (showFilters) TenantFilterBottomSheet(filterBank, filterUnit, filterDueDay, onDismiss = { showFilters = false }) { bank, unit, day -> filterBank = bank; filterUnit = unit; filterDueDay = day; showFilters = false }
    registerTenant?.let { tenant ->
        RegisterPaymentBottomSheet(tenant, period.year, period.monthValue, defaultStatus, { registerTenant = null }) { id, year, month, status -> viewModel.registerPayment(id, year, month, status) { registerTenant = null; showSnackbar("Pagamento atualizado") } }
    }
}

