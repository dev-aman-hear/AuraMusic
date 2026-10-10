package com.aman.auramusic.data.model

/**
 * Presets for AuraMusic Liquid Glass Design System.
 */
enum class LiquidGlassPreset(val displayName: String, val description: String) {
    CLASSIC("Classic", "Subtle, restrained translucency"),
    LIQUID("Liquid Glass", "Signature layered depth, soft specular highlights"),
    CRYSTAL("Crystal", "High clarity surfaces with crisp illuminated rim"),
    FROSTED("Frosted", "Soft matte diffusion and velvety texture"),
    CUSTOM("Custom", "Fine-tuned personalized glass properties");

    companion object {
        fun fromName(name: String?): LiquidGlassPreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIQUID
        }
    }
}

/**
 * Curated surface tint palette options for Liquid Glass surfaces.
 */
enum class LiquidGlassTintOption(val id: String, val displayName: String, val hexColor: String) {
    NEUTRAL("neutral", "Pure Glass", ""),
    OBSIDIAN("obsidian", "Obsidian", "#14121A"),
    MIDNIGHT("midnight", "Midnight", "#0F172A"),
    CRIMSON("crimson", "Aura Rose", "#D81B43"),
    INDIGO("indigo", "Cosmic Indigo", "#6366F1"),
    EMERALD("emerald", "Emerald", "#059669"),
    AMBER("amber", "Warm Amber", "#D97706");

    companion object {
        fun fromHex(hex: String): LiquidGlassTintOption {
            return entries.firstOrNull { it.hexColor.equals(hex, ignoreCase = true) } ?: NEUTRAL
        }
    }
}

/**
 * Complete typed configuration model for the Liquid Glass visual engine.
 */
data class LiquidGlassConfig(
    val preset: LiquidGlassPreset = LiquidGlassPreset.LIQUID,
    val intensity: Float = 0.50f, // 0.10f to 0.90f
    val blurRadius: Int = 24, // 0 to 50 dp
    val highlightStrength: Float = 0.75f, // 0.0f to 1.0f
    val shadowElevation: Int = 14, // 0 to 24 dp
    val cornerRadius: Int = 24, // 8 to 36 dp
    val tintHex: String = "",
    val accentTintEnabled: Boolean = false,
    val reduceTransparency: Boolean = false,
    val reduceMotion: Boolean = false
) {
    companion object {
        fun forPreset(preset: LiquidGlassPreset): LiquidGlassConfig {
            return when (preset) {
                LiquidGlassPreset.CLASSIC -> LiquidGlassConfig(
                    preset = LiquidGlassPreset.CLASSIC,
                    intensity = 0.65f,
                    blurRadius = 14,
                    highlightStrength = 0.45f,
                    shadowElevation = 8,
                    cornerRadius = 20,
                    tintHex = "",
                    accentTintEnabled = false
                )
                LiquidGlassPreset.LIQUID -> LiquidGlassConfig(
                    preset = LiquidGlassPreset.LIQUID,
                    intensity = 0.50f,
                    blurRadius = 24,
                    highlightStrength = 0.75f,
                    shadowElevation = 14,
                    cornerRadius = 24,
                    tintHex = "",
                    accentTintEnabled = false
                )
                LiquidGlassPreset.CRYSTAL -> LiquidGlassConfig(
                    preset = LiquidGlassPreset.CRYSTAL,
                    intensity = 0.30f,
                    blurRadius = 32,
                    highlightStrength = 0.95f,
                    shadowElevation = 18,
                    cornerRadius = 28,
                    tintHex = "",
                    accentTintEnabled = false
                )
                LiquidGlassPreset.FROSTED -> LiquidGlassConfig(
                    preset = LiquidGlassPreset.FROSTED,
                    intensity = 0.82f,
                    blurRadius = 18,
                    highlightStrength = 0.35f,
                    shadowElevation = 10,
                    cornerRadius = 22,
                    tintHex = "",
                    accentTintEnabled = false
                )
                LiquidGlassPreset.CUSTOM -> LiquidGlassConfig(
                    preset = LiquidGlassPreset.CUSTOM,
                    intensity = 0.55f,
                    blurRadius = 24,
                    highlightStrength = 0.70f,
                    shadowElevation = 14,
                    cornerRadius = 24
                )
            }
        }
    }
}
