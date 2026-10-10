package com.aman.auramusic.online.network.repository

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.extractor.DualTierStreamExtractor
import com.aman.auramusic.online.network.piped.PipedService
import com.aman.auramusic.online.network.saavn.JioSaavnService
import com.aman.auramusic.online.filter.TrendingFilterConfig
import com.aman.auramusic.online.filter.TrendingSongFilter
import android.util.LruCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Unified repository orchestrating online music search and dual-tier stream resolution.
 * - Primary Indian/Global catalog: JioSaavn 320kbps Direct CDN (DES decrypted)
 * - YouTube Music catalog: DualTierStreamExtractor (Tier 1: InnerTube Multi-Client Rotation, Tier 2: Piped Failover)
 * - Smart multi-level caching for search queries, playlists, song feeds, and stream URLs.
 */
class OnlineMusicRepository(
    private val jioSaavnService: JioSaavnService = JioSaavnService(),
    private val pipedService: PipedService = PipedService(),
    private val dualTierExtractor: DualTierStreamExtractor = DualTierStreamExtractor(pipedFallback = pipedService),
    val trendingFilter: TrendingSongFilter = TrendingSongFilter()
) {
    companion object {
        private val searchCache = LruCache<String, List<OnlineSong>>(100)
        private val playlistSongsCache = LruCache<String, List<OnlineSong>>(50)
        private val streamCache = LruCache<String, OnlineSong>(100)
        private val playlistsCache = mutableMapOf<AudioSource, Pair<Long, List<com.aman.auramusic.online.model.OnlinePlaylist>>>()
        private var songFeedCache: Pair<Long, List<OnlineSong>>? = null
        private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes
    }
    suspend fun search(query: String, source: AudioSource): List<OnlineSong> = coroutineScope {
        val cacheKey = "${source.name}_${query.trim().lowercase()}"
        searchCache.get(cacheKey)?.let { return@coroutineScope it }

        val results = when (source) {
            AudioSource.JIOSAAVN -> {
                jioSaavnService.searchSongs(query)
            }
            AudioSource.YOUTUBE -> {
                pipedService.searchSongs(query)
            }
            AudioSource.SPOTIFY -> {
                jioSaavnService.searchSongs(query)
            }
            AudioSource.ALL -> {
                val jioDeferred = async { jioSaavnService.searchSongs(query) }
                val ytDeferred = async { pipedService.searchSongs(query) }

                val jioResults = jioDeferred.await()
                val ytResults = ytDeferred.await()

                // Interleave results so the user gets best of both catalogs
                val combined = mutableListOf<OnlineSong>()
                val maxLen = maxOf(jioResults.size, ytResults.size)
                for (i in 0 until maxLen) {
                    if (i < jioResults.size) combined.add(jioResults[i])
                    if (i < ytResults.size) combined.add(ytResults[i])
                }
                combined
            }
        }
        if (results.isNotEmpty()) {
            searchCache.put(cacheKey, results)
        }
        results
    }

    suspend fun getTrending(source: AudioSource): List<OnlineSong> {
        val cacheKey = "trending_${source.name}"
        searchCache.get(cacheKey)?.let { return it }

        val rawList = mutableListOf<OnlineSong>()

        val filteredResult = when (source) {
            AudioSource.YOUTUBE -> {
                val primary = pipedService.searchSongs("Top Global Hits 2026")
                rawList.addAll(primary)
                var filtered = trendingFilter.filterSongs(rawList)

                // If filtering removes too many items, try secondary queries with bounded retry
                if (filtered.size < trendingFilter.config.minResultsAfterFilter) {
                    val fallbacks = listOf("Trending Songs 2026", "Billboard Hot 100 2026")
                    for (fallback in fallbacks) {
                        val secondary = pipedService.searchSongs(fallback)
                        rawList.addAll(secondary)
                        filtered = trendingFilter.filterSongs(rawList)
                        if (filtered.size >= trendingFilter.config.minResultsAfterFilter) break
                    }
                }
                filtered
            }

            AudioSource.JIOSAAVN -> {
                val primary = jioSaavnService.getTrendingSongs()
                rawList.addAll(primary)
                var filtered = trendingFilter.filterSongs(rawList)

                if (filtered.size < trendingFilter.config.minResultsAfterFilter) {
                    val secondary = jioSaavnService.searchSongs("Latest Bollywood Hits", limit = 25)
                    rawList.addAll(secondary)
                    filtered = trendingFilter.filterSongs(rawList)
                }
                filtered
            }

            else -> {
                val jio = getTrending(AudioSource.JIOSAAVN)
                val yt = getTrending(AudioSource.YOUTUBE)
                val combined = mutableListOf<OnlineSong>()
                val maxLen = maxOf(jio.size, yt.size)
                for (i in 0 until maxLen) {
                    if (i < jio.size) combined.add(jio[i])
                    if (i < yt.size) combined.add(yt[i])
                }
                trendingFilter.deduplicate(combined)
            }
        }

        if (filteredResult.isNotEmpty()) {
            searchCache.put(cacheKey, filteredResult)
        }
        return filteredResult
    }

    suspend fun getQuickHits(source: AudioSource = AudioSource.ALL): List<OnlineSong> {
        val cacheKey = "quick_hits_${source.name}"
        searchCache.get(cacheKey)?.let { return it }

        val rawList = mutableListOf<OnlineSong>()

        val filteredResult = when (source) {
            AudioSource.YOUTUBE -> {
                val primary = pipedService.searchSongs("Viral Hits Radio 2026")
                rawList.addAll(primary)
                var filtered = trendingFilter.filterSongs(rawList)

                if (filtered.size < trendingFilter.config.minResultsAfterFilter) {
                    val fallbacks = listOf("Hot Hits Global 2026", "Top Pop Hits 2026")
                    for (fallback in fallbacks) {
                        val secondary = pipedService.searchSongs(fallback)
                        rawList.addAll(secondary)
                        filtered = trendingFilter.filterSongs(rawList)
                        if (filtered.size >= trendingFilter.config.minResultsAfterFilter) break
                    }
                }
                filtered
            }

            AudioSource.JIOSAAVN -> {
                val primary = jioSaavnService.searchSongs("Top New Songs 2026", limit = 25)
                rawList.addAll(primary)
                var filtered = trendingFilter.filterSongs(rawList)
                if (filtered.size < trendingFilter.config.minResultsAfterFilter) {
                    val secondary = jioSaavnService.searchSongs("Latest Hit Songs", limit = 25)
                    rawList.addAll(secondary)
                    filtered = trendingFilter.filterSongs(rawList)
                }
                filtered
            }

            else -> {
                val jio = getQuickHits(AudioSource.JIOSAAVN)
                val yt = getQuickHits(AudioSource.YOUTUBE)
                val combined = mutableListOf<OnlineSong>()
                val maxLen = maxOf(jio.size, yt.size)
                for (i in 0 until maxLen) {
                    if (i < jio.size) combined.add(jio[i])
                    if (i < yt.size) combined.add(yt[i])
                }
                trendingFilter.deduplicate(combined)
            }
        }

        if (filteredResult.isNotEmpty()) {
            searchCache.put(cacheKey, filteredResult)
        }
        return filteredResult
    }

    /**
     * Search real curated playlists through the existing JioSaavn playlist endpoint.
     * Used by Explore to load full soundtrack collections instead of grouping a handful
     * of unrelated song-search results by their album label.
     */
    suspend fun searchPlaylists(
        query: String,
        limit: Int = 15
    ): List<com.aman.auramusic.online.model.OnlinePlaylist> {
        return jioSaavnService.searchPlaylists(query, limit)
    }

    /** Search real YouTube Music album/playlist entities, not song results. */
    suspend fun searchYouTubeMusicCollections(
        query: String,
        limit: Int = 12
    ): List<com.aman.auramusic.online.model.OnlinePlaylist> =
        pipedService.searchMusicCollections(query, limit)

    /** Load tracks by the YouTube Music collection's browse ID. */
    suspend fun getYouTubeMusicCollectionSongs(
        collection: com.aman.auramusic.online.model.OnlinePlaylist
    ): List<OnlineSong> = pipedService.getMusicCollectionSongs(collection)
    suspend fun getCuratedPlaylists(source: AudioSource = AudioSource.ALL): List<com.aman.auramusic.online.model.OnlinePlaylist> {
        val now = System.currentTimeMillis()
        playlistsCache[source]?.let { (timestamp, cached) ->
            if (now - timestamp < CACHE_TTL_MS && cached.isNotEmpty()) {
                return cached
            }
        }

        val jioPlaylists = jioSaavnService.getCuratedPlaylists()
        
        // Curated YouTube & Spotify entries
        val extraPlaylists = listOf(
            com.aman.auramusic.online.model.OnlinePlaylist(
                id = "yt_trending",
                title = "YouTube Music Top 50",
                subtitle = "YouTube Music • Global Trending",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1080&q=85",
                songCount = 50,
                source = AudioSource.YOUTUBE
            ),
            com.aman.auramusic.online.model.OnlinePlaylist(
                id = "spotify_top_global",
                title = "Spotify: Today's Top Hits",
                subtitle = "Spotify • Chart Toppers",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=1080&q=85",
                songCount = 50,
                source = AudioSource.SPOTIFY
            )
        )

        val combined = when (source) {
            AudioSource.JIOSAAVN -> jioPlaylists
            AudioSource.YOUTUBE -> extraPlaylists.filter { it.source == AudioSource.YOUTUBE }
            AudioSource.SPOTIFY -> jioPlaylists.filter { it.source == AudioSource.SPOTIFY } + extraPlaylists.filter { it.source == AudioSource.SPOTIFY }
            AudioSource.ALL -> extraPlaylists + jioPlaylists
        }.distinctBy { it.title }

        if (combined.isNotEmpty()) {
            playlistsCache[source] = now to combined
        }
        return combined
    }

    suspend fun getPlaylistSongs(playlist: com.aman.auramusic.online.model.OnlinePlaylist): List<OnlineSong> {
        playlistSongsCache.get(playlist.id)?.let { return it }

        val songs = if (playlist.id.contains("ytmusic:") || (playlist.source == AudioSource.YOUTUBE && !playlist.id.startsWith("yt_"))) {
            pipedService.getMusicCollectionSongs(playlist)
        } else if (playlist.id.startsWith("yt_")) {
            pipedService.searchSongs(playlist.title)
        } else if (playlist.id.startsWith("spotify_")) {
            jioSaavnService.searchSongs(playlist.title.replace("Spotify:", "").trim(), limit = 25)
        } else {
            val list = jioSaavnService.getPlaylistDetails(playlist.id)
            if (list.isEmpty()) {
                jioSaavnService.searchSongs(playlist.title, limit = 20)
            } else list
        }

        if (songs.isNotEmpty()) {
            playlistSongsCache.put(playlist.id, songs)
        }
        return songs
    }

    suspend fun getCuratedSongFeed(): List<OnlineSong> = coroutineScope {
        val now = System.currentTimeMillis()
        songFeedCache?.let { (timestamp, cached) ->
            if (now - timestamp < CACHE_TTL_MS && cached.isNotEmpty()) {
                return@coroutineScope cached
            }
        }

        val jioDeferred = async { jioSaavnService.getTrendingSongs() }
        val ytDeferred = async { pipedService.searchSongs("Billboard Hot 100 2026") }

        val rawJio = jioDeferred.await()
        val rawYt = ytDeferred.await()

        val jio = trendingFilter.filterSongs(rawJio)
        val yt = trendingFilter.filterSongs(rawYt)

        val list = mutableListOf<OnlineSong>()
        val maxLen = maxOf(jio.size, yt.size)
        for (i in 0 until maxLen) {
            if (i < jio.size) list.add(jio[i])
            if (i < yt.size) list.add(yt[i])
        }

        val cleanList = trendingFilter.deduplicate(list)
        if (cleanList.isNotEmpty()) {
            songFeedCache = now to cleanList
        }
        cleanList
    }

    suspend fun resolveStream(song: OnlineSong, targetBitrate: String = "320"): OnlineSong {
        val cacheKey = "${song.id}_$targetBitrate"
        streamCache.get(cacheKey)?.let { return it }

        val resolved = if (song.source == AudioSource.JIOSAAVN) {
            val res = jioSaavnService.resolveStream(song, targetBitrate)
            res.copy(
                extractorTier = "JioSaavn 320k Direct CDN",
                audioCodec = "AAC-LC ${targetBitrate}k"
            )
        } else {
            dualTierExtractor.resolveStream(song)
        }

        if (resolved.streamUrl.isNotBlank()) {
            streamCache.put(cacheKey, resolved)
        }
        return resolved
    }
}
