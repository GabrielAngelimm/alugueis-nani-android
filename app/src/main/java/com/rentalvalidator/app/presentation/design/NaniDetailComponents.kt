package com.rentalvalidator.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentalvalidator.app.presentation.components.premiumShadow
import com.rentalvalidator.app.presentation.components.surfaceDepthBorder
import com.rentalvalidator.app.presentation.theme.AppSpace

enum class DetailIdentity { TENANT, UNIT }

/** Identity and lease facts form one composition, above the supporting cards. */
@Composable
fun NaniDetailHero(
    title: String,
    subtitle: String,
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    status: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    subtitleIcon: ImageVector? = null,
    identity: DetailIdentity = DetailIdentity.TENANT
) {
    val colors = MaterialTheme.colorScheme
    val identityShape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).padding(top = 4.dp, bottom = 16.dp)) {
        Surface(
            shape = identityShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, colors.primary.copy(alpha = .10f))
        ) {
            val dark = colors.surface.luminance() < .5f
            val personal = identity == DetailIdentity.TENANT
            val heroStart = if (personal) { if (dark) Color(0xFF36304A) else Color(0xFFEDE5FA) } else { if (dark) Color(0xFF303252) else Color(0xFFE5E6FF) }
            val heroEnd = if (personal) { if (dark) Color(0xFF282638) else Color(0xFFDCD5F6) } else { if (dark) Color(0xFF202536) else Color(0xFFCDD3FF) }
            val heroInk = if (dark) Color(0xFFF0EFFF) else Color(0xFF24245C)
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(heroStart, heroEnd)))
                    .drawWithCache {
                        // Decorative architecture has no semantics and never sits over the content.
                        val landscape = Path().apply {
                            moveTo(0f, size.height)
                            cubicTo(size.width * .56f, size.height * 1.13f,
                                size.width * .68f, size.height * .70f, size.width, size.height * .18f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        val building = Path().apply {
                            moveTo(size.width * .86f, size.height)
                            lineTo(size.width * .86f, size.height * .65f)
                            lineTo(size.width * .91f, size.height * .48f)
                            lineTo(size.width * .98f, size.height * .59f)
                            lineTo(size.width * .98f, size.height)
                            close()
                        }
                        val lines = List(4) { index ->
                            val shift = index * size.height * .045f
                            Path().apply {
                                moveTo(size.width * .56f, size.height + shift)
                                cubicTo(size.width * .74f, size.height * .73f + shift,
                                    size.width * .83f, size.height * .88f + shift,
                                    size.width * 1.06f, size.height * .61f + shift)
                            }
                        }
                        onDrawBehind {
                            drawPath(landscape, colors.primary.copy(alpha = if (dark) .065f else .075f))
                            if (!personal) {
                            drawPath(building, colors.primary.copy(alpha = .09f))
                            repeat(2) { row -> repeat(2) { col ->
                                drawRoundRect(heroInk.copy(alpha = .065f),
                                    topLeft = Offset(size.width * (.89f + col * .035f), size.height * (.67f + row * .15f)),
                                    size = Size(size.width * .017f, size.height * .085f))
                            } }
                            } else {
                                // A quiet portrait silhouette distinguishes people from properties.
                                val center = Offset(size.width * .92f, size.height * .65f)
                                val tint = colors.primary.copy(alpha = .09f)
                                drawCircle(tint, size.height * .105f, center)
                                drawOval(tint, Offset(size.width * .84f, size.height * .79f), Size(size.width * .16f, size.height * .40f))
                                drawCircle(tint, size.height * .28f, Offset(center.x, size.height * .78f), style = Stroke(.8.dp.toPx()))
                            }
                            lines.forEach { drawPath(it, Color.White.copy(alpha = if (dark) .08f else .30f), style = Stroke(.7.dp.toPx())) }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = if (compact) 24.dp else 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val initials = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    .let { words -> listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 }) }
                    .joinToString("") { it.take(1).uppercase() }
                Surface(
                    modifier = Modifier.size(58.dp).clearAndSetSemantics { },
                    shape = CircleShape,
                    color = if (personal) { if (dark) Color(0xFF8270B3) else Color(0xFF8571CC) } else { if (dark) Color(0xFF6D70C5) else Color(0xFF797AEC) },
                    border = BorderStroke(4.dp, Color.White.copy(alpha = .23f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initials, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-.6).sp,
                            lineHeight = 29.sp
                        ),
                        color = heroInk,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().semantics { heading() }
                    )
                    Row(
                        Modifier.background(Color.White.copy(alpha = if (dark) .08f else .40f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (subtitleIcon != null) {
                            Icon(subtitleIcon, null, Modifier.size(14.dp), tint = heroInk.copy(alpha = .72f))
                        }
                        Text(subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = heroInk.copy(alpha = .82f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (status != null) {
            Spacer(Modifier.height(10.dp))
            status()
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = lerp(colors.surface, colors.primaryContainer, .16f),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = .09f))
        ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val valueStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            val labelStyle = MaterialTheme.typography.labelMedium
            val halfWidth = with(density) { ((maxWidth - 25.dp) / 2).toPx() }
            // Use actual text widths, including the user's font scale, rather than a device breakpoint.
            val fitsSideBySide = listOf(firstValue, secondValue).all {
                measurer.measure(AnnotatedString(it), valueStyle, softWrap = false).size.width <= halfWidth
            } && listOf(firstLabel, secondLabel).all {
                measurer.measure(AnnotatedString(it), labelStyle, softWrap = false).size.width <= halfWidth
            }
            if (!fitsSideBySide) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroFact(firstLabel, firstValue, Modifier.fillMaxWidth())
                    HorizontalDivider(
                        Modifier.padding(horizontal = 24.dp),
                        color = colors.outlineVariant.copy(alpha = .65f)
                    )
                    HeroFact(secondLabel, secondValue, Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroFact(firstLabel, firstValue, Modifier.weight(1f))
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                    HeroFact(secondLabel, secondValue, Modifier.weight(1f))
                }
            }
        }
        }
    }
}

@Composable
private fun HeroFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            value,
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
fun NaniDetailCard(
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    headerIcon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.page).premiumShadow(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = surfaceDepthBorder(.6f)
    ) {
        Column(Modifier.padding(contentPadding)) {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (headerIcon != null) Icon(headerIcon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
            }
            content()
        }
    }
}

@Composable
fun NaniDetailAction(title: String, description: String, icon: ImageVector, onClick: () -> Unit, divider: Boolean = true, status: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (status != null) Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        status,
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (divider) HorizontalDivider(
        Modifier.padding(start = 54.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)
    )
}
