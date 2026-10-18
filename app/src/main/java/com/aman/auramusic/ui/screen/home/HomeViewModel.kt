package com.aman.auramusic.ui.screen.home

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.filter.RecommendationDeduplicator
import com.aman.auramusic.online.filter.ExplicitTitleFilter
import com.aman.auramusic.online.filter.SectionDeduplicator
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.util.formatDuration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

sealed class QuickHitTrack {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    abstract val artworkModel: Any?
    abstract val sourceBadge: String
    abstract val durationText: String

    data class LocalTrack(
        val song: Song,
        override val sourceBadge: String = "Local"
    ) : QuickHitTrack() {
        override val id: String get() = "local_${song.id}"
        override val title: String get() = song.title
        override val artist: String get() = song.artist
        override val artworkModel: Any? get() = song.artworkUri
        override val durationText: String get() = formatDuration(song.duration)
    }

    data class OnlineTrack(
        val onlineSong: OnlineSong,
        override val sourceBadge: String = "Trending"
    ) : QuickHitTrack() {
        override val id: String get() = "online_${onlineSong.source.name}_${onlineSong.id}"
        override val title: String get() = onlineSong.title
        override val artist: String get() = onlineSong.artist
        override val artworkModel: Any? get() = onlineSong.artworkUrl
        override val durationText: String get() = onlineSong.durationFormatted
    }
}

data class TrendingSongItem(
    val onlineSong: OnlineSong,
    val localMatch: Song? = null,
    val isLocalAvailable: Boolean = localMatch != null
)

data class ArtistItem(
    val name: String,
    val songCountText: String = "",
    val artworkModel: Any? = null,
    val isOnline: Boolean = false,
    val artistId: String = ""
)

data class ArtistProfileData(
    val onlineSongs: List<OnlineSong> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val onlineAlbums: List<OnlinePlaylist> = emptyList(),
    val audienceText: String? = null,
    val similarArtists: List<ArtistItem> = emptyList(),
    val bio: com.aman.auramusic.online.network.repository.ArtistBioData? = null,
    val loadErrorMessage: String? = null
)

data class CategoryDiscoverItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val searchQuery: String,
    val gradientColors: List<Color> = emptyList()
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val featuredItem: DailyFeaturedItem? = null,
    val featuredTracks: List<OnlineSong> = emptyList(),
    val listenAgainTracks: List<QuickHitTrack> = emptyList(),
    val listenAgainColumns: List<List<QuickHitTrack>> = emptyList(),
    val trendingTracks: List<TrendingSongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val artistSearchQuery: String = "",
    val isArtistSearching: Boolean = false,
    val quickHitsColumns: List<List<QuickHitTrack>> = emptyList(),
    val categories: List<CategoryDiscoverItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val onlineRepository: OnlineMusicRepository,
    private val youtubeArtistRepository: com.aman.auramusic.online.network.repository.YouTubeArtistRepository
) : AndroidViewModel(application) {

    private val dailyCache = DailyFeaturedCache(application.applicationContext)
    private val feedCachePrefs = application.getSharedPreferences("aura_home_feed_cache", android.app.Application.MODE_PRIVATE)
    private val feedCacheTtlMs = 24L * 60L * 60L * 1000L // 24-hour freshness policy
    private var isFetchingFeed = false

    private inline fun <reified T> readCachedList(key: String): List<T> = runCatching {
        Gson().fromJson<List<T>>(
            feedCachePrefs.getString(key, null),
            object : TypeToken<List<T>>() {}.type
        ).orEmpty()
    }.getOrDefault(emptyList())

    private fun writeCachedList(key: String, value: Any) {
        feedCachePrefs.edit().putString(key, Gson().toJson(value)).apply()
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedTrendingOnline = emptyList<OnlineSong>()
    private var cachedQuickHitsOnline = emptyList<OnlineSong>()
    private var cachedFeaturedTracks = emptyList<OnlineSong>()
    private var cachedDiscoveredArtists = emptyList<com.aman.auramusic.online.network.repository.YouTubeArtist>()
    private val localArtistMetadataCache = java.util.concurrent.ConcurrentHashMap<String, com.aman.auramusic.online.network.repository.YouTubeArtist>()
    private val attemptedArtistLookups = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val defaultCategories = listOf(
        CategoryDiscoverItem(
            id = "pop",
            title = "Pop",
            subtitle = "Global Chart Toppers",
            searchQuery = "Pop Hits"
        ),
        CategoryDiscoverItem(
            id = "hiphop",
            title = "Hip Hop",
            subtitle = "Beats & Rhymes",
            searchQuery = "Hip Hop Rap Hits"
        ),
        CategoryDiscoverItem(
            id = "electronic",
            title = "Electronic",
            subtitle = "Dance, Synth & Bass",
            searchQuery = "Electronic EDM Dance"
        ),
        CategoryDiscoverItem(
            id = "rock",
            title = "Rock",
            subtitle = "Classics & Modern Alt",
            searchQuery = "Rock Alternative Hits"
        ),
        CategoryDiscoverItem(
            id = "rnb",
            title = "R&B / Soul",
            subtitle = "Smooth & Melodic",
            searchQuery = "RnB Soul Melodic"
        ),
        CategoryDiscoverItem(
            id = "chill",
            title = "Chill",
            subtitle = "Lo-Fi & Relaxing Vibes",
            searchQuery = "Lofi Chill Beats"
        ),
        CategoryDiscoverItem(
            id = "happy",
            title = "Happy",
            subtitle = "Feel-Good Anthems",
            searchQuery = "Happy Upbeat Summer"
        ),
        CategoryDiscoverItem(
            id = "sad",
            title = "Sad",
            subtitle = "Moody & Melancholic",
            searchQuery = "Sad Melancholic Acoustic"
        ),
        CategoryDiscoverItem(
            id = "desi",
            title = "Bollywood",
            subtitle = "Desi Hits & Melodies",
            searchQuery = "Bollywood Top Hits"
        ),
        CategoryDiscoverItem(
            id = "indie",
            title = "Indie",
            subtitle = "Acoustic & Independent",
            searchQuery = "Indie Folk Acoustic"
        )
    )

    init {
        _uiState.value = _uiState.value.copy(categories = defaultCategories)
    }

    private fun buildListenAgainFeed(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>
    ): List<List<QuickHitTrack>> {
        val gson = Gson()
        val seenTrackKeys = mutableSetOf<String>()
        val result = mutableListOf<QuickHitTrack>()

        for (entry in history) {
            if (!entry.onlineSongJson.isNullOrBlank()) {
                val onlineSong = runCatching {
                    gson.fromJson(entry.onlineSongJson, OnlineSong::class.java)
                }.getOrNull()
                if (onlineSong != null) {
                    val idKey = "id:${onlineSong.id.trim()}"
                    val normTitle = RecommendationDeduplicator.normalizeText(onlineSong.title)
                    val normArtist = RecommendationDeduplicator.normalizeText(onlineSong.artist)
                    val metaKey = "meta:$normTitle|$normArtist"
                    if (seenTrackKeys.add(idKey) && seenTrackKeys.add(metaKey)) {
                        result.add(QuickHitTrack.OnlineTrack(onlineSong, "History"))
                    }
                }
            } else {
                val localSong = localSongs.find { it.id == entry.songId }
                if (localSong != null) {
                    val idKey = "local:${localSong.id}"
                    val normTitle = RecommendationDeduplicator.normalizeText(localSong.title)
                    val normArtist = RecommendationDeduplicator.normalizeText(localSong.artist)
                    val metaKey = "meta:$normTitle|$normArtist"
                    if (seenTrackKeys.add(idKey) && seenTrackKeys.add(metaKey)) {
                        result.add(QuickHitTrack.LocalTrack(localSong, "History"))
                    }
                }
            }
            if (result.size >= 24) break
        }
        return result.chunked(2)
    }

    fun loadData(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>,
        forceRefresh: Boolean = false
    ) {
        if (isFetchingFeed && !forceRefresh) return
        viewModelScope.launch {
            isFetchingFeed = true
            try {
                val recommendationLocalSongs = localSongs.filterNot {
                    ExplicitTitleFilter.shouldHideTitle(it.title)
                }
                val listenAgainColumns = buildListenAgainFeed(recommendationLocalSongs, history)

                if (_uiState.value.trendingTracks.isNotEmpty() && !forceRefresh) {
                    updateLocalDerivedData(recommendationLocalSongs, history)
                    _uiState.value = _uiState.value.copy(
                        listenAgainColumns = listenAgainColumns,
                        listenAgainTracks = listenAgainColumns.flatten()
                    )
                    return@launch
                }

            val deduplicator = SectionDeduplicator()

            val initialArtists = buildArtistList(localSongs, cachedDiscoveredArtists)
            val initialQuickHits = buildQuickHitsFeed(
                localSongs = recommendationLocalSongs,
                history = history,
                onlineTracks = ExplicitTitleFilter.filterOnline(cachedQuickHitsOnline),
                deduplicator = null
            )

            _uiState.value = _uiState.value.copy(
                artists = if (_uiState.value.artists.isEmpty()) initialArtists else _uiState.value.artists,
                quickHitsColumns = if (_uiState.value.quickHitsColumns.isEmpty()) initialQuickHits else _uiState.value.quickHitsColumns,
                listenAgainColumns = listenAgainColumns,
                listenAgainTracks = listenAgainColumns.flatten(),
                isLoading = _uiState.value.trendingTracks.isEmpty(),
                isRefreshing = forceRefresh,
                errorMessage = null
            )
            resolveMissingArtistPortraits(initialArtists)

            val currentDayKey = DailyFeaturedCache.getCurrentDayKey()
            var featured = dailyCache.getDailyFeatured(currentDayKey)

            var isOfflineDetected = false
            var rawTrendingOnline = emptyList<OnlineSong>()
            var rawQuickHitsOnline = emptyList<OnlineSong>()
            var curatedPlaylists = emptyList<OnlinePlaylist>()
            var onlineArtistsDiscovered = emptyList<com.aman.auramusic.online.network.repository.YouTubeArtist>()
            var featuredTracks = emptyList<OnlineSong>()

            val cachedAt = feedCachePrefs.getLong("fetched_at", 0L)
            val feedCacheFresh = !forceRefresh && cachedAt > 0L &&
                System.currentTimeMillis() - cachedAt in 0..feedCacheTtlMs

            val cachedTrending: List<OnlineSong> = readCachedList("trending")
            val cachedQuickHits: List<OnlineSong> = readCachedList("quick_hits")
            val cachedPlaylists: List<OnlinePlaylist> = readCachedList("playlists")
            val cachedArtists: List<com.aman.auramusic.online.network.repository.YouTubeArtist> = readCachedList("artists")
            val cachedFeatured: List<OnlineSong> = readCachedList("featured_tracks")

            if (cachedTrending.isNotEmpty() || cachedQuickHits.isNotEmpty() || cachedPlaylists.isNotEmpty()) {
                rawTrendingOnline = cachedTrending
                rawQuickHitsOnline = cachedQuickHits
                curatedPlaylists = cachedPlaylists
                onlineArtistsDiscovered = cachedArtists
                featuredTracks = cachedFeatured
            }

            val cacheUsable = feedCacheFresh && (rawTrendingOnline.isNotEmpty() || rawQuickHitsOnline.isNotEmpty())

            if (!cacheUsable) {
                try {
                    withContext(Dispatchers.IO) {
                        val playlistsDeferred = async {
                            try {
                                onlineRepository.getCuratedPlaylists(AudioSource.ALL)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        val trendingDeferred = async {
                            try {
                                onlineRepository.getTrending(AudioSource.YOUTUBE)
                            } catch (_: Exception) {
                                try {
                                    onlineRepository.getTrending(AudioSource.JIOSAAVN)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                        }

                        val quickHitsDeferred = async {
                            try {
                                onlineRepository.getQuickHits(AudioSource.ALL)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        val ytArtistsDeferred = async {
                            try {
                                youtubeArtistRepository.getTrendingArtists()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        curatedPlaylists = playlistsDeferred.await()
                        rawTrendingOnline = trendingDeferred.await()
                        rawQuickHitsOnline = quickHitsDeferred.await()
                        onlineArtistsDiscovered = ytArtistsDeferred.await()
                    }
                } catch (e: Exception) {
                    isOfflineDetected = true
                }

                if (rawTrendingOnline.isEmpty() && rawQuickHitsOnline.isEmpty() &&
                    curatedPlaylists.isEmpty() && onlineArtistsDiscovered.isEmpty()) {
                    rawTrendingOnline = readCachedList("trending")
                    rawQuickHitsOnline = readCachedList("quick_hits")
                    curatedPlaylists = readCachedList("playlists")
                    onlineArtistsDiscovered = readCachedList("artists")
                    if (featuredTracks.isEmpty()) {
                        featuredTracks = readCachedList("featured_tracks")
                    }
                }
            }

            if (featured == null) {
                featured = resolveDailyFeatured(
                    dayKey = currentDayKey,
                    onlinePlaylists = curatedPlaylists,
                    localSongs = localSongs
                )
                if (featured != null) {
                    dailyCache.saveDailyFeatured(featured)
                }
            }

            if (!cacheUsable && featured?.onlinePlaylistId != null && featuredTracks.isEmpty()) {
                val plObj = curatedPlaylists.find { it.id == featured.onlinePlaylistId }
                    ?: OnlinePlaylist(id = featured.onlinePlaylistId!!, title = featured.title, source = AudioSource.ALL)
                withContext(Dispatchers.IO) {
                    featuredTracks = try {
                        val tracks = onlineRepository.getPlaylistSongs(plObj)
                        onlineRepository.trendingFilter.filterSongs(tracks)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            deduplicator.clear()

            val cleanFeaturedTracks = deduplicator.allocateAll(ExplicitTitleFilter.filterOnline(featuredTracks))
            if (featured?.albumName != null) {
                val albumSongs = localSongs.filter { it.album.equals(featured.albumName, ignoreCase = true) }
                deduplicator.allocateAllLocal(albumSongs)
            }

            var cleanTrending = ExplicitTitleFilter.filterOnline(rawTrendingOnline).filter { deduplicator.allocate(it) }
            if (!cacheUsable && cleanTrending.size < 10) {
                withContext(Dispatchers.IO) {
                    val secondary = try {
                        onlineRepository.search("Billboard Hot 100 2026", AudioSource.YOUTUBE)
                    } catch (_: Exception) { emptyList() }
                    val cleanSecondary = ExplicitTitleFilter.filterOnline(onlineRepository.trendingFilter.filterSongs(secondary))
                        .filter { deduplicator.allocate(it) }
                    cleanTrending = (cleanTrending + cleanSecondary).take(20)
                    rawTrendingOnline = (rawTrendingOnline + secondary).distinctBy { "${it.source.name}_${it.id}" }
                }
            }

            if (!cacheUsable && (rawTrendingOnline.isNotEmpty() || rawQuickHitsOnline.isNotEmpty() || curatedPlaylists.isNotEmpty() || onlineArtistsDiscovered.isNotEmpty())) {
                feedCachePrefs.edit().putLong("fetched_at", System.currentTimeMillis()).apply()
                writeCachedList("trending", rawTrendingOnline)
                writeCachedList("quick_hits", rawQuickHitsOnline)
                writeCachedList("playlists", curatedPlaylists)
                writeCachedList("artists", onlineArtistsDiscovered)
                writeCachedList("featured_tracks", featuredTracks)
            }

            val trendingItems = cleanTrending.map { onlineSong ->
                val match = localSongs.find { local ->
                    local.title.trim().equals(onlineSong.title.trim(), ignoreCase = true) ||
                            (local.title.contains(onlineSong.title, ignoreCase = true) &&
                                    local.artist.contains(onlineSong.artist, ignoreCase = true))
                }
                TrendingSongItem(
                    onlineSong = onlineSong,
                    localMatch = match,
                    isLocalAvailable = match != null
                )
            }

            val artistsList = buildArtistList(localSongs, onlineArtistsDiscovered)

            val cleanQuickHitsOnline = ExplicitTitleFilter.filterOnline(rawQuickHitsOnline).filter { deduplicator.allocate(it) }
            val quickHitsColumns = buildQuickHitsFeed(
                localSongs = recommendationLocalSongs,
                history = history,
                onlineTracks = cleanQuickHitsOnline,
                deduplicator = deduplicator
            )

            cachedTrendingOnline = cleanTrending
            cachedQuickHitsOnline = cleanQuickHitsOnline
            cachedFeaturedTracks = cleanFeaturedTracks
            cachedDiscoveredArtists = onlineArtistsDiscovered

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isOffline = isOfflineDetected && trendingItems.isEmpty(),
                errorMessage = if (isOfflineDetected && trendingItems.isEmpty()) "Offline — Showing local library" else null,
                featuredItem = featured,
                featuredTracks = cleanFeaturedTracks,
                listenAgainTracks = listenAgainColumns.flatten(),
                listenAgainColumns = listenAgainColumns,
                trendingTracks = trendingItems,
                artists = artistsList,
                quickHitsColumns = quickHitsColumns,
                categories = defaultCategories
            )
            resolveMissingArtistPortraits(artistsList)

            if (!isOfflineDetected && (rawTrendingOnline.isNotEmpty() || rawQuickHitsOnline.isNotEmpty() || curatedPlaylists.isNotEmpty())) {
                writeCachedList("trending", rawTrendingOnline)
                writeCachedList("quick_hits", rawQuickHitsOnline)
                writeCachedList("playlists", curatedPlaylists)
                writeCachedList("artists", onlineArtistsDiscovered)
                if (featuredTracks.isNotEmpty()) {
                    writeCachedList("featured_tracks", featuredTracks)
                }
                feedCachePrefs.edit().putLong("fetched_at", System.currentTimeMillis()).apply()
            }
        } finally {
            isFetchingFeed = false
        }
    }
}

    private fun updateLocalDerivedData(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>
    ) {
        val artistsList = buildArtistList(
            localSongs = localSongs,
            discoveredOnline = cachedDiscoveredArtists
        )

        val quickHitsColumns = buildQuickHitsFeed(
            localSongs = localSongs,
            history = history,
            onlineTracks = cachedQuickHitsOnline,
            deduplicator = null
        )

        val listenAgainColumns = buildListenAgainFeed(localSongs, history)

        _uiState.value = _uiState.value.copy(
            artists = artistsList,
            quickHitsColumns = quickHitsColumns,
            listenAgainColumns = listenAgainColumns,
            listenAgainTracks = listenAgainColumns.flatten()
        )
        resolveMissingArtistPortraits(artistsList)
    }

    private fun extractSingleArtistNames(raw: String): List<String> =
        com.aman.auramusic.util.ArtistHelper.extractArtistNames(raw)

    private fun buildArtistList(
        localSongs: List<Song>,
        discoveredOnline: List<com.aman.auramusic.online.network.repository.YouTubeArtist>
    ): List<ArtistItem> {
        val localArtistMap = linkedMapOf<String, Pair<String, MutableList<Song>>>()
        fun localArtistKey(value: String): String =
            value.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)

        localSongs.forEach { song ->
            extractSingleArtistNames(song.artist).forEach { rawName ->
                val artistName = rawName.trim().replace(Regex("\\s+"), " ")
                val key = localArtistKey(artistName)
                if (key.isNotBlank()) {
                    val entry = localArtistMap.getOrPut(key) { artistName to mutableListOf() }
                    if (entry.second.none { it.id == song.id }) entry.second.add(song)
                }
            }
        }

        val localArtists = localArtistMap.values
            .sortedByDescending { it.second.size }
            .map { (artistName, songList) ->
                val cached = localArtistMetadataCache[normalizeArtistComparable(artistName)]
                val onlineMatch = discoveredOnline.firstOrNull {
                    matchesArtistName(it.name, artistName) && !it.profileImageUrl.isNullOrBlank()
                }
                val portraitUrl = (cached?.profileImageUrl ?: onlineMatch?.profileImageUrl ?: com.aman.auramusic.util.ArtistArtworkCache.get(artistName))?.takeIf { it.isNotBlank() }
                ArtistItem(
                    name = artistName,
                    songCountText = "${songList.size} songs",
                    artworkModel = portraitUrl,
                    isOnline = false,
                    artistId = "local_${artistName.hashCode()}"
                )
            }

        val onlineArtists = discoveredOnline.mapNotNull { ytArtist ->
            val clean = extractSingleArtistNames(ytArtist.name).firstOrNull()
            if (!clean.isNullOrBlank()) {
                ArtistItem(
                    name = clean,
                    songCountText = ytArtist.subscriberCountText ?: "YouTube Music",
                    artworkModel = ytArtist.profileImageUrl,
                    isOnline = true,
                    artistId = ytArtist.id
                )
            } else null
        }

        val seen = mutableSetOf<String>()
        val balanced = mutableListOf<ArtistItem>()
        var localIdx = 0
        var onlineIdx = 0
        val maxTotal = 24

        while (balanced.size < maxTotal && (localIdx < localArtists.size || onlineIdx < onlineArtists.size)) {
            while (localIdx < localArtists.size) {
                val candidate = localArtists[localIdx++]
                if (seen.add(candidate.name.lowercase())) {
                    balanced.add(candidate)
                    break
                }
            }

            while (onlineIdx < onlineArtists.size && balanced.size < maxTotal) {
                val candidate = onlineArtists[onlineIdx++]
                if (seen.add(candidate.name.lowercase())) {
                    balanced.add(candidate)
                    break
                }
            }
        }

        while (balanced.size < maxTotal && localIdx < localArtists.size) {
            val candidate = localArtists[localIdx++]
            if (seen.add(candidate.name.lowercase())) balanced.add(candidate)
        }
        while (balanced.size < maxTotal && onlineIdx < onlineArtists.size) {
            val candidate = onlineArtists[onlineIdx++]
            if (seen.add(candidate.name.lowercase())) balanced.add(candidate)
        }

        return balanced
    }

    private fun buildQuickHitsFeed(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>,
        onlineTracks: List<OnlineSong>,
        deduplicator: SectionDeduplicator? = null
    ): List<List<QuickHitTrack>> {
        val historySongs = history.mapNotNull { entry -> localSongs.find { it.id == entry.songId } }.distinct()
        val allLocalPool = if (historySongs.isNotEmpty()) {
            (historySongs + (localSongs - historySongs.toSet())).distinctBy { it.id }
        } else {
            localSongs
        }

        val filteredLocal = if (deduplicator != null) {
            allLocalPool.filter { deduplicator.allocate(it) }
        } else {
            allLocalPool
        }

        val localTracks = filteredLocal.map { song ->
            val badge = if (historySongs.contains(song)) "Recent Hit" else "Local"
            QuickHitTrack.LocalTrack(song, badge)
        }

        val categoryBadges = listOf("Pop Hit", "Top Chart", "Billboard", "Rock Hit", "Bollywood", "Electronic", "Quick Hit")
        val formattedOnlineTracks = onlineTracks.mapIndexed { index, os ->
            val badge = categoryBadges[index % categoryBadges.size]
            QuickHitTrack.OnlineTrack(os, badge)
        }

        val unified = mutableListOf<QuickHitTrack>()
        var localIdx = 0
        var onlineIdx = 0

        val targetCount = if (localTracks.isEmpty() && formattedOnlineTracks.isEmpty()) {
            0
        } else {
            maxOf(24, localTracks.size + formattedOnlineTracks.size).coerceAtMost(36)
        }

        while (unified.size < targetCount && (localIdx < localTracks.size || onlineIdx < formattedOnlineTracks.size)) {
            if (localIdx < localTracks.size && unified.size < targetCount) {
                unified.add(localTracks[localIdx++])
            }
            if (onlineIdx < formattedOnlineTracks.size && unified.size < targetCount) {
                unified.add(formattedOnlineTracks[onlineIdx++])
            }
            if (onlineIdx < formattedOnlineTracks.size && unified.size < targetCount) {
                unified.add(formattedOnlineTracks[onlineIdx++])
            }
            if (localIdx >= localTracks.size && onlineIdx < formattedOnlineTracks.size) {
                unified.add(formattedOnlineTracks[onlineIdx++])
            }
            if (onlineIdx >= formattedOnlineTracks.size && localIdx < localTracks.size) {
                unified.add(localTracks[localIdx++])
            }
        }

        return unified.chunked(4)
    }

    private fun resolveDailyFeatured(
        dayKey: String,
        onlinePlaylists: List<OnlinePlaylist>,
        localSongs: List<Song>
    ): DailyFeaturedItem? {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        if (onlinePlaylists.isNotEmpty()) {
            val selected = onlinePlaylists[dayOfYear % onlinePlaylists.size]
            return DailyFeaturedItem(
                id = selected.id,
                title = selected.title,
                subtitle = selected.subtitle,
                badgeLabel = "DAILY FEATURED PLAYLIST",
                artworkUrl = selected.artworkUrl,
                isOnline = true,
                onlinePlaylistId = selected.id,
                songCountText = "${selected.songCount} Songs",
                dayKey = dayKey
            )
        }

        val albums = localSongs.groupBy { it.album.trim() }
            .filter { it.key.isNotBlank() && it.key != "Unknown Album" }
            .entries.toList()

        if (albums.isNotEmpty()) {
            val selectedAlbum = albums[dayOfYear % albums.size]
            val firstSong = selectedAlbum.value.firstOrNull()
            return DailyFeaturedItem(
                id = "album_${selectedAlbum.key.hashCode()}",
                title = selectedAlbum.key,
                subtitle = firstSong?.artist ?: "Various Artists",
                badgeLabel = "DAILY FEATURED ALBUM",
                artworkUrl = firstSong?.artworkUri,
                isOnline = false,
                albumName = selectedAlbum.key,
                songCountText = "${selectedAlbum.value.size} Tracks",
                dayKey = dayKey
            )
        }

        return null
    }

    suspend fun fetchCategorySongs(
        category: CategoryDiscoverItem,
        localSongs: List<Song>
    ): Pair<List<OnlineSong>, List<Song>> = withContext(Dispatchers.IO) {
        val onlineResults = try {
            val raw = onlineRepository.search(category.searchQuery, AudioSource.ALL)
            ExplicitTitleFilter.filterOnline(raw)
                .distinctBy { "${it.source.name}:${it.id}" }
                .take(40)
        } catch (_: Exception) {
            emptyList()
        }

        val keywords = when (category.id.lowercase()) {
            "pop" -> listOf("pop", "chart", "dance", "radio", "singer", "love", "weeknd", "swift", "bieber", "lipa", "ariana")
            "hiphop" -> listOf("hip hop", "hip-hop", "rap", "hiphop", "trap", "drill", "snoop", "eminem", "drake", "kendrick", "tupac", "50 cent")
            "electronic" -> listOf("electronic", "edm", "dance", "house", "techno", "trance", "bass", "dj", "remix")
            "rock" -> listOf("rock", "metal", "punk", "alternative", "grunge", "guitar", "indie rock")
            "rnb" -> listOf("r&b", "rnb", "soul", "neo soul", "rhythm and blues", "usher", "rihanna", "beyonce")
            "chill" -> listOf("lofi", "lo-fi", "chill", "ambient", "relax", "sleep", "acoustic", "instrumental")
            "happy" -> listOf("happy", "upbeat", "party", "summer", "feel good", "dance", "celebration")
            "sad" -> listOf("sad", "melancholy", "heartbreak", "acoustic", "lonely", "pain", "cry", "emotional")
            "desi" -> listOf("bollywood", "hindi", "punjabi", "desi", "indian", "filmi", "t-series", "zee music")
            "indie" -> listOf("indie", "independent", "folk", "singer-songwriter", "alternative", "acoustic")
            else -> listOf(category.title.lowercase())
        }
        val localFiltered = localSongs.asSequence()
            .filterNot { ExplicitTitleFilter.shouldHideTitle(it.title) }
            .filter { song ->
                val metadata = "${song.title} ${song.artist} ${song.album}".lowercase()
                keywords.any { keyword -> metadata.contains(keyword) }
            }
            .distinctBy { it.id }
            .take(40)
            .toList()

        Pair(onlineResults, localFiltered)
    }

    fun searchArtists(query: String, localSongs: List<Song>) {
        val q = query.trim()
        _uiState.value = _uiState.value.copy(artistSearchQuery = q)
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(
                artists = buildArtistList(localSongs, cachedDiscoveredArtists),
                isArtistSearching = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isArtistSearching = true)
            val onlineResults = try {
                youtubeArtistRepository.searchArtists(q)
            } catch (_: Exception) {
                emptyList()
            }

            val localMatching = localSongs.flatMap { extractSingleArtistNames(it.artist) }
                .distinctBy { it.lowercase() }
                .filter { it.contains(q, ignoreCase = true) }
                .map { name ->
                    val matchingSongs = localSongs.filter { it.artist.contains(name, ignoreCase = true) }
                    val matchingOnline = onlineResults.firstOrNull {
                        matchesArtistName(it.name, name) && !it.profileImageUrl.isNullOrBlank()
                    } ?: cachedDiscoveredArtists.firstOrNull {
                        matchesArtistName(it.name, name) && !it.profileImageUrl.isNullOrBlank()
                    } ?: localArtistMetadataCache[normalizeArtistComparable(name)]
                    if (matchingOnline != null && !matchingOnline.profileImageUrl.isNullOrBlank()) {
                        localArtistMetadataCache[normalizeArtistComparable(name)] = matchingOnline
                    }
                    ArtistItem(
                        name = name,
                        songCountText = "${matchingSongs.size} songs",
                        artworkModel = matchingOnline?.profileImageUrl?.takeIf { it.isNotBlank() },
                        isOnline = false,
                        artistId = "local_${name.hashCode()}"
                    )
                }

            val onlineItems = onlineResults.map { yt ->
                ArtistItem(
                    name = yt.name,
                    songCountText = yt.subscriberCountText ?: "YouTube Music",
                    artworkModel = yt.profileImageUrl?.takeIf { it.isNotBlank() },
                    isOnline = true,
                    artistId = yt.id
                )
            }

            val merged = (localMatching + onlineItems).distinctBy { it.name.lowercase() }
            _uiState.value = _uiState.value.copy(
                artists = merged,
                isArtistSearching = false
            )
        }
    }

    suspend fun fetchArtistCatalogue(
        artist: ArtistItem,
        localSongs: List<Song>
    ): ArtistProfileData = withContext(Dispatchers.IO) {
        val artistName = artist.name.trim()
        val selectedArtistKey = artistName.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
        val localFiltered = localSongs.filter { song ->
            extractSingleArtistNames(song.artist).any { creditedArtist ->
                creditedArtist.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT) == selectedArtistKey
            }
        }.distinctBy { it.id }

        val resolvedBrowseId = if (artist.isOnline &&
            artist.artistId.isNotBlank() &&
            !artist.artistId.startsWith("local_") &&
            !artist.artistId.startsWith("yt_artist_")
        ) {
            artist.artistId
        } else {
            val cached = localArtistMetadataCache[normalizeArtistComparable(artistName)]
                ?: cachedDiscoveredArtists.firstOrNull { matchesArtistName(it.name, artistName) }
            if (cached != null && cached.id.isNotBlank() && !cached.id.startsWith("local_") && !cached.id.startsWith("yt_artist_")) {
                cached.id
            } else {
                try {
                    val found = youtubeArtistRepository.searchArtists(artistName)
                        .firstOrNull { matchesArtistName(it.name, artistName) }
                    if (found != null && found.id.isNotBlank() && !found.id.startsWith("local_") && !found.id.startsWith("yt_artist_")) {
                        localArtistMetadataCache[normalizeArtistComparable(artistName)] = found
                        found.id
                    } else null
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
            }
        }

        // The repository also has a strict, artist-verified search fallback for
        // local artists that do not resolve to a YouTube Music browse ID.
        val catalogue = try {
            youtubeArtistRepository.getArtistCatalogue(artistName, resolvedBrowseId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            com.aman.auramusic.online.network.repository.ArtistCatalogue(
                loadErrorMessage = "Unable to load this artist's catalogue. Check your connection and try again."
            )
        }

        val audienceText = catalogue.subscriberCountText?.takeIf { it.isNotBlank() }
            ?: (localArtistMetadataCache[normalizeArtistComparable(artistName)] ?: cachedDiscoveredArtists.firstOrNull { matchesArtistName(it.name, artistName) })?.subscriberCountText?.takeIf { it.isNotBlank() }
            ?: (if (artist.isOnline && artist.songCountText.isNotBlank()) artist.songCountText else null)

        val similarArtistItems = catalogue.similarArtists.map {
            ArtistItem(
                name = it.name,
                songCountText = it.subscriberCountText.orEmpty(),
                artworkModel = it.profileImageUrl,
                isOnline = true,
                artistId = it.id
            )
        }

        ArtistProfileData(
            onlineSongs = catalogue.songs.filter { it.source == AudioSource.YOUTUBE }.distinctBy { it.id },
            localSongs = localFiltered,
            onlineAlbums = catalogue.albums,
            audienceText = audienceText,
            similarArtists = similarArtistItems,
            bio = catalogue.bio,
            loadErrorMessage = catalogue.loadErrorMessage
        )
    }

    suspend fun resolveArtistForLibrary(artist: ArtistItem): com.aman.auramusic.online.model.SavedOnlineArtist? =
        withContext(Dispatchers.IO) {
            val name = artist.name.trim()
            if (name.isBlank()) return@withContext null

            val existing = if (artist.artistId.isNotBlank() &&
                !artist.artistId.startsWith("local_") &&
                !artist.artistId.startsWith("yt_artist_")
            ) {
                cachedDiscoveredArtists.firstOrNull { it.id == artist.artistId }
                    ?: localArtistMetadataCache.values.firstOrNull { it.id == artist.artistId }
                    ?: com.aman.auramusic.online.network.repository.YouTubeArtist(
                        id = artist.artistId,
                        name = artist.name,
                        profileImageUrl = artist.artworkModel as? String
                    )
            } else {
                localArtistMetadataCache[normalizeArtistComparable(name)]
                    ?: cachedDiscoveredArtists.firstOrNull { matchesArtistName(it.name, name) }
            }

            val resolved = existing ?: runCatching {
                youtubeArtistRepository.searchArtists(name)
                    .firstOrNull { matchesArtistName(it.name, name) }
            }.getOrNull() ?: return@withContext null

            if (resolved.id.isBlank() || resolved.id.startsWith("local_") || resolved.id.startsWith("yt_artist_")) {
                return@withContext null
            }

            localArtistMetadataCache[normalizeArtistComparable(name)] = resolved
            val audience = resolved.subscriberCountText?.takeIf {
                !it.equals("YouTube Music", ignoreCase = true) && !it.equals("YouTube Music Artist", ignoreCase = true)
            } ?: artist.songCountText.takeIf {
                it.contains("audience", ignoreCase = true) || it.contains("subscribers", ignoreCase = true) || it.contains("listeners", ignoreCase = true)
            }
            com.aman.auramusic.online.model.SavedOnlineArtist(
                id = resolved.id,
                name = resolved.name.ifBlank { name },
                profileImageUrl = resolved.profileImageUrl ?: artist.artworkModel as? String,
                source = AudioSource.YOUTUBE,
                audienceText = audience
            )
        }

    suspend fun fetchArtistSongs(
        artist: ArtistItem,
        localSongs: List<Song>
    ): Pair<List<OnlineSong>, List<Song>> {
        val data = fetchArtistCatalogue(artist, localSongs)
        return Pair(data.onlineSongs, data.localSongs)
    }

    suspend fun fetchAlbumTracks(album: OnlinePlaylist): List<OnlineSong> = withContext(Dispatchers.IO) {
        try {
            onlineRepository.getYouTubeMusicCollectionSongs(album)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    private fun resolveMissingArtistPortraits(currentArtists: List<ArtistItem>) {
        val missing = currentArtists.filter { !it.isOnline && it.artworkModel == null }
        if (missing.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            for (artist in missing) {
                val key = normalizeArtistComparable(artist.name)
                if (key.isBlank() || !attemptedArtistLookups.add(key)) continue

                val artistName = artist.name.trim()
                val searchQuery = artistName

                try {
                    val results = youtubeArtistRepository.searchArtists(searchQuery)
                    val entity = results.firstOrNull {
                        matchesArtistName(it.name, artistName)
                    }

                    if (entity != null) {
                        val profileUrl = entity.profileImageUrl?.takeIf { it.isNotBlank() }
                        if (profileUrl != null) {
                            localArtistMetadataCache[key] = entity
                            cachedDiscoveredArtists = (cachedDiscoveredArtists + entity).distinctBy { it.id }
                            com.aman.auramusic.util.ArtistArtworkCache.put(getApplication(), artistName, profileUrl)

                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    artists = _uiState.value.artists.map { item ->
                                        if (!item.isOnline && matchesArtistName(item.name, artistName)) {
                                            item.copy(artworkModel = profileUrl)
                                        } else {
                                            item
                                        }
                                    }
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    companion object {
        fun normalizeArtistComparable(raw: String): String {
            return raw
                .replace(" - Topic", "", ignoreCase = true)
                .replace("Official", "", ignoreCase = true)
                .replace("VEVO", "", ignoreCase = true)
                .replace('\u00A0', ' ')
                .replace(Regex("""[^\p{L}\p{Nd}]"""), "")
                .trim()
                .lowercase()
        }

        fun matchesArtistName(name1: String, name2: String): Boolean {
            if (name1.equals(name2, ignoreCase = true)) return true
            val c1 = normalizeArtistComparable(name1)
            val c2 = normalizeArtistComparable(name2)
            if (c1.isNotBlank() && c1 == c2) return true
            return false
        }
    }
}
