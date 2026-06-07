package com.aman.auramusic.playback

import android.content.Context
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerManager(context: Context) {

    private val libVLC = LibVLC(context)
    private val mediaPlayer = MediaPlayer(libVLC)
    private var onProgressChanged: ((Long, Long) -> Unit)? = null
    private var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    private var onMediaEnded: (() -> Unit)? = null

    init {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.PositionChanged -> {
                    onProgressChanged?.invoke(position(), duration())
                }
                MediaPlayer.Event.Playing -> {
                    onPlaybackStateChanged?.invoke(true)
                }
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {
                    onPlaybackStateChanged?.invoke(false)
                }
                MediaPlayer.Event.EndReached -> {
                    onMediaEnded?.invoke()
                }
            }
        }
    }

    fun setListeners(
        onProgress: (Long, Long) -> Unit,
        onPlaybackState: (Boolean) -> Unit,
        onEnd: () -> Unit
    ) {
        onProgressChanged = onProgress
        onPlaybackStateChanged = onPlaybackState
        onMediaEnded = onEnd
    }

    fun play(path: String) {

        val media = Media(libVLC, path)

        mediaPlayer.media = media
        media.release()

        mediaPlayer.play()
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.play()
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun position(): Long {
        return mediaPlayer.time.coerceAtLeast(0L)
    }

    fun duration(): Long {
        return mediaPlayer.length.coerceAtLeast(0L)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer.time = positionMs.coerceAtLeast(0L)
    }

    fun release() {
        mediaPlayer.release()
        libVLC.release()
    }
}
