package com.aman.auramusic.theme

import androidx.compose.ui.graphics.Color
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.ui.theme.AppArea
import com.aman.auramusic.ui.theme.AppColorEngine
import com.aman.auramusic.ui.theme.AppColorScheme
import com.aman.auramusic.ui.theme.AppPaletteMode
import com.aman.auramusic.ui.theme.CustomPaletteValidator
import com.aman.auramusic.ui.theme.DarkMonochromeColors
import com.aman.auramusic.ui.theme.LightMonochromeColors
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import com.aman.auramusic.ui.theme.resolvePlayerColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppColorEngineTest {

    @Before
    fun setUp() {
        AppColorEngine.clearCache()
    }

    // 1. Default settings resolve to monochrome
    @Test
    fun defaultSettings_resolveToMonochromeEverywhere() {
        val settings = AppSettings()
        assertFalse("Default full app dynamic theme must be OFF", settings.enableFullAppDynamicTheme)

        for (area in AppArea.entries) {
            val darkScheme = AppColorEngine.resolveAreaColorScheme(area, null, settings, isDark = true)
            val lightScheme = AppColorEngine.resolveAreaColorScheme(area, null, settings, isDark = false)

            assertFalse("Area $area must be uncolored by default", darkScheme.isColored)
            assertFalse("Area $area must be uncolored by default in light mode", lightScheme.isColored)

            assertEquals("Dark background must match DarkMonochromeColors", DarkMonochromeColors.background, darkScheme.background)
            assertEquals("Light background must match LightMonochromeColors", LightMonochromeColors.background, lightScheme.background)
        }
    }

    // 2. Master switch OFF forces monochrome everywhere regardless of per-area flags
    @Test
    fun masterSwitchOff_forcesMonochromeEverywhere() {
        val settings = AppSettings(
            enableFullAppDynamicTheme = false,
            appPaletteMode = "Preset",
            appColorPreset = "Midnight Indigo",
            themeHomeEnabled = true,
            themeExploreEnabled = true,
            themeLibraryEnabled = true,
            themeAlbumDetailEnabled = true,
            themeArtistDetailEnabled = true,
            themePlaylistDetailEnabled = true
        )

        for (area in AppArea.entries) {
            val scheme = AppColorEngine.resolveAreaColorScheme(area, null, settings, isDark = true)
            assertFalse("Area $area must be forced to monochrome when master switch is OFF", scheme.isColored)
            assertEquals(DarkMonochromeColors.background, scheme.background)
        }
    }

    // 3. Each area switch operates independently
    @Test
    fun eachAreaSwitch_operatesIndependently() {
        // Only Library enabled, others disabled
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Preset",
            appColorPreset = "Midnight Indigo",
            themeHomeEnabled = false,
            themeExploreEnabled = false,
            themeLibraryEnabled = true,
            themeAlbumDetailEnabled = false,
            themeArtistDetailEnabled = false,
            themePlaylistDetailEnabled = false
        )

        val homeScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        val libraryScheme = AppColorEngine.resolveAreaColorScheme(AppArea.LIBRARY, null, settings, isDark = true)
        val albumScheme = AppColorEngine.resolveAreaColorScheme(AppArea.ALBUM_DETAIL, null, settings, isDark = true)

        assertFalse("Home must be monochrome when disabled", homeScheme.isColored)
        assertTrue("Library must be colored when enabled", libraryScheme.isColored)
        assertFalse("Album detail must be monochrome when disabled", albumScheme.isColored)
    }

    // 4. Enabling Home theming does not enable Artist theming
    @Test
    fun enablingHomeTheming_doesNotEnableArtistTheming() {
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Preset",
            appColorPreset = "Crimson Velvet",
            themeHomeEnabled = true,
            themeArtistDetailEnabled = false
        )

        val homeScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        val artistScheme = AppColorEngine.resolveAreaColorScheme(AppArea.ARTIST_DETAIL, null, settings, isDark = true)

        assertTrue("Home should be colored", homeScheme.isColored)
        assertFalse("Artist should remain monochrome", artistScheme.isColored)
    }

    // 5. Dynamic artwork produces the expected palette for the relevant artwork
    @Test
    fun dynamicArtwork_producesExpectedPalette() {
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Dynamic Artwork",
            themeHomeEnabled = true
        )

        val testPalette = PlayerArtworkPalette(
            primary = Color(0xFF1A237E),
            secondary = Color(0xFF0D1642),
            tertiary = Color(0xFF070B24),
            accent = Color(0xFF3949AB),
            isLightArtwork = false
        )

        val scheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, testPalette, settings, isDark = true)
        assertTrue("Scheme must be colored with dynamic artwork", scheme.isColored)
        assertEquals("Accent should match palette accent", testPalette.accent, scheme.accent)
        assertNotEquals("Background must be tinted, not raw pure black", DarkMonochromeColors.background, scheme.background)
    }

    // 6. Album and artist screens use their own artwork sources
    @Test
    fun albumAndArtistScreens_useOwnArtworkSources() {
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Dynamic Artwork",
            themeAlbumDetailEnabled = true,
            themeArtistDetailEnabled = true
        )

        val albumPalette = PlayerArtworkPalette(
            primary = Color(0xFF880E4F),
            secondary = Color(0xFF4A0027),
            tertiary = Color(0xFF260013),
            accent = Color(0xFFC2185B),
            isLightArtwork = false
        )

        val artistPalette = PlayerArtworkPalette(
            primary = Color(0xFF1B5E20),
            secondary = Color(0xFF0B330F),
            tertiary = Color(0xFF051C08),
            accent = Color(0xFF2E7D32),
            isLightArtwork = false
        )

        val albumScheme = AppColorEngine.resolveAreaColorScheme(AppArea.ALBUM_DETAIL, albumPalette, settings, isDark = true)
        val artistScheme = AppColorEngine.resolveAreaColorScheme(AppArea.ARTIST_DETAIL, artistPalette, settings, isDark = true)

        assertEquals(albumPalette.accent, albumScheme.accent)
        assertEquals(artistPalette.accent, artistScheme.accent)
        assertNotEquals(albumScheme.background, artistScheme.background)
    }

    // 7. Preset mode applies the selected preset
    @Test
    fun presetMode_appliesSelectedPreset() {
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Preset",
            appColorPreset = "Emerald Pine",
            themeHomeEnabled = true
        )

        val scheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        assertTrue(scheme.isColored)

        val expectedPreset = PlayerArtworkPalette.fromPreset("Emerald Pine", isDark = true)
        assertEquals(expectedPreset.accent, scheme.accent)
    }

    // 8. Custom palettes validate and resolve correctly
    @Test
    fun customPalettes_validateAndResolveCorrectly() {
        // Valid 3-color hex string
        val validHexes = "#1A237E,#3949AB,#0D1642"
        assertTrue(CustomPaletteValidator.validate(validHexes))
        assertEquals(3, CustomPaletteValidator.parseHexList(validHexes).size)

        // Valid 2-color hex string
        assertTrue(CustomPaletteValidator.validate("#FF0000,#00FF00"))

        // Invalid: 1 color
        assertFalse(CustomPaletteValidator.validate("#FF0000"))

        // Invalid: 6 colors (exceeds max 5)
        assertFalse(CustomPaletteValidator.validate("#111111,#222222,#333333,#444444,#555555,#666666"))

        // Invalid: Corrupt hex
        assertFalse(CustomPaletteValidator.validate("#GGZZYY,#123456"))

        // Custom palette mode resolution
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Custom",
            appCustomPaletteHexes = validHexes,
            themeHomeEnabled = true
        )

        val scheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        assertTrue(scheme.isColored)
        assertNotNull(scheme.background)
    }

    // 9. Invalid / missing artwork falls back safely
    @Test
    fun invalidOrMissingArtwork_fallsBackSafely() {
        // When dynamic artwork is null, but preset is selected
        val settingsWithPresetFallback = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Dynamic Artwork",
            appColorPreset = "Midnight Indigo",
            themeHomeEnabled = true
        )

        val fallbackScheme = AppColorEngine.resolveAreaColorScheme(
            AppArea.HOME,
            artworkPalette = null,
            appSettings = settingsWithPresetFallback,
            isDark = true
        )

        assertTrue("Should fall back to selected preset", fallbackScheme.isColored)
        val expectedPreset = PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = true)
        assertEquals(expectedPreset.accent, fallbackScheme.accent)

        // When preset is Monochrome and artwork is null, falls back to monochrome
        val settingsMonochromeFallback = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Dynamic Artwork",
            appColorPreset = "Monochrome",
            themeHomeEnabled = true
        )

        val monoFallbackScheme = AppColorEngine.resolveAreaColorScheme(
            AppArea.HOME,
            artworkPalette = null,
            appSettings = settingsMonochromeFallback,
            isDark = true
        )

        assertFalse("Should fall back to monochrome", monoFallbackScheme.isColored)
        assertEquals(DarkMonochromeColors.background, monoFallbackScheme.background)
    }

    // 10. Player settings remain independent of full-app settings
    @Test
    fun playerSettings_remainIndependentOfFullAppSettings() {
        // Full app theming is enabled with Crimson Velvet, but player has Monochrome
        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Preset",
            appColorPreset = "Crimson Velvet",
            dynamicArtworkColors = false,
            colorPreset = "Monochrome"
        )

        val fullAppScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        val neutralPal = PlayerArtworkPalette.neutral(isDark = true)
        val playerScheme = resolvePlayerColorScheme(
            isDynamic = false,
            isDark = true,
            primary = neutralPal.primary,
            secondary = neutralPal.secondary,
            tertiary = neutralPal.tertiary,
            accent = neutralPal.accent
        )

        assertTrue("Full app should be colored", fullAppScheme.isColored)
        assertFalse("Player scheme should remain monochrome", playerScheme.isDynamic)
    }

    // 11. Play/pause button colors remain fixed in light and dark modes
    @Test
    fun playPauseButton_colorsRemainFixedInLightAndDarkModes() {
        // Verify dark mode player prominent surface is white
        val darkPal = PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = true)
        val darkPlayerScheme = resolvePlayerColorScheme(
            isDynamic = true,
            isDark = true,
            primary = darkPal.primary,
            secondary = darkPal.secondary,
            tertiary = darkPal.tertiary,
            accent = darkPal.accent
        )
        assertEquals(
            "Player play/pause button surface must be White in dark mode",
            Color.White,
            darkPlayerScheme.prominentSurface
        )

        // Verify light mode player prominent surface is black
        val lightPal = PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = false)
        val lightPlayerScheme = resolvePlayerColorScheme(
            isDynamic = true,
            isDark = false,
            primary = lightPal.primary,
            secondary = lightPal.secondary,
            tertiary = lightPal.tertiary,
            accent = lightPal.accent
        )
        assertEquals(
            "Player play/pause button surface must be Black in light mode",
            Color.Black,
            lightPlayerScheme.prominentSurface
        )
    }
}
