package com.aman.auramusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.LocalIsDark

@Composable
fun PlaylistCard(
    title: String,
    songCountText: String,
    artworkModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    elevation: Dp = 6.dp
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.58f)

    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        AuraArtwork(
            model = artworkModel,
            size = size.value.toInt(),
            modifier = Modifier
                .size(size)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = elevation,
            fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = songCountText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
