package com.aman.auramusic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.aman.auramusic.ui.component.SongRow

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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

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

            val primaryMatches = songs.filter { song ->
                val title = song.title.lowercase()
                val artist = song.artist.lowercase()
                val album = song.album.lowercase()
                val path = song.filePath.lowercase()

                title.contains(q) || artist.contains(q) || album.contains(q) ||
                catKeywords.any { kw -> title.contains(kw) || artist.contains(kw) || album.contains(kw) || path.contains(kw) }
            }

            // Fallback: if category matched but local metadata doesn't contain exact keywords, return sample queue so screen is rich
            if (primaryMatches.isEmpty() && matchedCategory != null) {
                songs.shuffled().take(8)
            } else {
                primaryMatches
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // --- HEADER & SEARCH BAR ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
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
                            text = "Artists, Songs, Lyrics and more",
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
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
                        focusedBorderColor = Color(0xFFFA2D48),
                        unfocusedBorderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.15f),
                        focusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                        unfocusedContainerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                    )
                )
            }
        }

        // --- QUERY IS BLANK: DISPLAY 2-COLUMN CATEGORY GRID ---
        if (query.isBlank()) {
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
            // --- SEARCH RESULTS MODE ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (matchedCategory != null) "${matchedCategory.title} Songs" else "Results for '$query'",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    TextButton(onClick = { onQueryChange("") }) {
                        Text(text = "Clear", color = Color(0xFFFA2D48))
                    }
                }
            }

            if (filteredSongs.isEmpty() && matchedAlbums.isEmpty() && matchedArtists.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs found matching '$query'",
                            fontSize = 15.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Matching Songs
                if (filteredSongs.isNotEmpty()) {
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

                // Matching Albums
                if (matchedAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    items(matchedAlbums, key = { "search_album_$it" }) { albumName ->
                        val albumSongs = songs.filter { it.album == albumName }
                        CollectionRow(
                            title = albumName,
                            subtitle = "Album • ${albumSongs.firstOrNull()?.artist ?: "Artist"}",
                            song = albumSongs.first(),
                            onClick = { onAlbumSelected(albumName) }
                        )
                    }
                }

                // Matching Artists
                if (matchedArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    items(matchedArtists, key = { "search_artist_$it" }) { artistName ->
                        val artistSongs = songs.filter { it.artist == artistName }
                        CollectionRow(
                            title = artistName,
                            subtitle = "${artistSongs.size} songs",
                            song = artistSongs.first(),
                            onClick = { onArtistSelected(artistName) }
                        )
                    }
                }
            }
        }
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
