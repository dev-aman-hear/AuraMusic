package com.aman.auramusic.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material levels for the Aura Liquid Glass Design System.
 */
enum class GlassLevel {
    /**
     * Ultra-thin glass: Floating navigation, compact playback controls, badges, and subtle overlays.
     */
    UltraThin,

    /**
     * Regular glass: Cards, search fields, toolbar controls, list active states, and interactive panels.
     */
    Regular,

    /**
     * Thick glass: Bottom sheets, dialogs, expanded menus, player controls pod, and prominent floating panels.
     */
    Thick,

    /**
     * Tinted glass: Selected navigation items, active controls, favorite indicators, and prominent actions.
     */
    Tinted
}

/**
 * Aura Liquid Glass styling engine.
 * Computes background refraction brushes, specular top sheen, and beveled glass borders.
 */
object AuraLiquidGlass {

    fun containerBrush(
        level: GlassLevel,
        isDark: Boolean,
        tint: Color? = null
    ): Brush {
        val effectiveTint = tint ?: (if (isDark) Color(0xFF1E1A26) else Color(0xFFFFFFFF))
        return when (level) {
            GlassLevel.UltraThin -> {
                if (isDark) {
                    val c1 = effectiveTint.copy(alpha = 0.28f)
                    val c2 = Color(0xFF100E15).copy(alpha = 0.55f)
                    Brush.verticalGradient(listOf(c1, c2))
                } else {
                    val c1 = Color.White.copy(alpha = 0.82f)
                    val c2 = Color(0xFFF3F4F9).copy(alpha = 0.70f)
                    Brush.verticalGradient(listOf(c1, c2))
                }
            }
            GlassLevel.Regular -> {
                if (isDark) {
                    val c1 = effectiveTint.copy(alpha = 0.40f)
                    val c2 = Color(0xFF121018).copy(alpha = 0.78f)
                    Brush.verticalGradient(listOf(c1, c2))
                } else {
                    val c1 = Color.White.copy(alpha = 0.90f)
                    val c2 = Color(0xFFEDEFF6).copy(alpha = 0.82f)
                    Brush.verticalGradient(listOf(c1, c2))
                }
            }
            GlassLevel.Thick -> {
                if (isDark) {
                    val c1 = effectiveTint.copy(alpha = 0.50f)
                    val c2 = Color(0xFF0F0D14).copy(alpha = 0.90f)
                    Brush.verticalGradient(listOf(c1, c2))
                } else {
                    val c1 = Color.White.copy(alpha = 0.96f)
                    val c2 = Color(0xFFE8EBF3).copy(alpha = 0.90f)
                    Brush.verticalGradient(listOf(c1, c2))
                }
            }
            GlassLevel.Tinted -> {
                val accent = tint ?: AuraPrimary
                if (isDark) {
                    val c1 = accent.copy(alpha = 0.38f)
                    val c2 = accent.copy(alpha = 0.16f)
                    val c3 = Color(0xFF14101A).copy(alpha = 0.80f)
                    Brush.verticalGradient(listOf(c1, c2, c3))
                } else {
                    val c1 = accent.copy(alpha = 0.25f)
                    val c2 = Color.White.copy(alpha = 0.88f)
                    Brush.verticalGradient(listOf(c1, c2))
                }
            }
        }
    }

    fun borderBrush(
        level: GlassLevel,
        isDark: Boolean,
        tint: Color? = null
    ): Brush {
        val accent = tint ?: (if (isDark) Color.White else Color.Black)
        return when (level) {
            GlassLevel.UltraThin -> {
                if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f), // Top crisp rim
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.60f),
                            Color(0xFFCBD5E1).copy(alpha = 0.40f),
                            Color(0xFF94A3B8).copy(alpha = 0.20f)
                        )
                    )
                }
            }
            GlassLevel.Regular -> {
                if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color.White.copy(alpha = 0.65f),
                            Color(0xFFCBD5E1).copy(alpha = 0.45f),
                            Color(0xFF94A3B8).copy(alpha = 0.25f)
                        )
                    )
                }
            }
            GlassLevel.Thick -> {
                if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.70f),
                            Color(0xFF94A3B8).copy(alpha = 0.35f)
                        )
                    )
                }
            }
            GlassLevel.Tinted -> {
                if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.75f),
                            accent.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.40f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.60f),
                            Color.White.copy(alpha = 0.85f),
                            accent.copy(alpha = 0.30f)
                        )
                    )
                }
            }
        }
    }

    fun borderStroke(
        level: GlassLevel,
        isDark: Boolean,
        width: Dp = 1.dp,
        tint: Color? = null
    ): BorderStroke {
        return BorderStroke(width, borderBrush(level, isDark, tint))
    }

    fun defaultElevation(level: GlassLevel, isDark: Boolean): Dp {
        return when (level) {
            GlassLevel.UltraThin -> if (isDark) 12.dp else 8.dp
            GlassLevel.Regular -> if (isDark) 14.dp else 8.dp
            GlassLevel.Thick -> if (isDark) 20.dp else 12.dp
            GlassLevel.Tinted -> if (isDark) 14.dp else 8.dp
        }
    }

    fun defaultSheenAlpha(level: GlassLevel, isDark: Boolean): Float {
        return when (level) {
            GlassLevel.UltraThin -> if (isDark) 0.18f else 0.40f
            GlassLevel.Regular -> if (isDark) 0.22f else 0.45f
            GlassLevel.Thick -> if (isDark) 0.26f else 0.50f
            GlassLevel.Tinted -> if (isDark) 0.25f else 0.48f
        }
    }
}

/**
 * Primary Liquid Glass modifier for translucent surfaces.
 */
/**
 * Primary Liquid Glass modifier for translucent surfaces.
 * Automatically inherits active configuration from LocalLiquidGlassTokens.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.Regular,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    borderWidth: Dp = 1.1.dp,
    elevation: Dp? = null,
    sheenAlpha: Float? = null,
    blurRadius: Dp? = null
): Modifier {
    val tokens = LocalLiquidGlassTokens.current
    val effectiveShape = shape ?: tokens.surfaceShape
    val effectiveElevation = elevation ?: tokens.elevation(level)
    val effectiveSheen = sheenAlpha ?: tokens.sheenAlpha(level)
    val effectiveBlur = blurRadius ?: tokens.blurRadius
    val containerBrush = tokens.containerBrush(level, tint)
    val borderStroke = tokens.borderStroke(level, tint, borderWidth)

    var mod: Modifier = this

    // 1. Soft ambient drop shadow for authentic floating elevation
    if (effectiveElevation > 0.dp) {
        mod = mod.shadow(
            elevation = effectiveElevation,
            shape = effectiveShape,
            spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color(0x33000000),
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0x22000000)
        )
    }

    // 2. Clip to surface geometry
    mod = mod.clip(effectiveShape)

    // 3. Refraction background fill
    mod = mod.background(containerBrush)

    // 5. Specular top glare sheen (light passing through upper curvature of glass)
    if (effectiveSheen > 0f) {
        mod = mod.drawBehind {
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = effectiveSheen),
                        Color.White.copy(alpha = effectiveSheen * 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.45f
                )
            )
        }
    }

    // 6. Signature multi-stop illuminated top rim glass border
    mod = mod.border(borderStroke, effectiveShape)

    return mod
}

/**
 * Compatibility brushes
 */
@Composable
fun glassBackgroundBrush(isDark: Boolean = LocalIsDark.current): Brush {
    val tokens = LocalLiquidGlassTokens.current
    return tokens.containerBrush(GlassLevel.Regular)
}

@Composable
fun glassBorderBrush(isDark: Boolean = LocalIsDark.current): Brush {
    val tokens = LocalLiquidGlassTokens.current
    return tokens.borderBrush(GlassLevel.Regular)
}

@Composable
fun Modifier.glassEffect(
    shape: Shape? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp? = null
): Modifier {
    return this.liquidGlass(
        shape = shape,
        level = GlassLevel.Regular,
        borderWidth = borderWidth,
        elevation = elevation
    )
}

/**
 * Universal Liquid Glass Surface component.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.Regular,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.1.dp,
    sheenAlpha: Float? = null,
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val effectiveShape = shape ?: tokens.surfaceShape

    Surface(
        shape = effectiveShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier.liquidGlass(
            shape = effectiveShape,
            level = level,
            isDark = isDark,
            tint = tint,
            borderWidth = borderWidth,
            elevation = elevation,
            sheenAlpha = sheenAlpha
        )
    ) {
        content()
    }
}

/**
 * Alias for LiquidGlassSurface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.Regular,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.1.dp,
    sheenAlpha: Float? = null,
    content: @Composable () -> Unit
) = LiquidGlassSurface(
    modifier = modifier,
    shape = shape,
    level = level,
    isDark = isDark,
    tint = tint,
    elevation = elevation,
    borderWidth = borderWidth,
    sheenAlpha = sheenAlpha,
    content = content
)

/**
 * Reusable Liquid Glass Card component with optional click listener.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.Regular,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.2.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val effectiveShape = shape ?: tokens.cardShape

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .liquidGlass(
                shape = effectiveShape,
                level = level,
                isDark = isDark,
                tint = tint,
                borderWidth = borderWidth,
                elevation = elevation
            )
            .then(clickMod)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Alias for LiquidGlassCard
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.Regular,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.2.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) = LiquidGlassCard(
    modifier = modifier,
    onClick = onClick,
    shape = shape,
    level = level,
    isDark = isDark,
    tint = tint,
    elevation = elevation,
    borderWidth = borderWidth,
    contentPadding = contentPadding,
    content = content
)

/**
 * Reusable Liquid Glass Button/Control capsule or circle.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.UltraThin,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.1.dp,
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val effectiveShape = shape ?: CircleShape

    Box(
        modifier = modifier
            .liquidGlass(
                shape = effectiveShape,
                level = level,
                isDark = isDark,
                tint = tint,
                borderWidth = borderWidth,
                elevation = elevation
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Alias for LiquidGlassButton
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    level: GlassLevel = GlassLevel.UltraThin,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    elevation: Dp? = null,
    borderWidth: Dp = 1.1.dp,
    content: @Composable () -> Unit
) = LiquidGlassButton(
    onClick = onClick,
    modifier = modifier,
    shape = shape,
    level = level,
    isDark = isDark,
    tint = tint,
    elevation = elevation,
    borderWidth = borderWidth,
    content = content
)

/**
 * Sleek Glass Filter Pill / Chip.
 */
@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDark.current,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val level = if (selected) GlassLevel.Tinted else GlassLevel.UltraThin
    val tint = if (selected) activeColor else null

    Box(
        modifier = modifier
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                level = level,
                isDark = isDark,
                tint = tint,
                borderWidth = if (selected) 1.4.dp else 1.dp,
                elevation = if (selected) 8.dp else 3.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (selected) {
                if (isDark) Color.White else activeColor
            } else {
                if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.65f)
            }
        )
    }
}

/**
 * Alias for LiquidGlassChip
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDark.current,
    activeColor: Color = MaterialTheme.colorScheme.primary
) = LiquidGlassChip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier,
    isDark = isDark,
    activeColor = activeColor
)

/**
 * Reusable Liquid Glass Dialog container.
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .liquidGlass(
                    shape = shape ?: RoundedCornerShape((tokens.config.cornerRadius + 6).dp),
                    level = GlassLevel.Thick
                )
        ) {
            content()
        }
    }
}

/**
 * Reusable Liquid Glass Sheet container.
 */
@Composable
fun LiquidGlassSheet(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    content: @Composable () -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val radius = (tokens.config.cornerRadius + 4).dp
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape ?: RoundedCornerShape(topStart = radius, topEnd = radius),
                level = GlassLevel.Thick
            )
    ) {
        content()
    }
}
