package com.aman.auramusic.data.model

import com.aman.auramusic.online.model.OnlineSong

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val artworkSongId: Long? = null,
    val onlineSongs: List<OnlineSong> = emptyList(),
)

