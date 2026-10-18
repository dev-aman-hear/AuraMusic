package com.aman.auramusic.online.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.online.model.PlaybackState
import com.aman.auramusic.online.model.StreamingEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDiagnosticsSheet(
    state: PlaybackState,
    onChangeBitrate: (String) -> Unit,
    onChangeEngine: (StreamingEngine) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val song = state.currentSong

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stream Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YouTube Streaming Engine (Dedicated RiPlay)
            Text(
                text = "YouTube Streaming Engine",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RiPlay Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "fast4x/RiPlay",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dedicated Headless YouTube IFrame Player (Zero-Block, Native YouTube Music Audio)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (song == null) {
                Text(
                    text = "No track currently playing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DiagnosticItem(title = "Title", value = song.title)
                DiagnosticItem(title = "Artist", value = song.artist)
                DiagnosticItem(title = "Source Engine", value = song.source.displayName)
                DiagnosticItem(title = "Active Player Type", value = state.activePlayerType)
                DiagnosticItem(title = "Extraction Tier", value = if (song.extractorTier.isNotBlank()) song.extractorTier else "Direct CDN")
                val containerText = state.measuredMimeType ?: song.format
                val bitrateText = if (state.measuredBitrate != null && state.measuredBitrate > 0) {
                    "${state.measuredBitrate / 1000} kbps (Measured)"
                } else if (song.source == com.aman.auramusic.online.model.AudioSource.YOUTUBE) {
                    "~128–160 kbps (Advertised Opus/AAC)"
                } else if (song.bitrate.isNotBlank()) {
                    "${song.bitrate} (Requested/Advertised)"
                } else {
                    "Unknown / Variable"
                }
                val sampleRateText = if (state.measuredSampleRate != null && state.measuredSampleRate > 0) {
                    "${state.measuredSampleRate} Hz (Measured)"
                } else if (song.source == com.aman.auramusic.online.model.AudioSource.YOUTUBE) {
                    "48,000 Hz (Nominal / Unmeasured)"
                } else {
                    "44,100 Hz (Nominal / Unmeasured)"
                }

                DiagnosticItem(title = "Audio Container", value = containerText)
                DiagnosticItem(title = "Audio Codec", value = if (song.audioCodec.isNotBlank()) song.audioCodec else containerText)
                DiagnosticItem(title = "Stream Bitrate", value = bitrateText)
                DiagnosticItem(title = "Sample Rate", value = sampleRateText)
                DiagnosticItem(
                    title = "Lyrics Status",
                    value = if (state.isLoadingLyrics) "Syncing with LRCLIB..."
                            else if (state.currentLyrics?.isWordSynced == true) "Word-Synced (LRCLIB)"
                            else if (state.currentLyrics?.isSynced == true) "Line-Synced (LRCLIB)"
                            else if (!state.currentLyrics?.plainLyrics.isNullOrBlank()) "Plain (Unsynced)"
                            else "Not Available"
                )
                DiagnosticItem(title = "Player State", value = if (state.isBuffering) "Buffering" else if (state.isPlaying) "Playing" else "Paused")
                DiagnosticItem(
                    title = "Buffer Health",
                    value = "${state.currentPositionFormatted} / ${state.durationFormatted} (Buffered: ${(state.bufferedProgress * 100).toInt()}%)"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bitrate Quality Switcher (JioSaavn)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("320", "160", "96").forEach { bitrate ->
                        val isSelected = state.selectedBitrate.startsWith(bitrate)
                        Button(
                            onClick = { onChangeBitrate(bitrate) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "${bitrate}k", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Resolved Stream / Engine URI",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = song.streamUrl.ifBlank { "Resolving link..." },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DiagnosticItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
