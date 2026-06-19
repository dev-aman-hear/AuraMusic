package com.aman.auramusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF375F),
    secondary = Color(0xFFFF6B8A),
    tertiary = Color(0xFFFFB3C1),
    background = Color(0xFF0F0F10),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFEBEBF5),
    outline = Color(0xFF3A3A3C)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF2D55),
    secondary = Color(0xFFE64667),
    tertiary = Color(0xFFFF8FA3),
    background = Color(0xFFFFFBFF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    onPrimary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF3C3C43),
    outline = Color(0xFFD1D1D6)
)

@Composable
fun AuraMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledMode) {
                base.copy(background = Color.Black, surface = Color.Black)
            } else base
        }
        darkTheme -> {
            if (amoledMode) DarkColorScheme.copy(background = Color.Black, surface = Color.Black)
            else DarkColorScheme
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
