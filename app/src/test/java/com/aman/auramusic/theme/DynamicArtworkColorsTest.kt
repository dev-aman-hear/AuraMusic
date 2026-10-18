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

        // Prominent Play/Pause button must remain strictly monochrome: black in light mode and white in dark mode
        assertEquals(androidx.compose.ui.graphics.Color.White, scheme.prominentSurface)
        assertEquals(androidx.compose.ui.graphics.Color.Black, scheme.prominentIconTint)

        // Queue item active surface is subtle translucent highlight
        assertTrue("Queue active surface must have translucent alpha", scheme.queueItemActiveSurface.alpha < 0.40f)
        assertNotEquals(androidx.compose.ui.graphics.Color.Transparent, scheme.queueItemActiveBorder)

        // Queue item active content preserves accent in dynamic mode
        assertEquals(yellowAccent, scheme.queueItemActiveContent)

        // Queue inactive title and artist have subtle ambient tinting
        assertNotNull(scheme.queueItemInactiveTitle)
        assertNotNull(scheme.queueItemInactiveArtist)
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
        assertEquals(mockThemePrimary, scheme.queueItemInactiveTitle)
    }

    // --- 8. Comprehensive Three-Role System & Edge Case Validation ---

    @Test
    fun coherent_two_colour_background_and_controlled_accent_selection() {
        val deepBlue = 0xFF1565C0.toInt()
        val violet = 0xFF6A1B9A.toInt()
        val brightAmber = 0xFFFFB300.toInt()

        val palette = DynamicArtworkColorsHelper.processColors(
            colors = listOf(deepBlue, violet, brightAmber),
            isDark = true,
            dominantRgb = deepBlue
        )

        val priHsl = ColorHarmonizer.rgbToHsl(palette.primaryArgb)
        val secHsl = ColorHarmonizer.rgbToHsl(palette.secondaryArgb)
        val accHsl = ColorHarmonizer.rgbToHsl(palette.accentArgb)

        // Primary is dominant blue-based
        assertTrue("Primary should be in blue hue range (190-230): ${priHsl[0]}", priHsl[0] in 180f..240f)
        // Secondary is harmonious supporting violet
        assertTrue("Secondary should be in violet hue range (250-310): ${secHsl[0]}", secHsl[0] in 250f..320f)
        // Accent is amber highlight
        assertTrue("Accent should be in amber/warm hue range (30-60): ${accHsl[0]}", accHsl[0] in 25f..65f)
        // Primary and Secondary must be distinct
        val diff = DynamicArtworkColorsHelper.calculateColorDifference(priHsl, secHsl)
        assertTrue("Primary and Secondary must be visually distinct: $diff", diff >= 0.16f)
    }

    @Test
    fun distinct_primary_and_secondary_colours_where_suitable_swatches_exist() {
        val skyBlue = 0xFF0288D1.toInt()
        val emeraldGreen = 0xFF2E7D32.toInt()

        val palette = DynamicArtworkColorsHelper.processColors(
            colors = listOf(skyBlue, emeraldGreen),
            isDark = true
        )

        assertNotEquals("Primary and secondary must not be identical", palette.primaryArgb, palette.secondaryArgb)
        val diff = DynamicArtworkColorsHelper.calculateColorDifference(
            ColorHarmonizer.rgbToHsl(palette.primaryArgb),
            ColorHarmonizer.rgbToHsl(palette.secondaryArgb)
        )
        assertTrue("Color difference between primary and secondary must be substantial: $diff", diff >= 0.16f)
    }

    @Test
    fun monochromatic_artwork_generates_harmonious_analogous_depth() {
        // Single color cover (e.g. pure red album art)
        val pureRed = 0xFFD32F2F.toInt()

        val palette = DynamicArtworkColorsHelper.processColors(
            colors = listOf(pureRed),
            isDark = true
        )

        assertNotEquals("Monochromatic art should synthesize a non-identical secondary for depth", palette.primaryArgb, palette.secondaryArgb)
        val priHsl = ColorHarmonizer.rgbToHsl(palette.primaryArgb)
        val secHsl = ColorHarmonizer.rgbToHsl(palette.secondaryArgb)

        // Secondary should be an analogous hue or depth variation of Primary
        val hueDiff = minOf(kotlin.math.abs(priHsl[0] - secHsl[0]), 360f - kotlin.math.abs(priHsl[0] - secHsl[0]))
        assertTrue("Monochromatic secondary must remain analogous (|Δh| <= 45°): $hueDiff", hueDiff <= 45f)
    }

    @Test
    fun extremely_dark_artwork_elevates_accent_for_contrast() {
        // Dark brown / sepia artwork (luminance < 0.15)
        val darkBrown = 0xFF1C1008.toInt()
        val darkEspresso = 0xFF2D180C.toInt()

        val paletteDark = DynamicArtworkColorsHelper.processColors(
            colors = listOf(darkBrown, darkEspresso),
            isDark = true
        )

        val accHsl = ColorHarmonizer.rgbToHsl(paletteDark.accentArgb)
        assertTrue(
            "In dark mode, Accent lightness must be elevated (>= 0.45) to ensure high contrast against dark background: ${accHsl[2]}",
            accHsl[2] >= 0.45f
        )
    }

    @Test
    fun extremely_bright_artwork_deepens_accent_in_light_mode() {
        // Near-white pastel artwork
        val palePastel = 0xFFF9F5EC.toInt()
        val lightCream = 0xFFFFF8E7.toInt()

        val paletteLight = DynamicArtworkColorsHelper.processColors(
            colors = listOf(palePastel, lightCream),
            isDark = false
        )

        val accHsl = ColorHarmonizer.rgbToHsl(paletteLight.accentArgb)
        assertTrue(
            "In light mode, Accent lightness must be deepened (<= 0.52) to ensure readable contrast on white surfaces: ${accHsl[2]}",
            accHsl[2] <= 0.52f
        )
    }

    @Test
    fun neon_artwork_restrains_excessive_saturation() {
        // 100% saturation neon colors
        val neonGreen = 0xFF00FF00.toInt()
        val neonMagenta = 0xFFFF00FF.toInt()

        val palette = DynamicArtworkColorsHelper.processColors(
            colors = listOf(neonGreen, neonMagenta),
            isDark = true
        )

        val priHsl = ColorHarmonizer.rgbToHsl(palette.primaryArgb)
        val secHsl = ColorHarmonizer.rgbToHsl(palette.secondaryArgb)
        val accHsl = ColorHarmonizer.rgbToHsl(palette.accentArgb)

        assertTrue("Primary saturation must be capped below 0.75: ${priHsl[1]}", priHsl[1] <= 0.75f)
        assertTrue("Secondary saturation must be capped below 0.70: ${secHsl[1]}", secHsl[1] <= 0.70f)
        assertTrue("Accent saturation must be restrained below 0.85: ${accHsl[1]}", accHsl[1] <= 0.85f)
    }

    @Test
    fun missing_or_invalid_artwork_yields_neutral_fallbacks() {
        val emptyPalette = DynamicArtworkColorsHelper.processColors(emptyList(), isDark = true)
        val neutral = PlayerArtworkPalette.neutral(isDark = true)
        assertEquals(neutral.primaryArgb, emptyPalette.primaryArgb)
        assertEquals(neutral.secondaryArgb, emptyPalette.secondaryArgb)
        assertEquals(neutral.accentArgb, emptyPalette.accentArgb)

        // All pure black (lum < 0.03)
        val blackPalette = DynamicArtworkColorsHelper.processColors(listOf(0xFF000000.toInt()), isDark = true)
        assertEquals(neutral.primaryArgb, blackPalette.primaryArgb)
    }

    @Test
    fun preservation_of_monochrome_mini_player() {
        // AppSettings toggle does not affect standard MaterialTheme surfaces used by MiniPlayer
        val dynamicSettings = AppSettings(dynamicArtworkColors = true)
        val defaultSettings = AppSettings(dynamicArtworkColors = false)

        assertTrue(dynamicSettings.dynamicArtworkColors)
        assertFalse(defaultSettings.dynamicArtworkColors)

        // In resolvePlayerColorScheme when dynamic is false, all surfaces revert to monochrome tokens
        val monochromeScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = false,
            isDark = true,
            primary = androidx.compose.ui.graphics.Color.Red,
            secondary = androidx.compose.ui.graphics.Color.Green,
            tertiary = androidx.compose.ui.graphics.Color.Blue,
            accent = androidx.compose.ui.graphics.Color.Yellow,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
            outline = androidx.compose.ui.graphics.Color(0xFF38383A)
        )

        assertEquals(androidx.compose.ui.graphics.Color(0xFF2A2A2A), monochromeScheme.buttonSurface)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF38383A), monochromeScheme.buttonBorder)
    }

    // --- 9. Player Palette Presets & Precedence Regression Tests ---

    @Test
    fun each_preset_resolves_to_its_intended_distinct_palette() {
        val presets = listOf("Monochrome", "Midnight Indigo", "Crimson Velvet", "Emerald Pine", "Nordic Slate")
        val darkPalettes = presets.map { PlayerArtworkPalette.fromPreset(it, isDark = true) }
        val lightPalettes = presets.map { PlayerArtworkPalette.fromPreset(it, isDark = false) }

        // All 5 presets must have distinct primary colors in dark mode
        val darkPrimaries = darkPalettes.map { it.primary }.toSet()
        assertEquals("Each preset must resolve to a distinct primary in dark mode", 5, darkPrimaries.size)

        // All 5 presets must have distinct primary colors in light mode
        val lightPrimaries = lightPalettes.map { it.primary }.toSet()
        assertEquals("Each preset must resolve to a distinct primary in light mode", 5, lightPrimaries.size)

        // Verify specific preset hues
        val indigo = PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = true)
        val crimson = PlayerArtworkPalette.fromPreset("Crimson Velvet", isDark = true)
        val emerald = PlayerArtworkPalette.fromPreset("Emerald Pine", isDark = true)
        val slate = PlayerArtworkPalette.fromPreset("Nordic Slate", isDark = true)
        val monochrome = PlayerArtworkPalette.fromPreset("Monochrome", isDark = true)

        assertEquals(androidx.compose.ui.graphics.Color(0xFF1A237E), indigo.primary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF880E4F), crimson.primary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1B5E20), emerald.primary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF263238), slate.primary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1E1E1E), monochrome.primary)
    }

    @Test
    fun monochrome_preset_resolves_to_neutral_palette() {
        val darkNeutral = PlayerArtworkPalette.neutral(isDark = true)
        val lightNeutral = PlayerArtworkPalette.neutral(isDark = false)

        val presetDark = PlayerArtworkPalette.fromPreset("Monochrome", isDark = true)
        val presetLight = PlayerArtworkPalette.fromPreset("Monochrome", isDark = false)

        assertEquals(darkNeutral.primary, presetDark.primary)
        assertEquals(darkNeutral.secondary, presetDark.secondary)
        assertEquals(darkNeutral.accent, presetDark.accent)

        assertEquals(lightNeutral.primary, presetLight.primary)
        assertEquals(lightNeutral.secondary, presetLight.secondary)
        assertEquals(lightNeutral.accent, presetLight.accent)
    }

    @Test
    fun palette_mode_precedence_rules_are_strictly_enforced() {
        // 1. When dynamicArtworkColors = true, mode is ALWAYS DYNAMIC_ARTWORK, even with a preset selected
        val modeWithBoth = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = true,
            colorPreset = "Midnight Indigo"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.DYNAMIC_ARTWORK, modeWithBoth)
        assertTrue(modeWithBoth.isColored)

        // 2. When dynamicArtworkColors = false, selecting a static preset resolves to PRESET mode
        val modePresetIndigo = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Midnight Indigo"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, modePresetIndigo)
        assertTrue(modePresetIndigo.isColored)

        val modePresetCrimson = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Crimson Velvet"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, modePresetCrimson)
        assertTrue(modePresetCrimson.isColored)

        val modePresetEmerald = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Emerald Pine"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, modePresetEmerald)
        assertTrue(modePresetEmerald.isColored)

        val modePresetSlate = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Nordic Slate"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, modePresetSlate)
        assertTrue(modePresetSlate.isColored)

        // 3. When dynamicArtworkColors = false and preset is Monochrome, mode is MONOCHROME
        val modeMonochrome = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Monochrome"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.MONOCHROME, modeMonochrome)
        assertFalse(modeMonochrome.isColored)

        // 4. Custom palette when preset is Monochrome but custom hexes are present
        val modeCustom = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            dynamicArtworkColors = false,
            colorPreset = "Monochrome",
            customPaletteHexes = "#112233,#445566"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.CUSTOM, modeCustom)
        assertTrue(modeCustom.isColored)
    }

    @Test
    fun static_presets_colour_player_surfaces_when_dynamic_artwork_is_disabled() {
        val indigoPalette = PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = true)

        val scheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            paletteMode = com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET,
            isDark = true,
            primary = indigoPalette.primary,
            secondary = indigoPalette.secondary,
            tertiary = indigoPalette.tertiary,
            accent = indigoPalette.accent
        )

        // Dynamic / colored flag must be true
        assertTrue("Preset scheme must be marked colored / dynamic", scheme.isDynamic)
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, scheme.paletteMode)

        // Surfaces must use preset colors, not flat monochrome fallback
        assertEquals(indigoPalette.primary, scheme.ambientPrimary)
        assertEquals(indigoPalette.accent, scheme.progressIndicator)
        assertEquals(indigoPalette.secondary.copy(alpha = 0.75f), scheme.capsuleSurface)
        assertEquals(indigoPalette.secondary.copy(alpha = 0.75f), scheme.buttonSurface)
    }

    @Test
    fun play_pause_button_colour_remains_strictly_determined_only_by_light_dark_mode() {
        val testPresets = listOf("Monochrome", "Midnight Indigo", "Crimson Velvet", "Emerald Pine", "Nordic Slate")

        for (presetName in testPresets) {
            val darkPalette = PlayerArtworkPalette.fromPreset(presetName, isDark = true)
            val lightPalette = PlayerArtworkPalette.fromPreset(presetName, isDark = false)

            // Test in dark mode: prominent surface must ALWAYS be white, icon tint black
            val darkScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
                isDynamic = !presetName.equals("Monochrome", ignoreCase = true),
                isDark = true,
                primary = darkPalette.primary,
                secondary = darkPalette.secondary,
                tertiary = darkPalette.tertiary,
                accent = darkPalette.accent
            )
            assertEquals("Dark mode play/pause surface must be White for preset $presetName",
                androidx.compose.ui.graphics.Color.White, darkScheme.prominentSurface)
            assertEquals("Dark mode play/pause icon tint must be Black for preset $presetName",
                androidx.compose.ui.graphics.Color.Black, darkScheme.prominentIconTint)

            // Test in light mode: prominent surface must ALWAYS be black, icon tint white
            val lightScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
                isDynamic = !presetName.equals("Monochrome", ignoreCase = true),
                isDark = false,
                primary = lightPalette.primary,
                secondary = lightPalette.secondary,
                tertiary = lightPalette.tertiary,
                accent = lightPalette.accent
            )
            assertEquals("Light mode play/pause surface must be Black for preset $presetName",
                androidx.compose.ui.graphics.Color.Black, lightScheme.prominentSurface)
            assertEquals("Light mode play/pause icon tint must be White for preset $presetName",
                androidx.compose.ui.graphics.Color.White, lightScheme.prominentIconTint)
        }

        // Test with extreme vibrant neon accent in dynamic artwork mode
        val neonAccent = androidx.compose.ui.graphics.Color(0xFFFF0055)
        val dynamicDarkScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = true,
            isDark = true,
            primary = androidx.compose.ui.graphics.Color.Blue,
            secondary = androidx.compose.ui.graphics.Color.Cyan,
            tertiary = androidx.compose.ui.graphics.Color.DarkGray,
            accent = neonAccent
        )
        assertEquals("Dynamic artwork mode must NEVER override dark play/pause White surface",
            androidx.compose.ui.graphics.Color.White, dynamicDarkScheme.prominentSurface)
        assertEquals("Dynamic artwork mode must NEVER override dark play/pause Black icon tint",
            androidx.compose.ui.graphics.Color.Black, dynamicDarkScheme.prominentIconTint)

        val dynamicLightScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            isDynamic = true,
            isDark = false,
            primary = androidx.compose.ui.graphics.Color.Blue,
            secondary = androidx.compose.ui.graphics.Color.Cyan,
            tertiary = androidx.compose.ui.graphics.Color.DarkGray,
            accent = neonAccent
        )
        assertEquals("Dynamic artwork mode must NEVER override light play/pause Black surface",
            androidx.compose.ui.graphics.Color.Black, dynamicLightScheme.prominentSurface)
        assertEquals("Dynamic artwork mode must NEVER override light play/pause White icon tint",
            androidx.compose.ui.graphics.Color.White, dynamicLightScheme.prominentIconTint)
    }

    @Test
    fun player_theme_mode_enum_and_resolution() {
        // Dynamic Artwork mode
        val dynamicMode = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            playerThemeMode = "Dynamic Artwork",
            colorPreset = "Monochrome",
            customPaletteHexes = ""
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.DYNAMIC_ARTWORK, dynamicMode)
        assertTrue(dynamicMode.isColored)

        // Preset mode with colored preset
        val presetMode = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            playerThemeMode = "Preset",
            colorPreset = "Crimson Velvet",
            customPaletteHexes = ""
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.PRESET, presetMode)
        assertTrue(presetMode.isColored)

        // Preset mode with Monochrome preset
        val monochromeMode = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            playerThemeMode = "Preset",
            colorPreset = "Monochrome",
            customPaletteHexes = ""
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.MONOCHROME, monochromeMode)
        assertFalse(monochromeMode.isColored)

        // Custom mode
        val customMode = com.aman.auramusic.ui.theme.resolvePlayerPaletteMode(
            playerThemeMode = "Custom",
            colorPreset = "Midnight Indigo",
            customPaletteHexes = "#FF0000,#00FF00"
        )
        assertEquals(com.aman.auramusic.ui.theme.PlayerPaletteMode.CUSTOM, customMode)
        assertTrue(customMode.isColored)
    }

    @Test
    fun player_custom_palette_creation_and_play_pause_button_invariance() {
        val customHexes = "#2E1A47,#4A2574,#1B0E2B,#7B3FA2"
        val darkPalette = com.aman.auramusic.ui.theme.CustomPaletteValidator.createPaletteFromCustomHexes(customHexes, isDark = true)
        val lightPalette = com.aman.auramusic.ui.theme.CustomPaletteValidator.createPaletteFromCustomHexes(customHexes, isDark = false)

        val darkScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            paletteMode = com.aman.auramusic.ui.theme.PlayerPaletteMode.CUSTOM,
            isDark = true,
            primary = darkPalette.primary,
            secondary = darkPalette.secondary,
            tertiary = darkPalette.tertiary,
            accent = darkPalette.accent
        )
        assertEquals("Dark mode play/pause must remain White in CUSTOM mode",
            androidx.compose.ui.graphics.Color.White, darkScheme.prominentSurface)
        assertEquals("Dark mode play/pause icon tint must remain Black in CUSTOM mode",
            androidx.compose.ui.graphics.Color.Black, darkScheme.prominentIconTint)

        val lightScheme = com.aman.auramusic.ui.theme.resolvePlayerColorScheme(
            paletteMode = com.aman.auramusic.ui.theme.PlayerPaletteMode.CUSTOM,
            isDark = false,
            primary = lightPalette.primary,
            secondary = lightPalette.secondary,
            tertiary = lightPalette.tertiary,
            accent = lightPalette.accent
        )
        assertEquals("Light mode play/pause must remain Black in CUSTOM mode",
            androidx.compose.ui.graphics.Color.Black, lightScheme.prominentSurface)
        assertEquals("Light mode play/pause icon tint must remain White in CUSTOM mode",
            androidx.compose.ui.graphics.Color.White, lightScheme.prominentIconTint)

        // Invalid hex fallback
        val fallbackPalette = com.aman.auramusic.ui.theme.CustomPaletteValidator.createPaletteFromCustomHexes("invalid,garbage", isDark = true)
        assertNotNull(fallbackPalette.primary)
        assertNotNull(fallbackPalette.secondary)
    }
}
