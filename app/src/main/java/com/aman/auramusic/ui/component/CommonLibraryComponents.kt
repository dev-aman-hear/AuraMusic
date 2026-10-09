package com.aman.auramusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song

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
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No songs found for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlaylistRail(
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAddToQueue()
                                }
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
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
