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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Sensors
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
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.ui.component.MiniPlayer
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.component.SongOptionsDialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Divider
import com.aman.auramusic.ui.screen.HomeScreen
import com.aman.auramusic.ui.screen.hometest.HomeTestScreen
import com.aman.auramusic.ui.screen.OnlineScreen
import com.aman.auramusic.ui.screen.LibraryScreen
import androidx.compose.runtime.DisposableEffect
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.model.toLyricLines
import com.aman.auramusic.online.ui.components.StreamDiagnosticsSheet
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.playback.RepeatMode
import com.aman.auramusic.viewmodel.QueueEntry
import com.aman.auramusic.ui.screen.PlayerScreen
import com.aman.auramusic.ui.screen.SearchScreen
import com.aman.auramusic.ui.screen.SettingsScreen
import com.aman.auramusic.ui.screen.CollectionDetailScreen
import com.aman.auramusic.ui.component.BottomNavBar
import com.aman.auramusic.ui.component.AppTab
import com.aman.auramusic.ui.component.MadeForYouCard
import com.aman.auramusic.ui.component.EmptyLibrary
import com.aman.auramusic.ui.component.PlaylistRail
import com.aman.auramusic.ui.component.AddToPlaylistDialog
import com.aman.auramusic.ui.component.ExportPlaylistDialog
import com.aman.auramusic.ui.component.ImportResultDialog
import com.aman.auramusic.ui.component.NewPlaylistDialog
import com.aman.auramusic.ui.component.RenamePlaylistDialog
import com.aman.auramusic.ui.screen.AboutDialog
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.viewmodel.MusicViewModel
import com.aman.auramusic.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
                themeMode = appSettings.themeMode,
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
    
    val songs by musicViewModel.songs.collectAsStateWithLifecycle()
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

    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val onlinePlaybackManager = remember { OnlinePlaybackManager(context) }
    val onlinePlaybackState by onlinePlaybackManager.playbackState.collectAsStateWithLifecycle()
    var isOnlinePlaybackActive by remember { mutableStateOf(false) }
    var activeOnlineSong by remember { mutableStateOf<OnlineSong?>(null) }
    var showOnlineDiagnostics by remember { mutableStateOf(false) }

    DisposableEffect(onlinePlaybackManager) {
        onlinePlaybackManager.eventListener = object : OnlinePlaybackManager.PlaybackEventListener {
            override fun onSongChanged(song: OnlineSong) {
                activeOnlineSong = song
            }

            override fun onPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.updateOnlinePlaybackState(song, isPlaying, positionMs)
                }
            }

            override fun onProgressUpdate(song: OnlineSong, positionMs: Long) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.onOnlineProgressUpdate(song, positionMs)
                }
            }

            override fun onSeek(song: OnlineSong, positionMs: Long) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.onOnlineSeek(song, positionMs)
                }
            }

            override fun onLyricsLoaded(song: OnlineSong, lyrics: com.aman.auramusic.online.lyrics.model.SongLyrics) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.updateOnlineLyrics(song, lyrics.toLyricLines())
                }
            }

            override fun onPlaybackStopped(song: OnlineSong?) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.stopOnlinePlayback()
                }
            }
        }
        onDispose {
            onlinePlaybackManager.eventListener = null
            onlinePlaybackManager.release()
        }
    }

    LaunchedEffect(externalSong, playerViewModel.isServiceBound) {
        if (externalSong != null && playerViewModel.isServiceBound) {
            isOnlinePlaybackActive = false
            activeOnlineSong = null
            onlinePlaybackManager.pause()
            playerViewModel.stopOnlinePlayback()
            playerViewModel.setQueue(listOf(externalSong!!))
            playerViewModel.play(externalSong!!)
            showPlayer = true
            musicViewModel.clearExternalSong()
        }
    }

    LaunchedEffect(onlinePlaybackState.currentSong, onlinePlaybackState.isPlaying, isOnlinePlaybackActive) {
        if (isOnlinePlaybackActive) {
            val songToSync = onlinePlaybackState.currentSong ?: activeOnlineSong
            if (songToSync != null) {
                playerViewModel.updateOnlinePlaybackState(
                    song = songToSync,
                    isPlaying = onlinePlaybackState.isPlaying,
                    positionMs = onlinePlaybackState.currentPositionMs,
                    durationMs = onlinePlaybackState.durationMs
                )
            }
        }
    }

    LaunchedEffect(onlinePlaybackState.currentLyrics, isOnlinePlaybackActive) {
        if (isOnlinePlaybackActive) {
            val currentLyrics = onlinePlaybackState.currentLyrics
            val songToSync = onlinePlaybackState.currentSong ?: activeOnlineSong
            if (currentLyrics != null && songToSync != null) {
                playerViewModel.updateOnlineLyrics(songToSync, currentLyrics.toLyricLines())
            }
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
        isOnlinePlaybackActive = false
        activeOnlineSong = null
        onlinePlaybackManager.pause()
        playerViewModel.stopOnlinePlayback()
        playerViewModel.setQueue(queue, playlistId)
        playerViewModel.play(song)
        showPlayer = true
    }

    fun playOnlineSong(onlineSong: OnlineSong, queue: List<OnlineSong>) {
        isOnlinePlaybackActive = true
        activeOnlineSong = onlineSong
        playerViewModel.pause()
        onlinePlaybackManager.playSong(onlineSong, queue)
        playerViewModel.startOnlinePlayback(
            song = onlineSong,
            isPlaying = true,
            onPlayPause = { onlinePlaybackManager.togglePlayPause() },
            onNext = { onlinePlaybackManager.playNext() },
            onPrevious = { onlinePlaybackManager.playPrevious() },
            onSeekTo = { onlinePlaybackManager.seekTo(it) }
        )
        onlinePlaybackManager.playbackState.value.currentLyrics?.let { cachedLyrics ->
            playerViewModel.updateOnlineLyrics(onlineSong, cachedLyrics.toLyricLines())
        }
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

    BackHandler(showOnlineDiagnostics || showPlayer || showSettings || selectedPlaylistId != null || selectedAlbumName != null || selectedArtistName != null || selectedTab != AppTab.Home) {
        when {
            showOnlineDiagnostics -> showOnlineDiagnostics = false
            showPlayer -> showPlayer = false
            showSettings -> showSettings = false
            selectedPlaylistId != null -> selectedPlaylistId = null
            selectedAlbumName != null -> selectedAlbumName = null
            selectedArtistName != null -> selectedArtistName = null
            selectedTab != AppTab.Home -> selectedTab = AppTab.Home
        }
    }

    val isDark = LocalIsDark.current
    val topBgColor = if (isDark) Color.Black.copy(alpha = 0.65f) else MaterialTheme.colorScheme.background

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
                            topBgColor,
                            animatedDominantColor.copy(alpha = if (isDark) 0.35f else 0.15f),
                            animatedDominantColor.copy(alpha = if (isDark) 0.12f else 0.05f),
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
                                onThemeModeChange = { mode -> musicViewModel.setThemeMode(mode) },
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
                                AppTab.Home -> HomeTestScreen(
                                    songs = filteredSongs,
                                    username = username,
                                    history = playbackHistory,
                                    favorites = favoriteSongs,
                                    favoriteIds = favoriteIds,
                                    currentSongId = playerViewModel.currentSong?.id,
                                    isPlaying = playerViewModel.isPlaying,
                                    onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                    onSongSelected = { song, queue ->
                                        playSong(song, queue)
                                    },
                                    onFavoriteToggle = { song -> musicViewModel.toggleFavorite(song.id, song.id !in favoriteIds) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { playerViewModel.addToQueue(it) },
                                    onOnlineSongSelected = { onlineSong, queue ->
                                        playOnlineSong(onlineSong, queue)
                                    },
                                    onAlbumSelected = { selectedAlbumName = it },
                                    onArtistSelected = { selectedArtistName = it },
                                    onPlaylistSelected = { selectedPlaylistId = it },
                                    onOpenSettings = { showSettings = true }
                                )
                                AppTab.Online -> OnlineScreen(
                                    onlinePlaybackManager = onlinePlaybackManager,
                                    onOnlineSongSelected = { onlineSong, queue ->
                                        playOnlineSong(onlineSong, queue)
                                    },
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
                                    onArtistSelected = { selectedArtistName = it },
                                    onlinePlaybackManager = onlinePlaybackManager,
                                    onOnlineSongSelected = { onlineSong, queue ->
                                        playOnlineSong(onlineSong, queue)
                                    }
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
                                    onArtistSelected = { selectedArtistName = it },
                                    onlinePlaybackManager = onlinePlaybackManager,
                                    onOnlineSongSelected = { onlineSong, queue ->
                                        playOnlineSong(onlineSong, queue)
                                    }
                                )
                            }
                        }
                    }
                }

                if (!showPlayer) {
                    val currentOnlineMiniSong = onlinePlaybackState.currentSong ?: activeOnlineSong
                    if (isOnlinePlaybackActive && currentOnlineMiniSong != null) {
                        val onlineAsSong = remember(currentOnlineMiniSong, onlinePlaybackState.durationMs) {
                            val base = currentOnlineMiniSong.toSong()
                            if (onlinePlaybackState.durationMs > 0) base.copy(duration = onlinePlaybackState.durationMs) else base
                        }
                        MiniPlayer(
                            song = onlineAsSong,
                            isPlaying = onlinePlaybackState.isPlaying,
                            position = onlinePlaybackState.currentPositionMs,
                            duration = onlinePlaybackState.durationMs,
                            dominantColor = animatedDominantColor,
                            onPlayPause = { onlinePlaybackManager.togglePlayPause() },
                            onNext = { onlinePlaybackManager.playNext() },
                            onPrevious = { onlinePlaybackManager.playPrevious() },
                            onOpen = { showPlayer = true }
                        )
                    } else {
                        playerViewModel.currentSong?.let { song ->
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
                    }
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

        if (showPlayer) {
            val currentOnlineFullSong = onlinePlaybackState.currentSong ?: activeOnlineSong
            if (isOnlinePlaybackActive && currentOnlineFullSong != null) {
                val onlineAsSong = remember(currentOnlineFullSong, onlinePlaybackState.durationMs) {
                    val base = currentOnlineFullSong.toSong()
                    if (onlinePlaybackState.durationMs > 0) base.copy(duration = onlinePlaybackState.durationMs) else base
                }
                val onlineQueue = remember(onlinePlaybackState.queue) {
                    onlinePlaybackState.queue.mapIndexed { index, os ->
                        QueueEntry(
                            id = "${os.id}_$index",
                            song = os.toSong()
                        )
                    }
                }
                val onlineLyrics = remember(onlinePlaybackState.currentLyrics) {
                    onlinePlaybackState.currentLyrics?.toLyricLines() ?: emptyList()
                }

                PlayerScreen(
                    song = onlineAsSong,
                    isPlaying = onlinePlaybackState.isPlaying,
                    position = onlinePlaybackState.currentPositionMs,
                    duration = onlinePlaybackState.durationMs,
                    lyrics = onlineLyrics,
                    queue = onlineQueue,
                    history = emptyList(),
                    onBack = { showPlayer = false },
                    onPlayPause = { onlinePlaybackManager.togglePlayPause() },
                    onSeek = { onlinePlaybackManager.seekTo(it) },
                    onPrevious = { onlinePlaybackManager.playPrevious() },
                    onNext = { onlinePlaybackManager.playNext() },
                    onSongSelected = { selected ->
                        val target = onlinePlaybackState.queue.find { os ->
                            (os.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == selected.id || os.title == selected.title
                        }
                        if (target != null) {
                            onlinePlaybackManager.playSong(target)
                        }
                    },
                    onQueueRemove = { selected ->
                        val target = onlinePlaybackState.queue.find { os ->
                            (os.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == selected.id || os.title == selected.title
                        }
                        if (target != null) {
                            onlinePlaybackManager.removeQueueItem(target.id)
                        }
                    },
                    onQueueClear = { onlinePlaybackManager.clearQueueExceptCurrent() },
                    onQueueSave = {},
                    onHistoryClear = {},
                    onAddToPlaylist = { songToAddToPlaylist = onlineAsSong },
                    appSettings = appSettings,
                    playerViewModel = playerViewModel,
                    isFavorite = onlineAsSong.id in favoriteIds,
                    onFavoriteToggle = { musicViewModel.toggleFavorite(onlineAsSong.id, onlineAsSong.id !in favoriteIds) },
                    isShuffled = onlinePlaybackState.isShuffleEnabled,
                    repeatMode = if (onlinePlaybackState.isRepeatEnabled) RepeatMode.ONE else RepeatMode.NONE,
                    onToggleShuffle = { onlinePlaybackManager.toggleShuffle() },
                    onToggleRepeat = { onlinePlaybackManager.toggleRepeat() },
                    onAddToQueue = { selected ->
                        val target = onlinePlaybackState.queue.find { os ->
                            (os.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == selected.id || os.title == selected.title
                        } ?: OnlineSong(
                            id = selected.id.toString(),
                            title = selected.title,
                            artist = selected.artist,
                            album = selected.album,
                            artworkUrl = selected.artworkUri ?: "",
                            durationSeconds = selected.duration / 1000L,
                            streamUrl = selected.filePath
                        )
                        onlinePlaybackManager.addToQueue(target)
                    },
                    onMoveQueueItem = { from, to -> onlinePlaybackManager.moveQueueItem(from, to) },
                    onOpenDiagnostics = { showOnlineDiagnostics = true }
                )
            } else {
                playerViewModel.currentSong?.let { song ->
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
            }
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


        if (showOnlineDiagnostics) {
            StreamDiagnosticsSheet(
                state = onlinePlaybackState,
                onChangeBitrate = { onlinePlaybackManager.changeBitrate(it) },
                onChangeEngine = { onlinePlaybackManager.setStreamingEngine(it) },
                onDismiss = { showOnlineDiagnostics = false }
            )
        }
    }
}
