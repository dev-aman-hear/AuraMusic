package com.aman.auramusic.playback

import android.content.Context
import android.media.session.MediaSession
import android.media.session.PlaybackState
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerManager(context: Context) {

    private val libVLC = LibVLC(context)
    private val mediaPlayer = MediaPlayer(libVLC)
    private val mediaSession = MediaSession(context, "AuraMusicSession")
    
    private var onProgressChanged: ((Long, Long) -> Unit)? = null
    private var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    private var onMediaEnded: (() -> Unit)? = null
    private var pendingSeekMs: Long? = null

    init {
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { togglePlayPause() }
            override fun onPause() { togglePlayPause() }
            override fun onSkipToNext() { PlaybackActionRegistry.onNext?.invoke() }
            override fun onSkipToPrevious() { PlaybackActionRegistry.onPrevious?.invoke() }
            override fun onSeekTo(pos: Long) { seekTo(pos) }
        })
        mediaSession.isActive = true

        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.PositionChanged -> {
                    onProgressChanged?.invoke(position(), duration())
                    updatePlaybackState()
                }
                MediaPlayer.Event.Playing -> {
                    onPlaybackStateChanged?.invoke(true)
                    updatePlaybackState()
                    // Apply pending seek if any
                    pendingSeekMs?.let {
                        mediaPlayer.time = it
                        pendingSeekMs = null
                    }
                }
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {
                    onPlaybackStateChanged?.invoke(false)
                    updatePlaybackState()
                }
                MediaPlayer.Event.EndReached -> {
                    onMediaEnded?.invoke()
                }
            }
        }
    }

    fun getSessionToken(): MediaSession.Token = mediaSession.sessionToken

    private fun updatePlaybackState() {
        val state = if (mediaPlayer.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackState = PlaybackState.Builder()
            .setState(state, position(), 1f)
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    fun setMetadata(title: String, artist: String, album: String, duration: Long, artwork: android.graphics.Bitmap?) {
        val metadata = android.media.MediaMetadata.Builder()
            .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putString(android.media.MediaMetadata.METADATA_KEY_ALBUM, album)
            .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, duration)
            .putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            .build()
        mediaSession.setMetadata(metadata)
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

    fun prepare(path: String, positionMs: Long = 0L) {
        val media = Media(libVLC, path)
        mediaPlayer.media = media
        media.release()
        pendingSeekMs = if (positionMs > 0) positionMs else null
    }

    fun play(path: String) {

        pendingSeekMs = null

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

    fun setVolume(volume: Int) {
        mediaPlayer.volume = volume.coerceIn(0, 100)
    }

    fun release() {
        mediaSession.release()
        mediaPlayer.release()
        libVLC.release()
    }

}
