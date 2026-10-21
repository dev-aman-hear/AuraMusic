package com.aman.auramusic.lockscreen

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.toLyricLines
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.player.OnlinePlaybackManagerHolder
import com.aman.auramusic.ui.theme.AuraMusicTheme
import com.aman.auramusic.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@AndroidEntryPoint
class LockScreenPlayerActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup lockscreen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Transparent system bars for immersive lockscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AuraMusicTheme(themeMode = com.aman.auramusic.data.model.ThemeMode.DARK) {
                LockScreenPlayerContent(
                    playerViewModel = playerViewModel,
                    onUnlock = { unlockAndDismiss() }
                )
            }
        }
    }

    private fun unlockAndDismiss() {
        if (!isFinishing && !isDestroyed) {
            moveTaskToBack(false)
            finish()
            overridePendingTransition(0, 0)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockScreenPlayerContent(
    playerViewModel: PlayerViewModel,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val onlinePlaybackManager = remember { OnlinePlaybackManagerHolder.get(context) }
    val onlinePlaybackState by onlinePlaybackManager.playbackState.collectAsStateWithLifecycle()
    val isOnlineActive = onlinePlaybackState.isPlaying || onlinePlaybackState.currentSong != null

    val currentSong: Song? = if (isOnlineActive) {
        onlinePlaybackState.currentSong?.toSong()
    } else {
        playerViewModel.currentSong
    }

    val isPlaying: Boolean = if (isOnlineActive) {
        onlinePlaybackState.isPlaying
    } else {
        playerViewModel.isPlaying
    }

    val position: Long = if (isOnlineActive) {
        onlinePlaybackState.currentPositionMs
    } else {
        playerViewModel.currentPosition
    }

    val duration: Long = if (isOnlineActive) {
        onlinePlaybackState.durationMs
    } else {
        playerViewModel.duration
    }

    val localLyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
    val lyrics: List<LyricLine> = remember(isOnlineActive, onlinePlaybackState.currentLyrics, localLyrics) {
        if (isOnlineActive && onlinePlaybackState.currentLyrics != null) {
            onlinePlaybackState.currentLyrics?.toLyricLines() ?: emptyList()
        } else {
            localLyrics
        }
    }

    // Toggle between artwork compact mode and full-screen synced lyrics
    var isExpandedLyrics by remember { mutableStateOf(false) }

    // Live clock and date
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000L)
        }
    }

    BackHandler {
        onUnlock()
    }

    // Swipe up to unlock gesture
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < 0) {
                            offsetY += dragAmount.y
                            if (offsetY < -180f) {
                                onUnlock()
                            }
                        }
                    },
                    onDragEnd = {
                        offsetY = 0f
                    },
                    onDragCancel = {
                        offsetY = 0f
                    }
                )
            }
            .offset { IntOffset(0, offsetY.roundToInt()) }
    ) {
        // Blurred background artwork
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentSong?.artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(45.dp)
                .graphicsLayer { alpha = 0.5f }
        )

        // Gradient dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Time & Date Header
            Text(
                text = currentTimeString,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp
                ),
                color = Color.White
            )

            Text(
                text = currentDateString,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Center Area: Artwork + Compact Lyrics OR Expanded Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isExpandedLyrics,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "centerViewTransition"
                ) { expanded ->
                    if (expanded) {
                        // Fullscreen Synced Lyrics
                        LockScreenLyricsFullPage(
                            lyrics = lyrics,
                            position = position,
                            onSeek = { seekPos ->
                                if (isOnlineActive) {
                                    onlinePlaybackManager.seekTo(seekPos)
                                } else {
                                    playerViewModel.seekTo(seekPos)
                                }
                            },
                            onCollapse = { isExpandedLyrics = false }
                        )
                    } else {
                        // Artwork + Compact Lyrics View
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Album Art
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .aspectRatio(1f)
                                    .shadow(28.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.DarkGray.copy(alpha = 0.3f))
                                    .clickable { isExpandedLyrics = true }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentSong?.artworkUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = currentSong?.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Compact live lyric line preview
                            LockScreenCompactLyricLine(
                                lyrics = lyrics,
                                position = position,
                                onClick = { isExpandedLyrics = true }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song Info (Title & Artist)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentSong?.title ?: "No track playing",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentSong?.artist ?: "Aura Music",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seekbar
            LockScreenSeekBar(
                position = position,
                duration = duration,
                onSeek = { seekPos ->
                    if (isOnlineActive) {
                        onlinePlaybackManager.seekTo(seekPos)
                    } else {
                        playerViewModel.seekTo(seekPos)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand lyrics toggle button
                IconButton(
                    onClick = { isExpandedLyrics = !isExpandedLyrics },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Toggle Lyrics",
                        tint = if (isExpandedLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = {
                        if (isOnlineActive) {
                            onlinePlaybackManager.playPrevious()
                        } else {
                            playerViewModel.playPrevious()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause (Distinct prominent button)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (isOnlineActive) {
                                onlinePlaybackManager.togglePlayPause()
                            } else {
                                playerViewModel.togglePlayPause()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = {
                        if (isOnlineActive) {
                            onlinePlaybackManager.playNext()
                        } else {
                            playerViewModel.playNext()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Placeholder for symmetrical layout
                Spacer(modifier = Modifier.size(46.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom "Swipe up to unlock" hint
            SwipeUpUnlockHint(onUnlock = onUnlock)

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LockScreenCompactLyricLine(
    lyrics: List<LyricLine>,
    position: Long,
    onClick: () -> Unit
) {
    val activeIndex = lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    val currentLine = lyrics.getOrNull(activeIndex)?.text ?: if (lyrics.isEmpty()) "Tap for synced lyrics" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentLine,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn(tween(250))) togetherWith
                        (slideOutVertically { height -> -height / 2 } + fadeOut(tween(250)))
            },
            label = "compactLyricTransition"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LockScreenLyricsFullPage(
    lyrics: List<LyricLine>,
    position: Long,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit
) {
    val activeIndex = lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            val screenHeight = configuration.screenHeightDp.dp
            val scrollOffset = with(density) { (screenHeight * 0.22f).roundToPx() }
            listState.animateScrollToItem(index = activeIndex, scrollOffset = -scrollOffset)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp)
    ) {
        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No synced lyrics available",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item { Spacer(modifier = Modifier.height(48.dp)) }
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val opacity by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.25f,
                        label = "lyricOpacity"
                    )
                    val lyricScale by animateFloatAsState(
                        targetValue = if (isActive) 1.04f else 1f,
                        label = "lyricScale"
                    )

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = opacity),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = lyricScale
                                scaleY = lyricScale
                            }
                            .pointerInput(line.timeMs) {
                                detectTapGestures(onTap = { onSeek(line.timeMs) })
                            }
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun LockScreenSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val safeDuration = duration.coerceAtLeast(1L)
    val currentRatio = if (isDragging) dragPosition else (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = currentRatio,
            onValueChange = {
                isDragging = true
                dragPosition = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((dragPosition * safeDuration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayedPos = if (isDragging) (dragPosition * safeDuration).toLong() else position
            Text(
                text = formatDuration(displayedPos),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(safeDuration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SwipeUpUnlockHint(onUnlock: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrowFloat")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "arrowBounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onUnlock() }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Swipe up to unlock",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(24.dp)
                .offset(y = bounceOffset.dp)
        )
        Text(
            text = "Swipe up to unlock",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
