package com.aman.auramusic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.ui.theme.LocalIsDark

private fun Int.ifZero(default: Int): Int = if (this == 0) default else this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlinePlaylistDetailScreen(
    playlist: OnlinePlaylist,
    songs: List<OnlineSong>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongSelected: (OnlineSong, List<OnlineSong>) -> Unit
) {
    val isDark = LocalIsDark.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF121214) else Color(0xFFF8F8FA)
                )
            )
        },
        containerColor = if (isDark) Color(0xFF121214) else Color(0xFFF8F8FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Hero Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF222226) else Color(0xFFE5E5EA))
                    ) {
                        AsyncImage(
                            model = playlist.artworkUrl,
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlist.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${songs.size.ifZero(playlist.songCount)} Songs",
                        fontSize = 14.sp,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons (Play All / Shuffle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    onSongSelected(songs.first(), songs)
                                }
                            },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    val shuffled = songs.shuffled()
                                    onSongSelected(shuffled.first(), shuffled)
                                }
                            },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Songs List or Loading State
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading tracks...",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs found in this playlist.",
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(songs, key = { "pl_song_${it.id}" }) { song ->
                    OnlinePlaylistSongRow(
                        song = song,
                        isDark = isDark,
                        onClick = { onSongSelected(song, songs) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnlinePlaylistSongRow(
    song: OnlineSong,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF222226) else Color(0xFFE5E5EA))
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                fontSize = 13.sp,
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
