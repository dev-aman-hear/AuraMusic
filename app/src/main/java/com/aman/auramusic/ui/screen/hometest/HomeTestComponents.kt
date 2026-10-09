package com.aman.auramusic.ui.screen.hometest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.ui.component.AuraArtwork
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.theme.AuraPrimary
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.util.formatDuration

/**
 * Editorial top bar for the redesigned Home Test experience.
 */
@Composable
fun HomeTestHeader(
    greeting: String,
    dateText: String,
    userInitial: String,
    isOffline: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

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
                    fontWeight = FontWeight.ExtraBold,
                    color = AuraPrimary,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greeting,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOffline) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C2415),
                        border = BorderStroke(1.dp, Color(0xFFE6A23C).copy(alpha = 0.4f)),
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
                                tint = Color(0xFFE6A23C),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE6A23C)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feed",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = AuraPrimary
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
        }
    }
}

/**
 * Editorial Section Header with optional eyebrow and action.
 */
@Composable
fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

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
                color = AuraPrimary,
                letterSpacing = 1.1.sp
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
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (onActionClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onActionClick)
                ) {
                    if (!actionText.isNullOrBlank()) {
                        Text(
                            text = actionText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See more",
                        tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * A. Daily Featured Album or Playlist Card
 * Refresh selection once per calendar day (via DailyFeaturedCache).
 */
@Composable
fun DailyFeaturedCard(
    item: DailyFeaturedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val surfaceColor = if (isDark) Color(0xFF141318) else Color(0xFFF6F7F9)
    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.04f))
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = if (isDark) 12.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isDark) Color.Black else Color(0x33000000)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            surfaceColor,
                            surfaceColor.copy(alpha = 0.95f),
                            if (isDark) Color(0xFF261A22) else Color(0xFFFFEEF2)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large high-res artwork
                AuraArtwork(
                    model = item.artworkUrl,
                    size = 118,
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    modifier = Modifier.size(118.dp)
                )

                // Info details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AuraPrimary.copy(alpha = if (isDark) 0.22f else 0.15f),
                        border = BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.40f)),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = item.badgeLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraPrimary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.2).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.songCountText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.songCountText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AuraPrimary.copy(alpha = 0.90f)
                        )
                    }
                }

                // Floating circular Play Button
                Surface(
                    shape = CircleShape,
                    color = AuraPrimary,
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Featured",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * B. Trending Songs Horizontal Section (Sourced from YouTube Music / Curated)
 * Distinguishes local vs online availability.
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
            eyebrow = "YOUTUBE MUSIC CHARTS",
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
    cardWidth: Dp = 148.dp
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.58f) else Color.Black.copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth)
                .fillMaxWidth()
        ) {
            AuraArtwork(
                model = item.onlineSong.artworkUrl,
                size = cardWidth.value.toInt(),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                elevation = 6.dp
            )

            // Availability Badge
            val (badgeBg, badgeText, badgeColor) = if (item.isLocalAvailable) {
                Triple(Color(0xE610B981), "LOCAL", Color.White)
            } else {
                Triple(Color(0xD9000000), "STREAM", Color(0xFF00E5FF))
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (item.isLocalAvailable) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(10.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.onlineSong.title,
            fontSize = 13.5.sp,
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
 * C. Artists Section
 * Rounded-square artwork cards arranged horizontally with artist name beneath.
 */
@Composable
fun ArtistsSection(
    artists: List<ArtistItem>,
    onArtistClick: (ArtistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) return

    Column(modifier = modifier.padding(top = 26.dp)) {
        HomeSectionHeader(
            eyebrow = "ARTIST CLOUD",
            title = "Artist Discovery"
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
    size: Dp = 148.dp
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val ringColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prominent rounded-square artist portrait matching Trending Now dimensions
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1B1920) else Color(0xFFECEEF2),
            border = BorderStroke(1.dp, ringColor),
            modifier = Modifier
                .size(size)
                .shadow(6.dp, RoundedCornerShape(20.dp))
        ) {
            AuraArtwork(
                model = artist.artworkModel,
                size = size.value.toInt(),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                elevation = 0.dp,
                fallbackIcon = Icons.Default.Person
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Strictly single artist name with clean typography (song counter removed)
        Text(
            text = artist.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Bottom Sheet displaying dedicated Apple Music-inspired artist profile and playable songs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailSheet(
    artist: ArtistItem,
    onlineSongs: List<OnlineSong>,
    localSongs: List<Song>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayOnlineSong: (OnlineSong, List<OnlineSong>) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit = onShuffleAll
) {
    val isDark = LocalIsDark.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF141318) else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
        ) {
            // Apple Music-inspired Editorial Artist Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF222028) else Color(0xFFE5E7EB),
                    border = BorderStroke(2.dp, AuraPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(12.dp, CircleShape)
                ) {
                    AuraArtwork(
                        model = artist.artworkModel,
                        size = 110,
                        shape = CircleShape,
                        elevation = 0.dp,
                        fallbackIcon = Icons.Default.Person,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = artist.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (artist.isOnline) "YouTube Music Artist Profile" else "Local Library Artist",
                    fontSize = 12.5.sp,
                    color = AuraPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${onlineSongs.size + localSongs.size} Tracks available",
                    fontSize = 11.5.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Play All & Shuffle Dual Actions
                if (onlineSongs.isNotEmpty() || localSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play All",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShuffleAll,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDark) Color(0xFF201E26) else Color(0xFFF0F1F5)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AuraPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fetching tracks from YouTube Music...",
                            fontSize = 13.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (onlineSongs.isEmpty() && localSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks found for ${artist.name}",
                        fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local songs section
                    if (localSongs.isNotEmpty()) {
                        Text(
                            text = "LOCAL SONGS (${localSongs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraPrimary,
                            letterSpacing = 0.8.sp
                        )
                        localSongs.forEach { lSong ->
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
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Online YouTube songs section
                    if (onlineSongs.isNotEmpty()) {
                        Text(
                            text = "YOUTUBE MUSIC SONGS (${onlineSongs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 0.8.sp
                        )
                        onlineSongs.forEach { oSong ->
                            val track = QuickHitTrack.OnlineTrack(oSong, "YouTube")
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * D. Quick Hits Section
 * Draggable horizontally from right to left across 3-row stacked columns (like Quick Picks in Home tab).
 * Interleaves BOTH offline (local library) and online trending & category top hits.
 */
@Composable
fun QuickHitsSection(
    columns: List<List<QuickHitTrack>>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onTrackClick: (QuickHitTrack) -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (columns.isEmpty()) return

    Column(modifier = modifier.padding(top = 26.dp)) {
        HomeSectionHeader(
            eyebrow = "ONLINE & LOCAL HITS",
            title = "Quick Hits"
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
        targetValue = when {
            isActive -> AuraPrimary.copy(alpha = if (isDark) 0.18f else 0.12f)
            isDark -> Color(0xFF1B181E).copy(alpha = 0.85f)
            else -> Color(0xFFF3F3F7)
        },
        animationSpec = tween(200),
        label = "quickHitBg"
    )

    val badgeColor = remember(track.sourceBadge) {
        when {
            track.sourceBadge.contains("Local", ignoreCase = true) -> Color(0xFF10B981)
            track.sourceBadge.contains("Recent", ignoreCase = true) -> Color(0xFF10B981)
            track.sourceBadge.contains("Pop", ignoreCase = true) -> Color(0xFF00E5FF)
            track.sourceBadge.contains("Rock", ignoreCase = true) -> Color(0xFF38BDF8)
            track.sourceBadge.contains("Bollywood", ignoreCase = true) -> Color(0xFFFF70A6)
            track.sourceBadge.contains("Chart", ignoreCase = true) -> Color(0xFFFFB703)
            track.sourceBadge.contains("Billboard", ignoreCase = true) -> Color(0xFFFFB703)
            track.sourceBadge.contains("Electronic", ignoreCase = true) -> Color(0xFFD500F9)
            else -> AuraPrimary
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = itemBg,
        border = if (isActive) BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.50f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-quality artwork thumbnail
            AuraArtwork(
                model = track.artworkModel,
                size = 46,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(9.dp),
                elevation = 2.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) AuraPrimary else if (isDark) Color.White else Color(0xFF1C1C1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.60f),
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
                    tint = AuraPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AuraPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = AuraPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            if (track is QuickHitTrack.LocalTrack) {
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = if (isDark) Color.White.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * E. Discover by Category (Genres & Moods)
 * Compact rounded cards inspired by the Dribbble reference mesh gradient aesthetics.
 */
@Composable
fun DiscoverCategorySection(
    categories: List<CategoryDiscoverItem>,
    onCategoryClick: (CategoryDiscoverItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    Column(modifier = modifier.padding(top = 26.dp, bottom = 18.dp)) {
        HomeSectionHeader(
            eyebrow = "EXPLORE GENRES & MOODS",
            title = "Discover by Category"
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
    cardWidth: Dp = 142.dp,
    cardHeight: Dp = 86.dp
) {
    val isDark = LocalIsDark.current
    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.04f))
        )
    }

    Surface(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = category.gradientColors.first().copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            category.gradientColors.first().copy(alpha = if (isDark) 0.65f else 0.40f),
                            category.gradientColors.getOrElse(1) { category.gradientColors.first() }.copy(alpha = if (isDark) 0.35f else 0.20f),
                            if (isDark) Color(0xFF100F14) else Color(0xFFF0F1F5)
                        )
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = category.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF15151A),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.subtitle,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Bottom Sheet displaying songs for a selected category with Play & Shuffle options.
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
        containerColor = if (isDark) Color(0xFF141318) else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
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
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category.subtitle,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onShuffleAll,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
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
                    CircularProgressIndicator(color = AuraPrimary, modifier = Modifier.size(36.dp))
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
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onlineSongs.isNotEmpty()) {
                        onlineSongs.take(8).forEach { oSong ->
                            val track = QuickHitTrack.OnlineTrack(oSong, "Category Hit")
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
