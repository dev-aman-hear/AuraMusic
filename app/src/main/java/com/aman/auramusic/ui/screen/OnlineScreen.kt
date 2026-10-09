package com.aman.auramusic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.AuraLoadingState
import com.aman.auramusic.ui.component.PlaylistCard
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.AuraShapes
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AuraCoral = Color(0xFFFF5C7A)

@Composable
fun OnlineScreen(
    onlinePlaybackManager: OnlinePlaybackManager,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()
    val repository = remember { OnlineMusicRepository() }
    val playbackState by onlinePlaybackManager.playbackState.collectAsStateWithLifecycle()

    var selectedSource by remember { mutableStateOf(AudioSource.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var feedSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var curatedPlaylists by remember { mutableStateOf(emptyList<OnlinePlaylist>()) }
    var searchResults by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var selectedPlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var playlistSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    suspend fun loadCatalog(source: AudioSource = selectedSource) {
        isLoading = true
        loadError = false
        try {
            feedSongs = if (source == AudioSource.ALL) {
                repository.getCuratedSongFeed()
            } else {
                repository.getTrending(source)
            }.distinctBy { "${it.source.name}_${it.id}" }

            curatedPlaylists = try {
                repository.getCuratedPlaylists(source)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            loadError = feedSongs.isEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            feedSongs = emptyList()
            loadError = true
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSource) {
        selectedPlaylist = null
        playlistSongs = emptyList()
        loadCatalog(selectedSource)
    }

    LaunchedEffect(searchQuery, selectedPlaylist, selectedSource) {
        if (selectedPlaylist != null || searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        isLoading = true
        loadError = false
        try {
            searchResults = repository.search(searchQuery.trim(), selectedSource)
                .distinctBy { "${it.source.name}_${it.id}" }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            searchResults = emptyList()
            loadError = true
        } finally {
            isLoading = false
        }
    }

    val isSearching = searchQuery.isNotBlank() && selectedPlaylist == null
    val displayedSongs = when {
        selectedPlaylist != null -> playlistSongs
        isSearching -> searchResults
        else -> feedSongs
    }
    val heroSong = displayedSongs.firstOrNull() ?: feedSongs.firstOrNull()
    val quickPicks = remember(feedSongs) { feedSongs.drop(1).take(10) }
    val chartSongs = remember(feedSongs) { feedSongs.drop(4).take(12) }
    val deepCuts = remember(feedSongs) { feedSongs.drop(12).take(24) }
    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedText = textColor.copy(alpha = 0.62f)
    val fieldColor = if (isDark) Color(0xFF191A22) else Color(0xFFF4F4F7)

    AuraScreenBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "online_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSearching) "Search" else "Listen Now",
                                color = textColor,
                                fontSize = 34.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.sp
                            )
                            Text(
                                text = when {
                                    selectedPlaylist != null -> selectedPlaylist?.subtitle?.ifBlank { "Curated for streaming" } ?: "Curated for streaming"
                                    isSearching -> "Across ${selectedSource.displayName}"
                                    else -> "YouTube and JioSaavn, shaped for Aura"
                                },
                                color = mutedText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        IconButton(onClick = { scope.launch { loadCatalog(selectedSource) } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh catalog", tint = textColor)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OnlineSourceTabs(
                        selectedSource = selectedSource,
                        onSourceSelected = {
                            selectedSource = it
                            selectedPlaylist = null
                            playlistSongs = emptyList()
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            selectedPlaylist = null
                            playlistSongs = emptyList()
                            searchQuery = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = AuraShapes.Surface,
                        placeholder = { Text("Search songs, artists, albums", color = mutedText, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedText) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; selectedPlaylist = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = mutedText)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCoral,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = fieldColor,
                            unfocusedContainerColor = fieldColor,
                            cursorColor = AuraCoral
                        )
                    )
                }
            }

            if (!isSearching && selectedPlaylist == null && heroSong != null) {
                item(key = "hero") {
                    FeaturedOnlineCard(
                        song = heroSong,
                        sourceLabel = selectedSource.displayName,
                        onClick = { onOnlineSongSelected(heroSong, feedSongs.ifEmpty { listOf(heroSong) }) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (curatedPlaylists.isNotEmpty()) {
                    item(key = "playlist_heading") {
                        SectionHeader(
                            eyebrow = "EDITOR'S ROOM",
                            title = "Featured Playlists",
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    item(key = "playlist_rail") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(curatedPlaylists, key = { "playlist_${it.source.name}_${it.id}" }) { playlist ->
                                PlaylistCard(
                                    title = playlist.title,
                                    songCountText = if (playlist.songCount > 0) "${playlist.songCount} songs" else playlist.subtitle.ifBlank { playlist.source.displayName },
                                    artworkModel = playlist.artworkUrl,
                                    onClick = {
                                        scope.launch {
                                            selectedPlaylist = playlist
                                            searchQuery = ""
                                            playlistSongs = emptyList()
                                            isLoading = true
                                            loadError = false
                                            try {
                                                playlistSongs = repository.getPlaylistSongs(playlist)
                                                    .distinctBy { "${it.source.name}_${it.id}" }
                                                loadError = playlistSongs.isEmpty()
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                loadError = true
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (quickPicks.isNotEmpty()) {
                    item(key = "quick_picks_heading") {
                        SectionHeader(
                            eyebrow = "START HERE",
                            title = "Quick Picks",
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                    item(key = "quick_picks") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(quickPicks, key = { "pick_${it.source.name}_${it.id}" }) { song ->
                                OnlineArtworkCard(
                                    song = song,
                                    onClick = { onOnlineSongSelected(song, feedSongs) }
                                )
                            }
                        }
                    }
                }

                if (chartSongs.isNotEmpty()) {
                    item(key = "charts_heading") {
                        SectionHeader(
                            eyebrow = selectedSource.badgeText,
                            title = "Charts and New Heat",
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                    items(chartSongs.take(8), key = { "chart_${it.source.name}_${it.id}" }) { song ->
                        val isActive = playbackState.currentSong?.id == song.id
                        SongRow(
                            onlineSong = song,
                            isPlaying = isActive && playbackState.isPlaying,
                            isActive = isActive,
                            onClick = { onOnlineSongSelected(song, feedSongs) },
                            onPlayNow = { onOnlineSongSelected(song, feedSongs) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                if (deepCuts.isNotEmpty()) {
                    item(key = "more_heading") {
                        SectionHeader(
                            eyebrow = "KEEP LISTENING",
                            title = "More to Explore",
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                }
            } else {
                item(key = "tracks_heading") {
                    SectionHeader(
                        eyebrow = when {
                            selectedPlaylist != null -> selectedPlaylist?.subtitle?.ifBlank { "PLAYLIST" } ?: "PLAYLIST"
                            isSearching -> selectedSource.displayName.uppercase()
                            else -> "ONLINE CATALOG"
                        },
                        title = when {
                            selectedPlaylist != null -> selectedPlaylist?.title ?: "Playlist"
                            isSearching -> "Results"
                            else -> "Songs"
                        },
                        actionText = if (selectedPlaylist != null) "Clear" else null,
                        onActionClick = if (selectedPlaylist != null) ({ selectedPlaylist = null; playlistSongs = emptyList() }) else null,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            if (isLoading && displayedSongs.isEmpty()) {
                item(key = "loading") {
                    AuraLoadingState(
                        message = when {
                            selectedPlaylist != null -> "Loading playlist"
                            isSearching -> "Searching music"
                            else -> "Finding something good"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                    )
                }
            } else if (displayedSongs.isEmpty()) {
                item(key = "empty_state") {
                    AuraEmptyState(
                        title = when {
                            loadError && !isSearching -> "Couldn't load music"
                            isSearching -> "No matches yet"
                            selectedPlaylist != null -> "Playlist unavailable"
                            else -> "Nothing to play yet"
                        },
                        message = when {
                            isSearching -> "Try another song, artist, or album name."
                            selectedPlaylist != null -> "This playlist could not be loaded right now. Check your connection and try again."
                            else -> "Check your connection and refresh the catalog to discover music."
                        },
                        icon = Icons.Default.MusicNote,
                        actionLabel = "Try again",
                        onAction = {
                            scope.launch {
                                if (selectedPlaylist != null) {
                                    val playlist = selectedPlaylist ?: return@launch
                                    isLoading = true
                                    try {
                                        playlistSongs = repository.getPlaylistSongs(playlist)
                                        loadError = playlistSongs.isEmpty()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        loadError = true
                                    } finally {
                                        isLoading = false
                                    }
                                } else if (isSearching) {
                                    isLoading = true
                                    loadError = false
                                    try {
                                        searchResults = repository.search(searchQuery.trim(), selectedSource)
                                        loadError = searchResults.isEmpty()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        searchResults = emptyList()
                                        loadError = true
                                    } finally {
                                        isLoading = false
                                    }
                                } else {
                                    loadCatalog(selectedSource)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp)
                    )
                }
            } else {
                val trailingSongs = when {
                    !isSearching && selectedPlaylist == null -> deepCuts
                    else -> displayedSongs
                }
                items(trailingSongs, key = { "song_${it.source.name}_${it.id}" }) { song ->
                    val isActive = playbackState.currentSong?.id == song.id
                    SongRow(
                        onlineSong = song,
                        isPlaying = isActive && playbackState.isPlaying,
                        isActive = isActive,
                        onClick = { onOnlineSongSelected(song, displayedSongs) },
                        onPlayNow = { onOnlineSongSelected(song, displayedSongs) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineSourceTabs(
    selectedSource: AudioSource,
    onSourceSelected: (AudioSource) -> Unit
) {
    val sources = listOf(AudioSource.ALL, AudioSource.JIOSAAVN, AudioSource.YOUTUBE)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sources, key = { it.name }) { source ->
            val selected = selectedSource == source
            Surface(
                onClick = { onSourceSelected(source) },
                shape = AuraShapes.Control,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Text(
                    text = when (source) {
                        AudioSource.ALL -> "Listen Now"
                        AudioSource.JIOSAAVN -> "JioSaavn"
                        AudioSource.YOUTUBE -> "YouTube"
                        AudioSource.SPOTIFY -> "Spotify"
                    },
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedOnlineCard(
    song: OnlineSong,
    sourceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(244.dp)
            .clickable(onClick = onClick),
        shape = AuraShapes.Surface,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24171C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF682637), Color(0xFF24171C), Color(0xFF101010))
                    )
                )
        ) {
            AuraArtwork(
                model = song.artworkUrl,
                size = 260,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(210.dp),
                shape = AuraShapes.Artwork,
                elevation = 10.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.72f)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = sourceLabel.uppercase(),
                    color = Color(0xFFFFA1B1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 27.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AuraCoral),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play featured track", tint = Color.White)
            }
        }
    }
}

@Composable
private fun OnlineArtworkCard(
    song: OnlineSong,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(AuraShapes.Surface)
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        AuraArtwork(
            model = song.artworkUrl,
            size = 148,
            modifier = Modifier.size(148.dp),
            shape = AuraShapes.Artwork,
            elevation = 6.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
