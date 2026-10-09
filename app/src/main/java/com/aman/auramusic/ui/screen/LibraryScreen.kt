package com.aman.auramusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.data.CachedLibraryData
import com.aman.auramusic.online.data.LibraryOnlineCache
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.*
import com.aman.auramusic.ui.theme.*

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

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
    onlinePlaybackManager: OnlinePlaybackManager? = null,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val libraryCache = remember { LibraryOnlineCache(context) }
    val initialCached = remember { libraryCache.getCachedData() }

    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var showAllPlaylists by remember { mutableStateOf(false) }

    val onlineRepository = remember { OnlineMusicRepository() }
    var onlineSourceFilter by remember { mutableStateOf(AudioSource.ALL) }
    var onlinePlaylists by remember {
        mutableStateOf(initialCached?.playlistsBySource?.get(AudioSource.ALL.name) ?: emptyList())
    }
    var isOnlineFeedLoading by remember { mutableStateOf(false) }
    var lastUpdatedText by remember {
        mutableStateOf(libraryCache.getFormattedLastUpdated(initialCached?.lastFetchedTimestamp ?: 0L))
    }
    var forceRefreshTrigger by remember { mutableIntStateOf(0) }

    var selectedOnlinePlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var playlistDetailSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isPlaylistDetailLoading by remember { mutableStateOf(false) }

    // Fetch and cache curated feeds if expired
    LaunchedEffect(onlineSourceFilter, forceRefreshTrigger) {
        val cached = libraryCache.getCachedData()
        val cachedPlaylistsForFilter = cached?.playlistsBySource?.get(onlineSourceFilter.name)
        val isDailyExpired = libraryCache.shouldRefreshDaily(cached?.lastFetchedTimestamp ?: 0L)
        val isManual = forceRefreshTrigger > 0

        if (!isDailyExpired && !isManual && !cachedPlaylistsForFilter.isNullOrEmpty()) {
            onlinePlaylists = cachedPlaylistsForFilter
            lastUpdatedText = libraryCache.getFormattedLastUpdated(cached.lastFetchedTimestamp)
            isOnlineFeedLoading = false
        } else {
            isOnlineFeedLoading = true
            try {
                val fetchedPlaylists = onlineRepository.getCuratedPlaylists(onlineSourceFilter)
                onlinePlaylists = fetchedPlaylists

                val updatedMap = cached?.playlistsBySource?.toMutableMap() ?: mutableMapOf()
                updatedMap[onlineSourceFilter.name] = fetchedPlaylists

                val newCache = CachedLibraryData(
                    lastFetchedTimestamp = System.currentTimeMillis(),
                    playlistsBySource = updatedMap,
                    songFeed = cached?.songFeed ?: emptyList()
                )
                libraryCache.saveCachedData(newCache)
                lastUpdatedText = libraryCache.getFormattedLastUpdated(newCache.lastFetchedTimestamp)
            } catch (_: Exception) {
                if (!cachedPlaylistsForFilter.isNullOrEmpty()) {
                    onlinePlaylists = cachedPlaylistsForFilter
                }
            } finally {
                isOnlineFeedLoading = false
            }
        }
    }

    LaunchedEffect(selectedOnlinePlaylist) {
        val pl = selectedOnlinePlaylist
        if (pl != null) {
            isPlaylistDetailLoading = true
            try {
                playlistDetailSongs = onlineRepository.getPlaylistSongs(pl)
            } catch (_: Exception) {
                playlistDetailSongs = emptyList()
            } finally {
                isPlaylistDetailLoading = false
            }
        }
    }

    // Drill down to Online Playlist Details
    if (selectedOnlinePlaylist != null) {
        BackHandler {
            selectedOnlinePlaylist = null
        }
        OnlinePlaylistDetailScreen(
            playlist = selectedOnlinePlaylist!!,
            songs = playlistDetailSongs,
            isLoading = isPlaylistDetailLoading,
            onBack = { selectedOnlinePlaylist = null },
            onSongSelected = { song, queue ->
                onOnlineSongSelected(song, queue)
            }
        )
        return
    }

    // Drill down to All Playlists Screen
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

    val songById = remember(allSongs) { allSongs.associateBy { it.id } }

    // Filtered lists based on query
    val filteredSongs = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    val albumsList = remember(songs, query) {
        val grouped = songs.groupBy { it.album }
        if (query.isBlank()) {
            grouped.entries.toList()
        } else {
            grouped.entries.filter { (albumName, songs) ->
                albumName.contains(query, ignoreCase = true) ||
                songs.any { it.artist.contains(query, ignoreCase = true) }
            }.toList()
        }
    }

    val artistsList = remember(songs, query) {
        val grouped = songs.groupBy { it.artist }
        if (query.isBlank()) {
            grouped.entries.toList()
        } else {
            grouped.entries.filter { (artistName, _) ->
                artistName.contains(query, ignoreCase = true)
            }.toList()
        }
    }

    val filteredPlaylists = remember(playlists, query) {
        if (query.isBlank()) playlists else playlists.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) AuraDarkSurfaceElevated else Color(0xFFF1F5F9)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    AuraScreenBackground(modifier = modifier) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- EDITORIAL HEADER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = textColor,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = "${songs.size} tracks collected",
                        style = MaterialTheme.typography.labelMedium,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.55f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {
                        forceRefreshTrigger++
                        onRefresh()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = textColor.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Library Filter Search Box
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Filter ${selectedTab.label.lowercase()} in your collection...",
                        color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Filter",
                        tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textColor
                            )
                        }
                    }
                },
                shape = AuraShapes.Surface,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Category Tabs (Songs | Albums | Artists | Playlists)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val chipBg = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                    }
                    val chipText = if (isSelected) Color.White else textColor.copy(alpha = 0.85f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AuraShapes.Control)
                            .background(chipBg)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else cardBorder,
                                AuraShapes.Control
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = chipText
                        )
                    }
                }
            }
        }

        // --- CONTENT BY SELECTED TAB ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                LibraryTab.SONGS -> {
                    if (filteredSongs.isEmpty()) {
                        AuraEmptyState(
                            title = if (query.isNotBlank()) "No Matching Songs" else "Library is Empty",
                            message = if (query.isNotBlank()) "No songs matching \"$query\" found." else "Scan your device or download songs to begin listening.",
                            icon = Icons.Default.MusicNote,
                            actionLabel = if (query.isNotBlank()) "Clear Filter" else "Scan Device",
                            onAction = {
                                if (query.isNotBlank()) onQueryChange("") else onRefresh()
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            // Quick Action Header: Play All & Shuffle
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (filteredSongs.isNotEmpty()) {
                                                onSongSelected(filteredSongs.first(), filteredSongs)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play All (${filteredSongs.size})", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (filteredSongs.isNotEmpty()) {
                                                val shuffled = filteredSongs.shuffled()
                                                onSongSelected(shuffled.first(), shuffled)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = textColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle", color = textColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // High-performance track list using unified SongRow
                            items(filteredSongs, key = { it.id }) { song ->
                                val isCurrent = song.id == currentSongId
                                val isFav = favoriteIds.contains(song.id)

                                SongRow(
                                    song = song,
                                    isPlaying = isCurrent,
                                    isFavorite = isFav,
                                    onClick = { onSongSelected(song, filteredSongs) },
                                    onPlayNow = { onSongSelected(song, filteredSongs) },
                                    onToggleFavorite = { onFavoriteToggle(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) }
                                )
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    if (albumsList.isEmpty()) {
                        AuraEmptyState(
                            title = "No Albums Found",
                            message = if (query.isNotBlank()) "No albums matching \"$query\"." else "Your albums will appear here once audio files are indexed.",
                            icon = Icons.Default.Album,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albumsList, key = { it.key }) { (albumName, albumSongs) ->
                                val representativeSong = albumSongs.firstOrNull()
                                AlbumCard(
                                    title = albumName,
                                    subtitle = representativeSong?.artist ?: "Unknown Artist",
                                    artworkModel = representativeSong,
                                    size = 150.dp,
                                    onClick = { onAlbumSelected(albumName) }
                                )
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    if (artistsList.isEmpty()) {
                        AuraEmptyState(
                            title = "No Artists Found",
                            message = if (query.isNotBlank()) "No artists matching \"$query\"." else "Artists from your local collection will be listed here.",
                            icon = Icons.Default.Person,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(110.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(artistsList, key = { it.key }) { (artistName, artistSongs) ->
                                val representativeSong = artistSongs.firstOrNull()
                                ArtistCard(
                                    name = artistName,
                                    songCountText = "${artistSongs.size} songs",
                                    artworkModel = representativeSong,
                                    size = 110.dp,
                                    onClick = { onArtistSelected(artistName) }
                                )
                            }
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // User Playlists Header with Create button
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Playlists",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Button(
                                    onClick = onCreatePlaylist,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // User Playlists Horizontal Carousel
                        if (filteredPlaylists.isNotEmpty()) {
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(filteredPlaylists, key = { "pl_${it.id}" }) { playlist ->
                                        val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
                                        PlaylistCard(
                                            title = playlist.name,
                                            songCountText = "${playlist.songIds.size} songs",
                                            artworkModel = firstSong,
                                            onClick = { onPlaylistSelected(playlist) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        } else if (playlists.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBg)
                                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = textColor.copy(alpha = 0.4f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No custom playlists yet",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap 'New Playlist' to organize your favorite songs.",
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Curated Online Playlists Header
                        item {
                            SectionHeader(
                                title = "Curated Online Playlists",
                                eyebrow = "Stream & Discover",
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Source Filter Chips for online playlists: All, JioSaavn, YouTube, Spotify
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AudioSource.values()) { src ->
                                    val isSelected = onlineSourceFilter == src
                                    val chipBg = if (isSelected) {
                                        when (src) {
                                            AudioSource.JIOSAAVN -> AuraCyan
                                            AudioSource.YOUTUBE -> AuraPrimary
                                            AudioSource.SPOTIFY -> Color(0xFF1DB954)
                                            AudioSource.ALL -> MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                                    }
                                    val chipText = if (isSelected) Color.Black else textColor.copy(alpha = 0.8f)

                                    val chipLabel = when (src) {
                                        AudioSource.ALL -> "All Catalogs"
                                        AudioSource.JIOSAAVN -> "JioSaavn 320k"
                                        AudioSource.YOUTUBE -> "YouTube Music"
                                        AudioSource.SPOTIFY -> "Spotify Charts"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(chipBg)
                                            .clickable { onlineSourceFilter = src }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = chipLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = chipText
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Curated Playlists Grid/Row
                        if (onlinePlaylists.isNotEmpty()) {
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(onlinePlaylists, key = { "curated_${it.id}" }) { playlist ->
                                        PlaylistCard(
                                            title = playlist.title,
                                            songCountText = "${playlist.songCount} songs",
                                            artworkModel = playlist.artworkUrl,
                                            onClick = { selectedOnlinePlaylist = playlist }
                                        )
                                    }
                                }
                            }
                        } else if (isOnlineFeedLoading) {
                            item {
                                AuraLoadingState(
                                    message = "Fetching online playlists...",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
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
