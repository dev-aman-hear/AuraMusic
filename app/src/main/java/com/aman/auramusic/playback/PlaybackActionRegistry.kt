package com.aman.auramusic.playback

object PlaybackActionRegistry {
    var onPlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onFavorite: (() -> Unit)? = null
}
