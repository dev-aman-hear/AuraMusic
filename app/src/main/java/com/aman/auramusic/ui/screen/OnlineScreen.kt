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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
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
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.AuraLoadingState
import com.aman.auramusic.ui.component.PlaylistCard
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongRow
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

    var searchQuery by remember { mutableStateOf("") }
    var trendingSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var curatedPlaylists by remember { mutableStateOf(emptyList<OnlinePlaylist>()) }
    var searchResults by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var selectedPlaylist by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var playlistSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    suspend fun loadCatalog() {
        isLoading = true
        loadError = false
        try {
            // Load independently so a temporary playlist endpoint failure does not hide tracks.
            trendingSongs = repository.getTrending(AudioSource.ALL)
            curatedPlaylists = try {
                repository.getCuratedPlaylists(AudioSource.ALL)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            loadError = trendingSongs.isEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadCatalog() }

    // LaunchedEffect cancels the previous search when the query changes, preventing stale
    // responses from replacing newer results.
    LaunchedEffect(searchQuery, selectedPlaylist) {
        if (selectedPlaylist != null || searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }

        delay(300)
        isLoading = true
        loadError = false
        try {
            searchResults = repository.search(searchQuery.trim(), AudioSource.ALL)
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
        else -> trendingSongs
    }
    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedText = textColor.copy(alpha = 0.62f)
    val fieldColor = if (isDark) Color(0xFF191A22) else Color(0xFFF4F4F7)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "online_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSearching) "Search" else "Listen now",
                            color = textColor,
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp
                        )
                        Text(
                            text = if (isSearching) "Find your next favourite" else "A little more of what you love",
                            color = mutedText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    IconButton(onClick = { scope.launch { loadCatalog() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh catalog", tint = textColor)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        selectedPlaylist = null
                        playlistSongs = emptyList()
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Songs, artists, albums…", color = mutedText, fontSize = 14.sp) },
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

        if (!isSearching && selectedPlaylist == null && trendingSongs.isNotEmpty()) {
            item(key = "featured_song") {
                FeaturedOnlineCard(
                    song = trendingSongs.first(),
                    onClick = { onOnlineSongSelected(trendingSongs.first(), trendingSongs) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (curatedPlaylists.isNotEmpty()) {
                item(key = "curated_heading") {
                    SectionHeader(
                        eyebrow = "MADE TO BE PLAYED",
                        title = "Explore mixes",
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                item(key = "curated_playlists") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(curatedPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                            PlaylistCard(
                                title = playlist.title,
                                songCountText = if (playlist.songCount > 0) "${playlist.songCount} songs" else playlist.subtitle.ifBlank { "Curated for you" },
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
        }

        item(key = "tracks_heading") {
            SectionHeader(
                eyebrow = when {
                    selectedPlaylist != null -> selectedPlaylist?.subtitle?.ifBlank { "YOUR NEXT LISTEN" } ?: "YOUR NEXT LISTEN"
                    isSearching -> "ONLINE CATALOG"
                    else -> "FRESH FROM THE CATALOG"
                },
                title = when {
                    selectedPlaylist != null -> selectedPlaylist?.title ?: "Playlist"
                    isSearching -> "Results"
                    else -> "Trending right now"
                },
                actionText = if (selectedPlaylist != null) "Clear" else null,
                onActionClick = if (selectedPlaylist != null) ({ selectedPlaylist = null; playlistSongs = emptyList() }) else null,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (isLoading && displayedSongs.isEmpty()) {
            item(key = "loading") {
                AuraLoadingState(
                    message = when {
                        selectedPlaylist != null -> "Loading playlist…"
                        isSearching -> "Searching music…"
                        else -> "Finding something good…"
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp)
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
                        selectedPlaylist != null -> "This playlist couldn't be loaded right now. Check your connection and try again."
                        else -> "Check your connection and refresh the catalog to discover music."
                    },
                    icon = Icons.Default.MusicNote,
                    actionLabel = "Try again",
                    onAction = { scope.launch {
                        if (selectedPlaylist != null) {
                            val playlist = selectedPlaylist ?: return@launch
                            isLoading = true
                            try { playlistSongs = repository.getPlaylistSongs(playlist); loadError = playlistSongs.isEmpty() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { loadError = true }
                            finally { isLoading = false }
                        } else if (isSearching) {
                            isLoading = true
                            loadError = false
                            try {
                                searchResults = repository.search(searchQuery.trim(), AudioSource.ALL)
                                loadError = searchResults.isEmpty()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                searchResults = emptyList()
                                loadError = true
                            } finally {
                                isLoading = false
                            }
                        } else loadCatalog()
                    } },
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
                )
            }
        } else {
            items(displayedSongs, key = { "${it.source.name}_${it.id}" }) { song ->
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

@Composable
private fun FeaturedOnlineCard(
    song: OnlineSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24202C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF3A2034), Color(0xFF211F30), Color(0xFF11151E))
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.78f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "YOUR DAILY DISCOVERY",
                    color = Color(0xFFFF91A5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AuraCoral),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Play featured track", tint = Color.White)
            }
        }
    }
}
