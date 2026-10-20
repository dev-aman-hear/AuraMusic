package com.aman.auramusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Unified semantic color scheme for player surfaces across the expanded player,
 * mini player, queue, lyrics, and dialogs.
 */
data class PlayerColorScheme(
    val isDynamic: Boolean,
    val isDark: Boolean,
    val ambientPrimary: Color,
    val ambientSecondary: Color,
    val ambientTertiary: Color,
    val ambientAccent: Color,
    val buttonSurface: Color,
    val buttonBorder: Color,
    val buttonIconTint: Color,
    val prominentSurface: Color,
    val prominentBorder: Color,
    val prominentIconTint: Color,
    val capsuleSurface: Color,
    val capsuleBorder: Color,
    val capsuleContent: Color,
    val progressTrack: Color,
    val progressIndicator: Color,
    val queueItemActiveSurface: Color,
    val queueItemActiveBorder: Color,
    val queueItemActiveContent: Color,
    val queueItemDraggingSurface: Color,
    val queueActionSurface: Color,
    val queueActionContent: Color,
    val queueActionActiveSurface: Color,
    val queueActionActiveContent: Color,
    val dragHandle: Color,
    val dialogSurface: Color,
    val dialogBorder: Color,
    val dialogAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val queueItemInactiveTitle: Color = textPrimary,
    val queueItemInactiveArtist: Color = textSecondary,
    val paletteMode: PlayerPaletteMode = if (isDynamic) PlayerPaletteMode.DYNAMIC_ARTWORK else PlayerPaletteMode.MONOCHROME
)

/**
 * Pure calculation function resolving the unified PlayerColorScheme.
 */
fun resolvePlayerColorScheme(
    isDynamic: Boolean,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    accent: Color,
    surfaceVariant: Color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE5E5EA),
    outline: Color = if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6),
    outlineVariant: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
    onSurface: Color = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
    onSurfaceVariant: Color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
    themePrimary: Color = if (isDark) Color.White else Color.Black,
    onPrimary: Color = if (isDark) Color.Black else Color.White,
    background: Color = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
    onBackground: Color = if (isDark) Color.White else Color(0xFF111111),
    paletteMode: PlayerPaletteMode = if (isDynamic) PlayerPaletteMode.DYNAMIC_ARTWORK else PlayerPaletteMode.MONOCHROME
): PlayerColorScheme {
    val fixedProminentSurface = if (isDark) Color.White else Color.Black
    val fixedProminentBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.20f)
    val fixedProminentIconTint = if (isDark) Color.Black else Color.White

    if (!isDynamic) {
        return PlayerColorScheme(
            isDynamic = false,
            isDark = isDark,
            ambientPrimary = background,
            ambientSecondary = background,
            ambientTertiary = background,
            ambientAccent = background,
            buttonSurface = surfaceVariant,
            buttonBorder = outline,
            buttonIconTint = onSurface,
            prominentSurface = fixedProminentSurface,
            prominentBorder = fixedProminentBorder,
            prominentIconTint = fixedProminentIconTint,
            capsuleSurface = surfaceVariant,
            capsuleBorder = outlineVariant,
            capsuleContent = onSurface,
            progressTrack = outline,
            progressIndicator = themePrimary,
            queueItemActiveSurface = surfaceVariant,
            queueItemActiveBorder = Color.Transparent,
            queueItemActiveContent = themePrimary,
            queueItemDraggingSurface = surfaceVariant.copy(alpha = 0.7f),
            queueActionSurface = Color.Transparent,
            queueActionContent = onBackground,
            queueActionActiveSurface = Color.Transparent,
            queueActionActiveContent = onBackground,
            dragHandle = onSurfaceVariant.copy(alpha = 0.40f),
            dialogSurface = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
            dialogBorder = outline,
            dialogAccent = themePrimary,
            textPrimary = onBackground,
            textSecondary = onSurfaceVariant,
            queueItemInactiveTitle = onBackground,
            queueItemInactiveArtist = onSurfaceVariant,
            paletteMode = PlayerPaletteMode.MONOCHROME
        )
    }

    val accentArgb = accent.toArgb()
    val accentLuminance = ColorHarmonizer.calculateLuminance(accentArgb)
    val prominentIcon = if (accentLuminance > 0.55f) Color(0xFF111111) else Color.White
    val highContrastText = if (isDark) Color.White else Color(0xFF111111)
    val secondaryText = if (isDark) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.68f)

    // Ensure accent for queue item active content has adequate contrast against the ambient surface
    val contrastSafeAccent = if (isDark) {
        if (accentLuminance < 0.38f) {
            val hsl = ColorHarmonizer.rgbToHsl(accentArgb)
            Color(ColorHarmonizer.hslToRgb(hsl[0], hsl[1].coerceAtLeast(0.40f), 0.58f))
        } else accent
    } else {
        if (accentLuminance > 0.65f) {
            val hsl = ColorHarmonizer.rgbToHsl(accentArgb)
            Color(ColorHarmonizer.hslToRgb(hsl[0], hsl[1].coerceAtLeast(0.40f), 0.40f))
        } else accent
    }

    // Subtle dynamic ambient tinting for inactive queue titles and artists
    val queueInactiveTitle = if (isDark) {
        Color(
            red = (Color.White.red * 0.85f + primary.red * 0.15f),
            green = (Color.White.green * 0.85f + primary.green * 0.15f),
            blue = (Color.White.blue * 0.85f + primary.blue * 0.15f),
            alpha = 0.95f
        )
    } else {
        Color(
            red = (0.08f * 0.85f + primary.red * 0.15f).coerceIn(0f, 0.22f),
            green = (0.08f * 0.85f + primary.green * 0.15f).coerceIn(0f, 0.22f),
            blue = (0.08f * 0.85f + primary.blue * 0.15f).coerceIn(0f, 0.22f),
            alpha = 1.0f
        )
    }
    val queueInactiveArtist = if (isDark) {
        queueInactiveTitle.copy(alpha = 0.70f)
    } else {
        queueInactiveTitle.copy(alpha = 0.65f)
    }

    return PlayerColorScheme(
        isDynamic = true,
        isDark = isDark,
        ambientPrimary = primary,
        ambientSecondary = secondary,
        ambientTertiary = tertiary,
        ambientAccent = accent,
        // ~75% opacity (roughly 25% transparency) for translucent button surfaces
        buttonSurface = secondary.copy(alpha = if (isDark) 0.75f else 0.72f),
        buttonBorder = primary.copy(alpha = if (isDark) 0.35f else 0.30f),
        buttonIconTint = if (isDark) Color.White else Color(0xFF181818),
        // Prominent Play/Pause button MUST remain strictly monochrome: black in light mode, white in dark mode
        prominentSurface = fixedProminentSurface,
        prominentBorder = fixedProminentBorder,
        prominentIconTint = fixedProminentIconTint,
        // Secondary capsules (Quality, Switcher pill)
        capsuleSurface = secondary.copy(alpha = 0.75f),
        capsuleBorder = primary.copy(alpha = 0.30f),
        capsuleContent = if (isDark) Color.White else Color(0xFF181818),
        // Scrubber / Progress
        progressTrack = secondary.copy(alpha = 0.40f),
        progressIndicator = accent,
        // Queue current track highlight & dragging
        queueItemActiveSurface = primary.copy(alpha = if (isDark) 0.28f else 0.22f),
        queueItemActiveBorder = primary.copy(alpha = if (isDark) 0.45f else 0.35f),
        queueItemActiveContent = contrastSafeAccent,
        queueItemDraggingSurface = secondary.copy(alpha = 0.65f),
        // Queue action pills (Clear, Save, Shuffle, Repeat)
        queueActionSurface = secondary.copy(alpha = 0.50f),
        queueActionContent = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f),
        queueActionActiveSurface = accent.copy(alpha = 0.75f),
        queueActionActiveContent = prominentIcon,
        // Drag handle
        dragHandle = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f),
        // Dialogs
        dialogSurface = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.95f) else Color(0xFFF2F2F7).copy(alpha = 0.95f),
        dialogBorder = primary.copy(alpha = 0.35f),
        dialogAccent = accent,
        textPrimary = highContrastText,
        textSecondary = secondaryText,
        queueItemInactiveTitle = queueInactiveTitle,
        queueItemInactiveArtist = queueInactiveArtist,
        paletteMode = paletteMode
    )
}

/**
 * Pure calculation overload resolving PlayerColorScheme directly from PlayerPaletteMode.
 */
fun resolvePlayerColorScheme(
    paletteMode: PlayerPaletteMode,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    accent: Color,
    surfaceVariant: Color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE5E5EA),
    outline: Color = if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6),
    outlineVariant: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
    onSurface: Color = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
    onSurfaceVariant: Color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
    themePrimary: Color = if (isDark) Color.White else Color.Black,
    onPrimary: Color = if (isDark) Color.Black else Color.White,
    background: Color = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
    onBackground: Color = if (isDark) Color.White else Color(0xFF111111)
): PlayerColorScheme = resolvePlayerColorScheme(
    isDynamic = paletteMode.isColored,
    isDark = isDark,
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    accent = accent,
    surfaceVariant = surfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    themePrimary = themePrimary,
    onPrimary = onPrimary,
    background = background,
    onBackground = onBackground,
    paletteMode = paletteMode
)
