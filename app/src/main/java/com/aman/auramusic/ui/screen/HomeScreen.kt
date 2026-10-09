package com.aman.auramusic.ui.screen

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.ui.component.AlbumCard
import com.aman.auramusic.ui.component.ArtistCard
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongOptionsDialog
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.AuraShapes
import com.aman.auramusic.ui.theme.AuraSpacing
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    songs: List<Song>,
    username: String,
    history: List<PlaybackHistoryEntry>,
    favorites: List<Song>,
    favoriteIds: Set<Long>,
    dominantColor: Color,
    onRefresh: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit = { _, _ -> },
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()
    val onlineRepository = remember { OnlineMusicRepository() }

    var selectedSongOptions by remember { mutableStateOf<Song?>(null) }
    var activeContextQueue by remember { mutableStateOf<List<Song>>(emptyList()) }

    var onlineSongs by remember { mutableStateOf(emptyList<OnlineSong>()) }
    var onlinePlaylists by remember { mutableStateOf(emptyList<OnlinePlaylist>()) }
    var isOnlineLoading by remember { mutableStateOf(false) }
    var genreLoadingTitle by remember { mutableStateOf<String?>(null) }

    // --- Local Library Collections ---
    val albums = remember(songs) {
        songs.groupBy { it.album }.entries.toList().shuffled().take(12)
    }
    val topArtist = remember(songs) {
        songs.map { it.artist }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Featured Artist"
    }
    val topArtistSongs = remember(songs, topArtist) {
        songs.filter { it.artist == topArtist }
    }

    val historySongs = remember(history, songs) {
        history.mapNotNull { entry -> songs.find { it.id == entry.songId } }.distinct().take(14)
    }
    val recentSongs = remember(songs, historySongs) {
        if (historySongs.isNotEmpty()) historySongs else songs.take(12)
    }

    val topArtistsList = remember(songs) {
        songs.groupBy { it.artist }
            .entries
            .sortedByDescending { it.value.size }
            .take(10)
    }
    val recentlyAddedSongs = remember(songs) {
        songs.sortedByDescending { it.dateAdded }.take(14)
    }
    val discoveryRows = remember(songs, favorites, historySongs) {
        (songs - favorites.toSet() - historySongs.toSet()).take(24)
    }

    // --- Fetch Online Curated Music (JioSaavn & YouTube) ---
    LaunchedEffect(Unit) {
        isOnlineLoading = true
        try {
            onlineSongs = onlineRepository.getCuratedSongFeed()
                .distinctBy { "${it.source.name}_${it.id}" }
            onlinePlaylists = onlineRepository.getCuratedPlaylists(AudioSource.ALL)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            onlineSongs = emptyList()
            onlinePlaylists = emptyList()
        } finally {
            isOnlineLoading = false
        }
    }

    // 3-Row Stacked Quick Picks (Chunked in groups of 3)
    val quickPicksFeed = remember(onlineSongs, songs) {
        if (onlineSongs.isNotEmpty()) onlineSongs.take(15)
        else emptyList()
    }
    val quickPicksColumns = remember(quickPicksFeed) {
        quickPicksFeed.chunked(3)
    }

    // Fallback local quick picks if offline
    val localQuickPicksColumns = remember(songs) {
        songs.shuffled().take(15).chunked(3)
    }

    // More Online Hits
    val onlineTrending = remember(onlineSongs) {
        if (onlineSongs.size > 15) onlineSongs.drop(15).take(12)
        else onlineSongs.drop(6).take(12)
    }

    val dateHeader = remember {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        sdf.format(Date()).uppercase()
    }

    val greeting = remember(username) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
        val cleanName = username.trim()
        if (cleanName.isNotEmpty()) "$timeGreeting, $cleanName" else timeGreeting
    }
    val userInitial = remember(username) { username.trim().firstOrNull()?.uppercase() ?: "A" }

    if (selectedSongOptions != null) {
        SongOptionsDialog(
            song = selectedSongOptions!!,
            isFavorite = selectedSongOptions!!.id in favoriteIds,
            onDismiss = { selectedSongOptions = null },
            onPlay = {
                onSongSelected(selectedSongOptions!!, activeContextQueue.ifEmpty { songs })
                selectedSongOptions = null
            },
            onToggleFavorite = {
                onFavoriteToggle(selectedSongOptions!!)
                selectedSongOptions = null
            },
            onAddToPlaylist = {
                onAddToPlaylist(selectedSongOptions!!)
                selectedSongOptions = null
            },
            onAddToQueue = {
                onAddToQueue(selectedSongOptions!!)
                selectedSongOptions = null
            }
        )
    }

    AuraScreenBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 124.dp)
        ) {
            // =========================================================================
            // 1. APPLE MUSIC LARGE TITLE HEADER
            // =========================================================================
            item(key = "home_top_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = dateHeader,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Listen Now",
                                fontSize = 34.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Apple-style Profile Button
                        Surface(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(8.dp, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitial,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
                        thickness = 0.8.dp
                    )
                }
            }

            // =========================================================================
            // 2. QUICK PICKS (AT THE VERY TOP) - SIGNATURE 3-ROW STACKED GRID
            // =========================================================================
            if (quickPicksColumns.isNotEmpty() || localQuickPicksColumns.isNotEmpty()) {
                item(key = "home_quick_picks_grid") {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        SectionHeader(
                            eyebrow = if (onlineSongs.isNotEmpty()) "STREAMING PICKS" else "START HERE",
                            title = "Quick Picks",
                            actionText = if (isOnlineLoading) "Loading..." else null
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val columns = if (quickPicksColumns.isNotEmpty()) quickPicksColumns else localQuickPicksColumns

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (quickPicksColumns.isNotEmpty()) {
                                items(quickPicksColumns, key = { col -> "col_online_${col.firstOrNull()?.id}" }) { columnTracks ->
                                    AppleMusicTrackColumn(
                                        onlineTracks = columnTracks,
                                        onTrackClick = { clicked ->
                                            onOnlineSongSelected(clicked, quickPicksFeed)
                                        }
                                    )
                                }
                            } else {
                                items(localQuickPicksColumns, key = { col -> "col_local_${col.firstOrNull()?.id}" }) { columnTracks ->
                                    AppleMusicLocalTrackColumn(
                                        localTracks = columnTracks,
                                        onTrackClick = { clicked ->
                                            onSongSelected(clicked, songs)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. STATIONS FOR YOU (AFTER QUICK PICKS)
            // =========================================================================
            item(key = "home_personal_stations") {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    SectionHeader(
                        eyebrow = "RADIO",
                        title = "Stations For You"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Discovery Station
                        item(key = "station_discovery") {
                            AppleMusicStationCard(
                                title = "Discovery Station",
                                subtitle = "Personalized dynamic flow",
                                icon = Icons.Default.Sensors,
                                gradient = listOf(Color(0xFF7928CA), Color(0xFFFF0080), Color(0xFF1E0836)),
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        val mixed = songs.shuffled()
                                        onSongSelected(mixed.first(), mixed)
                                    } else if (onlineSongs.isNotEmpty()) {
                                        onOnlineSongSelected(onlineSongs.first(), onlineSongs)
                                    }
                                }
                            )
                        }

                        // 2. Favorites Station
                        if (favorites.isNotEmpty()) {
                            item(key = "station_favorites") {
                                AppleMusicStationCard(
                                    title = "Favorites Station",
                                    subtitle = "${favorites.size} loved tracks",
                                    icon = Icons.Default.Favorite,
                                    gradient = listOf(Color(0xFFFF2D55), Color(0xFFB5179E), Color(0xFF270515)),
                                    onClick = {
                                        onSongSelected(favorites.first(), favorites)
                                    }
                                )
                            }
                        }

                        // 3. Chill Station
                        item(key = "station_chill") {
                            AppleMusicStationCard(
                                title = "Chill Station",
                                subtitle = "Low-key beats & calm vibes",
                                icon = Icons.Default.SelfImprovement,
                                gradient = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
                                onClick = {
                                    scope.launch {
                                        genreLoadingTitle = "Chill"
                                        try {
                                            val chillSongs = onlineRepository.search("Lofi Chill Beats", AudioSource.ALL)
                                            if (chillSongs.isNotEmpty()) {
                                                onOnlineSongSelected(chillSongs.first(), chillSongs)
                                            }
                                        } catch (_: Exception) {} finally {
                                            genreLoadingTitle = null
                                        }
                                    }
                                }
                            )
                        }

                        // 4. Energy Station
                        item(key = "station_energy") {
                            AppleMusicStationCard(
                                title = "Energy Station",
                                subtitle = "High-tempo hits to keep moving",
                                icon = Icons.Default.ElectricBolt,
                                gradient = listOf(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFF2E091B)),
                                onClick = {
                                    scope.launch {
                                        genreLoadingTitle = "Energy"
                                        try {
                                            val energySongs = onlineRepository.search("Upbeat Workout Pop", AudioSource.ALL)
                                            if (energySongs.isNotEmpty()) {
                                                onOnlineSongSelected(energySongs.first(), energySongs)
                                            }
                                        } catch (_: Exception) {} finally {
                                            genreLoadingTitle = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 7. BROWSE BY MOOD & CATEGORY (GENRE TILES)
            // =========================================================================
            item(key = "home_genres_section") {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    SectionHeader(
                        eyebrow = "EXPLORE",
                        title = "Browse by Category"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = listOf(
                        "Bollywood & Hindi" to listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                        "Punjabi Pop" to listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
                        "Global Pop" to listOf(Color(0xFF8A2387), Color(0xFFE94057)),
                        "Hip-Hop & Rap" to listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
                        "Lo-Fi & Chill" to listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)),
                        "Indie Vibes" to listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                        "Romance Hits" to listOf(Color(0xFFFC5C7D), Color(0xFF6A82FB)),
                        "Dance & EDM" to listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories, key = { it.first }) { (categoryTitle, gradientColors) ->
                            val isLoadingThis = genreLoadingTitle == categoryTitle
                            AppleMusicGenreCard(
                                title = categoryTitle,
                                gradient = gradientColors,
                                isLoading = isLoadingThis,
                                onClick = {
                                    scope.launch {
                                        genreLoadingTitle = categoryTitle
                                        try {
                                            val categorySongs = onlineRepository.search("$categoryTitle Hits", AudioSource.ALL)
                                                .distinctBy { "${it.source.name}_${it.id}" }
                                            categorySongs.firstOrNull()?.let { first ->
                                                onOnlineSongSelected(first, categorySongs)
                                            }
                                        } catch (_: Exception) {} finally {
                                            genreLoadingTitle = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 8. RECENTLY PLAYED / HEAVY ROTATION
            // =========================================================================
            if (recentSongs.isNotEmpty()) {
                item(key = "home_recently_played") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        SectionHeader(
                            eyebrow = "RECENT",
                            title = "Recently Played",
                            actionText = "See All",
                            onActionClick = {
                                if (songs.isNotEmpty()) onAlbumSelected(songs.first().album)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recentSongs, key = { "recent_${it.id}" }) { song ->
                                AlbumCard(
                                    title = song.title,
                                    subtitle = song.artist,
                                    artworkModel = song.artworkUri,
                                    size = 144.dp,
                                    onClick = { onSongSelected(song, recentSongs) }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 9. TRENDING ONLINE SONGS (SINGLE COMPACT STREAMING ROWS)
            // =========================================================================
            if (onlineTrending.isNotEmpty()) {
                item(key = "home_trending_online_heading") {
                    Column(modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)) {
                        SectionHeader(
                            eyebrow = "ONLINE CATALOG",
                            title = "Trending Songs"
                        )
                    }
                }
                items(onlineTrending, key = { "home_trending_${it.source.name}_${it.id}" }) { song ->
                    SongRow(
                        onlineSong = song,
                        isPlaying = false,
                        onClick = { onOnlineSongSelected(song, onlineSongs) },
                        onPlayNow = { onOnlineSongSelected(song, onlineSongs) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // =========================================================================
            // 10. FEATURED LOCAL ALBUMS
            // =========================================================================
            if (albums.isNotEmpty()) {
                item(key = "home_featured_albums") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        SectionHeader(
                            eyebrow = "COLLECTIONS",
                            title = "Featured Albums"
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums, key = { "alb_${it.key}" }) { (albumName, albumSongs) ->
                                AlbumCard(
                                    title = albumName,
                                    subtitle = "${albumSongs.size} tracks • ${albumSongs.first().artist}",
                                    artworkModel = albumSongs.first().artworkUri,
                                    size = 144.dp,
                                    onClick = { onAlbumSelected(albumName) }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 11. YOUR TOP ARTISTS
            // =========================================================================
            if (topArtistsList.isNotEmpty()) {
                item(key = "home_top_artists") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        SectionHeader(
                            eyebrow = "ARTISTS",
                            title = "Your Top Artists"
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(topArtistsList, key = { "artist_${it.key}" }) { (artistName, artistTracks) ->
                                ArtistCard(
                                    name = artistName,
                                    songCountText = "${artistTracks.size} songs",
                                    artworkModel = artistTracks.firstOrNull()?.artworkUri,
                                    onClick = { onArtistSelected(artistName) }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 12. FAVORITE TRACKS (LOCAL LIKED SONGS)
            // =========================================================================
            if (favorites.isNotEmpty()) {
                item(key = "home_favorite_tracks_heading") {
                    Column(modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)) {
                        SectionHeader(
                            eyebrow = "FAVORITES",
                            title = "Favorite Tracks"
                        )
                    }
                }
                items(favorites.take(6), key = { "fav_row_${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = false,
                        isFavorite = true,
                        onClick = { onSongSelected(song, favorites) },
                        onPlayNow = { onSongSelected(song, favorites) },
                        onToggleFavorite = { onFavoriteToggle(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onLongClick = {
                            activeContextQueue = favorites
                            selectedSongOptions = song
                        }
                    )
                }
            }

            // =========================================================================
            // 13. EMPTY STATE FALLBACK
            // =========================================================================
            if (songs.isEmpty() && onlineSongs.isEmpty() && !isOnlineLoading) {
                item(key = "home_empty_state") {
                    AuraEmptyState(
                        title = "Welcome to AuraMusic",
                        message = "Scan your local library or refresh online music to build your personalized feed.",
                        actionLabel = "Scan Device",
                        onAction = onRefresh
                    )
                }
            }
        }
    }
}

// =========================================================================================
// SUB-COMPONENTS: APPLE MUSIC DESIGN SYSTEM
// =========================================================================================


/**
 * 2. Apple Music Station Card (Radio For You)
 */
@Composable
private fun AppleMusicStationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .height(174.dp)
            .shadow(8.dp, AuraShapes.Surface)
            .clip(AuraShapes.Surface)
            .clickable(onClick = onClick),
        shape = AuraShapes.Surface,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            // Station Icon Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play overlay in top right
            Surface(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(38.dp)
                    .shadow(6.dp, CircleShape),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play station",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Station Labels
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3. Signature Apple Music 3-Row Stacked Track Column (Online Songs)
 */
@Composable
private fun AppleMusicTrackColumn(
    onlineTracks: List<OnlineSong>,
    onTrackClick: (OnlineSong) -> Unit
) {
    Column(
        modifier = Modifier
            .width(314.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        onlineTracks.forEach { song ->
            AppleMusicCompactTrackItem(
                title = song.title,
                artist = song.artist,
                sourceBadge = song.source.displayName,
                artworkModel = song.artworkUrl,
                onClick = { onTrackClick(song) }
            )
        }
    }
}

/**
 * 3b. Signature Apple Music 3-Row Stacked Track Column (Local Songs)
 */
@Composable
private fun AppleMusicLocalTrackColumn(
    localTracks: List<Song>,
    onTrackClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .width(314.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        localTracks.forEach { song ->
            AppleMusicCompactTrackItem(
                title = song.title,
                artist = song.artist,
                sourceBadge = "Local",
                artworkModel = song.artworkUri,
                onClick = { onTrackClick(song) }
            )
        }
    }
}

/**
 * Individual Compact Track Item for the 3-Row Grid
 */
@Composable
private fun AppleMusicCompactTrackItem(
    title: String,
    artist: String,
    sourceBadge: String,
    artworkModel: Any?,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    val itemBg = if (isDark) Color(0xFF1B181E).copy(alpha = 0.85f) else Color(0xFFF3F3F7)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = itemBg,
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            AuraArtwork(
                model = artworkModel,
                size = 46,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artist,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $sourceBadge",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Play Icon Button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}


/**
 * 5. Apple Music Mood & Genre Tile
 */
@Composable
private fun AppleMusicGenreCard(
    title: String,
    gradient: List<Color>,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(148.dp)
            .height(78.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}
