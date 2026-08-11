package com.aman.auramusic.playback

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.aman.auramusic.coloros.ColorOSLiveLyricsBridge
import com.aman.auramusic.coloros.ColorOSLyricPayload
import com.aman.auramusic.data.repository.LyricsRepository

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
                val wasEnabled = colorOSBridge.isEnabled
                colorOSBridge.isEnabled = settings.colorOsLiveLyricsEnabled
                
                // If setting was newly enabled during playback, broadcast current song state immediately
                if (!wasEnabled && settings.colorOsLiveLyricsEnabled) {
                    currentSong?.let { song ->
                        val lyrics = lyricsRepository.lyricsFor(song)
                        colorOSBridge.onTrackChanged(song, lyrics, playerManager.isPlaying(), playerManager.position())
                    }
                }

                PillStateManager.setPillEnabled(settings.dynamicPillEnabled)
                PillStateManager.setPillPosition(settings.pillPosition)
                PillStateManager.setPillVerticalOffset(settings.pillVerticalOffset)
                PillStateManager.setPillSizeScale(settings.pillSizeScale)
                checkPillService()
            }
        }

        playerManager.addListener(object : VlcPlayerManager.PlayerListener {
            override fun onProgress(position: Long, duration: Long) {
                currentSong?.let { song ->
                    colorOSBridge.onPlaybackStateChanged(song, playerManager.isPlaying(), position)
                }
            }

            override fun onPlaybackState(isPlaying: Boolean) {
                PillStateManager.updateState(currentSong, isPlaying)
                currentSong?.let { song ->
                    serviceScope.launch {
                        val notification = notificationManager.createNotification(song, isPlaying, playerManager.getSessionToken())
                        notificationManager.show(song, isPlaying, playerManager.getSessionToken())
                    }
                    colorOSBridge.onPlaybackStateChanged(song, isPlaying, playerManager.position())
                }
                if (!isPlaying && isTaskRemoved) {
                    stopSelf()
                } else {
                    checkPillService()
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
        currentSong = song
        playerManager.play(song.filePath)
        startAsForeground(song, true)
    }

    fun prepare(song: Song, positionMs: Long) {
        currentSong = song
        playerManager.prepare(song.filePath, positionMs)
        PillStateManager.updateState(song, false)
        checkPillService()
    }

    private fun setupActions() {
        PlaybackActionRegistry.onPlayPause = { playerManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext() }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
    }

    private fun checkPillService() {
        // Pill should show if enabled and we have a song.
        // It should NOT disappear immediately on pause anymore.
        val shouldRun = PillStateManager.isPillEnabled.value && currentSong != null
        
        val serviceIntent = Intent(this, PillOverlayService::class.java)
        if (shouldRun) {
            startService(serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isTaskRemoved = true
        if (!keepPlayingOnClose || !playerManager.isPlaying()) {
            stopSelf()
        }
    }

    fun getPlayerManager(): VlcPlayerManager = playerManager
    fun getNotificationManager(): PlaybackNotificationManager = notificationManager

    fun startAsForeground(song: Song, isPlaying: Boolean) {
        currentSong = song
        PillStateManager.updateState(song, isPlaying)
        checkPillService()

        retryJob?.cancel()
        serviceScope.launch {
            val artwork = with(Dispatchers.IO) { notificationManager.loadArtwork(song) }
            val lyrics = with(Dispatchers.IO) { lyricsRepository.lyricsFor(song) }
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
        PillStateManager.updateState(currentSong, false)
        checkPillService()
        colorOSBridge.onPlaybackStopped(currentSong)
    }

    override fun onDestroy() {
        colorOSBridge.onPlaybackStopped(currentSong)
        serviceScope.cancel()
        // Stop pill service when playback service is destroyed
        stopService(Intent(this, PillOverlayService::class.java))
        playerManager.release()
        super.onDestroy()
    }
}
