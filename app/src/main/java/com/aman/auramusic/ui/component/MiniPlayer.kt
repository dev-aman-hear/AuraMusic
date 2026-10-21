package com.aman.auramusic.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.model.AppSettings

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
    appSettings: AppSettings? = null,
) {
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val themeActive = appSettings?.isPlayerThemeActive() == true
    val paletteMode = appSettings?.let {
        com.aman.auramusic.ui.theme.AppPaletteMode.fromString(it.themeColorStyle.ifBlank { it.appPaletteMode })
    } ?: com.aman.auramusic.ui.theme.AppPaletteMode.PRESET
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    var mediaBorderColor by remember(song.id, song.artworkUri, song.uri, isDark, themeActive, paletteMode, appSettings?.appColorPreset, appSettings?.appCustomPaletteHexes, defaultPrimaryColor) {
        mutableStateOf<Color?>(null)
    }
    LaunchedEffect(song.id, song.title, song.artist, song.artworkUri, song.uri, isDark, themeActive, paletteMode, appSettings?.appColorPreset, appSettings?.appCustomPaletteHexes, defaultPrimaryColor) {
        mediaBorderColor = when {
            !themeActive -> null
            paletteMode == com.aman.auramusic.ui.theme.AppPaletteMode.DYNAMIC_ARTWORK -> runCatching {
                DynamicArtworkColorsHelper.extractPalette(context, song, isDark).accent
            }.getOrElse {
                PlayerArtworkPalette.neutral(isDark).accent
            }
            paletteMode == com.aman.auramusic.ui.theme.AppPaletteMode.PRESET -> {
                val presetName = appSettings?.appColorPreset
                    ?.takeUnless { it.equals("Monochrome", ignoreCase = true) }
                    ?: "Midnight Indigo"
                PlayerArtworkPalette.fromPreset(presetName, isDark).accent
            }
            paletteMode == com.aman.auramusic.ui.theme.AppPaletteMode.CUSTOM -> {
                val customPalette = com.aman.auramusic.ui.theme.CustomPaletteValidator
                    .createPaletteFromCustomHexes(appSettings?.appCustomPaletteHexes.orEmpty(), isDark)
                customPalette.accent
            }
            else -> defaultPrimaryColor
        }
    }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val pillShape = RoundedCornerShape(18.dp)
    val titleColor = MaterialTheme.colorScheme.onSurface
    val iconTint = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 0.dp else 14.dp,
                vertical = if (compact) 0.dp else 3.dp
            )
             .height(if (compact) 54.dp else 40.dp)
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
            .background(if (isDark) Color(0xFF121212) else Color(0xFFF7F7F7), pillShape)
            .border(androidx.compose.foundation.BorderStroke(1.dp, mediaBorderColor ?: (if (isDark) Color(0xFF555555) else Color(0xFFB8B8B8))), pillShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                 .padding(horizontal = if (compact) 7.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(
                song = song,
                size = if (compact) 30 else 30,
                shape = RoundedCornerShape(7.dp),
                elevation = 0.dp
            )

            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.1).sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 1.dp)
            ) {
                if (!compact) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(27.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = iconTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(if (compact) 20.dp else 27.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!compact) {
                    IconButton(onClick = onNext, modifier = Modifier.size(22.dp)) {
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
                 .height(1.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = mediaBorderColor ?: MaterialTheme.colorScheme.primary,
                trackColor = (if (isDark) Color(0xFF555555) else Color(0xFFB8B8B8)).copy(alpha = 0.4f)
            )
        }
    }
}
