package com.aman.auramusic.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.LocalIsDark

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
        AuraArtwork(
            model = artworkModel,
            size = size.value.toInt(),
            modifier = Modifier
                .size(size)
                .border(2.dp, ringColor, CircleShape),
            shape = CircleShape,
            elevation = 6.dp,
            fallbackIcon = Icons.Default.Person
        )

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
