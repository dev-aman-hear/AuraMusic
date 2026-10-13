package com.aman.auramusic.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AuraSpacing {
    val ScreenHorizontal = 20.dp
    val Section = 28.dp
    val Item = 12.dp
}

object AuraShapes {
    val Artwork = RoundedCornerShape(14.dp)
    val Surface = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(22.dp)
    val Control = RoundedCornerShape(999.dp)
}

/**
 * Pure Monochrome solid background brush (no colored ambient blooms or gradients).
 */
@Composable
fun auraScreenGradient(): Brush {
    val isDark = LocalIsDark.current
    val bg = if (isDark) DarkMonochromeColors.background else LightMonochromeColors.background
    return Brush.verticalGradient(listOf(bg, bg))
}

/**
 * Opaque monochrome surface and border definitions.
 */
object AuraGlass {
    /**
     * Opaque base container color for surfaces.
     */
    fun containerColor(isDark: Boolean, opacity: Float = 1.0f): Color {
        return if (isDark) DarkMonochromeColors.surface else LightMonochromeColors.surface
    }

    /**
     * Solid opaque brush.
     */
    fun glassBrush(isDark: Boolean, tint: Color? = null): Brush {
        val color = if (isDark) DarkMonochromeColors.surface else LightMonochromeColors.surface
        return Brush.verticalGradient(listOf(color, color))
    }

    /**
     * Subtle monochrome border brush.
     */
    fun borderBrush(isDark: Boolean): Brush {
        val borderColor = if (isDark) DarkMonochromeColors.border else LightMonochromeColors.border
        return Brush.verticalGradient(listOf(borderColor, borderColor))
    }

    fun borderStroke(isDark: Boolean, width: Dp = 1.dp): BorderStroke {
        val borderColor = if (isDark) DarkMonochromeColors.border else LightMonochromeColors.border
        return BorderStroke(width, borderColor)
    }
}

/**
 * Replaced active frosted glass helper with normal opaque surface,
 * subtle border, and no decorative shadows or sheen glare.
 */
@Composable
fun Modifier.frostedGlass(
    shape: Shape = AuraShapes.Surface,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    sheenAlpha: Float = 0f
): Modifier {
    val surfaceColor = if (isDark) DarkMonochromeColors.surface else LightMonochromeColors.surface
    val borderColor = if (isDark) DarkMonochromeColors.border else LightMonochromeColors.border
    return this
        .clip(shape)
        .background(surfaceColor, shape)
        .border(BorderStroke(borderWidth, borderColor), shape)
}

/**
 * Solid monochrome screen background with zero ambient blooms or gradients.
 */
@Composable
fun AuraScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDark.current
    val bgColor = if (isDark) DarkMonochromeColors.background else LightMonochromeColors.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        content()
    }
}
