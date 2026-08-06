package com.aman.auramusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.MiniPlayer
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Divider
import com.aman.auramusic.ui.screen.HomeScreen
import com.aman.auramusic.ui.screen.LibraryScreen
import com.aman.auramusic.ui.screen.OnlinePlaylistsScreen
import com.aman.auramusic.ui.screen.PlayerScreen
import com.aman.auramusic.ui.screen.SearchScreen
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.viewmodel.MusicViewModel
import com.aman.auramusic.viewmodel.OnlinePlaylistViewModel
import com.aman.auramusic.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.OnlinePlaylist
import androidx.lifecycle.ViewModelProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)

        setContent {
            val appSettings by musicViewModel.settings.collectAsStateWithLifecycle()
            
            AuraMusicTheme(
                dynamicColor = appSettings.dynamicColors,
                amoledMode = appSettings.amoledMode
            ) {
                MusicScreen(musicViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                musicViewModel.handleExternalUri(uri)
            }
        }
    }
}

@Composable
fun MusicScreen(musicViewModel: MusicViewModel) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val onlinePlaylistViewModel: OnlinePlaylistViewModel = hiltViewModel()
    
    val songs by musicViewModel.songs.collectAsStateWithLifecycle()
    val matchedOnlinePlaylists by onlinePlaylistViewModel.matchedPlaylists.collectAsStateWithLifecycle()

    // Trigger matching logic when songs are loaded
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            onlinePlaylistViewModel.computeMatches(songs)
        }
    }

    val appSettings by musicViewModel.settings.collectAsStateWithLifecycle()
    val username by musicViewModel.username.collectAsStateWithLifecycle()
    val favoriteIds by musicViewModel.favoriteIds.collectAsStateWithLifecycle()
    val playbackHistory by musicViewModel.playbackHistory.collectAsStateWithLifecycle()
    val playlists by musicViewModel.playlists.collectAsStateWithLifecycle()
    val lastImportResult by musicViewModel.lastImportResult.collectAsStateWithLifecycle()
    val externalSong by musicViewModel.externalSongToPlay.collectAsStateWithLifecycle()

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            musicViewModel.importPlaylistFromFile(inputStream)
        }
    }

    var playlistToExport by remember { mutableStateOf<Playlist?>(null) }
    var exportAllSongsMode by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            val outputStream = context.contentResolver.openOutputStream(exportUri)
            if (exportAllSongsMode) {
                musicViewModel.exportCurrentSongs(songs, outputStream)
                Toast.makeText(context, "All songs exported!", Toast.LENGTH_SHORT).show()
                exportAllSongsMode = false
            } else {
                playlistToExport?.let { playlist ->
                    musicViewModel.exportPlaylistToFile(playlist, outputStream)
                    Toast.makeText(context, "Playlist exported!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = if (appSettings.dynamicColors) Color(playerViewModel.dominantColor) else Color.Transparent,
        animationSpec = tween(1000),
        label = "globalDynamicColor"
    )

    var hasAttemptedRestore by remember { mutableStateOf(false) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && !hasAttemptedRestore) {
            // Only set default queue if we haven't attempted restore yet
            playerViewModel.setQueue(songs)
            playerViewModel.restoreLastState(songs)
            hasAttemptedRestore = true
        }
    }

    var selectedTab by remember { mutableStateOf(AppTab.ListenNow) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(externalSong, playerViewModel.isServiceBound) {
        if (externalSong != null && playerViewModel.isServiceBound) {
            playerViewModel.setQueue(listOf(externalSong!!))
            playerViewModel.play(externalSong!!)
            showPlayer = true
            musicViewModel.clearExternalSong()
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showExportPlaylistDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songsToAddToPlaylist by remember { mutableStateOf<List<Song>?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    val filteredSongs = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }

    val favoriteSongs = remember(songs, favoriteIds) {
        songs.filter { it.id in favoriteIds }
    }

    fun playSong(song: Song, queue: List<Song>, playlistId: Long? = null) {
        playerViewModel.setQueue(queue, playlistId)
        playerViewModel.play(song)
        showPlayer = true
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
            hasStoragePermission = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: hasStoragePermission
        } else {
            hasStoragePermission = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: hasStoragePermission
        }
        
        if (hasStoragePermission) {
            musicViewModel.loadSongs(forceRefresh = true)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (!hasStoragePermission) {
            val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionsToRequest.add(storagePermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    BackHandler(showPlayer || showSettings || selectedPlaylistId != null || selectedAlbumName != null || selectedArtistName != null) {
        when {
            showPlayer -> showPlayer = false
            showSettings -> showSettings = false
            selectedPlaylistId != null -> selectedPlaylistId = null
            selectedAlbumName != null -> selectedAlbumName = null
            selectedArtistName != null -> selectedArtistName = null
        }
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
                            Color.Black.copy(alpha = 0.65f),
                            animatedDominantColor.copy(alpha = 0.35f),
                            animatedDominantColor.copy(alpha = 0.12f),
                            animatedDominantColor.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        showSettings -> {
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
                                onPillPositionChange = { musicViewModel.setPillPosition(it) },
                                onPillVerticalOffsetChange = { musicViewModel.setPillVerticalOffset(it) },
                                onPillSizeScaleChange = { musicViewModel.setPillSizeScale(it) },
                                onDynamicPillChange = { enabled ->
                                    if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                        Toast.makeText(context, "Please enable Overlay Permission", Toast.LENGTH_LONG).show()
                                    } else {
                                        musicViewModel.setDynamicPillEnabled(enabled)
                                    }
                                },
                                onImportPlaylistFile = {
                                    importFileLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                },
                                onExportAllSongs = {
                                    exportAllSongsMode = true
                                    exportFileLauncher.launch("AuraMusic_AllSongs.json")
                                },
                                onExportPlaylist = { showExportPlaylistDialog = true },
                                onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                onBack = { showSettings = false },
                                onShowAbout = { showAboutDialog = true }
                            )
                        }
                        selectedPlaylistId != null -> {
                            val playlist = playlists.find { it.id == selectedPlaylistId }
                            playlist?.let { p: Playlist ->
                                val playlistSongs = p.songIds.mapNotNull { id: Long -> songs.find { it.id == id } }
                                CollectionDetailScreen(
                                    title = p.name,
                                    subtitle = "${playlistSongs.size} songs",
                                    songs = playlistSongs,
                                    favoriteIds = favoriteIds,
                                    currentSongId = playerViewModel.currentSong?.id,
                                    onBack = { selectedPlaylistId = null },
                                    onSongSelected = { playSong(it, playlistSongs, p.id) },
                                    onRemoveSong = { song -> musicViewModel.removeFromPlaylist(p.id, song.id) },
                                    onDeletePlaylist = { 
                                        musicViewModel.deletePlaylist(p.id)
                                        selectedPlaylistId = null
                                    },
                                    onRenamePlaylist = { playlistToRename = p },
                                    onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { playerViewModel.addToQueue(it) },
                                    onAddCollectionToQueue = { playerViewModel.addToQueue(playlistSongs) },
                                    onAddCollectionToPlaylist = { songsToAddToPlaylist = playlistSongs },
                                    onFavoriteCollection = { playlistSongs.forEach { musicViewModel.toggleFavorite(it.id, true) } }
                                )
                            }
                        }
                        selectedAlbumName != null -> {
                            val isChart = selectedAlbumName!!.startsWith("Top")
                            val albumSongs = when (selectedAlbumName) {
                                "Top 100: Global" -> songs.sortedByDescending { it.dateAdded }.take(100)
                                "Top 100: India" -> {
                                    val ind = songs.filter { song ->
                                        val text = "${song.title} ${song.artist} ${song.album} ${song.filePath}".lowercase()
                                        text.contains("hindi") || text.contains("punjabi") || text.contains("bollywood") || text.contains("arijit") || text.contains("pritam") || text.contains("badshah") || text.contains("singh") || text.contains("diljit") || text.contains("jubin")
                                    }
                                    ind.ifEmpty { songs.take(100) }
                                }
                                "Top 25: Mumbai" -> songs.take(25)
                                "Top 25: Delhi" -> songs.reversed().take(25)
                                else -> songs.filter { it.album == selectedAlbumName }
                            }
                            CollectionDetailScreen(
                                title = selectedAlbumName!!,
                                subtitle = if (isChart) "Apple Music Chart • ${albumSongs.size} Songs" else "Album • ${albumSongs.firstOrNull()?.artist ?: "Unknown"}",
                                songs = albumSongs,
                                favoriteIds = favoriteIds,
                                currentSongId = playerViewModel.currentSong?.id,
                                onBack = { selectedAlbumName = null },
                                onSongSelected = { playSong(it, albumSongs) },
                                onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                onAddToPlaylist = { songToAddToPlaylist = it },
                                onAddToQueue = { playerViewModel.addToQueue(it) },
                                onAddCollectionToQueue = { playerViewModel.addToQueue(albumSongs) },
                                onAddCollectionToPlaylist = { songsToAddToPlaylist = albumSongs },
                                onFavoriteCollection = { albumSongs.forEach { musicViewModel.toggleFavorite(it.id, true) } }
                            )
                        }
                        selectedArtistName != null -> {
                            val artistSongs = songs.filter { it.artist == selectedArtistName }
                            CollectionDetailScreen(
                                title = selectedArtistName!!,
                                subtitle = "${artistSongs.size} songs",
                                songs = artistSongs,
                                favoriteIds = favoriteIds,
                                currentSongId = playerViewModel.currentSong?.id,
                                onBack = { selectedArtistName = null },
                                onSongSelected = { playSong(it, artistSongs) },
                                onToggleFavorite = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                onAddToPlaylist = { songToAddToPlaylist = it },
                                onAddToQueue = { playerViewModel.addToQueue(it) },
                                onAddCollectionToQueue = { playerViewModel.addToQueue(artistSongs) },
                                onAddCollectionToPlaylist = { songsToAddToPlaylist = artistSongs },
                                onFavoriteCollection = { artistSongs.forEach { musicViewModel.toggleFavorite(it.id, true) } }
                            )
                        }
                        else -> {
                            when (selectedTab) {
                                AppTab.ListenNow -> HomeScreen(
                                    songs = filteredSongs,
                                    username = username,
                                    history = playbackHistory,
                                    favorites = favoriteSongs,
                                    favoriteIds = favoriteIds,
                                    matchedOnlinePlaylists = matchedOnlinePlaylists,
                                    dominantColor = animatedDominantColor,
                                    onOnlinePlaylistClick = { onlinePlaylist -> },
                                    onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                    onSongSelected = { song, queue -> 
                                        playSong(song, queue) 
                                    },
                                    onFavoriteToggle = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { playerViewModel.addToQueue(it) },
                                    onAlbumSelected = { selectedAlbumName = it },
                                    onArtistSelected = { selectedArtistName = it },
                                    onOpenSettings = { showSettings = true }
                                )
                                AppTab.Library -> LibraryScreen(
                                    songs = filteredSongs,
                                    allSongs = songs,
                                    favoriteSongs = favoriteSongs,
                                    favoriteIds = favoriteIds,
                                    playlists = playlists,
                                    query = query,
                                    currentSongId = playerViewModel.currentSong?.id,
                                    playlistGridColumns = appSettings.playlistGridColumns,
                                    onQueryChange = { query = it },
                                    onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                    onFavoriteToggle = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                    onSongSelected = { song, queue -> 
                                        playSong(song, queue) 
                                    },
                                    onOpenSettings = { showSettings = true },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onCreatePlaylist = { showNewPlaylistDialog = true },
                                    onPlaylistSelected = { selectedPlaylistId = it.id },
                                    onPlaylistExport = { 
                                        playlistToExport = it
                                        exportFileLauncher.launch("${it.name}.aura")
                                    },
                                    onAlbumSelected = { selectedAlbumName = it },
                                    onArtistSelected = { selectedArtistName = it }
                                )
                                AppTab.Search -> SearchScreen(
                                    songs = songs,
                                    favoriteIds = favoriteIds,
                                    currentSongId = playerViewModel.currentSong?.id,
                                    query = query,
                                    onQueryChange = { query = it },
                                    onSongSelected = { song, queue -> playSong(song, queue) },
                                    onFavoriteToggle = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { playerViewModel.addToQueue(it) },
                                    onAlbumSelected = { selectedAlbumName = it },
                                    onArtistSelected = { selectedArtistName = it }
                                )
                            }
                        }
                    }
                }

                playerViewModel.currentSong?.takeIf { !showPlayer }?.let { song ->
                    MiniPlayer(
                        song = song,
                        isPlaying = playerViewModel.isPlaying,
                        position = playerViewModel.currentPosition,
                        duration = playerViewModel.duration,
                        dominantColor = animatedDominantColor,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.playNext() },
                        onPrevious = { playerViewModel.playPrevious() },
                        onOpen = { showPlayer = true }
                    )
                }

                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { 
                        selectedTab = it
                        showSettings = false
                        selectedPlaylistId = null
                        selectedAlbumName = null
                        selectedArtistName = null
                    }
                )
            }
        }

        playerViewModel.currentSong?.takeIf { showPlayer }?.let { song ->
            val queueState by playerViewModel.queue.collectAsStateWithLifecycle()
            val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
            val historySongs = remember(songs, playbackHistory, song.id) {
                playbackHistory
                    .sortedByDescending { it.playedAt }
                    .filter { it.songId != song.id }
                    .mapNotNull { entry -> songs.find { it.id == entry.songId } }
                    .distinctBy { it.id }
                    .take(10)
                    .reversed()
            }
            
            PlayerScreen(
                song = song,
                isPlaying = playerViewModel.isPlaying,
                position = playerViewModel.currentPosition,
                duration = playerViewModel.duration,
                lyrics = lyrics,
                queue = queueState,
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

        if (songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { songToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    musicViewModel.addToPlaylist(playlist.id, songToAddToPlaylist!!.id)
                    songToAddToPlaylist = null
                },
                onCreateNew = {
                    showNewPlaylistDialog = true
                }
            )
        }

        if (songsToAddToPlaylist != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { songsToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    songsToAddToPlaylist?.forEach { s ->
                        musicViewModel.addToPlaylist(playlist.id, s.id)
                    }
                    songsToAddToPlaylist = null
                },
                onCreateNew = {
                    showNewPlaylistDialog = true
                }
            )
        }

        if (showNewPlaylistDialog) {
            NewPlaylistDialog(
                onDismiss = { showNewPlaylistDialog = false },
                onConfirm = { name ->
                    val ids = songsToAddToPlaylist?.map { it.id }
                        ?: songToAddToPlaylist?.let { listOf(it.id) }
                        ?: emptyList()
                    musicViewModel.savePlaylist(name, ids)
                    showNewPlaylistDialog = false
                    songToAddToPlaylist = null
                    songsToAddToPlaylist = null
                }
            )
        }

        if (playlistToRename != null) {
            RenamePlaylistDialog(
                currentName = playlistToRename!!.name,
                onDismiss = { playlistToRename = null },
                onRename = { newName ->
                    musicViewModel.renamePlaylist(playlistToRename!!.id, newName)
                    playlistToRename = null
                }
            )
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }

        if (showExportPlaylistDialog) {
            ExportPlaylistDialog(
                playlists = playlists,
                onDismiss = { showExportPlaylistDialog = false },
                onPlaylistSelected = { playlist ->
                    playlistToExport = playlist
                    exportFileLauncher.launch("${playlist.name}.aura")
                    showExportPlaylistDialog = false
                }
            )
        }

        if (lastImportResult != null) {
            ImportResultDialog(
                result = lastImportResult!!,
                onDismiss = { musicViewModel.clearImportResult() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    songs: List<Song>,
    username: String,
    history: List<com.aman.auramusic.data.model.PlaybackHistoryEntry>,
    favorites: List<Song>,
    favoriteIds: Set<Long>,
    matchedOnlinePlaylists: List<OnlinePlaylist>,
    dominantColor: Color,
    onOnlinePlaylistClick: (OnlinePlaylist) -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSongOptions by remember { mutableStateOf<Song?>(null) }
    var mixQueue by remember { mutableStateOf<List<Song>>(emptyList()) }
    val albums = remember(songs) { songs.groupBy { it.album }.entries.toList().shuffled().take(10) }

    if (selectedSongOptions != null) {
        SongOptionsDialog(
            song = selectedSongOptions!!,
            isFavorite = selectedSongOptions!!.id in favoriteIds,
            onDismiss = { selectedSongOptions = null },
            onPlay = {
                onSongSelected(selectedSongOptions!!, mixQueue)
                selectedSongOptions = null
            },
            onToggleFavorite = { 
                onFavoriteToggle(selectedSongOptions!!)
                selectedSongOptions = null 
            },
            onAddToPlaylist = { 
                onAddToPlaylist(selectedSongOptions!!)
                selectedSongOptions = null 
            }
        )
    }

    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
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
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .background(
                                color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Quick Picks")
        }
        item {
            val mix = remember(songs) { songs.shuffled().take(6) }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                mix.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { song ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onSongSelected(song, songs) },
                                        onLongClick = { 
                                            mixQueue = songs
                                            selectedSongOptions = song 
                                        }
                                    )
                            ) {
                                SongArtwork(song = song, size = 120, modifier = Modifier.fillMaxSize())
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                SectionHeader(title = "Listen Again", icon = Icons.Default.History)
            }
            item {
                val historySongs = history.mapNotNull { entry -> songs.find { it.id == entry.songId } }.distinct().take(10)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historySongs, key = { it.id }) { song ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .combinedClickable(
                                    onClick = { onSongSelected(song, songs) },
                                    onLongClick = { 
                                        mixQueue = songs
                                        selectedSongOptions = song 
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SongArtwork(
                                song = song, 
                                size = 120, 
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (matchedOnlinePlaylists.isNotEmpty()) {
            item {
                SectionHeader(title = "Online Playlists for You", icon = Icons.Default.Info)
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(matchedOnlinePlaylists, key = { it.id }) { playlist ->
                        OnlinePlaylistCompactCard(
                            playlist = playlist,
                            onClick = { onOnlinePlaylistClick(playlist) }
                        )
                    }
                }
            }
        }

        if (albums.isNotEmpty()) {
            item {
                SectionHeader(title = "Featured Albums")
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(albums, key = { it.key }) { entry ->
                        val albumName = entry.key
                        val firstSong = entry.value.first()
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onAlbumSelected(albumName) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SongArtwork(
                                song = firstSong, 
                                size = 120, 
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = albumName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = firstSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Your Favorites")
        }
        
        if (favorites.isEmpty()) {
            item {
                Text(
                    text = "No favorites yet. Start hearting!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            items(favorites.take(5), key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isPlaying = false,
                    isFavorite = true,
                    onPlayNow = { onSongSelected(song, favorites) },
                    onToggleFavorite = { onFavoriteToggle(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onClick = { onSongSelected(song, favorites) },
                    onLongClick = { 
                        mixQueue = favorites
                        selectedSongOptions = song 
                    }
                )
            }
        }

        /* Removed redundant dialog call from end of LazyColumn */
    }
}

@Composable
private fun OnlinePlaylistCompactCard(
    playlist: OnlinePlaylist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = playlist.cover,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongOptionsDialog(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SongArtwork(song = song, size = 100, shape = RoundedCornerShape(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Play", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    TextButton(
                        onClick = onAddToPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Playlist", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    favoriteIds: Set<Long>,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onRemoveSong: ((Song) -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null,
    onRenamePlaylist: (() -> Unit)? = null,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddCollectionToQueue: (() -> Unit)? = null,
    onAddCollectionToPlaylist: (() -> Unit)? = null,
    onFavoriteCollection: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val firstSong = songs.firstOrNull()
    val totalMinutes = songs.sumOf { it.duration / 1000 } / 60

    var showTopMenu by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Header (Back, Share, Overflow 3-dot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out $title on AuraMusic!")
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }

                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Playlist Options",
                                    tint = if (isDark) Color.White else Color.Black
                                )
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false }
                            ) {
                                if (onAddCollectionToQueue != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add Playlist to Queue", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onAddCollectionToQueue()
                                        }
                                    )
                                }
                                if (onAddCollectionToPlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add Playlist to...", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onAddCollectionToPlaylist()
                                        }
                                    )
                                }
                                if (onFavoriteCollection != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add All to Favorites", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFA2D48)) },
                                        onClick = {
                                            showTopMenu = false
                                            onFavoriteCollection()
                                        }
                                    )
                                }
                                if (onRenamePlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Playlist", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onRenamePlaylist()
                                        }
                                    )
                                }
                                if (onDeletePlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showTopMenu = false
                                            onDeletePlaylist()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Centered Poster Art (Apple Music Style)
                SongArtwork(
                    song = firstSong,
                    size = 250,
                    shape = RoundedCornerShape(20.dp),
                    elevation = 12.dp,
                    modifier = Modifier.shadow(12.dp, RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle (Artists)
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Audio Format Pill
                Text(
                    text = "Bollywood • 2026 • Lossless",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // iOS Action Controls (Shuffle circle, Pill Play, Add circle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { songs.shuffled().firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { songs.firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White else Color.Black,
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box {
                        IconButton(
                            onClick = { showPlusMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add options",
                                tint = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            if (onAddCollectionToPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showPlusMenu = false
                                        onAddCollectionToPlaylist()
                                    }
                                )
                            }
                            if (onAddCollectionToQueue != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        showPlusMenu = false
                                        onAddCollectionToQueue()
                                    }
                                )
                            }
                            if (onFavoriteCollection != null) {
                                DropdownMenuItem(
                                    text = { Text("Favorite All Tracks", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFA2D48)) },
                                    onClick = {
                                        showPlusMenu = false
                                        onFavoriteCollection()
                                    }
                                )
                            }
                            if (onDeletePlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showPlusMenu = false
                                        onDeletePlaylist()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Track Listing (Numbered 1..N)
        if (songs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No songs in this collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                var itemMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelected(song) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        modifier = Modifier.width(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (song.id == currentSongId) Color(0xFFFA2D48) else if (isDark) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { itemMenuExpanded = true },
                            modifier = Modifier
                                .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.08f), CircleShape)
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = itemMenuExpanded,
                            onDismissRequest = { itemMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Play Now", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    itemMenuExpanded = false
                                    onSongSelected(song)
                                }
                            )
                            val isFav = song.id in favoriteIds
                            DropdownMenuItem(
                                text = { Text(if (isFav) "Remove from Favorites" else "Add to Favorites", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFav) Color(0xFFFA2D48) else MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    itemMenuExpanded = false
                                    onToggleFavorite(song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    itemMenuExpanded = false
                                    onAddToPlaylist(song)
                                }
                            )
                            if (onAddToQueue != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onAddToQueue(song)
                                    }
                                )
                            }
                            if (onRemoveSong != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove from Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onRemoveSong(song)
                                    }
                                )
                            }
                        }
                    }
                }
                Divider(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 52.dp, end = 20.dp)
                )
            }
        }

        // Footer Album Metadata
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "July 21, 2026",
                    fontSize = 13.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                )
                Text(
                    text = "${songs.size} songs, ${totalMinutes} minutes",
                    fontSize = 13.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                )
                Text(
                    text = "℗ 2026 AuraMusic LLC Under Exclusive License",
                    fontSize = 12.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RECORD LABEL AuraMusic",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Related Carousels
        if (songs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More by ${firstSong?.artist ?: "Artist"}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(songs.take(6), key = { "more_${it.id}" }) { s ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { onSongSelected(s) }
                            ) {
                                SongArtwork(
                                    song = s,
                                    size = 130,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = s.album,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = s.artist,
                                    fontSize = 11.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRail(
    playlists: List<Playlist>,
    songById: Map<Long, Song>,
    columns: Int,
    onPlaylistSelected: (Playlist) -> Unit,
    onAddToQueue: ((Playlist) -> Unit)? = null,
    onAddToPlaylist: ((Playlist) -> Unit)? = null,
    onFavoritePlaylist: ((Playlist) -> Unit)? = null,
    onRenamePlaylist: ((Playlist) -> Unit)? = null,
    onDeletePlaylist: ((Playlist) -> Unit)? = null
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
    val chunks = remember(sorted, columns) { sorted.chunked(columns) }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        chunks.forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                chunk.forEach { playlist ->
                    val previewSong = playlist.artworkSongId?.let(songById::get)
                        ?: playlist.songIds.firstOrNull()?.let(songById::get)
                    Box(modifier = Modifier.weight(1f)) {
                        PlaylistPreviewCard(
                            playlistName = playlist.name,
                            songCount = playlist.songIds.size,
                            previewSong = previewSong,
                            onClick = { onPlaylistSelected(playlist) },
                            onAddToQueue = onAddToQueue?.let { { it(playlist) } },
                            onAddToPlaylist = onAddToPlaylist?.let { { it(playlist) } },
                            onFavoritePlaylist = onFavoritePlaylist?.let { { it(playlist) } },
                            onRenamePlaylist = onRenamePlaylist?.let { { it(playlist) } },
                            onDeletePlaylist = onDeletePlaylist?.let { { it(playlist) } }
                        )
                    }
                }
                // Fill empty slots if last chunk is incomplete
                if (chunk.size < columns) {
                    repeat(columns - chunk.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistPreviewCard(
    playlistName: String,
    songCount: Int,
    previewSong: Song?,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onFavoritePlaylist: (() -> Unit)? = null,
    onRenamePlaylist: (() -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.height(160.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                previewSong?.let {
                    SongArtwork(
                        song = it,
                        size = 200,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Playlist Options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onAddToQueue != null) {
                            DropdownMenuItem(
                                text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAddToQueue()
                                }
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAddToPlaylist()
                                }
                            )
                        }
                        if (onFavoritePlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Favorite All Songs", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFA2D48)) },
                                onClick = {
                                    showMenu = false
                                    onFavoritePlaylist()
                                }
                            )
                        }
                        if (onRenamePlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Rename Playlist", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRenamePlaylist()
                                }
                            )
                        }
                        if (onDeletePlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Delete Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeletePlaylist()
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = playlistName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = "$songCount songs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MadeForYouCard(
    songs: List<Song>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Quick Mix", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "Play your favorites instantly", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun EmptyLibrary(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No songs found for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Add to Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Create New Playlist", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(playlists) { playlist ->
                        TextButton(onClick = { onPlaylistSelected(playlist) }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ExportPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Export Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (playlists.isEmpty()) {
                    Text(text = "No playlists to export", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { playlist ->
                            TextButton(onClick = { onPlaylistSelected(playlist) }, modifier = Modifier.fillMaxWidth()) {
                                Text(text = playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ImportResultDialog(
    result: MusicViewModel.ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Result") },
        text = {
            Column {
                Text("Playlist: ${result.playlistName}", fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Successfully matched: ${result.matchedCount} local songs")
                
                if (result.unmatchedSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Unmatched songs (${result.unmatchedSongs.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .padding(top = 8.dp)
                    ) {
                        items(result.unmatchedSongs) { song ->
                            Text(
                                text = "• $song",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "New Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = { if (name.isNotBlank()) onConfirm(name) },
                        enabled = name.isNotBlank()
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Rename Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Playlist Name") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onRename(newName) }) { Text("Rename") }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .navigationBarsPadding()
            .shadow(16.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val tint = if (selected) Color(0xFFFA2D48) else Color.White.copy(alpha = 0.60f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tint
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
    onPillPositionChange: (Int) -> Unit,
    onPillVerticalOffsetChange: (Int) -> Unit,
    onPillSizeScaleChange: (Float) -> Unit,
    onDynamicPillChange: (Boolean) -> Unit,
    onImportPlaylistFile: () -> Unit,
    onExportAllSongs: () -> Unit,
    onExportPlaylist: () -> Unit,
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
                    ToggleRow(title = "Dynamic Pill (Overlay)", checked = appSettings.dynamicPillEnabled, onCheckedChange = onDynamicPillChange)
                    
                    if (appSettings.dynamicPillEnabled) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            val posText = when(appSettings.pillPosition) {
                                0 -> "Left"
                                2 -> "Right"
                                else -> "Center"
                            }
                            Text("Pill position: $posText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = appSettings.pillPosition.toFloat(),
                                onValueChange = { onPillPositionChange(it.toInt()) },
                                valueRange = 0f..2f,
                                steps = 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pill vertical offset: ${appSettings.pillVerticalOffset}dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = appSettings.pillVerticalOffset.toFloat(),
                                onValueChange = { onPillVerticalOffsetChange(it.toInt()) },
                                valueRange = 0f..64f,
                                steps = 15
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pill size scale: ${String.format("%.1f", appSettings.pillSizeScale)}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = appSettings.pillSizeScale,
                                onValueChange = { onPillSizeScaleChange(it) },
                                valueRange = 1.0f..2.0f,
                                steps = 9
                            )
                        }
                    }

                    ToggleRow(title = "Karaoke mode", checked = appSettings.karaokeMode, onCheckedChange = onKaraokeChange)
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
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("System & Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    SettingsRow(
                        icon = Icons.Default.ArrowUpward,
                        title = "Import from file",
                        subtitle = "Import shared playlist files",
                        onClick = onImportPlaylistFile
                    )

                    SettingsRow(
                        icon = Icons.Default.PlaylistAdd,
                        title = "Export playlist",
                        subtitle = "Export your playlists to a file",
                        onClick = onExportPlaylist
                    )

                    SettingsRow(
                        icon = Icons.Default.ArrowDownward,
                        title = "Export all songs",
                        subtitle = "Export all songs present on device to JSON",
                        onClick = onExportAllSongs
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
                    subtitle = "Version 2.7.0",
                    onClick = onShowAbout
                )
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.aura_logo),
                            contentDescription = "Aura Logo",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Aura Music",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Version 3.0.0 (Glass Edition)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "A premium, Apple Music-inspired player with stable background playback, intelligent audio focus, and cross-user playlist sharing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Developed by Aman",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ListenNow("Home", Icons.Default.Home, Icons.Default.Home),
    Library("Library", Icons.Default.LibraryMusic, Icons.Default.LibraryMusic),
    Search("Search", Icons.Default.Search, Icons.Default.Search)
}


