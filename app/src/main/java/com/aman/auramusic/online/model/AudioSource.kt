package com.aman.auramusic.online.model

enum class AudioSource(val displayName: String, val badgeText: String, val badgeColorHex: Long = 0xFF00B0FF) {
    ALL("All Catalogs", "ALL", 0xFF9E9E9E),
    JIOSAAVN("JioSaavn 320k", "320K", 0xFF00B0FF),
    YOUTUBE("YouTube Music", "YT", 0xFFFF0055),
    SPOTIFY("Spotify Charts", "SPOTIFY", 0xFF1DB954)
}
