package com.aman.auramusic.online.model

/**
 * Domain model representing a persistent online artist saved in the user's Library.
 * Stores genuine provider identity, stable artist browse/channel ID, and portrait metadata.
 */
data class SavedOnlineArtist(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val source: AudioSource = AudioSource.YOUTUBE
)
