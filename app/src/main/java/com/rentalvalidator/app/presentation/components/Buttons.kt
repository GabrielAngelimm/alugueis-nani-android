package com.rentalvalidator.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.NaniTheme

/** The one strongest action on a surface. Ink-filled, never more than one per view. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fill: Boolean = true
) = NaniButton(text, onClick, modifier, enabled, icon, fill, ButtonDefaults.buttonColors(
    containerColor = NaniTheme.colors.action,
    contentColor = NaniTheme.colors.onAction,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f)
), border = null)

/** An alternative that should not compete with the primary action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fill: Boolean = true
) = NaniButton(text, onClick, modifier, enabled, icon, fill, ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) .7f else .3f)))

/** A supporting action that still deserves a filled target, such as "Cobrar". */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fill: Boolean = true
) = NaniButton(text, onClick, modifier, enabled, icon, fill, ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
), border = null)

/** Irreversible removal. Used only inside a confirmation. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fill: Boolean = true
) = NaniButton(text, onClick, modifier, enabled, icon, fill, ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError
), border = null)

@Composable
private fun NaniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    fill: Boolean,
    colors: ButtonColors,
    border: BorderStroke?
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, AppMotion.PressScale, label = "button press")
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = AppSize.control)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(AppSize.controlRadius),
        colors = colors,
        border = border,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
