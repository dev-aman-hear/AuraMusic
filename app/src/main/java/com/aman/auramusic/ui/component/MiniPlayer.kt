package com.aman.auramusic.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.LocalIsDark

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

    val containerColor = if (isDark) {
        Color.Black.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(if (isDark) 16.dp else 10.dp, RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            (dragY < -60f) && (kotlin.math.abs(dragY) > kotlin.math.abs(dragX)) -> onOpen()
                            dragX > 60f -> onNext()
                            dragX < -60f -> onPrevious()
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
            .clickable { onOpen() },
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        dominantColor.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            } else {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                    )
                )
            }
        ),
        shadowElevation = if (isDark) 12.dp else 6.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
            
            Row(
                modifier = Modifier
                    .height(60.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    song = song,
                    size = 46,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next",
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
