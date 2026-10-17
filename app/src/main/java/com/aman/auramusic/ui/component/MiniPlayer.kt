package com.aman.auramusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import com.aman.auramusic.ui.theme.rememberPlayerColorScheme

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    compact: Boolean = false,
    dynamicColors: Boolean = false,
) {
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    var artworkPalette by remember(isDark) { mutableStateOf(PlayerArtworkPalette.neutral(isDark)) }

    LaunchedEffect(song.id, song.artworkUri, song.uri, isDark, dynamicColors) {
        if (dynamicColors) {
            val extracted = DynamicArtworkColorsHelper.extractPalette(context, song, isDark)
            artworkPalette = extracted
        } else {
            artworkPalette = PlayerArtworkPalette.neutral(isDark)
        }
    }

    val animatedPrimary by animateColorAsState(
        targetValue = artworkPalette.primary,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "miniPlayerPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = artworkPalette.secondary,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "miniPlayerSecondary"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = artworkPalette.tertiary,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "miniPlayerTertiary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = artworkPalette.accent,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "miniPlayerAccent"
    )

    val playerColors = rememberPlayerColorScheme(
        isDynamic = dynamicColors,
        isDark = isDark,
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary,
        accent = animatedAccent
    )

    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val pillShape = RoundedCornerShape(27.dp)
    val titleColor = if (dynamicColors) playerColors.textPrimary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (dynamicColors) playerColors.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (dynamicColors) playerColors.buttonIconTint else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 0.dp else 14.dp,
                vertical = if (compact) 0.dp else 3.dp
            )
            .height(54.dp)
            .animateContentSize(animationSpec = tween(220))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            dragY < -50f && kotlin.math.abs(dragY) > kotlin.math.abs(dragX) -> onOpen()
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
            .clip(pillShape)
            .background(
                if (dynamicColors) playerColors.capsuleSurface else MaterialTheme.colorScheme.surface,
                pillShape
            )
            .border(
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dynamicColors) playerColors.capsuleBorder else MaterialTheme.colorScheme.outline
                ),
                pillShape
            )
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 8.dp else 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                song = song,
                size = if (compact) 38 else 40,
                shape = RoundedCornerShape(10.dp),
                elevation = 0.dp
            )

            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    fontSize = if (compact) 13.sp else 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.1).sp
                )
                if (!compact) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = song.artist,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 1.dp)
            ) {
                if (!compact) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(if (compact) 34.dp else 36.dp)
                        .then(
                            if (dynamicColors) {
                                Modifier
                                    .clip(CircleShape)
                                    .background(playerColors.prominentSurface, CircleShape)
                                    .border(
                                        androidx.compose.foundation.BorderStroke(1.dp, playerColors.prominentBorder),
                                        CircleShape
                                    )
                            } else Modifier
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (dynamicColors) playerColors.prominentIconTint else iconTint,
                        modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                    )
                }
                if (!compact) {
                    IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next track",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = if (compact) 12.dp else 14.dp)
                .height(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = if (dynamicColors) playerColors.progressIndicator else MaterialTheme.colorScheme.primary,
                trackColor = if (dynamicColors) playerColors.progressTrack else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }
    }
}
