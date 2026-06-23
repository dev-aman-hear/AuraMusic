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
    val smartAudioFocus: Boolean = true,
    val keepPlayingOnClose: Boolean = true,
    val playlistGridColumns: Int = 2,
    val dynamicPillEnabled: Boolean = false,
    val pillPosition: Int = 1, // 0: Left, 1: Center, 2: Right
    val pillVerticalOffset: Int = 32,
    val pillSizeScale: Float = 1.0f
)
