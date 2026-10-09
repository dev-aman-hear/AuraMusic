package com.aman.auramusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AuraSpacing {
    val ScreenHorizontal = 20.dp
    val Section = 28.dp
    val Item = 12.dp
}

object AuraShapes {
    val Artwork = RoundedCornerShape(14.dp)
    val Surface = RoundedCornerShape(18.dp)
    val Control = RoundedCornerShape(999.dp)
}

@Composable
fun auraScreenGradient(): Brush {
    val isDark = LocalIsDark.current
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF171216),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFF3F5),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )
    }
}

@Composable
fun AuraScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(auraScreenGradient())
    ) {
        content()
    }
}

