package com.rentalvalidator.app.presentation.design
import com.rentalvalidator.app.presentation.theme.AppSpace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import com.rentalvalidator.app.presentation.components.AppMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Bounded scrolling content with persistent actions, also at larger font scales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaniSheet(title: String, onDismiss: () -> Unit, actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
        containerColor=MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(topStart=24.dp,topEnd=24.dp),tonalElevation=0.dp) {
        Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp * .85f).dp)) {
            NaniHeader(title, action = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Fechar painel")
                }
            })
            Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState())
                .padding(horizontal=AppSpace.page).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
            if(actions!=null) {
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.fillMaxWidth().padding(24.dp)) { actions() }
            }
        }
    }
}

@Composable
fun NaniChoice(title: String, description: String? = null, selected: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(
        if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec=tween(AppMotion.StateDuration),label="choice surface")
    val stroke by animateColorAsState(
        if(selected) MaterialTheme.colorScheme.primary.copy(alpha=.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f),
        animationSpec=tween(AppMotion.StateDuration),label="choice outline")
    Surface(shape=RoundedCornerShape(12.dp),color=fill,border=BorderStroke(1.dp,stroke)) {
        Row(Modifier.fillMaxWidth().selectable(selected,role=Role.RadioButton,onClick=onClick)
            .heightIn(min=56.dp).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title,style=MaterialTheme.typography.titleSmall)
                if(description!=null) Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            RadioButton(selected,onClick=null)
        }
    }
}
