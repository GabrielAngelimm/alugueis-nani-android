package com.rentalvalidator.app.presentation.design

import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rentalvalidator.app.presentation.components.*

@Composable
fun NaniSearchButton(query: String, onClick: () -> Unit) {
    IconButton(onClick=onClick) {
        BadgedBox(badge={if(query.isNotBlank()) Badge()}) { Icon(Icons.Outlined.Search, "Buscar inquilino") }
    }
}

/** Focus belongs to the search window; dismissing keeps the entered query active. */
@Composable
fun NaniSearchDialog(query: String, onQuery: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest=onDismiss) {
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))) {
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(R.string.search_tenant),Modifier.weight(1f).semantics { heading() },style=MaterialTheme.typography.titleLarge)
                    IconButton(onClick=onDismiss) {Icon(Icons.Outlined.Close,"Fechar busca")}
                }
                ModernTextField(query,onQuery,"Nome do inquilino",focusRequester=focus,
                    trailingIcon={if(query.isNotEmpty()) IconButton(onClick={onQuery(""); focus.requestFocus(); keyboard?.show()}) {Icon(Icons.Outlined.Close,"Limpar busca")}},
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                    keyboardActions=KeyboardActions(onSearch={onDismiss()}))
                TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End)) {Text(stringResource(R.string.view_results))}
            }
        }
        LaunchedEffect(Unit) { withFrameNanos {}; focus.requestFocus(); keyboard?.show() }
    }
}

@Composable
fun NaniSummaryStrip(firstLabel:String,firstCount:Int,firstIcon:ImageVector,secondLabel:String,secondCount:Int,secondIcon:ImageVector,
    modifier:Modifier=Modifier,firstKind:StatusKind=StatusKind.INFO,secondKind:StatusKind=StatusKind.NEUTRAL, tonal:Boolean=false) {
    val shape = RoundedCornerShape(16.dp)
    Surface(modifier.fillMaxWidth().premiumShadow(shape),shape=shape,color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,if (tonal) MaterialTheme.colorScheme.primary.copy(alpha=.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))) {
        val backdrop = if (tonal) Modifier.background(Brush.linearGradient(listOf(
            lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, .22f),
            MaterialTheme.colorScheme.surface
        ))) else Modifier
        Row(backdrop.height(IntrinsicSize.Min).heightIn(min=56.dp).padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
            SummaryHalf(firstLabel,firstCount,firstIcon,firstKind,Modifier.weight(1f),tonal)
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical=4.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.7f))
            SummaryHalf(secondLabel,secondCount,secondIcon,secondKind,Modifier.weight(1f),tonal)
        }
    }
}
@Composable
private fun SummaryHalf(label:String,count:Int,icon:ImageVector,kind:StatusKind,modifier:Modifier,tonal:Boolean) {
    val color=semanticPalette(kind).foreground
    Row(modifier.semantics(mergeDescendants=true) {}.padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
        Text(count.toString(),style=MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),color=color)
        Text(label,modifier=Modifier.weight(1f,fill=false),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
