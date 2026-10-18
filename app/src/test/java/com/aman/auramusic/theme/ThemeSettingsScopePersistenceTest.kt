package com.aman.auramusic.theme

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aman.auramusic.data.repository.UserPreferencesRepository
import com.aman.auramusic.ui.theme.AppArea
import com.aman.auramusic.ui.theme.AppColorEngine
import com.aman.auramusic.ui.theme.PlayerPaletteMode
import com.aman.auramusic.ui.theme.resolvePlayerPaletteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThemeSettingsScopePersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.IO + Job())
        dataStoreFile = tempFolder.newFile("test_theme_user_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { dataStoreFile }
        )
        repository = UserPreferencesRepository(dataStore)
        AppColorEngine.clearCache()
    }

    @After
    fun tearDown() {
        if (testScope.isActive) {
            testScope.cancel()
        }
    }

    private suspend fun reloadRepository(): UserPreferencesRepository {
        testScope.cancel()
        testScope.coroutineContext[Job]?.join()
        testScope = CoroutineScope(Dispatchers.IO + Job())
        val reloadedDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { dataStoreFile }
        )
        return UserPreferencesRepository(reloadedDataStore)
    }

    // 1. Sequence: ON -> Entire App -> Player Only -> Entire App -> Player Only
    @Test
    fun scopeSwitchingSequence_preservesMasterChangeThemeEnabled() = runBlocking {
        repository.setChangeThemeEnabled(true)
        repository.setThemeScope("Entire App")

        var settings = repository.settingsFlow.first()
        assertTrue("Master Change Theme must be ON", settings.changeThemeEnabled)
        assertEquals("Entire App", settings.themeScope)
        assertTrue("Full app dynamic theme must be true", settings.enableFullAppDynamicTheme)
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        // Switch to Player Only
        repository.setThemeScope("Player Only")
        settings = repository.settingsFlow.first()
        assertTrue("Master switch must REMAIN true after switching to Player Only", settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertFalse("enableFullAppDynamicTheme must be false for Player Only", settings.enableFullAppDynamicTheme)
        assertFalse("Full app theme must not be active for Player Only", settings.isFullAppThemeActive())
        assertTrue("Player theme must remain active", settings.isPlayerThemeActive())

        // Switch back to Entire App
        repository.setThemeScope("Entire App")
        settings = repository.settingsFlow.first()
        assertTrue("Master switch must REMAIN true", settings.changeThemeEnabled)
        assertEquals("Entire App", settings.themeScope)
        assertTrue("enableFullAppDynamicTheme must match Entire App", settings.enableFullAppDynamicTheme)
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        // Switch back to Player Only
        repository.setThemeScope("Player Only")
        settings = repository.settingsFlow.first()
        assertTrue("Master switch must REMAIN true", settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertFalse(settings.enableFullAppDynamicTheme)
        assertFalse(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
    }

    // 2. Sequence: ON -> Player Only -> Entire App -> Player Only
    @Test
    fun startInPlayerOnly_scopeSwitching_preservesMasterChangeThemeEnabled() = runBlocking {
        repository.setChangeThemeEnabled(true)
        repository.setThemeScope("Player Only")

        var settings = repository.settingsFlow.first()
        assertTrue(settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertFalse(settings.enableFullAppDynamicTheme)
        assertFalse(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        // Switch to Entire App
        repository.setThemeScope("Entire App")
        settings = repository.settingsFlow.first()
        assertTrue(settings.changeThemeEnabled)
        assertEquals("Entire App", settings.themeScope)
        assertTrue(settings.enableFullAppDynamicTheme)
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())

        // Switch back to Player Only
        repository.setThemeScope("Player Only")
        settings = repository.settingsFlow.first()
        assertTrue(settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertFalse(settings.enableFullAppDynamicTheme)
        assertFalse(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
    }

    // 3. Repeat scope switching at least 10 times in rapid succession
    @Test
    fun repeatScopeSwitchingTenTimes_neverTurnsOffMasterSwitch() = runBlocking {
        repository.setChangeThemeEnabled(true)

        for (i in 1..10) {
            val targetScope = if (i % 2 == 1) "Player Only" else "Entire App"
            repository.setThemeScope(targetScope)

            val settings = repository.settingsFlow.first()
            assertTrue("Master switch must stay true on iteration $i", settings.changeThemeEnabled)
            assertEquals("Scope must match targetScope on iteration $i", targetScope, settings.themeScope)
            assertEquals(
                "enableFullAppDynamicTheme must match scope on iteration $i",
                targetScope == "Entire App",
                settings.enableFullAppDynamicTheme
            )
        }
    }

    // 4. OFF -> ON in both scopes without destroying settings
    @Test
    fun toggleOffAndOnInBothScopes_preservesConfiguration() = runBlocking {
        repository.setChangeThemeEnabled(true)
        repository.setThemeScope("Player Only")
        repository.setThemeColorStyle("Custom")
        repository.setThemeCustomHexes("#123456,#654321")

        // Turn OFF while in Player Only
        repository.setChangeThemeEnabled(false)
        var settings = repository.settingsFlow.first()
        assertFalse("Master must be false", settings.changeThemeEnabled)
        assertFalse("Full app must be inactive when master is OFF", settings.isFullAppThemeActive())
        assertFalse("Player must be inactive when master is OFF", settings.isPlayerThemeActive())
        assertEquals("Scope must be preserved", "Player Only", settings.themeScope)
        assertEquals("Style must be preserved", "Custom", settings.themeColorStyle)
        assertEquals("#123456,#654321", settings.appCustomPaletteHexes)

        // Turn ON again
        repository.setChangeThemeEnabled(true)
        settings = repository.settingsFlow.first()
        assertTrue("Master must be ON", settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertEquals("Custom", settings.themeColorStyle)
        assertEquals("#123456,#654321", settings.appCustomPaletteHexes)
        assertFalse("Full app remains inactive for Player Only", settings.isFullAppThemeActive())
        assertTrue("Player returns to active themed state", settings.isPlayerThemeActive())

        // Switch to Entire App, turn OFF, then turn ON
        repository.setThemeScope("Entire App")
        repository.setChangeThemeEnabled(false)
        settings = repository.settingsFlow.first()
        assertFalse(settings.changeThemeEnabled)
        assertEquals("Entire App", settings.themeScope)
        assertFalse(settings.isFullAppThemeActive())
        assertFalse(settings.isPlayerThemeActive())

        repository.setChangeThemeEnabled(true)
        settings = repository.settingsFlow.first()
        assertTrue(settings.changeThemeEnabled)
        assertEquals("Entire App", settings.themeScope)
        assertTrue(settings.isFullAppThemeActive())
        assertTrue(settings.isPlayerThemeActive())
    }

    // 5. Switch scope with each style: Dynamic Artwork, Preset, and Custom
    @Test
    fun switchScopeWithEachStyle_preservesStyleAndPalette() = runBlocking {
        repository.setChangeThemeEnabled(true)

        val styles = listOf(
            Triple("Dynamic Artwork", "Emerald Pine", "#1A237E,#3949AB,#0D1642"),
            Triple("Preset", "Crimson Velvet", "#1A237E,#3949AB,#0D1642"),
            Triple("Custom", "Crimson Velvet", "#9C27B0,#E91E63,#2196F3")
        )

        for ((style, preset, hexes) in styles) {
            repository.setThemeColorStyle(style)
            repository.setThemePreset(preset)
            repository.setThemeCustomHexes(hexes)

            // Switch to Player Only
            repository.setThemeScope("Player Only")
            var settings = repository.settingsFlow.first()
            assertTrue("Master must remain ON for style $style", settings.changeThemeEnabled)
            assertEquals("Player Only", settings.themeScope)
            assertEquals(style, settings.themeColorStyle)
            assertEquals(preset, settings.appColorPreset)
            assertEquals(hexes, settings.appCustomPaletteHexes)

            // Switch to Entire App
            repository.setThemeScope("Entire App")
            settings = repository.settingsFlow.first()
            assertTrue("Master must remain ON for style $style", settings.changeThemeEnabled)
            assertEquals("Entire App", settings.themeScope)
            assertEquals(style, settings.themeColorStyle)
            assertEquals(preset, settings.appColorPreset)
            assertEquals(hexes, settings.appCustomPaletteHexes)
        }
    }

    // 6. Navigate away and return; force-stop and reopen the app (simulating DataStore reload)
    @Test
    fun forceStopAndReopen_persistsScopeAndMasterSetting() = runBlocking {
        repository.setChangeThemeEnabled(true)
        repository.setThemeScope("Player Only")
        repository.setThemeColorStyle("Preset")
        repository.setThemePreset("Nordic Slate")

        var settings = repository.settingsFlow.first()
        assertTrue(settings.changeThemeEnabled)
        assertEquals("Player Only", settings.themeScope)
        assertEquals("Nordic Slate", settings.appColorPreset)

        // Simulate app kill and reopen by creating a new repository against the same file
        val reloadedRepo = reloadRepository()
        val reloadedSettings = reloadedRepo.settingsFlow.first()

        assertTrue("Master setting must persist after reopen", reloadedSettings.changeThemeEnabled)
        assertEquals("Scope must persist after reopen", "Player Only", reloadedSettings.themeScope)
        assertEquals("Style must persist after reopen", "Preset", reloadedSettings.themeColorStyle)
        assertEquals("Preset must persist after reopen", "Nordic Slate", reloadedSettings.appColorPreset)
        assertFalse("Full app remains inactive for Player Only", reloadedSettings.isFullAppThemeActive())
        assertTrue("Player remains active for Player Only", reloadedSettings.isPlayerThemeActive())

        // Verify rendered theme colors match expected scope
        val homeScheme = AppColorEngine.resolveAreaColorScheme(AppArea.HOME, null, reloadedSettings, isDark = true)
        assertFalse("Home screen must be monochrome under Player Only", homeScheme.isColored)
        assertEquals(PlayerPaletteMode.PRESET, resolvePlayerPaletteMode(reloadedSettings))
    }

    // 7. Calling setEnableFullAppDynamicTheme does not overwrite changeThemeEnabled
    @Test
    fun setEnableFullAppDynamicTheme_neverDisablesMasterSetting() = runBlocking {
        repository.setChangeThemeEnabled(true)
        repository.setThemeScope("Entire App")

        // Calling setEnableFullAppDynamicTheme(false) as in legacy callbacks
        repository.setEnableFullAppDynamicTheme(false)
        val settings = repository.settingsFlow.first()

        assertTrue("Master setting must NEVER be turned off by setEnableFullAppDynamicTheme(false)", settings.changeThemeEnabled)
        assertFalse(settings.enableFullAppDynamicTheme)
    }
}
