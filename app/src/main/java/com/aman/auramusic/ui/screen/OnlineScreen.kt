package com.aman.auramusic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.aman.auramusic.ui.theme.AuraCyan
import com.aman.auramusic.ui.theme.AuraDarkBackground
import com.aman.auramusic.ui.theme.AuraDarkSurfaceElevated
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val selectedSource = AudioSource.ALL
    var trendingSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var curatedPlaylists by remember { mutableStateOf<List<OnlinePlaylist>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadInitialData() {
        isLoading = true
        scope.launch {
            try {
                trendingSongs = repository.getTrending(selectedSource)
                curatedPlaylists = repository.getCuratedPlaylists(selectedSource)
            } catch (_: Exception) {
                trendingSongs = emptyList()
                curatedPlaylists = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (searchQuery.isBlank()) {
            loadInitialData()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isLoading = true
            delay(250)
            try {
                searchResults = repository.search(searchQuery, selectedSource)
            } catch (_: Exception) {
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        } else {
            searchResults = emptyList()
            if (trendingSongs.isEmpty()) {
                loadInitialData()
            }
        }
    }

    val displayedSongs = if (searchQuery.isNotBlank()) searchResults else trendingSongs

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDark) AuraDarkSurfaceElevated else Color(0xFFF1F5F9)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AuraDarkBackground else MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // --- EDITORIAL HEADER ---
        item {
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
                            text = "Discover",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = textColor,
                            letterSpacing = (-0.5).sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "High-Fidelity 320kbps Catalog",
                                style = MaterialTheme.typography.labelMedium,
                                color = AuraCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { loadInitialData() }) {
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

                Spacer(modifier = Modifier.height(16.dp))

                // Unified Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search songs, artists, albums, podcasts...",
                            color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = textColor
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = cardBorder,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg
                    ),
                    singleLine = true
                )
            }
        }

        // Curated Playlists Carousel (when not actively searching)
        if (searchQuery.isBlank() && curatedPlaylists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "Curated Playlists",
                    eyebrow = "Featured",
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(curatedPlaylists, key = { "pl_${it.id}" }) { playlist ->
                        PlaylistCard(
                            title = playlist.title,
                            songCountText = "${playlist.songCount} songs",
                            artworkModel = playlist.artworkUrl,
                            onClick = {
                                // Load this playlist's songs as the active search view
                                searchQuery = playlist.title
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Status / Section Title Header
        item {
            val sectionTitle = when {
                searchQuery.isNotBlank() -> "Search Results"
                else -> "Trending Tracks"
            }
            val eyebrowText = when {
                searchQuery.isNotBlank() -> "Found ${displayedSongs.size} tracks"
                else -> "Top 50 Global"
            }

            SectionHeader(
                title = sectionTitle,
                eyebrow = eyebrowText
            )
        }

        // Error message if any
        if (playbackState.errorMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = playbackState.errorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Loading State
        if (isLoading && displayedSongs.isEmpty()) {
            item {
                AuraLoadingState(
                    message = if (searchQuery.isNotBlank()) "Searching online catalogs..." else "Loading trending tracks...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                )
            }
        }

        // Empty state
        if (displayedSongs.isEmpty() && !isLoading) {
            item {
                AuraEmptyState(
                    title = if (searchQuery.isNotBlank()) "No Songs Found" else "No Music Available",
                    message = if (searchQuery.isNotBlank()) {
                        "We couldn't find any tracks matching \"$searchQuery\". Try checking the spelling or searching another artist."
                    } else {
                        "Could not fetch trending music. Tap the refresh button or check your network connection."
                    },
                    icon = Icons.Default.MusicNote,
                    actionLabel = "Refresh Catalog",
                    onAction = { loadInitialData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                )
            }
        } else {
            // Unified tracks list using universal SongRow
            items(displayedSongs, key = { it.id }) { song ->
                val isActive = playbackState.currentSong?.id == song.id
                SongRow(
                    onlineSong = song,
                    isPlaying = isActive && playbackState.isPlaying,
                    onClick = {
                        onOnlineSongSelected(song, displayedSongs)
                    },
                    onPlayNow = {
                        onOnlineSongSelected(song, displayedSongs)
                    }
                )
            }
        }
    }
}
