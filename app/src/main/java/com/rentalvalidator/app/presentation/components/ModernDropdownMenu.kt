package com.rentalvalidator.app.presentation.components
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDropdownMenu(label:String, options:List<String>, selectedOption:String,
    onOptionSelected:(String)->Unit, modifier:Modifier=Modifier, readOnly:Boolean=false) {
    var expanded by remember { mutableStateOf(false) }
    val labelColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AppMotion.FeedbackDuration),
        label = "dropdown label"
    )
    val containerColor = modernFieldContainerColor()
    Column(modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(7.dp))
        ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={ if(!readOnly) expanded=it }) {
            OutlinedTextField(value=selectedOption,onValueChange={},readOnly=true,singleLine=true,
                modifier=Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable,enabled=!readOnly)
                    .semantics { contentDescription=label },
                textStyle=MaterialTheme.typography.bodyLarge,
                trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},shape=RoundedCornerShape(10.dp),
                colors=OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ))
            ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false},
                modifier=Modifier.heightIn(max=320.dp).floatingShadow(RoundedCornerShape(12.dp)),shape=RoundedCornerShape(12.dp),
                containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,shadowElevation=0.dp) {
                options.forEach { option ->
                    DropdownMenuItem(text={Text(option)},onClick={onOptionSelected(option);expanded=false},
                        modifier=Modifier.heightIn(min=48.dp).semantics { selected=option==selectedOption },
                        trailingIcon={if(option==selectedOption) Icon(Icons.Outlined.Check,null,tint=MaterialTheme.colorScheme.primary)})
                }
            }
        }
    }
}
