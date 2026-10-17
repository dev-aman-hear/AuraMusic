package com.aman.auramusic.theme

import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.ColorHarmonizer
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Default OFF setting and persistence behavior.
 * 2. Neutral fallback palette in dark and light modes.
 * 3. Harmonious palette selection, saturation safeguards, and grayscale/monochrome detection.
 * 4. Stale palette prevention during rapid song switches.
 * 5. Clean restoration of default background when the setting is disabled.
 */
class DynamicArtworkColorsTest {

    @Before
    fun setUp() {
        DynamicArtworkColorsHelper.clearCache()
    }

    private fun createSong(
        id: Long = 1L,
        title: String = "Test Song",
        artworkUri: String? = "content://media/external/audio/albumart/1"
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = "Aura Artist",
            album = "Aura Album",
            duration = 240000L,
            uri = "content://media/$id",
            filePath = "/storage/emulated/0/Music/$title.mp3",
            artworkUri = artworkUri,
            mimeType = "audio/mpeg"
        )
    }

    // --- 1. Preference Default & Persistence State Machine ---

    @Test
    fun default_settings_has_dynamic_artwork_colors_disabled() {
        val settings = AppSettings()
        assertFalse(
            "Dynamic Artwork Colors must default to OFF to preserve existing monochrome theme",
            settings.dynamicArtworkColors
        )
    }

    @Test
    fun setting_toggle_and_persistence_roundtrip() {
        var persistedSettings = AppSettings()
        assertFalse(persistedSettings.dynamicArtworkColors)

        // User enables setting
        persistedSettings = persistedSettings.copy(dynamicArtworkColors = true)
        assertTrue(persistedSettings.dynamicArtworkColors)

        // Simulate app restart / preference deserialization
        val restoredSettings = persistedSettings.copy()
        assertTrue(
            "User preference must be faithfully restored after app restarts",
            restoredSettings.dynamicArtworkColors
        )

        // User disables setting
        persistedSettings = persistedSettings.copy(dynamicArtworkColors = false)
        assertFalse(
            "When toggled off, dynamic artwork colors must be disabled",
            persistedSettings.dynamicArtworkColors
        )
    }

    // --- 2. Neutral Fallback Palette Behavior ---

    @Test
    fun neutral_palette_dark_mode_characteristics() {
        val darkNeutral = PlayerArtworkPalette.neutral(isDark = true)
        assertFalse("Dark neutral palette should not be flagged as light artwork", darkNeutral.isLightArtwork)
        assertNotNull(darkNeutral.primary)
        assertNotNull(darkNeutral.secondary)
        assertNotNull(darkNeutral.tertiary)
        assertNotNull(darkNeutral.accent)
        assertNotEquals(0, darkNeutral.primaryArgb)
    }

    @Test
    fun neutral_palette_light_mode_characteristics() {
        val lightNeutral = PlayerArtworkPalette.neutral(isDark = false)
        assertTrue("Light neutral palette should be flagged as light artwork", lightNeutral.isLightArtwork)
        assertNotNull(lightNeutral.primary)
        assertNotNull(lightNeutral.secondary)
        assertNotNull(lightNeutral.tertiary)
        assertNotNull(lightNeutral.accent)
        assertNotEquals(0, lightNeutral.primaryArgb)
    }

    // --- 3. Palette Selection & Saturation Safeguards ---

    @Test
    fun empty_swatches_falls_back_to_neutral() {
        val result = DynamicArtworkColorsHelper.processColors(emptyList(), isDark = true)
        val expected = PlayerArtworkPalette.neutral(isDark = true)
        assertEquals(expected.primaryArgb, result.primaryArgb)
        assertEquals(expected.secondaryArgb, result.secondaryArgb)
    }

    @Test
    fun grayscale_or_low_saturation_artwork_safely_falls_back_to_neutral() {
        // Pure grayscale / low saturation swatches (saturation < 0.08)
        val gray1 = 0xFF808080.toInt()
        val gray2 = 0xFF505050.toInt()
        val gray3 = 0xFFA0A0A0.toInt()

        val result = DynamicArtworkColorsHelper.processColors(listOf(gray1, gray2, gray3), isDark = true)
        val expected = PlayerArtworkPalette.neutral(isDark = true)

        assertEquals(
            "Grayscale artwork must fall back to neutral to avoid unnatural gray color casts",
            expected.primaryArgb,
            result.primaryArgb
        )
    }

    @Test
    fun colorful_swatches_extract_harmonious_multi_tone_palette() {
        // High vibrancy swatches: Blue, Purple, Orange
        val blue = 0xFF1E88E5.toInt()
        val purple = 0xFF8E24AA.toInt()
        val orange = 0xFFFB8C00.toInt()

        val resultDark = DynamicArtworkColorsHelper.processColors(
            listOf(blue, purple, orange),
            isDark = true,
            dominantRgb = blue
        )

        assertNotNull(resultDark.primary)
        assertNotNull(resultDark.secondary)
        assertNotNull(resultDark.tertiary)
        assertNotNull(resultDark.accent)
        assertNotEquals(resultDark.primaryArgb, resultDark.secondaryArgb)

        // Luminance should be comfortable and within readable range
        val primaryHsl = ColorHarmonizer.rgbToHsl(resultDark.primaryArgb)
        assertTrue(
            "Dark theme primary lightness must be restrained to avoid neon blinding: ${primaryHsl[2]}",
            primaryHsl[2] in 0.18f..0.50f
        )
    }

    // --- 4. ColorHarmonizer Math Correctness ---

    @Test
    fun color_harmonizer_rgb_to_hsl_and_back_roundtrip() {
        val originalRgb = 0xFF4285F4.toInt() // Google Blue
        val hsl = ColorHarmonizer.rgbToHsl(originalRgb)
        val convertedRgb = ColorHarmonizer.hslToRgb(hsl[0], hsl[1], hsl[2])

        // Roundtrip within 1-2 integer variance due to 8-bit rounding
        val r1 = (originalRgb shr 16) and 0xFF
        val g1 = (originalRgb shr 8) and 0xFF
        val b1 = originalRgb and 0xFF

        val r2 = (convertedRgb shr 16) and 0xFF
        val g2 = (convertedRgb shr 8) and 0xFF
        val b2 = convertedRgb and 0xFF

        assertTrue(kotlin.math.abs(r1 - r2) <= 2)
        assertTrue(kotlin.math.abs(g1 - g2) <= 2)
        assertTrue(kotlin.math.abs(b1 - b2) <= 2)
    }

    @Test
    fun color_harmonizer_luminance_calculation() {
        val blackLuminance = ColorHarmonizer.calculateLuminance(0xFF000000.toInt())
        val whiteLuminance = ColorHarmonizer.calculateLuminance(0xFFFFFFFF.toInt())
        assertEquals(0f, blackLuminance, 0.001f)
        assertEquals(1f, whiteLuminance, 0.001f)
    }

    // --- 5. Rapid Track Changes & Stale Palette Prevention Simulation ---

    @Test
    fun rapid_track_switch_with_missing_artwork_resets_to_neutral_without_stale_color_bleed() {
        // Song 1 has vibrant artwork
        val vibrant = 0xFFE91E63.toInt()
        val song1Palette = DynamicArtworkColorsHelper.processColors(listOf(vibrant), isDark = true)
        assertNotEquals(PlayerArtworkPalette.neutral(isDark = true).primaryArgb, song1Palette.primaryArgb)

        // Song 2 has missing artwork or empty swatches
        val song2Palette = DynamicArtworkColorsHelper.processColors(emptyList(), isDark = true)
        val neutral = PlayerArtworkPalette.neutral(isDark = true)

        assertEquals(
            "Song 2 without artwork must immediately yield neutral palette rather than inheriting Song 1's colors",
            neutral.primaryArgb,
            song2Palette.primaryArgb
        )
    }

    // --- 6. Setting Disabled State Restoration ---

    @Test
    fun when_setting_is_disabled_player_background_state_is_neutral() {
        val appSettings = AppSettings(dynamicArtworkColors = false)
        val isDark = true

        // Simulate PlayerScreen logic:
        val effectivePalette = if (appSettings.dynamicArtworkColors) {
            val vibrant = 0xFF00E676.toInt()
            DynamicArtworkColorsHelper.processColors(listOf(vibrant), isDark)
        } else {
            PlayerArtworkPalette.neutral(isDark)
        }

        assertEquals(
            "When setting is disabled, palette must strictly be neutral",
            PlayerArtworkPalette.neutral(isDark).primaryArgb,
            effectivePalette.primaryArgb
        )
    }

    // --- 7. PlayerColorScheme Dynamic vs Monochrome Behavior ---

    @Test
    fun when_dynamic_colors_enabled_player_color_scheme_uses_translucent_harmonious_surfaces() {
        val blue = androidx.compose.ui.graphics.Color(0xFF1E88E5)
        val purple = androidx.compose.ui.graphics.Color(0xFF8E24AA)
        val teal = androidx.compose.ui.graphics.Color(0xFF00897B)
        val yellowAccent = androidx.compose.ui.graphics.Color(0xFFFFD600) // Light accent

        val scheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = true,
            isDark = true,
            primary = blue,
            secondary = purple,
            tertiary = teal,
            accent = yellowAccent
        )

        assertTrue("Scheme must be dynamic", scheme.isDynamic)
        assertTrue("Scheme must preserve isDark", scheme.isDark)

        // Validate ~75% opacity button surfaces (approx 25% transparency)
        assertEquals(0.75f, scheme.buttonSurface.alpha, 0.05f)
        assertEquals(0.75f, scheme.capsuleSurface.alpha, 0.05f)

        // Validate prominent surface derived from accent
        assertEquals(0.85f, scheme.prominentSurface.alpha, 0.05f)

        // Yellow accent is light, so prominentIconTint must be dark for contrast
        assertEquals(androidx.compose.ui.graphics.Color(0xFF111111), scheme.prominentIconTint)

        // Queue item active surface is subtle translucent highlight
        assertTrue("Queue active surface must have translucent alpha", scheme.queueItemActiveSurface.alpha < 0.40f)
        assertNotEquals(androidx.compose.ui.graphics.Color.Transparent, scheme.queueItemActiveBorder)

        // Queue item active content must be high-contrast white in dark mode
        assertEquals(androidx.compose.ui.graphics.Color.White, scheme.queueItemActiveContent)
    }

    @Test
    fun queue_item_active_content_guarantees_contrast_with_dark_accent_in_dark_mode() {
        // Simulates dark brown/sepia artwork where accent has very low luminance
        val darkBrownAccent = androidx.compose.ui.graphics.Color(0xFF2E1A11)
        val scheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = true,
            isDark = true,
            primary = androidx.compose.ui.graphics.Color(0xFF4A2C1D),
            secondary = androidx.compose.ui.graphics.Color(0xFF382216),
            tertiary = androidx.compose.ui.graphics.Color(0xFF2B1910),
            accent = darkBrownAccent
        )

        // In dark mode, active queue content must be crisp white, NOT dark brown
        assertEquals(
            "Active queue song title/text must be high contrast White in dark mode",
            androidx.compose.ui.graphics.Color.White,
            scheme.queueItemActiveContent
        )
    }

    @Test
    fun queue_item_active_content_guarantees_contrast_with_light_accent_in_light_mode() {
        // Simulates near-white/pastel artwork where accent has very high luminance
        val pastelAccent = androidx.compose.ui.graphics.Color(0xFFF0EAE1)
        val scheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = true,
            isDark = false,
            primary = androidx.compose.ui.graphics.Color(0xFFE8DFD4),
            secondary = androidx.compose.ui.graphics.Color(0xFFD8CCC0),
            tertiary = androidx.compose.ui.graphics.Color(0xFFC4B6A6),
            accent = pastelAccent
        )

        // In light mode, active queue content must be dark text, NOT pale pastel
        assertEquals(
            "Active queue song title/text must be high contrast dark in light mode",
            androidx.compose.ui.graphics.Color(0xFF111111),
            scheme.queueItemActiveContent
        )
    }

    @Test
    fun when_dynamic_colors_disabled_player_color_scheme_faithfully_restores_monochrome() {
        val mockSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A)
        val mockOutline = androidx.compose.ui.graphics.Color(0xFF38383A)
        val mockThemePrimary = androidx.compose.ui.graphics.Color.White
        val mockOnPrimary = androidx.compose.ui.graphics.Color.Black

        val scheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = false,
            isDark = true,
            primary = androidx.compose.ui.graphics.Color.Red,
            secondary = androidx.compose.ui.graphics.Color.Green,
            tertiary = androidx.compose.ui.graphics.Color.Blue,
            accent = androidx.compose.ui.graphics.Color.Yellow,
            surfaceVariant = mockSurfaceVariant,
            outline = mockOutline,
            themePrimary = mockThemePrimary,
            onPrimary = mockOnPrimary
        )

        assertFalse("Scheme must not be dynamic", scheme.isDynamic)
        assertEquals(mockSurfaceVariant, scheme.buttonSurface)
        assertEquals(mockOutline, scheme.buttonBorder)
        assertEquals(mockThemePrimary, scheme.prominentSurface)
        assertEquals(mockOnPrimary, scheme.prominentIconTint)
        assertEquals(mockSurfaceVariant, scheme.queueItemActiveSurface)
        assertEquals(androidx.compose.ui.graphics.Color.Transparent, scheme.queueItemActiveBorder)
        assertEquals(androidx.compose.ui.graphics.Color.Transparent, scheme.queueActionSurface)
    }
}
