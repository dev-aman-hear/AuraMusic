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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.AuraLoadingState
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.AuraShapes
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.liquidGlass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val AuraCoral = Color(0xFFFF5C7A)

private data class MoodCollection(
    val title: String,
    val query: String
)

private val moodCollections = listOf(
    MoodCollection("Romantic", "Romantic Love Songs"),
    MoodCollection("Sad", "Sad Songs"),
    MoodCollection("Party", "Party Hits"),
    MoodCollection("Chill", "Chill Lofi Songs"),
    MoodCollection("Workout", "Workout Hits"),
    MoodCollection("Focus", "Focus Music")
)

@Composable
fun OnlineScreen(
    onlinePlaybackManager: OnlinePlaybackManager,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val repository = remember { OnlineMusicRepository() }

    val currentPlayingSongId by remember(onlinePlaybackManager) {
        onlinePlaybackManager.playbackState
            .map { it.currentSong?.id }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val isPlaying by remember(onlinePlaybackManager) {
        onlinePlaybackManager.playbackState
            .map { it.isPlaying }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    var feedSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var movieSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var moodResults by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var activeMood by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMoodLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var moodJob by remember { mutableStateOf<Job?>(null) }
    var selectedCollection by remember { mutableStateOf<OnlinePlaylist?>(null) }
    var selectedCollectionSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }

    suspend fun loadCatalog() {
        isLoading = true
        loadError = false
        try {
            feedSongs = repository.getCuratedSongFeed()
                .distinctBy { "${it.source.name}_${it.id.ifBlank { it.title }}" }
            movieSongs = try {
                repository.search("Latest Bollywood Movie Songs", AudioSource.ALL)
                    .distinctBy { "${it.source.name}_${it.id.ifBlank { it.title }}" }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            loadError = feedSongs.isEmpty() && movieSongs.isEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            feedSongs = emptyList()
            movieSongs = emptyList()
            loadError = true
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadCatalog() }

    val uniqueSongs = remember(feedSongs, movieSongs) {
        (movieSongs + feedSongs).distinctBy { "${it.source.name}_${it.id.ifBlank { it.title }}" }
    }
    val featuredSong = movieSongs.firstOrNull() ?: feedSongs.firstOrNull()
    val trendingSongs = remember(feedSongs, movieSongs) {
        feedSongs.filterNot { candidate ->
            movieSongs.any { it.source == candidate.source && it.id == candidate.id }
        }.take(12)
    }
    val movieAlbums = remember(movieSongs, feedSongs) {
        val albumPool = if (movieSongs.any { it.album.isNotBlank() }) movieSongs else feedSongs
        albumPool.filter { it.album.isNotBlank() }
            .groupBy { it.album.trim() }
            .entries
            .map { entry -> entry.key to entry.value.distinctBy { "${it.source.name}_${it.id.ifBlank { it.title }}" } }
            .distinctBy { it.first.lowercase() }
            .take(12)
    }
    val exploreSongs = remember(uniqueSongs, trendingSongs, featuredSong) {
        uniqueSongs.filterNot { candidate ->
            (featuredSong != null && candidate.source == featuredSong.source && candidate.id == featuredSong.id) ||
            trendingSongs.any { it.source == candidate.source && it.id == candidate.id }
        }.take(20)
    }
    if (selectedCollection != null) {
        OnlinePlaylistDetailScreen(
            playlist = selectedCollection!!,
            songs = selectedCollectionSongs,
            isLoading = false,
            onBack = { selectedCollection = null },
            onSongSelected = onOnlineSongSelected
        )
        return
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    AuraScreenBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "explore_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Explore",
                                color = textColor,
                                fontSize = 34.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "New sounds, moods and movie music",
                                color = mutedText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        IconButton(onClick = {
                            if (!isLoading) {
                                scope.launch { loadCatalog() }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Explore", tint = textColor)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(textColor.copy(alpha = 0.08f))
                    )
                }
            }

            if (featuredSong != null) {
                item(key = "explore_movie_feature") {
                    val featuredQueue = remember(movieSongs, feedSongs) {
                        if (movieSongs.isNotEmpty()) movieSongs else feedSongs
                    }
                    FeaturedExploreCard(
                        song = featuredSong,
                        onClick = { onOnlineSongSelected(featuredSong, featuredQueue.ifEmpty { listOf(featuredSong) }) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            if (uniqueSongs.isNotEmpty()) {
                item(key = "mood_heading") {
                    SectionHeader(
                        eyebrow = "FIND YOUR VIBE",
                        title = "Music by Mood",
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
                item(key = "mood_carousel") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(moodCollections, key = { it.title }) { mood ->
                            val artworkSong = uniqueSongs[moodCollections.indexOf(mood) % uniqueSongs.size]
                            MoodArtworkCard(
                                mood = mood.title,
                                artworkUrl = artworkSong.artworkUrl,
                                onClick = {
                                    moodJob?.cancel()
                                    moodJob = scope.launch {
                                        activeMood = mood.title
                                        isMoodLoading = true
                                        moodResults = emptyList()
                                        try {
                                            moodResults = repository.search(mood.query, AudioSource.ALL)
                                                .distinctBy { "${it.source.name}_${it.id.ifBlank { it.title }}" }
                                            if (moodResults.isNotEmpty()) {
                                                selectedCollectionSongs = moodResults
                                                selectedCollection = OnlinePlaylist(
                                                    id = "mood_${mood.title.lowercase()}",
                                                    title = "${mood.title} Mix",
                                                    subtitle = "AuraMusic • Music by Mood",
                                                    artworkUrl = moodResults.first().artworkUrl,
                                                    songCount = moodResults.size,
                                                    source = AudioSource.ALL,
                                                    songs = moodResults
                                                )
                                            }
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            moodResults = emptyList()
                                        } finally {
                                            isMoodLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                if (activeMood != null) {
                    item(key = "mood_results_heading") {
                        SectionHeader(
                            eyebrow = "YOUR SELECTED MOOD",
                            title = activeMood ?: "Mood",
                            actionText = "Clear",
                            onActionClick = {
                                moodJob?.cancel()
                                activeMood = null
                                moodResults = emptyList()
                                isMoodLoading = false
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    if (isMoodLoading && moodResults.isEmpty()) {
                        item(key = "mood_loading") {
                            AuraLoadingState(
                                message = "Finding ${activeMood?.lowercase()} music",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        }
                    } else if (moodResults.isEmpty()) {
                        item(key = "mood_empty") {
                            Text(
                                text = "No songs found for this mood",
                                color = mutedText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(moodResults.take(5), key = { "mood_${it.source.name}_${it.id.ifBlank { it.title }}" }) { song ->
                            val isActive = currentPlayingSongId == song.id
                            SongRow(
                                onlineSong = song,
                                isPlaying = isActive && isPlaying,
                                isActive = isActive,
                                onClick = { onOnlineSongSelected(song, moodResults) },
                                onPlayNow = { onOnlineSongSelected(song, moodResults) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            if (trendingSongs.isNotEmpty()) {
                item(key = "fresh_heading") {
                    SectionHeader(
                        eyebrow = "FRESH FINDS",
                        title = "Trending & Fresh",
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
                item(key = "fresh_carousel") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(trendingSongs, key = { "fresh_${it.source.name}_${it.id.ifBlank { it.title }}" }) { song ->
                            ExploreArtworkCard(
                                song = song,
                                onClick = { onOnlineSongSelected(song, trendingSongs) }
                            )
                        }
                    }
                }
            }

            if (movieAlbums.isNotEmpty()) {
                item(key = "movie_albums_heading") {
                    SectionHeader(
                        eyebrow = "SOUNDTRACKS",
                        title = "Movie Albums",
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
                item(key = "movie_albums_carousel") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(movieAlbums, key = { "album_${it.first.lowercase()}" }) { (albumName, albumTracks) ->
                            ExploreAlbumCard(
                                title = albumName,
                                song = albumTracks.first(),
                                onClick = {
                                    selectedCollectionSongs = albumTracks
                                    selectedCollection = OnlinePlaylist(
                                        id = "album_${albumName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
                                        title = albumName,
                                        subtitle = albumTracks.firstOrNull()?.artist.orEmpty(),
                                        artworkUrl = albumTracks.firstOrNull()?.artworkUrl.orEmpty(),
                                        songCount = albumTracks.size,
                                        source = albumTracks.firstOrNull()?.source ?: AudioSource.ALL,
                                        songs = albumTracks
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (exploreSongs.isNotEmpty()) {
                item(key = "more_explore_heading") {
                    SectionHeader(
                        eyebrow = "A LITTLE OF EVERYTHING",
                        title = "More to Explore",
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
                item(key = "more_explore_carousel") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(exploreSongs, key = { "explore_${it.source.name}_${it.id.ifBlank { it.title }}" }) { song ->
                            ExploreArtworkCard(
                                song = song,
                                onClick = { onOnlineSongSelected(song, exploreSongs) }
                            )
                        }
                    }
                }
            }

            if (isLoading && uniqueSongs.isEmpty()) {
                item(key = "explore_loading") {
                    AuraLoadingState(
                        message = "Finding something good",
                        modifier = Modifier.fillMaxWidth().padding(top = 30.dp)
                    )
                }
            } else if (uniqueSongs.isEmpty()) {
                item(key = "explore_empty") {
                    AuraEmptyState(
                        title = if (loadError) "Couldn't load music" else "Nothing to explore yet",
                        message = "Check your connection and refresh to discover online music.",
                        icon = Icons.Default.MusicNote,
                        actionLabel = "Try again",
                        onAction = { scope.launch { loadCatalog() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedExploreCard(
    song: OnlineSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(258.dp)
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
                size = 280,
                modifier = Modifier.align(Alignment.CenterEnd).size(220.dp),
                shape = AuraShapes.Artwork,
                elevation = 10.dp
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent))
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.76f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = if (song.album.isNotBlank()) "MOVIE MUSIC SPOTLIGHT" else "FEATURED MUSIC",
                    color = Color(0xFFFFA1B1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = song.album.ifBlank { song.title },
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp)
                    .liquidGlass(level = GlassLevel.Tinted, shape = CircleShape, tint = AuraCoral),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play featured music", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun MoodArtworkCard(
    mood: String,
    artworkUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(132.dp).clip(AuraShapes.Surface).clickable(onClick = onClick).padding(bottom = 6.dp)
    ) {
        AuraArtwork(
            model = artworkUrl,
            size = 132,
            modifier = Modifier.size(132.dp),
            shape = AuraShapes.Artwork,
            elevation = 5.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mood,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreArtworkCard(
    song: OnlineSong,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(148.dp).clip(AuraShapes.Surface).clickable(onClick = onClick).padding(bottom = 6.dp)
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

@Composable
private fun ExploreAlbumCard(
    title: String,
    song: OnlineSong,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(148.dp).clip(AuraShapes.Surface).clickable(onClick = onClick).padding(bottom = 6.dp)
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
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Album",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
