package com.aman.auramusic.online.lyrics.model

/**
 * Represents an individual word within a word-synced lyrics line (Apple Music style).
 */
data class LyricsWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

/**
 * Represents a line in a synchronized lyrics stream.
 * Can contain word-level timings if enhanced LRC is available.
 */
data class LyricsLine(
    val lineText: String,
    val startMs: Long,
    val endMs: Long = 0L,
    val words: List<LyricsWord> = emptyList()
) {
    val isWordSynced: Boolean
        get() = words.isNotEmpty()

    val formattedTimestamp: String
        get() {
            val totalSec = startMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%02d:%02d".format(min, sec)
        }
}

/**
 * Full lyrics payload for a song.
 */
data class SongLyrics(
    val trackName: String,
    val artistName: String,
    val durationSeconds: Long = 0L,
    val lines: List<LyricsLine> = emptyList(),
    val isSynced: Boolean = false,
    val isWordSynced: Boolean = false,
    val plainLyrics: String? = null
) {
    val isEmpty: Boolean
        get() = lines.isEmpty() && plainLyrics.isNullOrBlank()

    fun findActiveLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (positionMs >= lines[i].startMs) {
                return i
            }
        }
        return 0
    }
}
