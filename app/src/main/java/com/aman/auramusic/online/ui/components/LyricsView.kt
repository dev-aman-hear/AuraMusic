package com.aman.auramusic.online.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.online.lyrics.model.LyricsLine
import com.aman.auramusic.online.lyrics.model.SongLyrics
import com.aman.auramusic.online.ui.AccentCyan
import com.aman.auramusic.online.ui.TextMuted
import com.aman.auramusic.online.ui.TextPrimary
import com.aman.auramusic.online.ui.TextSecondary

/**
 * Apple Music-inspired Kinetic Synchronized Lyrics Component.
 * - Smooth fluid auto-centering via LazyListState.
 * - Glowing kinetic typography for active lines.
 * - Word-by-word progressive highlighting if word timings are available.
 * - Interactive tap-to-seek karaoke navigation.
 */
@Composable
fun LyricsView(
    lyrics: SongLyrics?,
    activeLineIndex: Int,
    currentPositionMs: Long,
    isLoading: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Syncing live lyrics from LRCLIB...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    if (lyrics == null || lyrics.isEmpty) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No synchronized lyrics available for this track",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (!lyrics.isSynced && !lyrics.plainLyrics.isNullOrBlank()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x15FFFFFF))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "PLAIN LYRICS (UNSYNCED)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
            itemsIndexed(lyrics.plainLyrics.lines()) { _, line ->
                if (line.isNotBlank()) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = TextPrimary
                    )
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lyrics.lines.size) {
            val targetScroll = (activeLineIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetScroll,
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 80.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeLineIndex
            val isPast = index < activeLineIndex

            LyricsLineItem(
                line = line,
                isActive = isActive,
                isPast = isPast,
                currentPositionMs = currentPositionMs,
                onClick = { onSeekTo(line.startMs) }
            )
        }
    }
}

@Composable
private fun LyricsLineItem(
    line: LyricsLine,
    isActive: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "lineScale"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> Color.White
            isPast -> Color(0x55FFFFFF)
            else -> Color(0x99FFFFFF)
        },
        animationSpec = tween(durationMillis = 300),
        label = "lineTextColor"
    )

    val fontSize = if (isActive) 24.sp else 19.sp
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        if (isActive && line.isWordSynced) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                line.words.forEach { word ->
                    val isWordActive = currentPositionMs >= word.startMs
                    val wordColor = if (isWordActive) AccentCyan else Color(0x66FFFFFF)

                    Text(
                        text = "${word.text} ",
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = wordColor
                    )
                }
            }
        } else {
            Text(
                text = line.lineText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = if (isActive) AccentCyan else textColor,
                lineHeight = 32.sp
            )
        }
    }
}
