package com.aman.auramusic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pure Monochrome Semantic Colour System for AuraMusic.
 * All screen elements and controls bind strictly to this palette.
 */
data class MonochromeColors(
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfacePressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val borderStrong: Color,
    val controlBackground: Color,
    val controlSelected: Color,
    val controlSelectedContent: Color
)

val LightMonochromeColors = MonochromeColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    surfaceSecondary = Color(0xFFEEEEEE),
    surfacePressed = Color(0xFFE5E5E5),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF666666),
    textTertiary = Color(0xFF888888),
    border = Color(0xFFD9D9D9),
    borderStrong = Color(0xFFBFBFBF),
    controlBackground = Color(0xFFEEEEEE),
    controlSelected = Color(0xFF000000),
    controlSelectedContent = Color(0xFFFFFFFF)
)

val DarkMonochromeColors = MonochromeColors(
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    surfaceSecondary = Color(0xFF1A1A1A),
    surfacePressed = Color(0xFF262626),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    textTertiary = Color(0xFF808080),
    border = Color(0xFF333333),
    borderStrong = Color(0xFF555555),
    controlBackground = Color(0xFF1A1A1A),
    controlSelected = Color(0xFFFFFFFF),
    controlSelectedContent = Color(0xFF000000)
)

val LocalMonochromeColors = staticCompositionLocalOf { LightMonochromeColors }

/**
 * Direct accessor for current active monochrome tokens.
 */
val Monochrome: MonochromeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMonochromeColors.current

// --- Semantic aliases mapped strictly to pure monochrome palette ---
val AppleMusicAccent = Color(0xFF000000)
val AuraPrimary = Color(0xFF000000)
val AuraPrimaryVariant = Color(0xFF262626)
val AuraSecondary = Color(0xFF666666)
val AuraTertiary = Color(0xFF888888)

// --- Dark Theme constants ---
val AuraDarkBackground = Color(0xFF000000)
val AuraDarkSurface = Color(0xFF111111)
val AuraDarkSurfaceVariant = Color(0xFF1A1A1A)
val AuraDarkSurfaceElevated = Color(0xFF262626)
val AuraDarkBorder = Color(0xFF333333)
val AuraDarkBorderGlow = Color(0xFF555555)

val AuraTextPrimaryDark = Color(0xFFFFFFFF)
val AuraTextSecondaryDark = Color(0xFFAAAAAA)
val AuraTextTertiaryDark = Color(0xFF808080)

// --- Light Theme constants ---
val AuraLightBackground = Color(0xFFFFFFFF)
val AuraLightSurface = Color(0xFFF5F5F5)
val AuraLightSurfaceVariant = Color(0xFFEEEEEE)
val AuraLightSurfaceElevated = Color(0xFFE5E5E5)
val AuraLightBorder = Color(0xFFD9D9D9)

val AuraTextPrimaryLight = Color(0xFF000000)
val AuraTextSecondaryLight = Color(0xFF666666)
val AuraTextTertiaryLight = Color(0xFF888888)
