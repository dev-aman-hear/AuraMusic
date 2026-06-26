package com.aman.auramusic.data.repository

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import java.io.File

class LyricsRepository {

    fun lyricsFor(song: Song): List<LyricLine> {
        return try {
            val audioFile = File(song.filePath)
            val lyricFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")

            if (!lyricFile.exists()) {
                return emptyList()
            }

            lyricFile
                .readLines()
                .flatMap(::parseLine)
                .sortedBy { it.timeMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseLine(line: String): List<LyricLine> {
        val matches = timeTagRegex.findAll(line).toList()
        if (matches.isEmpty()) {
            return emptyList()
        }

        val lyricText = line.replace(timeTagRegex, "").trim()
        if (lyricText.isBlank()) {
            return emptyList()
        }

        return matches.mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull()
                ?: return@mapNotNull null

            LyricLine(
                timeMs = (minutes * 60_000) + (seconds * 1_000) + fraction,
                text = lyricText,
            )
        }
    }

    private companion object {
        val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})\\.(\\d{1,3})]")
    }
}
