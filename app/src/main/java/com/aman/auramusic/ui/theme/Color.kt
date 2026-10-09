package com.aman.auramusic.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Core Aura Brand Colors ---
val AuraPrimary = Color(0xFFFF2D55)        // Signature Electric Rose / Coral
val AuraPrimaryVariant = Color(0xFFFF375F) // Luminous Crimson
val AuraSecondary = Color(0xFFFF6B8A)      // Soft Rose
val AuraTertiary = Color(0xFF7A3E4C)       // Muted wine accent
val AuraCyan = Color(0xFF00E5FF)           // Streaming Hi-Fi Cyan
val AuraEmerald = Color(0xFF10B981)        // Downloaded / Offline Emerald

// --- Dark Theme (Obsidian Elegance) ---
val AuraDarkBackground = Color(0xFF0B0A0C)        // Deep warm black
val AuraDarkSurface = Color(0xFF161316)           // Surface base
val AuraDarkSurfaceVariant = Color(0xFF211A1F)    // Secondary surface
val AuraDarkSurfaceElevated = Color(0xFF2A2026)   // Elevated / active surface
val AuraDarkSurfaceGlass = Color(0xD9161316)      // Translucent glass surface
val AuraDarkBorder = Color(0x1AFFFFFF)            // 10% Translucent Border
val AuraDarkBorderGlow = Color(0x33FFFFFF)        // 20% Border Highlight

val AuraTextPrimaryDark = Color(0xFFFFFFFF)
val AuraTextSecondaryDark = Color(0xB8FFFFFF)     // 72% opacity
val AuraTextTertiaryDark = Color(0x66FFFFFF)      // 40% opacity

// --- Light Theme (Clean Crisp Glass) ---
val AuraLightBackground = Color(0xFFFFFAFB)
val AuraLightSurface = Color(0xFFFFFFFF)
val AuraLightSurfaceVariant = Color(0xFFF4EEF1)
val AuraLightSurfaceElevated = Color(0xFFFFEEF2)
val AuraLightSurfaceGlass = Color(0xE6FFFFFF)
val AuraLightBorder = Color(0x14000000)

val AuraTextPrimaryLight = Color(0xFF0F1017)
val AuraTextSecondaryLight = Color(0xFF6B6E7E)
val AuraTextTertiaryLight = Color(0xFFA0A3B1)

// --- Gradient Presets ---
val AuraGlassBorderDark = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
)

val AuraGlassBorderLight = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.03f))
)

val AuraHeroGradientDark = Brush.linearGradient(
    listOf(Color(0xFF3A161E), Color(0xFF8C263B), Color(0xFF151012))
)
