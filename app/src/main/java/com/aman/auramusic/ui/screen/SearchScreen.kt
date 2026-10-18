package com.aman.auramusic.ui.screen

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.LocalIsDark
import kotlinx.coroutines.delay

data class SearchCategory(
    val title: String,
    val keywords: List<String>
)

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
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
    activeFilter: SearchFilter = SearchFilter.ALL,
    onActiveFilterChange: (SearchFilter) -> Unit = {},
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onlinePlaybackManager: OnlinePlaybackManager? = null,
    onOnlineSongSelected: (OnlineSong, List<OnlineSong>) -> Unit = { _, _ -> },
    onOnlinePlayNext: ((OnlineSong) -> Unit)? = null,
    onOnlineAddToQueue: ((OnlineSong) -> Unit)? = null,
    onAddOnlineSongToPlaylist: ((OnlineSong) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { OnlineMusicRepository() }

    // Adaptive Monochrome Tokens
    val screenBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textPlaceholder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val searchContainerBg = MaterialTheme.colorScheme.surfaceVariant
    val searchBorderColor = MaterialTheme.colorScheme.outline
    val chipBg = MaterialTheme.colorScheme.surfaceVariant
    val chipBorder = MaterialTheme.colorScheme.outline

    // Active result filter state
    var internalFilter by rememberSaveable(
        stateSaver = Saver(
            save = { it.name },
            restore = { name -> runCatching { SearchFilter.valueOf(name) }.getOrDefault(SearchFilter.ALL) }
        )
    ) { mutableStateOf(activeFilter) }

    LaunchedEffect(activeFilter) {
        if (internalFilter != activeFilter) {
            internalFilter = activeFilter
        }
    }

    val updateFilter: (SearchFilter) -> Unit = { newFilter ->
        internalFilter = newFilter
        onActiveFilterChange(newFilter)
    }

    val currentFilter = internalFilter
    val searchListState = rememberLazyListState()

    // Intercept back button when search query is active to smoothly return to category discovery
    BackHandler(enabled = query.isNotEmpty()) {
        onQueryChange("")
        updateFilter(SearchFilter.ALL)
        focusManager.clearFocus()
    }

    // Recent search history state
    var recentSearches by remember { mutableStateOf(getRecentSearches(context)) }

    var selectedAudioSource by remember { mutableStateOf(AudioSource.ALL) }

    // Online search state
    var onlineResults by remember { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val onlinePlaybackState = onlinePlaybackManager?.playbackState?.collectAsState()

    // 12 Curated Categories
    val categories = remember {
        listOf(
            SearchCategory(
                title = "Pop",
                keywords = listOf("pop", "swift", "sheeran", "bieber", "lipa", "ariana", "weekend", "drake", "bruno", "gomez", "billie", "olivia", "perry", "styles", "puth")
            ),
            SearchCategory(
                title = "Hip Hop",
                keywords = listOf("hip-hop", "hip hop", "rap", "rapper", "divine", "emiway", "raftaar", "stan", "krsna", "drake", "eminem", "kanye", "travis")
            ),
            SearchCategory(
                title = "Electronic",
                keywords = listOf("electronic", "dance", "edm", "party", "club", "dj", "remix", "house", "garrix", "guetta", "avicii", "marshmello")
            ),
            SearchCategory(
                title = "Alternative",
                keywords = listOf("alternative", "indie", "alt", "arctic monkeys", "strokes", "radiohead", "cage the elephant", "tame impala", "twenty one pilots")
            ),
            SearchCategory(
                title = "Rock",
                keywords = listOf("rock", "metal", "band", "guitar", "queen", "nirvana", "metallica", "coldplay", "dragons", "linkin park", "ac/dc")
            ),
            SearchCategory(
                title = "R&B",
                keywords = listOf("r&b", "rnb", "soul", "sza", "frank ocean", "brent faiyaz", "khalid", "daniel caesar", "summer walker", "usher")
            ),
            SearchCategory(
                title = "Bollywood",
                keywords = listOf("bollywood", "hindi", "arijit", "neha", "badshah", "jubin", "shreya", "pritam", "t-series", "filmi", "singh", "kapoor", "khan")
            ),
            SearchCategory(
                title = "Hindi Music",
                keywords = listOf("hindi", "arijit", "sonu nigam", "shreya ghoshal", "kk", "atif", "mohit chauhan", "ar rahman", "pritam", "mithoon")
            ),
            SearchCategory(
                title = "Punjabi",
                keywords = listOf("punjabi", "sidhu", "diljit", "karan", "aujla", "ap dhillon", "bhangra", "desi", "jatt", "honey singh", "randhawa", "b praak")
            ),
            SearchCategory(
                title = "Indie",
                keywords = listOf("indie", "prateek kuhad", "anuv jain", "zaeden", "ritviz", "when chai met toast", "the local train", "jasleen royal")
            ),
            SearchCategory(
                title = "Classical",
                keywords = listOf("classical", "sufi", "ghazal", "qawwali", "raag", "classic", "retro", "90s", "80s", "kishore", "lata", "rafi", "jagjit")
            ),
            SearchCategory(
                title = "Chill",
                keywords = listOf("chill", "lofi", "soft", "acoustic", "relax", "peace", "sleep", "night", "rain", "calm", "study", "ambient")
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

    val matchedAlbums = remember(songs, onlineResults, query) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            val localAlbums = songs.map { it.album }
                .distinct()
                .filter { it.isNotBlank() && it.lowercase().contains(q) }
            val onlineAlbums = onlineResults.map { it.album }
                .distinct()
                .filter { it.isNotBlank() && (it.lowercase().contains(q) || q.contains(it.lowercase())) }
            (localAlbums + onlineAlbums).distinct()
        }
    }

    val matchedArtists = remember(songs, onlineResults, query) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            val localArtists = songs.map { it.artist }
                .distinct()
                .filter { it.isNotBlank() && it.lowercase().contains(q) }
            val onlineArtists = onlineResults.map { it.artist }
                .distinct()
                .filter { it.isNotBlank() && (it.lowercase().contains(q) || q.contains(it.lowercase())) }
            val merged = (localArtists + onlineArtists).distinct().toMutableList()
            if (merged.none { it.equals(query.trim(), ignoreCase = true) } && onlineResults.isNotEmpty()) {
                val matchingArtist = onlineResults.firstOrNull { it.artist.contains(query.trim(), ignoreCase = true) || query.trim().contains(it.artist, ignoreCase = true) }?.artist
                if (matchingArtist != null && !merged.contains(matchingArtist)) {
                    merged.add(0, matchingArtist)
                }
            }
            merged
        }
    }

    // Standout Top Match Candidate (Artist or Song)
    val topArtistMatch = remember(matchedArtists, query) {
        if (query.isBlank()) null
        else matchedArtists.firstOrNull { it.equals(query.trim(), ignoreCase = true) } ?: matchedArtists.firstOrNull()
    }

    // Songs matching the searched artist or album
    val artistLocalSongs = remember(filteredLocalSongs, query) {
        if (query.isBlank()) emptyList()
        else filteredLocalSongs.filter { it.artist.contains(query.trim(), ignoreCase = true) || query.trim().contains(it.artist, ignoreCase = true) }
    }
    val artistOnlineSongs = remember(onlineResults, query) {
        if (query.isBlank()) emptyList()
        else onlineResults.filter { it.artist.contains(query.trim(), ignoreCase = true) || query.trim().contains(it.artist, ignoreCase = true) }
    }
    val albumLocalSongs = remember(filteredLocalSongs, query) {
        if (query.isBlank()) emptyList()
        else filteredLocalSongs.filter { it.album.contains(query.trim(), ignoreCase = true) || query.trim().contains(it.album, ignoreCase = true) }
    }
    val albumOnlineSongs = remember(onlineResults, query) {
        if (query.isBlank()) emptyList()
        else onlineResults.filter { it.album.contains(query.trim(), ignoreCase = true) || query.trim().contains(it.album, ignoreCase = true) }
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
            state = searchListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // ==========================================
            // HEADER & SEARCH FIELD
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
                                    updateFilter(SearchFilter.ALL)
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

                        // Right Spacer to balance the header and keep the Search title centered
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleek Rounded Search Bar
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
                                tint = if (query.isNotEmpty()) MaterialTheme.colorScheme.primary else textPlaceholder,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    onQueryChange("")
                                    updateFilter(SearchFilter.ALL)
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
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(26.dp)),
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
                            cursorColor = MaterialTheme.colorScheme.primary,
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
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        clearRecentSearches(context)
                                        recentSearches = emptyList()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal recent search pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { hist ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
                                        .clickable {
                                            onQueryChange(hist)
                                            updateFilter(SearchFilter.ALL)
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

                    // Result Filter Chips when query is active
                    if (query.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchFilter.values().forEach { filter ->
                                val isSelected = currentFilter == filter
                                val activeTextColor = MaterialTheme.colorScheme.onPrimary
                                val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { updateFilter(filter) }
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
            // DISCOVERY MODE: 2-COLUMN CATEGORY CARDS
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
                                CategoryCard(
                                    category = cat,
                                    onClick = {
                                        onQueryChange(cat.title)
                                        updateFilter(SearchFilter.ALL)
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
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 1. TOP RESULT CARD (Standout Artist or Song highlight)
                if (currentFilter == SearchFilter.ALL || currentFilter == SearchFilter.ARTISTS) {
                    if (topArtistMatch != null) {
                        item {
                            val artSongs = songs.filter { it.artist.contains(topArtistMatch, ignoreCase = true) }
                            val artOnline = onlineResults.filter { it.artist.contains(topArtistMatch, ignoreCase = true) }
                            val artwork = artSongs.firstOrNull() ?: artOnline.firstOrNull()?.artworkUrl
                            val totalCount = artSongs.size + artOnline.size
                            TopArtistResultCard(
                                artistName = topArtistMatch,
                                songCount = totalCount,
                                artworkModel = artwork,
                                onClick = { onArtistSelected(topArtistMatch) }
                            )
                        }
                    }
                }

                // 2. LOCAL SONGS
                if (currentFilter in listOf(SearchFilter.ALL, SearchFilter.SONGS)) {
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
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onClick = { onSongSelected(song, filteredLocalSongs) }
                            )
                        }
                    }
                }

                // 3. ONLINE STREAMING SONGS (YouTube + JioSaavn)
                if (currentFilter in listOf(SearchFilter.ALL, SearchFilter.SONGS)) {
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
                                onPlayNow = { onOnlineSongSelected(onlineSong, onlineResults) },
                                onPlayNext = onOnlinePlayNext?.let { cb -> { cb(onlineSong) } },
                                onAddToQueue = onOnlineAddToQueue?.let { cb -> { cb(onlineSong) } },
                                onAddToPlaylist = onAddOnlineSongToPlaylist?.let { cb -> { cb(onlineSong) } }
                            )
                        }
                    }
                }

                // 4. MATCHING ARTISTS
                if (currentFilter in listOf(SearchFilter.ALL, SearchFilter.ARTISTS)) {
                    if (matchedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Artists",
                                eyebrow = "${matchedArtists.size} matching"
                            )
                        }

                        items(matchedArtists, key = { "artist_$it" }) { artistName ->
                            val artSongs = songs.filter { it.artist.contains(artistName, ignoreCase = true) }
                            val artOnline = onlineResults.filter { it.artist.contains(artistName, ignoreCase = true) }
                            val artwork = artSongs.firstOrNull() ?: artOnline.firstOrNull()?.artworkUrl
                            val subtitle = if (artSongs.isNotEmpty() && artOnline.isNotEmpty()) {
                                "Artist • ${artSongs.size} in library, ${artOnline.size} online"
                            } else if (artSongs.isNotEmpty()) {
                                "Artist • ${artSongs.size} songs in library"
                            } else {
                                "Artist • ${artOnline.size} tracks online"
                            }
                            SearchMediaRow(
                                title = artistName,
                                subtitle = subtitle,
                                artworkModel = artwork,
                                isCircular = true,
                                onClick = { onArtistSelected(artistName) }
                            )
                        }
                    }

                    // When filter is specifically ARTISTS, also display the artist tracks below
                    if (currentFilter == SearchFilter.ARTISTS) {
                        if (artistLocalSongs.isNotEmpty() || artistOnlineSongs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                SectionHeader(
                                    title = "Artist Tracks",
                                    eyebrow = "${artistLocalSongs.size + artistOnlineSongs.size} tracks found"
                                )
                            }
                            items(artistLocalSongs, key = { "art_local_${it.id}" }) { song ->
                                SongRow(
                                    song = song,
                                    isPlaying = song.id == currentSongId,
                                    isFavorite = song.id in favoriteIds,
                                    onPlayNow = { onSongSelected(song, artistLocalSongs) },
                                    onToggleFavorite = { onFavoriteToggle(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onPlayNext = { onPlayNext(song) },
                                    onAddToQueue = { onAddToQueue(song) },
                                    onClick = { onSongSelected(song, artistLocalSongs) }
                                )
                            }
                            items(artistOnlineSongs, key = { "art_online_${it.id}" }) { onlineSong ->
                                val currentPlayingId = onlinePlaybackState?.value?.currentSong?.id
                                val isPlaying = currentPlayingId == onlineSong.id && (onlinePlaybackState?.value?.isPlaying == true)
                                SongRow(
                                    onlineSong = onlineSong,
                                    isPlaying = isPlaying,
                                    onClick = { onOnlineSongSelected(onlineSong, artistOnlineSongs) },
                                    onPlayNow = { onOnlineSongSelected(onlineSong, artistOnlineSongs) },
                                    onPlayNext = onOnlinePlayNext?.let { cb -> { cb(onlineSong) } },
                                    onAddToQueue = onOnlineAddToQueue?.let { cb -> { cb(onlineSong) } },
                                    onAddToPlaylist = onAddOnlineSongToPlaylist?.let { cb -> { cb(onlineSong) } }
                                )
                            }
                        }
                    }
                }

                // 5. MATCHING ALBUMS
                if (currentFilter in listOf(SearchFilter.ALL, SearchFilter.ALBUMS)) {
                    if (matchedAlbums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionHeader(
                                title = "Albums",
                                eyebrow = "${matchedAlbums.size} matching"
                            )
                        }

                        items(matchedAlbums, key = { "album_$it" }) { albumName ->
                            val albSongs = songs.filter { it.album.contains(albumName, ignoreCase = true) }
                            val albOnline = onlineResults.filter { it.album.contains(albumName, ignoreCase = true) }
                            val artwork = albSongs.firstOrNull() ?: albOnline.firstOrNull()?.artworkUrl
                            val artistName = albSongs.firstOrNull()?.artist ?: albOnline.firstOrNull()?.artist ?: "Various Artists"
                            val subtitle = "Album • $artistName"
                            SearchMediaRow(
                                title = albumName,
                                subtitle = subtitle,
                                artworkModel = artwork,
                                isCircular = false,
                                onClick = { onAlbumSelected(albumName) }
                            )
                        }
                    }

                    // When filter is specifically ALBUMS, also display the album tracks below
                    if (currentFilter == SearchFilter.ALBUMS) {
                        if (albumLocalSongs.isNotEmpty() || albumOnlineSongs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                SectionHeader(
                                    title = "Album Tracks",
                                    eyebrow = "${albumLocalSongs.size + albumOnlineSongs.size} tracks found"
                                )
                            }
                            items(albumLocalSongs, key = { "alb_local_${it.id}" }) { song ->
                                SongRow(
                                    song = song,
                                    isPlaying = song.id == currentSongId,
                                    isFavorite = song.id in favoriteIds,
                                    onPlayNow = { onSongSelected(song, albumLocalSongs) },
                                    onToggleFavorite = { onFavoriteToggle(song) },
                                    onAddToPlaylist = { onAddToPlaylist(song) },
                                    onPlayNext = { onPlayNext(song) },
                                    onAddToQueue = { onAddToQueue(song) },
                                    onClick = { onSongSelected(song, albumLocalSongs) }
                                )
                            }
                            items(albumOnlineSongs, key = { "alb_online_${it.id}" }) { onlineSong ->
                                val currentPlayingId = onlinePlaybackState?.value?.currentSong?.id
                                val isPlaying = currentPlayingId == onlineSong.id && (onlinePlaybackState?.value?.isPlaying == true)
                                SongRow(
                                    onlineSong = onlineSong,
                                    isPlaying = isPlaying,
                                    onClick = { onOnlineSongSelected(onlineSong, albumOnlineSongs) },
                                    onPlayNow = { onOnlineSongSelected(onlineSong, albumOnlineSongs) },
                                    onPlayNext = onOnlinePlayNext?.let { cb -> { cb(onlineSong) } },
                                    onAddToQueue = onOnlineAddToQueue?.let { cb -> { cb(onlineSong) } },
                                    onAddToPlaylist = onAddOnlineSongToPlaylist?.let { cb -> { cb(onlineSong) } }
                                )
                            }
                        }
                    }
                }

                // EMPTY STATE
                val isFilterEmpty = when (currentFilter) {
                    SearchFilter.ALL -> filteredLocalSongs.isEmpty() && onlineResults.isEmpty() && matchedArtists.isEmpty() && matchedAlbums.isEmpty()
                    SearchFilter.SONGS -> filteredLocalSongs.isEmpty() && onlineResults.isEmpty()
                    SearchFilter.ARTISTS -> matchedArtists.isEmpty() && artistLocalSongs.isEmpty() && artistOnlineSongs.isEmpty()
                    SearchFilter.ALBUMS -> matchedAlbums.isEmpty() && albumLocalSongs.isEmpty() && albumOnlineSongs.isEmpty()
                }

                if (isFilterEmpty && !isOnlineLoading) {
                    item {
                        val emptyTitle = when (currentFilter) {
                            SearchFilter.ARTISTS -> "No Artists Found"
                            SearchFilter.ALBUMS -> "No Albums Found"
                            SearchFilter.SONGS -> "No Songs Found"
                            SearchFilter.ALL -> "No Matches Found"
                        }
                        val emptyMsg = when (currentFilter) {
                            SearchFilter.ARTISTS -> "No artists or tracks matched \"$query\"."
                            SearchFilter.ALBUMS -> "No albums or tracks matched \"$query\"."
                            SearchFilter.SONGS -> "No songs matched \"$query\"."
                            SearchFilter.ALL -> "No songs, artists, or albums matched \"$query\". Check spelling or explore categories."
                        }
                        AuraEmptyState(
                            title = emptyTitle,
                            message = emptyMsg,
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
 * Clean monochrome category card for search genres.
 */
@Composable
fun CategoryCard(
    category: SearchCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface, cardShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            textAlign = TextAlign.Center
        )
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
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface, cardShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), cardShape)
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "TOP MATCH",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

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
            tint = textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
