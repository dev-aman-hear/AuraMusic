package com.aman.auramusic.data.model

data class PlaybackHistoryEntry(
    val songId: Long,
    val playedAt: Long,
    val playCount: Int = 1,
)
