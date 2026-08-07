package com.aman.auramusic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aman.auramusic.R
import com.aman.auramusic.data.model.OnlinePlaylist
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.LocalIsDark
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    songs: List<Song>,
    username: String,
    history: List<PlaybackHistoryEntry>,
    favorites: List<Song>,
    favoriteIds: Set<Long>,
    matchedOnlinePlaylists: List<OnlinePlaylist>,
    dominantColor: Color,
    onOnlinePlaylistClick: (OnlinePlaylist) -> Unit,
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
    var selectedSongOptions by remember { mutableStateOf<Song?>(null) }
    var mixQueue by remember { mutableStateOf<List<Song>>(emptyList()) }
    val albums = remember(songs) { songs.groupBy { it.album }.entries.toList().shuffled().take(10) }
    val topArtist = remember(songs) { songs.map { it.artist }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Featured Artist" }
    val topArtistSong = remember(songs, topArtist) { songs.find { it.artist == topArtist } }

    if (selectedSongOptions != null) {
        SongOptionsDialog(
            song = selectedSongOptions!!,
            isFavorite = selectedSongOptions!!.id in favoriteIds,
            onDismiss = { selectedSongOptions = null },
            onPlay = {
                onSongSelected(selectedSongOptions!!, mixQueue)
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

    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // --- APPLE MUSIC HEADER ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Home",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black,
                        letterSpacing = (-0.5).sp
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile & Settings",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Divider(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.12f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // --- TOP PICKS SECTION ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FEATURED STATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF2D55),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Picks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            text = "Featuring $topArtist",
                            fontSize = 13.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CARD 1: Discovery Station
                    item {
                        Card(
                            modifier = Modifier
                                .width(260.dp)
                                .height(310.dp)
                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        val shuffled = songs.shuffled()
                                        onSongSelected(shuffled.first(), shuffled)
                                    }
                                },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF3B0764),
                                                Color(0xFF1E3A8A),
                                                Color(0xFF0284C7),
                                                Color(0xFF0D9488),
                                                Color(0xFF6D28D9)
                                            )
                                        )
                                    )
                            ) {
                                // Top Branding Badge
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Discovery",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Center Artwork / Graphic Icon
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.Center)
                                )

                                // Floating Play Button
                                Surface(
                                    onClick = {
                                        if (songs.isNotEmpty()) {
                                            val shuffled = songs.shuffled()
                                            onSongSelected(shuffled.first(), shuffled)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 80.dp)
                                        .size(46.dp)
                                        .shadow(8.dp, CircleShape),
                                    shape = CircleShape,
                                    color = Color(0xFFFF2D55)
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

                                // Bottom Glass Panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF0F172A).copy(alpha = 0.65f),
                                                    Color(0xFF020617).copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = "Discovery Station",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Personalized mix based on your taste",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 2: Artist Station
                    item {
                        Card(
                            modifier = Modifier
                                .width(260.dp)
                                .height(310.dp)
                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    val artistSongs = songs.filter { it.artist == topArtist }
                                    if (artistSongs.isNotEmpty()) {
                                        onSongSelected(artistSongs.first(), artistSongs)
                                    } else if (songs.isNotEmpty()) {
                                        onSongSelected(songs.first(), songs)
                                    }
                                },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF334155),
                                                Color(0xFF1E293B),
                                                Color(0xFF0F172A)
                                            )
                                        )
                                    )
                            ) {
                                if (topArtistSong != null) {
                                    SongArtwork(
                                        song = topArtistSong,
                                        size = 260,
                                        shape = RoundedCornerShape(0.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 95.dp)
                                    )
                                }

                                // Circular Artist Badge Overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .size(56.dp)
                                        .shadow(6.dp, CircleShape)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                                        .background(Color.DarkGray)
                                ) {
                                    SongArtwork(
                                        song = topArtistSong,
                                        size = 56,
                                        shape = CircleShape,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Floating Play Button
                                Surface(
                                    onClick = {
                                        val artistSongs = songs.filter { it.artist == topArtist }
                                        if (artistSongs.isNotEmpty()) {
                                            onSongSelected(artistSongs.first(), artistSongs)
                                        } else if (songs.isNotEmpty()) {
                                            onSongSelected(songs.first(), songs)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 80.dp)
                                        .size(46.dp)
                                        .shadow(8.dp, CircleShape),
                                    shape = CircleShape,
                                    color = Color(0xFFFF2D55)
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

                                // Bottom Glass Panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Color(0xFF0F172A).copy(alpha = 0.92f)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = "$topArtist & Similar",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Artist Radio Station",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 3: Favorites Station
                    item {
                        Card(
                            modifier = Modifier
                                .width(260.dp)
                                .height(310.dp)
                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (favorites.isNotEmpty()) {
                                        onSongSelected(favorites.first(), favorites)
                                    } else if (songs.isNotEmpty()) {
                                        onSongSelected(songs.first(), songs)
                                    }
                                },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF831843),
                                                Color(0xFFBE185D),
                                                Color(0xFF6D28D9),
                                                Color(0xFF1E1B4B)
                                            )
                                        )
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF2D55),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Favorites",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.Center)
                                )

                                // Floating Play Button
                                Surface(
                                    onClick = {
                                        if (favorites.isNotEmpty()) {
                                            onSongSelected(favorites.first(), favorites)
                                        } else if (songs.isNotEmpty()) {
                                            onSongSelected(songs.first(), songs)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 80.dp)
                                        .size(46.dp)
                                        .shadow(8.dp, CircleShape),
                                    shape = CircleShape,
                                    color = Color(0xFFFF2D55)
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

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Color(0xFF1E1B4B).copy(alpha = 0.92f)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = "Favorites Station",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${favorites.size} Liked Tracks",
                                            color = Color.White.copy(alpha = 0.75f),
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

        // --- RECENTLY PLAYED > SECTION ---
        item {
            val historySongs = remember(history, songs) {
                history.mapNotNull { entry -> songs.find { it.id == entry.songId } }.distinct().take(12)
            }
            val displayList = if (historySongs.isNotEmpty()) historySongs else songs.take(10)

            Column(modifier = Modifier.padding(top = 28.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable {
                            if (songs.isNotEmpty()) {
                                onAlbumSelected(songs.first().album)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See All",
                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayList, key = { it.id }) { song ->
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .combinedClickable(
                                    onClick = { onSongSelected(song, displayList) },
                                    onLongClick = {
                                        mixQueue = displayList
                                        selectedSongOptions = song
                                    }
                                )
                        ) {
                            SongArtwork(
                                song = song,
                                size = 140,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(140.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- FEATURED ALBUMS SECTION ---
        if (albums.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Featured Albums",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(albums, key = { it.key }) { entry ->
                            val albumName = entry.key
                            val firstSong = entry.value.first()
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onAlbumSelected(albumName) }
                            ) {
                                SongArtwork(
                                    song = firstSong,
                                    size = 140,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .size(140.dp)
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = albumName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = firstSong.artist,
                                    fontSize = 12.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ONLINE PLAYLISTS SECTION ---
        if (matchedOnlinePlaylists.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playlists for You",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(matchedOnlinePlaylists, key = { it.id }) { playlist ->
                            OnlinePlaylistCompactCard(
                                playlist = playlist,
                                onClick = { onOnlinePlaylistClick(playlist) }
                            )
                        }
                    }
                }
            }
        }

        // --- FAVORITES SECTION ---
        if (favorites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorite Tracks",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(favorites.take(5), key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isPlaying = false,
                    isFavorite = true,
                    onPlayNow = { onSongSelected(song, favorites) },
                    onToggleFavorite = { onFavoriteToggle(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onClick = { onSongSelected(song, favorites) },
                    onLongClick = {
                        mixQueue = favorites
                        selectedSongOptions = song
                    }
                )
            }
        }
    }
}

@Composable
fun OnlinePlaylistCompactCard(
    playlist: OnlinePlaylist,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = playlist.cover,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.author,
            fontSize = 12.sp,
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongOptionsDialog(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SongArtwork(song = song, size = 100, shape = RoundedCornerShape(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Now", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFFA2D48) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onAddToPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Playlist", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onAddToQueue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Queue", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
