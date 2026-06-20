package com.aman.auramusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.MiniPlayer
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.screen.PlayerScreen
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.viewmodel.MusicViewModel
import com.aman.auramusic.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            val appSettings by musicViewModel.settings.collectAsStateWithLifecycle()
            
            AuraMusicTheme(
                dynamicColor = appSettings.dynamicColors,
                amoledMode = appSettings.amoledMode
            ) {
                MusicScreen(musicViewModel)
            }
        }
    }
}

@Composable
fun MusicScreen(musicViewModel: MusicViewModel) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel()
    
    // Extracted colors for global use
    val dominantColor by remember(playerViewModel.dominantColor) { 
        mutableStateOf(Color(playerViewModel.dominantColor)) 
    }

    val appSettings by musicViewModel.settings.collectAsStateWithLifecycle()
    val songs by musicViewModel.songs.collectAsStateWithLifecycle()
    val username by musicViewModel.username.collectAsStateWithLifecycle()
    val favoriteIds by musicViewModel.favoriteIds.collectAsStateWithLifecycle()
    val playbackHistory by musicViewModel.playbackHistory.collectAsStateWithLifecycle()
    val playlists by musicViewModel.playlists.collectAsStateWithLifecycle()

    val animatedDominantColor by animateColorAsState(
        targetValue = if (appSettings.dynamicColors) dominantColor else Color.Transparent,
        animationSpec = tween(1000),
        label = "globalDynamicColor"
    )
    val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()

    val permission = rememberAudioPermission()
    val notificationPermission = rememberNotificationPermission()
    var hasPermission by remember { mutableStateOf(context.hasAudioPermission(permission)) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission(notificationPermission)) }
    var showPlayer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var selectedLibraryTab by remember { mutableStateOf(LibraryTab.Songs) }
    var query by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle system back button with priority
    BackHandler(enabled = showPlayer || showSettings || selectedPlaylistId != null || selectedAlbumName != null || selectedArtistName != null) {
        when {
            showPlayer -> showPlayer = false
            showSettings -> showSettings = false
            selectedPlaylistId != null -> selectedPlaylistId = null
            selectedAlbumName != null -> selectedAlbumName = null
            selectedArtistName != null -> selectedArtistName = null
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (playlistToRename != null) {
        RenamePlaylistDialog(
            currentName = playlistToRename!!.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                musicViewModel.renamePlaylist(playlistToRename!!.id, newName)
                playlistToRename = null
            }
        )
    }

    val selectedPlaylist = remember(playlists, selectedPlaylistId) {
        playlists.find { it.id == selectedPlaylistId }
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[permission] ?: hasPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[notificationPermission] ?: hasNotificationPermission
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasPermission) permissionsToRequest.add(permission)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(notificationPermission)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            musicViewModel.loadSongs()
        }
    }

    var hasAttemptedRestore by remember { mutableStateOf(false) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && !hasAttemptedRestore) {
            playerViewModel.setQueue(songs)
            playerViewModel.restoreLastState(songs)
            hasAttemptedRestore = true
        }
    }
    
    // Ensure queue is kept in sync if songs change after restore
    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && hasAttemptedRestore) {
            playerViewModel.setQueue(songs)
        }
    }

    fun playSong(song: Song, queue: List<Song> = songs) {
        playerViewModel.setQueue(queue)
        playerViewModel.play(song)
        showPlayer = true
    }

    if (songToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAddToPlaylist = null },
            onPlaylistSelected = { playlist ->
                musicViewModel.addToPlaylist(playlist.id, songToAddToPlaylist!!.id)
                songToAddToPlaylist = null
            },
            onCreateNew = {
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                musicViewModel.savePlaylist(name, emptyList())
                showCreatePlaylistDialog = false
            }
        )
    }

    val filteredSongs = remember(songs, query) {
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }
        }
    }

    val favoriteSongs = remember(songs, favoriteIds) {
        songs.filter { it.id in favoriteIds }
    }

    val sortedPlaylists = remember(playlists) {
        playlists.sortedBy { it.name }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f), // Dark top for status bar visibility
                            animatedDominantColor.copy(alpha = 0.35f),
                            animatedDominantColor.copy(alpha = 0.12f),
                            animatedDominantColor.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
            bottomBar = {
                AppBottomBar(
                    selectedTab = selectedTab,
                    currentSong = playerViewModel.currentSong,
                    isPlaying = playerViewModel.isPlaying,
                    position = playerViewModel.currentPosition,
                    duration = playerViewModel.duration,
                    onTabSelected = { 
                    selectedTab = it 
                    selectedPlaylistId = null
                    selectedAlbumName = null
                    selectedArtistName = null
                    showSettings = false
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                    dominantColor = dominantColor,
                    onOpenPlayer = { showPlayer = true },
                    onPlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.playNext() }
                )
            }
        ) { padding ->
            if (!hasPermission) {
                PermissionScreen(
                    modifier = Modifier.padding(padding),
                    onGrant = { 
                        val perms = mutableListOf(permission)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(notificationPermission)
                        }
                        multiplePermissionLauncher.launch(perms.toTypedArray())
                    }
                )
            } else {
                if (showSettings) {
                    SettingsScreen(
                        songCount = songs.size,
                        albumCount = songs.distinctBy { it.album }.size,
                        artistCount = songs.distinctBy { it.artist }.size,
                        username = username,
                        appSettings = appSettings,
                        onUsernameChange = { musicViewModel.updateUsername(it) },
                        onDynamicColorsChange = { musicViewModel.setDynamicColors(it) },
                        onAmoledChange = { musicViewModel.setAmoledMode(it) },
                        onBlurIntensityChange = { musicViewModel.setBlurIntensity(it) },
                        onKaraokeChange = { musicViewModel.setKaraokeMode(it) },
                        onLyricFontScaleChange = { musicViewModel.setLyricFontScale(it) },
                        onCrossfadeChange = { musicViewModel.setCrossfadeEnabled(it) },
                        onGaplessChange = { musicViewModel.setGaplessEnabled(it) },
                        onSkipSilenceChange = { musicViewModel.setSkipSilence(it) },
                        onSmartAudioFocusChange = { musicViewModel.setSmartAudioFocus(it) },
                        onKeepPlayingOnCloseChange = { musicViewModel.setKeepPlayingOnClose(it) },
                        onPlaylistGridColumnsChange = { musicViewModel.setPlaylistGridColumns(it) },
                        onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                        onBack = { showSettings = false },
                        onShowAbout = { showAboutDialog = true },
                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                    )
                } else {
                    when {
                        selectedPlaylist != null -> {
                            val playlistSongs = remember(songs, selectedPlaylist) {
                                songs.filter { it.id in selectedPlaylist.songIds }
                            }
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist,
                                songs = playlistSongs,
                                currentSongId = playerViewModel.currentSongId,
                                favoriteIds = favoriteIds,
                                onBack = { selectedPlaylistId = null },
                                onSongSelected = { song -> playSong(song, playlistSongs) },
                                onRemoveSong = { song -> musicViewModel.removeFromPlaylist(selectedPlaylist.id, song.id) },
                                onDeletePlaylist = {
                                    musicViewModel.deletePlaylist(selectedPlaylist.id)
                                    selectedPlaylistId = null
                                },
                                onRenamePlaylist = { playlistToRename = selectedPlaylist },
                                onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                onShufflePlay = {
                                    val shuffled = playlistSongs.shuffled()
                                    playerViewModel.setQueue(shuffled)
                                    shuffled.firstOrNull()?.let { playerViewModel.play(it) }
                                    showPlayer = true
                                },
                                modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                            )
                        }
                        selectedAlbumName != null -> {
                            val albumSongs = remember(songs, selectedAlbumName) {
                                songs.filter { it.album == selectedAlbumName }
                            }
                            CollectionDetailScreen(
                                title = selectedAlbumName!!,
                                subtitle = "Album • ${albumSongs.firstOrNull()?.artist ?: "Unknown"}",
                                songs = albumSongs,
                                currentSongId = playerViewModel.currentSongId,
                                favoriteIds = favoriteIds,
                                onBack = { selectedAlbumName = null },
                                onSongSelected = { song -> playSong(song, albumSongs) },
                                onAddToPlaylist = { songToAddToPlaylist = it },
                                onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                onShufflePlay = {
                                    val shuffled = albumSongs.shuffled()
                                    playerViewModel.setQueue(shuffled)
                                    shuffled.firstOrNull()?.let { playerViewModel.play(it) }
                                    showPlayer = true
                                },
                                modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                            )
                        }
                        selectedArtistName != null -> {
                            val artistSongs = remember(songs, selectedArtistName) {
                                songs.filter { it.artist == selectedArtistName }
                            }
                            CollectionDetailScreen(
                                title = selectedArtistName!!,
                                subtitle = "Artist • ${artistSongs.size} songs",
                                songs = artistSongs,
                                currentSongId = playerViewModel.currentSongId,
                                favoriteIds = favoriteIds,
                                onBack = { selectedArtistName = null },
                                onSongSelected = { song -> playSong(song, artistSongs) },
                                onAddToPlaylist = { songToAddToPlaylist = it },
                                onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                onShufflePlay = {
                                    val shuffled = artistSongs.shuffled()
                                    playerViewModel.setQueue(shuffled)
                                    shuffled.firstOrNull()?.let { playerViewModel.play(it) }
                                    showPlayer = true
                                },
                                modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                            )
                        }
                        else -> {
                            when (selectedTab) {
                                AppTab.Home -> HomeScreen(
                                    songs = songs,
                                    username = username,
                                    history = playbackHistory,
                                    favorites = favoriteSongs,
                                    dominantColor = animatedDominantColor,
                                    onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                    onSongSelected = { song, queue -> playSong(song, queue) },
                                    onAlbumSelected = { selectedAlbumName = it },
                                    onArtistSelected = { selectedArtistName = it },
                                    onOpenSettings = { showSettings = true },
                                    modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                                )

                                AppTab.Library -> {
                                    LibraryScreen(
                                        songs = filteredSongs,
                                        allSongs = songs,
                                        favoriteSongs = favoriteSongs,
                                        favoriteIds = favoriteIds,
                                        playlists = sortedPlaylists,
                                        playbackHistory = playbackHistory,
                                        selectedTab = selectedLibraryTab,
                                        query = query,
                                        currentSongId = playerViewModel.currentSongId,
                                        playlistGridColumns = appSettings.playlistGridColumns,
                                        onQueryChange = { query = it },
                                        onTabSelected = { 
                                            selectedLibraryTab = it 
                                            selectedPlaylistId = null
                                            selectedAlbumName = null
                                            selectedArtistName = null
                                        },
                                        onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                        onFavoriteToggle = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                        onSongSelected = { song, queue -> playSong(song, queue) },
                                        onOpenSettings = { showSettings = true },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onPlaylistDeleted = { playlist -> musicViewModel.deletePlaylist(playlist.id) },
                                        onPlaylistSelected = { selectedPlaylistId = it.id },
                                        onPlaylistRenamed = { playlistToRename = it },
                                        onAlbumSelected = { selectedAlbumName = it },
                                        onArtistSelected = { selectedArtistName = it },
                                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        playerViewModel.currentSong?.takeIf { showPlayer }?.let { song ->
            val queue by playerViewModel.queue.collectAsStateWithLifecycle()
            val historySongs = remember(songs, playbackHistory, song.id) {
                playbackHistory
                    .sortedByDescending { it.playedAt }
                    .filter { it.songId != song.id } // Don't show current song in history
                    .mapNotNull { entry -> songs.firstOrNull { it.id == entry.songId } }
                    .distinctBy { it.id }
                    .take(10)
                    .reversed() // Show in chronological order (oldest at top of history section)
            }
            
            PlayerScreen(
                song = song,
                isPlaying = playerViewModel.isPlaying,
                position = playerViewModel.currentPosition,
                duration = playerViewModel.duration,
                lyrics = lyrics,
                queue = queue,
                history = historySongs,
                onBack = { showPlayer = false },
                onPlayPause = { playerViewModel.togglePlayPause() },
                onSeek = { playerViewModel.seekTo(it) },
                onPrevious = { playerViewModel.playPrevious() },
                onNext = { playerViewModel.playNext() },
                onSongSelected = { playerViewModel.play(it) },
                onQueueRemove = { playerViewModel.removeQueueItem(it.id) },
                onQueueClear = { playerViewModel.clearQueueExceptCurrent() },
                onQueueSave = { playerViewModel.saveQueueAsPlaylist("Queue") },
                onHistoryClear = { musicViewModel.clearHistory() },
                onAddToPlaylist = { songToAddToPlaylist = song },
                appSettings = appSettings,
                playerViewModel = playerViewModel
            )
        }
    }
}
}

@Composable
private fun HomeScreen(
    songs: List<Song>,
    username: String,
    history: List<com.aman.auramusic.data.model.PlaybackHistoryEntry>,
    favorites: List<Song>,
    dominantColor: Color,
    onRefresh: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentPlayed = remember(songs, history) {
        history
            .sortedByDescending { it.playedAt }
            .mapNotNull { entry -> songs.firstOrNull { it.id == entry.songId } }
            .distinctBy { it.id }
            .take(10)
    }

    val mostPlayed = remember(songs, history) {
        val counts = history.groupBy { it.songId }.mapValues { it.value.sumOf { entry -> entry.playCount } }
        songs.sortedByDescending { counts[it.id] ?: 0 }.take(10)
    }


    val albumGroups = remember(songs) { songs.groupBy { it.album }.entries.sortedByDescending { it.value.size }.take(10) }
    val artistGroups = remember(songs) { songs.groupBy { it.artist }.entries.sortedByDescending { it.value.size }.take(10) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HomeHero(
                username = username,
                dominantColor = dominantColor,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings
            )
        }

        item {
            SectionTitle("Recently Played")
            HorizontalSongRail(
                songs = recentPlayed,
                onSongSelected = { onSongSelected(it, recentPlayed) },
                emptyLabel = "Nothing played yet"
            )
        }

        item {
            SectionTitle("Favorites")
            HorizontalSongRail(
                songs = favorites,
                onSongSelected = { onSongSelected(it, favorites) },
                emptyLabel = "Heart songs to see them here"
            )
        }

        item {
            SectionTitle("Albums")
            HorizontalCollectionRail(
                items = albumGroups,
                onClick = { entry -> onAlbumSelected(entry.key.toString()) },
                subtitle = { entry -> "${entry.value.size} songs" }
            )
        }

        item {
            SectionTitle("Artists")
            HorizontalCollectionRail(
                items = artistGroups,
                onClick = { entry -> onArtistSelected(entry.key.toString()) },
                subtitle = { entry -> "${entry.value.size} songs" }
            )
        }

        item {
            SectionTitle("Most Played")
            HorizontalSongRail(
                songs = mostPlayed,
                onSongSelected = { onSongSelected(it, mostPlayed) },
                emptyLabel = "Play songs to build this rail"
            )
        }


    }
}

@Composable
private fun HomeHero(
    username: String,
    dominantColor: Color,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
            }
            Row {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh, 
                        contentDescription = "Scan local music",
                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings, 
                        contentDescription = "Settings",
                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalSongRail(
    songs: List<Song>,
    onSongSelected: (Song) -> Unit,
    emptyLabel: String
) {
    if (songs.isEmpty()) {
        Text(
            text = emptyLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            RecentSongCard(
                song = song,
                onClick = { onSongSelected(song) }
            )
        }
    }
}

@Composable
private fun PlaylistRail(
    playlists: List<com.aman.auramusic.data.model.Playlist>,
    songById: Map<Long, Song>,
    columns: Int,
    onPlaylistSelected: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        return
    }

    val sorted = remember(playlists) { playlists.sortedBy { it.name } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.height(500.dp) // Adjusted to show several rows
    ) {
        items(sorted, key = { it.id }) { playlist ->
            val previewSong = playlist.artworkSongId?.let(songById::get)
                ?: playlist.songIds.firstOrNull()?.let(songById::get)
            PlaylistPreviewCard(
                playlistName = playlist.name,
                songCount = playlist.songIds.size,
                previewSong = previewSong,
                onClick = { onPlaylistSelected(playlist) }
            )
        }
    }
}

@Composable
private fun PlaylistPreviewCard(
    playlistName: String,
    songCount: Int,
    previewSong: Song?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.height(160.dp) // Slightly taller for grid
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                previewSong?.let {
                    SongArtwork(
                        song = it,
                        size = 200, // Large enough to fill grid cell
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$songCount songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> HorizontalCollectionRail(
    items: List<T>,
    onClick: (T) -> Unit,
    subtitle: (T) -> String
) {
    if (items.isEmpty()) {
        Text(
            text = "No results yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items, key = { item ->
            when (item) {
                is Map.Entry<*, *> -> item.key.hashCode()
                else -> item.hashCode()
            }
        }) { item ->
            val title = when (item) {
                is Map.Entry<*, *> -> item.key.toString()
                else -> item.toString()
            }
            val song = when (item) {
                is Map.Entry<*, *> -> (item.value as? List<*>)?.firstOrNull() as? Song
                else -> null
            }
            Surface(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onClick(item) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SongArtwork(
                        song = song,
                        size = 124,
                        shape = RoundedCornerShape(20.dp),
                        elevation = 2.dp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Text(
                        text = subtitle(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box {
                SongArtwork(
                    song = song,
                    size = 124,
                    shape = RoundedCornerShape(20.dp),
                    elevation = 2.dp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun MadeForYouCard(
    songs: List<Song>,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            songs.firstOrNull()?.let {
                SongArtwork(song = it, size = 96, shape = RoundedCornerShape(20.dp))
            } ?: Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aura Mix",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (songs.isEmpty()) {
                        "Scan your library to build a personal mix."
                    } else {
                        "A polished queue from your local library."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaylistDetailScreen(
    playlist: Playlist,
    songs: List<Song>,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onRemoveSong: (Song) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRenamePlaylist: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onShufflePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Playlist") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                onRenamePlaylist()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Playlist") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDeletePlaylist()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Centered Artwork
                songs.firstOrNull()?.let {
                    SongArtwork(
                        song = it,
                        size = 240,
                        shape = RoundedCornerShape(24.dp),
                        elevation = 8.dp
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play & Shuffle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShufflePlay,
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { songs.firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f, fill = false)
                            .width(140.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No songs in this playlist yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onPlayNow = { onSongSelected(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onRemove = { onRemoveSong(song) },
                    onClick = { onSongSelected(song) }
                )
            }
        }
    }
}

@Composable
private fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long?,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onShufflePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }

        item {
            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Centered Artwork
                songs.firstOrNull()?.let {
                    SongArtwork(
                        song = it,
                        size = 240,
                        shape = RoundedCornerShape(24.dp),
                        elevation = 8.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play & Shuffle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShufflePlay,
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { songs.firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f, fill = false)
                            .width(140.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isPlaying = song.id == currentSongId,
                onPlayNow = { onSongSelected(song) },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onClick = { onSongSelected(song) }
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    allSongs: List<Song>,
    favoriteSongs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<com.aman.auramusic.data.model.Playlist>,
    playbackHistory: List<com.aman.auramusic.data.model.PlaybackHistoryEntry>,
    selectedTab: LibraryTab,
    query: String,
    currentSongId: Long?,
    playlistGridColumns: Int,
    onQueryChange: (String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onRefresh: () -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onOpenSettings: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onPlaylistDeleted: (Playlist) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onPlaylistRenamed: (Playlist) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            LibraryHeader(
                query = query,
                onQueryChange = onQueryChange,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings
            )
        }

        item {
            LibraryTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }

        when (selectedTab) {
            LibraryTab.Songs -> {
                if (songs.isEmpty()) {
                    item { EmptyLibrary(query = query) }
                } else {
                    items(songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isPlaying = song.id == currentSongId,
                            onPlayNow = { 
                                onSongSelected(song, songs)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            onToggleFavorite = { onFavoriteToggle(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onClick = { 
                                onSongSelected(song, songs)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }

            LibraryTab.Albums -> {
                val albums = songs.groupBy { it.album }.toSortedMap()
                if (albums.isEmpty()) {
                    item { EmptyLibrary(query = query) }
                } else {
                    items(albums.entries.toList(), key = { it.key }) { album ->
                        CollectionRow(
                            title = album.key,
                            subtitle = "${album.value.size} songs / ${album.value.first().artist}",
                            song = album.value.first(),
                            onClick = { 
                                onAlbumSelected(album.key)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }

            LibraryTab.Artists -> {
                val artists = songs.groupBy { it.artist }.toSortedMap()
                if (artists.isEmpty()) {
                    item { EmptyLibrary(query = query) }
                } else {
                    items(artists.entries.toList(), key = { it.key }) { artist ->
                        CollectionRow(
                            title = artist.key,
                            subtitle = "${artist.value.size} songs",
                            song = artist.value.first(),
                            onClick = { 
                                onArtistSelected(artist.key)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }

            LibraryTab.Playlists -> {
                if (playlists.isEmpty()) {
                    item {
                        MadeForYouCard(
                            songs = songs,
                            onClick = { songs.firstOrNull()?.let { onSongSelected(it, songs) } }
                        )
                    }
                } else {
                    item {
                        PlaylistRail(
                            playlists = playlists,
                            songById = allSongs.associateBy { it.id },
                            columns = playlistGridColumns,
                            onPlaylistSelected = onPlaylistSelected
                        )
                    }
                }
            }

            LibraryTab.Favorites -> {
                if (favoriteSongs.isEmpty()) {
                    item { EmptyLibrary(query = "favorites") }
                } else {
                    items(favoriteSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isPlaying = song.id == currentSongId,
                            onPlayNow = { 
                                onSongSelected(song, favoriteSongs)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            onToggleFavorite = { onFavoriteToggle(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onClick = { 
                                onSongSelected(song, favoriteSongs)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Scan local music")
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibraryTabs(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LibraryTab.entries, key = { it.title }) { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.title) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CollectionRow(
    title: String,
    subtitle: String,
    song: Song,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(song = song, size = 56, shape = RoundedCornerShape(14.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRename != null) {
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    username: String,
    appSettings: com.aman.auramusic.data.model.AppSettings,
    onUsernameChange: (String) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onBlurIntensityChange: (Int) -> Unit,
    onKaraokeChange: (Boolean) -> Unit,
    onLyricFontScaleChange: (Float) -> Unit,
    onCrossfadeChange: (Boolean) -> Unit,
    onGaplessChange: (Boolean) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onSmartAudioFocusChange: (Boolean) -> Unit,
    onKeepPlayingOnCloseChange: (Boolean) -> Unit,
    onPlaylistGridColumnsChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Personalization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    var localUsername by remember { mutableStateOf(username) }
                    
                    OutlinedTextField(
                        value = localUsername,
                        onValueChange = { 
                            localUsername = it
                            onUsernameChange(it) 
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ToggleRow(title = "Dynamic colors", checked = appSettings.dynamicColors, onCheckedChange = onDynamicColorsChange)
                    ToggleRow(title = "AMOLED dark mode", checked = appSettings.amoledMode, onCheckedChange = onAmoledChange)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Blur intensity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appSettings.blurIntensity.toFloat(),
                        onValueChange = { onBlurIntensityChange(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9
                    )
                    Text("Lyric font size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appSettings.lyricFontScale,
                        onValueChange = onLyricFontScaleChange,
                        valueRange = 0.8f..1.4f,
                        steps = 5
                    )
                    ToggleRow(title = "Karaoke mode", checked = appSettings.karaokeMode, onCheckedChange = onKaraokeChange)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Playback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ToggleRow(title = "Crossfade", checked = appSettings.crossfadeEnabled, onCheckedChange = onCrossfadeChange)
                    ToggleRow(title = "Gapless playback", checked = appSettings.gaplessEnabled, onCheckedChange = onGaplessChange)
                    ToggleRow(title = "Skip silence", checked = appSettings.skipSilence, onCheckedChange = onSkipSilenceChange)
                    ToggleRow(title = "Smart audio focus", checked = appSettings.smartAudioFocus, onCheckedChange = onSmartAudioFocusChange)
                    ToggleRow(title = "Keep playing on app close", checked = appSettings.keepPlayingOnClose, onCheckedChange = onKeepPlayingOnCloseChange)
                    Text("Playlist view columns: ${appSettings.playlistGridColumns}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appSettings.playlistGridColumns.toFloat(),
                        onValueChange = { onPlaylistGridColumnsChange(it.toInt()) },
                        valueRange = 1f..2f,
                        steps = 0
                    )
                }
            }
        }

        item {
            SettingsRow(
                icon = Icons.Default.Refresh,
                title = "Scan local music",
                subtitle = "Refresh songs from device storage",
                onClick = onRefresh
            )
        }

        item {
            SettingsRow(
                icon = Icons.Default.Info,
                title = "About Aura Music",
                subtitle = "Version 2.3.0",
                onClick = onShowAbout
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    selectedTab: AppTab,
    currentSong: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    dominantColor: Color,
    onTabSelected: (AppTab) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        currentSong?.let { song ->
            MiniPlayer(
                song = song,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                dominantColor = dominantColor,
                onOpen = onOpenPlayer,
                onPlayPause = onPlayPause,
                onNext = onNext
            )
        }

        NavigationBar(
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ) {
            AppTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == tab) tab.selectedIcon else tab.icon,
                            contentDescription = tab.title
                        )
                    },
                    label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp)
    )
}

@Composable
private fun EmptyLibrary(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (query.isBlank()) "No local songs found" else "No matching songs",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (query.isBlank()) {
                "Add music files to your device and scan again."
            } else {
                "Try a different song, artist, or album name."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionScreen(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Let Aura Music access your library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The app needs access to your local music and notifications to provide a seamless playback experience.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onGrant) {
                Text("Allow access")
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<com.aman.auramusic.data.model.Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (com.aman.auramusic.data.model.Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            LazyColumn {
                item {
                    TextButton(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Create New Playlist")
                        }
                    }
                }
                items(playlists) { playlist ->
                    TextButton(
                        onClick = { onPlaylistSelected(playlist) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.aura_logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("About Aura Music")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Aura Music is a premium, lightweight offline music player built with Jetpack Compose.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Key Features:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                val features = listOf(
                    "Dynamic artwork colors",
                    "Immersive synced lyrics",
                    "Queue and lyrics sheets",
                    "Local library scan",
                    "AMOLED-friendly playback"
                )

                features.forEach { feature ->
                    Text(text = feature, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Developed by Aman",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Library("Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic)
}

private enum class LibraryTab(val title: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
    Playlists("Playlists"),
    Favorites("Favorites")
}

private enum class LibrarySort(val title: String) {
    DateAdded("Recent"),
    Name("Name"),
    Artist("Artist"),
    Album("Album"),
    Duration("Duration"),
    PlayCount("Play count")
}

@Composable
private fun rememberAudioPermission(): String {
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

@Composable
private fun rememberNotificationPermission(): String {
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            Manifest.permission.POST_NOTIFICATIONS
        }
    }
}

private fun Context.hasAudioPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasNotificationPermission(permission: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

