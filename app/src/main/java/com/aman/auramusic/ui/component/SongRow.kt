package com.aman.auramusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.util.formatDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = isPlaying,
    isFavorite: Boolean = false,
    trackNumber: Int? = null,
    artworkSize: Dp = 48.dp,
    showArtwork: Boolean = true,
    onPlayNow: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    var showMenu by remember { mutableStateOf(false) }

    val activeColor = MaterialTheme.colorScheme.primary
    val titleColor by animateColorAsState(
        targetValue = when {
            isActive -> activeColor
            isDark -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "titleColor"
    )
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.58f) else Color.Black.copy(alpha = 0.55f)

    val rowBackground = if (isActive) {
        activeColor.copy(alpha = if (isDark) 0.12f else 0.08f)
    } else {
        Color.Transparent
    }

    Surface(
        color = rowBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Track Number
            if (trackNumber != null) {
                Text(
                    text = trackNumber.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) activeColor else subtitleColor,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Track Artwork
            if (showArtwork) {
                SongArtwork(
                    song = song,
                    size = artworkSize.value.toInt(),
                    modifier = Modifier.size(artworkSize),
                    shape = RoundedCornerShape(10.dp),
                    elevation = if (isActive) 4.dp else 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Track Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.5.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = remember(song.artist, song.album) {
                    val cleanAlbum = song.album.trim()
                    if (cleanAlbum.isNotBlank() && cleanAlbum != "Online Stream" && cleanAlbum != song.title) {
                        "${song.artist} • $cleanAlbum"
                    } else {
                        song.artist
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Playing State Indicator or Duration
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Playing",
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (song.duration > 0) {
                Text(
                    text = formatDuration(song.duration),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor
                )
            }

            // Only show an options button when at least one action is available.
            if (onPlayNow != null || onAddToQueue != null || onAddToPlaylist != null ||
                onToggleFavorite != null || onRemove != null
            ) Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onPlayNow != null) {
                        DropdownMenuItem(
                            text = { Text("Play Now", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPlayNow()
                            }
                        )
                    }

                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onAddToQueue()
                            }
                        )
                    }

                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            }
                        )
                    }

                    if (onToggleFavorite != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            }
                        )
                    }

                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Remove", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Universal SongRow overload for online tracks. Automatically converts OnlineSong to Song.
 */
@Composable
fun SongRow(
    onlineSong: OnlineSong,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = isPlaying,
    isFavorite: Boolean = false,
    trackNumber: Int? = null,
    artworkSize: Dp = 48.dp,
    showArtwork: Boolean = true,
    onPlayNow: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val song = remember(onlineSong) { onlineSong.toSong() }
    SongRow(
        song = song,
        isPlaying = isPlaying,
        isActive = isActive,
        onClick = onClick,
        modifier = modifier,
        isFavorite = isFavorite,
        trackNumber = trackNumber,
        artworkSize = artworkSize,
        showArtwork = showArtwork,
        onPlayNow = onPlayNow,
        onAddToPlaylist = onAddToPlaylist,
        onToggleFavorite = onToggleFavorite,
        onRemove = onRemove,
        onAddToQueue = onAddToQueue,
        onLongClick = onLongClick
    )
}
