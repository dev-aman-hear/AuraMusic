package com.aman.auramusic.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.util.formatDuration

/**
 * Editorial top bar for the Apple Music monochrome Home experience.
 */
@Composable
fun HomeHeader(
    timeGreeting: String,
    username: String,
    dateText: String,
    userInitial: String,
    isOffline: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val titleColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = dateText.uppercase(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.0.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formattedGreeting = if (username.isNotBlank()) "$timeGreeting," else timeGreeting
                Text(
                    text = formattedGreeting,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (username.isNotBlank()) {
                    Text(
                        text = username,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isOffline) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Profile Avatar Badge
                AuraAvatarBadge(
                    initial = userInitial,
                    onClick = onOpenSettings,
                    size = 42.dp
                )
            }
        }
    }
}

/**
 * Profile Avatar Badge.
 */
@Composable
fun AuraAvatarBadge(
    initial: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val displayInitial = initial.ifBlank { "A" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayInitial,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (size.value * 0.46f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun HomeHeader(
    greeting: String,
    dateText: String,
    userInitial: String,
    isOffline: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeHeader(
        timeGreeting = greeting,
        username = "",
        dateText = dateText,
        userInitial = userInitial,
        isOffline = isOffline,
        onOpenSettings = onOpenSettings,
        onRefresh = onRefresh,
        modifier = modifier
    )
}

/**
 * Editorial Section Header with Apple Music typography and signature accent.
 */
@Composable
fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actionText: String? = "See All",
    onActionClick: (() -> Unit)? = null
) {
    val titleColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (onActionClick != null && !actionText.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = { onActionClick.invoke() })
                ) {
                    Text(
                        text = actionText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See more",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Minimal Circular Monochrome Play Button
 */
@Composable
fun AuraPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = (size.value * 0.52f).dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(iconSize)
                .offset(x = 1.dp)
        )
    }
}

/**
 * Daily Featured Album or Playlist Card.
 */
@Composable
fun DailyFeaturedCard(
    item: DailyFeaturedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                AuraArtwork(
                    model = item.artworkUrl,
                    size = 112,
                    shape = RoundedCornerShape(20.dp),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline)
                        .padding(horizontal = 5.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.badgeLabel.uppercase().ifBlank { "DAILY FEATURED PLAYLIST" },
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.25.sp,
                        maxLines = 1
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.title,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.3).sp
                    )

                    if (item.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.songCountText.isNotBlank()) {
                        Text(
                            text = item.songCountText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AuraPlayButton(
                        onClick = onClick,
                        size = 36.dp
                    )
                }
            }
        }
    }
}

/**
 * Listen Again Horizontal Section
 */
@Composable
fun ListenAgainSection(
    columns: List<List<QuickHitTrack>>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onTrackClick: (QuickHitTrack) -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (columns.isEmpty()) return

    Column(modifier = modifier.padding(top = 22.dp)) {
        HomeSectionHeader(
            title = "Listen Again"
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(columns, key = { col -> "lacol_${col.firstOrNull()?.id}" }) { columnTracks ->
                QuickHitsColumn(
                    tracks = columnTracks,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onTrackClick = onTrackClick,
                    onOptionsClick = onOptionsClick
                )
            }
        }
    }
}

/**
 * Trending Songs Horizontal Section
 */
@Composable
fun TrendingSongsSection(
    tracks: List<TrendingSongItem>,
    onTrackClick: (TrendingSongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return

    Column(modifier = modifier.padding(top = 22.dp)) {
        HomeSectionHeader(
            title = "Trending Now"
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks, key = { "trend_${it.onlineSong.id}" }) { item ->
                TrendingTrackCard(
                    item = item,
                    onClick = { onTrackClick(item) }
                )
            }
        }
    }
}

@Composable
fun TrendingTrackCard(
    item: TrendingSongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 142.dp
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        AuraArtwork(
            model = item.onlineSong.artworkUrl,
            size = cardWidth.value.toInt(),
            modifier = Modifier.size(cardWidth),
            shape = RoundedCornerShape(20.dp),
            elevation = 4.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.onlineSong.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.onlineSong.artist,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Artists Section
 */
@Composable
fun ArtistsSection(
    artists: List<ArtistItem>,
    onArtistClick: (ArtistItem) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (artists.isEmpty()) return

    Column(modifier = modifier.padding(top = 24.dp)) {
        HomeSectionHeader(
            title = "Artist Discovery",
            actionText = "See All",
            onActionClick = onSeeAllClick
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists, key = { "artist_${it.artistId}_${it.name}" }) { artist ->
                ArtistSquareCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
fun ArtistSquareCard(
    artist: ArtistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 94.dp
) {
    val titleColor = MaterialTheme.colorScheme.onBackground
    var imageFailed by remember(artist.name, artist.artworkModel) { mutableStateOf(false) }
    val showImage = artist.artworkModel != null && !imageFailed

    Column(
        modifier = modifier
            .width(size)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (showImage) {
                AsyncImage(
                    model = artist.artworkModel,
                    contentDescription = "${artist.name} artist portrait",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true }
                )
            } else {
                val initial = artist.name.trim().firstOrNull()?.uppercaseChar()?.toString()
                if (!initial.isNullOrBlank()) {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Artist detail view sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailSheet(
    artist: ArtistItem,
    onlineSongs: List<OnlineSong>,
    localSongs: List<Song>,
    onlineAlbums: List<OnlinePlaylist> = emptyList(),
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayOnlineSong: (OnlineSong, List<OnlineSong>) -> Unit,
    onOpenOnlineAlbum: (OnlinePlaylist) -> Unit = {},
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit = onShuffleAll,
    isSavedInLibrary: Boolean = false,
    onToggleSaveToLibrary: (() -> Unit)? = null,
    audienceText: String? = null
) {
    val pageBackground = MaterialTheme.colorScheme.background
    val titleColor = MaterialTheme.colorScheme.onBackground
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val realAlbums = remember(localSongs) {
        localSongs.filter { it.album.isNotBlank() && !it.album.equals("unknown", true) }
            .groupBy { it.album.trim() }
            .entries
            .sortedByDescending { it.value.size }
    }
    var showArtistMenu by remember(artist.artistId, artist.name) { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = pageBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 176.dp)
        ) {
            item(key = "artist_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    val initial = artist.name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (initial.isNotBlank()) {
                            Text(
                                text = initial,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 76.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (artist.artworkModel != null) {
                        AsyncImage(
                            model = artist.artworkModel,
                            contentDescription = "${artist.name} artist artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.50f to Color.Transparent,
                                    0.78f to pageBackground.copy(alpha = 0.70f),
                                    1f to pageBackground
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { showArtistMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Artist options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showArtistMenu,
                                onDismissRequest = { showArtistMenu = false }
                            ) {
                                if (onToggleSaveToLibrary != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (isSavedInLibrary) "Remove from Artist" else "Save to Artist")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSavedInLibrary) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showArtistMenu = false
                                            onToggleSaveToLibrary()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = artist.name,
                                color = titleColor,
                                fontSize = 32.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            val effectiveAudience = audienceText?.takeIf { it.isNotBlank() && !it.equals("YouTube Music", ignoreCase = true) }
                                ?: artist.songCountText.takeIf { it.isNotBlank() && !it.equals("YouTube Music", ignoreCase = true) && (artist.isOnline || it.contains("audience", ignoreCase = true) || it.contains("subscribers", ignoreCase = true)) }
                                ?: if (localSongs.isNotEmpty()) "${localSongs.size} songs" else null

                            if (!effectiveAudience.isNullOrBlank()) {
                                val formattedAudience = if (!effectiveAudience.startsWith("Artist", ignoreCase = true)) {
                                    "Artist • $effectiveAudience"
                                } else {
                                    effectiveAudience
                                }
                                Text(
                                    text = formattedAudience,
                                    color = secondaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        AuraPlayButton(
                            onClick = onPlayAll,
                            size = 54.dp,
                            iconSize = 28.dp
                        )
                    }
                }
            }

            if (isLoading) {
                item(key = "artist_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading artist catalogue from YouTube Music…", color = secondaryColor, fontSize = 13.sp)
                        }
                    }
                }
            } else if (onlineSongs.isEmpty() && localSongs.isEmpty()) {
                item(key = "artist_empty") {
                    Text(
                        text = "No tracks are available for this artist yet.",
                        color = secondaryColor,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp)
                    )
                }
            } else {
                if (onlineSongs.isNotEmpty()) {
                    item(key = "artist_top_songs_heading") {
                        Text(
                            text = "Top songs",
                            color = titleColor,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp)
                        )
                    }
                    items(onlineSongs.take(20), key = { "yt_artist_track_${it.id}" }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayOnlineSong(song, onlineSongs) }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraArtwork(
                                model = song.artworkUrl,
                                size = 54,
                                shape = RoundedCornerShape(6.dp),
                                elevation = 0.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                                Text(song.title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = listOf(song.artist, song.album.takeIf { it.isNotBlank() && !it.equals("YouTube Music", true) })
                                        .filterNotNull().filter { it.isNotBlank() }.distinct().joinToString(" · "),
                                    color = secondaryColor,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AuraPlayButton(
                                onClick = { onPlayOnlineSong(song, onlineSongs) },
                                size = 32.dp,
                                iconSize = 16.dp
                            )
                        }
                    }
                }

                if (localSongs.isNotEmpty()) {
                    item(key = "artist_local_heading") {
                        Text(
                            text = if (onlineSongs.isEmpty()) "Top songs" else "In your library",
                            color = titleColor,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (onlineSongs.isEmpty()) 10.dp else 24.dp,
                                bottom = 8.dp
                            )
                        )
                    }
                    items(localSongs, key = { "local_artist_track_${it.id}" }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaySong(song, localSongs) }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtwork(song = song, size = 54)
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                                Text(song.title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(3.dp))
                                val subtitleText = listOf(song.artist, song.album)
                                    .filter { it.isNotBlank() && !it.equals("unknown", true) }
                                    .distinct()
                                    .joinToString(" · ")
                                    .ifBlank { song.artist }
                                Text(subtitleText, color = secondaryColor, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            AuraPlayButton(
                                onClick = { onPlaySong(song, localSongs) },
                                size = 32.dp,
                                iconSize = 16.dp
                            )
                        }
                    }
                }

                if (onlineAlbums.isNotEmpty()) {
                    item(key = "artist_online_albums_heading") {
                        Text(
                            text = "Albums & singles",
                            color = titleColor,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp)
                        )
                    }
                    item(key = "artist_online_albums_rail") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(onlineAlbums, key = { "yt_album_${it.id}" }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(142.dp)
                                        .clickable { onOpenOnlineAlbum(album) }
                                ) {
                                    AuraArtwork(
                                        model = album.artworkUrl,
                                        size = 142,
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = 0.dp,
                                        modifier = Modifier.size(142.dp)
                                    )
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        album.title,
                                        color = titleColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        album.subtitle.ifBlank { "Release" },
                                        color = secondaryColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (realAlbums.isNotEmpty()) {
                    item(key = "artist_albums_heading") {
                        Text(
                            text = "Albums in your library",
                            color = titleColor,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp)
                        )
                    }
                    item(key = "artist_albums_rail") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(realAlbums, key = { "artist_album_${it.key}" }) { entry ->
                                Column(
                                    modifier = Modifier.width(142.dp).clickable {
                                        val albumTracks = entry.value
                                        albumTracks.firstOrNull()?.let { onPlaySong(it, albumTracks) }
                                    }
                                ) {
                                    AuraArtwork(
                                        model = entry.value.firstOrNull()?.artworkUri,
                                        size = 142,
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = 0.dp,
                                        modifier = Modifier.size(142.dp)
                                    )
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(entry.key, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${entry.value.size} songs · Local library", color = secondaryColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Hits Section
 */
@Composable
fun QuickHitsSection(
    columns: List<List<QuickHitTrack>>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onTrackClick: (QuickHitTrack) -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (columns.isEmpty()) return

    Column(modifier = modifier.padding(top = 26.dp)) {
        HomeSectionHeader(
            title = "Quick Hits",
            actionText = "See All",
            onActionClick = onSeeAllClick
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(columns, key = { col -> "qcol_${col.firstOrNull()?.id}" }) { columnTracks ->
                QuickHitsColumn(
                    tracks = columnTracks,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onTrackClick = onTrackClick,
                    onOptionsClick = onOptionsClick
                )
            }
        }
    }
}

@Composable
fun QuickHitsColumn(
    tracks: List<QuickHitTrack>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onTrackClick: (QuickHitTrack) -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    columnWidth: Dp = 314.dp
) {
    Column(
        modifier = modifier
            .width(columnWidth)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tracks.forEach { track ->
            val isActive = when (track) {
                is QuickHitTrack.LocalTrack -> currentSongId == track.song.id
                is QuickHitTrack.OnlineTrack -> false
            }
            QuickHitTrackItem(
                track = track,
                isActive = isActive,
                isPlaying = isActive && isPlaying,
                onClick = { onTrackClick(track) },
                onOptionsClick = {
                    if (track is QuickHitTrack.LocalTrack) {
                        onOptionsClick(track.song)
                    }
                }
            )
        }
    }
}

@Composable
fun QuickHitTrackItem(
    track: QuickHitTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val itemBg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "quickHitBg"
    )

    val badgeColor = MaterialTheme.colorScheme.onSurfaceVariant

    val itemShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(itemShape)
            .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant, itemShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuraArtwork(
                model = track.artworkModel,
                size = 46,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(9.dp),
                elevation = 0.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${track.sourceBadge}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                AuraPlayButton(
                    onClick = onClick,
                    size = 32.dp,
                    iconSize = 16.dp
                )
            }
        }
    }
}

/**
 * Discover by Category (Genres & Moods)
 */
@Composable
fun DiscoverCategorySection(
    categories: List<CategoryDiscoverItem>,
    onCategoryClick: (CategoryDiscoverItem) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    if (categories.isEmpty()) return

    Column(modifier = modifier.padding(top = 20.dp, bottom = 4.dp)) {
        HomeSectionHeader(
            title = "Discover by Category",
            actionText = "See All",
            onActionClick = onSeeAllClick
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories, key = { "cat_${it.id}" }) { category ->
                DiscoverCategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
fun DiscoverCategoryCard(
    category: CategoryDiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 154.dp,
    cardHeight: Dp = 92.dp
) {
    val isDark = LocalIsDark.current
    val cardShape = RoundedCornerShape(22.dp)

    val sizeModifier = if (cardWidth == Dp.Unspecified) {
        Modifier.height(cardHeight)
    } else {
        Modifier.size(width = cardWidth, height = cardHeight)
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant, cardShape)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                cardShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = category.subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Category Detail Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailSheet(
    category: CategoryDiscoverItem,
    onlineSongs: List<OnlineSong>,
    localSongs: List<Song>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayOnlineSong: (OnlineSong, List<OnlineSong>) -> Unit,
    onShuffleAll: () -> Unit
) {
    val isDark = LocalIsDark.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category.subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onShuffleAll,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            } else if (onlineSongs.isEmpty() && localSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs found for this category.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onlineSongs.isNotEmpty()) {
                        onlineSongs.take(8).forEach { oSong ->
                            val track = QuickHitTrack.OnlineTrack(oSong, "${category.title} Hit")
                            QuickHitTrackItem(
                                track = track,
                                isActive = false,
                                isPlaying = false,
                                onClick = {
                                    onPlayOnlineSong(oSong, onlineSongs)
                                    onDismiss()
                                },
                                onOptionsClick = {}
                            )
                        }
                    } else {
                        localSongs.take(8).forEach { lSong ->
                            val track = QuickHitTrack.LocalTrack(lSong, "Local")
                            QuickHitTrackItem(
                                track = track,
                                isActive = false,
                                isPlaying = false,
                                onClick = {
                                    onPlaySong(lSong, localSongs)
                                    onDismiss()
                                },
                                onOptionsClick = {}
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * All Artists Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllArtistsSheet(
    artists: List<ArtistItem>,
    onArtistClick: (ArtistItem) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDark.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "All Artists",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(artists, key = { "all_art_${it.artistId}_${it.name}" }) { artist ->
                    ArtistSquareCard(
                        artist = artist,
                        onClick = {
                            onArtistClick(artist)
                            onDismiss()
                        },
                        size = 100.dp
                    )
                }
            }
        }
    }
}

/**
 * All Quick Hits Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllQuickHitsSheet(
    tracks: List<QuickHitTrack>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onTrackClick: (QuickHitTrack) -> Unit,
    onOptionsClick: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDark.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Quick Hits",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(tracks, key = { "all_qh_${it.id}" }) { track ->
                    val isActive = when (track) {
                        is QuickHitTrack.LocalTrack -> currentSongId == track.song.id
                        is QuickHitTrack.OnlineTrack -> false
                    }
                    QuickHitTrackItem(
                        track = track,
                        isActive = isActive,
                        isPlaying = isActive && isPlaying,
                        onClick = {
                            onTrackClick(track)
                            onDismiss()
                        },
                        onOptionsClick = {
                            if (track is QuickHitTrack.LocalTrack) {
                                onOptionsClick(track.song)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * All Categories Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesSheet(
    categories: List<CategoryDiscoverItem>,
    onCategoryClick: (CategoryDiscoverItem) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDark.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Discover by Category",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(categories, key = { "all_cat_${it.id}" }) { category ->
                    DiscoverCategoryCard(
                        category = category,
                        onClick = {
                            onCategoryClick(category)
                            onDismiss()
                        },
                        cardWidth = Dp.Unspecified,
                        cardHeight = 100.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
