package com.aman.auramusic.ui.screen.hometest

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class CategoryDiscoverItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val searchQuery: String
)

data class HomeTestUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val featuredItem: DailyFeaturedItem? = null,
    val trendingTracks: List<TrendingSongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val artistSearchQuery: String = "",
    val isArtistSearching: Boolean = false,
    val quickHitsColumns: List<List<QuickHitTrack>> = emptyList(),
    val categories: List<CategoryDiscoverItem> = emptyList()
)

@HiltViewModel
class HomeTestViewModel @Inject constructor(
    application: Application,
    private val onlineRepository: OnlineMusicRepository,
    private val youtubeArtistRepository: com.aman.auramusic.online.network.repository.YouTubeArtistRepository
) : AndroidViewModel(application) {

    private val dailyCache = DailyFeaturedCache(application.applicationContext)
    private val _uiState = MutableStateFlow(HomeTestUiState())
    val uiState: StateFlow<HomeTestUiState> = _uiState.asStateFlow()

    private var cachedTrendingOnline = emptyList<OnlineSong>()
    private var cachedCuratedOnline = emptyList<OnlineSong>()
    private var cachedDiscoveredArtists = emptyList<com.aman.auramusic.online.network.repository.YouTubeArtist>()

    private val defaultCategories = listOf(
        CategoryDiscoverItem(
            id = "pop",
            title = "Pop",
            subtitle = "Global Chart Toppers",
            gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF0D1B2A)),
            searchQuery = "Pop Hits"
        ),
        CategoryDiscoverItem(
            id = "hiphop",
            title = "Hip Hop",
            subtitle = "Beats & Rhymes",
            gradientColors = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFF26120D)),
            searchQuery = "Hip Hop Rap Hits"
        ),
        CategoryDiscoverItem(
            id = "electronic",
            title = "Electronic",
            subtitle = "Dance, Synth & Bass",
            gradientColors = listOf(Color(0xFFD500F9), Color(0xFF651FFF), Color(0xFF1B0B2E)),
            searchQuery = "Electronic EDM Dance"
        ),
        CategoryDiscoverItem(
            id = "rock",
            title = "Rock",
            subtitle = "Classics & Modern Alt",
            gradientColors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF0E1A2C)),
            searchQuery = "Rock Alternative Hits"
        ),
        CategoryDiscoverItem(
            id = "rnb",
            title = "R&B / Soul",
            subtitle = "Smooth & Melodic",
            gradientColors = listOf(Color(0xFFE040FB), Color(0xFFFF5252), Color(0xFF2B0C1E)),
            searchQuery = "RnB Soul Melodic"
        ),
        CategoryDiscoverItem(
            id = "chill",
            title = "Chill",
            subtitle = "Lo-Fi & Relaxing Vibes",
            gradientColors = listOf(Color(0xFF00B4D8), Color(0xFF0077B6), Color(0xFF031627)),
            searchQuery = "Lofi Chill Beats"
        ),
        CategoryDiscoverItem(
            id = "happy",
            title = "Happy",
            subtitle = "Feel-Good Anthems",
            gradientColors = listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFF2B1803)),
            searchQuery = "Happy Upbeat Summer"
        ),
        CategoryDiscoverItem(
            id = "sad",
            title = "Sad",
            subtitle = "Moody & Melancholic",
            gradientColors = listOf(Color(0xFF7209B7), Color(0xFF3F37C9), Color(0xFF160B29)),
            searchQuery = "Sad Melancholic Acoustic"
        ),
        CategoryDiscoverItem(
            id = "desi",
            title = "Bollywood",
            subtitle = "Desi Hits & Melodies",
            gradientColors = listOf(Color(0xFFFF70A6), Color(0xFFFF9770), Color(0xFF2D101E)),
            searchQuery = "Bollywood Top Hits"
        ),
        CategoryDiscoverItem(
            id = "indie",
            title = "Indie",
            subtitle = "Acoustic & Independent",
            gradientColors = listOf(Color(0xFF80ED99), Color(0xFF38A3A5), Color(0xFF0B2124)),
            searchQuery = "Indie Folk Acoustic"
        )
    )

    init {
        _uiState.value = _uiState.value.copy(categories = defaultCategories)
    }

    fun loadData(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            if (_uiState.value.trendingTracks.isNotEmpty() && !forceRefresh) {
                updateLocalDerivedData(localSongs, history)
                return@launch
            }

            // Immediately populate artists and quick hits from local tracks and cache so UI is instant
            val initialArtists = buildArtistList(localSongs, cachedDiscoveredArtists)
            val initialQuickHits = buildQuickHitsFeed(localSongs, history, cachedTrendingOnline, cachedCuratedOnline)

            _uiState.value = _uiState.value.copy(
                artists = if (_uiState.value.artists.isEmpty()) initialArtists else _uiState.value.artists,
                quickHitsColumns = if (_uiState.value.quickHitsColumns.isEmpty()) initialQuickHits else _uiState.value.quickHitsColumns,
                isLoading = _uiState.value.trendingTracks.isEmpty(),
                isRefreshing = forceRefresh,
                errorMessage = null
            )

            val currentDayKey = DailyFeaturedCache.getCurrentDayKey()
            var featured = dailyCache.getDailyFeatured(currentDayKey)

            var isOfflineDetected = false
            var trendingOnline = emptyList<OnlineSong>()
            var curatedFeed = emptyList<OnlineSong>()
            var curatedPlaylists = emptyList<OnlinePlaylist>()
            var onlineArtistsDiscovered = emptyList<com.aman.auramusic.online.network.repository.YouTubeArtist>()

            try {
                // Fetch YouTube trending, curated feed, playlists, and artists concurrently
                withContext(Dispatchers.IO) {
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

                    val curatedDeferred = async {
                        try {
                            onlineRepository.getCuratedSongFeed()
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                    val playlistsDeferred = async {
                        try {
                            onlineRepository.getCuratedPlaylists(AudioSource.ALL)
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

                    trendingOnline = trendingDeferred.await()
                    curatedFeed = curatedDeferred.await()
                    curatedPlaylists = playlistsDeferred.await()
                    onlineArtistsDiscovered = ytArtistsDeferred.await()
                }

                cachedTrendingOnline = trendingOnline
                cachedCuratedOnline = curatedFeed
                cachedDiscoveredArtists = onlineArtistsDiscovered
            } catch (e: Exception) {
                isOfflineDetected = true
            }

            // 5. Resolve Daily Featured Item (Refreshes once per day)
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

            // 6. Map Trending Songs with matching local tracks
            val trendingItems = trendingOnline.map { onlineSong ->
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

            // 7. Structure Artists: Extract single artists and balance evenly
            val artistsList = buildArtistList(localSongs, cachedDiscoveredArtists)

            // 8. Quick Hits: Interleave BOTH offline and online trending & category top hits
            val quickHitsColumns = buildQuickHitsFeed(
                localSongs = localSongs,
                history = history,
                trendingOnline = trendingOnline,
                curatedFeed = curatedFeed
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isOffline = isOfflineDetected && trendingItems.isEmpty(),
                errorMessage = if (isOfflineDetected && trendingItems.isEmpty()) "Offline — Showing local library" else null,
                featuredItem = featured,
                trendingTracks = trendingItems,
                artists = artistsList,
                quickHitsColumns = quickHitsColumns,
                categories = defaultCategories
            )
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
            trendingOnline = cachedTrendingOnline,
            curatedFeed = cachedCuratedOnline
        )

        _uiState.value = _uiState.value.copy(
            artists = artistsList,
            quickHitsColumns = quickHitsColumns
        )
    }

    /**
     * Splits composite strings into strictly individual sanitized solo artist names.
     * E.g. "Anirudh Ravichander, Kaala Bhairava" -> ["Anirudh Ravichander", "Kaala Bhairava"]
     * "Taylor Swift feat. Post Malone" -> ["Taylor Swift", "Post Malone"]
     * "The Weeknd - Topic" -> ["The Weeknd"]
     */
    private fun extractSingleArtistNames(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        var cleaned = raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()

        // Extract featured artists from parentheses
        val parentheticalRegex = Regex("""[\(\[\{](?:feat\.?|ft\.?|with)\s+([^\)\]\}]+)[\)\]\}]""", RegexOption.IGNORE_CASE)
        val fromParens = mutableListOf<String>()
        parentheticalRegex.findAll(cleaned).forEach { match ->
            fromParens.add(match.groupValues[1])
        }
        cleaned = cleaned.replace(parentheticalRegex, " ")

        val splitRegex = Regex(""",|;|&|/|\||\s+feat\.?\s+|\s+ft\.?\s+|\s+with\s+|\s+and\s+|\s+x\s+|\s+vs\.?\s+""", RegexOption.IGNORE_CASE)
        val parts = cleaned.split(splitRegex)

        val invalidNames = setOf(
            "unknown", "unknown artist", "various", "various artists", "various artist",
            "soundtrack", "ost", "compilation", "audio", "music", "null", "undefined",
            "remix", "records", "producer", "feat", "ft", "instrumental",
            "video", "videos", "official video", "official audio", "clip", "channel",
            "top hits", "hits", "mix", "best of", "trending",
            "song", "songs", "track", "tracks", "album", "single", "singles"
        )

        return (parts + fromParens)
            .map { it.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_', '.', '~') }
            .filter { name ->
                val lower = name.lowercase()
                name.length in 2..40 &&
                !invalidNames.contains(lower) &&
                !lower.startsWith("top ") &&
                !lower.endsWith(" hits") &&
                !lower.contains("202") &&
                name.any { it.isLetter() }
            }
            .distinctBy { it.lowercase() }
    }

    /**
     * Builds artist list guaranteeing strictly ONE single artist per card and evenly balancing
     * local library artists with online YouTube Music artists (1:1 interleaved distribution).
     */
    private fun buildArtistList(
        localSongs: List<Song>,
        discoveredOnline: List<com.aman.auramusic.online.network.repository.YouTubeArtist>
    ): List<ArtistItem> {
        // 1. Process local songs into individual single artists
        val localArtistMap = mutableMapOf<String, MutableList<Song>>()
        localSongs.forEach { song ->
            val singleNames = extractSingleArtistNames(song.artist)
            singleNames.forEach { name ->
                localArtistMap.getOrPut(name) { mutableListOf() }.add(song)
            }
        }

        val localArtists = localArtistMap.entries
            .sortedByDescending { it.value.size }
            .map { (artistName, songList) ->
                ArtistItem(
                    name = artistName,
                    songCountText = "${songList.size} songs",
                    artworkModel = songList.firstOrNull { it.artworkUri != null }?.artworkUri,
                    isOnline = false,
                    artistId = "local_${artistName.hashCode()}"
                )
            }

        // 2. Format online YouTube artists (ensuring strictly 1 single artist per item)
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

        // 3. Evenly balance local and online artists (1:1 interleaved)
        val seen = mutableSetOf<String>()
        val balanced = mutableListOf<ArtistItem>()
        var localIdx = 0
        var onlineIdx = 0
        val maxTotal = 24

        while (balanced.size < maxTotal && (localIdx < localArtists.size || onlineIdx < onlineArtists.size)) {
            // Pick 1 local artist
            while (localIdx < localArtists.size) {
                val candidate = localArtists[localIdx++]
                if (seen.add(candidate.name.lowercase())) {
                    balanced.add(candidate)
                    break
                }
            }

            // Pick 1 online YouTube artist
            while (onlineIdx < onlineArtists.size && balanced.size < maxTotal) {
                val candidate = onlineArtists[onlineIdx++]
                if (seen.add(candidate.name.lowercase())) {
                    balanced.add(candidate)
                    break
                }
            }
        }

        // Fill remaining if one pool is depleted
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

    /**
     * Interleaves both offline (local) and online songs (trending & category top hits).
     * Chunked into columns of 3 so the user can drag horizontally from right to left smoothly.
     */
    private fun normalizedOnlineId(song: OnlineSong): String? =
        song.id.trim().takeIf { it.isNotEmpty() }?.lowercase()

    private fun normalizedSongMetadata(title: String, artist: String): String =
        "${title.trim().lowercase().replace(Regex("\\s+"), " ")}::${artist.trim().lowercase().replace(Regex("\\s+"), " ")}"

    private fun buildQuickHitsFeed(
        localSongs: List<Song>,
        history: List<PlaybackHistoryEntry>,
        trendingOnline: List<OnlineSong>,
        curatedFeed: List<OnlineSong>
    ): List<List<QuickHitTrack>> {
        val historySongs = history.sortedByDescending { it.playedAt }
            .mapNotNull { entry -> localSongs.find { it.id == entry.songId } }
            .distinctBy { it.id }

        // Keep Quick Hits distinct from Trending Now, including songs with alternate IDs.
        val trendingIds = trendingOnline.mapNotNull { normalizedOnlineId(it) }.toSet()
        val trendingMetadata = trendingOnline.map { normalizedSongMetadata(it.title, it.artist) }.toSet()

        val localTracks = localSongs.distinctBy { it.id }
            .filterNot { normalizedSongMetadata(it.title, it.artist) in trendingMetadata }
            .map { song ->
                val badge = if (historySongs.any { it.id == song.id }) "Recent Hit" else "Local"
                QuickHitTrack.LocalTrack(song, badge)
            }

        val categoryBadges = listOf("Pop Hit", "Top Chart", "Billboard", "Rock Hit", "Bollywood", "Electronic", "Discovery")
        val onlineTracks = curatedFeed
            .filterNot { normalizedOnlineId(it) in trendingIds ||
                normalizedSongMetadata(it.title, it.artist) in trendingMetadata }
            .distinctBy { normalizedOnlineId(it) ?: normalizedSongMetadata(it.title, it.artist) }
            .mapIndexed { index, os ->
                QuickHitTrack.OnlineTrack(os, categoryBadges[index % categoryBadges.size])
            }

        val unified = mutableListOf<QuickHitTrack>()
        var localIdx = 0
        var onlineIdx = 0

        // Build a generous horizontal feed of up to 24-30 tracks (8-10 columns)
        val targetCount = if (localTracks.isEmpty() && onlineTracks.isEmpty()) {
            0
        } else {
            maxOf(24, localTracks.size + onlineTracks.size).coerceAtMost(36)
        }

        while (unified.size < targetCount && (localIdx < localTracks.size || onlineIdx < onlineTracks.size)) {
            if (localIdx < localTracks.size && unified.size < targetCount) {
                unified.add(localTracks[localIdx++])
            }
            if (onlineIdx < onlineTracks.size && unified.size < targetCount) {
                unified.add(onlineTracks[onlineIdx++])
            }
            if (onlineIdx < onlineTracks.size && unified.size < targetCount) {
                unified.add(onlineTracks[onlineIdx++])
            }
            if (localIdx >= localTracks.size && onlineIdx < onlineTracks.size) {
                unified.add(onlineTracks[onlineIdx++])
            }
            if (onlineIdx >= onlineTracks.size && localIdx < localTracks.size) {
                unified.add(localTracks[localIdx++])
            }
        }

        return unified.chunked(3)
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
            onlineRepository.search(category.searchQuery, AudioSource.ALL).take(25)
        } catch (_: Exception) {
            emptyList()
        }

        val localFiltered = localSongs.filter { song ->
            val queryLower = category.title.lowercase()
            song.title.contains(queryLower, ignoreCase = true) ||
                    song.artist.contains(queryLower, ignoreCase = true) ||
                    song.album.contains(queryLower, ignoreCase = true)
        }.take(25)

        Pair(onlineResults, localFiltered)
    }

    /**
     * Searches for artists across local single artists and YouTube Artist Cloud.
     */
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

            // Local single artists matching query
            val localMatching = localSongs.flatMap { extractSingleArtistNames(it.artist) }
                .distinctBy { it.lowercase() }
                .filter { it.contains(q, ignoreCase = true) }
                .map { name ->
                    val matchingSongs = localSongs.filter { it.artist.contains(name, ignoreCase = true) }
                    ArtistItem(
                        name = name,
                        songCountText = "${matchingSongs.size} songs",
                        artworkModel = matchingSongs.firstOrNull { it.artworkUri != null }?.artworkUri,
                        isOnline = false,
                        artistId = "local_${name.hashCode()}"
                    )
                }

            val onlineItems = onlineResults.map { yt ->
                ArtistItem(
                    name = yt.name,
                    songCountText = yt.subscriberCountText ?: "YouTube Music",
                    artworkModel = yt.profileImageUrl,
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

    /**
     * Fetches tracks related to an artist from online YouTube Music + matching local library songs.
     * Uses YouTubeArtistRepository's intelligent candidate ranking to filter out covers, reaction videos,
     * and karaoke while prioritizing verified releases.
     */
    suspend fun fetchArtistSongs(
        artistName: String,
        localSongs: List<Song>
    ): Pair<List<OnlineSong>, List<Song>> = withContext(Dispatchers.IO) {
        val onlineResults = try {
            youtubeArtistRepository.getArtistSongs(artistName)
        } catch (_: Exception) {
            emptyList()
        }

        val localFiltered = localSongs.filter { song ->
            val singleNames = extractSingleArtistNames(song.artist)
            singleNames.any { it.equals(artistName, ignoreCase = true) || it.contains(artistName, ignoreCase = true) } ||
            song.title.contains(artistName, ignoreCase = true)
        }.take(25)

        Pair(onlineResults, localFiltered)
    }
}
