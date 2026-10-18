package com.aman.auramusic.data.model

data class AppSettings(
    val karaokeMode: Boolean = true,
    val lyricFontScale: Float = 1.0f,
    val crossfadeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val smartAudioFocus: Boolean = true,
    val keepPlayingOnClose: Boolean = true,
    val playlistGridColumns: Int = 2,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicArtworkColors: Boolean = false,
    val colorPreset: String = "Monochrome",
    val customPaletteHexes: String = "",
    val enablePlayerColor: Boolean = true,
    val enableMiniPlayerColor: Boolean = false
)

