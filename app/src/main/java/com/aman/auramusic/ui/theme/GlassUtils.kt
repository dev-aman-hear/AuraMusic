package com.aman.auramusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
fun glassBackgroundBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
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
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.55f)
            )
        )
    }
}

@Composable
fun glassBorderBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
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
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.30f)
            )
        )
    }
}

@Composable
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp
): Modifier {
    val isDark = isSystemInDarkTheme()
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(glassBackgroundBrush(isDark))
        .border(borderWidth, glassBorderBrush(isDark), shape)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(glassBackgroundBrush(isDark))
            .border(borderWidth, glassBorderBrush(isDark), shape)
    ) {
        content()
    }
}
