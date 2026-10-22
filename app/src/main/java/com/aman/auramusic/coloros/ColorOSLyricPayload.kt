package com.aman.auramusic.coloros

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import org.json.JSONObject
import java.util.Locale

/**
 * Builds the public lyricInfo payload consumed by the native OPlus/ColorOS media pipeline.
 *
 * This payload is independent of the optional LSPosed Bridge: compatible firmware can consume
 * it from AuraMusic's existing MediaSession, while Bridge can provide additional rendering.
 */
object ColorOSLyricPayload {

    fun formatLrc(lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        return lyrics
            .filter { it.timeMs >= 0L && it.text.isNotBlank() }
            .sortedBy { it.timeMs }
            .joinToString(separator = "\n", postfix = "\n") { line ->
                val totalSeconds = line.timeMs / 1000L
                val minutes = totalSeconds / 60L
                val seconds = totalSeconds % 60L
                val millis = line.timeMs % 1000L
                String.format(Locale.US, "[%02d:%02d.%03d]%s", minutes, seconds, millis, line.text)
            }
    }

    fun generateLyricInfoJson(song: Song, lyrics: List<LyricLine>): String {
        val lrc = formatLrc(lyrics)
        if (lrc.isBlank()) return ""

        return JSONObject().apply {
            put("songName", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("songId", song.onlineId?.takeIf { it.isNotBlank() } ?: song.id.toString())
            put("lyricType", 0)
            put("lyric", lrc)
            put("provider", "com.heytap.music")
            put("source", "com.heytap.music")
            put("trackKey", buildTrackKey(song))
            put("noLyric", false)
            // Do not populate rawLyric for line-timed LRC. The Bridge protocol reserves that
            // field for enhanced LRC with real word-level timestamps.
        }.toString()
    }

    fun buildTrackKey(song: Song): String {
        val durationSeconds = (song.duration.coerceAtLeast(0L) / 1000L).toString()
        return listOf(
            song.onlineId?.takeIf { it.isNotBlank() } ?: song.id.toString(),
            song.title.trim().lowercase(Locale.ROOT),
            song.artist.trim().lowercase(Locale.ROOT),
            durationSeconds
        ).joinToString("|")
    }
}
