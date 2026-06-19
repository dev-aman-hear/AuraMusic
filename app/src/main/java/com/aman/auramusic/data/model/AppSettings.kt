package com.aman.auramusic.data.model

data class AppSettings(
    val dynamicColors: Boolean = true,
    val amoledMode: Boolean = true,
    val blurIntensity: Int = 42,
    val karaokeMode: Boolean = true,
    val lyricFontScale: Float = 1.0f,
    val crossfadeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val skipSilence: Boolean = false,
    val playlistGridColumns: Int = 2
)
