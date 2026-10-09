package com.aman.auramusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.theme.LocalIsDark

@Composable
fun AllPlaylistsScreen(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    columns: Int,
    onBack: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val isDark = LocalIsDark.current
    val songById = remember(allSongs) { allSongs.associateBy { it.id } }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Playlists",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            IconButton(
                onClick = onCreatePlaylist,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Playlist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No custom playlists created yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreatePlaylist,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            if (columns <= 1) {
                // 1 Column List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp)
                ) {
                    items(playlists, key = { "all_pl_${it.id}" }) { playlist ->
                        val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtwork(
                                song = firstSong,
                                size = 60,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${playlist.songIds.size} songs",
                                    fontSize = 13.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        HorizontalDivider(
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            } else {
                // 2 Column Grid View
                val chunkedPlaylists = remember(playlists) { playlists.chunked(2) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chunkedPlaylists) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            pair.forEach { playlist ->
                                val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onPlaylistSelected(playlist) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .shadow(6.dp, RoundedCornerShape(16.dp))
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (firstSong != null) {
                                            SongArtwork(
                                                song = firstSong,
                                                size = 200,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = playlist.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.songIds.size} songs",
                                        fontSize = 13.sp,
                                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
