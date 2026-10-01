package com.rentalvalidator.app.presentation.design
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import com.rentalvalidator.app.presentation.components.AppMotion
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Product primitives: open headings, indexed records and purposeful surfaces. */
@Composable
fun NaniHeader(title:String, subtitle:String?=null, onBack:(()->Unit)?=null, action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page,vertical=20.dp),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(onBack!=null) IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Voltar") }
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})
            if(!subtitle.isNullOrBlank()) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun NaniTabs(options:List<String>,selected:Int,onSelect:(Int)->Unit,modifier:Modifier=Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
        options.forEachIndexed { index,title ->
            val active=index==selected
            val tint by animateColorAsState(if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,animationSpec=tween(AppMotion.StateDuration),label="tab")
            Column(Modifier.weight(1f).fillMaxHeight().selectable(active,role=Role.Tab,onClick={onSelect(index)})) {
                Box(Modifier.fillMaxWidth().weight(1f).heightIn(min=48.dp).padding(horizontal=4.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
                    Text(title,style=MaterialTheme.typography.labelLarge,color=tint,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
                Box(Modifier.fillMaxWidth().height(3.dp),contentAlignment=Alignment.BottomCenter) {
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.65f))
                    if(active) Box(Modifier.fillMaxWidth().padding(horizontal=16.dp).height(3.dp)
                        .clip(RoundedCornerShape(2.dp)).background(tint))
                }
            }
        }
    }
}

@Composable
fun NaniActionRow(title:String,description:String?,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role=Role.Button,onClick=onClick).heightIn(min=64.dp).padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Icon(icon,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall)
            if(description!=null) Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NaniFact(label:String,value:String,modifier:Modifier=Modifier) {
    Column(modifier.padding(vertical=10.dp)) {
        Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp)); Text(value,style=MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun NaniSection(
    title:String,
    detail:String?=null,
    topPadding:Dp=AppSpace.section,
    content:@Composable ColumnScope.()->Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal=AppSpace.page).padding(top=topPadding)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleMedium)
            if(detail!=null) Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp)); content()
    }
}

/** Long forms own the full window. Back and dismiss keep the existing unsaved-edit behavior. */
@Composable
fun NaniEditor(onDismiss:()->Unit,content:@Composable ()->Unit) {
    // Read insets from the host window: a decor-fitting Dialog reports zero system bars.
    val hostBars = WindowInsets.systemBars.asPaddingValues()
    val availableHeight = LocalConfiguration.current.screenHeightDp.dp - hostBars.calculateTopPadding() - hostBars.calculateBottomPadding()
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,
        properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)) {
        val view = LocalView.current
        val window = (view.parent as DialogWindowProvider).window
        val light = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.decorView.elevation = 0f
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
        Surface(Modifier.heightIn(max=availableHeight).fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
                Box(Modifier.widthIn(max=AppSize.editorMaxWidth).fillMaxSize()) {content()}
            }
        }
    }
}
