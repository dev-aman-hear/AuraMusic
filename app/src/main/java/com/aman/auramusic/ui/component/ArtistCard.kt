package com.aman.auramusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.LocalIsDark
import coil.compose.AsyncImage

@Composable
fun ArtistCard(
    name: String,
    songCountText: String? = null,
    artworkModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 108.dp
) {
    val isDark = LocalIsDark.current
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ringColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .width(size + 12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // If the YouTube Music artist image is missing or fails to load,
        // show a deliberate artist-avatar fallback, never a song/album cover.
        var imageFailed by remember(name, artworkModel) { mutableStateOf(false) }
        val showImage = artworkModel != null && !imageFailed
        Box(
            modifier = Modifier
                .size(size)
                .border(1.5.dp, ringColor, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (showImage) {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = "$name artist portrait",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true }
                )
            } else {
                val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString()
                if (!initial.isNullOrBlank()) {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (!songCountText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = songCountText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
