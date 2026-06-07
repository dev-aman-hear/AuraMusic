package com.aman.auramusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.MusicRepository
import com.aman.auramusic.ui.component.MiniPlayer
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.screen.PlayerScreen
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.util.formatDuration
import com.aman.auramusic.viewmodel.MusicViewModel
import com.aman.auramusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuraMusicTheme {
                MusicScreen()
            }
        }
    }
}

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val musicViewModel: MusicViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    
    val songs by musicViewModel.songs.collectAsStateWithLifecycle()
    val isSongsLoading by musicViewModel.isLoading.collectAsStateWithLifecycle()
    val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
    
    val permission = rememberAudioPermission()
    var hasPermission by remember {
        mutableStateOf(context.hasAudioPermission(permission))
    }
    var showPlayer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(LibraryTab.Songs) }
    var query by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            musicViewModel.loadSongs()
        }
    }

    LaunchedEffect(songs) {
        playerViewModel.setQueue(songs)
    }

    fun playSong(song: Song) {
        playerViewModel.play(song)
        showPlayer = true
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

    playerViewModel.currentSong?.takeIf { showPlayer }?.let { song ->
        val queue by playerViewModel.queue.collectAsStateWithLifecycle()
        PlayerScreen(
            song = song,
            isPlaying = playerViewModel.isPlaying,
            position = playerViewModel.currentPosition,
            duration = playerViewModel.duration,
            lyrics = lyrics,
            queue = queue,
            onBack = { showPlayer = false },
            onPlayPause = { playerViewModel.togglePlayPause() },
            onSeek = { playerViewModel.seekTo(it) },
            onPrevious = { playerViewModel.playPrevious() },
            onNext = { playerViewModel.playNext() },
            onSongSelected = { playerViewModel.play(it) }
        )
        return
    }

    Scaffold(
        bottomBar = {
            playerViewModel.currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = playerViewModel.isPlaying,
                    position = playerViewModel.currentPosition,
                    duration = playerViewModel.duration,
                    onOpen = { showPlayer = true },
                    onPlayPause = { playerViewModel.togglePlayPause() }
                )
            }
        }
    ) { padding ->
        if (!hasPermission) {
            PermissionScreen(
                modifier = Modifier.padding(padding),
                onGrant = { permissionLauncher.launch(permission) }
            )
        } else {
            LibraryScreen(
                songs = filteredSongs,
                allSongs = songs,
                selectedTab = selectedTab,
                query = query,
                currentSongId = playerViewModel.currentSongId,
                onQueryChange = { query = it },
                onTabSelected = { selectedTab = it },
                onRefresh = { musicViewModel.loadSongs() },
                onSongSelected = ::playSong,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    allSongs: List<Song>,
    selectedTab: LibraryTab,
    query: String,
    currentSongId: Long?,
    onQueryChange: (String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            LibraryHeader(
                songCount = allSongs.size,
                albumCount = allSongs.distinctBy { it.album }.size,
                artistCount = allSongs.distinctBy { it.artist }.size,
                query = query,
                onQueryChange = onQueryChange,
                onRefresh = onRefresh
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
                            onClick = { onSongSelected(song) }
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
                            onClick = { onSongSelected(album.value.first()) }
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
                            onClick = { onSongSelected(artist.value.first()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.aura_logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Aura Music",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = { showAbout = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = "Your offline library, ready from local storage.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                icon = Icons.Default.LibraryMusic,
                label = "Songs",
                value = songCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Album,
                label = "Albums",
                value = albumCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.MicExternalOn,
                label = "Artists",
                value = artistCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Scan local music")
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryTab.entries.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.title) }
            )
        }
    }
}

@Composable
private fun CollectionRow(
    title: String,
    subtitle: String,
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(song = song, size = 58)
            Spacer(modifier = Modifier.size(12.dp))
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
        }
    }
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
                text = "Let Aura Music read your audio library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The app only scans local audio files so it can play your offline music.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onGrant) {
                Text("Allow audio access")
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
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
                        .clip(RoundedCornerShape(6.dp))
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
                    "• Dynamic Artwork-based Colors",
                    "• Immersive Synced Lyrics",
                    "• Fluid Pager Navigation",
                    "• Advanced Audio Info & Quality Badges",
                    "• High-Performance Local Scan"
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
        shape = RoundedCornerShape(16.dp)
    )
}

private enum class LibraryTab(val title: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists")
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

private fun Context.hasAudioPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Int.floorMod(size: Int): Int {
    if (size == 0) return 0
    return ((this % size) + size) % size
}
