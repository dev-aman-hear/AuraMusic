package com.aman.auramusic.online.model

import com.aman.auramusic.online.lyrics.model.SongLyrics

data class OnlineSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val durationSeconds: Long = 0L,
    val source: AudioSource = AudioSource.JIOSAAVN,
    val streamUrl: String = "",
    val encryptedMediaUrl: String = "",
    val mediaPreviewUrl: String = "",
    val bitrate: String = "320 kbps",
    val format: String = "AAC/MP4",
    val extractorTier: String = "Direct CDN",
    val audioCodec: String = "AAC (320k)",
    val year: String = "",
    val lyrics: String? = null,
    val syncedLyrics: SongLyrics? = null
) {
    val durationFormatted: String
        get() {
            if (durationSeconds <= 0) return "--:--"
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return "%d:%02d".format(m, s)
        }

    val isStreamResolved: Boolean
        get() = streamUrl.isNotBlank()
}
