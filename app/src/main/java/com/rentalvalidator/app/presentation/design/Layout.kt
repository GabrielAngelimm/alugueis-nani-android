package com.rentalvalidator.app.presentation.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.rentalvalidator.app.presentation.components.AppMotion
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace

/**
 * Page header. Top-level pages get a large title in the account-book voice; pages reached
 * from another one get a back arrow and a smaller title so the content below can lead.
 */
@Composable
fun NaniHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    if (onBack == null) {
        Row(Modifier.fillMaxWidth().padding(start = AppSpace.page, end = 8.dp, top = 20.dp, bottom = 10.dp)
            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) Row(verticalAlignment = Alignment.CenterVertically) { action() }
        }
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) Row(verticalAlignment = Alignment.CenterVertically) { action() }
        }
    }
}

/**
 * Segmented choice between views of the same content. A sheet-colored thumb slides
 * under the chosen option; the track is a recess in the paper.
 */
@Composable
fun NaniTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab
) {
    if (options.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val light = colors.background.luminance() > .5f
    // At night the recess is darker than the sheet and the thumb lighter, so the choice stays legible.
    BoxWithConstraints(modifier.fillMaxWidth().clip(RoundedCornerShape(AppSize.controlRadius))
        .background(if (light) colors.surfaceContainerHigh else colors.surfaceContainerLowest).padding(4.dp)) {
        val segment = maxWidth / options.size
        val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        val fit = rememberWordFitScale(options, labelStyle, segment - 14.dp)
        val offset by animateDpAsState(segment * selected.coerceIn(options.indices), tween(AppMotion.StateDuration, easing = AppMotion.Settle), label = "tab thumb")
        Box {
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset(x = offset).width(segment).fillMaxHeight()
                    .then(if (light) Modifier.shadow(1.dp, RoundedCornerShape(11.dp), clip = false) else Modifier)
                    .clip(RoundedCornerShape(11.dp)).background(if (light) colors.surfaceBright else colors.surfaceContainerHighest))
            }
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { index, title ->
                    val active = index == selected
                    val tint by animateColorAsState(if (active) colors.onSurface else colors.onSurfaceVariant,
                        tween(AppMotion.StateDuration), label = "tab label")
                    Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(11.dp))
                        .selectable(active, role = role, onClick = { onSelect(index) })
                        .padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(title, style = labelStyle.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold).scaled(fit),
                            color = tint, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
    }
}

/** A titled block on a page, for content that is not a lazy list. */
@Composable
fun NaniSection(
    title: String,
    detail: String? = null,
    topPadding: Dp = AppSpace.section,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = topPadding)) {
        SectionTitle(title, detail = detail)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page), content = content)
    }
}

/**
 * Long forms own the whole window. Back and dismiss keep the existing unsaved-edit behavior.
 *
 * With [edgeToEdge] the window lays out behind the system bars and the keyboard instead of being
 * fitted or panned by the system: the editor keeps the status bar clear and leaves the bottom to
 * its content, which lifts its own actions with the keyboard (`imePadding`) and clears the
 * navigation bar. Nothing is pushed off the top when a field takes focus.
 */
@Composable
fun NaniEditor(onDismiss: () -> Unit, edgeToEdge: Boolean = false, content: @Composable () -> Unit) {
    // Read insets from the host window: a decor-fitting Dialog reports zero system bars.
    val hostBars = WindowInsets.systemBars.asPaddingValues()
    val availableHeight = LocalConfiguration.current.screenHeightDp.dp - hostBars.calculateTopPadding() - hostBars.calculateBottomPadding()
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = !edgeToEdge)) {
        val view = LocalView.current
        val window = (view.parent as DialogWindowProvider).window
        val light = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.decorView.elevation = 0f
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, !edgeToEdge)
            if (edgeToEdge) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
        Surface((if (edgeToEdge) Modifier else Modifier.heightIn(max = availableHeight)).fillMaxSize(),
            color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().then(if (edgeToEdge) Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)) else Modifier),
                contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = AppSize.editorMaxWidth).fillMaxSize()) { content() }
            }
        }
    }
}
