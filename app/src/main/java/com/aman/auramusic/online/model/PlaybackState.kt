package com.aman.auramusic.online.model

import com.aman.auramusic.online.lyrics.model.SongLyrics

/**
 * Reactive state model consumed by Jetpack Compose UI to represent live playback status.
 */
data class PlaybackState(
    val currentSong: OnlineSong? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val queue: List<OnlineSong> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val errorMessage: String? = null,
    val selectedBitrate: String = "320 kbps",
    val currentLyrics: SongLyrics? = null,
    val activeLyricsLineIndex: Int = -1,
    val isLyricsMode: Boolean = false,
    val isLoadingLyrics: Boolean = false,
    val selectedEngine: StreamingEngine = StreamingEngine.RIPLAY,
    val activePlayerType: String = "ExoPlayer"
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val currentPositionFormatted: String
        get() = formatTimeMs(currentPositionMs)

    val durationFormatted: String
        get() = formatTimeMs(durationMs)

    companion object {
        fun formatTimeMs(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
