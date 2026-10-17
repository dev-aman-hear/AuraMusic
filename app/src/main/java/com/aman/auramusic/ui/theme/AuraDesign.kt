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

object AuraActionButtons {
    /** 25% opacity = 75% transparency for subtle Apple Music-style action & navigation buttons */
    const val BackgroundAlpha = 0.25f
    const val BorderAlpha = 0.25f
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
