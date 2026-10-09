package com.aman.auramusic.online.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.ui.AccentCyan
import com.aman.auramusic.online.ui.AccentRed
import com.aman.auramusic.online.ui.GlassBorder
import com.aman.auramusic.online.ui.SurfaceCard
import com.aman.auramusic.online.ui.TextMuted
import com.aman.auramusic.online.ui.TextPrimary
import com.aman.auramusic.online.ui.TextSecondary

@Composable
fun SongCard(
    song: OnlineSong,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (isActive) Color(0x3300E5FF) else SurfaceCard

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(1.dp, if (isActive) AccentCyan.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Artwork
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF232738)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            if (isActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artists
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) AccentCyan else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        if (song.durationSeconds > 0) {
            Text(
                text = song.durationFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
