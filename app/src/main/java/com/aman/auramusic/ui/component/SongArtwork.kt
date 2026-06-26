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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.util.ArtworkExtractor

@Composable
fun SongArtwork(
    song: Song?,
    size: Int,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    elevation: androidx.compose.ui.unit.Dp = 4.dp,
) {
    val context = LocalContext.current
    val model = remember(song?.id, song?.uri, song?.artworkUri) {
        if (song != null) {
            if (song.id == -1L && song.uri.isNotBlank()) {
                // For external songs, use our custom extractor to get the bitmap directly
                ArtworkExtractor.getArtwork(context, song.uri)
            } else {
                song.artworkUri
            }
        } else null
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(elevation, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Fallback icon if no artwork
        if (model == null) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size((size / 2).dp)
            )
        }
    }
}
