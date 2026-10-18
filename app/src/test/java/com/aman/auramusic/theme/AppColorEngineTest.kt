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
import com.aman.auramusic.ui.theme.PlayerPaletteMode
import com.aman.auramusic.ui.theme.resolvePlayerColorScheme
import com.aman.auramusic.ui.theme.resolvePlayerPaletteMode
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

        // Legacy Monochrome preset safely migrates to Midnight Indigo fallback
        val settingsLegacyFallback = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Dynamic Artwork",
            appColorPreset = "Monochrome",
            themeHomeEnabled = true
        )

        val legacyFallbackScheme = AppColorEngine.resolveAreaColorScheme(
            AppArea.HOME,
            artworkPalette = null,
            appSettings = settingsLegacyFallback,
            isDark = true
        )

        assertTrue("Should safely migrate legacy Monochrome to Midnight Indigo preset", legacyFallbackScheme.isColored)
        assertEquals(expectedPreset.accent, legacyFallbackScheme.accent)
    }

    // Legacy Monochrome palette mode migrates to Preset
    @Test
    fun legacyMonochromeMode_migratesToPreset() {
        val parsed = AppPaletteMode.fromString("Monochrome")
        assertEquals(AppPaletteMode.PRESET, parsed)

        val settings = AppSettings(
            enableFullAppDynamicTheme = true,
            appPaletteMode = "Monochrome",
            appColorPreset = "Emerald Pine",
            themeHomeEnabled = true
        )
        val scheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        assertTrue("Legacy Monochrome mode must resolve as colored preset", scheme.isColored)
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

    // 12. Master switch Change Theme OFF always resolves to monochrome
    @Test
    fun changeThemeMasterSwitchOff_alwaysResolvesToMonochrome() {
        val settings = AppSettings(
            changeThemeEnabled = false,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Midnight Indigo",
            themeHomeEnabled = true
        )

        assertFalse(settings.isFullAppThemeActive())
        assertFalse(settings.isPlayerThemeActive())

        for (area in AppArea.entries) {
            val scheme = AppColorEngine.resolveAreaColorScheme(area, null, settings, isDark = true)
            assertFalse("All areas must be monochrome when master switch is OFF", scheme.isColored)
            assertEquals(DarkMonochromeColors.background, scheme.background)
        }

        val playerPaletteMode = resolvePlayerPaletteMode(settings)
        assertEquals(PlayerPaletteMode.MONOCHROME, playerPaletteMode)
        assertFalse(playerPaletteMode.isColored)
    }

    // 13. ON + Entire App applies selected style to supported app surfaces and player
    @Test
    fun onPlusEntireApp_appliesSelectedStyleToSupportedSurfacesAndPlayer() {
        val settings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Emerald Pine",
            themeHomeEnabled = true,
            themeLibraryEnabled = false
        )

        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        val homeScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        val libraryScheme = AppColorEngine.resolveAreaColorScheme(AppArea.LIBRARY, null, settings, isDark = true)
        assertTrue("Home should be colored", homeScheme.isColored)
        assertFalse("Library should remain uncolored because per-area flag is false", libraryScheme.isColored)

        val playerPaletteMode = resolvePlayerPaletteMode(settings)
        assertEquals(PlayerPaletteMode.PRESET, playerPaletteMode)
        assertTrue("Player should be colored", playerPaletteMode.isColored)
    }

    // 14. ON + Player Only keeps main app monochrome and colors the player
    @Test
    fun onPlusPlayerOnly_keepsMainAppMonochromeAndColorsPlayer() {
        val settings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Player Only",
            themeColorStyle = "Preset",
            appColorPreset = "Crimson Velvet",
            themeHomeEnabled = true,
            themeExploreEnabled = true
        )

        assertFalse("Full app theme must NOT be active when scope is Player Only", settings.isFullAppThemeActive())
        assertTrue("Player theme must be active", settings.isPlayerThemeActive())

        for (area in AppArea.entries) {
            val scheme = AppColorEngine.resolveAreaColorScheme(area, null, settings, isDark = true)
            assertFalse("Area $area must remain strictly monochrome in Player Only scope", scheme.isColored)
            assertEquals(DarkMonochromeColors.background, scheme.background)
        }

        val playerPaletteMode = resolvePlayerPaletteMode(settings)
        assertEquals(PlayerPaletteMode.PRESET, playerPaletteMode)
        assertTrue(playerPaletteMode.isColored)
    }

    // 15. Presets resolve correctly in light and dark modes
    @Test
    fun presetsResolveCorrectlyInLightAndDarkModes() {
        val presets = listOf("Midnight Indigo", "Crimson Velvet", "Emerald Pine", "Nordic Slate")
        for (preset in presets) {
            val darkPal = PlayerArtworkPalette.fromPreset(preset, isDark = true)
            val lightPal = PlayerArtworkPalette.fromPreset(preset, isDark = false)

            assertNotNull(darkPal.primary)
            assertNotNull(darkPal.accent)
            assertNotNull(lightPal.primary)
            assertNotNull(lightPal.accent)

            assertFalse("Dark palette should not be flagged as light artwork", darkPal.isLightArtwork)
            assertTrue("Light palette should be flagged as light artwork", lightPal.isLightArtwork)
            assertNotEquals("Light and dark accent colors should differ appropriately", darkPal.accent, lightPal.accent)
        }
    }

    // 16. Turning master switch OFF and ON restores previous choices
    @Test
    fun turningMasterSwitchOffAndOn_restoresPreviousChoices() {
        val initialSettings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Player Only",
            themeColorStyle = "Custom",
            appColorPreset = "Crimson Velvet",
            appCustomPaletteHexes = "#1A237E,#3949AB,#0D1642"
        )

        // Turn OFF
        val offSettings = initialSettings.copy(changeThemeEnabled = false)
        assertFalse("Full app must be inactive when OFF", offSettings.isFullAppThemeActive())
        assertFalse("Player must be inactive when OFF", offSettings.isPlayerThemeActive())
        assertEquals(PlayerPaletteMode.MONOCHROME, resolvePlayerPaletteMode(offSettings))

        // Turn ON: Previous choices are faithfully restored
        val onSettings = offSettings.copy(changeThemeEnabled = true)
        assertEquals("Player Only", onSettings.themeScope)
        assertEquals("Custom", onSettings.themeColorStyle)
        assertEquals("Crimson Velvet", onSettings.appColorPreset)
        assertEquals("#1A237E,#3949AB,#0D1642", onSettings.appCustomPaletteHexes)
        assertEquals(PlayerPaletteMode.CUSTOM, resolvePlayerPaletteMode(onSettings))
    }

    // 17. Changing scope preserves selected style and palettes
    @Test
    fun changingScope_preservesSelectedStyleAndPalettes() {
        val entireAppSettings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Nordic Slate",
            appCustomPaletteHexes = "#123456,#654321"
        )

        val playerOnlySettings = entireAppSettings.copy(themeScope = "Player Only")
        assertEquals("Preset", playerOnlySettings.themeColorStyle)
        assertEquals("Nordic Slate", playerOnlySettings.appColorPreset)
        assertEquals("#123456,#654321", playerOnlySettings.appCustomPaletteHexes)
        assertEquals(PlayerPaletteMode.PRESET, resolvePlayerPaletteMode(playerOnlySettings))
    }

    // 18. Changing color styles preserves stored preset and custom palette
    @Test
    fun changingColorStyles_preservesStoredPresetAndCustomPalette() {
        val presetSettings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Emerald Pine",
            appCustomPaletteHexes = "#112233,#445566"
        )

        // Switch to Dynamic Artwork
        val dynamicSettings = presetSettings.copy(themeColorStyle = "Dynamic Artwork")
        assertEquals("Emerald Pine", dynamicSettings.appColorPreset)
        assertEquals("#112233,#445566", dynamicSettings.appCustomPaletteHexes)

        // Switch to Custom
        val customSettings = dynamicSettings.copy(themeColorStyle = "Custom")
        assertEquals("Emerald Pine", customSettings.appColorPreset)
        assertEquals("#112233,#445566", customSettings.appCustomPaletteHexes)
    }

    // 19. Full-app and player palettes do not conflict
    @Test
    fun fullAppAndPlayerPalettes_doNotConflict() {
        val settings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Midnight Indigo",
            themeHomeEnabled = true
        )

        val appScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, settings, isDark = true)
        val playerPalette = PlayerArtworkPalette.fromPreset(settings.appColorPreset, isDark = true)
        val playerScheme = resolvePlayerColorScheme(
            isDynamic = true,
            isDark = true,
            primary = playerPalette.primary,
            secondary = playerPalette.secondary,
            tertiary = playerPalette.tertiary,
            accent = playerPalette.accent
        )

        assertTrue("App scheme must be colored", appScheme.isColored)
        assertTrue("Player scheme must be dynamic/colored", playerScheme.isDynamic)
        assertEquals(playerPalette.accent, appScheme.accent)
        assertEquals(playerPalette.accent, playerScheme.ambientAccent)
    }

    // 20. Regression test: Sequence of scope switching, toggle ON/OFF, and config preservation
    @Test
    fun regressionSequence_scopeSwitchingAndToggling_preservesConfiguration() {
        // Step 1: Turn Dynamic Theme ON
        var settings = AppSettings(
            changeThemeEnabled = true,
            themeScope = "Entire App",
            themeColorStyle = "Preset",
            appColorPreset = "Nordic Slate",
            appCustomPaletteHexes = "#123456,#654321"
        )
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        // Step 2 & 3: Switch from Entire App to Player Only
        settings = settings.copy(themeScope = "Player Only")
        assertFalse(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
        assertEquals("Preset", settings.themeColorStyle)
        assertEquals("Nordic Slate", settings.appColorPreset)
        assertEquals(PlayerPaletteMode.PRESET, resolvePlayerPaletteMode(settings))

        // Step 4 & 5: Switch back to Entire App
        settings = settings.copy(themeScope = "Entire App")
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
        assertEquals("Preset", settings.themeColorStyle)
        assertEquals("Nordic Slate", settings.appColorPreset)

        // Step 6: Turn Dynamic Theme OFF and ON again
        settings = settings.copy(changeThemeEnabled = false)
        assertFalse(settings.isFullAppThemeActive())
        assertFalse(settings.isPlayerThemeActive())
        assertEquals(PlayerPaletteMode.MONOCHROME, resolvePlayerPaletteMode(settings))

        settings = settings.copy(changeThemeEnabled = true)
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
        assertEquals("Entire App", settings.themeScope)
        assertEquals("Preset", settings.themeColorStyle)
        assertEquals("Nordic Slate", settings.appColorPreset)
        assertEquals("#123456,#654321", settings.appCustomPaletteHexes)
    }
}
