package com.aman.auramusic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    songs: List<Song>,
    username: String,
    history: List<PlaybackHistoryEntry>,
    favorites: List<Song>,
    favoriteIds: Set<Long>,
    dominantColor: Color,
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

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Added padding for MiniPlayer
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
                SectionHeader(title = "Listen Again")
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
    }
}

@Composable
fun SongOptionsDialog(
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
fun SectionHeader(title: String, icon: ImageVector? = null) {
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
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
