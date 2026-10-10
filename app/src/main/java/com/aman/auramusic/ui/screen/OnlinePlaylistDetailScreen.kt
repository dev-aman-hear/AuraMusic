package com.aman.auramusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.AuraLoadingState
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.AuraShapes

private fun Int.ifZero(default: Int): Int = if (this == 0) default else this

@Composable
fun OnlinePlaylistDetailScreen(
    playlist: OnlinePlaylist,
    songs: List<OnlineSong>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongSelected: (OnlineSong, List<OnlineSong>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    AuraScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Text(
                    text = playlist.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                // Header Hero Banner
                item(key = "collection_hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuraArtwork(
                            model = playlist.artworkUrl,
                            size = 260,
                            modifier = Modifier.size(260.dp),
                            shape = AuraShapes.Card,
                            elevation = 16.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = playlist.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (playlist.subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = playlist.subtitle,
                                fontSize = 15.sp,
                                color = mutedText,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${songs.size.ifZero(playlist.songCount)} songs • AuraMusic",
                            fontSize = 13.sp,
                            color = mutedText.copy(alpha = 0.8f),
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
                                    .height(48.dp),
                                shape = AuraShapes.Surface,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                    .height(48.dp),
                                shape = AuraShapes.Surface
                            ) {
                                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                // Songs List or Loading State
                if (isLoading) {
                    item(key = "collection_loading") {
                        AuraLoadingState(
                            message = "Loading tracks...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        )
                    }
                } else if (songs.isEmpty()) {
                    item(key = "collection_empty") {
                        AuraEmptyState(
                            title = "No songs found",
                            message = "There are no tracks available in this collection.",
                            icon = Icons.Default.MusicNote,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = songs,
                        key = { index, song -> "${song.source.name}_${song.id.ifBlank { song.title }}_$index" }
                    ) { _, song ->
                        OnlinePlaylistSongRow(
                            song = song,
                            onClick = { onSongSelected(song, songs) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlinePlaylistSongRow(
    song: OnlineSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraArtwork(
            model = song.artworkUrl,
            size = 50,
            modifier = Modifier.size(50.dp),
            shape = AuraShapes.Artwork,
            elevation = 2.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitleText = if (song.album.isNotBlank()) {
                "${song.artist} • ${song.album}"
            } else {
                song.artist
            }
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
