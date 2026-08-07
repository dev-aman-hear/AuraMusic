package com.aman.auramusic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.LocalIsDark

@Composable
fun LibraryScreen(
    songs: List<Song>,
    allSongs: List<Song>,
    favoriteSongs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<Playlist>,
    query: String,
    currentSongId: Long?,
    playlistGridColumns: Int,
    onQueryChange: (String) -> Unit,
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
    val isDark = LocalIsDark.current

    var showAllPlaylists by remember { mutableStateOf(false) }

    if (showAllPlaylists) {
        AllPlaylistsScreen(
            playlists = playlists,
            allSongs = allSongs,
            columns = playlistGridColumns,
            onBack = { showAllPlaylists = false },
            onPlaylistSelected = { playlist ->
                showAllPlaylists = false
                onPlaylistSelected(playlist)
            },
            onCreatePlaylist = onCreatePlaylist
        )
        return
    }

    val featuredSong = remember(songs) { songs.firstOrNull() }
    val bestNewSongs = remember(songs) { songs.take(4) }
    val trendingSongs = remember(songs) { songs.drop(4).take(4).ifEmpty { songs.take(4) } }
    val albums = remember(songs) { songs.groupBy { it.album }.entries.toList() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // --- HEADER ---
        item {
            LibraryHeader(
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings
            )
        }

        // --- HERO FEATURED ALBUM BANNER (Musafir Cafe style) ---
        if (featuredSong != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "NEW ALBUM / UPCOMING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFA2D48),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${featuredSong.album} - EP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = featuredSong.artist,
                        fontSize = 13.sp,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSongSelected(featuredSong, songs) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SongArtwork(
                                song = featuredSong,
                                size = 340,
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // --- MY PLAYLISTS (CUSTOM PLAYLISTS) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showAllPlaylists = true }
                ) {
                    Text(
                        text = "My Playlists",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                val songById = remember(allSongs) { allSongs.associateBy { it.id } }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom Playlists
                    items(playlists, key = { "pl_${it.id}" }) { playlist ->
                        val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onPlaylistSelected(playlist) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .shadow(4.dp, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (firstSong != null) {
                                    SongArtwork(
                                        song = firstSong,
                                        size = 140,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playlist.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.songIds.size} songs",
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- BEST NEW SONGS > ---
        if (bestNewSongs.isNotEmpty()) {
            item {
                SectionHeaderTitle(title = "Best New Songs", onClick = {})
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    bestNewSongs.forEach { song ->
                        StackedSongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onPlay = { onSongSelected(song, songs) },
                            onAddToPlaylist = { onAddToPlaylist(song) }
                        )
                    }
                }
            }
        }

        // --- NEW THIS WEEK > ---
        if (songs.size >= 3) {
            item {
                SectionHeaderTitle(title = "New This Week", onClick = {})
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(songs.take(6), key = { "ntw_${it.id}" }) { song ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onSongSelected(song, songs) }
                        ) {
                            SongArtwork(
                                song = song,
                                size = 140,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- RECENT RELEASES > ---
        if (albums.isNotEmpty()) {
            item {
                SectionHeaderTitle(title = "Recent Releases", onClick = {})
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(albums.take(8), key = { "rr_${it.key}" }) { entry ->
                        val albumName = entry.key
                        val firstSong = entry.value.first()
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onAlbumSelected(albumName) }
                        ) {
                            SongArtwork(
                                song = firstSong,
                                size = 140,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = albumName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = firstSong.artist,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- TRENDING SONGS > ---
        if (trendingSongs.isNotEmpty()) {
            item {
                SectionHeaderTitle(title = "Trending Songs", onClick = {})
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    trendingSongs.forEach { song ->
                        StackedSongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onPlay = { onSongSelected(song, songs) },
                            onAddToPlaylist = { onAddToPlaylist(song) }
                        )
                    }
                }
            }
        }

        // --- DAILY TOP 100 & CITY CHARTS ---
        item {
            SectionHeaderTitle(title = "Daily Top 100", onClick = {})
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ChartCard(
                        title = "Top 100",
                        subtitle = "Global",
                        gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFFFB7185)),
                        onClick = { onAlbumSelected("Top 100: Global") }
                    )
                }
                item {
                    ChartCard(
                        title = "Top 100",
                        subtitle = "India",
                        gradientColors = listOf(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFACC15)),
                        onClick = { onAlbumSelected("Top 100: India") }
                    )
                }
                item {
                    ChartCard(
                        title = "Top 25",
                        subtitle = "Mumbai",
                        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF818CF8)),
                        onClick = { onAlbumSelected("Top 25: Mumbai") }
                    )
                }
                item {
                    ChartCard(
                        title = "Top 25",
                        subtitle = "Delhi",
                        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFEC4899)),
                        onClick = { onAlbumSelected("Top 25: Delhi") }
                    )
                }
            }
        }


    }
}

@Composable
fun SectionHeaderTitle(title: String, onClick: () -> Unit) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color.Black
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun StackedSongRow(
    song: Song,
    isCurrent: Boolean,
    isFavorite: Boolean = false,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(
            song = song,
            size = 50,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFA2D48) else if (isDark) Color.White else Color.Black,
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
                onClick = { showMenu = true },
                modifier = Modifier
                    .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.08f), CircleShape)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Now", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showMenu = false
                        onPlay()
                    }
                )
                if (onToggleFavorite != null) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFavorite) Color(0xFFFA2D48) else MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
                if (onAddToQueue != null) {
                    DropdownMenuItem(
                        text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onAddToQueue()
                        }
                    )
                }
            }
        }
    }
    Divider(
        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 64.dp)
    )
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(140.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(14.dp)
        ) {
            Text(
                text = "Music",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LibraryHeader(
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isDark = LocalIsDark.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 8.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color.Black,
                letterSpacing = (-0.5).sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(2.dp, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    val isDark = isSystemInDarkTheme()
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MadeForYouCard(songs: List<Song>, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFFFA2D48),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Made for You",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "Play your automatically generated mix",
                    fontSize = 12.sp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
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
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        playlists.forEach { playlist ->
            val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistSelected(playlist) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    song = firstSong,
                    size = 56,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibrary(query: String) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isEmpty()) "No songs in library" else "No results found for '$query'",
            fontSize = 15.sp,
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
        )
    }
}

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
                        imageVector = Icons.Default.ArrowBack,
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
                        imageVector = Icons.Default.QueueMusic,
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
                        Divider(
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
                                                imageVector = Icons.Default.QueueMusic,
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
