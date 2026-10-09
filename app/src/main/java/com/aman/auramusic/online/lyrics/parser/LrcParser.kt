package com.aman.auramusic.online.lyrics.parser

import com.aman.auramusic.online.lyrics.model.LyricsLine
import com.aman.auramusic.online.lyrics.model.LyricsWord
import com.aman.auramusic.online.lyrics.model.SongLyrics
import java.util.regex.Pattern

/**
 * High-performance parser for Standard and Enhanced (Word-Synced) LRC formats.
 * Compatible with LRCLIB, Apple Music timed text, and Spotify-style enhanced LRC.
 */
object LrcParser {

    private val LINE_TIMESTAMP_REGEX = Pattern.compile("^\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)$")
    private val WORD_TAG_REGEX = Pattern.compile("<(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?>([^<]+)")

    fun parse(
        lrcContent: String,
        trackName: String = "",
        artistName: String = "",
        durationSeconds: Long = 0L,
        plainLyrics: String? = null
    ): SongLyrics {
        if (lrcContent.isBlank()) {
            return SongLyrics(
                trackName = trackName,
                artistName = artistName,
                durationSeconds = durationSeconds,
                lines = emptyList(),
                isSynced = false,
                isWordSynced = false,
                plainLyrics = plainLyrics
            )
        }

        val rawLines = lrcContent.lines()
        val parsedLines = mutableListOf<LyricsLine>()
        var hasWordTimings = false

        for (raw in rawLines) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue

            // Skip standard ID3/LRC header metadata tags like [ti:...], [ar:...], [length:...]
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:") || trimmed.startsWith("[length:")
            ) {
                continue
            }

            val matcher = LINE_TIMESTAMP_REGEX.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3)
                val millis = parseMillis(millisRaw)
                val startMs = (min * 60 + sec) * 1000 + millis
                val content = matcher.group(4)?.trim() ?: ""

                // Check for enhanced word-synced tokens e.g. <00:15.30> word
                val wordMatcher = WORD_TAG_REGEX.matcher(content)
                val words = mutableListOf<LyricsWord>()

                while (wordMatcher.find()) {
                    val wMin = wordMatcher.group(1)?.toLongOrNull() ?: 0L
                    val wSec = wordMatcher.group(2)?.toLongOrNull() ?: 0L
                    val wMillis = parseMillis(wordMatcher.group(3))
                    val wStartMs = (wMin * 60 + wSec) * 1000 + wMillis
                    val wText = wordMatcher.group(4)?.trim() ?: ""

                    if (wText.isNotBlank()) {
                        words.add(LyricsWord(text = wText, startMs = wStartMs, endMs = wStartMs + 500L))
                    }
                }

                if (words.isNotEmpty()) {
                    hasWordTimings = true
                    for (i in 0 until words.size - 1) {
                        words[i] = words[i].copy(endMs = words[i + 1].startMs)
                    }
                }

                val cleanedText = if (words.isNotEmpty()) {
                    words.joinToString(" ") { it.text }
                } else {
                    content.replace(Regex("<[^>]+>"), "").trim()
                }

                parsedLines.add(
                    LyricsLine(
                        lineText = cleanedText,
                        startMs = startMs,
                        words = words
                    )
                )
            }
        }

        parsedLines.sortBy { it.startMs }

        val finalizedLines = mutableListOf<LyricsLine>()
        for (i in parsedLines.indices) {
            val current = parsedLines[i]
            val endMs = if (i < parsedLines.size - 1) {
                parsedLines[i + 1].startMs
            } else {
                if (durationSeconds > 0) durationSeconds * 1000 else current.startMs + 5000L
            }
            finalizedLines.add(current.copy(endMs = endMs))
        }

        return SongLyrics(
            trackName = trackName,
            artistName = artistName,
            durationSeconds = durationSeconds,
            lines = finalizedLines,
            isSynced = finalizedLines.isNotEmpty(),
            isWordSynced = hasWordTimings,
            plainLyrics = plainLyrics
        )
    }

    private fun parseMillis(raw: String?): Long {
        if (raw.isNullOrEmpty()) return 0L
        return when (raw.length) {
            1 -> raw.toLong() * 100
            2 -> raw.toLong() * 10
            else -> raw.take(3).toLong()
        }
    }
}
