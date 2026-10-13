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
        get() = if (isDark) DarkMonochromeColors.surface else LightMonochromeColors.surface

    /**
     * Compute background brush for a surface level conforming to monochrome palette.
     */
    fun containerBrush(
        level: GlassLevel,
        customTint: Color? = null,
        isDark: Boolean = this.isDark
    ): Brush {
        val bg = if (isDark) {
            when (level) {
                GlassLevel.UltraThin -> DarkMonochromeColors.controlBackground
                GlassLevel.Regular -> DarkMonochromeColors.surface
                GlassLevel.Thick -> DarkMonochromeColors.surfaceSecondary
                GlassLevel.Tinted -> customTint ?: DarkMonochromeColors.controlSelected
            }
        } else {
            when (level) {
                GlassLevel.UltraThin -> LightMonochromeColors.controlBackground
                GlassLevel.Regular -> LightMonochromeColors.surface
                GlassLevel.Thick -> LightMonochromeColors.surfaceSecondary
                GlassLevel.Tinted -> customTint ?: LightMonochromeColors.controlSelected
            }
        }
        return Brush.verticalGradient(listOf(bg, bg))
    }

    /**
     * Compute clean border gradient conforming to monochrome palette.
     */
    fun borderBrush(
        level: GlassLevel,
        customTint: Color? = null,
        isDark: Boolean = this.isDark
    ): Brush {
        val border = if (isDark) {
            when (level) {
                GlassLevel.Thick, GlassLevel.Tinted -> DarkMonochromeColors.borderStrong
                else -> DarkMonochromeColors.border
            }
        } else {
            when (level) {
                GlassLevel.Thick, GlassLevel.Tinted -> LightMonochromeColors.borderStrong
                else -> LightMonochromeColors.border
            }
        }
        return Brush.verticalGradient(listOf(border, border))
    }

    /**
     * Calibrated border stroke with dynamic stroke width.
     */
    fun borderStroke(
        level: GlassLevel,
        customTint: Color? = null,
        width: Dp = 1.1.dp,
        isDark: Boolean = this.isDark
    ): BorderStroke {
        return BorderStroke(width, borderBrush(level, customTint, isDark))
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
    fun sheenAlpha(
        level: GlassLevel,
        isDark: Boolean = this.isDark
    ): Float = 0f
}

/**
 * Global composition local for the active Liquid Glass design tokens.
 */
val LocalLiquidGlassTokens = staticCompositionLocalOf {
    LiquidGlassTokens()
}
