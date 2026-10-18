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
import androidx.compose.ui.graphics.Path
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
import com.aman.auramusic.ui.theme.CustomPaletteValidator
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import com.aman.auramusic.ui.theme.PlayerColorScheme
import com.aman.auramusic.ui.theme.PlayerPaletteMode
import com.aman.auramusic.ui.theme.rememberPlayerColorScheme
import com.aman.auramusic.ui.theme.resolvePlayerPaletteMode
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

    val paletteMode = if (!appSettings.isPlayerThemeActive()) {
        PlayerPaletteMode.MONOCHROME
    } else {
        val style = appSettings.themeColorStyle.ifBlank { appSettings.playerThemeMode }
        when {
            style.equals("Dynamic Artwork", ignoreCase = true) -> PlayerPaletteMode.DYNAMIC_ARTWORK
            style.equals("Custom", ignoreCase = true) || style.equals("Custom Palette", ignoreCase = true) -> PlayerPaletteMode.CUSTOM
            else -> {
                val preset = appSettings.appColorPreset.ifBlank { appSettings.colorPreset }
                if (preset.equals("Monochrome", ignoreCase = true)) {
                    PlayerPaletteMode.MONOCHROME
                } else {
                    PlayerPaletteMode.PRESET
                }
            }
        }
    }

    val activePreset = remember(appSettings.appColorPreset, appSettings.colorPreset) {
        val p = appSettings.appColorPreset.ifBlank { appSettings.colorPreset }
        if (p.equals("Monochrome", ignoreCase = true)) "Midnight Indigo" else p
    }
    val activeHexes = remember(appSettings.appCustomPaletteHexes, appSettings.customPaletteHexes) {
        appSettings.appCustomPaletteHexes.ifBlank { appSettings.customPaletteHexes }
    }

    val initialPalette = remember(isDark, paletteMode, activePreset, activeHexes) {
        when (paletteMode) {
            PlayerPaletteMode.DYNAMIC_ARTWORK -> PlayerArtworkPalette.neutral(isDark)
            PlayerPaletteMode.PRESET -> PlayerArtworkPalette.fromPreset(activePreset, isDark)
            PlayerPaletteMode.CUSTOM -> CustomPaletteValidator.createPaletteFromCustomHexes(activeHexes, isDark)
            PlayerPaletteMode.MONOCHROME -> PlayerArtworkPalette.neutral(isDark)
        }
    }

    var artworkPalette by remember(initialPalette) { mutableStateOf(initialPalette) }

    LaunchedEffect(
        song.id,
        song.artworkUri,
        song.uri,
        isDark,
        paletteMode,
        activePreset,
        activeHexes
    ) {
        when (paletteMode) {
            PlayerPaletteMode.DYNAMIC_ARTWORK -> {
                val extracted = DynamicArtworkColorsHelper.extractPalette(context, song, isDark)
                artworkPalette = extracted
            }
            PlayerPaletteMode.PRESET -> {
                artworkPalette = PlayerArtworkPalette.fromPreset(activePreset, isDark)
            }
            PlayerPaletteMode.CUSTOM -> {
                artworkPalette = CustomPaletteValidator.createPaletteFromCustomHexes(activeHexes, isDark)
            }
            PlayerPaletteMode.MONOCHROME -> {
                artworkPalette = PlayerArtworkPalette.neutral(isDark)
            }
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

    val isColored = paletteMode.isColored

    val playerColors = rememberPlayerColorScheme(
        isDynamic = isColored,
        isDark = isDark,
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary,
        accent = animatedAccent,
        paletteMode = paletteMode
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
            enabled = isColored,
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

        // 1. Restrained primary-to-secondary gradient base
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.55f else 0.38f),
                    secondaryColor.copy(alpha = if (isDark) 0.45f else 0.28f)
                ),
                start = Offset(0f, 0f),
                end = Offset(width * 0.5f, height)
            )
        )

        // 2. Large, soft radial primary bloom near the upper/artwork region
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.60f else 0.40f),
                    primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                    Color.Transparent
                ),
                center = Offset(width * 0.75f, height * 0.25f),
                radius = width * 1.20f
            ),
            center = Offset(width * 0.75f, height * 0.25f),
            radius = width * 1.20f
        )

        // 3. Large, soft radial secondary bloom in the lower-middle region for dimensional depth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = if (isDark) 0.50f else 0.30f),
                    secondaryColor.copy(alpha = if (isDark) 0.18f else 0.08f),
                    Color.Transparent
                ),
                center = Offset(width * 0.20f, height * 0.70f),
                radius = width * 1.15f
            ),
            center = Offset(width * 0.20f, height * 0.70f),
            radius = width * 1.15f
        )

        // 4. Contrast preservation scrim
        // Soft gradient overlay to keep album artwork, lyrics, and controls visually dominant
        val scrimColors = if (isDark) {
            listOf(
                Color.Black.copy(alpha = 0.35f),
                Color.Black.copy(alpha = 0.48f),
                Color.Black.copy(alpha = 0.62f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.55f)
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
                val queueExtractedColor = playerColors.dialogAccent
                val queueIconActiveColor = if (playerColors.isDynamic) queueExtractedColor else MaterialTheme.colorScheme.onBackground
                val queueIconInactiveColor = if (playerColors.isDynamic) {
                    queueExtractedColor.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onShuffleToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) queueIconActiveColor else queueIconInactiveColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRepeatToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.NONE) queueIconActiveColor else queueIconInactiveColor,
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
            Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = playerColors.queueItemInactiveTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = playerColors.queueItemInactiveArtist, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    if (active) Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Currently playing",
                        tint = playerColors.queueItemActiveContent,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (active) playerColors.queueItemActiveContent else playerColors.queueItemInactiveTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) playerColors.queueItemActiveContent.copy(alpha = 0.82f) else playerColors.queueItemInactiveArtist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

            // Compact, squared-off Hi-Res/Lossless badge inspired by the supplied Figma component.
            val badgeShape = RoundedCornerShape(5.dp)
            Box(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(playerColors.capsuleSurface, badgeShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, playerColors.capsuleBorder), badgeShape)
                    .clickable { showQ = true }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (q.badge.contains("Lossless", ignoreCase = true) || q.badge.contains("Stream", ignoreCase = true)) {
                        LosslessMark(
                            tint = playerColors.capsuleContent,
                            modifier = Modifier.size(width = 13.dp, height = 11.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = q.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
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

        // Figma-matched Apple Music-inspired transport controls: standalone glyphs, no circular button backgrounds.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                TransportGlyph(
                    type = TransportGlyphType.Previous,
                    tint = playerColors.buttonIconTint,
                    modifier = Modifier.size(width = 50.dp, height = 29.dp)
                )
            }

            val playPauseColor = playerColors.prominentSurface
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Canvas(modifier = Modifier.size(width = 43.dp, height = 44.dp)) {
                        val barWidth = size.width * 0.30f
                        val barGap = size.width * 0.16f
                        val radius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f)
                        drawRoundRect(
                            color = playPauseColor,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            color = playPauseColor,
                            topLeft = Offset(barWidth + barGap, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                    }
                } else {
                    TransportGlyph(
                        type = TransportGlyphType.Play,
                        tint = playPauseColor,
                        modifier = Modifier.size(width = 43.dp, height = 44.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                TransportGlyph(
                    type = TransportGlyphType.Next,
                    tint = playerColors.buttonIconTint,
                    modifier = Modifier.size(width = 50.dp, height = 29.dp)
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

private enum class TransportGlyphType { Previous, Play, Next }

/**
 * Vector-drawn transport glyphs matching the silhouettes in Figma node 2017:76.
 * Drawing paths directly keeps the shape crisp at different screen densities.
 */
@Composable
private fun TransportGlyph(
    type: TransportGlyphType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        when (type) {
            TransportGlyphType.Play -> {
                val sx = size.width / 43f
                val sy = size.height / 44f
                val path = Path().apply {
                    moveTo(7.00815f, 0.791516f)
                    cubicTo(3.10361f, -1.23592f, 0f, 0.791516f, 0f, 5.04912f)
                    lineTo(0f, 38.9072f)
                    cubicTo(0f, 43.1649f, 3.30384f, 45.2937f, 7.00815f, 43.1649f)
                    lineTo(39.8463f, 25.2221f)
                    cubicTo(44.0512f, 22.9919f, 44.0512f, 20.1535f, 39.8463f, 17.9233f)
                    close()
                }
                drawContext.transform.scale(sx, sy, pivot = Offset.Zero)
                drawPath(path, tint)
                drawContext.transform.scale(1f / sx, 1f / sy, pivot = Offset.Zero)
            }
            TransportGlyphType.Previous -> {
                val sx = size.width / 50f
                val sy = size.height / 29f
                val path = Path().apply {
                    moveTo(22.1468f, 0.580642f)
                    cubicTo(23.8562f, -0.470659f, 25.264f, 0.685773f, 25.264f, 2.68325f)
                    lineTo(25.264f, 11.1988f)
                    lineTo(46.0784f, 0.580642f)
                    cubicTo(47.9889f, -0.78605f, 50f, 0.370382f, 50f, 2.89351f)
                    lineTo(50f, 25.8119f)
                    cubicTo(50f, 28.4401f, 47.9889f, 29.8068f, 46.0784f, 28.4401f)
                    lineTo(25.264f, 17.1912f)
                    lineTo(25.264f, 26.0221f)
                    cubicTo(25.264f, 28.5453f, 23.6551f, 29.8068f, 21.644f, 28.4401f)
                    lineTo(1.73454f, 16.7707f)
                    cubicTo(-0.57818f, 15.404f, -0.57818f, 13.4065f, 1.73454f, 11.9347f)
                    close()
                }
                drawContext.transform.scale(sx, sy, pivot = Offset.Zero)
                drawPath(path, tint)
                drawContext.transform.scale(1f / sx, 1f / sy, pivot = Offset.Zero)
            }
            TransportGlyphType.Next -> {
                val sx = size.width / 50f
                val sy = size.height / 29f
                val path = Path().apply {
                    moveTo(3.82102f, 0.439687f)
                    cubicTo(1.91051f, -0.722481f, 0f, 0.545339f, 0f, 2.76402f)
                    lineTo(0f, 25.9017f)
                    cubicTo(0f, 28.4374f, 1.91051f, 29.8108f, 3.92157f, 28.4374f)
                    lineTo(24.736f, 17.1326f)
                    lineTo(24.736f, 26.0074f)
                    cubicTo(24.736f, 28.543f, 26.3449f, 29.8108f, 28.356f, 28.4374f)
                    lineTo(48.2655f, 16.71f)
                    cubicTo(50.5782f, 15.3366f, 50.5782f, 13.3292f, 48.2655f, 11.8501f)
                    lineTo(27.8532f, 0.439687f)
                    cubicTo(26.1438f, -0.616829f, 24.736f, 0.545339f, 24.736f, 2.55272f)
                    lineTo(24.736f, 11.1105f)
                    close()
                }
                drawContext.transform.scale(sx, sy, pivot = Offset.Zero)
                drawPath(path, tint)
                drawContext.transform.scale(1f / sx, 1f / sy, pivot = Offset.Zero)
            }
        }
    }
}

/** Small diagonal-stripe lossless mark from the Figma badge. */
@Composable
private fun LosslessMark(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 14f
        val sy = size.height / 11f
        val strokes = listOf(
            listOf(0.416f, 2.265f, 4.416f, 8.265f),
            listOf(2.416f, 0.265f, 7.416f, 10.265f),
            listOf(5.416f, 0.265f, 10.416f, 10.265f),
            listOf(8.416f, 0.265f, 13.416f, 8.265f),
            listOf(11.416f, 1.265f, 13.416f, 5.265f)
        )
        strokes.forEach { p ->
            drawLine(
                color = tint,
                start = Offset(p[0] * sx, p[1] * sy),
                end = Offset(p[2] * sx, p[3] * sy),
                strokeWidth = 1.dp.toPx()
            )
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
