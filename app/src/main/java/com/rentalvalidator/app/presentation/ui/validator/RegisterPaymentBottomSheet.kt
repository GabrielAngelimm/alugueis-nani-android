package com.rentalvalidator.app.presentation.ui.validator
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import java.time.YearMonth
import com.rentalvalidator.app.presentation.ui.monthly_grid.PaymentStatusChoices

@Composable
fun RegisterPaymentBottomSheet(tenant:Tenant,defaultYear:Int,defaultMonth:Int,defaultStatus:PaymentStatus=PaymentStatus.PAGO,
    onDismiss:()->Unit,onConfirm:(String,Int,Int,PaymentStatus)->Unit) {
    var period by remember { mutableStateOf(YearMonth.of(defaultYear,defaultMonth)) }
    var status by remember { mutableStateOf(defaultStatus) }
    var showPicker by remember { mutableStateOf(false) }
    NaniSheet("Registrar pagamento",onDismiss,actions={
        PrimaryButton("Confirmar",{onConfirm(tenant.id,period.year,period.monthValue,status)})
    }) {
        NaniSheetTenantCard(tenant.name, tenant.unit.ifBlank { "Geral" })
        NaniActionRow("Mês de referência","${period.monthValue.toString().padStart(2,'0')}/${period.year}",
            Icons.Outlined.CalendarMonth,{showPicker=true})
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Situação do pagamento",style=MaterialTheme.typography.titleSmall)
            PaymentStatusChoices(status) { status=it }
        }
    }
    if(showPicker) MonthYearPickerBottomSheet(period,{showPicker=false}) {period=it;showPicker=false}
}
