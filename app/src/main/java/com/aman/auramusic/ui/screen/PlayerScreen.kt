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
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.zIndex
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
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.component.SongArtwork
import com.aman.auramusic.util.audioQuality
import com.aman.auramusic.util.formatDuration
import com.aman.auramusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    lyrics: List<LyricLine>,
    queue: List<Song>,
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
    playerViewModel: PlayerViewModel = viewModel()
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 }) // Initialized to Now Playing (page 1)
    val scope = rememberCoroutineScope()
    val dominantColorInt = playerViewModel.dominantColor
    val accentColorInt = playerViewModel.accentColor
    val density = LocalDensity.current
    val offsetY = remember { Animatable(0f) }
    val scaleFactor = remember(offsetY.value) {
        (1f - (offsetY.value / 3000f)).coerceIn(0.9f, 1f)
    }
    val cornerRadius = remember(offsetY.value) {
        (offsetY.value / 20f).coerceIn(0f, 32f).dp
    }
    
    // Auto-hide controls state
    var showControls by remember { mutableStateOf(true) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var dragStartedInHeader by remember { mutableStateOf(false) }
    
    // Smooth color transition
    val animatedColor by animateColorAsState(
        targetValue = Color(dominantColorInt),
        animationSpec = tween(durationMillis = 800),
        label = "dominantColorAnimation"
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = Color(accentColorInt),
        animationSpec = tween(durationMillis = 800),
        label = "accentColorAnimation"
    )
    
    // Modern status bar handling
    SideEffect {
        // You would typically use a SystemUiController or similar here
        // but since we called enableEdgeToEdge(), we just need to ensure
        // content doesn't look bad.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scaleFactor)
            .clip(RoundedCornerShape(cornerRadius))
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragStartedInHeader = offset.y < with(density) { 120.dp.toPx() }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value > 400f) {
                                onBack()
                                offsetY.snapTo(0f)
                            } else {
                                offsetY.animateTo(
                                    0f, 
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                        dragStartedInHeader = false
                    },
                    onDragCancel = {
                        scope.launch { offsetY.animateTo(0f, tween(300)) }
                        dragStartedInHeader = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // Allow dragging down from header on any page, or anywhere on Artwork page
                        if (pagerState.currentPage == 1 || offsetY.value > 0f || dragStartedInHeader) {
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                                offsetY.snapTo(newOffset)
                            }
                        }
                    }
                )
            }
    ) {
        // Dynamic Background
        PlayerBackground(
            song = song, 
            dominantColor = animatedColor, 
            accentColor = animatedAccentColor,
            blurIntensity = appSettings.blurIntensity
        )

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                onDismiss = { showSleepTimerDialog = false },
                onSelect = { 
                    playerViewModel.setSleepTimer(it)
                    showSleepTimerDialog = false
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            PlayerHeader(
                song = song,
                onBack = onBack,
                isLyricsPage = pagerState.currentPage != 1, // Show mini-header on Lyrics and Queue
                isFavorite = playerViewModel.isFavorite,
                onFavoriteToggle = { playerViewModel.toggleFavorite() },
                onAddToPlaylist = onAddToPlaylist,
                onShowSleepTimer = { showSleepTimerDialog = true }
            )

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> LyricsPage(
                        lyrics = lyrics, 
                        position = position, 
                        onSeek = onSeek,
                        onToggleControls = { showControls = it },
                        fontScale = appSettings.lyricFontScale,
                        karaokeMode = appSettings.karaokeMode
                    )
                    1 -> NowPlayingPage(
                        song = song,
                        isPlaying = isPlaying,
                        isFavorite = playerViewModel.isFavorite,
                        onFavoriteToggle = { playerViewModel.toggleFavorite() },
                        onAddToPlaylist = onAddToPlaylist,
                        onShowSleepTimer = { showSleepTimerDialog = true }
                    )
                    2 -> QueuePage(
                        queue = queue,
                        history = history,
                        currentSongId = song.id,
                        isShuffled = playerViewModel.isShuffled,
                        repeatMode = playerViewModel.repeatMode,
                        isCurrentPage = pagerState.currentPage == 2,
                        onShuffleToggle = { playerViewModel.toggleShuffle() },
                        onRepeatToggle = { playerViewModel.toggleRepeat() },
                        onSongSelected = onSongSelected,
                        onQueueRemove = onQueueRemove,
                        onQueueClear = onQueueClear,
                        onQueueSave = onQueueSave,
                        onHistoryClear = onHistoryClear,
                        onToggleControls = { showControls = it },
                        onMove = { from, to -> playerViewModel.moveQueueItem(from, to) }
                    )
                }
            }

            // Controls (Pinned at bottom)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(500)
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
                    onPageToggle = { targetPage ->
                        scope.launch {
                            if (pagerState.currentPage == targetPage) {
                                pagerState.animateScrollToPage(1)
                            } else {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerBackground(song: Song, dominantColor: Color, accentColor: Color, blurIntensity: Int) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurIntensity.dp)
                .scale(2.5f),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.4f),
                            accentColor.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlayerHeader(
    song: Song,
    onBack: () -> Unit,
    isLyricsPage: Boolean, // Now generic for "Mini Header" pages
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowSleepTimer: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLyricsPage) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    song = song,
                    size = 48,
                    shape = RoundedCornerShape(8.dp),
                    elevation = 0.dp
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .background(if (isFavorite) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Black.copy(alpha = 0.65f) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Favorite") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            onClick = { 
                                onFavoriteToggle()
                                showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            onClick = { 
                                onAddToPlaylist()
                                showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer") },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                            onClick = { 
                                onShowSleepTimer()
                                showMenu = false 
                            }
                        )
                    }
                }
            }
        } else {
            // Clean transparent space when not in mini-header mode (Now Playing)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp) // Reduced height to bring artwork higher
                    .clickable { onBack() }
            )
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
    onShowSleepTimer: () -> Unit
) {
    val artworkScale by animateFloatAsState(if (isPlaying) 1f else 0.85f, label = "artworkScale")
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp)) // Pull up slightly

        SongArtwork(
            song = song,
            size = 280, // Reduced from 300 to fix bottom overlap
            modifier = Modifier
                .scale(artworkScale)
                .clip(RoundedCornerShape(24.dp))
        )
        
        Spacer(modifier = Modifier.height(32.dp)) // Slightly tightened

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp) // Extra padding to avoid Star button overlap
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f), // Slightly brighter for physical devices
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .background(if (isFavorite) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Black.copy(alpha = 0.65f) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Favorite") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = { 
                            onFavoriteToggle()
                            showMenu = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        onClick = { 
                            onAddToPlaylist()
                            showMenu = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep Timer") },
                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                        onClick = { 
                            onShowSleepTimer()
                            showMenu = false 
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsPage(
    lyrics: List<LyricLine>,
    position: Long,
    onSeek: (Long) -> Unit,
    onToggleControls: (Boolean) -> Unit,
    fontScale: Float,
    karaokeMode: Boolean
) {
    val activeIndex = lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    val view = LocalView.current
    val density = LocalDensity.current

    val controlScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -15) onToggleControls(false)
                if (available.y > 15) onToggleControls(true)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // Keep screen on when lyrics are visible
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            // Scroll to center the active lyric line
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -with(density) { 200.dp.roundToPx() }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(controlScrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls(true) })
            }
    ) {
        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No synced lyrics found", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item { Spacer(modifier = Modifier.height(100.dp)) }
                itemsIndexed(lyrics) { index, line ->
                    val active = index == activeIndex
                    val opacity by animateFloatAsState(
                        targetValue = if (active) 1f else if (karaokeMode) 0.08f else 0.25f,
                        label = "lyricOpacity"
                    )
                    
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = opacity),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(line.timeMs) {
                                detectTapGestures(onTap = { onSeek(line.timeMs) })
                            }
                    )
                }
                item { Spacer(modifier = Modifier.height(240.dp)) }
            }
        }

        // Invisible touch area on the right to show controls
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleControls(true) })
                }
        )
    }
}

@Composable
private fun QueuePage(
    queue: List<Song>,
    history: List<Song>,
    currentSongId: Long?,
    isShuffled: Boolean,
    repeatMode: PlayerViewModel.RepeatMode,
    isCurrentPage: Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onQueueRemove: (Song) -> Unit,
    onQueueClear: () -> Unit,
    onQueueSave: () -> Unit,
    onHistoryClear: () -> Unit,
    onToggleControls: (Boolean) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val activeIndex = queue.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    val controlScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -15) onToggleControls(false)
                if (available.y > 15) onToggleControls(true)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    LaunchedEffect(activeIndex, queue.size, isCurrentPage, history.size) {
        if (queue.isNotEmpty() && draggingIndex == null && isCurrentPage) {
            // Find the index of the "Now Playing" header
            var targetIndex = 0
            if (history.isNotEmpty()) {
                targetIndex += history.size + 2 // History Header + Items + Spacer
            }
            if (activeIndex > 0) {
                targetIndex += activeIndex + 1 // Previous items + Spacer
            }
            
            // Scroll so Now Playing is exactly at the top
            listState.scrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .nestedScroll(controlScrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls(true) })
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShuffleToggle,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(32.dp)
                        .background(if (isShuffled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRepeatToggle,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .background(if (repeatMode != PlayerViewModel.RepeatMode.NONE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            PlayerViewModel.RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != PlayerViewModel.RepeatMode.NONE) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onHistoryClear() }
                                .padding(8.dp)
                        )
                    }
                }
                itemsIndexed(history, key = { index, song -> "history_${index}_${song.id}" }) { index, song ->
                    HistoryItem(song = song, onClick = { onSongSelected(song) })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Section for songs previously in the queue (won't "vanish" now)
            if (activeIndex > 0) {
                itemsIndexed(
                    items = queue.subList(0, activeIndex),
                    key = { index, song -> "prev_${index}_${song.id}" }
                ) { index, song ->
                    QueueItem(
                        song = song,
                        active = false,
                        isDragging = false,
                        elevation = 0.dp,
                        onSongSelected = { onSongSelected(song) },
                        onMove = { _, _ -> }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            item {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (activeIndex != -1 && activeIndex < queue.size) {
                val song = queue[activeIndex]
                item(key = "nowplaying_${song.id}") {
                    QueueItem(
                        song = song,
                        active = true,
                        isDragging = false,
                        elevation = 0.dp,
                        onSongSelected = { onSongSelected(song) },
                        onMove = { _, _ -> }
                    )
                }
            }

            // Up Next section
            if (activeIndex + 1 < queue.size) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onQueueClear() }
                                .padding(8.dp)
                        )
                    }
                }
                
                itemsIndexed(
                    items = queue.subList(activeIndex + 1, queue.size),
                    key = { index, song -> "upnext_${index}_${song.id}" }
                ) { index, song ->
                    val actualIndex = activeIndex + 1 + index
                    val isDragging = draggingIndex == actualIndex
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                    QueueItem(
                        song = song,
                        active = false,
                        isDragging = isDragging,
                        elevation = elevation,
                        onSongSelected = { onSongSelected(song) },
                        onMove = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            val itemHeight = 64
                            val relativeTargetIndex = (index + (dragOffset / itemHeight).toInt()).coerceIn(0, queue.size - activeIndex - 2)
                            val targetIndex = activeIndex + 1 + relativeTargetIndex
                            
                            if (draggingIndex != null && targetIndex != draggingIndex) {
                                onMove(draggingIndex!!, targetIndex)
                                draggingIndex = targetIndex
                                dragOffset = 0f
                            }
                        },
                        onDragStart = { 
                            draggingIndex = actualIndex
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffset = 0f
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(song = song, size = 40, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QualityDetailsDialog(
    quality: com.aman.auramusic.util.AudioQuality,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White, // White background as in reference
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = quality.badge,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Text(
                    text = "${quality.bitDepth}/${quality.sampleRate} ${quality.format}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Surface(
        color = if (active) Color.White.copy(alpha = 0.12f) else if (isDragging) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .clickable { onSongSelected() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(song = song, size = 52, shape = RoundedCornerShape(10.dp))
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (active) MaterialTheme.colorScheme.primary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!active) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount -> onMove(change, dragAmount) }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                val options = listOf(
                    "Off" to 0,
                    "15 minutes" to 15,
                    "30 minutes" to 30,
                    "45 minutes" to 45,
                    "1 hour" to 60,
                    "End of song" to -1
                )
                options.forEach { (label, value) ->
                    androidx.compose.material3.TextButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onPageToggle: (Int) -> Unit
) {
    val resolvedDuration = if (duration > 0) duration else song.duration
    val sliderValue = if (resolvedDuration > 0) {
        position.toFloat() / resolvedDuration.toFloat()
    } else {
        0f
    }
    val quality = audioQuality(song)
    var showQualityDetails by remember { mutableStateOf(false) }

    if (showQualityDetails) {
        QualityDetailsDialog(
            quality = quality,
            onDismiss = { showQualityDetails = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Apple Music Style Seekbar
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = { onSeek((it * resolvedDuration).toLong()) },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 18.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(7.dp).clip(CircleShape),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    thumbTrackGapSize = 0.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            // Quality Badge centered below slider as in reference
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { showQualityDetails = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (quality.badge.contains("Lossless")) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = quality.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Text(
                text = "-${formatDuration((resolvedDuration - position).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Transport
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious, 
                    contentDescription = "Prev", 
                    tint = Color.White, 
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Surface(
                modifier = Modifier
                    .size(92.dp)
                    .clickable { onPlayPause() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext, 
                    contentDescription = "Next", 
                    tint = Color.White, 
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(34.dp))
        
        // Bottom Pager Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onPageToggle(0) }) {
                Icon(
                    Icons.Default.Article,
                    contentDescription = "Lyrics",
                    tint = if (currentPage == 0) Color.White else Color.White.copy(alpha = 0.45f)
                )
            }

            IconButton(onClick = { onPageToggle(2) }) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = if (currentPage == 2) Color.White else Color.White.copy(alpha = 0.45f)
                )
            }
        }
    }
}
