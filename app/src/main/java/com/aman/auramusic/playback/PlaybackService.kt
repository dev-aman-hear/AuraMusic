package com.aman.auramusic.playback

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import com.aman.auramusic.coloros.ColorOSLiveLyricsBridge
import com.aman.auramusic.coloros.ColorOSLyricPayload
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong

class PlaybackService : Service() {

    private lateinit var playerManager: VlcPlayerManager
    private lateinit var notificationManager: PlaybackNotificationManager
    private lateinit var userRepository: UserPreferencesRepository
    private lateinit var colorOSBridge: ColorOSLiveLyricsBridge
    private val lyricsRepository = LyricsRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var keepPlayingOnClose = true
    private val binder = LocalBinder()
    
    var currentSong: Song? = null
    
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queueFlow = _queue.asStateFlow()
    var queue: List<Song>
        get() = _queue.value
        set(value) { _queue.value = value }

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatModeFlow = _repeatMode.asStateFlow()
    var repeatMode: RepeatMode
        get() = _repeatMode.value
        set(value) { _repeatMode.value = value }

    private val _isShuffled = MutableStateFlow(value = false)
    val isShuffledFlow = _isShuffled.asStateFlow()
    var isShuffled: Boolean
        get() = _isShuffled.value
        set(value) { _isShuffled.value = value }

    var currentPlaylistId: Long? = null

    private var isTaskRemoved = false

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = VlcPlayerManager(this)
        notificationManager = PlaybackNotificationManager(this)
        userRepository = UserPreferencesRepository(this)
        colorOSBridge = ColorOSLiveLyricsBridge(this)
        
        setupActions()

        serviceScope.launch {
            userRepository.settingsFlow.collect { settings ->
                keepPlayingOnClose = settings.keepPlayingOnClose
                playerManager.smartAudioFocusEnabled = settings.smartAudioFocus
            }
        }

        playerManager.addListener(object : VlcPlayerManager.PlayerListener {
            override fun onProgress(position: Long, duration: Long) {
                currentSong?.let { song ->
                    colorOSBridge.onPlaybackStateChanged(song, playerManager.isPlaying(), position)
                }
            }

            override fun onPlaybackState(isPlaying: Boolean) {
                currentSong?.let { song ->
                    serviceScope.launch {
                        val notification = notificationManager.createNotification(song, isPlaying, playerManager.getSessionToken())
                        notificationManager.show(song, isPlaying, playerManager.getSessionToken())
                    }
                    colorOSBridge.onPlaybackStateChanged(song, isPlaying, playerManager.position())
                }
                if (!isPlaying && isTaskRemoved) {
                    stopSelf()
                }
            }

            override fun onEnd() {
                handlePlaybackEnd()
            }
        })
    }

    private var retryJob: kotlinx.coroutines.Job? = null

    private fun handlePlaybackEnd() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                currentSong?.let { play(it) }
            }
            else -> {
                playNext()
            }
        }
    }

    fun playNext() {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
        val nextIndex = (currentIndex + 1) % queue.size
        
        if (nextIndex == 0 && (repeatMode == RepeatMode.NONE) && currentIndex != -1) {
            if (playerManager.isPlaying()) playerManager.togglePlayPause()
            return
        }
        
        play(queue[nextIndex])
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
        val prevIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        play(queue[prevIndex])
    }

    fun play(song: Song) {
        if (isOnlinePlaybackActive) {
            isOnlinePlaybackActive = false
            currentOnlineSong = null
        }
        setupActions()
        currentSong = song
        playerManager.play(song.filePath)
        startAsForeground(song, true)
    }

    fun prepare(song: Song, positionMs: Long) {
        currentSong = song
        playerManager.prepare(song.filePath, positionMs)
    }

    var isOnlinePlaybackActive: Boolean = false
        private set
    var currentOnlineSong: OnlineSong? = null
        private set
    private var cachedOnlineLyrics: List<LyricLine> = emptyList()
    private var isOnlinePlaying: Boolean = false
    private var currentOnlinePosition: Long = 0L

    fun startOnlinePlayback(
        song: OnlineSong,
        isPlaying: Boolean,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onSeekTo: ((Long) -> Unit)? = null
    ) {
        isOnlinePlaybackActive = true
        currentOnlineSong = song
        currentSong = null
        isOnlinePlaying = isPlaying
        currentOnlinePosition = 0L
        cachedOnlineLyrics = emptyList()

        // Stop offline player if running
        playerManager.pause()

        val displaySong = song.toSong()

        PlaybackActionRegistry.onPlayPause = onPlayPause
        PlaybackActionRegistry.onNext = onNext
        PlaybackActionRegistry.onPrevious = onPrevious
        PlaybackActionRegistry.onSeekTo = { pos ->
            currentOnlinePosition = pos
            colorOSBridge.onSeek(displaySong, pos, isOnlinePlaying)
            onSeekTo?.invoke(pos)
        }

        colorOSBridge.onTrackChanged(displaySong, cachedOnlineLyrics, isPlaying, 0L)
        updateOnlineNotification(song, isPlaying)

        serviceScope.launch {
            val lyrics = withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(displaySong) }
            if (lyrics.isNotEmpty() && currentOnlineSong?.id == song.id) {
                updateOnlineLyrics(song, lyrics)
            }
        }
    }

    fun updateOnlineLyrics(song: OnlineSong, lyrics: List<LyricLine>) {
        if (!isOnlinePlaybackActive || currentOnlineSong?.id != song.id) return
        cachedOnlineLyrics = lyrics
        val displaySong = song.toSong()

        colorOSBridge.onLyricsLoaded(displaySong, lyrics, isPlaying = isOnlinePlaying, positionMs = currentOnlinePosition)

        val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(displaySong, lyrics)

        retryJob?.cancel()
        serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(displaySong) }
            playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork, lyricInfoJson)

            if (lyricInfoJson.isNotBlank()) {
                retryJob = launch {
                    kotlinx.coroutines.delay(800)
                    if (currentOnlineSong?.id == song.id) {
                        playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork, lyricInfoJson)
                    }
                }
            }
        }
    }

    fun updateOnlinePlaybackState(
        song: OnlineSong,
        isPlaying: Boolean,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        if (!isOnlinePlaybackActive) return
        val songChanged = currentOnlineSong?.id != song.id
        val playStateChanged = isOnlinePlaying != isPlaying
        currentOnlineSong = song
        isOnlinePlaying = isPlaying
        currentOnlinePosition = positionMs

        val displaySong = song.toSong()

        playerManager.updateSessionPlaybackState(isPlaying, positionMs)
        colorOSBridge.onPlaybackStateChanged(displaySong, isPlaying, positionMs)

        if (songChanged) {
            cachedOnlineLyrics = emptyList()
            colorOSBridge.onTrackChanged(displaySong, cachedOnlineLyrics, isPlaying, positionMs)
            updateOnlineNotification(song, isPlaying)

            serviceScope.launch {
                val lyrics = withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(displaySong) }
                if (lyrics.isNotEmpty() && currentOnlineSong?.id == song.id) {
                    updateOnlineLyrics(song, lyrics)
                }
            }
        } else if (playStateChanged) {
            updateOnlineNotification(song, isPlaying)
        }
    }

    fun updateOnlinePosition(positionMs: Long) {
        if (!isOnlinePlaybackActive) return
        currentOnlinePosition = positionMs
        val displaySong = currentOnlineSong?.toSong() ?: return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
        colorOSBridge.onPlaybackStateChanged(displaySong, isOnlinePlaying, positionMs)
    }

    fun onOnlineSeek(positionMs: Long) {
        if (!isOnlinePlaybackActive) return
        currentOnlinePosition = positionMs
        val displaySong = currentOnlineSong?.toSong() ?: return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
        colorOSBridge.onSeek(displaySong, positionMs, isOnlinePlaying)
    }

    fun stopOnlinePlayback() {
        if (!isOnlinePlaybackActive) return
        val displaySong = currentOnlineSong?.toSong()
        isOnlinePlaybackActive = false
        currentOnlineSong = null
        cachedOnlineLyrics = emptyList()
        currentOnlinePosition = 0L
        setupActions()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clear()
        colorOSBridge.onPlaybackStopped(displaySong)
    }

    private fun updateOnlineNotification(
        onlineSong: OnlineSong,
        isPlaying: Boolean
    ) {
        val displaySong = onlineSong.toSong()

        retryJob?.cancel()
        serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(displaySong) }
            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(displaySong, cachedOnlineLyrics)
            playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork, lyricInfoJson)
            val notification = notificationManager.createNotification(displaySong, isPlaying, playerManager.getSessionToken())
            if (isPlaying) {
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification)
            } else {
                notificationManager.show(displaySong, isPlaying, playerManager.getSessionToken())
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun setupActions() {
        PlaybackActionRegistry.onPlayPause = { playerManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext() }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
        PlaybackActionRegistry.onSeekTo = { playerManager.seekTo(it) }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isTaskRemoved = true
        val isAnyPlaying = if (isOnlinePlaybackActive) true else playerManager.isPlaying()
        if (!keepPlayingOnClose || !isAnyPlaying) {
            stopSelf()
        }
    }

    fun getPlayerManager(): VlcPlayerManager = playerManager
    fun getNotificationManager(): PlaybackNotificationManager = notificationManager

    fun startAsForeground(song: Song, isPlaying: Boolean) {
        currentSong = song

        retryJob?.cancel()
        serviceScope.launch {
            val artwork = with(Dispatchers.IO) { notificationManager.loadArtwork(song) }
            val lyrics = withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(song) }
            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(song, lyrics)

            playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork, lyricInfoJson)
            colorOSBridge.onTrackChanged(song, lyrics, isPlaying, playerManager.position())

            val notification = notificationManager.createNotification(song, isPlaying, playerManager.getSessionToken())
            startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification)

            // Single 800ms check to recover from OPlus media metadata debounce lost updates
            if (lyricInfoJson.isNotBlank()) {
                retryJob = launch {
                    kotlinx.coroutines.delay(800)
                    if (currentSong?.id == song.id) {
                        playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork, lyricInfoJson)
                    }
                }
            }
        }
    }

    fun stopAsForeground(removeNotification: Boolean) {
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        val activeSong = currentSong ?: currentOnlineSong?.toSong()
        colorOSBridge.onPlaybackStopped(activeSong)
    }

    override fun onDestroy() {
        val activeSong = currentSong ?: currentOnlineSong?.toSong()
        colorOSBridge.onPlaybackStopped(activeSong)
        serviceScope.cancel()
        playerManager.release()
        super.onDestroy()
    }
}
