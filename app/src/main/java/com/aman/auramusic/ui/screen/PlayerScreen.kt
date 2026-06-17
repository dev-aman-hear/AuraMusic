package com.aman.auramusic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onQueueRemove: (Song) -> Unit,
    onQueueClear: () -> Unit,
    onQueueSave: () -> Unit,
    onAddToPlaylist: () -> Unit,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val dominantColorInt = playerViewModel.dominantColor
    
    // Auto-hide controls state
    var showControls by remember { mutableStateOf(true) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    // Handle auto-hide logic
    LaunchedEffect(pagerState.currentPage, showControls) {
        if (pagerState.currentPage == 0 && showControls) {
            delay(8000) // Longer delay for lyrics
            showControls = false
        } else if (pagerState.currentPage != 0) {
            showControls = true
        }
    }

    // Smooth color transition
    val animatedColor by animateColorAsState(
        targetValue = Color(dominantColorInt),
        animationSpec = tween(durationMillis = 800),
        label = "dominantColorAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pagerState.currentPage) {
                detectTapGestures(
                    onTap = { 
                        if (pagerState.currentPage == 0) showControls = !showControls 
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount > 50) {
                        onBack()
                    }
                }
            }
    ) {
        // Dynamic Background
        PlayerBackground(song = song, dominantColor = animatedColor)

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
                        onInteraction = { showControls = true }
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
                        currentSongId = song.id,
                        isShuffled = playerViewModel.isShuffled,
                        repeatMode = playerViewModel.repeatMode,
                        onShuffleToggle = { playerViewModel.toggleShuffle() },
                        onRepeatToggle = { playerViewModel.toggleRepeat() },
                        onSongSelected = onSongSelected,
                        onQueueRemove = onQueueRemove,
                        onQueueClear = onQueueClear,
                        onQueueSave = onQueueSave,
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
private fun PlayerBackground(song: Song, dominantColor: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .scale(1.5f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.85f),
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
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
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
    val artworkScale by animateFloatAsState(if (isPlaying) 1f else 0.82f, label = "artworkScale")
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SongArtwork(
            song = song,
            size = 320,
            modifier = Modifier
                .scale(artworkScale)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2, // Allow up to 2 lines for title
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 1.0f), // Full opacity for artist
                    maxLines = 1,
                    overflow = TextOverflow.Visible // Ensure it's not clipped
                )
            }
            
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
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
    onInteraction: () -> Unit
) {
    val activeIndex = lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    val isUserScrolling by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            onInteraction()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onInteraction()
                                onSeek(line.timeMs) 
                            }
                    )
                }
                item { Spacer(modifier = Modifier.height(240.dp)) }
            }
        }
    }
}

@Composable
private fun QueuePage(
    queue: List<Song>,
    currentSongId: Long?,
    isShuffled: Boolean,
    repeatMode: PlayerViewModel.RepeatMode,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onQueueRemove: (Song) -> Unit,
    onQueueClear: () -> Unit,
    onQueueSave: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val activeIndex = queue.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(activeIndex, queue.size) {
        if (queue.isNotEmpty() && draggingIndex == null) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
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
                modifier = Modifier.padding(vertical = 12.dp) // Reduced padding
            )
            
            Row {
                IconButton(
                    onClick = onShuffleToggle,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(if (isShuffled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onRepeatToggle,
                    modifier = Modifier
                        .background(if (repeatMode != PlayerViewModel.RepeatMode.NONE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            PlayerViewModel.RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != PlayerViewModel.RepeatMode.NONE) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                val active = song.id == currentSongId
                val isDragging = draggingIndex == index
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                
                if (index == activeIndex + 1) {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                Surface(
                    color = if (active) Color.White.copy(alpha = 0.12f) else if (isDragging) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = elevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .clickable { onSongSelected(song) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongArtwork(song = song, size = 52, shape = RoundedCornerShape(10.dp))
                        Spacer(modifier = Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (active) Color.White else Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = {},
                            modifier = Modifier.pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingIndex = index },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        
                                        val itemHeight = 64 // Reduced for more sensitivity
                                        val targetIndex = (index + (dragOffset / itemHeight).toInt()).coerceIn(0, queue.size - 1)
                                        
                                        if (targetIndex != draggingIndex) {
                                            onMove(draggingIndex!!, targetIndex)
                                            draggingIndex = targetIndex
                                            dragOffset = 0f
                                        }
                                    }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Slider
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = { onSeek((it * resolvedDuration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
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
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(42.dp))
            }
            
            Surface(
                modifier = Modifier
                    .size(82.dp)
                    .clickable { onPlayPause() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(42.dp))
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
            
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${quality.badge} • ${quality.compact}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
