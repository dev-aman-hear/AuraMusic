package com.aman.auramusic.ui.screen

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.AlbumCard
import com.aman.auramusic.ui.component.ArtistCard
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongOptionsDialog
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.LocalIsDark
import java.util.Calendar

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
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    var selectedSongOptions by remember { mutableStateOf<Song?>(null) }
    var activeContextQueue by remember { mutableStateOf<List<Song>>(emptyList()) }

    val albums = remember(songs) {
        songs.groupBy { it.album }.entries.toList().shuffled().take(12)
    }
    val topArtist = remember(songs) {
        songs.map { it.artist }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Featured Artist"
    }
    val topArtistSongs = remember(songs, topArtist) {
        songs.filter { it.artist == topArtist }
    }
    val topArtistSong = remember(topArtistSongs) { topArtistSongs.firstOrNull() }

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

    if (songs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            AuraEmptyState(
                title = "Welcome to AuraMusic",
                message = "Scan your local library or stream online songs to build your personalized feed.",
                actionLabel = "Scan Device",
                onAction = onRefresh
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // --- 1. TOP HEADER & PROFILE ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "AURAMUSIC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greeting,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(6.dp, CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userInitial,
                                color = Color.White,
                                fontSize = 17.sp,
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

        // --- 2. EDITORIAL FEATURED HERO STATIONS ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    eyebrow = "FEATURED STATIONS",
                    title = "Top Picks"
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CARD 1: Discovery Station
                    item {
                        Card(
                            modifier = Modifier
                                .width(270.dp)
                                .height(310.dp)
                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    val shuffled = songs.shuffled()
                                    if (shuffled.isNotEmpty()) onSongSelected(shuffled.first(), shuffled)
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF831843), Color(0xFF6B21A8), Color(0xFF0F172A))
                                        )
                                    )
                            ) {
                                songs.firstOrNull()?.let { firstSong ->
                                    AuraArtwork(
                                        model = firstSong.artworkUri,
                                        size = 270,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 90.dp),
                                        shape = RoundedCornerShape(0.dp),
                                        elevation = 0.dp
                                    )
                                }

                                // Play button overlay
                                Surface(
                                    onClick = {
                                        val shuffled = songs.shuffled()
                                        if (shuffled.isNotEmpty()) onSongSelected(shuffled.first(), shuffled)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 76.dp)
                                        .size(48.dp)
                                        .shadow(8.dp, CircleShape),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                // Bottom glass panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = "Discovery Station",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Personalized dynamic mix",
                                            color = Color.White.copy(alpha = 0.70f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 2: Artist Radio Station
                    if (topArtistSong != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .width(270.dp)
                                    .height(310.dp)
                                    .shadow(12.dp, RoundedCornerShape(22.dp))
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        if (topArtistSongs.isNotEmpty()) {
                                            onSongSelected(topArtistSongs.first(), topArtistSongs)
                                        }
                                    },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E3A8A), Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        )
                                    ) {
                                        AuraArtwork(
                                            model = topArtistSong.artworkUri,
                                            size = 270,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = 90.dp),
                                            shape = RoundedCornerShape(0.dp),
                                            elevation = 0.dp
                                        )

                                        // Play button overlay
                                        Surface(
                                            onClick = {
                                                if (topArtistSongs.isNotEmpty()) {
                                                    onSongSelected(topArtistSongs.first(), topArtistSongs)
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 16.dp, bottom = 76.dp)
                                                .size(48.dp)
                                                .shadow(8.dp, CircleShape),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }

                                        // Bottom glass panel
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(92.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Column {
                                                Text(
                                                    text = "$topArtist Radio",
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${topArtistSongs.size} tracks from this artist",
                                                    color = Color.White.copy(alpha = 0.70f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // CARD 3: Favorites Station
                    if (favorites.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .width(270.dp)
                                    .height(310.dp)
                                    .shadow(12.dp, RoundedCornerShape(22.dp))
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        onSongSelected(favorites.first(), favorites)
                                    },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF9D174D), Color(0xFFBE185D), Color(0xFF4C1D95))
                                            )
                                        )
                                ) {
                                    AuraArtwork(
                                        model = favorites.first().artworkUri,
                                        size = 270,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 90.dp),
                                        shape = RoundedCornerShape(0.dp),
                                        elevation = 0.dp
                                    )

                                    // Floating Favorite badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(14.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color(0xFFFF2D55),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${favorites.size}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Play button overlay
                                    Surface(
                                        onClick = {
                                            onSongSelected(favorites.first(), favorites)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 16.dp, bottom = 76.dp)
                                            .size(48.dp)
                                            .shadow(8.dp, CircleShape),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }

                                    // Bottom glass panel
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(92.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column {
                                            Text(
                                                text = "Favorites Station",
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${favorites.size} Liked tracks",
                                                color = Color.White.copy(alpha = 0.70f),
                                                fontSize = 12.sp
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

        // --- 3. CONTINUE LISTENING / RECENTLY PLAYED ---
        item {
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

        // --- 4. FEATURED ALBUMS ---
        if (albums.isNotEmpty()) {
            item {
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

        // --- 5. YOUR TOP ARTISTS ---
        if (topArtistsList.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    SectionHeader(
                        eyebrow = "DISCOVER",
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

        // --- 6. FAVORITE TRACKS (COMPACT LIST) ---
        if (favorites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)) {
                    SectionHeader(
                        eyebrow = "QUICK PLAY",
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
    }
}
