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
    songCountText: String,
    artworkModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 108.dp
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.58f)
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.20f)

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
                .border(2.dp, ringColor, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (isDark) {
                            listOf(Color(0xFF34304A), Color(0xFF171923), Color(0xFF4A243A))
                        } else {
                            listOf(Color(0xFFFFD9E2), Color(0xFFE6E8FF), Color(0xFFFFE7C2))
                        }
                    )
                ),
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
                        color = if (isDark) Color.White else Color(0xFF493044),
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF493044),
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
