package com.aman.auramusic.online.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.StreamingEngine
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.online.ui.components.SongCard
import kotlinx.coroutines.launch

/**
 * Online streaming discovery and search view integrated directly into AuraMusic's Search tab.
 * Allows searching across JioSaavn (320kbps) & YouTube Music with 4 engine strategies:
 * - fast4x/RiPlay
 * - kushagrasinghx/BitChord
 * - VikrantRuhela/DA-Tunes
 * - cr7pt0gr4ph7/obsidian
 */
@Composable
fun OnlineSearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onlinePlaybackManager: OnlinePlaybackManager,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit,
    modifier: Modifier = Modifier,
    repository: OnlineMusicRepository = remember { OnlineMusicRepository() }
) {
    val scope = rememberCoroutineScope()
    val playbackState by onlinePlaybackManager.playbackState.collectAsState()

    var selectedSource by remember { mutableStateOf(AudioSource.ALL) }
    var searchResults by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val quickSearches = remember {
        listOf(
            "Trending Hits 2026",
            "Arijit Singh",
            "Believer",
            "Diljit Dosanjh",
            "Coldplay",
            "Lofi Beats",
            "Alan Walker",
            "Sidhu Moosewala",
            "Anirudh",
            "Bollywood Hits"
        )
    }

    fun executeSearch(searchTarget: String) {
        if (searchTarget.isBlank()) return
        isLoading = true
        scope.launch {
            try {
                val results = repository.search(searchTarget, selectedSource)
                searchResults = results
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // Trigger search when query or source changes
    LaunchedEffect(query, selectedSource) {
        if (query.isNotBlank()) {
            executeSearch(query)
        } else {
            // Load initial trending items if query is blank
            isLoading = true
            try {
                searchResults = repository.getTrending(selectedSource)
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Source Catalog Filter Chips (ALL, JIOSAAVN, YOUTUBE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioSource.values().forEach { source ->
                val isSelected = selectedSource == source
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentCyan else SurfaceCard)
                        .border(1.dp, if (isSelected) AccentCyan else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedSource = source }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = source.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextPrimary
                    )
                }
            }
        }


        // Quick Trending Suggestions
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickSearches) { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            onQueryChange(tag)
                            executeSearch(tag)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (query.isNotBlank()) "Search Results" else "Trending Hits",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "${searchResults.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Error / Notice Banner
        AnimatedVisibility(
            visible = playbackState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            playbackState.errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentRed.copy(alpha = 0.15f))
                        .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = AccentRed,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Results List
        if (searchResults.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (query.isNotBlank()) "No online results found for '$query'" else "Search any song, artist or album",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchResults.forEach { song ->
                    val isActive = playbackState.currentSong?.id == song.id
                    SongCard(
                        song = song,
                        isActive = isActive,
                        isPlaying = isActive && playbackState.isPlaying,
                        onClick = {
                            onOnlineSongSelected(song, searchResults)
                        }
                    )
                }
            }
        }
    }
}
