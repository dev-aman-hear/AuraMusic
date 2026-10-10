package com.aman.auramusic.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.theme.AuraPrimary
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.liquidGlass

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
    onAddToPlaylist: (Song) -> Unit,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddCollectionToQueue: (() -> Unit)? = null,
    onAddCollectionToPlaylist: (() -> Unit)? = null,
    onFavoriteCollection: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val firstSong = songs.firstOrNull()
    val totalMinutes = songs.sumOf { it.duration / 1000 } / 60

    var showTopMenu by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Header (Back, Share, Overflow 3-dot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out $title on AuraMusic!")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }

                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Playlist Options",
                                    tint = if (isDark) Color.White else Color.Black
                                )
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false }
                            ) {
                                if (onAddCollectionToQueue != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add Playlist to Queue", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onAddCollectionToQueue()
                                        }
                                    )
                                }
                                if (onAddCollectionToPlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add Playlist to...", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onAddCollectionToPlaylist()
                                        }
                                    )
                                }
                                if (onFavoriteCollection != null) {
                                    DropdownMenuItem(
                                        text = { Text("Add All to Favorites", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFA2D48)) },
                                        onClick = {
                                            showTopMenu = false
                                            onFavoriteCollection()
                                        }
                                    )
                                }
                                if (onRenamePlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Playlist", fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            onRenamePlaylist()
                                        }
                                    )
                                }
                                if (onDeletePlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showTopMenu = false
                                            onDeletePlaylist()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Centered Poster Art (Apple Music Style)
                SongArtwork(
                    song = firstSong,
                    size = 250,
                    shape = RoundedCornerShape(22.dp),
                    elevation = 14.dp,
                    modifier = Modifier.shadow(14.dp, RoundedCornerShape(22.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle (Artists)
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.75f),
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
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Liquid Glass Action Controls (Shuffle circle, Pill Play, Add circle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            .clickable { songs.shuffled().firstOrNull()?.let { onSongSelected(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                level = GlassLevel.Regular,
                                isDark = isDark,
                                tint = AuraPrimary,
                                elevation = 10.dp,
                                borderWidth = 1.3.dp
                            )
                            .clickable { songs.firstOrNull()?.let { onSongSelected(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box {
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
                                .clickable { showPlusMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add options",
                                tint = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            if (onAddCollectionToPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showPlusMenu = false
                                        onAddCollectionToPlaylist()
                                    }
                                )
                            }
                            if (onAddCollectionToQueue != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        showPlusMenu = false
                                        onAddCollectionToQueue()
                                    }
                                )
                            }
                            if (onFavoriteCollection != null) {
                                DropdownMenuItem(
                                    text = { Text("Favorite All Tracks", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFA2D48)) },
                                    onClick = {
                                        showPlusMenu = false
                                        onFavoriteCollection()
                                    }
                                )
                            }
                            if (onDeletePlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showPlusMenu = false
                                        onDeletePlaylist()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Track Listing (Numbered 1..N)
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
                        text = "No songs in this collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                var itemMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelected(song) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        modifier = Modifier.width(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (song.id == currentSongId) Color(0xFFFA2D48) else if (isDark) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { itemMenuExpanded = true },
                            modifier = Modifier
                                .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.08f), CircleShape)
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = if (isDark) Color.White else Color.Black,
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
                                    onSongSelected(song)
                                }
                            )
                            val isFav = song.id in favoriteIds
                            DropdownMenuItem(
                                text = { Text(if (isFav) "Remove from Favorites" else "Add to Favorites", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFav) Color(0xFFFA2D48) else MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    itemMenuExpanded = false
                                    onToggleFavorite(song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    itemMenuExpanded = false
                                    onAddToPlaylist(song)
                                }
                            )
                            if (onAddToQueue != null) {
                                DropdownMenuItem(
                                    text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onAddToQueue(song)
                                    }
                                )
                            }
                            if (onRemoveSong != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove from Playlist", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onRemoveSong(song)
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 52.dp, end = 20.dp)
                )
            }
        }

        // Footer Album Metadata
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "July 21, 2026",
                    fontSize = 13.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                )
                Text(
                    text = "${songs.size} songs, ${totalMinutes} minutes",
                    fontSize = 13.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                )
                Text(
                    text = "℗ 2026 AuraMusic LLC Under Exclusive License",
                    fontSize = 12.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RECORD LABEL AuraMusic",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Related Carousels
        if (songs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More by ${firstSong?.artist ?: "Artist"}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(songs.take(6), key = { "more_${it.id}" }) { s ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { onSongSelected(s) }
                            ) {
                                SongArtwork(
                                    song = s,
                                    size = 130,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = s.album,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = s.artist,
                                    fontSize = 11.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
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
