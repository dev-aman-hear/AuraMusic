package com.aman.auramusic.data.model

data class OnlinePlaylist(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val cover: String,
    val category: String,
    val songCount: Int,
    val playlistUrl: String
)

data class OnlinePlaylistDetails(
    val name: String,
    val description: String,
    val artwork: String,
    val songs: List<OnlineSong>
)

data class OnlineSong(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null,
    val isrc: String? = null,
    val cover: String? = null
)
