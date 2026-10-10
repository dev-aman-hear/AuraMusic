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
import androidx.compose.runtime.remember
import com.aman.auramusic.data.model.LiquidGlassConfig
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

/**
 * Liquid Glass Theme wrapper providing dynamic Liquid Glass tokens.
 */
@Composable
fun LiquidGlassTheme(
    config: LiquidGlassConfig = LiquidGlassConfig(),
    isDark: Boolean = LocalIsDark.current,
    isAmoled: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val tokens = remember(config, isDark, isAmoled, accentColor) {
        LiquidGlassTokens(
            config = config,
            isDark = isDark,
            isAmoled = isAmoled,
            accentColor = accentColor
        )
    }
    CompositionLocalProvider(
        LocalLiquidGlassTokens provides tokens,
        content = content
    )
}

@Composable
fun AuraMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
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

    val tokens = remember(liquidGlassConfig, darkTheme, amoledMode, colorScheme.primary) {
        LiquidGlassTokens(
            config = liquidGlassConfig,
            isDark = darkTheme,
            isAmoled = amoledMode && darkTheme,
            accentColor = colorScheme.primary
        )
    }

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalLiquidGlassTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
