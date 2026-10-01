package com.rentalvalidator.app.presentation.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.design.NaniSheet
import java.time.YearMonth

@Composable
fun MonthYearPickerBottomSheet(initialPeriod:YearMonth,onDismiss:()->Unit,onSave:(YearMonth)->Unit) {
    var year by remember { mutableIntStateOf(initialPeriod.year) }
    var month by remember { mutableIntStateOf(initialPeriod.monthValue) }
    val months=listOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")
    NaniSheet("Selecionar mês",onDismiss,actions={
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Cancelar",onDismiss,Modifier.weight(1f))
            PrimaryButton("Aplicar",{onSave(YearMonth.of(year,month))},Modifier.weight(1f))
        }
    }) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton({year--}) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"Ano anterior")}
            Text(year.toString(),Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center)
            IconButton({year++}) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"Próximo ano")}
        }
        Column(Modifier.selectableGroup(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            months.chunked(3).forEachIndexed { row,labels ->
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { col,label ->
                        val value=row*3+col+1
                        Surface(Modifier.weight(1f),shape=RoundedCornerShape(12.dp),
                            color=if(month==value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                            Box(Modifier.selectable(month==value,role=Role.RadioButton,onClick={month=value})
                                .heightIn(min=56.dp).padding(12.dp),contentAlignment=Alignment.Center) {
                                Text(label,style=MaterialTheme.typography.labelLarge,
                                    color=if(month==value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
