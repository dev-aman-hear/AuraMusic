package com.aman.auramusic.playback

import android.util.Log
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.Build
import android.media.session.PlaybackState
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerManager(context: Context) {

    private val libVLC = LibVLC(context)
    private val mediaPlayer = MediaPlayer(libVLC)
    private val mediaSession = MediaSession(context, "AuraMusicSession")
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private var onProgressChanged: ((Long, Long) -> Unit)? = null
    private var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    private var onMediaEnded: (() -> Unit)? = null
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
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
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
        shouldResumeOnFocusGain = false

        if (smartAudioFocusEnabled) {
            requestAudioFocus()
        }

        val media = Media(libVLC, path)

        mediaPlayer.media = media
        media.release()

        mediaPlayer.play()
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            shouldResumeOnFocusGain = false
            mediaPlayer.pause()
            abandonAudioFocus()
        } else {
            if (smartAudioFocusEnabled) requestAudioFocus()
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
