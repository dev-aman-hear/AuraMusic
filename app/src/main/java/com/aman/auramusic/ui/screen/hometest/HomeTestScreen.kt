package com.aman.auramusic.ui.screen.hometest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SongOptionsDialog
import com.aman.auramusic.ui.theme.AuraPrimary
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeTestScreen(
    songs: List<Song>,
    username: String,
    history: List<PlaybackHistoryEntry>,
    favorites: List<Song>,
    favoriteIds: Set<Long>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onRefresh: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit = { _, _ -> },
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onPlaylistSelected: (Long) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeTestViewModel = hiltViewModel()
) {
    val isDark = LocalIsDark.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedSongOptions by remember { mutableStateOf<Song?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryDiscoverItem?>(null) }
    var categoryOnlineSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var categoryLocalSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isCategoryLoading by remember { mutableStateOf(false) }

    var selectedArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var artistOnlineSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var artistLocalSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isArtistLoading by remember { mutableStateOf(false) }

    LaunchedEffect(songs, history) {
        viewModel.loadData(songs, history)
    }

    // Dynamic greeting based on time of day
    val timeGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    val cleanName = remember(username) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank() && trimmed != "Master") trimmed else ""
    }

    val dateHeader = remember {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        sdf.format(Date())
    }

    val userInitial = remember(username) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank() && trimmed != "Master") trimmed.first().uppercase() else "A"
    }

    // Background gradient: Sophisticated charcoal layered surfaces
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F0E13),
                Color(0xFF0A090D),
                Color(0xFF070709)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7F8FA),
                MaterialTheme.colorScheme.background
            )
        )
    }

    // Category Detail Bottom Sheet
    if (selectedCategory != null) {
        CategoryDetailSheet(
            category = selectedCategory!!,
            onlineSongs = categoryOnlineSongs,
            localSongs = categoryLocalSongs,
            isLoading = isCategoryLoading,
            onDismiss = { selectedCategory = null },
            onPlaySong = onSongSelected,
            onPlayOnlineSong = onOnlineSongSelected,
            onShuffleAll = {
                if (categoryOnlineSongs.isNotEmpty()) {
                    val shuffled = categoryOnlineSongs.shuffled()
                    onOnlineSongSelected(shuffled.first(), shuffled)
                } else if (categoryLocalSongs.isNotEmpty()) {
                    val shuffled = categoryLocalSongs.shuffled()
                    onSongSelected(shuffled.first(), shuffled)
                }
                selectedCategory = null
            }
        )
    }

    // Artist Detail Bottom Sheet (Fetches YouTube Music & Local tracks)
    if (selectedArtist != null) {
        ArtistDetailSheet(
            artist = selectedArtist!!,
            onlineSongs = artistOnlineSongs,
            localSongs = artistLocalSongs,
            isLoading = isArtistLoading,
            onDismiss = { selectedArtist = null },
            onPlaySong = onSongSelected,
            onPlayOnlineSong = onOnlineSongSelected,
            onPlayAll = {
                if (artistOnlineSongs.isNotEmpty()) {
                    onOnlineSongSelected(artistOnlineSongs.first(), artistOnlineSongs)
                } else if (artistLocalSongs.isNotEmpty()) {
                    onSongSelected(artistLocalSongs.first(), artistLocalSongs)
                }
                selectedArtist = null
            },
            onShuffleAll = {
                if (artistOnlineSongs.isNotEmpty()) {
                    val shuffled = artistOnlineSongs.shuffled()
                    onOnlineSongSelected(shuffled.first(), shuffled)
                } else if (artistLocalSongs.isNotEmpty()) {
                    val shuffled = artistLocalSongs.shuffled()
                    onSongSelected(shuffled.first(), shuffled)
                }
                selectedArtist = null
            }
        )
    }

    // Song Options Menu
    if (selectedSongOptions != null) {
        SongOptionsDialog(
            song = selectedSongOptions!!,
            isFavorite = selectedSongOptions!!.id in favoriteIds,
            onDismiss = { selectedSongOptions = null },
            onPlay = {
                onSongSelected(selectedSongOptions!!, songs)
                selectedSongOptions = null
            },
            onToggleFavorite = {
                onFavoriteToggle(selectedSongOptions!!)
                selectedSongOptions = null
            },
            onAddToPlaylist = {
                onAddToPlaylist(selectedSongOptions!!)
                selectedSongOptions = null
            },
            onAddToQueue = {
                onAddToQueue(selectedSongOptions!!)
                selectedSongOptions = null
            }
        )
    }

    AuraScreenBackground(
        modifier = modifier
    ) {
        if (uiState.isLoading && songs.isEmpty() && uiState.trendingTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AuraPrimary, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Curating your music...",
                        fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                // 1. Top Header
                item(key = "hometest_header") {
                    HomeTestHeader(
                        timeGreeting = timeGreeting,
                        username = cleanName,
                        dateText = dateHeader,
                        userInitial = userInitial,
                        isOffline = uiState.isOffline,
                        onOpenSettings = onOpenSettings,
                        onRefresh = {
                            viewModel.loadData(songs, history, forceRefresh = true)
                            onRefresh()
                        }
                    )
                }

                // 2. A. Daily Featured Album or Playlist Card (Refreshed once per day)
                uiState.featuredItem?.let { featured ->
                    item(key = "hometest_daily_featured") {
                        DailyFeaturedCard(
                            item = featured,
                            onClick = {
                                if (featured.albumName != null) {
                                    onAlbumSelected(featured.albumName)
                                } else if (featured.playlistId != null) {
                                    onPlaylistSelected(featured.playlistId)
                                } else if (uiState.trendingTracks.isNotEmpty()) {
                                    val firstTrend = uiState.trendingTracks.first()
                                    if (firstTrend.isLocalAvailable && firstTrend.localMatch != null) {
                                        onSongSelected(firstTrend.localMatch, songs)
                                    } else {
                                        onOnlineSongSelected(
                                            firstTrend.onlineSong,
                                            uiState.trendingTracks.map { it.onlineSong }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // 3. B. Trending Songs (Horizontally swipeable, YouTube Music / Curated)
                if (uiState.trendingTracks.isNotEmpty()) {
                    item(key = "hometest_trending_section") {
                        TrendingSongsSection(
                            tracks = uiState.trendingTracks,
                            onTrackClick = { item ->
                                if (item.isLocalAvailable && item.localMatch != null) {
                                    onSongSelected(item.localMatch, songs)
                                } else {
                                    onOnlineSongSelected(
                                        item.onlineSong,
                                        uiState.trendingTracks.map { it.onlineSong }
                                    )
                                }
                            }
                        )
                    }
                }

                // 4. C. Artists Section (Rounded-square artwork cards)
                if (uiState.artists.isNotEmpty()) {
                    item(key = "hometest_artists_section") {
                        ArtistsSection(
                            artists = uiState.artists,
                            onArtistClick = { artistItem ->
                                selectedArtist = artistItem
                                coroutineScope.launch {
                                    isArtistLoading = true
                                    val (onlineRes, localRes) = viewModel.fetchArtistSongs(artistItem.name, songs)
                                    artistOnlineSongs = onlineRes
                                    artistLocalSongs = localRes
                                    isArtistLoading = false
                                }
                            }
                        )
                    }
                }

                // 5. D. Quick Hits Section (Horizontally draggable across 3-row stacked columns)
                if (uiState.quickHitsColumns.isNotEmpty()) {
                    item(key = "hometest_quick_hits_section") {
                        val allOnlineQuickHits = remember(uiState.quickHitsColumns) {
                            uiState.quickHitsColumns.flatten().mapNotNull { (it as? QuickHitTrack.OnlineTrack)?.onlineSong }
                        }
                        QuickHitsSection(
                            columns = uiState.quickHitsColumns,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            onTrackClick = { track ->
                                when (track) {
                                    is QuickHitTrack.LocalTrack -> onSongSelected(track.song, songs)
                                    is QuickHitTrack.OnlineTrack -> onOnlineSongSelected(track.onlineSong, allOnlineQuickHits)
                                }
                            },
                            onOptionsClick = { song -> selectedSongOptions = song }
                        )
                    }
                }

                // 6. E. Discover by Category (Compact glowing mesh cards)
                if (uiState.categories.isNotEmpty()) {
                    item(key = "hometest_categories_section") {
                        DiscoverCategorySection(
                            categories = uiState.categories,
                            onCategoryClick = { category ->
                                selectedCategory = category
                                coroutineScope.launch {
                                    isCategoryLoading = true
                                    val (onlineRes, localRes) = viewModel.fetchCategorySongs(category, songs)
                                    categoryOnlineSongs = onlineRes
                                    categoryLocalSongs = localRes
                                    isCategoryLoading = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
