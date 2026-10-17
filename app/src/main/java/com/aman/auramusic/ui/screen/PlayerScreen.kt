package com.aman.auramusic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import coil.request.ImageRequest
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.util.ArtworkQualityOptimizer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.aman.auramusic.playback.RepeatMode
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import com.aman.auramusic.ui.theme.PlayerColorScheme
import com.aman.auramusic.ui.theme.rememberPlayerColorScheme
import com.aman.auramusic.util.audioQuality
import com.aman.auramusic.util.formatDuration
import com.aman.auramusic.viewmodel.PlayerViewModel
import com.aman.auramusic.viewmodel.QueueEntry
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    lyrics: List<LyricLine>,
    queue: List<QueueEntry>,
    history: List<Song> = emptyList(),
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onQueueRemove: (Song) -> Unit,
    onQueueClear: () -> Unit,
    onQueueSave: (() -> Unit)? = null,
    onHistoryClear: () -> Unit,
    onAddToPlaylist: () -> Unit,
    appSettings: AppSettings,
    playerViewModel: PlayerViewModel = viewModel(),
    isFavorite: Boolean = playerViewModel.isFavorite,
    onFavoriteToggle: () -> Unit = { playerViewModel.toggleFavorite() },
    isShuffled: Boolean = playerViewModel.isShuffled,
    repeatMode: RepeatMode = playerViewModel.repeatMode,
    onToggleShuffle: () -> Unit = { playerViewModel.toggleShuffle() },
    onToggleRepeat: () -> Unit = { playerViewModel.toggleRepeat() },
    onAddToQueue: ((Song) -> Unit)? = null,
    onMoveQueueItem: ((Int, Int) -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val artworkModel = remember(song.id, song.uri, song.artworkUri) {
        if (song.id == -1L && song.uri.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(song.uri)
                .crossfade(true)
                .build()
        } else {
            song.artworkUri
        }
    }

    val isDark = LocalIsDark.current
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    var artworkPalette by remember(isDark) { mutableStateOf(PlayerArtworkPalette.neutral(isDark)) }

    LaunchedEffect(song.id, song.artworkUri, song.uri, isDark, appSettings.dynamicArtworkColors) {
        if (appSettings.dynamicArtworkColors) {
            val extracted = DynamicArtworkColorsHelper.extractPalette(context, song, isDark)
            artworkPalette = extracted
        } else {
            artworkPalette = PlayerArtworkPalette.neutral(isDark)
        }
    }

    val animatedPrimary by animateColorAsState(
        targetValue = artworkPalette.primary,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientPrimaryAnimation"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = artworkPalette.secondary,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientSecondaryAnimation"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = artworkPalette.tertiary,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientTertiaryAnimation"
    )
    val animatedAccent by animateColorAsState(
        targetValue = artworkPalette.accent,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "ambientAccentAnimation"
    )

    val playerColors = rememberPlayerColorScheme(
        isDynamic = appSettings.dynamicArtworkColors,
        isDark = isDark,
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary,
        accent = animatedAccent
    )

    val offsetY = remember { Animatable(0f) }
    
    val scaleFactor = remember(offsetY.value) { (1f - (offsetY.value / 3000f)).coerceIn(0.9f, 1f) }
    val cornerRadius = remember(offsetY.value) { (offsetY.value / 20f).coerceIn(0f, 32f).dp }
    
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var dragStartedInHeader by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scaleFactor)
            .clip(RoundedCornerShape(cornerRadius))
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragStartedInHeader = offset.y < 120.dp.toPx() },
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value > 400f) { onBack(); offsetY.snapTo(0f) }
                            else { offsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
                        }
                        dragStartedInHeader = false
                    },
                    onDragCancel = { scope.launch { offsetY.animateTo(0f, tween(300)) }; dragStartedInHeader = false },
                    onVerticalDrag = { change, dragAmount ->
                        if (pagerState.currentPage == 1 || offsetY.value > 0f || dragStartedInHeader) {
                            change.consume()
                            scope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f)) }
                        }
                    }
                )
            }
    ) {
        PlayerBackground(
            enabled = appSettings.dynamicArtworkColors,
            primaryColor = animatedPrimary,
            secondaryColor = animatedSecondary,
            tertiaryColor = animatedTertiary,
            accentColor = animatedAccent,
            isDark = isDark
        )

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepTimerDialog = false }, 
                onSelect = { playerViewModel.setSleepTimer(it); showSleepTimerDialog = false },
                remainingMs = playerViewModel.sleepTimerRemaining,
                playerColors = playerColors
            )
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            PlayerHeader(
                song = song,
                onBack = onBack,
                isLyricsPage = pagerState.currentPage != 1,
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                onAddToPlaylist = onAddToPlaylist,
                onAddToQueue = { onAddToQueue?.invoke(song) ?: playerViewModel.addToQueue(song) },
                onShowSleepTimer = { showSleepTimerDialog = true },
                onOpenDiagnostics = onOpenDiagnostics,
                playerColors = playerColors
            )

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top, beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> LyricsPage(lyrics, position, onSeek, appSettings.lyricFontScale, appSettings.karaokeMode)
                    1 -> NowPlayingPage(
                        song = song,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        onFavoriteToggle = onFavoriteToggle,
                        onAddToPlaylist = onAddToPlaylist,
                        onAddToQueue = { onAddToQueue?.invoke(song) ?: playerViewModel.addToQueue(song) },
                        onShowSleepTimer = { showSleepTimerDialog = true },
                        onOpenDiagnostics = onOpenDiagnostics,
                        playerColors = playerColors
                    )
                    2 -> QueuePage(
                        queue = queue,
                        history = history,
                        currentSongId = song.id,
                        isShuffled = isShuffled,
                        repeatMode = repeatMode,
                        isCurrentPage = pagerState.currentPage == 2,
                        onShuffleToggle = onToggleShuffle,
                        onRepeatToggle = onToggleRepeat,
                        onSongSelected = onSongSelected,
                        onQueueRemove = onQueueRemove,
                        onQueueClear = onQueueClear,
                        onQueueSave = onQueueSave,
                        onHistoryClear = onHistoryClear,
                        onMove = onMoveQueueItem ?: { from, to -> playerViewModel.moveQueueItem(from, to) },
                        playerColors = playerColors
                    )
                }
            }

            AnimatedVisibility(
                visible = pagerState.currentPage == 1,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { it }, 
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(400)) + slideOutVertically(
                    targetOffsetY = { it }, 
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                )
            ) {
                PlayerControls(
                    song = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    currentPage = pagerState.currentPage,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onPageToggle = { targetPage -> scope.launch { if (pagerState.currentPage == targetPage) pagerState.animateScrollToPage(1) else pagerState.animateScrollToPage(targetPage) } },
                    playerColors = playerColors
                )
            }
        }
    }
}

@Composable
private fun PlayerBackground(
    enabled: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    accentColor: Color,
    isDark: Boolean
) {
    if (!enabled) {
        // Strict monochrome restoration when disabled - zero residual tinting
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    // Dynamic Apple Music-style ambient background with GPU-accelerated layered radial gradients
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0D0D0D) else Color(0xFFF7F7F7))
    ) {
        val width = size.width
        val height = size.height

        // 1. Base ambient diagonal blend
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.65f else 0.45f),
                    secondaryColor.copy(alpha = if (isDark) 0.55f else 0.35f),
                    tertiaryColor.copy(alpha = if (isDark) 0.60f else 0.40f)
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        // 2. Top-right prominent bloom (Primary)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.70f else 0.50f),
                    primaryColor.copy(alpha = if (isDark) 0.35f else 0.20f),
                    Color.Transparent
                ),
                center = Offset(width * 0.85f, height * 0.22f),
                radius = width * 1.1f
            ),
            center = Offset(width * 0.85f, height * 0.22f),
            radius = width * 1.1f
        )

        // 3. Middle-left counter-bloom (Secondary)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = if (isDark) 0.65f else 0.45f),
                    secondaryColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                    Color.Transparent
                ),
                center = Offset(width * 0.10f, height * 0.52f),
                radius = width * 1.05f
            ),
            center = Offset(width * 0.10f, height * 0.52f),
            radius = width * 1.05f
        )

        // 4. Bottom-right subtle accent bloom (Accent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDark) 0.50f else 0.35f),
                    Color.Transparent
                ),
                center = Offset(width * 0.75f, height * 0.82f),
                radius = width * 0.95f
            ),
            center = Offset(width * 0.75f, height * 0.82f),
            radius = width * 0.95f
        )

        // 5. Contrast preservation scrim
        // Soft gradient overlay to guarantee text and controls remain legible and accessible
        val scrimColors = if (isDark) {
            listOf(
                Color.Black.copy(alpha = 0.42f),
                Color.Black.copy(alpha = 0.55f),
                Color.Black.copy(alpha = 0.68f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.62f)
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = scrimColors,
                startY = 0f,
                endY = height
            )
        )
    }
}

@Composable
private fun PlayerHeader(
    song: Song,
    onBack: () -> Unit,
    isLyricsPage: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    playerColors: PlayerColorScheme
) {
    var showMenu by remember { mutableStateOf(value = false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLyricsPage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(song, size = 48, shape = RoundedCornerShape(8.dp), elevation = 0.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = playerColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = playerColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play/Pause button in mini mode
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(playerColors.buttonSurface, CircleShape)
                            .border(1.dp, playerColors.buttonBorder, CircleShape)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = playerColors.buttonIconTint,
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (!isPlaying) Modifier.offset(x = 1.dp) else Modifier)
                        )
                    }

                    // Favorite (Like) button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFavorite) playerColors.prominentSurface else playerColors.buttonSurface,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isFavorite) playerColors.prominentBorder else playerColors.buttonBorder,
                                CircleShape
                            )
                            .clickable(onClick = onFavoriteToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) playerColors.prominentIconTint else playerColors.buttonIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3-dot (More options) button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(playerColors.buttonSurface, CircleShape)
                                .border(1.dp, playerColors.buttonBorder, CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Menu",
                                tint = playerColors.buttonIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) playerColors.dialogAccent else if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { onFavoriteToggle(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { onAddToPlaylist(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { onAddToQueue(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { onShowSleepTimer(); showMenu = false }
                        )
                        if (onOpenDiagnostics != null) {
                            DropdownMenuItem(
                                text = { Text("Stream Quality", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = { onOpenDiagnostics(); showMenu = false }
                            )
                        }
                    }
                }
            }
        }
    } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(playerColors.dragHandle)
                )
            }
        }
    }
}

@Composable
private fun NowPlayingPage(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    playerColors: PlayerColorScheme
) {
    val artworkScale by animateFloatAsState(if (isPlaying) 1f else 0.96f, label = "artworkScale")
    var showMenu by remember { mutableStateOf(value = false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        SongArtwork(
            song = song,
            size = 314,
            modifier = Modifier
                .scale(artworkScale)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            elevation = 16.dp
        )
        Spacer(modifier = Modifier.height(34.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(song.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = playerColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Spacer(modifier = Modifier.height(5.dp))
                Text(song.artist, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = playerColors.textSecondary, maxLines = 1, modifier = Modifier.basicMarquee())
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) playerColors.prominentSurface else playerColors.buttonSurface,
                        CircleShape
                    )
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isFavorite) playerColors.prominentBorder else playerColors.buttonBorder
                        ),
                        CircleShape
                    )
                    .clickable(onClick = onFavoriteToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) playerColors.prominentIconTint else playerColors.buttonIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(playerColors.buttonSurface, CircleShape)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.buttonBorder), CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Menu",
                        tint = playerColors.buttonIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) playerColors.dialogAccent else if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { onFavoriteToggle(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { onAddToPlaylist(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { onAddToQueue(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep Timer", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { onShowSleepTimer(); showMenu = false }
                    )
                    if (onOpenDiagnostics != null) {
                        DropdownMenuItem(
                            text = { Text("Stream Quality", fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { onOpenDiagnostics(); showMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPage(lyrics: List<LyricLine>, position: Long, onSeek: (Long) -> Unit, fontScale: Float, karaokeMode: Boolean) {
    val activeIndex = lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            val screenHeight = configuration.screenHeightDp.dp
            val scrollOffset = with(density) { (screenHeight * 0.35f).roundToPx() }
            listState.animateScrollToItem(index = activeIndex, scrollOffset = -scrollOffset)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No synced lyrics found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                item { Spacer(modifier = Modifier.height(100.dp)) }
                itemsIndexed(lyrics) { index, line ->
                    val opacity by animateFloatAsState(if (index == activeIndex) 1f else if (karaokeMode) 0.08f else 0.25f, label = "lyricOpacity")
                    Text(line.text, style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = opacity), modifier = Modifier.fillMaxWidth().pointerInput(line.timeMs) { detectTapGestures(onTap = { onSeek(line.timeMs) }) })
                }
                item { Spacer(modifier = Modifier.height(240.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueuePage(
    queue: List<QueueEntry>,
    history: List<Song>,
    currentSongId: Long?,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    isCurrentPage: Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onQueueRemove: (Song) -> Unit,
    onQueueClear: () -> Unit,
    onQueueSave: (() -> Unit)?,
    onHistoryClear: () -> Unit,
    onMove: (Int, Int) -> Unit,
    playerColors: PlayerColorScheme
) {
    val listState = rememberLazyListState()
    val activeIndex = queue.indexOfFirst { it.song.id == currentSongId }.coerceAtLeast(0)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(draggingIndex, dragOffset) {
        if (draggingIndex != null) {
            val itemHeight = with(density) { 72.dp.toPx() }
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val draggedItem = visibleItems.find { it.key == queue[draggingIndex!!].id }
            if (draggedItem != null) {
                val viewportHeight = listState.layoutInfo.viewportSize.height
                if (draggedItem.offset + dragOffset < 100) listState.animateScrollBy(-itemHeight)
                else if (draggedItem.offset + draggedItem.size + dragOffset > viewportHeight - 100) listState.animateScrollBy(itemHeight)
            }
        }
    }

    LaunchedEffect(activeIndex, queue.size, isCurrentPage, history.size) {
        if (queue.isNotEmpty() && draggingIndex == null && isCurrentPage) {
            val historyCount = if (history.isNotEmpty()) history.size + 2 else 0
            listState.animateScrollToItem(historyCount + activeIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = playerColors.textPrimary, modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val canClear = queue.size > 1
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (canClear) playerColors.queueActionContent else playerColors.queueActionContent.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (playerColors.isDynamic) playerColors.queueActionSurface else Color.Transparent)
                        .border(
                            if (playerColors.isDynamic) androidx.compose.foundation.BorderStroke(1.dp, playerColors.buttonBorder.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(enabled = canClear) { onQueueClear() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                if (onQueueSave != null) {
                    val canSave = queue.isNotEmpty()
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (canSave) playerColors.queueActionContent else playerColors.queueActionContent.copy(alpha = 0.4f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (playerColors.isDynamic) playerColors.queueActionSurface else Color.Transparent)
                            .border(
                                if (playerColors.isDynamic) androidx.compose.foundation.BorderStroke(1.dp, playerColors.buttonBorder.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = canSave) { onQueueSave() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (playerColors.isDynamic) {
                                if (isShuffled) playerColors.queueActionActiveSurface else playerColors.queueActionSurface
                            } else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            if (playerColors.isDynamic) androidx.compose.foundation.BorderStroke(1.dp, if (isShuffled) playerColors.prominentBorder else playerColors.buttonBorder.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                            CircleShape
                        )
                        .clickable(onClick = onShuffleToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) {
                            if (playerColors.isDynamic) playerColors.queueActionActiveContent else MaterialTheme.colorScheme.onBackground
                        } else {
                            if (playerColors.isDynamic) playerColors.queueActionContent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (playerColors.isDynamic) {
                                if (repeatMode != RepeatMode.NONE) playerColors.queueActionActiveSurface else playerColors.queueActionSurface
                            } else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            if (playerColors.isDynamic) androidx.compose.foundation.BorderStroke(1.dp, if (repeatMode != RepeatMode.NONE) playerColors.prominentBorder else playerColors.buttonBorder.copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                            CircleShape
                        )
                        .clickable(onClick = onRepeatToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.NONE) {
                            if (playerColors.isDynamic) playerColors.queueActionActiveContent else MaterialTheme.colorScheme.onBackground
                        } else {
                            if (playerColors.isDynamic) playerColors.queueActionContent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (history.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("History", style = MaterialTheme.typography.labelLarge, color = playerColors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium, color = playerColors.textPrimary, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onHistoryClear() }.padding(8.dp))
                    }
                }
                itemsIndexed(history, key = { index, song -> "history_${index}_${song.id}" }) { _, song -> HistoryItem(song, { onSongSelected(song) }, playerColors, Modifier.animateItem()) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            itemsIndexed(queue, key = { _, entry -> entry.id }) { index, entry ->
                val isDragging = draggingIndex == index
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                QueueItem(
                    song = entry.song,
                    active = entry.song.id == currentSongId,
                    isDragging = isDragging,
                    elevation = elevation,
                    onSongSelected = { onSongSelected(entry.song) },
                    onRemove = { onQueueRemove(entry.song) },
                    onMove = { change, dragAmount ->
                        change.consume(); dragOffset += dragAmount.y
                        val h = with(density) { 72.dp.toPx() }
                        while (dragOffset > h * 0.4f && draggingIndex != null && draggingIndex!! < queue.size - 1) {
                            val c = draggingIndex!!; onMove(c, c + 1); draggingIndex = c + 1; dragOffset -= h; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        while (dragOffset < -h * 0.4f && draggingIndex != null && draggingIndex!! > 0) {
                            val c = draggingIndex!!; onMove(c, c - 1); draggingIndex = c - 1; dragOffset += h; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragStart = { draggingIndex = index; dragOffset = 0f; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragEnd = { draggingIndex = null; dragOffset = 0f },
                    playerColors = playerColors,
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
                        .graphicsLayer { if (isDragging) { translationY = dragOffset; scaleX = 1.1f; scaleY = 1.1f; rotationZ = 1.5f; shadowElevation = 24f; alpha = 0.98f } }.zIndex(if (isDragging) 100f else 0f)
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(song: Song, onClick: () -> Unit, playerColors: PlayerColorScheme, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        SongArtwork(song, size = 40, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = playerColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = playerColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    active: Boolean,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onSongSelected: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onMove: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit,
    playerColors: PlayerColorScheme,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    Surface(
        color = if (active) {
            playerColors.queueItemActiveSurface
        } else if (isDragging) {
            playerColors.queueItemDraggingSurface
        } else {
            Color.Transparent
        },
        border = if (active && playerColors.isDynamic) {
            androidx.compose.foundation.BorderStroke(1.dp, playerColors.queueItemActiveBorder)
        } else null,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = elevation,
        modifier = modifier.fillMaxWidth().height(72.dp).clickable { onSongSelected() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongArtwork(song, size = 52, shape = RoundedCornerShape(10.dp))
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) Icon(Icons.Default.PlayArrow, null, tint = playerColors.queueItemActiveContent, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, color = if (active) playerColors.queueItemActiveContent else playerColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(song.artist, style = MaterialTheme.typography.labelMedium, color = if (active) playerColors.queueItemActiveContent.copy(alpha = 0.8f) else playerColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from queue",
                        tint = if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(modifier = Modifier.size(40.dp).pointerInput(Unit) { detectDragGesturesAfterLongPress(onDragStart = { onDragStart() }, onDragEnd = { onDragEnd() }, onDragCancel = { onDragEnd() }, onDrag = { change, dragAmount -> onMove(change, dragAmount) }) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.DragIndicator, "Drag", tint = if (isDragging) (if (playerColors.isDynamic) playerColors.buttonIconTint else MaterialTheme.colorScheme.onSurface) else (if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit, 
    onSelect: (Int) -> Unit,
    remainingMs: Long = 0L,
    playerColors: PlayerColorScheme
) {
    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .clip(dialogShape)
                .background(playerColors.dialogSurface, dialogShape)
                .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.dialogBorder), dialogShape)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = playerColors.textPrimary
                )
                
                if (remainingMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (playerColors.isDynamic) playerColors.dialogAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Ends in ${formatDuration(remainingMs)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (playerColors.isDynamic) playerColors.dialogAccent else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val options = listOf(
                    "Off" to 0,
                    "15 minutes" to 15,
                    "30 minutes" to 30,
                    "45 minutes" to 45,
                    "1 hour" to 60,
                    "End of song" to -1
                )
                
                options.forEach { (label, value) ->
                    val isActive = if (value == -1) remainingMs == -1L else (value > 0 && remainingMs > 0)
                    
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isActive && value != 0) playerColors.dialogAccent else playerColors.textPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isActive && value != 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive && value != 0) playerColors.dialogAccent else playerColors.textPrimary
                            )
                            if (isActive && value != 0) {
                                Icon(Icons.Default.Star, null, tint = playerColors.dialogAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = playerColors.dialogAccent)
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    currentPage: Int,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPageToggle: (Int) -> Unit,
    playerColors: PlayerColorScheme
) {
    val rd = if (duration > 0) duration else song.duration
    val sv = if (rd > 0) position.toFloat() / rd.toFloat() else 0f
    val q = audioQuality(song)
    var showQ by remember { mutableStateOf(false) }
    if (showQ) QualityDetailsDialog(quality = q, onDismiss = { showQ = false }, playerColors = playerColors)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scrubber
        AppleMusicScrubber(
            progress = sv.coerceIn(0f, 1f),
            onScrub = { fraction -> onSeek((fraction * rd).toLong()) },
            trackColor = playerColors.progressTrack,
            progressColor = playerColors.progressIndicator
        )

        // Elapsed time, Quality capsule, Remaining time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = playerColors.textSecondary
            )

            // Audio Quality Capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(playerColors.capsuleSurface, RoundedCornerShape(999.dp))
                    .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.capsuleBorder), RoundedCornerShape(999.dp))
                    .clickable { showQ = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (q.badge.contains("Lossless") || q.badge.contains("Stream")) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = playerColors.capsuleContent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = q.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = playerColors.capsuleContent
                    )
                }
            }

            Text(
                text = "-${formatDuration((rd - position).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = playerColors.textSecondary
            )
        }

        Spacer(Modifier.height(26.dp))

        // Playback Controls (Previous, Play/Pause, Next) with translucent ~75% opacity surfaces
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button in controlBackground circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(playerColors.buttonSurface, CircleShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.buttonBorder), CircleShape)
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = playerColors.buttonIconTint,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Central Play/Pause in coordinated prominent surface
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(playerColors.prominentSurface, CircleShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.prominentBorder), CircleShape)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = playerColors.prominentIconTint,
                    modifier = Modifier.size(46.dp)
                )
            }

            // Next button in controlBackground circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(playerColors.buttonSurface, CircleShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.buttonBorder), CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = playerColors.buttonIconTint,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Secondary Actions (Lyrics, Queue)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(playerColors.capsuleSurface, RoundedCornerShape(999.dp))
                .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.capsuleBorder), RoundedCornerShape(999.dp))
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPageToggle(0) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = "Lyrics",
                        tint = if (currentPage == 0) playerColors.progressIndicator else if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { onPageToggle(2) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (currentPage == 2) playerColors.progressIndicator else if (playerColors.isDynamic) playerColors.buttonIconTint.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleMusicScrubber(
    progress: Float,
    onScrub: (Float) -> Unit,
    trackColor: Color = MaterialTheme.colorScheme.outline,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onScrub((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onScrub((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(progress.coerceIn(0.001f, 1f))
                    .background(progressColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - progress).coerceIn(0.001f, 1f))
            )
        }
    }
}

@Composable
private fun QualityDetailsDialog(quality: com.aman.auramusic.util.AudioQuality, onDismiss: () -> Unit, playerColors: PlayerColorScheme) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(playerColors.dialogSurface, RoundedCornerShape(28.dp))
                .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.dialogBorder), RoundedCornerShape(28.dp))
                .padding(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = null,
                    tint = if (playerColors.isDynamic) playerColors.dialogAccent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = quality.badge,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = playerColors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${quality.bitDepth}/${quality.sampleRate} • ${quality.format}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = playerColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "Close", color = playerColors.dialogAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
