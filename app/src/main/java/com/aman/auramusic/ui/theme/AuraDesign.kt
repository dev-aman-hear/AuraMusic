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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
 * Atmospheric background with subtle glowing colored nebulae/ambient blooms
 * that make frosted glass overlays translucent and luminous.
 */
@Composable
fun auraScreenGradient(): Brush {
    val isDark = LocalIsDark.current
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF131018),
                Color(0xFF0C0A10),
                Color(0xFF08070B)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFF8FA),
                Color(0xFFF6F7FB),
                Color(0xFFEEF0F6)
            )
        )
    }
}

/**
 * High-end Frosted Glass colors and border brushes
 */
object AuraGlass {
    /**
     * Frosted Glass base container color (translucent smoked obsidian in dark, crystal frosted milk in light)
     */
    fun containerColor(isDark: Boolean, opacity: Float = if (isDark) 0.72f else 0.85f): Color {
        return if (isDark) {
            Color(0xFF14121A).copy(alpha = opacity)
        } else {
            Color(0xFFFFFFFF).copy(alpha = opacity)
        }
    }

    /**
     * Subtle gradient for glass refraction and depth
     */
    fun glassBrush(isDark: Boolean, tint: Color? = null): Brush {
        return if (isDark) {
            val base1 = tint?.copy(alpha = 0.22f) ?: Color(0xFF221E2C).copy(alpha = 0.75f)
            val base2 = Color(0xFF121017).copy(alpha = 0.85f)
            Brush.verticalGradient(listOf(base1, base2))
        } else {
            val base1 = tint?.copy(alpha = 0.12f) ?: Color(0xFFFFFFFF).copy(alpha = 0.92f)
            val base2 = Color(0xFFF3F4F9).copy(alpha = 0.85f)
            Brush.verticalGradient(listOf(base1, base2))
        }
    }

    /**
     * 4-stop Beveled Glass Border brush reflecting top specular light
     */
    fun borderBrush(isDark: Boolean): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f), // Bright specular rim
                    Color.White.copy(alpha = 0.14f), // Glass edge diffusion
                    Color.White.copy(alpha = 0.05f), // Sub-surface edge
                    Color.Black.copy(alpha = 0.35f)  // Ground contact shadow
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.90f), // Crisp bright top rim
                    Color.White.copy(alpha = 0.50f),
                    Color(0xFFCBD5E1).copy(alpha = 0.45f),
                    Color(0xFF94A3B8).copy(alpha = 0.25f)
                )
            )
        }
    }

    fun borderStroke(isDark: Boolean, width: Dp = 1.dp): BorderStroke {
        return BorderStroke(width, borderBrush(isDark))
    }
}

/**
 * Universal frosted glass modifier that applies:
 * 1. Ambient drop shadow
 * 2. Translucent glass refraction background
 * 3. Top specular glare sheen
 * 4. Specular beveled glass border
 * 5. Shape clipping
 */
@Composable
fun Modifier.frostedGlass(
    shape: Shape = AuraShapes.Surface,
    isDark: Boolean = LocalIsDark.current,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    sheenAlpha: Float = if (isDark) 0.16f else 0.35f
): Modifier = this.liquidGlass(
    shape = shape,
    level = if (tint != null) GlassLevel.Tinted else GlassLevel.Regular,
    isDark = isDark,
    tint = tint,
    borderWidth = borderWidth,
    elevation = elevation,
    sheenAlpha = sheenAlpha
)

@Composable
fun AuraScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDark.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(auraScreenGradient())
            .drawBehind {
                val w = size.width
                val h = size.height

                // Ambient colored nebula bloom at top right
                val orb1Color = if (isDark) Color(0xFFE91E63).copy(alpha = 0.08f) else Color(0xFFFFB3C6).copy(alpha = 0.22f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orb1Color, Color.Transparent),
                        center = Offset(w * 0.9f, h * 0.12f),
                        radius = w * 0.75f
                    ),
                    center = Offset(w * 0.9f, h * 0.12f),
                    radius = w * 0.75f
                )

                // Ambient cyan/indigo bloom at middle left
                val orb2Color = if (isDark) Color(0xFF6366F1).copy(alpha = 0.06f) else Color(0xFFBAE6FD).copy(alpha = 0.25f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orb2Color, Color.Transparent),
                        center = Offset(w * 0.1f, h * 0.48f),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.1f, h * 0.48f),
                    radius = w * 0.65f
                )
            }
    ) {
        content()
    }
}


