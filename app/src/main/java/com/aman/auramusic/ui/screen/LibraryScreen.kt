package com.aman.auramusic.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.viewmodel.MusicViewModel

enum class LibraryTab {
    Songs, Albums, Artists, Playlists, Favorites
}

@Composable
fun LibraryScreen(
    songs: List<Song>,
    allSongs: List<Song>,
    favoriteSongs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<Playlist>,
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
    onCreatePlaylist: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onPlaylistExport: (Playlist) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
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
                            isFavorite = song.id in favoriteIds,
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
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onCreatePlaylist,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "New Playlist", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
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
                            isFavorite = true,
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
fun LibraryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) } }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun LibraryTabs(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) },
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = tab.name,
                    modifier = Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CollectionRow(
    title: String,
    subtitle: String,
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(song = song, size = 64, shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CollectionDetailScreen(
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
    onAddToPlaylist: (Song) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                    Row {
                        if (onRenamePlaylist != null) {
                            IconButton(onClick = onRenamePlaylist) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                        }
                        if (onDeletePlaylist != null) {
                            IconButton(onClick = onDeletePlaylist) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SongArtwork(
                    song = songs.firstOrNull(),
                    size = 260,
                    shape = RoundedCornerShape(32.dp),
                    elevation = 8.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { songs.shuffled().firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE1E2EC),
                            contentColor = Color(0xFF1B1B1F)
                        )
                    ) {
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { songs.firstOrNull()?.let { onSongSelected(it) } },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF566894),
                            contentColor = Color.White
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
                        text = "No local songs matched yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    isFavorite = song.id in favoriteIds,
                    onPlayNow = { onSongSelected(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onAddToPlaylist = if (onRemoveSong == null) { { onAddToPlaylist(song) } } else null,
                    onRemove = onRemoveSong?.let { removeFunc -> { removeFunc(song) } },
                    onClick = { onSongSelected(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistRail(
    playlists: List<Playlist>,
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
                            onClick = { onPlaylistSelected(playlist) }
                        )
                    }
                }
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
fun PlaylistPreviewCard(
    playlistName: String,
    songCount: Int,
    previewSong: Song?,
    onClick: () -> Unit
) {
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
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = playlistName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = "$songCount songs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MadeForYouCard(
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
fun EmptyLibrary(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No songs found for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddToPlaylistDialog(
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
fun ExportPlaylistDialog(
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
fun ImportResultDialog(
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
fun NewPlaylistDialog(
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
fun RenamePlaylistDialog(
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
