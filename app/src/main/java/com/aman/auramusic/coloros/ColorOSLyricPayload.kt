package com.aman.auramusic.coloros

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import org.json.JSONObject
import java.util.Locale

/**
 * Builds the public lyricInfo payload consumed by the native OPlus/ColorOS media pipeline.
 *
 * Keep this independent of the optional LSPosed Bridge so compatible firmware can consume
 * the same metadata directly from AuraMusic's existing MediaSession.
 */
object ColorOSLyricPayload {

    fun formatLrc(lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        return lyrics
            .filter { it.timeMs >= 0L && it.text.isNotBlank() }
            .sortedBy { it.timeMs }
            .joinToString(separator = "\n", postfix = "\n") { line ->
                val totalMillis = line.timeMs
                val totalSeconds = totalMillis / 1000L
                val minutes = totalSeconds / 60L
                val seconds = totalSeconds % 60L
                val millis = totalMillis % 1000L
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
            put("provider", "com.aman.auramusic")
            put("source", "com.aman.auramusic-v1")
            put("trackKey", buildTrackKey(song))
            put("noLyric", false)
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
