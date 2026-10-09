package com.aman.auramusic.online.player

import com.aman.auramusic.online.lyrics.model.SongLyrics
import com.aman.auramusic.online.lyrics.network.LrclibService
import com.aman.auramusic.online.model.OnlineSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates fetching, caching, and state synchronization for synchronized lyrics.
 */
class LyricsManager(
    private val lrclibService: LrclibService = LrclibService()
) {
    private val lyricsCache = ConcurrentHashMap<String, SongLyrics>()

    suspend fun getOrFetchLyrics(song: OnlineSong): SongLyrics = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(song)
        lyricsCache[cacheKey]?.let { return@withContext it }

        val fetched = lrclibService.getLyrics(
            title = song.title,
            artist = song.artist,
            durationSeconds = song.durationSeconds
        )

        lyricsCache[cacheKey] = fetched
        fetched
    }

    fun getCachedLyrics(song: OnlineSong): SongLyrics? {
        return lyricsCache[buildCacheKey(song)]
    }

    private fun buildCacheKey(song: OnlineSong): String {
        return "${song.title.trim().lowercase()}::${song.artist.trim().lowercase()}"
    }
}
