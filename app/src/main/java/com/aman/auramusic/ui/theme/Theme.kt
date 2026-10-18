package com.aman.auramusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.aman.auramusic.data.model.ThemeMode

val LocalIsDark = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = DarkMonochromeColors.controlSelected,
    onPrimary = DarkMonochromeColors.controlSelectedContent,
    secondary = DarkMonochromeColors.textSecondary,
    onSecondary = DarkMonochromeColors.controlSelectedContent,
    tertiary = DarkMonochromeColors.textTertiary,
    onTertiary = DarkMonochromeColors.controlSelectedContent,
    background = DarkMonochromeColors.background,
    onBackground = DarkMonochromeColors.textPrimary,
    surface = DarkMonochromeColors.surface,
    onSurface = DarkMonochromeColors.textPrimary,
    surfaceVariant = DarkMonochromeColors.surfaceSecondary,
    onSurfaceVariant = DarkMonochromeColors.textSecondary,
    surfaceContainerLowest = DarkMonochromeColors.background,
    surfaceContainerLow = DarkMonochromeColors.surface,
    surfaceContainer = DarkMonochromeColors.surface,
    surfaceContainerHigh = DarkMonochromeColors.surfaceSecondary,
    surfaceContainerHighest = DarkMonochromeColors.surfacePressed,
    outline = DarkMonochromeColors.border,
    outlineVariant = DarkMonochromeColors.borderStrong,
    primaryContainer = DarkMonochromeColors.surfaceSecondary,
    onPrimaryContainer = DarkMonochromeColors.textPrimary,
    secondaryContainer = DarkMonochromeColors.surfaceSecondary,
    onSecondaryContainer = DarkMonochromeColors.textPrimary,
    error = DarkMonochromeColors.textPrimary,
    onError = DarkMonochromeColors.controlSelectedContent
)

private val LightColorScheme = lightColorScheme(
    primary = LightMonochromeColors.controlSelected,
    onPrimary = LightMonochromeColors.controlSelectedContent,
    secondary = LightMonochromeColors.textSecondary,
    onSecondary = LightMonochromeColors.controlSelectedContent,
    tertiary = LightMonochromeColors.textTertiary,
    onTertiary = LightMonochromeColors.controlSelectedContent,
    background = LightMonochromeColors.background,
    onBackground = LightMonochromeColors.textPrimary,
    surface = LightMonochromeColors.surface,
    onSurface = LightMonochromeColors.textPrimary,
    surfaceVariant = LightMonochromeColors.surfaceSecondary,
    onSurfaceVariant = LightMonochromeColors.textSecondary,
    surfaceContainerLowest = LightMonochromeColors.background,
    surfaceContainerLow = LightMonochromeColors.surface,
    surfaceContainer = LightMonochromeColors.surface,
    surfaceContainerHigh = LightMonochromeColors.surfaceSecondary,
    surfaceContainerHighest = LightMonochromeColors.surfacePressed,
    outline = LightMonochromeColors.border,
    outlineVariant = LightMonochromeColors.borderStrong,
    primaryContainer = LightMonochromeColors.surfaceSecondary,
    onPrimaryContainer = LightMonochromeColors.textPrimary,
    secondaryContainer = LightMonochromeColors.surfaceSecondary,
    onSecondaryContainer = LightMonochromeColors.textPrimary,
    error = LightMonochromeColors.textPrimary,
    onError = LightMonochromeColors.controlSelectedContent
)

/**
 * Pure Monochrome Root Theme for AuraMusic.
 * Uses only the agreed Monochrome design system without colored accents or glass effects.
 * Supports System, Light, and Dark modes.
 */
@Composable
fun AuraMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val monochromeColors = if (darkTheme) DarkMonochromeColors else LightMonochromeColors

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalMonochromeColors provides monochromeColors,
        LocalAppColorScheme provides AppColorScheme.monochrome(darkTheme)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
