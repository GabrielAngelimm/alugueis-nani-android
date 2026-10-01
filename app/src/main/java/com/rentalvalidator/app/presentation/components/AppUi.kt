package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.*

val ScreenHorizontalPadding = AppSpace.page
// Content is intentionally cleared above floating controls. A shallow bottom
// inset looks tidy in screenshots but hides the last useful row in real use.
val FloatingNavigationContentClearance = 12.dp
val FloatingActionContentClearance = 100.dp
val FloatingActionBottomClearance = 24.dp

private val AppHeaderSideSlotWidth = 48.dp

/**
 * A compact, animated segmented control inspired by premium mobile interfaces.
 * The translucent shell is intentionally limited to this floating control.
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if(options.isNotEmpty()) com.rentalvalidator.app.presentation.design.NaniTabs(options,selectedIndex,onSelected,modifier)
}

@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    transparent: Boolean = false
) {
    com.rentalvalidator.app.presentation.design.NaniHeader(title,subtitle,onBack) {
        if(onSearch!=null) IconButton(onClick=onSearch) { Icon(Icons.Rounded.Search,"Buscar") }
        action?.invoke()
    }
}

@Composable
private fun HeaderBackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.size(AppSize.touch).background(MaterialTheme.colorScheme.surface, CircleShape)
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = AppIconNeutral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f).semantics { heading() },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                trailing,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, colorIndex: Int = name.hashCode()) {
    val initials=name.trim().split(Regex("\\s+")).filter{it.isNotBlank()}.take(2).joinToString(""){it.first().uppercase()}.ifBlank{"?"}
    Box(modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
        Text(initials,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun StatusBadge(label: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val colors = semanticPalette(kind)
    Surface(
        modifier = modifier,
        color = colors.background,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.foreground.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(4.dp).background(colors.foreground.copy(alpha = 0.88f), CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

data class SemanticPalette(val background: Color, val foreground: Color)

@Composable
fun semanticPalette(kind: StatusKind): SemanticPalette {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val (background, foreground) = when (kind) {
        StatusKind.SUCCESS -> if (isLight) SuccessSoft to SuccessGreen else Color(0xFF77D4AE).copy(alpha = 0.14f) to Color(0xFF77D4AE)
        StatusKind.WARNING -> if (isLight) WarningSoft to WarningAmber else Color(0xFFF2B85C).copy(alpha = 0.14f) to Color(0xFFF2B85C)
        StatusKind.ERROR -> if (isLight) ErrorSoft to ErrorRed else Color(0xFFFF8B83).copy(alpha = 0.14f) to Color(0xFFFF8B83)
        StatusKind.INFO -> if (isLight) InfoSoft to InfoBlue else Color(0xFFAAB5E8).copy(alpha = 0.14f) to Color(0xFFAAB5E8)
        StatusKind.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    return SemanticPalette(background, foreground)
}

enum class StatusKind { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

@Composable
fun AppEmptyState(icon:ImageVector,title:String,message:String,modifier:Modifier=Modifier,actionLabel:String?=null,onAction:(()->Unit)?=null) {
    Column(modifier.fillMaxWidth().padding(24.dp).semantics{liveRegion=LiveRegionMode.Polite}) {
        Icon(icon,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp));Text(title,style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp));Text(message,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(actionLabel!=null&&onAction!=null){Spacer(Modifier.height(20.dp));PrimaryButton(actionLabel,onAction)}
    }
}
@Composable
fun AppLoadingState(title:String,message:String,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp).semantics{liveRegion=LiveRegionMode.Polite}) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp));Text(title,style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp));Text(message,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun AppErrorState(title:String,message:String,actionLabel:String,onAction:()->Unit,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp).semantics{liveRegion=LiveRegionMode.Assertive}) {
        Icon(Icons.Rounded.ErrorOutline,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(20.dp));Text(title,style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp));Text(message,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));PrimaryButton(actionLabel,onAction)
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
        Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, trailingIcon: (@Composable () -> Unit)? = null) {
    val fieldShape = RoundedCornerShape(10.dp)
    val containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White
    } else {
        AppSearchFieldDark
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            ,
        minLines = 1,
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, "Buscar", modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingIcon,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        shape = fieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            unfocusedBorderColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
                Color.Transparent
            } else {
                Color.Transparent
            },
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
