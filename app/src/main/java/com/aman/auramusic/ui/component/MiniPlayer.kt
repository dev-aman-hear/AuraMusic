package com.aman.auramusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.liquidGlass

/**
 * Floating Liquid Glass Mini Player inspired by iOS & Apple Music.
 * Features a floating glass capsule with specular border highlight,
 * artwork thumbnail, crisp typography, responsive playback controls,
 * and edge-to-edge content transmittance behind it.
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    dominantColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
) {
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val isDark = LocalIsDark.current

    val titleColor = if (isDark) Color.White else Color(0xFF111111)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.60f) else Color(0xFF8E8E93)
    val iconTint = if (isDark) Color.White else Color(0xFF111111)
    val pillShape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(64.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            (dragY < -50f) && (kotlin.math.abs(dragY) > kotlin.math.abs(dragX)) -> onOpen()
                            dragX > 50f -> onNext()
                            dragX < -50f -> onPrevious()
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    }
                ) { _, dragAmount ->
                    dragX += dragAmount.x
                    dragY += dragAmount.y
                }
            }
            .liquidGlass(
                shape = pillShape,
                level = GlassLevel.UltraThin,
                isDark = isDark,
                tint = dominantColor,
                elevation = if (isDark) 16.dp else 10.dp,
                borderWidth = 1.2.dp,
                sheenAlpha = if (isDark) 0.24f else 0.44f
            )
            .clickable(onClick = onOpen)
    ) {
        // Main content row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork thumbnail with subtle glass border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                SongArtwork(
                    song = song,
                    size = 48,
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.1).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Play / Pause Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Next Track Button (Apple Music style double arrow SkipNext)
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Hairline subtle progress line embedded along bottom of pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = com.aman.auramusic.ui.theme.AppleMusicAccent,
                trackColor = Color.Transparent
            )
        }
    }
}

/**
 * Reusable Liquid Glass Mini Player component.
 */
@Composable
fun LiquidGlassMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    dominantColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {}
) = MiniPlayer(
    song = song,
    isPlaying = isPlaying,
    position = position,
    duration = duration,
    dominantColor = dominantColor,
    onOpen = onOpen,
    onPlayPause = onPlayPause,
    onNext = onNext,
    onPrevious = onPrevious
)
