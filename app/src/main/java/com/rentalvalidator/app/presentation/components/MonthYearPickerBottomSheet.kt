package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.design.NaniSheet
import com.rentalvalidator.app.presentation.theme.NaniTheme
import java.time.YearMonth

private val shortMonths = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
private val fullMonths = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")

/** A year of months laid out like a calendar page; today's month carries a small dot. */
@Composable
fun MonthYearPickerBottomSheet(initialPeriod: YearMonth, onDismiss: () -> Unit, onSave: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(initialPeriod.year) }
    var month by remember { mutableIntStateOf(initialPeriod.monthValue) }
    val today = remember { YearMonth.now() }
    val colors = MaterialTheme.colorScheme
    NaniSheet("Selecionar mês", onDismiss, actions = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Cancelar", onDismiss, Modifier.weight(1f))
            PrimaryButton("Aplicar", { onSave(YearMonth.of(year, month)) }, Modifier.weight(1f))
        }
    }) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainer)
            .padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ year-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Ano anterior") }
            Text(year.toString(), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            IconButton({ year++ }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo ano") }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shortMonths.chunked(3).forEachIndexed { row, labels ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { column, label ->
                        val value = row * 3 + column + 1
                        val active = month == value
                        val isToday = today.year == year && today.monthValue == value
                        val fill by animateColorAsState(if (active) NaniTheme.colors.action else colors.surfaceContainerLow,
                            tween(AppMotion.StateDuration), label = "month fill")
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(fill)
                            .selectable(active, role = Role.RadioButton, onClick = { month = value })
                            .semantics { contentDescription = "${fullMonths[value - 1]} de $year" }
                            .heightIn(min = 56.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold),
                                color = if (active) NaniTheme.colors.onAction else colors.onSurface)
                            if (isToday) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(4.dp)
                                .background(if (active) NaniTheme.colors.onAction else colors.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
