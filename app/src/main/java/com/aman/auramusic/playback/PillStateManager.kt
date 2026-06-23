package com.aman.auramusic.playback

import com.aman.auramusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object PillStateManager {
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isPillEnabled = MutableStateFlow(false)
    val isPillEnabled = _isPillEnabled.asStateFlow()

    private val _pillPosition = MutableStateFlow(1) // 0: Left, 1: Center, 2: Right
    val pillPosition = _pillPosition.asStateFlow()

    private val _pillVerticalOffset = MutableStateFlow(32)
    val pillVerticalOffset = _pillVerticalOffset.asStateFlow()

    private val _pillSizeScale = MutableStateFlow(1.0f)
    val pillSizeScale = _pillSizeScale.asStateFlow()

    fun updateState(song: Song?, isPlaying: Boolean) {
        _currentSong.value = song
        _isPlaying.value = isPlaying
    }

    fun setPillEnabled(enabled: Boolean) {
        _isPillEnabled.value = enabled
    }

    fun setPillPosition(position: Int) {
        _pillPosition.value = position
    }

    fun setPillVerticalOffset(offset: Int) {
        _pillVerticalOffset.value = offset
    }

    fun setPillSizeScale(scale: Float) {
        _pillSizeScale.value = scale
    }
}
