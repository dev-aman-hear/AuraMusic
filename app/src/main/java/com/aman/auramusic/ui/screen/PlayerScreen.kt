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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.playback.RepeatMode
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.util.audioQuality
import com.aman.auramusic.util.formatDuration
import com.aman.auramusic.viewmodel.PlayerViewModel
import com.aman.auramusic.viewmodel.QueueEntry
import kotlinx.coroutines.delay
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
    onQueueSave: () -> Unit,
    onHistoryClear: () -> Unit,
    onAddToPlaylist: () -> Unit,
    appSettings: AppSettings,
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val dominantColorInt = playerViewModel.dominantColor
    val accentColorInt = playerViewModel.accentColor
    val offsetY = remember { Animatable(0f) }
    
    val scaleFactor = remember(offsetY.value) { (1f - (offsetY.value / 3000f)).coerceIn(0.9f, 1f) }
    val cornerRadius = remember(offsetY.value) { (offsetY.value / 20f).coerceIn(0f, 32f).dp }
    
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var dragStartedInHeader by remember { mutableStateOf(false) }
    
    val animatedColor by animateColorAsState(targetValue = Color(dominantColorInt), animationSpec = tween(800), label = "dominantColorAnimation")
    val animatedAccentColor by animateColorAsState(targetValue = Color(accentColorInt), animationSpec = tween(800), label = "accentColorAnimation")

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
        PlayerBackground(song, animatedColor, animatedAccentColor, appSettings.blurIntensity)

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepTimerDialog = false }, 
                onSelect = { playerViewModel.setSleepTimer(it); showSleepTimerDialog = false },
                remainingMs = playerViewModel.sleepTimerRemaining
            )
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            PlayerHeader(
                song = song,
                onBack = onBack,
                isLyricsPage = pagerState.currentPage != 1,
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                isFavorite = playerViewModel.isFavorite,
                onFavoriteToggle = { playerViewModel.toggleFavorite() },
                onAddToPlaylist = onAddToPlaylist,
                onShowSleepTimer = { showSleepTimerDialog = true }
            )

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top, beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> LyricsPage(lyrics, position, onSeek, appSettings.lyricFontScale, appSettings.karaokeMode)
                    1 -> NowPlayingPage(song, isPlaying, playerViewModel.isFavorite, { playerViewModel.toggleFavorite() }, onAddToPlaylist, { showSleepTimerDialog = true })
                    2 -> QueuePage(queue, history, song.id, playerViewModel.isShuffled, playerViewModel.repeatMode, pagerState.currentPage == 2, { playerViewModel.toggleShuffle() }, { playerViewModel.toggleRepeat() }, onSongSelected, onQueueRemove, onQueueClear, onQueueSave, onHistoryClear, { from, to -> playerViewModel.moveQueueItem(from, to) })
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
                PlayerControls(song, isPlaying, position, duration, pagerState.currentPage, onPlayPause, onSeek, onPrevious, onNext, { targetPage -> scope.launch { if (pagerState.currentPage == targetPage) pagerState.animateScrollToPage(1) else pagerState.animateScrollToPage(targetPage) } })
            }
        }
    }
}

@Composable
private fun PlayerBackground(song: Song, dominantColor: Color, accentColor: Color, blurIntensity: Int) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(model = song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(blurIntensity.dp).scale(2.5f), contentScale = ContentScale.Crop, alpha = 0.45f)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(dominantColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.7f), Color.Black))))
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
    onShowSleepTimer: () -> Unit
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
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song, size = 48, shape = RoundedCornerShape(8.dp), elevation = 0.dp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                // Play/Pause button in mini mode
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onFavoriteToggle, modifier = Modifier.background(if (isFavorite) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.1f), CircleShape).size(36.dp)) {
                    Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "Favorite", tint = if (isFavorite) Color.Black.copy(alpha = 0.65f) else Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(36.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Remove from Favorite" else "Add to Favorite") },
                            leadingIcon = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null) },
                            onClick = { onFavoriteToggle(); showMenu = false }
                        )
                        DropdownMenuItem(text = { Text("Add to Playlist") }, leadingIcon = { Icon(Icons.Default.LibraryMusic, null) }, onClick = { onAddToPlaylist(); showMenu = false })
                        DropdownMenuItem(text = { Text("Sleep Timer") }, leadingIcon = { Icon(Icons.Default.Bedtime, null) }, onClick = { onShowSleepTimer(); showMenu = false })
                    }
                }
            }
        } else { Box(modifier = Modifier.weight(1f).height(16.dp).clickable { onBack() }) }
    }
}

@Composable
private fun NowPlayingPage(song: Song, isPlaying: Boolean, isFavorite: Boolean, onFavoriteToggle: () -> Unit, onAddToPlaylist: () -> Unit, onShowSleepTimer: () -> Unit) {
    val artworkScale by animateFloatAsState(if (isPlaying) 1f else 0.85f, label = "artworkScale")
    var showMenu by remember { mutableStateOf(value = false) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        Spacer(modifier = Modifier.height(24.dp))
        SongArtwork(song, size = 280, modifier = Modifier.scale(artworkScale).clip(RoundedCornerShape(24.dp)))
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Spacer(modifier = Modifier.height(6.dp))
                Text(song.artist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.85f), maxLines = 1, modifier = Modifier.basicMarquee())
            }
            IconButton(onClick = onFavoriteToggle, modifier = Modifier.background(if (isFavorite) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.1f), CircleShape).size(40.dp)) {
                Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "Favorite", tint = if (isFavorite) Color.Black.copy(alpha = 0.65f) else Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(40.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.White, modifier = Modifier.size(22.dp)) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from Favorite" else "Add to Favorite") },
                        leadingIcon = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null) },
                        onClick = { onFavoriteToggle(); showMenu = false }
                    )
                    DropdownMenuItem(text = { Text("Add to Playlist") }, leadingIcon = { Icon(Icons.Default.LibraryMusic, null) }, onClick = { onAddToPlaylist(); showMenu = false })
                    DropdownMenuItem(text = { Text("Sleep Timer") }, leadingIcon = { Icon(Icons.Default.Bedtime, null) }, onClick = { onShowSleepTimer(); showMenu = false })
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No synced lyrics found", color = Color.White.copy(alpha = 0.5f)) }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                item { Spacer(modifier = Modifier.height(100.dp)) }
                itemsIndexed(lyrics) { index, line ->
                    val opacity by animateFloatAsState(if (index == activeIndex) 1f else if (karaokeMode) 0.08f else 0.25f, label = "lyricOpacity")
                    Text(line.text, style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = opacity), modifier = Modifier.fillMaxWidth().pointerInput(line.timeMs) { detectTapGestures(onTap = { onSeek(line.timeMs) }) })
                }
                item { Spacer(modifier = Modifier.height(240.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueuePage(
    queue: List<QueueEntry>, history: List<Song>, currentSongId: Long?, isShuffled: Boolean, repeatMode: RepeatMode, isCurrentPage: Boolean,
    onShuffleToggle: () -> Unit, onRepeatToggle: () -> Unit, onSongSelected: (Song) -> Unit, onQueueRemove: (Song) -> Unit, onQueueClear: () -> Unit, onQueueSave: () -> Unit, onHistoryClear: () -> Unit, onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val activeIndex = queue.indexOfFirst { it.song.id == currentSongId }.coerceAtLeast(0)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

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
            Text("Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShuffleToggle, modifier = Modifier.padding(end = 4.dp).size(32.dp).background(if (isShuffled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onRepeatToggle, modifier = Modifier.padding(end = 8.dp).size(32.dp).background(if (repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)) { Icon(imageVector = when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat }, contentDescription = "Repeat", tint = if (repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (history.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("History", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onHistoryClear() }.padding(8.dp))
                    }
                }
                itemsIndexed(history, key = { index, song -> "history_${index}_${song.id}" }) { _, song -> HistoryItem(song, { onSongSelected(song) }, Modifier.animateItem()) }
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
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
                        .graphicsLayer { if (isDragging) { translationY = dragOffset; scaleX = 1.1f; scaleY = 1.1f; rotationZ = 1.5f; shadowElevation = 24f; alpha = 0.98f } }.zIndex(if (isDragging) 100f else 0f)
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        SongArtwork(song, size = 40, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onMove: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    Surface(color = if (active) Color.White.copy(alpha = 0.12f) else if (isDragging) Color.White.copy(alpha = 0.22f) else Color.Transparent, shape = RoundedCornerShape(16.dp), tonalElevation = elevation, modifier = modifier.fillMaxWidth().height(72.dp).clickable { onSongSelected() }) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongArtwork(song, size = 52, shape = RoundedCornerShape(10.dp))
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, color = if (active) MaterialTheme.colorScheme.primary else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(song.artist, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.size(48.dp).pointerInput(Unit) { detectDragGesturesAfterLongPress(onDragStart = { onDragStart() }, onDragEnd = { onDragEnd() }, onDragCancel = { onDragEnd() }, onDrag = { change, dragAmount -> onMove(change, dragAmount) }) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.DragIndicator, "Drag", tint = if (isDragging) Color.White else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit, 
    onSelect: (Int) -> Unit,
    remainingMs: Long = 0L
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                    fontWeight = FontWeight.Bold
                )
                
                if (remainingMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Ends in ${formatDuration(remainingMs)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            contentColor = if (isActive && value != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isActive && value != 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isActive && value != 0) {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControls(song: Song, isPlaying: Boolean, position: Long, duration: Long, currentPage: Int, onPlayPause: () -> Unit, onSeek: (Long) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onPageToggle: (Int) -> Unit) {
    val rd = if (duration > 0) duration else song.duration
    val sv = if (rd > 0) position.toFloat() / rd.toFloat() else 0f
    val q = audioQuality(song)
    var showQ by remember { mutableStateOf(false) }
    if (showQ) QualityDetailsDialog(quality = q) { showQ = false }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(value = sv.coerceIn(0f, 1f), onValueChange = { onSeek((it * rd).toLong()) }, thumb = { Box(modifier = Modifier.size(width = 3.dp, height = 18.dp).background(Color.White, RoundedCornerShape(2.dp))) }, track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(7.dp).clip(CircleShape), colors = SliderDefaults.colors(activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.22f)), thumbTrackGapSize = 0.dp) }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatDuration(position), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f)); Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(top = 4.dp).clickable { showQ = true }) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { if (q.badge.contains("Lossless")) { Icon(Icons.Default.Waves, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp)); Spacer(Modifier.width(6.dp)) }; Text(q.badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f)) } }; Text("-${formatDuration((rd - position).coerceAtLeast(0L))}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f)) }
        Spacer(Modifier.height(28.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(48.dp)) }; Surface(modifier = Modifier.size(92.dp).clickable { onPlayPause() }, shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) { Box(contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(56.dp)) } }; IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(48.dp)) } }
        Spacer(Modifier.height(34.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { onPageToggle(0) }) { Icon(Icons.Default.Article, "Lyrics", tint = if (currentPage == 0) Color.White else Color.White.copy(alpha = 0.45f)) }; IconButton(onClick = { onPageToggle(2) }) { Icon(Icons.Default.QueueMusic, "Queue", tint = if (currentPage == 2) Color.White else Color.White.copy(alpha = 0.45f)) } }
    }
}

@Composable
private fun QualityDetailsDialog(quality: com.aman.auramusic.util.AudioQuality, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Waves, null, tint = Color.Black, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(quality.badge, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("${quality.bitDepth}/${quality.sampleRate} ${quality.format}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}
