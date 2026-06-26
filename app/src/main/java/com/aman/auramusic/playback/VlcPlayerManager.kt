package com.aman.auramusic.playback

import android.util.Log
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerManager(context: Context) {

    interface PlayerListener {
        fun onProgress(position: Long, duration: Long)
        fun onPlaybackState(isPlaying: Boolean)
        fun onEnd()
    }

    private val listeners = mutableListOf<PlayerListener>()
    
    fun addListener(listener: PlayerListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    private val libVLC = LibVLC(context)
    private val mediaPlayer = MediaPlayer(libVLC)
    private val mediaSession = MediaSession(context, "AuraMusicSession")
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private var pendingSeekMs: Long? = null
    private var shouldResumeOnFocusGain = false
    var smartAudioFocusEnabled = true

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d("AuraMusicFocus", "Focus changed: $focusChange")
        if (!smartAudioFocusEnabled || mediaPlayer.isReleased) return@OnAudioFocusChangeListener
        
        try {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d("AuraMusicFocus", "Focus gained, shouldResume: $shouldResumeOnFocusGain")
                    if (shouldResumeOnFocusGain) {
                        mediaPlayer.play()
                        shouldResumeOnFocusGain = false
                    }
                    mediaPlayer.volume = 100
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.d("AuraMusicFocus", "Focus lost permanently")
                    shouldResumeOnFocusGain = false
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                    abandonAudioFocus()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                -> {
                    Log.d("AuraMusicFocus", "Focus lost transiently")
                    if (mediaPlayer.isPlaying) {
                        shouldResumeOnFocusGain = true
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.volume = 20
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuraMusicFocus", "Error handling focus change", e)
        }
    }

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
                    val pos = position()
                    val dur = duration()
                    listeners.forEach { it.onProgress(pos, dur) }
                    updatePlaybackState()
                }
                MediaPlayer.Event.Playing -> {
                    listeners.forEach { it.onPlaybackState(true) }
                    updatePlaybackState()
                    // Apply pending seek if any
                    pendingSeekMs?.let {
                        mediaPlayer.time = it
                        pendingSeekMs = null
                    }
                }
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {
                    listeners.forEach { it.onPlaybackState(false) }
                    updatePlaybackState()
                }
                MediaPlayer.Event.EndReached -> {
                    listeners.forEach { it.onEnd() }
                }
                MediaPlayer.Event.EncounteredError -> {
                    Log.e("VlcPlayerManager", "VLC Error encountered")
                    listeners.forEach { it.onPlaybackState(false) }
                }
            }
        }
    }

    fun getSessionToken(): MediaSession.Token = mediaSession.sessionToken

    private fun updatePlaybackState() {
        val state = try {
            if (mediaPlayer.isReleased) PlaybackState.STATE_NONE
            else if (mediaPlayer.isPlaying) PlaybackState.STATE_PLAYING 
            else PlaybackState.STATE_PAUSED
        } catch (_: Exception) {
            PlaybackState.STATE_NONE
        }
        
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

    fun prepare(path: String, positionMs: Long = 0L) {
        val media = createMedia(path)
        mediaPlayer.media = media
        media.release()
        pendingSeekMs = if (positionMs > 0) positionMs else null
    }

    fun play(path: String) {
        Log.d("VlcPlayerManager", "Playing: $path")
        pendingSeekMs = null
        shouldResumeOnFocusGain = false

        if (smartAudioFocusEnabled) {
            requestAudioFocus()
        }

        val media = createMedia(path)
        mediaPlayer.media = media
        media.release()

        mediaPlayer.play()
        listeners.forEach { it.onPlaybackState(true) } // Notify immediately when starting new track
    }

    private fun createMedia(path: String): Media {
        return if (path.startsWith("content://")) {
            Media(libVLC, path.toUri())
        } else {
            Media(libVLC, path)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            shouldResumeOnFocusGain = false
            mediaPlayer.pause()
            abandonAudioFocus()
            listeners.forEach { it.onPlaybackState(false) } // Notify immediately
        } else {
            if (smartAudioFocusEnabled) requestAudioFocus()
            mediaPlayer.play()
            listeners.forEach { it.onPlaybackState(true) } // Notify immediately
        }
        updatePlaybackState()
    }

    fun isPlaying(): Boolean {
        return try {
            if (mediaPlayer.isReleased) false else mediaPlayer.isPlaying
        } catch (e: Exception) {
            false
        }
    }

    fun position(): Long {
        return try {
            if (mediaPlayer.isReleased) 0L else mediaPlayer.time.coerceAtLeast(0L)
        } catch (e: Exception) {
            0L
        }
    }

    fun duration(): Long {
        return try {
            if (mediaPlayer.isReleased) 0L else mediaPlayer.length.coerceAtLeast(0L)
        } catch (e: Exception) {
            0L
        }
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
