package com.aman.auramusic.coloros

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import org.json.JSONObject

object ColorOSLyricPayload {

    fun formatLrc(lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        return lyrics.joinToString(separator = "\n") { line ->
            val totalSeconds = line.timeMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hundredths = (line.timeMs % 1000) / 10
            String.format("[%02d:%02d.%02d]%s", minutes, seconds, hundredths, line.text)
        }
    }

    fun generateLyricInfoJson(song: Song, lyrics: List<LyricLine>): String {
        val lrc = formatLrc(lyrics)
        if (lrc.isBlank()) return ""

        return JSONObject().apply {
            put("songName", song.title)
            put("artist", song.artist)
            put("songId", song.id.toString())
            put("lyric", lrc)
            put("rawLyric", lrc)
        }.toString()
    }

    fun buildTrackKey(song: Song): String {
        return "${song.title.trim().lowercase()}_${song.artist.trim().lowercase()}"
    }
}
