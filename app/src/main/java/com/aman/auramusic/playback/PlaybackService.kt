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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : Service() {

    private lateinit var playerManager: VlcPlayerManager
    private lateinit var notificationManager: PlaybackNotificationManager
    private lateinit var userRepository: UserPreferencesRepository
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

    private val _isShuffled = MutableStateFlow(false)
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
        
        setupActions()

        serviceScope.launch {
            userRepository.settingsFlow.collect { settings ->
                keepPlayingOnClose = settings.keepPlayingOnClose
                playerManager.smartAudioFocusEnabled = settings.smartAudioFocus
                PillStateManager.setPillEnabled(settings.dynamicPillEnabled)
                PillStateManager.setPillPosition(settings.pillPosition)
                PillStateManager.setPillVerticalOffset(settings.pillVerticalOffset)
                PillStateManager.setPillSizeScale(settings.pillSizeScale)
                checkPillService()
            }
        }

        playerManager.setListeners(
            onProgress = { pos, dur -> },
            onPlaybackState = { isPlaying ->
                PillStateManager.updateState(currentSong, isPlaying)
                if (!isPlaying && isTaskRemoved) {
                    stopSelf()
                } else {
                    checkPillService()
                }
            },
            onEnd = {
                handlePlaybackEnd()
            }
        )
    }

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
        
        if (nextIndex == 0 && repeatMode == RepeatMode.NONE && currentIndex != -1) {
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
        val notification = notificationManager.createNotification(song, isPlaying, playerManager.getSessionToken())
        startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification)
    }

    fun stopAsForeground(removeNotification: Boolean) {
        stopForeground(removeNotification)
        PillStateManager.updateState(currentSong, false)
        checkPillService()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        // Stop pill service when playback service is destroyed
        stopService(Intent(this, PillOverlayService::class.java))
        playerManager.release()
        super.onDestroy()
    }
}
