package com.aman.auramusic.ui.screen.home

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.SavedOnlineArtist
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SongOptionsDialog
import com.aman.auramusic.ui.screen.OnlinePlaylistDetailScreen
import com.aman.auramusic.ui.theme.AuraPrimary
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
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
    savedOnlineArtists: List<SavedOnlineArtist> = emptyList(),
    savedOnlineAlbums: List<OnlinePlaylist> = emptyList(),
    onSaveOnlineArtist: ((SavedOnlineArtist) -> Unit)? = null,
    onRemoveSavedOnlineArtist: ((String) -> Unit)? = null,
    onSaveOnlineAlbum: ((OnlinePlaylist) -> Unit)? = null,
    onRemoveSavedOnlineAlbum: ((String) -> Unit)? = null,
    homeResetKey: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isDark = LocalIsDark.current
    val context = LocalContext.current
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
    var artistOnlineAlbums by remember { mutableStateOf<List<OnlinePlaylist>>(emptyList()) }
    var artistAudienceText by remember { mutableStateOf<String?>(null) }
    var isArtistLoading by remember { mutableStateOf(false) }

    var selectedOnlineAlbum by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var selectedOnlineAlbumSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isAlbumLoading by remember { mutableStateOf(false) }

    var showAllArtistsSheet by remember { mutableStateOf(false) }
    var showAllQuickHitsSheet by remember { mutableStateOf(false) }
    var showAllCategoriesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(songs, history) {
        viewModel.loadData(songs, history)
    }

    LaunchedEffect(homeResetKey) {
        if (homeResetKey > 0) {
            selectedArtist = null
            artistOnlineSongs = emptyList()
            artistLocalSongs = emptyList()
            artistOnlineAlbums = emptyList()
            artistAudienceText = null
            selectedOnlineAlbum = null
            selectedOnlineAlbumSongs = emptyList()
            selectedCategory = null
            categoryOnlineSongs = emptyList()
            categoryLocalSongs = emptyList()
            selectedSongOptions = null
            showAllArtistsSheet = false
            showAllQuickHitsSheet = false
            showAllCategoriesSheet = false
        }
    }

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

    if (showAllArtistsSheet) {
        AllArtistsSheet(
            artists = uiState.artists,
            onArtistClick = { artistItem ->
                selectedArtist = artistItem
                coroutineScope.launch {
                    isArtistLoading = true
                    val catalogue = viewModel.fetchArtistCatalogue(artistItem, songs)
                    artistOnlineSongs = catalogue.onlineSongs
                    artistLocalSongs = catalogue.localSongs
                    artistOnlineAlbums = catalogue.onlineAlbums
                    artistAudienceText = catalogue.audienceText
                    isArtistLoading = false
                }
            },
            onDismiss = { showAllArtistsSheet = false }
        )
    }

    if (showAllQuickHitsSheet) {
        val allQuickHitTracks = remember(uiState.quickHitsColumns) {
            uiState.quickHitsColumns.flatten()
        }
        val allOnlineQuickHits = remember(allQuickHitTracks) {
            allQuickHitTracks.mapNotNull { (it as? QuickHitTrack.OnlineTrack)?.onlineSong }
        }
        AllQuickHitsSheet(
            tracks = allQuickHitTracks,
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            onTrackClick = { track ->
                when (track) {
                    is QuickHitTrack.LocalTrack -> onSongSelected(track.song, songs)
                    is QuickHitTrack.OnlineTrack -> onOnlineSongSelected(track.onlineSong, allOnlineQuickHits)
                }
            },
            onOptionsClick = { song -> selectedSongOptions = song },
            onDismiss = { showAllQuickHitsSheet = false }
        )
    }

    if (showAllCategoriesSheet) {
        AllCategoriesSheet(
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
            },
            onDismiss = { showAllCategoriesSheet = false }
        )
    }

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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Curating your music...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val effectiveBarHeight = if (statusBarHeight > 0.dp) statusBarHeight else 28.dp
            val isScrolled by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
                }
            }
            val scrimAlpha by animateFloatAsState(
                targetValue = if (isScrolled) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "homeStatusBarScrimAlpha"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    item(key = "home_header") {
                        HomeHeader(
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

                    uiState.featuredItem?.let { featured ->
                        item(key = "home_daily_featured") {
                            DailyFeaturedCard(
                                item = featured,
                                onClick = {
                                    if (featured.albumName != null) {
                                        onAlbumSelected(featured.albumName)
                                    } else if (featured.playlistId != null) {
                                        onPlaylistSelected(featured.playlistId)
                                    } else if (uiState.featuredTracks.isNotEmpty()) {
                                        onOnlineSongSelected(
                                            uiState.featuredTracks.first(),
                                            uiState.featuredTracks
                                        )
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

                    if (uiState.trendingTracks.isNotEmpty()) {
                        item(key = "home_trending_section") {
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

                    if (uiState.listenAgainColumns.isNotEmpty()) {
                        item(key = "home_listen_again_section") {
                            val allOnlineListenAgain = remember(uiState.listenAgainColumns) {
                                uiState.listenAgainColumns.flatten().mapNotNull { (it as? QuickHitTrack.OnlineTrack)?.onlineSong }
                            }
                            ListenAgainSection(
                                columns = uiState.listenAgainColumns,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                onTrackClick = { track ->
                                    when (track) {
                                        is QuickHitTrack.LocalTrack -> onSongSelected(track.song, songs)
                                        is QuickHitTrack.OnlineTrack -> onOnlineSongSelected(
                                            track.onlineSong,
                                            allOnlineListenAgain.ifEmpty { listOf(track.onlineSong) }
                                        )
                                    }
                                },
                                onOptionsClick = { song -> selectedSongOptions = song }
                            )
                        }
                    }

                    if (uiState.artists.isNotEmpty()) {
                        item(key = "home_artists_section") {
                            ArtistsSection(
                                artists = uiState.artists,
                                onArtistClick = { artistItem ->
                                    selectedArtist = artistItem
                                    coroutineScope.launch {
                                        isArtistLoading = true
                                        val catalogue = viewModel.fetchArtistCatalogue(artistItem, songs)
                                        artistOnlineSongs = catalogue.onlineSongs
                                        artistLocalSongs = catalogue.localSongs
                                        artistOnlineAlbums = catalogue.onlineAlbums
                                        artistAudienceText = catalogue.audienceText
                                        isArtistLoading = false
                                    }
                                },
                                onSeeAllClick = { showAllArtistsSheet = true }
                            )
                        }
                    }

                    if (uiState.quickHitsColumns.isNotEmpty()) {
                        item(key = "home_quick_hits_section") {
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
                                onOptionsClick = { song -> selectedSongOptions = song },
                                onSeeAllClick = { showAllQuickHitsSheet = true }
                            )
                        }
                    }

                    if (uiState.categories.isNotEmpty()) {
                        item(key = "home_categories_section") {
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
                                },
                                onSeeAllClick = { showAllCategoriesSheet = true }
                            )
                        }
                    }
                }

                if (scrimAlpha > 0f) {
                    val scrimColor = MaterialTheme.colorScheme.background
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(effectiveBarHeight + 16.dp)
                            .graphicsLayer { alpha = scrimAlpha }
                            .background(
                                Brush.verticalGradient(
                                    0.0f to scrimColor.copy(alpha = 0.98f),
                                    0.68f to scrimColor.copy(alpha = 0.88f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                }
            }
        }

        if (selectedOnlineAlbum != null) {
            val curAlbum = selectedOnlineAlbum!!
            val isAlbumSaved = savedOnlineAlbums.any { it.id == curAlbum.id }
            OnlinePlaylistDetailScreen(
                playlist = curAlbum,
                songs = selectedOnlineAlbumSongs,
                isLoading = isAlbumLoading,
                isSavedInLibrary = isAlbumSaved,
                showAddButton = true,
                onToggleSaveToLibrary = {
                    if (isAlbumSaved) {
                        onRemoveSavedOnlineAlbum?.invoke(curAlbum.id)
                    } else {
                        val albumToSave = if (selectedOnlineAlbumSongs.isNotEmpty()) {
                            curAlbum.copy(songs = selectedOnlineAlbumSongs)
                        } else curAlbum
                        onSaveOnlineAlbum?.invoke(albumToSave)
                    }
                },
                onBack = {
                    selectedOnlineAlbum = null
                    selectedOnlineAlbumSongs = emptyList()
                },
                onSongSelected = { song, queue ->
                    onOnlineSongSelected(song, queue)
                }
            )
        } else if (selectedArtist != null) {
            val curArtist = selectedArtist!!
            val savedArtistEntry = savedOnlineArtists.firstOrNull {
                (curArtist.artistId.isNotBlank() && it.id == curArtist.artistId) ||
                        it.name.equals(curArtist.name, ignoreCase = true)
            }
            val isArtistSaved = savedArtistEntry != null
            ArtistDetailSheet(
                artist = curArtist,
                onlineSongs = artistOnlineSongs,
                localSongs = artistLocalSongs,
                onlineAlbums = artistOnlineAlbums,
                isLoading = isArtistLoading,
                isSavedInLibrary = isArtistSaved,
                audienceText = artistAudienceText,
                onToggleSaveToLibrary = {
                    if (isArtistSaved) {
                        savedArtistEntry?.let { onRemoveSavedOnlineArtist?.invoke(it.id) }
                    } else {
                        coroutineScope.launch {
                            val resolved = viewModel.resolveArtistForLibrary(curArtist)
                            if (resolved != null) {
                                onSaveOnlineArtist?.invoke(resolved)
                            } else {
                                Toast.makeText(context, "Could not resolve artist for library", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onDismiss = {
                    selectedArtist = null
                    artistOnlineSongs = emptyList()
                    artistLocalSongs = emptyList()
                    artistOnlineAlbums = emptyList()
                },
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
                },
                onOpenOnlineAlbum = { album ->
                    selectedOnlineAlbum = album
                    coroutineScope.launch {
                        isAlbumLoading = true
                        selectedOnlineAlbumSongs = viewModel.fetchAlbumTracks(album)
                        isAlbumLoading = false
                    }
                }
            )
        }
    }
}
