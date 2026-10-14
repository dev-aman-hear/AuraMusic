package com.aman.auramusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.aman.auramusic.ui.theme.LocalIsDark
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.aman.auramusic.ui.theme.AuraPrimary
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.liquidGlass

@Composable
fun OnlinePlaylistDetailScreen(
    playlist: OnlinePlaylist,
    songs: List<OnlineSong>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongSelected: (OnlineSong, List<OnlineSong>) -> Unit,
    isSavedInLibrary: Boolean = false,
    onToggleSaveToLibrary: (() -> Unit)? = null,
    onAddOnlineSongToPlaylist: ((OnlineSong) -> Unit)? = null,
    showMenuButton: Boolean = false,
    showAddButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val isDark = LocalIsDark.current
    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val totalSecs = songs.sumOf { it.durationSeconds }
    val totalMinutes = if (totalSecs > 0) (totalSecs / 60) else (songs.size * 3L)

    var showTopMenu by remember { mutableStateOf(false) }

    AuraScreenBackground(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item(key = "online_collection_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Action Header (Back, Overflow 3-dot)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (showMenuButton) {
                            Box {
                                IconButton(onClick = { showTopMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    if (onToggleSaveToLibrary != null) {
                                        DropdownMenuItem(
                                            text = { Text(if (isSavedInLibrary) "Remove from Library" else "Save to Library", fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = {
                                                Icon(
                                                    if (isSavedInLibrary) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                                    contentDescription = null,
                                                    tint = if (isSavedInLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                showTopMenu = false
                                                onToggleSaveToLibrary()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Play All", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showTopMenu = false
                                            if (songs.isNotEmpty()) onSongSelected(songs.first(), songs)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Shuffle", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            if (songs.isNotEmpty()) {
                                                val shuffled = songs.shuffled()
                                                onSongSelected(shuffled.first(), shuffled)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Centered Poster Art (Apple Music Style)
                    val artworkModel = playlist.artworkUrl.ifBlank {
                        songs.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
                    }
                    AuraArtwork(
                        model = artworkModel,
                        size = 250,
                        shape = RoundedCornerShape(22.dp),
                        elevation = 14.dp,
                        modifier = Modifier
                            .size(250.dp)
                            .shadow(14.dp, RoundedCornerShape(22.dp))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = playlist.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle (Album / Artists)
                    val subtitleText = playlist.subtitle.ifBlank {
                        val artists = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().take(3).joinToString(", ")
                        if (artists.isNotBlank()) "Album • $artists" else "Album • AuraMusic"
                    }
                    Text(
                        text = subtitleText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Audio Format Pill
                    Text(
                        text = "Aura Music • Lossless Audio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Liquid Glass Action Controls (Shuffle, Play, and optional Add button)
                    val isAlbum = playlist.id.startsWith("ytmusic:MPRE") ||
                        playlist.id.startsWith("ytmusic_album_MPRE") ||
                        playlist.id.startsWith("album_") ||
                        playlist.subtitle.startsWith("Album", ignoreCase = true)
                    val shouldShowPlus = showAddButton

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (shouldShowPlus) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        level = GlassLevel.UltraThin,
                                        isDark = isDark,
                                        elevation = 6.dp,
                                        borderWidth = 1.1.dp
                                    )
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            val shuffled = songs.shuffled()
                                            onSongSelected(shuffled.first(), shuffled)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(dynamicPrimary)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            onSongSelected(songs.first(), songs)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        level = GlassLevel.UltraThin,
                                        isDark = isDark,
                                        elevation = 6.dp,
                                        borderWidth = 1.1.dp
                                    )
                                    .clickable {
                                        onToggleSaveToLibrary?.invoke()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSavedInLibrary) Icons.Default.BookmarkAdded else Icons.Default.Add,
                                    contentDescription = if (isSavedInLibrary) "Saved to Library" else "Save to Library",
                                    tint = if (isSavedInLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            // Symmetrical dual-action pills for albums
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(dynamicPrimary)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            onSongSelected(songs.first(), songs)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(24.dp),
                                        level = GlassLevel.UltraThin,
                                        isDark = isDark,
                                        elevation = 6.dp,
                                        borderWidth = 1.1.dp
                                    )
                                    .clickable {
                                        if (songs.isNotEmpty()) {
                                            val shuffled = songs.shuffled()
                                            onSongSelected(shuffled.first(), shuffled)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Shuffle",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Songs List or Loading / Empty State
            if (isLoading) {
                item(key = "online_collection_loading") {
                    AuraLoadingState(
                        message = "Loading tracks...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    )
                }
            } else if (songs.isEmpty()) {
                item(key = "online_collection_empty") {
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
                // Numbered Track Listing (1..N) matching offline album
                itemsIndexed(
                    items = songs,
                    key = { index, song -> "${song.source.name}_${song.id.ifBlank { song.title }}_$index" }
                ) { index, song ->
                    var itemMenuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelected(song, songs) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val artistText = if (song.artist.isNotBlank()) song.artist else "Unknown Artist"
                            Text(
                                text = artistText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { itemMenuExpanded = true },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = itemMenuExpanded,
                                onDismissRequest = { itemMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play Now", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onSongSelected(song, songs)
                                    }
                                )
                                if (onAddOnlineSongToPlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            itemMenuExpanded = false
                                            onAddOnlineSongToPlaylist(song)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp, end = 20.dp)
                    )
                }
            }

            // Footer Album Metadata
            item(key = "online_collection_footer") {
                val yearText = songs.firstOrNull { it.year.isNotBlank() }?.year ?: "2026"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = yearText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${songs.size} songs, $totalMinutes minutes",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "℗ $yearText AuraMusic LLC Under Exclusive License",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RECORD LABEL AuraMusic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Related Carousel ("More by [Artist] >")
            if (songs.isNotEmpty()) {
                val firstArtist = songs.firstOrNull()?.artist?.split(",", "&", "•")?.firstOrNull()?.trim() ?: "Artist"
                item(key = "online_collection_more_by") {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More by $firstArtist",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(songs.take(6), key = { "more_online_${it.id}_${it.title}" }) { s ->
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { onSongSelected(s, songs) }
                                ) {
                                    AuraArtwork(
                                        model = s.artworkUrl.ifBlank { playlist.artworkUrl },
                                        size = 130,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(130.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = s.album.ifBlank { s.title },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.artist,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
