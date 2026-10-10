package com.aman.auramusic.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aman.auramusic.data.model.LiquidGlassConfig
import com.aman.auramusic.data.model.LiquidGlassPreset

/**
 * Resolved dynamic tokens for the Liquid Glass visual engine.
 * Computes calibrated colors, refraction gradients, specular border rim, and depths.
 */
@Immutable
data class LiquidGlassTokens(
    val config: LiquidGlassConfig = LiquidGlassConfig(),
    val isDark: Boolean = true,
    val isAmoled: Boolean = false,
    val accentColor: Color = AuraPrimary
) {
    val preset: LiquidGlassPreset get() = config.preset
    val reduceTransparency: Boolean get() = config.reduceTransparency
    val reduceMotion: Boolean get() = config.reduceMotion

    val blurRadius: Dp get() = if (reduceTransparency) 0.dp else config.blurRadius.dp
    val baseCornerRadius: Dp get() = config.cornerRadius.dp

    val surfaceShape: Shape get() = RoundedCornerShape(baseCornerRadius)
    val cardShape: Shape get() = RoundedCornerShape((config.cornerRadius + 2).dp)
    val pillShape: Shape get() = RoundedCornerShape(999.dp)
    val circleShape: Shape get() = CircleShape

    /**
     * Parse surface tint color from config or fallback.
     */
    val effectiveTint: Color
        get() {
            if (config.tintHex.isNotBlank()) {
                val hex = config.tintHex.removePrefix("#")
                if (hex.length == 6 || hex.length == 8) {
                    val parsed = runCatching {
                        val colorLong = java.lang.Long.parseLong(hex, 16)
                        if (hex.length == 6) Color(colorLong or 0xFF000000L) else Color(colorLong)
                    }.getOrNull()
                    if (parsed != null) return parsed
                }
            }
            if (config.accentTintEnabled) {
                return accentColor
            }
            return if (isDark) {
                if (isAmoled) Color(0xFF0F0D14) else Color(0xFF1E1A26)
            } else {
                Color(0xFFFFFFFF)
            }
        }

    /**
     * Compute translucent refraction background brush for a glass surface level.
     */
    fun containerBrush(
        level: GlassLevel,
        customTint: Color? = null
    ): Brush {
        val tint = customTint ?: effectiveTint

        if (reduceTransparency) {
            // Accessible solid opaque fallback
            return if (isDark) {
                val solidBg = if (isAmoled) Color(0xFF0A090E) else Color(0xFF181520)
                Brush.verticalGradient(listOf(solidBg, solidBg))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFF8F9FD), Color(0xFFEFF1F7)))
            }
        }

        val intensity = config.intensity

        return when (level) {
            GlassLevel.UltraThin -> {
                if (isDark) {
                    val topAlpha = (intensity * 0.40f + 0.20f).coerceIn(0.25f, 0.75f)
                    val bottomAlpha = (intensity * 0.55f + 0.35f).coerceIn(0.45f, 0.90f)
                    Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = topAlpha),
                            (if (isAmoled) Color.Black else Color(0xFF100E16)).copy(alpha = bottomAlpha)
                        )
                    )
                } else {
                    val topAlpha = (intensity * 0.35f + 0.60f).coerceIn(0.75f, 0.96f)
                    val bottomAlpha = (intensity * 0.35f + 0.52f).coerceIn(0.68f, 0.92f)
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = topAlpha),
                            Color(0xFFEFF1F8).copy(alpha = bottomAlpha)
                        )
                    )
                }
            }
            GlassLevel.Regular -> {
                if (isDark) {
                    val topAlpha = (intensity * 0.45f + 0.25f).coerceIn(0.35f, 0.85f)
                    val bottomAlpha = (intensity * 0.60f + 0.35f).coerceIn(0.55f, 0.95f)
                    Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = topAlpha),
                            (if (isAmoled) Color.Black else Color(0xFF13101A)).copy(alpha = bottomAlpha)
                        )
                    )
                } else {
                    val topAlpha = (intensity * 0.25f + 0.70f).coerceIn(0.82f, 0.98f)
                    val bottomAlpha = (intensity * 0.25f + 0.65f).coerceIn(0.78f, 0.95f)
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = topAlpha),
                            Color(0xFFEAEDF6).copy(alpha = bottomAlpha)
                        )
                    )
                }
            }
            GlassLevel.Thick -> {
                if (isDark) {
                    val topAlpha = (intensity * 0.58f).coerceIn(0.18f, 0.88f)
                    val bottomAlpha = (intensity * 0.92f).coerceIn(0.35f, 0.97f)
                    Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = topAlpha),
                            (if (isAmoled) Color.Black else Color(0xFF0D0B12)).copy(alpha = bottomAlpha)
                        )
                    )
                } else {
                    val topAlpha = (intensity * 0.97f).coerceIn(0.70f, 0.99f)
                    val bottomAlpha = (intensity * 0.90f).coerceIn(0.60f, 0.96f)
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = topAlpha),
                            Color(0xFFE8EBF5).copy(alpha = bottomAlpha)
                        )
                    )
                }
            }
            GlassLevel.Tinted -> {
                val activeAccent = customTint ?: (if (config.accentTintEnabled) effectiveTint else accentColor)
                if (isDark) {
                    val topAlpha = (intensity * 0.45f + 0.15f).coerceIn(0.25f, 0.85f)
                    val midAlpha = (intensity * 0.25f + 0.08f).coerceIn(0.10f, 0.60f)
                    val bottomAlpha = (intensity * 0.85f).coerceIn(0.40f, 0.95f)
                    Brush.verticalGradient(
                        listOf(
                            activeAccent.copy(alpha = topAlpha),
                            activeAccent.copy(alpha = midAlpha),
                            Color(0xFF14101A).copy(alpha = bottomAlpha)
                        )
                    )
                } else {
                    val topAlpha = (intensity * 0.35f + 0.10f).coerceIn(0.18f, 0.70f)
                    val bottomAlpha = (intensity * 0.88f).coerceIn(0.50f, 0.95f)
                    Brush.verticalGradient(
                        listOf(
                            activeAccent.copy(alpha = topAlpha),
                            Color.White.copy(alpha = bottomAlpha)
                        )
                    )
                }
            }
        }
    }

    /**
     * Compute multi-stop specular illuminated rim border gradient.
     * Matches the visual reference: crisp bright top-rim highlight fading smoothly into dark contact shadow.
     */
    fun borderBrush(
        level: GlassLevel,
        customTint: Color? = null
    ): Brush {
        val mult = config.highlightStrength
        val activeAccent = customTint ?: (if (level == GlassLevel.Tinted) accentColor else null)

        if (level == GlassLevel.Tinted && activeAccent != null) {
            return if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        activeAccent.copy(alpha = (0.85f * mult).coerceIn(0.2f, 1f)),
                        activeAccent.copy(alpha = (0.40f * mult).coerceIn(0.1f, 0.8f)),
                        Color.White.copy(alpha = (0.15f * mult).coerceIn(0.02f, 0.4f)),
                        Color.Black.copy(alpha = 0.40f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        activeAccent.copy(alpha = (0.75f * mult).coerceIn(0.2f, 1f)),
                        Color.White.copy(alpha = 0.85f),
                        activeAccent.copy(alpha = (0.35f * mult).coerceIn(0.1f, 0.6f))
                    )
                )
            }
        }

        return if (isDark) {
            val topRim = (0.70f * mult).coerceIn(0.15f, 1.0f)
            val topMid = (0.28f * mult).coerceIn(0.05f, 0.70f)
            val lowerMid = (0.08f * mult).coerceIn(0.01f, 0.30f)
            val bottomShadow = if (isAmoled) 0.50f else 0.35f
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = topRim),
                    Color.White.copy(alpha = topMid),
                    Color.White.copy(alpha = lowerMid),
                    Color.Black.copy(alpha = bottomShadow)
                )
            )
        } else {
            val topRim = (0.98f * mult).coerceIn(0.30f, 1.0f)
            val topMid = (0.65f * mult).coerceIn(0.20f, 0.90f)
            val lowerMid = (0.40f * mult).coerceIn(0.10f, 0.60f)
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = topRim),
                    Color.White.copy(alpha = topMid),
                    Color(0xFFCBD5E1).copy(alpha = lowerMid),
                    Color(0xFF94A3B8).copy(alpha = 0.25f)
                )
            )
        }
    }

    /**
     * Calibrated border stroke with dynamic stroke width.
     */
    fun borderStroke(
        level: GlassLevel,
        customTint: Color? = null,
        width: Dp = 1.1.dp
    ): BorderStroke {
        return BorderStroke(width, borderBrush(level, customTint))
    }

    /**
     * Atmospheric drop shadow elevation.
     */
    fun elevation(level: GlassLevel): Dp {
        if (reduceTransparency) return 4.dp
        val base = config.shadowElevation.toFloat()
        return when (level) {
            GlassLevel.UltraThin -> (base * 0.75f).coerceAtLeast(2f).dp
            GlassLevel.Regular -> base.dp
            GlassLevel.Thick -> (base * 1.35f).dp
            GlassLevel.Tinted -> (base * 0.90f).dp
        }
    }

    /**
     * Top specular sheen glare alpha.
     */
    fun sheenAlpha(level: GlassLevel): Float {
        if (reduceMotion || reduceTransparency) return 0f
        val mult = config.highlightStrength
        return when (level) {
            GlassLevel.UltraThin -> if (isDark) 0.20f * mult else 0.40f * mult
            GlassLevel.Regular -> if (isDark) 0.25f * mult else 0.48f * mult
            GlassLevel.Thick -> if (isDark) 0.30f * mult else 0.55f * mult
            GlassLevel.Tinted -> if (isDark) 0.28f * mult else 0.50f * mult
        }.coerceIn(0f, 0.95f)
    }
}

/**
 * Global composition local for the active Liquid Glass design tokens.
 */
val LocalLiquidGlassTokens = staticCompositionLocalOf {
    LiquidGlassTokens()
}
