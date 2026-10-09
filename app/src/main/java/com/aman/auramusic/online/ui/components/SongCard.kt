package com.aman.auramusic.online.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.ui.component.SongRow

/** Shared online track row; keeps search and discovery lists visually consistent. */
@Composable
fun SongCard(
    song: OnlineSong,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SongRow(
        onlineSong = song,
        isActive = isActive,
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier
    )
}
