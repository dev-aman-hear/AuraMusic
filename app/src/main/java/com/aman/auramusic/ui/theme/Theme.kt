package com.aman.auramusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.aman.auramusic.data.model.LiquidGlassConfig
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
    outline = DarkMonochromeColors.border,
    outlineVariant = DarkMonochromeColors.borderStrong,
    primaryContainer = DarkMonochromeColors.surfaceSecondary,
    onPrimaryContainer = DarkMonochromeColors.textPrimary,
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
    outline = LightMonochromeColors.border,
    outlineVariant = LightMonochromeColors.borderStrong,
    primaryContainer = LightMonochromeColors.surfaceSecondary,
    onPrimaryContainer = LightMonochromeColors.textPrimary,
    error = LightMonochromeColors.textPrimary,
    onError = LightMonochromeColors.controlSelectedContent
)

/**
 * Liquid Glass Theme wrapper providing dynamic Liquid Glass tokens.
 * Kept intact for future reuse and configuration model compatibility.
 */
@Composable
fun LiquidGlassTheme(
    config: LiquidGlassConfig = LiquidGlassConfig(),
    isDark: Boolean = LocalIsDark.current,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = remember(config, isDark, isAmoled) {
        LiquidGlassTokens(
            config = config,
            isDark = isDark,
            isAmoled = isAmoled,
            accentColor = if (isDark) DarkMonochromeColors.controlSelected else LightMonochromeColors.controlSelected
        )
    }
    CompositionLocalProvider(
        LocalLiquidGlassTokens provides tokens,
        content = content
    )
}

/**
 * Pure Monochrome Root Theme for AuraMusic.
 * Dynamic wallpaper colors are explicitly disabled in this monochrome-only phase.
 * System, Light, and Dark modes are fully supported.
 */
@Composable
fun AuraMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false, // Effective default false; dynamic wallpaper disabled
    amoledMode: Boolean = false,
    liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Fixed monochrome color schemes (wallpaper/dynamic colors never override in this phase)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val monochromeColors = if (darkTheme) DarkMonochromeColors else LightMonochromeColors

    val tokens = remember(liquidGlassConfig, darkTheme) {
        LiquidGlassTokens(
            config = liquidGlassConfig,
            isDark = darkTheme,
            isAmoled = false,
            accentColor = monochromeColors.controlSelected
        )
    }

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalMonochromeColors provides monochromeColors,
        LocalLiquidGlassTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
