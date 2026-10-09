package com.aman.auramusic.online.model

/**
 * Model representing an online curated playlist fetched automatically from
 * JioSaavn, YouTube Music, or Spotify.
 */
data class OnlinePlaylist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artworkUrl: String = "",
    val songCount: Int = 0,
    val source: AudioSource = AudioSource.JIOSAAVN,
    val language: String = "",
    val songs: List<OnlineSong> = emptyList()
)
