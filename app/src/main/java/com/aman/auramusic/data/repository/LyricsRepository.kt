package com.aman.auramusic.data.repository

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.lyrics.network.LrclibService
import com.aman.auramusic.online.model.toLyricLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class LyricsRepository(
    private val lrclibService: LrclibService = LrclibService()
) {
    private val memoryCache = ConcurrentHashMap<String, List<LyricLine>>()

    fun getCachedLyrics(song: Song): List<LyricLine> {
        val cacheKey = buildCacheKey(song)
        memoryCache[cacheKey]?.let { return it }

        val local = lyricsForLocal(song)
        if (local.isNotEmpty()) {
            memoryCache[cacheKey] = local
            return local
        }

        return emptyList()
    }

    fun cacheLyrics(song: Song, lyrics: List<LyricLine>) {
        if (lyrics.isNotEmpty()) {
            memoryCache[buildCacheKey(song)] = lyrics
        }
    }

    suspend fun lyricsFor(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        val cached = getCachedLyrics(song)
        if (cached.isNotEmpty()) return@withContext cached

        // 2. Fallback to online lyrics via LRCLIB
        try {
            val durationSec = (song.duration / 1000L).coerceAtLeast(0L)
            val onlineLyrics = lrclibService.getLyrics(
                title = song.title,
                artist = song.artist,
                durationSeconds = durationSec
            )
            val converted = onlineLyrics.toLyricLines()
            if (converted.isNotEmpty()) {
                cacheLyrics(song, converted)
                return@withContext converted
            }
        } catch (_: Exception) {}

        emptyList()
    }

    fun lyricsForLocal(song: Song): List<LyricLine> {
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

    private fun buildCacheKey(song: Song): String {
        return "${song.title.trim().lowercase()}::${song.artist.trim().lowercase()}"
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
