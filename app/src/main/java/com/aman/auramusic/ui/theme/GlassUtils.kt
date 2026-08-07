package com.aman.auramusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility extensions and composables for Glassmorphism UI finish.
 */

@Composable
fun glassBackgroundBrush(isDark: Boolean = LocalIsDark.current): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.75f)
            )
        )
    }
}

@Composable
fun glassBorderBrush(isDark: Boolean = LocalIsDark.current): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFE0E0E6).copy(alpha = 0.60f)
            )
        )
    }
}

@Composable
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp
): Modifier {
    val isDark = LocalIsDark.current
    val effectiveElevation = if (isDark) elevation else (elevation / 2).coerceAtLeast(1.dp)
    return this
        .shadow(effectiveElevation, shape, clip = false)
        .clip(shape)
        .background(glassBackgroundBrush(isDark))
        .border(borderWidth, glassBorderBrush(isDark), shape)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDark.current
    val effectiveElevation = if (isDark) elevation else (elevation / 2).coerceAtLeast(1.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier
            .shadow(effectiveElevation, shape, clip = false)
            .clip(shape)
            .background(glassBackgroundBrush(isDark))
            .border(borderWidth, glassBorderBrush(isDark), shape)
    ) {
        content()
    }
}
