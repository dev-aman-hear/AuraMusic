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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.ui.component.MiniPlayer
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.component.SongOptionsDialog
import androidx.compose.ui.draw.shadow
import com.aman.auramusic.ui.screen.home.HomeScreen
import com.aman.auramusic.ui.screen.OnlineScreen
import com.aman.auramusic.ui.screen.LibraryScreen
import com.aman.auramusic.ui.screen.LibraryTab
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.online.player.OnlinePlaybackManagerHolder
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.model.toLyricLines
import com.aman.auramusic.online.util.LastPlayedStore
import com.aman.auramusic.online.ui.components.StreamDiagnosticsSheet
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.playback.RepeatMode
import com.aman.auramusic.viewmodel.QueueEntry
import com.aman.auramusic.ui.screen.PlayerScreen
import com.aman.auramusic.ui.screen.SearchScreen
import com.aman.auramusic.ui.screen.SearchFilter
import com.aman.auramusic.ui.screen.SettingsScreen
import com.aman.auramusic.ui.screen.CollectionDetailScreen
import com.aman.auramusic.ui.screen.home.ArtistDetailSheet
import com.aman.auramusic.ui.screen.home.ArtistItem
import com.aman.auramusic.ui.theme.AppArea
import com.aman.auramusic.ui.theme.AppThemedScreen
import com.aman.auramusic.online.network.repository.ArtistBioData
import com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.model.SavedOnlineArtist
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.ui.component.BottomNavBar
import com.aman.auramusic.ui.component.AppTab
import com.aman.auramusic.ui.component.MadeForYouCard
import com.aman.auramusic.ui.component.AddToPlaylistDialog
import com.aman.auramusic.ui.component.ExportPlaylistDialog
import com.aman.auramusic.ui.component.ImportResultDialog
import com.aman.auramusic.ui.component.NewPlaylistDialog
import com.aman.auramusic.ui.component.RenamePlaylistDialog
import com.aman.auramusic.ui.screen.AboutDialog
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.viewmodel.MusicViewModel
import com.aman.auramusic.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import com.google.gson.Gson
import java.io.File
import com.aman.auramusic.util.ArtistArtworkCache

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
                themeMode = appSettings.themeMode
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

sealed interface ExportTarget {
    data object AllSongs : ExportTarget
    data class SinglePlaylist(val playlist: Playlist) : ExportTarget
}

@Composable
fun MusicScreen(musicViewModel: MusicViewModel) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = hiltViewModel()
    
    val songs by musicViewModel.songs.collectAsStateWithLifecycle()
    val appSettings by musicViewModel.settings.collectAsStateWithLifecycle()
    val username by musicViewModel.username.collectAsStateWithLifecycle()
    val favoriteIds by musicViewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteOnlineSongs by musicViewModel.favoriteOnlineSongs.collectAsStateWithLifecycle()
    val playbackHistory by musicViewModel.playbackHistory.collectAsStateWithLifecycle()
    val playlists by musicViewModel.playlists.collectAsStateWithLifecycle()
    val savedOnlinePlaylists by musicViewModel.savedOnlinePlaylists.collectAsStateWithLifecycle()
    val savedOnlineArtists by musicViewModel.savedOnlineArtists.collectAsStateWithLifecycle()
    val savedOnlineAlbums by musicViewModel.savedOnlineAlbums.collectAsStateWithLifecycle()
    val lastImportResult by musicViewModel.lastImportResult.collectAsStateWithLifecycle()
    val externalSong by musicViewModel.externalSongToPlay.collectAsStateWithLifecycle()

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (inputStream != null) {
                musicViewModel.importPlaylistFromFile(inputStream)
            } else {
                Toast.makeText(context, "Unable to read selected file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingExportTarget by remember { mutableStateOf<ExportTarget?>(null) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val target = pendingExportTarget
        pendingExportTarget = null
        if (uri != null && target != null) {
            val outputStream = try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (outputStream == null) {
                Toast.makeText(context, "Unable to access file destination", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            when (target) {
                is ExportTarget.AllSongs -> {
                    musicViewModel.exportCurrentSongs(songs, outputStream)
                }
                is ExportTarget.SinglePlaylist -> {
                    musicViewModel.exportPlaylistToFile(target.playlist, outputStream)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        musicViewModel.operationEvent.collect { event ->
            val message = when (event) {
                is MusicViewModel.OperationMessage.Success -> event.message
                is MusicViewModel.OperationMessage.Error -> event.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.userFeedback.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var hasAttemptedRestore by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var selectedLibraryTab by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { it.name },
            restore = { name -> runCatching { LibraryTab.valueOf(name) }.getOrDefault(LibraryTab.SONGS) }
        )
    ) { mutableStateOf(LibraryTab.SONGS) }
    var homeReselectKey by remember { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isBottomCollapsed by remember { mutableStateOf(false) }

    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                when {
                    consumed.y + available.y < -1f -> isBottomCollapsed = true
                    consumed.y + available.y > 1f -> isBottomCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

    val onlinePlaybackManager = OnlinePlaybackManagerHolder.get(context)
    val onlinePlaybackState by onlinePlaybackManager.playbackState.collectAsStateWithLifecycle()
    var isOnlinePlaybackActive by remember { mutableStateOf(false) }
    var activeOnlineSong by remember { mutableStateOf<OnlineSong?>(null) }
    var showOnlineDiagnostics by remember { mutableStateOf(false) }

    // Restore online playback if an online song was the last source played
    LaunchedEffect(Unit) {
        val currentState = onlinePlaybackManager.playbackState.value
        if (currentState.currentSong != null) {
            isOnlinePlaybackActive = true
            activeOnlineSong = currentState.currentSong
            playerViewModel.updateOnlinePlaybackState(
                song = currentState.currentSong!!,
                isPlaying = currentState.isPlaying,
                positionMs = currentState.currentPositionMs,
                durationMs = currentState.durationMs
            )
            playerViewModel.extractColor(currentState.currentSong!!.toSong())
            hasAttemptedRestore = true
        } else {
            val lastSource = LastPlayedStore.getLastSourceType(context)
            if (lastSource == LastPlayedStore.SOURCE_ONLINE) {
                val lastOnlineSong = LastPlayedStore.getLastOnlineSong(context)
                if (lastOnlineSong != null) {
                    val lastPos = LastPlayedStore.getLastPosition(context)
                    val lastQueue = LastPlayedStore.getLastOnlineQueue(context)

                    isOnlinePlaybackActive = true
                    activeOnlineSong = lastOnlineSong

                    onlinePlaybackManager.prepareSong(lastOnlineSong, lastPos, lastQueue)

                    playerViewModel.restoreOnlineState(
                        song = lastOnlineSong,
                        onPlayPause = { onlinePlaybackManager.togglePlayPause() },
                        onNext = { onlinePlaybackManager.playNext() },
                        onPrevious = { onlinePlaybackManager.playPrevious() },
                        onSeekTo = { onlinePlaybackManager.seekTo(it) }
                    )

                    playerViewModel.extractColor(lastOnlineSong.toSong())
                    hasAttemptedRestore = true
                }
            }
        }
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && !hasAttemptedRestore) {
            val lastSource = LastPlayedStore.getLastSourceType(context)
            if (lastSource != LastPlayedStore.SOURCE_ONLINE) {
                // Only set default queue and restore offline state if online wasn't the last source
                playerViewModel.setQueue(songs)
                playerViewModel.restoreLastState(songs)
                hasAttemptedRestore = true
            }
        }
    }

    DisposableEffect(onlinePlaybackManager) {
        val uiListener = object : OnlinePlaybackManager.PlaybackEventListener {
            override fun onSongChanged(song: OnlineSong) {
                activeOnlineSong = song
                playerViewModel.extractColor(song.toSong())
                if (isOnlinePlaybackActive) {
                    playerViewModel.updateOnlinePlaybackState(
                        song = song,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = song.durationSeconds * 1000L
                    )
                }
            }

            override fun onPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.updateOnlinePlaybackState(song, isPlaying, positionMs)
                }
            }

            override fun onProgressUpdate(song: OnlineSong, positionMs: Long, durationMs: Long) {
                if (isOnlinePlaybackActive) {
                    playerViewModel.onOnlineProgressUpdate(song, positionMs, durationMs)
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
        onlinePlaybackManager.addListener(uiListener)
        onDispose {
            // Detach the UI listener only. Releasing the shared player here stops audio
            // when the Activity/Compose content is recreated.
            onlinePlaybackManager.removeListener(uiListener)
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

    LaunchedEffect(onlinePlaybackState.currentSong, onlinePlaybackState.isPlaying, onlinePlaybackState.durationMs, isOnlinePlaybackActive) {
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var libraryQuery by rememberSaveable { mutableStateOf("") }
    var searchFilter by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { it.name },
            restore = { name -> runCatching { SearchFilter.valueOf(name) }.getOrDefault(SearchFilter.ALL) }
        )
    ) { mutableStateOf(SearchFilter.ALL) }
    
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var onlineSongToAddToPlaylist by remember { mutableStateOf<OnlineSong?>(null) }
    var songsToAddToPlaylist by remember { mutableStateOf<List<Song>?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    val favoriteSongs = remember(songs, favoriteIds, favoriteOnlineSongs, playbackHistory, playlists) {
        val localFavorites = songs.filter { it.id in favoriteIds }
        val onlineFavMap = mutableMapOf<Long, Song>()

        // 1. Explicitly favorited online songs
        favoriteOnlineSongs.forEach { os ->
            val s = os.toSong()
            if (s.id in favoriteIds) {
                onlineFavMap[s.id] = s
            }
        }

        // 2. Any favorite songs matching playback history entries
        playbackHistory.forEach { entry ->
            if (entry.songId in favoriteIds && !onlineFavMap.containsKey(entry.songId)) {
                val json = entry.onlineSongJson
                if (!json.isNullOrBlank()) {
                    runCatching {
                        Gson().fromJson(json, OnlineSong::class.java)?.toSong()
                    }.getOrNull()?.let { s ->
                        onlineFavMap[entry.songId] = s
                    }
                }
            }
        }

        // 3. Any favorite songs from custom playlists
        playlists.forEach { pl ->
            pl.onlineSongs.forEach { os ->
                val s = os.toSong()
                if (s.id in favoriteIds && !onlineFavMap.containsKey(s.id)) {
                    onlineFavMap[s.id] = s
                }
            }
        }

        (onlineFavMap.values + localFavorites).distinctBy { it.id }
    }

    fun toggleSongFavorite(song: Song) {
        val isFav = song.id in favoriteIds
        val onlineMatch = favoriteOnlineSongs.find { 
            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title.equals(song.title, ignoreCase = true) 
        } ?: playbackHistory.mapNotNull { entry ->
            entry.onlineSongJson?.let { runCatching { Gson().fromJson(it, OnlineSong::class.java) }.getOrNull() }
        }.find {
            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title.equals(song.title, ignoreCase = true)
        } ?: playlists.flatMap { it.onlineSongs }.find {
            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title.equals(song.title, ignoreCase = true)
        } ?: (onlinePlaybackState.currentSong ?: activeOnlineSong)?.takeIf {
            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title.equals(song.title, ignoreCase = true)
        } ?: if (song.album == "Online Stream" || song.filePath.isNullOrBlank() || !File(song.filePath).exists()) {
            OnlineSong(
                id = song.id.toString(),
                title = song.title,
                artist = song.artist,
                album = song.album,
                artworkUrl = song.artworkUri ?: "",
                durationSeconds = song.duration / 1000L,
                source = AudioSource.YOUTUBE,
                streamUrl = "",
                bitrate = "320 kbps",
                format = "AAC/MP4"
            )
        } else null

        musicViewModel.toggleFavorite(song.id, !isFav, onlineMatch)
    }

    fun playSong(song: Song, queue: List<Song>, playlistId: Long? = null) {
        LastPlayedStore.saveOffline(context, song, 0L)
        isOnlinePlaybackActive = false
        activeOnlineSong = null
        onlinePlaybackManager.pause()
        playerViewModel.stopOnlinePlayback()
        playerViewModel.setQueue(queue, playlistId)
        playerViewModel.play(song)
        showPlayer = true
    }

    fun playOnlineSong(onlineSong: OnlineSong, queue: List<OnlineSong>, playlistId: Long? = null) {
        LastPlayedStore.saveOnline(context, onlineSong, 0L, queue)
        musicViewModel.recordOnlinePlayback(onlineSong)
        isOnlinePlaybackActive = true
        activeOnlineSong = onlineSong
        playerViewModel.currentPlaylistId = playlistId
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
        playerViewModel.extractColor(onlineSong.toSong())
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(bottomBarScrollConnection)
        ) {
            val currentOnlineMiniSong = onlinePlaybackState.currentSong ?: activeOnlineSong
            val onlineAsSong = remember(currentOnlineMiniSong, onlinePlaybackState.durationMs) {
                currentOnlineMiniSong?.toSong()?.let { base ->
                    if (onlinePlaybackState.durationMs > 0) base.copy(duration = onlinePlaybackState.durationMs) else base
                }
            }
            val localMiniSong = playerViewModel.currentSong
            val hasMiniSong = (isOnlinePlaybackActive && onlineAsSong != null) || (!isOnlinePlaybackActive && localMiniSong != null)

            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Tab Screen destination: stays active on back-stack
                    when (selectedTab) {
                        AppTab.Home -> HomeScreen(
                            songs = songs,
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
                            onFavoriteToggle = { song -> toggleSongFavorite(song) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) },
                            onOnlineSongSelected = { onlineSong, queue ->
                                playOnlineSong(onlineSong, queue)
                            },
                            onAlbumSelected = { selectedAlbumName = it },
                            onArtistSelected = { selectedArtistName = it },
                            onPlaylistSelected = { selectedPlaylistId = it },
                            onOpenSettings = { showSettings = true },
                            savedOnlineArtists = savedOnlineArtists,
                            savedOnlineAlbums = savedOnlineAlbums,
                            onSaveOnlineArtist = { musicViewModel.saveOnlineArtistToLibrary(it) },
                            onRemoveSavedOnlineArtist = { musicViewModel.removeOnlineArtistFromLibrary(it) },
                            onSaveOnlineAlbum = { musicViewModel.saveOnlineAlbumToLibrary(it) },
                            onRemoveSavedOnlineAlbum = { musicViewModel.removeOnlineAlbumFromLibrary(it) },
                            appSettings = appSettings,
                            homeResetKey = homeReselectKey
                        )
                        AppTab.Online -> OnlineScreen(
                            onlinePlaybackManager = onlinePlaybackManager,
                            onOnlineSongSelected = { onlineSong, queue ->
                                playOnlineSong(onlineSong, queue)
                            },
                            onOpenSettings = { showSettings = true },
                            savedOnlinePlaylists = savedOnlinePlaylists,
                            onSaveOnlinePlaylist = { musicViewModel.saveOnlinePlaylistToLibrary(it) },
                            onRemoveSavedOnlinePlaylist = { musicViewModel.removeOnlinePlaylistFromLibrary(it) },
                            savedOnlineAlbums = savedOnlineAlbums,
                            onSaveOnlineAlbum = { musicViewModel.saveOnlineAlbumToLibrary(it) },
                            onRemoveSavedOnlineAlbum = { musicViewModel.removeOnlineAlbumFromLibrary(it) },
                            onAddOnlineSongToPlaylist = { onlineSongToAddToPlaylist = it },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) },
                            onPlayNextCollection = { playerViewModel.playNextOnline(it) },
                            onAddCollectionToQueue = { playerViewModel.addToQueueOnline(it) },
                            appSettings = appSettings
                        )
                        AppTab.Library -> LibraryScreen(
                            selectedTab = selectedLibraryTab,
                            onTabSelected = { selectedLibraryTab = it },
                            songs = songs,
                            allSongs = songs,
                            favoriteSongs = favoriteSongs,
                            favoriteIds = favoriteIds,
                            playlists = playlists,
                            query = libraryQuery,
                            currentSongId = playerViewModel.currentSong?.id,
                            playlistGridColumns = appSettings.playlistGridColumns,
                            appSettings = appSettings,
                            onQueryChange = { libraryQuery = it },
                            onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                            onFavoriteToggle = { song -> toggleSongFavorite(song) },
                            onSongSelected = { song, queue -> 
                                val onlineMatch = favoriteOnlineSongs.find { 
                                    (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title == song.title 
                                } ?: playbackHistory.mapNotNull { entry ->
                                    entry.onlineSongJson?.let { runCatching { Gson().fromJson(it, OnlineSong::class.java) }.getOrNull() }
                                }.find {
                                    (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title == song.title
                                } ?: playlists.flatMap { it.onlineSongs }.find {
                                    (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id || it.title == song.title
                                }

                                if (onlineMatch != null) {
                                    val onlineQueue = queue.map { qSong ->
                                        favoriteOnlineSongs.find { 
                                            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == qSong.id || it.title == qSong.title 
                                        } ?: playbackHistory.mapNotNull { entry ->
                                            entry.onlineSongJson?.let { runCatching { Gson().fromJson(it, OnlineSong::class.java) }.getOrNull() }
                                        }.find {
                                            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == qSong.id || it.title == qSong.title
                                        } ?: playlists.flatMap { it.onlineSongs }.find {
                                            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == qSong.id || it.title == qSong.title
                                        } ?: OnlineSong(
                                            id = qSong.id.toString(),
                                            title = qSong.title,
                                            artist = qSong.artist,
                                            album = qSong.album,
                                            artworkUrl = qSong.artworkUri ?: "",
                                            durationSeconds = qSong.duration / 1000L,
                                            source = AudioSource.YOUTUBE,
                                            streamUrl = "",
                                            bitrate = "320 kbps",
                                            format = "AAC/MP4"
                                        )
                                    }
                                    playOnlineSong(onlineMatch, onlineQueue)
                                } else {
                                    playSong(song, queue)
                                }
                            },
                            onOpenSettings = { showSettings = true },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onCreatePlaylist = { showNewPlaylistDialog = true },
                            onPlaylistSelected = { selectedPlaylistId = it.id },
                            onPlaylistExport = { playlist ->
                                pendingExportTarget = ExportTarget.SinglePlaylist(playlist)
                                exportFileLauncher.launch("${playlist.name}.aura")
                            },
                            onAlbumSelected = { selectedAlbumName = it },
                            onArtistSelected = { selectedArtistName = it },
                            onlinePlaybackManager = onlinePlaybackManager,
                            onOnlineSongSelected = { onlineSong, queue ->
                                playOnlineSong(onlineSong, queue)
                            },
                            savedOnlinePlaylists = savedOnlinePlaylists,
                            onSaveOnlinePlaylist = { musicViewModel.saveOnlinePlaylistToLibrary(it) },
                            onRemoveSavedOnlinePlaylist = { musicViewModel.removeOnlinePlaylistFromLibrary(it) },
                            savedOnlineArtists = savedOnlineArtists,
                            onSaveOnlineArtist = { musicViewModel.saveOnlineArtistToLibrary(it) },
                            onRemoveSavedOnlineArtist = { musicViewModel.removeOnlineArtistFromLibrary(it) },
                            savedOnlineAlbums = savedOnlineAlbums,
                            onSaveOnlineAlbum = { musicViewModel.saveOnlineAlbumToLibrary(it) },
                            onRemoveSavedOnlineAlbum = { musicViewModel.removeOnlineAlbumFromLibrary(it) },
                            onAddOnlineSongToPlaylist = { onlineSongToAddToPlaylist = it },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) },
                            onOnlinePlayNext = { playerViewModel.playNext(it) },
                            onOnlineAddToQueue = { playerViewModel.addToQueue(it) },
                            onPlayNextSongs = { playerViewModel.playNext(it) },
                            onAddSongsToQueue = { playerViewModel.addToQueue(it) },
                            onPlayNextOnlineSongs = { playerViewModel.playNextOnline(it) },
                            onAddOnlineSongsToQueue = { playerViewModel.addToQueueOnline(it) }
                        )
                        AppTab.Search -> SearchScreen(
                            songs = songs,
                            favoriteIds = favoriteIds,
                            currentSongId = playerViewModel.currentSong?.id,
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            activeFilter = searchFilter,
                            onActiveFilterChange = { searchFilter = it },
                            onSongSelected = { song, queue -> playSong(song, queue) },
                            onFavoriteToggle = { song -> toggleSongFavorite(song) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onPlayNext = { playerViewModel.playNext(it) },
                            onAddToQueue = { playerViewModel.addToQueue(it) },
                            onAlbumSelected = { selectedAlbumName = it },
                            onArtistSelected = { selectedArtistName = it },
                            onlinePlaybackManager = onlinePlaybackManager,
                            onOnlineSongSelected = { onlineSong, queue ->
                                playOnlineSong(onlineSong, queue)
                            },
                            onOnlinePlayNext = { playerViewModel.playNext(it) },
                            onOnlineAddToQueue = { playerViewModel.addToQueue(it) },
                            onAddOnlineSongToPlaylist = { onlineSongToAddToPlaylist = it },
                            appSettings = appSettings
                        )
                    }

                    // Detail Screens overlay
                    when {
                        showSettings -> {
                            AppThemedScreen(
                                area = AppArea.HOME,
                                artworkModel = if (isOnlinePlaybackActive) onlineAsSong else localMiniSong,
                                appSettings = appSettings
                            ) {
                                SettingsScreen(
                                    songCount = songs.size,
                                    albumCount = songs.distinctBy { it.album }.size,
                                    artistCount = songs.distinctBy { it.artist }.size,
                                    username = username,
                                    appSettings = appSettings,
                                    onUsernameChange = { musicViewModel.updateUsername(it) },
                                    onThemeModeChange = { mode -> musicViewModel.setThemeMode(mode) },
                                    onChangeThemeEnabled = { musicViewModel.setChangeThemeEnabled(it) },
                                    onThemeScopeChange = { musicViewModel.setThemeScope(it) },
                                    onThemeColorStyleChange = { musicViewModel.setThemeColorStyle(it) },
                                    onThemePresetChange = { musicViewModel.setThemePreset(it) },
                                    onThemeCustomHexesChange = { musicViewModel.setThemeCustomHexes(it) },
                                    onDynamicArtworkColorsChange = { musicViewModel.setDynamicArtworkColors(it) },
                                    onPlayerThemeModeChange = { musicViewModel.setPlayerThemeMode(it) },
                                    onColorPresetChange = { preset -> musicViewModel.setColorPreset(preset) },
                                    onCustomPaletteHexesChange = { musicViewModel.setCustomPaletteHexes(it) },
                                    onEnableFullAppDynamicThemeChange = { musicViewModel.setEnableFullAppDynamicTheme(it) },
                                    onAppPaletteModeChange = { musicViewModel.setAppPaletteMode(it) },
                                    onAppColorPresetChange = { musicViewModel.setAppColorPreset(it) },
                                    onAppCustomPaletteHexesChange = { musicViewModel.setAppCustomPaletteHexes(it) },
                                    onThemeAreaChange = { area, enabled -> musicViewModel.setThemeAreaEnabled(area, enabled) },
                                    onKaraokeChange = { musicViewModel.setKaraokeMode(it) },
                                    onLyricFontScaleChange = { musicViewModel.setLyricFontScale(it) },
                                    onLyricBlurStrengthChange = { musicViewModel.setLyricBlurStrength(it) },
                                    onSmartAudioFocusChange = { musicViewModel.setSmartAudioFocus(it) },
                                    onPlaylistGridColumnsChange = { musicViewModel.setPlaylistGridColumns(it) },
                                    onImportPlaylistFile = {
                                        importFileLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                    },
                                    onExportAllSongs = {
                                        pendingExportTarget = ExportTarget.AllSongs
                                        exportFileLauncher.launch("AuraMusic_AllSongs.json")
                                    },
                                    onExportPlaylist = { showExportPlaylistDialog = true },
                                    onRefresh = { musicViewModel.loadSongs(forceRefresh = true) },
                                    onBack = { showSettings = false },
                                    onShowAbout = { showAboutDialog = true }
                                )
                            }
                        }
                        selectedPlaylistId != null -> {
                            val playlist = playlists.find { it.id == selectedPlaylistId }
                            playlist?.let { p: Playlist ->
                                val localPlaylistSongs = p.songIds.mapNotNull { id: Long -> songs.find { it.id == id } }
                                val onlineAsLocalSongs = p.onlineSongs.map { it.toSong() }
                                val playlistSongs = localPlaylistSongs + onlineAsLocalSongs
                                CollectionDetailScreen(
                                    title = p.name,
                                    subtitle = "${playlistSongs.size} songs",
                                    songs = playlistSongs,
                                    favoriteIds = favoriteIds,
                                    currentSongId = playerViewModel.currentSong?.id,
                                    appArea = AppArea.PLAYLIST_DETAIL,
                                    appSettings = appSettings,
                                    onBack = { selectedPlaylistId = null },
                                    onSongSelected = { song ->
                                        val onlineMatch = com.aman.auramusic.util.SongIdentityMatcher.findOnlineMatch(song, p.onlineSongs)
                                        if (onlineMatch != null) {
                                            playOnlineSong(onlineMatch, p.onlineSongs, p.id)
                                        } else {
                                            playSong(song, localPlaylistSongs, p.id)
                                        }
                                    },
                                    onRemoveSong = { song -> 
                                        val onlineMatch = com.aman.auramusic.util.SongIdentityMatcher.findOnlineMatch(song, p.onlineSongs)
                                        if (onlineMatch != null) {
                                            musicViewModel.removeOnlineSongFromPlaylist(p.id, onlineMatch.id)
                                        } else {
                                            musicViewModel.removeFromPlaylist(p.id, song.id)
                                        }
                                    },
                                    onDeletePlaylist = { 
                                        musicViewModel.deletePlaylist(p.id)
                                        selectedPlaylistId = null
                                    },
                                    onRenamePlaylist = { playlistToRename = p },
                                    onToggleFavorite = { song -> toggleSongFavorite(song) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onPlayNext = { playerViewModel.playNext(it) },
                                    onAddToQueue = { playerViewModel.addToQueue(it) },
                                    onPlayNextCollection = { playerViewModel.playNext(playlistSongs) },
                                    onAddCollectionToQueue = { playerViewModel.addToQueue(playlistSongs) },
                                    onAddCollectionToPlaylist = { songsToAddToPlaylist = playlistSongs },
                                    onFavoriteCollection = {
                                        val items = playlistSongs.map { song ->
                                            val match = p.onlineSongs.find {
                                                (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id ||
                                                it.id == song.id.toString() ||
                                                it.title.equals(song.title, ignoreCase = true)
                                            } ?: favoriteOnlineSongs.find {
                                                (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id ||
                                                it.id == song.id.toString() ||
                                                it.title.equals(song.title, ignoreCase = true)
                                            }
                                            song.id to match
                                        }
                                        musicViewModel.toggleFavorites(items, true)
                                    }
                                )
                            }
                        }
                        selectedAlbumName != null -> {
                            val isChart = selectedAlbumName!!.startsWith("Top")
                            val localAlbumSongs = when (selectedAlbumName) {
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
                                else -> songs.filter { it.album.equals(selectedAlbumName, ignoreCase = true) }
                            }

                            var onlineAlbumSongs by remember(selectedAlbumName) { mutableStateOf<List<OnlineSong>>(emptyList()) }
                            var isAlbumLoading by remember(selectedAlbumName) { mutableStateOf(false) }

                            LaunchedEffect(selectedAlbumName) {
                                if (localAlbumSongs.isEmpty()) {
                                    isAlbumLoading = true
                                    try {
                                        val repo = OnlineMusicRepository()
                                        val rawResults = repo.search(selectedAlbumName!!, AudioSource.ALL)
                                        val filtered = com.aman.auramusic.util.SongIdentityMatcher.filterAlbumTracks(
                                            targetAlbumName = selectedAlbumName!!,
                                            targetArtistName = localAlbumSongs.firstOrNull()?.artist,
                                            rawTracks = rawResults
                                        )
                                        onlineAlbumSongs = if (filtered.isNotEmpty()) filtered else rawResults
                                    } catch (_: Exception) {
                                        onlineAlbumSongs = emptyList()
                                    } finally {
                                        isAlbumLoading = false
                                    }
                                }
                            }

                            val convertedOnlineSongs = remember(onlineAlbumSongs) {
                                onlineAlbumSongs.map { it.toSong() }
                            }
                            val displaySongs = if (localAlbumSongs.isNotEmpty()) localAlbumSongs else convertedOnlineSongs

                            CollectionDetailScreen(
                                title = selectedAlbumName!!,
                                subtitle = if (isChart) "Apple Music Chart • ${displaySongs.size} Songs" else "Album • ${displaySongs.firstOrNull()?.artist ?: "Various Artists"}",
                                songs = displaySongs,
                                favoriteIds = favoriteIds,
                                currentSongId = playerViewModel.currentSong?.id,
                                appArea = AppArea.ALBUM_DETAIL,
                                appSettings = appSettings,
                                onBack = { selectedAlbumName = null },
                                onSongSelected = { song ->
                                    val onlineMatch = com.aman.auramusic.util.SongIdentityMatcher.findOnlineMatch(song, onlineAlbumSongs)
                                    if (onlineMatch != null) {
                                        playOnlineSong(onlineMatch, onlineAlbumSongs)
                                    } else {
                                        playSong(song, displaySongs)
                                    }
                                },
                                onToggleFavorite = { song ->
                                    val isFav = song.id in favoriteIds
                                    val onlineMatch = com.aman.auramusic.util.SongIdentityMatcher.findOnlineMatch(song, onlineAlbumSongs)
                                        ?: com.aman.auramusic.util.SongIdentityMatcher.findOnlineMatch(song, favoriteOnlineSongs)
                                    musicViewModel.toggleFavorite(song.id, !isFav, onlineMatch)
                                },
                                onAddToPlaylist = { songToAddToPlaylist = it },
                                onPlayNext = { playerViewModel.playNext(it) },
                                onAddToQueue = { playerViewModel.addToQueue(it) },
                                onPlayNextCollection = {
                                    if (localAlbumSongs.isNotEmpty()) {
                                        playerViewModel.playNext(localAlbumSongs)
                                    } else {
                                        playerViewModel.playNext(displaySongs)
                                    }
                                },
                                onAddCollectionToQueue = {
                                    if (localAlbumSongs.isNotEmpty()) {
                                        playerViewModel.addToQueue(localAlbumSongs)
                                    } else {
                                        playerViewModel.addToQueue(displaySongs)
                                    }
                                },
                                onAddCollectionToPlaylist = { songsToAddToPlaylist = displaySongs },
                                onFavoriteCollection = {
                                    val items = displaySongs.map { song ->
                                        val match = onlineAlbumSongs.find {
                                            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id ||
                                            it.id == song.id.toString() ||
                                            it.title.equals(song.title, ignoreCase = true)
                                        } ?: favoriteOnlineSongs.find {
                                            (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == song.id ||
                                            it.id == song.id.toString() ||
                                            it.title.equals(song.title, ignoreCase = true)
                                        }
                                        song.id to match
                                    }
                                    musicViewModel.toggleFavorites(items, true)
                                },
                                showMenuButton = false,
                                showAddButton = false
                            )
                        }
                        selectedArtistName != null -> {
                            val artistName = selectedArtistName!!
                            val artistSongs = remember(artistName, songs) {
                                songs.filter { 
                                    com.aman.auramusic.util.ArtistHelper.songHasArtist(it, artistName) 
                                }
                            }
                            val savedArtistEntry = savedOnlineArtists.firstOrNull {
                                it.name.equals(artistName, ignoreCase = true)
                            }
                            val isArtistSaved = savedArtistEntry != null

                            var artistOnlineSongs by remember(artistName) { mutableStateOf<List<OnlineSong>>(emptyList()) }
                            var artistOnlineAlbums by remember(artistName) { mutableStateOf<List<OnlinePlaylist>>(emptyList()) }
                            var artistSimilarArtists by remember(artistName) { mutableStateOf<List<ArtistItem>>(emptyList()) }
                            var artistBioData by remember(artistName) { mutableStateOf<ArtistBioData?>(null) }
                            var isArtistLoading by remember(artistName) { mutableStateOf(false) }
                            var artistDetailErrorMessage by remember(artistName) { mutableStateOf<String?>(null) }

                            val onlineRepo = remember { OnlineMusicRepository() }
                            val ytArtistRepo = remember(onlineRepo) {
                                YouTubeArtistRepositoryImpl(
                                    pipedService = com.aman.auramusic.online.network.piped.PipedService(),
                                    onlineRepository = onlineRepo
                                )
                            }

                            LaunchedEffect(artistName) {
                                isArtistLoading = true
                                artistDetailErrorMessage = null
                                try {
                                    val catalogue = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val searchResults = try {
                                            ytArtistRepo.searchArtists(artistName)
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            emptyList()
                                        }
                                        val targetArtistKey = artistName.lowercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() }
                                        val found = searchResults.firstOrNull {
                                            it.name.lowercase(java.util.Locale.ROOT).filter { ch -> ch.isLetterOrDigit() } == targetArtistKey
                                        }
                                        val browseId = if (found != null && found.id.isNotBlank() && !found.id.startsWith("local_") && !found.id.startsWith("artist_") && !found.id.startsWith("yt_artist_")) {
                                            found.id
                                        } else {
                                            ""
                                        }
                                        try {
                                            ytArtistRepo.getArtistCatalogue(artistName, browseId)
                                        } catch (e: Exception) {
                                            if (e is kotlinx.coroutines.CancellationException) throw e
                                            com.aman.auramusic.online.network.repository.ArtistCatalogue(
                                                loadErrorMessage = "Unable to load this artist's catalogue. Check your connection and try again."
                                            )
                                        }
                                    }
                                    artistOnlineSongs = catalogue.songs
                                    artistOnlineAlbums = catalogue.albums
                                    artistBioData = catalogue.bio
                                    artistDetailErrorMessage = catalogue.loadErrorMessage
                                    artistSimilarArtists = catalogue.similarArtists.map {
                                        ArtistItem(
                                            name = it.name,
                                            songCountText = it.subscriberCountText.orEmpty(),
                                            artworkModel = it.profileImageUrl,
                                            isOnline = true,
                                            artistId = it.id
                                        )
                                    }
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    artistOnlineSongs = emptyList()
                                    artistOnlineAlbums = emptyList()
                                    artistSimilarArtists = emptyList()
                                    artistBioData = null
                                    artistDetailErrorMessage = "Unable to load this artist's catalogue. Check your connection and try again."
                                } finally {
                                    isArtistLoading = false
                                }
                            }

                            val portrait = remember(artistName, artistSongs, artistOnlineSongs) {
                                ArtistArtworkCache.get(artistName)
                                    ?: YouTubeArtistRepositoryImpl.getCachedPortrait(artistName)
                                    ?: artistOnlineSongs.firstOrNull()?.artworkUrl
                                    ?: artistSongs.firstOrNull()?.artworkUri
                            }
                            val audience = remember(artistName) {
                                YouTubeArtistRepositoryImpl.getCachedAudience(artistName)
                            }
                            ArtistDetailSheet(
                                artist = ArtistItem(
                                    name = artistName,
                                    songCountText = audience.orEmpty(),
                                    artworkModel = portrait,
                                    isOnline = artistSongs.isEmpty(),
                                    artistId = savedArtistEntry?.id.orEmpty()
                                ),
                                onlineSongs = artistOnlineSongs,
                                localSongs = artistSongs,
                                onlineAlbums = artistOnlineAlbums,
                                similarArtists = artistSimilarArtists,
                                bioData = artistBioData,
                                isLoading = isArtistLoading,
                                errorMessage = artistDetailErrorMessage,
                                isSavedInLibrary = isArtistSaved,
                                audienceText = audience,
                                appSettings = appSettings,
                                onToggleSaveToLibrary = {
                                    if (isArtistSaved) {
                                        savedArtistEntry?.let { musicViewModel.removeOnlineArtistFromLibrary(it.id) }
                                    } else {
                                        val cachedArtist = YouTubeArtistRepositoryImpl.getCachedArtist(artistName)
                                        val finalId = cachedArtist?.id?.takeIf { it.isNotBlank() }
                                            ?: savedArtistEntry?.id?.takeIf { it.isNotBlank() }
                                            ?: "artist_${artistName.trim().lowercase()}"
                                        musicViewModel.saveOnlineArtistToLibrary(
                                            SavedOnlineArtist(
                                                id = finalId,
                                                name = artistName,
                                                profileImageUrl = (cachedArtist?.profileImageUrl ?: portrait) as? String,
                                                source = AudioSource.YOUTUBE,
                                                audienceText = audience
                                            )
                                        )
                                    }
                                },
                                onDismiss = { selectedArtistName = null },
                                onPlaySong = { song, queue -> playSong(song, queue) },
                                onPlayOnlineSong = { song, queue -> playOnlineSong(song, queue) },
                                onPlayAll = {
                                    if (artistSongs.isNotEmpty()) {
                                        playSong(artistSongs.first(), artistSongs)
                                    } else if (artistOnlineSongs.isNotEmpty()) {
                                        playOnlineSong(artistOnlineSongs.first(), artistOnlineSongs)
                                    }
                                    selectedArtistName = null
                                },
                                onShuffleAll = {
                                    if (artistSongs.isNotEmpty()) {
                                        val shuffled = artistSongs.shuffled()
                                        playSong(shuffled.first(), shuffled)
                                    } else if (artistOnlineSongs.isNotEmpty()) {
                                        val shuffled = artistOnlineSongs.shuffled()
                                        playOnlineSong(shuffled.first(), shuffled)
                                    }
                                },
                                onSelectSimilarArtist = { simArtist ->
                                    selectedArtistName = simArtist.name
                                }
                            )
                        }
                    }
                }

                // Floating Navigation & Playback Pod
                if (!showPlayer) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(top = if (isBottomCollapsed) 2.dp else 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        AnimatedContent(
                            targetState = isBottomCollapsed && hasMiniSong,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) +
                                    slideInVertically(
                                        animationSpec = tween(260),
                                        initialOffsetY = { height -> height / 8 }
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(160)) +
                                        slideOutVertically(
                                            animationSpec = tween(180),
                                            targetOffsetY = { height -> height / 10 }
                                        ))
                            },
                            contentAlignment = Alignment.BottomCenter,
                            label = "bottomPlaybackNavigationTransition"
                        ) { compactLayout ->
                            if (compactLayout) {
                                BottomNavBar(
                                    selectedTab = selectedTab,
                                    compact = true,
                                    isPlaying = if (isOnlinePlaybackActive) onlinePlaybackState.isPlaying else playerViewModel.isPlaying,
                                    song = if (isOnlinePlaybackActive) onlineAsSong else localMiniSong,
                                    appSettings = appSettings,
                                    compactPlayer = {
                                        if (isOnlinePlaybackActive && onlineAsSong != null) {
                                            MiniPlayer(
                                                song = onlineAsSong,
                                                isPlaying = onlinePlaybackState.isPlaying,
                                                position = onlinePlaybackState.currentPositionMs,
                                                duration = onlinePlaybackState.durationMs,
                                                onPlayPause = { onlinePlaybackManager.togglePlayPause() },
                                                onNext = { onlinePlaybackManager.playNext() },
                                                onPrevious = { onlinePlaybackManager.playPrevious() },
                                                onOpen = { showPlayer = true },
                                                compact = true,
                                                appSettings = appSettings,
                                            )
                                        } else {
                                            localMiniSong?.let { song ->
                                                MiniPlayer(
                                                    song = song,
                                                    isPlaying = playerViewModel.isPlaying,
                                                    position = playerViewModel.currentPosition,
                                                    duration = playerViewModel.duration,
                                                    onPlayPause = { playerViewModel.togglePlayPause() },
                                                    onNext = { playerViewModel.playNext() },
                                                    onPrevious = { playerViewModel.playPrevious() },
                                                    onOpen = { showPlayer = true },
                                                    compact = true,
                                                    appSettings = appSettings,
                                                )
                                            }
                                        }
                                    },
                                    onTabSelected = {
                                        if (it == AppTab.Home && selectedTab == AppTab.Home) homeReselectKey++
                                        selectedTab = it
                                        showSettings = false
                                        selectedPlaylistId = null
                                        selectedAlbumName = null
                                        selectedArtistName = null
                                        if (it == AppTab.Home) isBottomCollapsed = false
                                    }
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isOnlinePlaybackActive && onlineAsSong != null) {
                                        MiniPlayer(
                                            song = onlineAsSong,
                                            isPlaying = onlinePlaybackState.isPlaying,
                                            position = onlinePlaybackState.currentPositionMs,
                                            duration = onlinePlaybackState.durationMs,
                                            onPlayPause = { onlinePlaybackManager.togglePlayPause() },
                                            onNext = { onlinePlaybackManager.playNext() },
                                            onPrevious = { onlinePlaybackManager.playPrevious() },
                                            onOpen = { showPlayer = true },
                                            appSettings = appSettings,
                                        )
                                    } else {
                                        localMiniSong?.let { song ->
                                            MiniPlayer(
                                                song = song,
                                                isPlaying = playerViewModel.isPlaying,
                                                position = playerViewModel.currentPosition,
                                                duration = playerViewModel.duration,
                                                onPlayPause = { playerViewModel.togglePlayPause() },
                                                onNext = { playerViewModel.playNext() },
                                                onPrevious = { playerViewModel.playPrevious() },
                                                onOpen = { showPlayer = true },
                                                appSettings = appSettings,
                                            )
                                        }
                                    }

                                    BottomNavBar(
                                        selectedTab = selectedTab,
                                        compact = false,
                                        isPlaying = if (isOnlinePlaybackActive) onlinePlaybackState.isPlaying else playerViewModel.isPlaying,
                                        song = if (isOnlinePlaybackActive) onlineAsSong else localMiniSong,
                                        appSettings = appSettings,
                                        onTabSelected = {
                                            if (it == AppTab.Home && selectedTab == AppTab.Home) homeReselectKey++
                                            selectedTab = it
                                            showSettings = false
                                            selectedPlaylistId = null
                                            selectedAlbumName = null
                                            selectedArtistName = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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
                    onQueueSave = null,
                    onHistoryClear = {},
                    onAddToPlaylist = { 
                        val cur = onlinePlaybackState.currentSong ?: activeOnlineSong
                        if (cur != null) {
                            onlineSongToAddToPlaylist = cur
                        }
                    },
                    appSettings = appSettings,
                    playerViewModel = playerViewModel,
                    isFavorite = onlineAsSong.id in favoriteIds,
                    onFavoriteToggle = { 
                        val isFav = onlineAsSong.id in favoriteIds
                        musicViewModel.toggleFavorite(onlineAsSong.id, !isFav, currentOnlineFullSong)
                    },
                    isShuffled = playerViewModel.isShuffled,
                    repeatMode = playerViewModel.repeatMode,
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onToggleRepeat = { playerViewModel.toggleRepeat() },
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
                        onQueueSave = {
                            songsToAddToPlaylist = queueState.map { it.song }
                            showNewPlaylistDialog = true
                        },
                        onHistoryClear = { musicViewModel.clearHistory() },
                        onAddToPlaylist = { songToAddToPlaylist = song },
                        onAddToQueue = { playerViewModel.addToQueue(it) },
                        onMoveQueueItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
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

        if (onlineSongToAddToPlaylist != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { onlineSongToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    val os = onlineSongToAddToPlaylist
                    if (os != null) {
                        musicViewModel.addOnlineSongToPlaylist(playlist.id, os)
                        Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                    }
                    onlineSongToAddToPlaylist = null
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
                    val onlineSongs = onlineSongToAddToPlaylist?.let { listOf(it) } ?: emptyList()
                    musicViewModel.savePlaylist(name, ids, onlineSongs = onlineSongs)
                    showNewPlaylistDialog = false
                    songToAddToPlaylist = null
                    onlineSongToAddToPlaylist = null
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
                    pendingExportTarget = ExportTarget.SinglePlaylist(playlist)
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
