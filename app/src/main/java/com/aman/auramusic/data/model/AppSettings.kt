package com.aman.auramusic.data.model

data class AppSettings(
    val karaokeMode: Boolean = true,
    val lyricFontScale: Float = 1.0f,
    val crossfadeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val smartAudioFocus: Boolean = true,
    val playlistGridColumns: Int = 2,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicArtworkColors: Boolean = false,
    val colorPreset: String = "Monochrome",
    val customPaletteHexes: String = "#1A237E,#3949AB,#0D1642",
    val playerThemeMode: String = "Preset",
    val enablePlayerColor: Boolean = true,
    val enableMiniPlayerColor: Boolean = false,
    // Full-App Theming
    val enableFullAppDynamicTheme: Boolean = false,
    val appPaletteMode: String = "Preset",
    val appColorPreset: String = "Midnight Indigo",
    val appCustomPaletteHexes: String = "#1A237E,#3949AB,#0D1642",
    val themeHomeEnabled: Boolean = true,
    val themeExploreEnabled: Boolean = true,
    val themeLibraryEnabled: Boolean = true,
    val themeAlbumDetailEnabled: Boolean = true,
    val themeArtistDetailEnabled: Boolean = true,
    val themePlaylistDetailEnabled: Boolean = true,
    // Unified Appearance / Theme System
    val changeThemeEnabled: Boolean = false,
    val themeScope: String = "Entire App",
    val themeColorStyle: String = appPaletteMode
) {
    fun isAreaThemingEnabled(areaName: String): Boolean {
        if (!isFullAppThemeActive()) return false
        return when (areaName.uppercase()) {
            "HOME" -> themeHomeEnabled
            "EXPLORE", "SEARCH" -> themeExploreEnabled
            "LIBRARY" -> themeLibraryEnabled
            "ALBUM_DETAIL" -> themeAlbumDetailEnabled
            "ARTIST_DETAIL" -> themeArtistDetailEnabled
            "PLAYLIST_DETAIL" -> themePlaylistDetailEnabled
            else -> false
        }
    }

    /**
     * Returns true if colored theming is enabled and applied across the full app.
     */
    fun isFullAppThemeActive(): Boolean = changeThemeEnabled && themeScope.equals("Entire App", ignoreCase = true)

    /**
     * Returns true if colored theming is enabled for player surfaces.
     */
    fun isPlayerThemeActive(): Boolean = changeThemeEnabled
}

