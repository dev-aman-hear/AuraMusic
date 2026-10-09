package com.aman.auramusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.aman.auramusic.data.model.ThemeMode

val LocalIsDark = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = AuraPrimary,
    secondary = AuraSecondary,
    tertiary = AuraTertiary,
    background = AuraDarkBackground,
    surface = AuraDarkSurface,
    surfaceVariant = AuraDarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AuraTextPrimaryDark,
    onSurface = AuraTextPrimaryDark,
    onSurfaceVariant = AuraTextSecondaryDark,
    outline = AuraDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPrimary,
    secondary = AuraSecondary,
    tertiary = AuraTertiary,
    background = AuraLightBackground,
    surface = AuraLightSurface,
    surfaceVariant = AuraLightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AuraTextPrimaryLight,
    onSurface = AuraTextPrimaryLight,
    onSurfaceVariant = AuraTextSecondaryLight,
    outline = AuraLightBorder,
    primaryContainer = Color(0xFFFFE8EC),
    onPrimaryContainer = Color(0xFFD81B43)
)

@Composable
fun AuraMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

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

    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
