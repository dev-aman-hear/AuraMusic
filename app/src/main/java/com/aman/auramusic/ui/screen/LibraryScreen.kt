package com.aman.auramusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.data.CachedLibraryData
import com.aman.auramusic.online.data.LibraryOnlineCache
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.SavedOnlineArtist
import com.aman.auramusic.online.filter.ExplicitTitleFilter
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.network.repository.ArtistBioData
import com.aman.auramusic.online.network.repository.YouTubeArtistRepositoryImpl
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.*
import com.aman.auramusic.ui.screen.home.ArtistDetailSheet
import com.aman.auramusic.ui.screen.home.ArtistItem
import com.aman.auramusic.util.ArtistArtworkCache
import com.aman.auramusic.util.ArtistHelper
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.ui.theme.*

private fun formatArtistAudience(raw: String?): String {
    if (raw.isNullOrBlank()) return "Artist"
    val clean = raw.trim()
    val stripped = if (clean.startsWith("Artist •", ignoreCase = true)) {
        clean.substringAfter("Artist •").trim()
    } else if (clean.startsWith("Artist -", ignoreCase = true)) {
        clean.substringAfter("Artist -").trim()
    } else if (clean.startsWith("Artist", ignoreCase = true) && clean.length > 6) {
        clean.substring(6).trim(' ', '•', '-', '·')
    } else {
        clean
    }
    return if (stripped.isNotBlank() &&
        !stripped.equals("YouTube Music", ignoreCase = true) &&
        !stripped.equals("YouTube Music Artist", ignoreCase = true) &&
        !stripped.contains("Saved", ignoreCase = true)
    ) {
        stripped
    } else {
        "Artist"
    }
}

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    FAVORITES("Favorites"),
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
    savedOnlinePlaylists: List<OnlinePlaylist> = emptyList(),
    onSaveOnlinePlaylist: ((OnlinePlaylist) -> Unit)? = null,
    onRemoveSavedOnlinePlaylist: ((String) -> Unit)? = null,
    savedOnlineArtists: List<SavedOnlineArtist> = emptyList(),
    onSaveOnlineArtist: ((SavedOnlineArtist) -> Unit)? = null,
    onRemoveSavedOnlineArtist: ((String) -> Unit)? = null,
    savedOnlineAlbums: List<OnlinePlaylist> = emptyList(),
    onSaveOnlineAlbum: ((OnlinePlaylist) -> Unit)? = null,
    onRemoveSavedOnlineAlbum: ((String) -> Unit)? = null,
    onAddOnlineSongToPlaylist: ((OnlineSong) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onOnlinePlayNext: ((OnlineSong) -> Unit)? = null,
    onOnlineAddToQueue: ((OnlineSong) -> Unit)? = null,
    onPlayNextSongs: ((List<Song>) -> Unit)? = null,
    onAddSongsToQueue: ((List<Song>) -> Unit)? = null,
    onPlayNextOnlineSongs: ((List<OnlineSong>) -> Unit)? = null,
    onAddOnlineSongsToQueue: ((List<OnlineSong>) -> Unit)? = null,
    selectedTab: LibraryTab = LibraryTab.SONGS,
    onTabSelected: (LibraryTab) -> Unit = {},
    appSettings: AppSettings = AppSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    var selectedLocalAlbum by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    var showAllPlaylists by remember { mutableStateOf(false) }

    val onlineRepository = remember { OnlineMusicRepository() }
    val youtubeArtistRepository = remember(onlineRepository) {
        YouTubeArtistRepositoryImpl(
            pipedService = com.aman.auramusic.online.network.piped.PipedService(),
            onlineRepository = onlineRepository
        )
    }
    var selectedOnlinePlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var playlistDetailSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isPlaylistDetailLoading by remember { mutableStateOf(false) }

    var selectedArtist by remember { mutableStateOf<ArtistItem?>(null) }
    var onlineArtistAudiences by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var onlineArtistPortraits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var artistDetailSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var artistDetailAlbums by remember { mutableStateOf<List<OnlinePlaylist>>(emptyList()) }
    var artistDetailSimilarArtists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var artistDetailBioData by remember { mutableStateOf<ArtistBioData?>(null) }
    var artistDetailErrorMessage by remember { mutableStateOf<String?>(null) }
    var isArtistDetailLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedOnlinePlaylist) {
        val pl = selectedOnlinePlaylist
        if (pl != null) {
            if (pl.songs.isNotEmpty()) {
                playlistDetailSongs = pl.songs
                isPlaylistDetailLoading = false
            } else {
                isPlaylistDetailLoading = true
            }
            try {
                val fetched = onlineRepository.getPlaylistSongs(pl)
                if (fetched.isNotEmpty()) {
                    playlistDetailSongs = fetched
                }
            } catch (_: Exception) {
                if (playlistDetailSongs.isEmpty()) {
                    playlistDetailSongs = emptyList()
                }
            } finally {
                isPlaylistDetailLoading = false
            }
        }
    }

    LaunchedEffect(selectedArtist) {
        val artist = selectedArtist
        if (artist != null) {
            isArtistDetailLoading = true
            artistDetailErrorMessage = null
            try {
                var browseId = artist.artistId
                if (browseId.isBlank() || browseId.startsWith("local_") || browseId.startsWith("artist_")) {
                    val searchResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            youtubeArtistRepository.searchArtists(artist.name)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    val targetArtistKey = artist.name.lowercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() }
                    val found = searchResults.firstOrNull {
                        it.name.lowercase(java.util.Locale.ROOT).filter { ch -> ch.isLetterOrDigit() } == targetArtistKey
                    }
                    if (found != null && found.id.isNotBlank() && !found.id.startsWith("local_")) {
                        browseId = found.id
                        if (!found.subscriberCountText.isNullOrBlank()) {
                            onlineArtistAudiences = onlineArtistAudiences + (found.id to found.subscriberCountText) + (artist.name to found.subscriberCountText)
                        }
                        if (!found.profileImageUrl.isNullOrBlank()) {
                            onlineArtistPortraits = onlineArtistPortraits + (found.id to found.profileImageUrl) + (artist.name to found.profileImageUrl)
                        }
                    }
                }
                val catalogue = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val idToUse = if (browseId.isNotBlank() && !browseId.startsWith("local_") && !browseId.startsWith("artist_")) browseId else ""
                        youtubeArtistRepository.getArtistCatalogue(artist.name, idToUse)
                    } catch (_: Exception) {
                        com.aman.auramusic.online.network.repository.ArtistCatalogue(
                            loadErrorMessage = "Unable to load this artist's catalogue. Check your connection and try again."
                        )
                    }
                }
                
                val songs = catalogue.songs
                val albums = catalogue.albums
                artistDetailErrorMessage = catalogue.loadErrorMessage
                val similarItems = catalogue.similarArtists.map {
                    ArtistItem(
                        name = it.name,
                        songCountText = it.subscriberCountText.orEmpty(),
                        artworkModel = it.profileImageUrl,
                        isOnline = true,
                        artistId = it.id
                    )
                }
                artistDetailSongs = songs
                artistDetailAlbums = albums
                artistDetailSimilarArtists = similarItems
                artistDetailBioData = catalogue.bio
                if (!catalogue.subscriberCountText.isNullOrBlank()) {
                    onlineArtistAudiences = onlineArtistAudiences + (artist.name to catalogue.subscriberCountText)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                artistDetailSongs = emptyList()
                artistDetailAlbums = emptyList()
                artistDetailSimilarArtists = emptyList()
                artistDetailBioData = null
                artistDetailErrorMessage = "Unable to load this artist's catalogue. Check your connection and try again."
            } finally {
                isArtistDetailLoading = false
            }
        }
    }

    // Drill down to Local Album Details
    if (selectedLocalAlbum != null) {
        BackHandler {
            selectedLocalAlbum = null
        }
        val (albumName, albumSongs) = selectedLocalAlbum!!
        val isChart = albumName.startsWith("Apple Music") || albumName.startsWith("Spotify")
        CollectionDetailScreen(
            title = albumName,
            subtitle = if (isChart) "Apple Music Chart • ${albumSongs.size} Songs" else "Album • ${albumSongs.firstOrNull()?.artist ?: "Unknown"}",
            songs = albumSongs,
            favoriteIds = favoriteIds,
            currentSongId = currentSongId,
            onBack = { selectedLocalAlbum = null },
            onSongSelected = { onSongSelected(it, albumSongs) },
            onToggleFavorite = onFavoriteToggle,
            onAddToPlaylist = onAddToPlaylist,
            onPlayNext = onPlayNext?.let { cb -> { cb(it) } },
            onAddToQueue = onAddToQueue?.let { cb -> { cb(it) } },
            onPlayNextCollection = onPlayNextSongs?.let { cb -> { cb(albumSongs) } },
            onAddCollectionToQueue = onAddSongsToQueue?.let { cb -> { cb(albumSongs) } },
            showAddButton = false,
            appSettings = appSettings
        )
        return
    }

    // Drill down to Online Playlist Details
    if (selectedOnlinePlaylist != null) {
        BackHandler {
            selectedOnlinePlaylist = null
        }
        val isSavedAlbum = savedOnlineAlbums.any { it.id == selectedOnlinePlaylist!!.id }
        val isSavedPlaylist = savedOnlinePlaylists.any { it.id == selectedOnlinePlaylist!!.id }
        val isSaved = isSavedAlbum || isSavedPlaylist
        OnlinePlaylistDetailScreen(
            playlist = selectedOnlinePlaylist!!,
            songs = playlistDetailSongs,
            isLoading = isPlaylistDetailLoading,
            isSavedInLibrary = isSaved,
            showAddButton = true,
            appSettings = appSettings,
            onToggleSaveToLibrary = {
                val cur = selectedOnlinePlaylist ?: return@OnlinePlaylistDetailScreen
                if (isSaved) {
                    if (isSavedAlbum) {
                        onRemoveSavedOnlineAlbum?.invoke(cur.id)
                    }
                    if (isSavedPlaylist) {
                        onRemoveSavedOnlinePlaylist?.invoke(cur.id)
                    }
                } else {
                    val toSave = if (playlistDetailSongs.isNotEmpty()) {
                        cur.copy(songs = playlistDetailSongs)
                    } else cur
                    val isAlbum = cur.id.startsWith("ytmusic:MPRE") ||
                        cur.id.startsWith("ytmusic_album_MPRE") ||
                        cur.id.startsWith("album_")
                    if (isAlbum) {
                        onSaveOnlineAlbum?.invoke(toSave)
                    } else {
                        onSaveOnlinePlaylist?.invoke(toSave)
                    }
                }
            },
            onAddOnlineSongToPlaylist = onAddOnlineSongToPlaylist,
            onPlayNext = onOnlinePlayNext,
            onAddToQueue = onOnlineAddToQueue,
            onPlayNextCollection = onPlayNextOnlineSongs,
            onAddCollectionToQueue = onAddOnlineSongsToQueue,
            onBack = { selectedOnlinePlaylist = null },
            onSongSelected = { song, queue ->
                onOnlineSongSelected(song, queue)
            },
            showMenuButton = !isSavedAlbum
        )
        return
    }

    // Drill down to Artist Details
    if (selectedArtist != null) {
        val curArtist = selectedArtist!!
        val savedArtistEntry = savedOnlineArtists.firstOrNull {
            (curArtist.artistId.isNotBlank() && it.id == curArtist.artistId) ||
                it.name.equals(curArtist.name, ignoreCase = true)
        }
        val isSaved = savedArtistEntry != null
        val audience = formatArtistAudience(
            curArtist.songCountText.takeIf { it.isNotBlank() && !it.contains("songs", ignoreCase = true) }
                ?: onlineArtistAudiences[curArtist.artistId]
                ?: onlineArtistAudiences[curArtist.name]
                ?: YouTubeArtistRepositoryImpl.getCachedAudience(curArtist.name)
        )
        val localMatchingSongs = remember(curArtist.name, allSongs) {
            allSongs.filter { it.artist.contains(curArtist.name, ignoreCase = true) }
        }
        val resolvedArtwork = curArtist.artworkModel
            ?: onlineArtistPortraits[curArtist.name]
            ?: YouTubeArtistRepositoryImpl.getCachedPortrait(curArtist.name)
            ?: localMatchingSongs.firstOrNull()?.artworkUri
        ArtistDetailSheet(
            artist = curArtist.copy(
                songCountText = audience ?: curArtist.songCountText,
                artworkModel = resolvedArtwork
            ),
            onlineSongs = artistDetailSongs,
            localSongs = localMatchingSongs,
            onlineAlbums = artistDetailAlbums,
            similarArtists = artistDetailSimilarArtists,
            bioData = artistDetailBioData,
            isLoading = isArtistDetailLoading,
            errorMessage = artistDetailErrorMessage,
            isSavedInLibrary = isSaved,
            appSettings = appSettings,
            onToggleSaveToLibrary = {
                if (isSaved) {
                    savedArtistEntry?.let { onRemoveSavedOnlineArtist?.invoke(it.id) }
                } else {
                    val toSave = SavedOnlineArtist(
                        id = curArtist.artistId.ifBlank { "local_${curArtist.name}" },
                        name = curArtist.name,
                        profileImageUrl = resolvedArtwork as? String,
                        source = AudioSource.YOUTUBE,
                        audienceText = audience
                    )
                    onSaveOnlineArtist?.invoke(toSave)
                }
            },
            onDismiss = {
                selectedArtist = null
                artistDetailSongs = emptyList()
                artistDetailAlbums = emptyList()
                artistDetailSimilarArtists = emptyList()
                artistDetailBioData = null
                artistDetailErrorMessage = null
            },
            onPlaySong = onSongSelected,
            onPlayOnlineSong = onOnlineSongSelected,
            onPlayAll = {
                if (artistDetailSongs.isNotEmpty()) {
                    onOnlineSongSelected(artistDetailSongs.first(), artistDetailSongs)
                } else if (localMatchingSongs.isNotEmpty()) {
                    onSongSelected(localMatchingSongs.first(), localMatchingSongs)
                }
                selectedArtist = null
            },
            onShuffleAll = {
                if (artistDetailSongs.isNotEmpty()) {
                    val shuffled = artistDetailSongs.shuffled()
                    onOnlineSongSelected(shuffled.first(), shuffled)
                } else if (localMatchingSongs.isNotEmpty()) {
                    val shuffled = localMatchingSongs.shuffled()
                    onSongSelected(shuffled.first(), shuffled)
                }
                selectedArtist = null
            },
            onOpenOnlineAlbum = { album ->
                selectedOnlinePlaylist = album
            },
            onSelectSimilarArtist = { simArtist ->
                selectedArtist = simArtist
                artistDetailSongs = emptyList()
                artistDetailAlbums = emptyList()
                artistDetailSimilarArtists = emptyList()
                artistDetailBioData = null
            },
            audienceText = audience
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
    // Hide explicit-title tracks from Library browsing without deleting them
    // or applying this rule to the dedicated Search screen.
    val libraryVisibleSongs = remember(songs) {
        songs.filterNot { ExplicitTitleFilter.shouldHideTitle(it.title) }
    }

    val filteredSongs = remember(libraryVisibleSongs, query) {
        if (query.isBlank()) libraryVisibleSongs else libraryVisibleSongs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    val libraryVisibleFavorites = remember(favoriteSongs) {
        favoriteSongs.filterNot { ExplicitTitleFilter.shouldHideTitle(it.title) }
    }

    val filteredFavorites = remember(libraryVisibleFavorites, query) {
        if (query.isBlank()) libraryVisibleFavorites else libraryVisibleFavorites.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    val albumsList = remember(libraryVisibleSongs, query) {
        val grouped = libraryVisibleSongs.groupBy { it.album }
        if (query.isBlank()) {
            grouped.entries.toList()
        } else {
            grouped.entries.filter { (albumName, songs) ->
                albumName.contains(query, ignoreCase = true) ||
                songs.any { it.artist.contains(query, ignoreCase = true) }
            }.toList()
        }
    }

    LaunchedEffect(Unit) {
        ArtistArtworkCache.init(context)
    }

    val artistsList = remember(libraryVisibleSongs, query) {
        val grouped = ArtistHelper.groupSongsByArtist(libraryVisibleSongs)
        if (query.isBlank()) {
            grouped
        } else {
            grouped.filter { (artistName, _) ->
                artistName.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(savedOnlineArtists) {
        val artistsNeedingAudience = savedOnlineArtists.filter { artist ->
            artist.audienceText.isNullOrBlank() &&
            onlineArtistAudiences[artist.id] == null &&
            YouTubeArtistRepositoryImpl.getCachedAudience(artist.name) == null
        }
        if (artistsNeedingAudience.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                for (artist in artistsNeedingAudience) {
                    try {
                        val aud = youtubeArtistRepository.resolveArtistAudience(artist.name, artist.id)
                        if (!aud.isNullOrBlank()) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onlineArtistAudiences = onlineArtistAudiences + (artist.id to aud)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(artistsList) {
        val missingArtists = artistsList.map { it.first }.filter { artistName ->
            ArtistArtworkCache.get(artistName) == null &&
            YouTubeArtistRepositoryImpl.getCachedPortrait(artistName) == null
        }
        if (missingArtists.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                for (artistName in missingArtists) {
                    try {
                        val portraitUrl = youtubeArtistRepository.resolveArtistPortrait(artistName)
                        if (portraitUrl != null) {
                            ArtistArtworkCache.put(context, artistName, portraitUrl)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onlineArtistPortraits = onlineArtistPortraits + (artistName to portraitUrl)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val filteredPlaylists = remember(playlists, query) {
        if (query.isBlank()) playlists else playlists.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = MaterialTheme.colorScheme.outline

    val libraryArtwork = remember(playlists, songs, favoriteSongs) {
        playlists.firstOrNull { it.onlineSongs.any { os -> os.artworkUrl.isNotBlank() } }?.onlineSongs?.firstOrNull()?.artworkUrl
            ?: songs.firstOrNull { it.artworkUri != null }?.artworkUri
            ?: favoriteSongs.firstOrNull { it.artworkUri != null }?.artworkUri
    }

    AppThemedScreen(
        area = AppArea.LIBRARY,
        artworkModel = libraryArtwork,
        appSettings = appSettings
    ) {
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
                        text = when (selectedTab) {
                            LibraryTab.FAVORITES -> "${libraryVisibleFavorites.size} favorite tracks"
                            LibraryTab.SONGS -> "${libraryVisibleSongs.size} tracks collected"
                            LibraryTab.ALBUMS -> "${albumsList.size} albums"
                            LibraryTab.ARTISTS -> "${artistsList.size} artists"
                            LibraryTab.PLAYLISTS -> "${filteredPlaylists.size + savedOnlinePlaylists.size} playlists"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
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

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Category Tabs (Songs | Favorites | Albums | Artists | Playlists)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(AuraShapes.Control)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                AuraShapes.Control
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                AuraShapes.Control
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
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
                            contentPadding = PaddingValues(bottom = 140.dp)
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

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (filteredSongs.isNotEmpty()) {
                                                    val shuffled = filteredSongs.shuffled()
                                                    onSongSelected(shuffled.first(), shuffled)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } }
                                )
                            }
                        }
                    }
                }

                LibraryTab.FAVORITES -> {
                    if (filteredFavorites.isEmpty()) {
                        AuraEmptyState(
                            title = if (query.isNotBlank()) "No Matching Favorites" else "No Favorite Songs Yet",
                            message = if (query.isNotBlank()) "No favorite songs matching \"$query\" found." else "Heart songs while playing or browsing to add them to your favorites collection.",
                            icon = if (query.isNotBlank()) Icons.Default.MusicNote else Icons.Default.FavoriteBorder,
                            actionLabel = if (query.isNotBlank()) "Clear Filter" else null,
                            onAction = if (query.isNotBlank()) { { onQueryChange("") } } else null,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 140.dp)
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
                                            if (filteredFavorites.isNotEmpty()) {
                                                onSongSelected(filteredFavorites.first(), filteredFavorites)
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
                                        Text("Play All (${filteredFavorites.size})", fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (filteredFavorites.isNotEmpty()) {
                                                    val shuffled = filteredFavorites.shuffled()
                                                    onSongSelected(shuffled.first(), shuffled)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            }

                            // Favorite tracks list using unified SongRow
                            items(filteredFavorites, key = { "fav_${it.id}" }) { song ->
                                val isCurrent = song.id == currentSongId

                                SongRow(
                                    song = song,
                                    isPlaying = isCurrent,
                                    isFavorite = true,
                                    onClick = { onSongSelected(song, filteredFavorites) },
                                    onPlayNow = { onSongSelected(song, filteredFavorites) },
                                    onToggleFavorite = { onFavoriteToggle(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } }
                                )
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    val filteredOnlineAlbums = remember(savedOnlineAlbums, query) {
                        if (query.isBlank()) savedOnlineAlbums else savedOnlineAlbums.filter {
                            it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                        }
                    }
                    if (albumsList.isEmpty() && filteredOnlineAlbums.isEmpty()) {
                        AuraEmptyState(
                            title = "No Albums Found",
                            message = if (query.isNotBlank()) "No albums matching \"$query\"." else "Your albums will appear here once audio files or online albums are saved.",
                            icon = Icons.Default.Album,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (filteredOnlineAlbums.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(
                                        title = "Saved Online Albums",
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                items(filteredOnlineAlbums, key = { "saved_online_album_${it.id}" }) { album ->
                                    AlbumCard(
                                        title = album.title,
                                        subtitle = "${album.source.badgeText} • ${album.subtitle.ifBlank { "Album" }}",
                                        artworkModel = album.artworkUrl,
                                        size = 150.dp,
                                        onClick = { selectedOnlinePlaylist = album }
                                    )
                                }
                            }
                            if (albumsList.isNotEmpty()) {
                                if (filteredOnlineAlbums.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(
                                            title = "Local Albums",
                                            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                        )
                                    }
                                }
                                items(albumsList, key = { it.key }) { (albumName, albumSongs) ->
                                    val representativeSong = albumSongs.firstOrNull { !it.artworkUri.isNullOrBlank() } ?: albumSongs.firstOrNull()
                                    AlbumCard(
                                        title = albumName,
                                        subtitle = representativeSong?.artist ?: "Unknown Artist",
                                        artworkModel = representativeSong?.artworkUri,
                                        size = 150.dp,
                                        onClick = { selectedLocalAlbum = (albumName to albumSongs) }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    val filteredOnlineArtists = remember(savedOnlineArtists, query) {
                        if (query.isBlank()) savedOnlineArtists else savedOnlineArtists.filter {
                            it.name.contains(query, ignoreCase = true)
                        }
                    }
                    if (artistsList.isEmpty() && filteredOnlineArtists.isEmpty()) {
                        AuraEmptyState(
                            title = "No Artists Found",
                            message = if (query.isNotBlank()) "No artists matching \"$query\"." else "Artists from your local collection or saved online artists will be listed here.",
                            icon = Icons.Default.Person,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(110.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (filteredOnlineArtists.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SectionHeader(
                                        title = "Saved Online Artists",
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                items(filteredOnlineArtists, key = { "saved_online_artist_${it.id}" }) { artist ->
                                    ArtistCard(
                                        name = artist.name,
                                        artworkModel = artist.profileImageUrl,
                                        size = 110.dp,
                                        onClick = {
                                            val audience = formatArtistAudience(
                                                artist.audienceText
                                                    ?: onlineArtistAudiences[artist.id]
                                                    ?: YouTubeArtistRepositoryImpl.getCachedAudience(artist.name)
                                            )
                                            selectedArtist = ArtistItem(
                                                name = artist.name,
                                                songCountText = audience.orEmpty(),
                                                artworkModel = artist.profileImageUrl,
                                                isOnline = true,
                                                artistId = artist.id
                                            )
                                        }
                                    )
                                }
                            }
                            if (artistsList.isNotEmpty()) {
                                if (filteredOnlineArtists.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        SectionHeader(
                                            title = "Local Artists",
                                            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                        )
                                    }
                                }
                                items(artistsList, key = { "local_artist_${it.first}" }) { (artistName, artistSongs) ->
                                    val portraitUrl = onlineArtistPortraits[artistName]
                                        ?: ArtistArtworkCache.get(artistName)
                                        ?: YouTubeArtistRepositoryImpl.getCachedPortrait(artistName)
                                        ?: artistSongs.firstOrNull()?.artworkUri
                                    ArtistCard(
                                        name = artistName,
                                        songCountText = "${artistSongs.size} songs",
                                        artworkModel = portraitUrl,
                                        size = 110.dp,
                                        onClick = {
                                            val cachedAudience = formatArtistAudience(
                                                onlineArtistAudiences[artistName]
                                                    ?: YouTubeArtistRepositoryImpl.getCachedAudience(artistName)
                                            )
                                            selectedArtist = ArtistItem(
                                                name = artistName,
                                                songCountText = cachedAudience ?: "${artistSongs.size} songs",
                                                artworkModel = portraitUrl,
                                                isOnline = false,
                                                artistId = ""
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp)
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
                        if (filteredPlaylists.isNotEmpty() || libraryVisibleFavorites.isNotEmpty()) {
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Pinned Favorite Songs Card in Playlists
                                    if (libraryVisibleFavorites.isNotEmpty() || query.isBlank()) {
                                        item(key = "fav_songs_pinned_pl") {
                                            FavoriteSongsPlaylistCard(
                                                count = libraryVisibleFavorites.size,
                                                onClick = { onTabSelected(LibraryTab.FAVORITES) }
                                            )
                                        }
                                    }

                                    items(filteredPlaylists, key = { "pl_${it.id}" }) { playlist ->
                                        val firstSong = playlist.songIds.firstOrNull()?.let { songById[it] }
                                        val artworkModel = firstSong?.artworkUri
                                            ?: playlist.onlineSongs.firstOrNull()?.artworkUrl?.ifBlank { null }
                                        PlaylistCard(
                                            title = playlist.name,
                                            songCountText = "${playlist.songIds.size + playlist.onlineSongs.size} songs",
                                            artworkModel = artworkModel,
                                            onClick = { onPlaylistSelected(playlist) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        } else if (playlists.isEmpty() && libraryVisibleFavorites.isEmpty()) {
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

                        // Saved Online Playlists Section
                        if (savedOnlinePlaylists.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Saved Online Playlists",
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(savedOnlinePlaylists, key = { "saved_online_${it.id}" }) { playlist ->
                                        PlaylistCard(
                                            title = playlist.title,
                                            songCountText = "${playlist.source.badgeText} • ${playlist.songCount} songs",
                                            artworkModel = playlist.artworkUrl,
                                            onClick = { selectedOnlinePlaylist = playlist }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun FavoriteSongsPlaylistCard(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite Songs",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Favorite Songs",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (count == 1) "1 song" else "$count songs",
            fontSize = 12.sp,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

