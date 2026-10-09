package com.aman.auramusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.LocalIsDark

@Composable
fun AuraEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.MusicNote,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val messageColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.55f)
    val circleBg = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(circleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 13.5.sp,
            color = messageColor,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AuraLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val textColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AuraErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val messageColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Something went wrong",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = messageColor,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
