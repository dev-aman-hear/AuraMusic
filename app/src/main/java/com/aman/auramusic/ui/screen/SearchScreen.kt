package com.aman.auramusic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.AuraCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SearchCategory(
    val title: String,
    val keywords: List<String>,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    currentSongId: Long?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onlinePlaybackManager: OnlinePlaybackManager? = null,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val repository = remember { OnlineMusicRepository() }

    var selectedSource by remember { mutableStateOf(AudioSource.ALL) }
    var onlineResults by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var trendingOnlineSongs by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val onlinePlaybackState = onlinePlaybackManager?.playbackState?.collectAsState()

    val categories = remember {
        listOf(
            SearchCategory("Bollywood", listOf("bollywood", "hindi", "arijit", "neha", "badshah", "jubin", "shreya", "pritam", "t-series", "tseries", "filmi", "singh", "kapoor", "khan"), listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))),
            SearchCategory("Punjabi", listOf("punjabi", "sidhu", "diljit", "karan", "aujla", "ap dhillon", "bhangra", "desi", "jatt", "honey singh", "randhawa", "b praak", "jaani", "hardy"), listOf(Color(0xFFEA580C), Color(0xFFC2410C))),
            SearchCategory("Pop", listOf("pop", "swift", "sheeran", "bieber", "lipa", "ariana", "weekend", "drake", "bruno", "gomez", "billie", "olivia", "perry", "styles"), listOf(Color(0xFFDB2777), Color(0xFF9D174D))),
            SearchCategory("Charts", listOf("chart", "top", "hit", "hot", "trending", "best", "popular", "number 1"), listOf(Color(0xFFD97706), Color(0xFFB45309))),
            SearchCategory("Hip-Hop", listOf("hip-hop", "hip hop", "rap", "rapper", "divine", "emiway", "raftaar", "stan", "krsna", "drake", "eminem", "kanye", "travis"), listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
            SearchCategory("Radio", listOf("radio", "fm", "mix", "station", "wave"), listOf(Color(0xFFDC2626), Color(0xFF991B1B))),
            SearchCategory("Concerts", listOf("concert", "tour", "live", "stage", "performance"), listOf(Color(0xFF991B1B), Color(0xFF450A0A))),
            SearchCategory("Live Music", listOf("live", "unplugged", "acoustic", "session"), listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))),
            SearchCategory("Tamil", listOf("tamil", "kollywood", "anirudh", "rahman", "yuvan", "harris", "prakash", "ilayaraja", "vijay"), listOf(Color(0xFF4F46E5), Color(0xFF3730A3))),
            SearchCategory("Telugu", listOf("telugu", "tollywood", "dsp", "thaman", "keeravani", "allu", "mahesh", "prabhas"), listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
            SearchCategory("Rock", listOf("rock", "metal", "band", "guitar", "queen", "nirvana", "metallica", "coldplay", "dragons"), listOf(Color(0xFFD97706), Color(0xFF9A3412))),
            SearchCategory("Dance", listOf("dance", "edm", "party", "club", "dj", "remix", "house", "garrix", "guetta", "avicii"), listOf(Color(0xFF10B981), Color(0xFF047857))),
            SearchCategory("Love", listOf("love", "romantic", "romance", "dil", "pyar", "pyaar", "ishq", "mohabbat", "heart", "humsafar", "jaan"), listOf(Color(0xFFF43F5E), Color(0xFFE11D48))),
            SearchCategory("Chill", listOf("chill", "lofi", "soft", "acoustic", "relax", "peace", "sleep", "night", "rain", "calm"), listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
            SearchCategory("Fitness", listOf("fitness", "workout", "gym", "beast", "energy", "power", "pump", "motivation", "strong", "cardio"), listOf(Color(0xFF22C55E), Color(0xFF15803D))),
            SearchCategory("Focus", listOf("focus", "study", "work", "brain", "concentration", "instrumental", "zen"), listOf(Color(0xFF10B981), Color(0xFF047857))),
            SearchCategory("Malayalam", listOf("malayalam", "mollywood", "sushin", "shaan", "gopi", "dulquer", "fahadh"), listOf(Color(0xFF6B21A8), Color(0xFF581C87))),
            SearchCategory("Classical", listOf("classical", "sufi", "ghazal", "qawwali", "raag", "classic", "retro", "90s", "80s", "kishore", "lata", "rafi"), listOf(Color(0xFF1E3A8A), Color(0xFF172554)))
        )
    }

    val matchedCategory = remember(categories, query) {
        categories.find { it.title.equals(query, ignoreCase = true) }
    }

    val filteredSongs = remember(songs, query, matchedCategory) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            val catKeywords = matchedCategory?.keywords ?: emptyList()

            songs.filter { song ->
                val title = song.title.lowercase()
                val artist = song.artist.lowercase()
                val album = song.album.lowercase()
                val path = song.filePath.lowercase()

                title.contains(q) || artist.contains(q) || album.contains(q) ||
                catKeywords.any { kw -> title.contains(kw) || artist.contains(kw) || album.contains(kw) || path.contains(kw) }
            }
        }
    }

    val matchedAlbums = remember(songs, query) {
        if (query.isBlank()) emptyList()
        else songs.map { it.album }.distinct().filter { it.contains(query, ignoreCase = true) }
    }

    val matchedArtists = remember(songs, query) {
        if (query.isBlank()) emptyList()
        else songs.map { it.artist }.distinct().filter { it.contains(query, ignoreCase = true) }
    }

    // Unified Online Search bound directly to query and source filter
    LaunchedEffect(query, selectedSource) {
        if (query.isNotBlank()) {
            isOnlineLoading = true
            delay(250) // slight debounce
            try {
                onlineResults = repository.search(query, selectedSource)
            } catch (e: Exception) {
                onlineResults = emptyList()
            } finally {
                isOnlineLoading = false
            }
        } else {
            onlineResults = emptyList()
            if (trendingOnlineSongs.isEmpty()) {
                isOnlineLoading = true
                try {
                    trendingOnlineSongs = repository.getTrending(selectedSource)
                } catch (e: Exception) {
                    trendingOnlineSongs = emptyList()
                } finally {
                    isOnlineLoading = false
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // --- HEADER & UNIFIED SEARCH BAR ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Search",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums...",
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF)
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.15f),
                        focusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                        unfocusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Catalog Filter Chips: All, Top Results, Global
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioSource.values().filter { it != AudioSource.SPOTIFY }.forEach { source ->
                        val isSelected = selectedSource == source
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AuraCyan else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)))
                                .border(1.dp, if (isSelected) AuraCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { selectedSource = source }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (source) {
                                    AudioSource.ALL -> "All"
                                    AudioSource.JIOSAAVN -> "Top Results"
                                    AudioSource.YOUTUBE -> "Global"
                                    else -> "All"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)
                            )
                        }
                    }
                }
            }
        }

        // --- QUERY IS BLANK: TRENDING ONLINE FEED & CATEGORY GRID ---
        if (query.isBlank()) {

            // Top Trending Hits
            if (trendingOnlineSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trending Now",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        if (isOnlineLoading) {
                            CircularProgressIndicator(
                                color = AuraCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                items(trendingOnlineSongs.take(6), key = { "trending_search_${it.id}" }) { song ->
                    val currentPlayingId = onlinePlaybackState?.value?.currentSong?.id
                    val isPlaying = currentPlayingId == song.id && (onlinePlaybackState?.value?.isPlaying == true)
                    SongRow(
                        onlineSong = song,
                        isPlaying = isPlaying,
                        onClick = { onOnlineSongSelected(song, trendingOnlineSongs) },
                        onPlayNow = { onOnlineSongSelected(song, trendingOnlineSongs) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Explore Categories Title
            item {
                Text(
                    text = "Explore Categories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            val rows = categories.chunked(2)
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { cat ->
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryCard(
                                category = cat,
                                onClick = { onQueryChange(cat.title) }
                            )
                        }
                    }
                    if (row.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // --- SEARCH RESULTS MODE (UNIFIED ONLINE + LOCAL) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results for '$query'",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOnlineLoading) {
                            CircularProgressIndicator(
                                color = AuraCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        TextButton(onClick = { onQueryChange("") }) {
                            Text(text = "Clear", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 1. ONLINE STREAMING RESULTS (JioSaavn 320k & YouTube)
            if (onlineResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Online Streaming",
                        eyebrow = "${onlineResults.size} tracks found"
                    )
                }

                items(onlineResults, key = { "search_online_${it.id}" }) { song ->
                    val currentPlayingId = onlinePlaybackState?.value?.currentSong?.id
                    val isPlaying = currentPlayingId == song.id && (onlinePlaybackState?.value?.isPlaying == true)
                    SongRow(
                        onlineSong = song,
                        isPlaying = isPlaying,
                        onClick = { onOnlineSongSelected(song, onlineResults) },
                        onPlayNow = { onOnlineSongSelected(song, onlineResults) }
                    )
                }
            }

            // 2. LOCAL LIBRARY RESULTS
            if (filteredSongs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        title = "Local Library",
                        eyebrow = "${filteredSongs.size} tracks found"
                    )
                }

                items(filteredSongs, key = { "search_song_${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        isFavorite = song.id in favoriteIds,
                        onPlayNow = { onSongSelected(song, filteredSongs) },
                        onToggleFavorite = { onFavoriteToggle(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onClick = { onSongSelected(song, filteredSongs) }
                    )
                }
            }

            // 3. MATCHING LOCAL ALBUMS
            if (matchedAlbums.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        title = "Albums",
                        eyebrow = "${matchedAlbums.size} matching"
                    )
                }
                items(matchedAlbums, key = { "search_album_$it" }) { albumName ->
                    val albumSongs = songs.filter { it.album == albumName }
                    SearchMediaRow(
                        title = albumName,
                        subtitle = "Album • ${albumSongs.firstOrNull()?.artist ?: "Unknown Artist"}",
                        artworkModel = albumSongs.firstOrNull(),
                        isCircular = false,
                        onClick = { onAlbumSelected(albumName) }
                    )
                }
            }

            // 4. MATCHING LOCAL ARTISTS
            if (matchedArtists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        title = "Artists",
                        eyebrow = "${matchedArtists.size} matching"
                    )
                }
                items(matchedArtists, key = { "search_artist_$it" }) { artistName ->
                    val artistSongs = songs.filter { it.artist == artistName }
                    SearchMediaRow(
                        title = artistName,
                        subtitle = "${artistSongs.size} songs",
                        artworkModel = artistSongs.firstOrNull(),
                        isCircular = true,
                        onClick = { onArtistSelected(artistName) }
                    )
                }
            }

            // EMPTY STATE
            if (onlineResults.isEmpty() && filteredSongs.isEmpty() && !isOnlineLoading) {
                item {
                    AuraEmptyState(
                        title = "No Tracks Found",
                        message = "We couldn't find any tracks matching \"$query\".",
                        icon = Icons.Default.MusicNote,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchMediaRow(
    title: String,
    subtitle: String,
    artworkModel: Any?,
    isCircular: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraArtwork(
            model = artworkModel,
            size = 50,
            shape = if (isCircular) CircleShape else RoundedCornerShape(12.dp),
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CategoryCard(
    category: SearchCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(category.gradientColors))
                .padding(14.dp)
        ) {
            Text(
                text = category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
