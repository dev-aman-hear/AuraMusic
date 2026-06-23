package com.aman.auramusic.playback

enum class RepeatMode {
    NONE, ONE, ALL
}

object PlaybackActionRegistry {
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onFavorite: (() -> Unit)? = null
}
