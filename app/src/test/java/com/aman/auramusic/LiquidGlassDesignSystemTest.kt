package com.aman.auramusic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LiquidGlassConfig
import com.aman.auramusic.data.model.LiquidGlassPreset
import com.aman.auramusic.data.model.LiquidGlassTintOption
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.LiquidGlassTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassDesignSystemTest {

    @Test
    fun testAppSettingsDefaultsToLiquidGlassPreset() {
        val settings = AppSettings()
        assertNotNull(settings.liquidGlass)
        assertEquals(LiquidGlassPreset.LIQUID, settings.liquidGlass.preset)
        assertEquals(0.50f, settings.liquidGlass.intensity, 0.01f)
        assertEquals(24, settings.liquidGlass.blurRadius)
        assertEquals(0.75f, settings.liquidGlass.highlightStrength, 0.01f)
        assertEquals(14, settings.liquidGlass.shadowElevation)
        assertEquals(24, settings.liquidGlass.cornerRadius)
        assertFalse(settings.liquidGlass.reduceTransparency)
        assertFalse(settings.liquidGlass.reduceMotion)
    }

    @Test
    fun testPresetConfigurations() {
        val classic = LiquidGlassConfig.forPreset(LiquidGlassPreset.CLASSIC)
        assertEquals(LiquidGlassPreset.CLASSIC, classic.preset)
        assertEquals(0.65f, classic.intensity, 0.01f)
        assertEquals(14, classic.blurRadius)
        assertEquals(0.45f, classic.highlightStrength, 0.01f)

        val crystal = LiquidGlassConfig.forPreset(LiquidGlassPreset.CRYSTAL)
        assertEquals(LiquidGlassPreset.CRYSTAL, crystal.preset)
        assertEquals(0.30f, crystal.intensity, 0.01f)
        assertEquals(32, crystal.blurRadius)
        assertEquals(0.95f, crystal.highlightStrength, 0.01f)

        val frosted = LiquidGlassConfig.forPreset(LiquidGlassPreset.FROSTED)
        assertEquals(LiquidGlassPreset.FROSTED, frosted.preset)
        assertEquals(0.82f, frosted.intensity, 0.01f)
        assertEquals(18, frosted.blurRadius)
        assertEquals(0.35f, frosted.highlightStrength, 0.01f)
    }

    @Test
    fun testTokenResolutionAndSheenAlpha() {
        val config = LiquidGlassConfig(
            preset = LiquidGlassPreset.LIQUID,
            highlightStrength = 0.80f,
            shadowElevation = 16,
            cornerRadius = 24
        )
        val tokens = LiquidGlassTokens(config = config, isDark = true)

        assertEquals(24.dp, tokens.baseCornerRadius)
        assertEquals(24.dp, tokens.blurRadius)
        assertEquals(16.dp, tokens.elevation(GlassLevel.Regular))
        assertTrue(tokens.sheenAlpha(GlassLevel.Regular) > 0f)

        // Light theme should have higher sheen visibility
        val lightTokens = LiquidGlassTokens(config = config, isDark = false)
        assertTrue(lightTokens.sheenAlpha(GlassLevel.Regular) > tokens.sheenAlpha(GlassLevel.Regular))
    }

    @Test
    fun testAccessibilityReduceTransparency() {
        val config = LiquidGlassConfig(reduceTransparency = true, blurRadius = 30)
        val tokens = LiquidGlassTokens(config = config, isDark = true)

        // Blur radius must be zeroed when reduce transparency is active
        assertEquals(0.dp, tokens.blurRadius)
        // Sheen alpha must be zeroed
        assertEquals(0f, tokens.sheenAlpha(GlassLevel.Regular), 0.001f)
        // Solid elevation
        assertEquals(4.dp, tokens.elevation(GlassLevel.Regular))
    }

    @Test
    fun testAccessibilityReduceMotion() {
        val config = LiquidGlassConfig(reduceMotion = true)
        val tokens = LiquidGlassTokens(config = config, isDark = true)

        // Sheen reflections should be disabled to prevent visual motion
        assertEquals(0f, tokens.sheenAlpha(GlassLevel.Regular), 0.001f)
        assertEquals(0f, tokens.sheenAlpha(GlassLevel.Thick), 0.001f)
    }

    @Test
    fun testTintResolution() {
        val hexConfig = LiquidGlassConfig(tintHex = "#6366F1")
        val hexTokens = LiquidGlassTokens(config = hexConfig, isDark = true)
        assertEquals(Color(0xFF6366F1), hexTokens.effectiveTint)

        val accentConfig = LiquidGlassConfig(accentTintEnabled = true)
        val accentColor = Color(0xFFD81B43)
        val accentTokens = LiquidGlassTokens(config = accentConfig, accentColor = accentColor)
        assertEquals(accentColor, accentTokens.effectiveTint)

        val neutralConfig = LiquidGlassConfig(tintHex = "")
        val neutralTokens = LiquidGlassTokens(config = neutralConfig, isDark = true)
        assertEquals(Color(0xFF1E1A26), neutralTokens.effectiveTint)
    }

    @Test
    fun testTintPaletteOptions() {
        assertEquals(LiquidGlassTintOption.NEUTRAL, LiquidGlassTintOption.fromHex(""))
        assertEquals(LiquidGlassTintOption.OBSIDIAN, LiquidGlassTintOption.fromHex("#14121A"))
        assertEquals(LiquidGlassTintOption.CRIMSON, LiquidGlassTintOption.fromHex("#D81B43"))
        assertEquals(LiquidGlassTintOption.INDIGO, LiquidGlassTintOption.fromHex("#6366F1"))
    }
}
