package com.aman.auramusic.ui.screen

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.AuraEmptyState
import com.aman.auramusic.ui.component.SectionHeader
import com.aman.auramusic.ui.component.SongRow
import com.aman.auramusic.ui.theme.AuraCyan
import com.aman.auramusic.ui.theme.AuraGlass
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.frostedGlass
import kotlinx.coroutines.delay

/**
 * Organic Mesh Pattern styles matching each specific card in the Dribbble search reference design.
 */
enum class MeshPattern {
    POP,          // Dark canopy top, radiant lime/emerald glow in center-lower
    HIP_HOP,      // Deep crimson-dark top, fiery neon amber/orange explosion lower-center
    ELECTRONIC,   // Candy coral-pink top, neon magenta bottom, dark contrasting void in middle
    ALTERNATIVE,  // Ethereal ice-cyan top-left & bottom, orchid lavender right, diagonal teal ribbon
    ROCK,         // Horizontal electric cyan-blue beam across midnight navy abyss
    RNB,          // Acid chartreuse lime top-left bleeding into royal ultraviolet bottom-right
    BOLLYWOOD,    // Saffron gold warm left bleeding into royal ruby magenta right
    HINDI,        // Warm amber-coral center with vibrant fuchsia aura
    PUNJABI,      // Volcanic amber-flame lower-center
    INDIE,        // Electric neon violet upper-right, soft pastel rose lower-left
    CLASSICAL,    // Midnight sapphire with celestial starlight indigo
    CHILL         // Bioluminescent marine aqua & deep oceanic teal pool
}

data class SearchCategory(
    val title: String,
    val keywords: List<String>,
    val pattern: MeshPattern,
    val glowColor: Color
)

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    ONLINE("Online"),
    LOCAL("Local")
}

private const val SEARCH_PREFS_NAME = "auramusic_search_history"
private const val KEY_RECENT_QUERIES = "recent_queries"

private fun getRecentSearches(context: Context): List<String> {
    val prefs = context.getSharedPreferences(SEARCH_PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_RECENT_QUERIES, null) ?: return emptyList()
    return raw.split("|||").filter { it.isNotBlank() }
}

private fun saveRecentSearch(context: Context, query: String) {
    val trimmed = query.trim()
    if (trimmed.length < 2) return
    val current = getRecentSearches(context).toMutableList()
    current.remove(trimmed)
    current.add(0, trimmed)
    val limited = current.take(8)
    context.getSharedPreferences(SEARCH_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_RECENT_QUERIES, limited.joinToString("|||"))
        .apply()
}

private fun removeRecentSearch(context: Context, query: String) {
    val current = getRecentSearches(context).toMutableList()
    current.remove(query.trim())
    context.getSharedPreferences(SEARCH_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_RECENT_QUERIES, current.joinToString("|||"))
        .apply()
}

private fun clearRecentSearches(context: Context) {
    context.getSharedPreferences(SEARCH_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_RECENT_QUERIES)
        .apply()
}

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
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { OnlineMusicRepository() }

    // Adaptive Theme Tokens (Dynamic Light & Dark Mode)
    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF8F9FA)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF9E9EA7) else Color(0xFF64748B)
    val textPlaceholder = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val searchContainerBg = if (isDark) Color(0xFF141418) else Color.White
    val searchBorderColor = if (isDark) Color(0xFF27272E) else Color(0xFFE2E8F0)
    val chipBg = if (isDark) Color(0xFF181820) else Color.White
    val chipBorder = if (isDark) Color(0xFF2C2C36) else Color(0xFFE2E8F0)
    val avatarBg = if (isDark) Color(0xFF18181C) else Color(0xFFF1F5F9)
    val avatarBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)
    val avatarIconTint = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)

    // Intercept back button when search query is active to smoothly return to category discovery
    BackHandler(enabled = query.isNotEmpty()) {
        onQueryChange("")
        focusManager.clearFocus()
    }

    // Recent search history state
    var recentSearches by remember { mutableStateOf(getRecentSearches(context)) }

    // Active result filter
    var activeFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var selectedAudioSource by remember { mutableStateOf(AudioSource.ALL) }

    // Online search state
    var onlineResults by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val onlinePlaybackState = onlinePlaybackManager?.playbackState?.collectAsState()

    // 12 Curated Atmospheric Glowing Mesh Categories matching the Dribbble Reference
    val categories = remember {
        listOf(
            SearchCategory(
                title = "Pop",
                keywords = listOf("pop", "swift", "sheeran", "bieber", "lipa", "ariana", "weekend", "drake", "bruno", "gomez", "billie", "olivia", "perry", "styles", "puth"),
                pattern = MeshPattern.POP,
                glowColor = Color(0xFF22C55E)
            ),
            SearchCategory(
                title = "Hip Hop",
                keywords = listOf("hip-hop", "hip hop", "rap", "rapper", "divine", "emiway", "raftaar", "stan", "krsna", "drake", "eminem", "kanye", "travis"),
                pattern = MeshPattern.HIP_HOP,
                glowColor = Color(0xFFFF6D00)
            ),
            SearchCategory(
                title = "Electronic",
                keywords = listOf("electronic", "dance", "edm", "party", "club", "dj", "remix", "house", "garrix", "guetta", "avicii", "marshmello"),
                pattern = MeshPattern.ELECTRONIC,
                glowColor = Color(0xFFFF2A85)
            ),
            SearchCategory(
                title = "Alternative",
                keywords = listOf("alternative", "indie", "alt", "arctic monkeys", "strokes", "radiohead", "cage the elephant", "tame impala", "twenty one pilots"),
                pattern = MeshPattern.ALTERNATIVE,
                glowColor = Color(0xFF06B6D4)
            ),
            SearchCategory(
                title = "Rock",
                keywords = listOf("rock", "metal", "band", "guitar", "queen", "nirvana", "metallica", "coldplay", "dragons", "linkin park", "ac/dc"),
                pattern = MeshPattern.ROCK,
                glowColor = Color(0xFF2563EB)
            ),
            SearchCategory(
                title = "R&B",
                keywords = listOf("r&b", "rnb", "soul", "sza", "frank ocean", "brent faiyaz", "khalid", "daniel caesar", "summer walker", "usher"),
                pattern = MeshPattern.RNB,
                glowColor = Color(0xFF84CC16)
            ),
            SearchCategory(
                title = "Bollywood",
                keywords = listOf("bollywood", "hindi", "arijit", "neha", "badshah", "jubin", "shreya", "pritam", "t-series", "filmi", "singh", "kapoor", "khan"),
                pattern = MeshPattern.BOLLYWOOD,
                glowColor = Color(0xFFF59E0B)
            ),
            SearchCategory(
                title = "Hindi Music",
                keywords = listOf("hindi", "arijit", "sonu nigam", "shreya ghoshal", "kk", "atif", "mohit chauhan", "ar rahman", "pritam", "mithoon"),
                pattern = MeshPattern.HINDI,
                glowColor = Color(0xFFFB923C)
            ),
            SearchCategory(
                title = "Punjabi",
                keywords = listOf("punjabi", "sidhu", "diljit", "karan", "aujla", "ap dhillon", "bhangra", "desi", "jatt", "honey singh", "randhawa", "b praak"),
                pattern = MeshPattern.PUNJABI,
                glowColor = Color(0xFFFF5722)
            ),
            SearchCategory(
                title = "Indie",
                keywords = listOf("indie", "prateek kuhad", "anuv jain", "zaeden", "ritviz", "when chai met toast", "the local train", "jasleen royal"),
                pattern = MeshPattern.INDIE,
                glowColor = Color(0xFFA855F7)
            ),
            SearchCategory(
                title = "Classical",
                keywords = listOf("classical", "sufi", "ghazal", "qawwali", "raag", "classic", "retro", "90s", "80s", "kishore", "lata", "rafi", "jagjit"),
                pattern = MeshPattern.CLASSICAL,
                glowColor = Color(0xFF6366F1)
            ),
            SearchCategory(
                title = "Chill",
                keywords = listOf("chill", "lofi", "soft", "acoustic", "relax", "peace", "sleep", "night", "rain", "calm", "study", "ambient"),
                pattern = MeshPattern.CHILL,
                glowColor = Color(0xFF2DD4BF)
            )
        )
    }

    val matchedCategory = remember(categories, query) {
        categories.find { it.title.equals(query.trim(), ignoreCase = true) }
    }

    // Instant local library matching (runs synchronously in <5ms)
    val filteredLocalSongs = remember(songs, query, matchedCategory) {
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
        else songs.map { it.album }.distinct().filter { it.contains(query.trim(), ignoreCase = true) }
    }

    val matchedArtists = remember(songs, query) {
        if (query.isBlank()) emptyList()
        else songs.map { it.artist }.distinct().filter { it.contains(query.trim(), ignoreCase = true) }
    }

    // Standout Top Match Candidate (Artist or Song)
    val topArtistMatch = remember(matchedArtists, query) {
        if (query.isBlank()) null
        else matchedArtists.firstOrNull { it.equals(query.trim(), ignoreCase = true) } ?: matchedArtists.firstOrNull()
    }

    // Debounced Online Search (YouTube + JioSaavn)
    LaunchedEffect(query, selectedAudioSource) {
        if (query.isNotBlank()) {
            isOnlineLoading = true
            delay(300) // Debounce rapid keystrokes
            try {
                onlineResults = repository.search(query.trim(), selectedAudioSource)
            } catch (_: Exception) {
                onlineResults = emptyList()
            } finally {
                isOnlineLoading = false
            }
        } else {
            onlineResults = emptyList()
            isOnlineLoading = false
        }
    }

    // Base background: Adaptive Dark / Light Canvas with ambient color blooms
    AuraScreenBackground(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // ==========================================
            // HEADER & SEARCH FIELD (Adaptive Glassmorphism)
            // ==========================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Top App Header: Centered Search title + right profile avatar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Back Button if in search mode
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textPrimary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        // Centered Clean Title matching Dribbble Reference
                        Text(
                            text = "Search",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )

                        // Top-right Profile Avatar Icon with Glass Touch
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(avatarBg)
                                .border(1.dp, avatarBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = avatarIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleek Rounded Search Bar with Dynamic Glass Surface
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = "Search songs, artists, albums",
                                color = textPlaceholder,
                                fontSize = 14.5.sp,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (query.isNotEmpty()) AuraCyan else textPlaceholder,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    onQueryChange("")
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = textPrimary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .frostedGlass(
                                shape = RoundedCornerShape(26.dp),
                                isDark = isDark,
                                elevation = if (isDark) 8.dp else 4.dp,
                                borderWidth = 1.1.dp,
                                sheenAlpha = if (isDark) 0.16f else 0.35f
                            ),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                if (query.isNotBlank()) {
                                    saveRecentSearch(context, query)
                                    recentSearches = getRecentSearches(context)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = AuraCyan,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    // Recent Searches Section when query is blank & history exists
                    if (query.isBlank() && recentSearches.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "Clear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AuraCyan,
                                modifier = Modifier
                                    .clickable {
                                        clearRecentSearches(context)
                                        recentSearches = emptyList()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal recent search pills with frosted glass styling
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { hist ->
                                Box(
                                    modifier = Modifier
                                        .frostedGlass(
                                            shape = RoundedCornerShape(16.dp),
                                            isDark = isDark,
                                            elevation = 2.dp,
                                            borderWidth = 1.dp
                                        )
                                        .clickable {
                                            onQueryChange(hist)
                                            saveRecentSearch(context, hist)
                                            recentSearches = getRecentSearches(context)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = hist,
                                            fontSize = 12.5.sp,
                                            color = textPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = textSecondary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    removeRecentSearch(context, hist)
                                                    recentSearches = getRecentSearches(context)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Result Filter Chips when query is active with frosted glass styling
                    if (query.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchFilter.values().forEach { filter ->
                                val isSelected = activeFilter == filter
                                val activeTextColor = if (isDark) Color.Black else Color.White
                                val inactiveTextColor = if (isDark) Color.White.copy(0.85f) else Color(0xFF334155)

                                Box(
                                    modifier = Modifier
                                        .frostedGlass(
                                            shape = RoundedCornerShape(20.dp),
                                            isDark = isDark,
                                            tint = if (isSelected) (if (isDark) Color.White else Color(0xFF0F172A)) else null,
                                            elevation = if (isSelected) 4.dp else 1.dp,
                                            borderWidth = 1.dp,
                                            sheenAlpha = if (isSelected) 0.35f else 0.12f
                                        )
                                        .clickable { activeFilter = filter }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeTextColor else inactiveTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // DISCOVERY MODE: 2-COLUMN GLOWING MESH GLASS CARDS (Dribbble Reference)
            // ==========================================
            if (query.isBlank()) {
                val categoryRows = categories.chunked(2)
                items(categoryRows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { cat ->
                            Box(modifier = Modifier.weight(1f)) {
                                AtmosphericGlassCategoryCard(
                                    category = cat,
                                    onClick = {
                                        onQueryChange(cat.title)
                                        saveRecentSearch(context, cat.title)
                                        recentSearches = getRecentSearches(context)
                                    }
                                )
                            }
                        }
                        if (row.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // ==========================================
                // SEARCH RESULTS MODE (UNIFIED LOCAL + ONLINE)
                // ==========================================

                // Search Result Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Results for \"$query\"",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
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

                // 1. TOP RESULT CARD (Standout Artist or Song highlight)
                if (activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.ARTISTS) {
                    if (topArtistMatch != null) {
                        item {
                            val artistSongs = songs.filter { it.artist == topArtistMatch }
                            TopArtistResultCard(
                                artistName = topArtistMatch,
                                songCount = artistSongs.size,
                                artworkModel = artistSongs.firstOrNull(),
                                onClick = { onArtistSelected(topArtistMatch) }
                            )
                        }
                    }
                }

                // 2. LOCAL SONGS
                if (activeFilter in listOf(SearchFilter.ALL, SearchFilter.SONGS, SearchFilter.LOCAL)) {
                    if (filteredLocalSongs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Local Library Songs",
                                eyebrow = "${filteredLocalSongs.size} tracks available offline"
                            )
                        }

                        items(filteredLocalSongs, key = { "local_${it.id}" }) { song ->
                            SongRow(
                                song = song,
                                isPlaying = song.id == currentSongId,
                                isFavorite = song.id in favoriteIds,
                                onPlayNow = { onSongSelected(song, filteredLocalSongs) },
                                onToggleFavorite = { onFavoriteToggle(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onClick = { onSongSelected(song, filteredLocalSongs) }
                            )
                        }
                    }
                }

                // 3. ONLINE STREAMING SONGS (YouTube + JioSaavn)
                if (activeFilter in listOf(SearchFilter.ALL, SearchFilter.SONGS, SearchFilter.ONLINE)) {
                    if (onlineResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Online Streaming Tracks",
                                eyebrow = "${onlineResults.size} tracks available to stream"
                            )
                        }

                        items(onlineResults, key = { "online_${it.id}" }) { onlineSong ->
                            val currentPlayingId = onlinePlaybackState?.value?.currentSong?.id
                            val isPlaying = currentPlayingId == onlineSong.id && (onlinePlaybackState?.value?.isPlaying == true)
                            SongRow(
                                onlineSong = onlineSong,
                                isPlaying = isPlaying,
                                onClick = { onOnlineSongSelected(onlineSong, onlineResults) },
                                onPlayNow = { onOnlineSongSelected(onlineSong, onlineResults) }
                            )
                        }
                    }
                }

                // 4. MATCHING ARTISTS
                if (activeFilter in listOf(SearchFilter.ALL, SearchFilter.ARTISTS)) {
                    if (matchedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Artists",
                                eyebrow = "${matchedArtists.size} matching"
                            )
                        }

                        items(matchedArtists, key = { "artist_$it" }) { artistName ->
                            val artistSongs = songs.filter { it.artist == artistName }
                            SearchMediaRow(
                                title = artistName,
                                subtitle = "Artist • ${artistSongs.size} songs in library",
                                artworkModel = artistSongs.firstOrNull(),
                                isCircular = true,
                                onClick = { onArtistSelected(artistName) }
                            )
                        }
                    }
                }

                // 5. MATCHING ALBUMS
                if (activeFilter in listOf(SearchFilter.ALL, SearchFilter.ALBUMS)) {
                    if (matchedAlbums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Albums",
                                eyebrow = "${matchedAlbums.size} matching"
                            )
                        }

                        items(matchedAlbums, key = { "album_$it" }) { albumName ->
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
                }

                // EMPTY STATE
                if (filteredLocalSongs.isEmpty() && onlineResults.isEmpty() && matchedArtists.isEmpty() && matchedAlbums.isEmpty() && !isOnlineLoading) {
                    item {
                        AuraEmptyState(
                            title = "No Matches Found",
                            message = "No songs, artists, or albums matched \"$query\". Check spelling or explore categories.",
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
}

/**
 * Atmospheric Glowing Mesh Category Card replicating the Dribbble Reference:
 * - Dynamic Light Mode and Dark Mode support
 * - Lush pillowy 28.dp squircle shape
 * - Organic multi-node mesh shader matching each genre
 * - Dual-pass Frosted Glass Specular Sheen (top ambient arc reflection + diagonal glass face sheen)
 * - Specular Refractive Beveled Glass Border with ambient colored drop aura
 * - Centered bold crisp typography with contrast shadow
 */
@Composable
fun AtmosphericGlassCategoryCard(
    category: SearchCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val cardShape = RoundedCornerShape(28.dp)

    val shadowElevation = if (isDark) 14.dp else 10.dp
    val shadowSpot = if (isDark) category.glowColor.copy(alpha = 0.55f) else category.glowColor.copy(alpha = 0.38f)
    val shadowAmbient = if (isDark) category.glowColor.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.12f)
    val baseCardBg = if (isDark) Color(0xFF07070A) else Color(0xFF1E293B)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(162.dp)
            .shadow(
                elevation = shadowElevation,
                shape = cardShape,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = baseCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // LAYER 1: Dynamic Multi-Node Atmospheric Mesh Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (category.pattern) {
                    MeshPattern.POP -> {
                        // Deep moss foundation
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF020E05), Color(0xFF082611), Color(0xFF031408))
                                } else {
                                    listOf(Color(0xFF063A17), Color(0xFF15803D), Color(0xFF0B461E))
                                }
                            )
                        )
                        // Radiant lush lime/emerald glow in center-lower
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF86EFAC), Color(0xFF22C55E), Color(0xFF15803D), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.58f),
                                radius = w * 0.68f
                            ),
                            center = Offset(w * 0.5f, h * 0.58f),
                            radius = w * 0.68f
                        )
                        // Top edge dark canopy shadow from reference
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(if (isDark) Color(0xDD020B04) else Color(0x99021206), Color.Transparent),
                                center = Offset(w * 0.5f, -h * 0.05f),
                                radius = w * 0.6f
                            ),
                            topLeft = Offset(0f, -h * 0.25f),
                            size = androidx.compose.ui.geometry.Size(w, h * 0.55f)
                        )
                    }

                    MeshPattern.HIP_HOP -> {
                        // Deep mahogany foundation
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF1E0402), Color(0xFF2F0803), Color(0xFF160301))
                                } else {
                                    listOf(Color(0xFF450A0A), Color(0xFF991B1B), Color(0xFF7F1D1D))
                                }
                            )
                        )
                        // Blazing fiery neon orange & amber center-lower
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFA000), Color(0xFFFF6D00), Color(0xFFDC2626), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.60f),
                                radius = w * 0.72f
                            ),
                            center = Offset(w * 0.5f, h * 0.60f),
                            radius = w * 0.72f
                        )
                        // Top edge dark burgundy shadow from reference
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(if (isDark) Color(0xEA160301) else Color(0xAA1C0402), Color.Transparent),
                                center = Offset(w * 0.5f, -h * 0.08f),
                                radius = w * 0.62f
                            ),
                            topLeft = Offset(0f, -h * 0.28f),
                            size = androidx.compose.ui.geometry.Size(w, h * 0.58f)
                        )
                    }

                    MeshPattern.ELECTRONIC -> {
                        // Deep obsidian cosmic base
                        drawRect(color = if (isDark) Color(0xFF0B020E) else Color(0xFF1E0727))
                        // Top rim neon candy coral-pink glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF3399), Color(0xFFE11D48), Color.Transparent),
                                center = Offset(w * 0.5f, 0f),
                                radius = w * 0.62f
                            ),
                            center = Offset(w * 0.5f, 0f),
                            radius = w * 0.62f
                        )
                        // Bottom rim neon magenta glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF2A85), Color(0xFFC026D3), Color.Transparent),
                                center = Offset(w * 0.5f, h),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.5f, h),
                            radius = w * 0.65f
                        )
                        // Dark contrasting hourglass center void right behind text
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(if (isDark) Color(0xF008140E) else Color(0xCC0D1B14), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.55f
                            ),
                            topLeft = Offset(w * 0.05f, h * 0.32f),
                            size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.36f)
                        )
                    }

                    MeshPattern.ALTERNATIVE -> {
                        // Deep petrol navy base
                        drawRect(color = if (isDark) Color(0xFF031622) else Color(0xFF07273C))
                        // Ethereal ice cyan top-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF67E8F9), Color(0xFF06B6D4), Color.Transparent),
                                center = Offset(w * 0.15f, h * 0.25f),
                                radius = w * 0.68f
                            ),
                            center = Offset(w * 0.15f, h * 0.25f),
                            radius = w * 0.68f
                        )
                        // Orchid lavender center-right glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFE879F9), Color(0xFFA855F7), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.65f),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.85f, h * 0.65f),
                            radius = w * 0.65f
                        )
                        // Diagonal dark petrol shadow ribbon
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(if (isDark) Color(0xDD021018) else Color(0x99031C28), Color.Transparent),
                                center = Offset(w * 0.45f, h * 0.55f),
                                radius = w * 0.5f
                            ),
                            topLeft = Offset(w * 0.1f, h * 0.35f),
                            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.4f)
                        )
                    }

                    MeshPattern.ROCK -> {
                        // Deep abyss navy
                        drawRect(color = if (isDark) Color(0xFF020718) else Color(0xFF07153D))
                        // Center electric cyan-blue beam/oval
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF67E8F9), Color(0xFF00D2FF), Color(0xFF2563EB), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.52f),
                                radius = w * 0.65f
                            ),
                            topLeft = Offset(w * 0.05f, h * 0.28f),
                            size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.48f)
                        )
                        // Top & bottom edge dark shadow
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    if (isDark) Color(0xCC020614) else Color(0x99040F2D),
                                    Color.Transparent,
                                    if (isDark) Color(0xCC020614) else Color(0x99040F2D)
                                )
                            )
                        )
                    }

                    MeshPattern.RNB -> {
                        // Deep twilight base
                        drawRect(color = if (isDark) Color(0xFF0C0318) else Color(0xFF1E083B))
                        // Vivid acid lime / chartreuse at top-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFBEF264), Color(0xFF84CC16), Color.Transparent),
                                center = Offset(w * 0.25f, h * 0.2f),
                                radius = w * 0.72f
                            ),
                            center = Offset(w * 0.25f, h * 0.2f),
                            radius = w * 0.72f
                        )
                        // Royal ultraviolet / violet at bottom-right
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF9333EA), Color(0xFF6B21A8), Color.Transparent),
                                center = Offset(w * 0.8f, h * 0.8f),
                                radius = w * 0.75f
                            ),
                            center = Offset(w * 0.8f, h * 0.8f),
                            radius = w * 0.75f
                        )
                    }

                    MeshPattern.BOLLYWOOD -> {
                        // Deep burgundy wine base
                        drawRect(color = if (isDark) Color(0xFF1A020B) else Color(0xFF380517))
                        // Saffron gold warm node (left)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color.Transparent),
                                center = Offset(w * 0.25f, h * 0.45f),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.25f, h * 0.45f),
                            radius = w * 0.65f
                        )
                        // Royal magenta ruby node (right)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFB7185), Color(0xFFE11D48), Color.Transparent),
                                center = Offset(w * 0.75f, h * 0.55f),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.75f, h * 0.55f),
                            radius = w * 0.65f
                        )
                    }

                    MeshPattern.HINDI -> {
                        drawRect(color = if (isDark) Color(0xFF16020E) else Color(0xFF360522))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFDBA74), Color(0xFFFB923C), Color(0xFFDB2777), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.52f),
                                radius = w * 0.7f
                            ),
                            center = Offset(w * 0.5f, h * 0.52f),
                            radius = w * 0.7f
                        )
                    }

                    MeshPattern.PUNJABI -> {
                        drawRect(color = if (isDark) Color(0xFF1A0401) else Color(0xFF3B0B03))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFED7AA), Color(0xFFFB923C), Color(0xFFEA580C), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.58f),
                                radius = w * 0.72f
                            ),
                            center = Offset(w * 0.5f, h * 0.58f),
                            radius = w * 0.72f
                        )
                    }

                    MeshPattern.INDIE -> {
                        drawRect(color = if (isDark) Color(0xFF120317) else Color(0xFF2E093B))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFE879F9), Color(0xFFA855F7), Color.Transparent),
                                center = Offset(w * 0.75f, h * 0.3f),
                                radius = w * 0.68f
                            ),
                            center = Offset(w * 0.75f, h * 0.3f),
                            radius = w * 0.68f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFF472B6), Color(0xFFDB2777), Color.Transparent),
                                center = Offset(w * 0.25f, h * 0.75f),
                                radius = w * 0.65f
                            ),
                            center = Offset(w * 0.25f, h * 0.75f),
                            radius = w * 0.65f
                        )
                    }

                    MeshPattern.CLASSICAL -> {
                        drawRect(color = if (isDark) Color(0xFF040616) else Color(0xFF0E1338))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFA5B4FC), Color(0xFF6366F1), Color(0xFF3730A3), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.7f
                            ),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.7f
                        )
                    }

                    MeshPattern.CHILL -> {
                        drawRect(color = if (isDark) Color(0xFF011414) else Color(0xFF042B2B))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF5EEAD4), Color(0xFF14B8A6), Color(0xFF0F766E), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.52f),
                                radius = w * 0.72f
                            ),
                            center = Offset(w * 0.5f, h * 0.52f),
                            radius = w * 0.72f
                        )
                    }
                }

                // Cushion Vignette: Perimeter shadow giving recessed lens/pillowy depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x22000000),
                            if (isDark) Color(0x99000000) else Color(0x66000000)
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.75f
                    )
                )
            }

            // LAYER 2: Frosted Glass Specular Sheen (Added Dynamic Glass Effect)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top Specular Highlight Arc reflecting ambient light on curved glass lens
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.38f else 0.52f),
                            Color.White.copy(alpha = if (isDark) 0.12f else 0.18f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(w * 0.08f, -h * 0.15f),
                    size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.52f)
                )

                // Diagonal Glass Sheen across the lens
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.14f else 0.22f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isDark) 0.22f else 0.14f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }

            // LAYER 3: Refractive Specular Beveled Glass Rim Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.4.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.48f),
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.38f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.20f)
                                )
                            }
                        ),
                        shape = cardShape
                    )
            )

            // LAYER 4: Centered Bold Typography with Contrast Depth
            Text(
                text = category.title,
                color = Color.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        blurRadius = 14f,
                        offset = Offset(0f, 2f)
                    )
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Top Artist Highlight Card for prominent search matches
 */
@Composable
fun TopArtistResultCard(
    artistName: String,
    songCount: Int,
    artworkModel: Any?,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    val cardBg = if (isDark) Color(0xFF141418) else Color.White
    val cardBorder = if (isDark) Color(0xFF24242C) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .frostedGlass(
                shape = cardShape,
                isDark = isDark,
                elevation = if (isDark) 10.dp else 6.dp,
                borderWidth = 1.2.dp,
                sheenAlpha = if (isDark) 0.18f else 0.38f
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuraArtwork(
                model = artworkModel,
                size = 64,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = AuraCyan.copy(alpha = if (isDark) 0.15f else 0.20f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "TOP MATCH",
                        color = if (isDark) AuraCyan else Color(0xFF0891B2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = artistName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Artist • $songCount tracks in library",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
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
    val isDark = LocalIsDark.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraArtwork(
            model = artworkModel,
            size = 52,
            shape = if (isCircular) CircleShape else RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}
