package com.aman.auramusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.util.ArtworkExtractor

@Composable
fun SongArtwork(
    song: Song?,
    size: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 4.dp,
    fallbackIcon: ImageVector = Icons.Default.MusicNote
) {
    val context = LocalContext.current
    val model = remember(song?.id, song?.uri, song?.artworkUri) {
        if (song != null) {
            if (song.id == -1L && song.uri.isNotBlank()) {
                ArtworkExtractor.getArtwork(context, song.uri)
            } else {
                song.artworkUri
            }
        } else null
    }

    AuraArtwork(
        model = model,
        size = size,
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        fallbackIcon = fallbackIcon
    )
}

@Composable
fun AuraArtwork(
    model: Any?,
    size: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 4.dp,
    fallbackIcon: ImageVector = Icons.Default.MusicNote,
    contentScale: ContentScale = ContentScale.Crop
) {
    val isDark = LocalIsDark.current
    val fallbackGradient = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(Color(0xFF222533), Color(0xFF141620))
            )
        } else {
            Brush.linearGradient(
                listOf(Color(0xFFEFF1F8), Color(0xFFDFE2EE))
            )
        }
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(elevation, shape)
            .clip(shape)
            .background(fallbackGradient),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.30f),
                modifier = Modifier.size((size * 0.45f).dp)
            )
        }
    }
}
