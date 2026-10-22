package com.aman.auramusic.playback

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import com.aman.auramusic.coloros.ColorOSLyricPayload
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.online.player.OnlinePlaybackManagerHolder
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toLyricLines
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.util.LastPlayedStore
import kotlinx.coroutines.flow.first

class PlaybackService : Service() {

    companion object {
        @Volatile
        var currentInstance: PlaybackService? = null
            private set
    }

    private lateinit var playerManager: VlcPlayerManager
    private lateinit var notificationManager: PlaybackNotificationManager
    private lateinit var userRepository: UserPreferencesRepository
    private lateinit var onlinePlaybackManager: OnlinePlaybackManager
    private val lyricsRepository = LyricsRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
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
        set(value) {
            _repeatMode.value = value
            PlaybackQueueCoordinator.setRepeatMode(value)
        }

    private val _isShuffled = MutableStateFlow(value = false)
    val isShuffledFlow = _isShuffled.asStateFlow()
    var isShuffled: Boolean
        get() = _isShuffled.value
        set(value) {
            _isShuffled.value = value
            PlaybackQueueCoordinator.setShuffle(value)
        }

    var currentPlaylistId: Long? = null
    private val queueSyncListener: (List<QueueTrack>) -> Unit = { tracks ->
        _queue.value = tracks.map { it.song }
    }

    private var isTaskRemoved = false
    private var sleepTimerJob: Job? = null
    @Volatile private var sleepAtTrackEnd = false

    var isOnlinePlaybackActive: Boolean = false
        private set
    var currentOnlineSong: OnlineSong? = null
        private set
    private var cachedOnlineLyrics: List<LyricLine> = emptyList()
    var isOnlinePlaying: Boolean = false
        private set
    private var currentOnlinePosition: Long = 0L
    private var currentOnlineDuration: Long = 0L

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var isScreenReceiverRegistered = false

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    Log.d("AuraPlaybackDebug", "Screen turned on / user present, verifying playback and notification")
                    if (isAnyPlaybackActive()) {
                        acquireWakeLock()
                        val activeLocal = currentSong
                        val activeOnline = currentOnlineSong
                        if (isOnlinePlaybackActive && activeOnline != null) {
                            updateOnlineNotification(activeOnline, isOnlinePlaying)
                        } else if (activeLocal != null) {
                            startAsForeground(activeLocal, playerManager.isPlaying())
                        }
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d("AuraPlaybackDebug", "Screen turned off")
                }
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "AuraMusic:PlaybackWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
                Log.d("AuraPlaybackDebug", "PlaybackService: WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("AuraPlaybackDebug", "PlaybackService: WakeLock released")
            }
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "Failed to release WakeLock", e)
        }
    }

    fun isAnyPlaybackActive(): Boolean {
        val isLocalActive = ::playerManager.isInitialized && (playerManager.isPlaying() || playerManager.isTransitioning)
        val isOnlineActive = isOnlinePlaybackActive && (isOnlinePlaying || (::onlinePlaybackManager.isInitialized && (onlinePlaybackManager.playbackState.value.isBuffering || onlinePlaybackManager.isTransitioning)))
        return isLocalActive || isOnlineActive
    }

    private fun getOnlineDisplaySong(song: OnlineSong? = currentOnlineSong): Song? {
        val target = song ?: currentOnlineSong ?: return null
        val baseSong = target.toSong()
        return if (currentOnlineDuration > 0L) baseSong.copy(duration = currentOnlineDuration) else baseSong
    }

    private fun setupOnlineActions() {
        PlaybackActionRegistry.onPlayPause = { onlinePlaybackManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { PlaybackQueueCoordinator.playNextTrack(isManual = true) }
        PlaybackActionRegistry.onPrevious = { PlaybackQueueCoordinator.playPreviousTrack() }
        PlaybackActionRegistry.onSeekTo = { onlinePlaybackManager.seekTo(it) }
    }

    private val onlinePlayerListener = object : OnlinePlaybackManager.PlaybackEventListener {
        override fun onSongEnded(song: OnlineSong): Boolean {
            if (!sleepAtTrackEnd) return false
            sleepAtTrackEnd = false
            cancelSleepTimer()
            pausePlaybackForSleepTimer()
            return true
        }

        override fun onSongChanged(song: OnlineSong) {
            Log.d("AuraPlaybackDebug", "PlaybackService onlinePlayerListener.onSongChanged: ${song.title}")
            isOnlinePlaybackActive = true
            currentOnlineSong = song
            currentSong = null
            PlaybackQueueCoordinator.setCurrentOnlineSong(song)
            isOnlinePlaying = true
            currentOnlinePosition = 0L
            currentOnlineDuration = (song.durationSeconds * 1000L).coerceAtLeast(0L)
            cachedOnlineLyrics = emptyList()
            playerManager.pause()
            setupOnlineActions()
            updateOnlinePlaybackState(song, isPlaying = true, positionMs = 0L, durationMs = currentOnlineDuration)
        }

        override fun onPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {
            Log.d("AuraPlaybackDebug", "PlaybackService onlinePlayerListener.onPlaybackStateChanged: ${song.title}, isPlaying=$isPlaying, pos=$positionMs")
            isOnlinePlaybackActive = true
            currentOnlineSong = song
            isOnlinePlaying = isPlaying
            currentOnlinePosition = positionMs
            setupOnlineActions()
            updateOnlinePlaybackState(song, isPlaying = isPlaying, positionMs = positionMs, durationMs = currentOnlineDuration)
            if (isPlaying) {
                acquireWakeLock()
            } else if (!isAnyPlaybackActive()) {
                releaseWakeLock()
            }
            if (!isPlaying && !playerManager.isPlaying() && isTaskRemoved) {
                val onlineState = if (::onlinePlaybackManager.isInitialized) onlinePlaybackManager.playbackState.value else null
                val isOnlineActive = isOnlinePlaybackActive &&
                    (isOnlinePlaying || onlineState?.isPlaying == true || onlineState?.isBuffering == true || onlinePlaybackManager.isTransitioning)
                if (!isOnlineActive) stopSelf()
            }
        }

        override fun onProgressUpdate(song: OnlineSong, positionMs: Long, durationMs: Long) {
            if (isOnlinePlaybackActive) {
                updateOnlinePosition(positionMs, durationMs)
            }
        }

        override fun onSeek(song: OnlineSong, positionMs: Long) {
            if (isOnlinePlaybackActive) {
                onOnlineSeek(positionMs)
            }
        }

        override fun onLyricsLoaded(song: OnlineSong, lyrics: com.aman.auramusic.online.lyrics.model.SongLyrics) {
            if (isOnlinePlaybackActive && currentOnlineSong?.id == song.id) {
                updateOnlineLyrics(song, lyrics.toLyricLines())
            }
        }

        override fun onPlaybackStopped(song: OnlineSong?) {
            Log.d("AuraPlaybackDebug", "PlaybackService onlinePlayerListener.onPlaybackStopped")
            stopOnlinePlayback()
            if (isTaskRemoved) stopSelf()
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        currentInstance = this
        playerManager = VlcPlayerManager(this)
        notificationManager = PlaybackNotificationManager(this)
        userRepository = UserPreferencesRepository(this)
        // Share the online player with the UI, but keep its lifetime tied to this service.
        onlinePlaybackManager = OnlinePlaybackManagerHolder.get(this)
        onlinePlaybackManager.addListener(onlinePlayerListener)
        
        PlaybackQueueCoordinator.registerDispatcher(object : PlaybackDispatcher {
            override fun playLocal(song: Song) {
                this@PlaybackService.play(song)
            }
            override fun playOnline(onlineSong: OnlineSong, startPositionMs: Long) {
                this@PlaybackService.isOnlinePlaybackActive = true
                this@PlaybackService.currentOnlineSong = onlineSong
                this@PlaybackService.currentSong = null
                this@PlaybackService.playerManager.pause()
                onlinePlaybackManager.playSong(onlineSong, startPositionMs = startPositionMs)
            }
            override fun pauseLocal() {
                this@PlaybackService.playerManager.pause()
            }
            override fun pauseOnline() {
                onlinePlaybackManager.pause()
            }
            override fun isOnlineActive(): Boolean {
                return this@PlaybackService.isOnlinePlaybackActive
            }
        })
        PlaybackQueueCoordinator.addEngineSyncListener(queueSyncListener)

        setupActions()

        try {
            val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_ON).apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)
            isScreenReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "Failed to register screenReceiver", e)
        }

        serviceScope.launch {
            userRepository.settingsFlow.collect { settings ->
                playerManager.smartAudioFocusEnabled = settings.smartAudioFocus
                playerManager.isLyricActive = settings.lockscreenLyricsEnabled
                notificationManager.isLyricActive = settings.lockscreenLyricsEnabled
            }
        }

        serviceScope.launch {
            PlaybackQueueCoordinator.repeatModeFlow.collect { mode ->
                _repeatMode.value = mode
            }
        }

        serviceScope.launch {
            PlaybackQueueCoordinator.isShuffledFlow.collect { shuffled ->
                _isShuffled.value = shuffled
            }
        }

        playerManager.addListener(object : VlcPlayerManager.PlayerListener {
            override fun onProgress(position: Long, duration: Long) = Unit

            override fun onPlaybackState(isPlaying: Boolean) {
                Log.d("AuraPlaybackDebug", "PlaybackService onPlaybackState: isPlaying=$isPlaying, isTransitioning=${playerManager.isTransitioning}, currentSong=${currentSong?.title}")
                if (isPlaying) {
                    acquireWakeLock()
                } else if (!isAnyPlaybackActive()) {
                    releaseWakeLock()
                }
                currentSong?.let { song ->
                    if (isPlaying) {
                        startAsForeground(song, true)
                    } else {
                        // Do not show paused notification or detach if actively transitioning to next song
                        if (!playerManager.isTransitioning) {
                            serviceScope.launch {
                                notificationManager.show(song, false, playerManager.getSessionToken())
                            }
                        } else {
                            Log.d("AuraPlaybackDebug", "PlaybackService: Suppressing paused notification during transition")
                        }
                    }
                }
                if (!isPlaying && !isOnlinePlaybackActive && isTaskRemoved && !playerManager.isTransitioning) {
                    stopSelf()
                }
            }

            override fun onEnd() {
                Log.d("AuraPlaybackDebug", "PlaybackService onEnd: currentSong=${currentSong?.title}")
                handlePlaybackEnd()
            }
        })
    }

    private var retryJob: Job? = null
    private var notificationJob: Job? = null
    private var lastEndHandledTimeMs: Long = 0L

    private fun handlePlaybackEnd() {
        if (sleepAtTrackEnd) {
            sleepAtTrackEnd = false
            cancelSleepTimer()
            pausePlaybackForSleepTimer()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastEndHandledTimeMs < 500) {
            Log.d("AuraPlaybackDebug", "handlePlaybackEnd debounced duplicate trigger within 500ms")
            return
        }
        lastEndHandledTimeMs = now
        Log.d("AuraPlaybackDebug", "handlePlaybackEnd called. RepeatMode: $repeatMode, currentSong: ${currentSong?.title}")
        val started = PlaybackQueueCoordinator.playNextTrack(isManual = false)
        if (!started) {
            Log.d("AuraPlaybackDebug", "handlePlaybackEnd: End of queue reached cleanly, pausing.")
            playerManager.pause()
        }
    }

    fun playNext(isManual: Boolean = false) {
        val started = PlaybackQueueCoordinator.playNextTrack(isManual)
        if (!started && queue.isNotEmpty()) {
            val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
            val nextIndex = currentIndex + 1
            if (nextIndex < queue.size) {
                play(queue[nextIndex])
            }
        }
    }

    fun playPrevious() {
        val started = PlaybackQueueCoordinator.playPreviousTrack()
        if (!started && queue.isNotEmpty()) {
            val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
            val prevIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
            play(queue[prevIndex])
        }
    }

    fun play(song: Song) {
        Log.d("AuraPlaybackDebug", "play() called for: ${song.title} (${song.id})")
        if (isOnlinePlaybackActive) {
            isOnlinePlaybackActive = false
            currentOnlineSong = null
        }
        setupActions()
        currentSong = song
        PlaybackQueueCoordinator.setCurrentLocalSong(song)
        playerManager.play(song.filePath)
        startAsForeground(song, true)
    }

    fun prepare(song: Song, positionMs: Long) {
        currentSong = song
        playerManager.prepare(song.filePath, positionMs)
    }

    fun startOnlinePlayback(
        song: OnlineSong,
        isPlaying: Boolean,
        onPlayPause: () -> Unit = { onlinePlaybackManager.togglePlayPause() },
        onNext: () -> Unit = { onlinePlaybackManager.playNext() },
        onPrevious: () -> Unit = { onlinePlaybackManager.playPrevious() },
        onSeekTo: ((Long) -> Unit)? = null
    ) {
        isOnlinePlaybackActive = true
        currentOnlineSong = song
        currentSong = null
        isOnlinePlaying = isPlaying
        currentOnlinePosition = 0L
        currentOnlineDuration = (song.durationSeconds * 1000L).coerceAtLeast(0L)

        // Stop offline player if running
        playerManager.pause()

        val displaySong = getOnlineDisplaySong(song) ?: song.toSong()
        cachedOnlineLyrics = lyricsRepository.getCachedLyrics(displaySong)

        setupOnlineActions()
        PlaybackActionRegistry.onPlayPause = onPlayPause
        PlaybackActionRegistry.onNext = onNext
        PlaybackActionRegistry.onPrevious = onPrevious
        PlaybackActionRegistry.onSeekTo = { pos ->
            currentOnlinePosition = pos
            if (onSeekTo != null) {
                onSeekTo.invoke(pos)
            } else {
                onlinePlaybackManager.seekTo(pos)
            }
        }

        updateOnlineNotification(song, isPlaying)

        serviceScope.launch {
            val lyrics = if (cachedOnlineLyrics.isNotEmpty()) {
                cachedOnlineLyrics
            } else {
                withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(displaySong) }
            }
            if (lyrics.isNotEmpty() && currentOnlineSong?.id == song.id) {
                updateOnlineLyrics(song, lyrics)
            }
        }
    }

    fun updateOnlineLyrics(song: OnlineSong, lyrics: List<LyricLine>) {
        if (!isOnlinePlaybackActive || currentOnlineSong?.id != song.id) return
        cachedOnlineLyrics = lyrics
        val displaySong = getOnlineDisplaySong(song) ?: song.toSong()
        lyricsRepository.cacheLyrics(displaySong, lyrics)

        val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(displaySong, lyrics)

        retryJob?.cancel()
        serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(displaySong) }
            playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork, lyricInfoJson)

            if (lyricInfoJson.isNotBlank()) {
                retryJob = launch {
                    kotlinx.coroutines.delay(800)
                    if (currentOnlineSong?.id == song.id) {
                        val currentDisplay = getOnlineDisplaySong(song) ?: displaySong
                        playerManager.setMetadata(currentDisplay.title, currentDisplay.artist, currentDisplay.album, currentDisplay.duration, artwork, lyricInfoJson)
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

        var durationChanged = false
        if (durationMs > 0L && durationMs != currentOnlineDuration) {
            currentOnlineDuration = durationMs
            durationChanged = true
        } else if (songChanged) {
            currentOnlineDuration = (song.durationSeconds * 1000L).coerceAtLeast(0L)
        }

        val displaySong = getOnlineDisplaySong(song) ?: song.toSong()

        playerManager.updateSessionPlaybackState(isPlaying, positionMs)
        if (songChanged) {
            cachedOnlineLyrics = lyricsRepository.getCachedLyrics(displaySong)
            updateOnlineNotification(song, isPlaying)

            serviceScope.launch {
                val lyrics = if (cachedOnlineLyrics.isNotEmpty()) {
                    cachedOnlineLyrics
                } else {
                    withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(displaySong) }
                }
                if (lyrics.isNotEmpty() && currentOnlineSong?.id == song.id) {
                    updateOnlineLyrics(song, lyrics)
                }
            }
        } else if (playStateChanged || durationChanged) {
            updateOnlineNotification(song, isPlaying)
        }
    }

    fun updateOnlinePosition(positionMs: Long, durationMs: Long = 0L) {
        if (!isOnlinePlaybackActive) return
        currentOnlinePosition = positionMs
        var durationUpdated = false
        if (durationMs > 0L && (currentOnlineDuration <= 0L || Math.abs(durationMs - currentOnlineDuration) > 2000L)) {
            currentOnlineDuration = durationMs
            durationUpdated = true
        }
        if (getOnlineDisplaySong() == null) return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
        if (durationUpdated) {
            currentOnlineSong?.let { updateOnlineNotification(it, isOnlinePlaying) }
        }
    }

    fun onOnlineSeek(positionMs: Long) {
        if (!isOnlinePlaybackActive) return
        currentOnlinePosition = positionMs
        if (getOnlineDisplaySong() == null) return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtTrackEnd = false

        if (minutes == -1) {
            // Consume natural completion before the queue can advance to another track.
            sleepAtTrackEnd = true
            return
        }
        if (minutes <= 0) return

        sleepTimerJob = serviceScope.launch {
            kotlinx.coroutines.delay(minutes * 60_000L)
            pausePlaybackForSleepTimer()
            sleepTimerJob = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtTrackEnd = false
    }

    fun pausePlaybackForSleepTimer() {
        Log.d("AuraPlaybackDebug", "Sleep timer expired; pausing active playback")
        cancelSleepTimer()
        if (::onlinePlaybackManager.isInitialized && isOnlinePlaybackActive) {
            onlinePlaybackManager.pause()
            isOnlinePlaying = false
            currentOnlineSong?.let {
                updateOnlinePlaybackState(it, isPlaying = false, positionMs = currentOnlinePosition, durationMs = currentOnlineDuration)
            }
        }
        if (playerManager.isPlaying()) {
            playerManager.pause()
        }
        currentSong?.let { startAsForeground(it, false) }
    }

    fun stopOnlinePlayback() {
        if (!isOnlinePlaybackActive) return
        Log.d("AuraPlaybackDebug", "stopOnlinePlayback called")
        isOnlinePlaybackActive = false
        currentOnlineSong = null
        currentOnlineDuration = 0L
        cachedOnlineLyrics = emptyList()
        currentOnlinePosition = 0L
        setupActions()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clear()
    }

    private fun updateOnlineNotification(
        onlineSong: OnlineSong,
        isPlaying: Boolean
    ) {
        val displaySong = getOnlineDisplaySong(onlineSong) ?: onlineSong.toSong()
        val isSameSong = currentOnlineSong?.id == onlineSong.id
        Log.d("AuraPlaybackDebug", "updateOnlineNotification: song=${onlineSong.title}, isPlaying=$isPlaying, isOnlinePlaying=$isOnlinePlaying")

        retryJob?.cancel()
        notificationJob?.cancel()

        // 1. Immediate fast notification to maintain foreground service state without network delay
        val fastArtwork = notificationManager.loadArtworkFast(displaySong)
        val initialLyrics = if (cachedOnlineLyrics.isNotEmpty()) {
            cachedOnlineLyrics
        } else {
            lyricsRepository.getCachedLyrics(displaySong).also { if (it.isNotEmpty()) cachedOnlineLyrics = it }
        }
        val initialLyricInfo = when {
            initialLyrics.isNotEmpty() -> ColorOSLyricPayload.generateLyricInfoJson(displaySong, initialLyrics)
            isSameSong -> null
            else -> ""
        }
        playerManager.setMetadata(
            displaySong.title,
            displaySong.artist,
            displaySong.album,
            displaySong.duration,
            fastArtwork,
            lyricInfoJson = initialLyricInfo
        )
        val initialNotification = notificationManager.createNotification(
            displaySong,
            isPlaying,
            playerManager.getSessionToken(),
            customArtwork = fastArtwork
        )
        try {
            if (isPlaying || isOnlinePlaying) {
                acquireWakeLock()
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, initialNotification)
            } else {
                if (!playerManager.isPlaying()) releaseWakeLock()
                notificationManager.show(displaySong, false, playerManager.getSessionToken(), customArtwork = fastArtwork)
            }
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "updateOnlineNotification: initial startForeground error", e)
        }

        // 2. Asynchronous high-resolution artwork & metadata update
        notificationJob = serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(displaySong) }

            // Guard against stale track or state change during async artwork loading
            if (currentOnlineSong?.id != onlineSong.id || isOnlinePlaying != isPlaying) {
                Log.d("AuraPlaybackDebug", "updateOnlineNotification: state changed during async artwork load, aborting stale update")
                return@launch
            }

            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(displaySong, cachedOnlineLyrics)
            playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork ?: fastArtwork, lyricInfoJson)

            val updatedNotification = notificationManager.createNotification(
                displaySong,
                isPlaying,
                playerManager.getSessionToken(),
                customArtwork = artwork ?: fastArtwork
            )
            try {
                if (isPlaying || isOnlinePlaying) {
                    startForeground(PlaybackNotificationManager.NOTIFICATION_ID, updatedNotification)
                } else {
                    notificationManager.show(displaySong, false, playerManager.getSessionToken(), customArtwork = artwork ?: fastArtwork)
                }
            } catch (e: Exception) {
                Log.e("AuraPlaybackDebug", "updateOnlineNotification: error updating notification", e)
            }

            if (lyricInfoJson.isNotBlank()) {
                retryJob = launch {
                    kotlinx.coroutines.delay(800)
                    if (currentOnlineSong?.id == onlineSong.id) {
                        playerManager.setMetadata(displaySong.title, displaySong.artist, displaySong.album, displaySong.duration, artwork ?: fastArtwork, lyricInfoJson)
                    }
                }
            }
        }
    }

    private fun setupActions() {
        PlaybackActionRegistry.onPlayPause = { playerManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext(isManual = true) }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
        PlaybackActionRegistry.onSeekTo = { playerManager.seekTo(it) }
        PlaybackActionRegistry.onLyricToggle = {
            Log.d("AuraPlaybackDebug", "Lyric action triggered from notification or media session")
            serviceScope.launch {
                val next = !playerManager.isLyricActive
                playerManager.isLyricActive = next
                notificationManager.isLyricActive = next
                playerManager.updatePlaybackState()
                if (isOnlinePlaybackActive && currentOnlineSong != null) {
                    updateOnlineNotification(currentOnlineSong!!, isOnlinePlaying)
                } else if (currentSong != null) {
                    startAsForeground(currentSong!!, playerManager.isPlaying())
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.d("AuraPlaybackDebug", "PlaybackService restarted with null intent (system recreation)")
            if (!isOnlinePlaybackActive && currentSong == null) {
                serviceScope.launch {
                    val lastSource = LastPlayedStore.getLastSourceType(this@PlaybackService)
                    if (lastSource == LastPlayedStore.SOURCE_ONLINE) {
                        val lastSong = LastPlayedStore.getLastOnlineSong(this@PlaybackService)
                        val lastPos = LastPlayedStore.getLastPosition(this@PlaybackService)
                        val lastQueue = LastPlayedStore.getLastOnlineQueue(this@PlaybackService)
                        if (lastSong != null) {
                            onlinePlaybackManager.prepareSong(lastSong, lastPos, lastQueue)
                            startOnlinePlayback(lastSong, isPlaying = false)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isTaskRemoved = true

        // Explicitly stop playback when the user dismisses AuraMusic from Recents.
        // Normal lock-screen/background playback remains supported while the task is alive.
        Log.d("AuraPlaybackDebug", "Task removed; stopping online and local playback")
        if (::onlinePlaybackManager.isInitialized && isOnlinePlaybackActive) {
            onlinePlaybackManager.pause()
        }
        playerManager.pause()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clear()
        stopSelf()
    }

    fun getPlayerManager(): VlcPlayerManager = playerManager
    fun getNotificationManager(): PlaybackNotificationManager = notificationManager

    fun startAsForeground(song: Song, isPlaying: Boolean) {
        Log.d("AuraPlaybackDebug", "startAsForeground: song=${song.title}, isPlaying=$isPlaying")
        val isSameSong = currentSong?.id == song.id
        currentSong = song

        retryJob?.cancel()
        notificationJob?.cancel()

        // 1. Immediately create and attach foreground notification with cached/local artwork.
        // This guarantees the service is promoted to / kept in foreground instantly without
        // waiting for network I/O or LRCLIB fetching.
        val fastArtwork = notificationManager.loadArtworkFast(song)
        val cachedLyrics = lyricsRepository.getCachedLyrics(song)
        val initialLyricInfo = when {
            cachedLyrics.isNotEmpty() -> ColorOSLyricPayload.generateLyricInfoJson(song, cachedLyrics)
            isSameSong -> null
            else -> ""
        }
        playerManager.setMetadata(
            song.title,
            song.artist,
            song.album,
            song.duration,
            fastArtwork,
            lyricInfoJson = initialLyricInfo
        )
        val initialNotification = notificationManager.createNotification(
            song,
            isPlaying,
            playerManager.getSessionToken(),
            customArtwork = fastArtwork
        )
        try {
            if (isPlaying) {
                acquireWakeLock()
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, initialNotification)
            } else {
                if (!isOnlinePlaying) releaseWakeLock()
                notificationManager.show(song, false, playerManager.getSessionToken(), customArtwork = fastArtwork)
            }
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "Failed to startForeground initially", e)
        }

        // 2. Asynchronously load high-resolution artwork, lyrics, and metadata
        notificationJob = serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(song) }
            val lyrics = if (cachedLyrics.isNotEmpty()) {
                cachedLyrics
            } else {
                withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(song) }
            }

            // Guard against stale track if user or queue shifted during background load
            if (currentSong?.id != song.id) {
                Log.d("AuraPlaybackDebug", "startAsForeground: song changed during async load, discarding stale update")
                return@launch
            }

            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(song, lyrics)
            playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork ?: fastArtwork, lyricInfoJson)

            val finalNotification = notificationManager.createNotification(
                song,
                isPlaying,
                playerManager.getSessionToken(),
                customArtwork = artwork ?: fastArtwork
            )
            try {
                if (isPlaying) {
                    startForeground(PlaybackNotificationManager.NOTIFICATION_ID, finalNotification)
                } else {
                    notificationManager.show(song, false, playerManager.getSessionToken(), customArtwork = artwork ?: fastArtwork)
                }
            } catch (e: Exception) {
                Log.e("AuraPlaybackDebug", "Failed to update foreground notification with high-res artwork", e)
            }

            if (lyricInfoJson.isNotBlank()) {
                retryJob = launch {
                    kotlinx.coroutines.delay(800)
                    if (currentSong?.id == song.id) {
                        playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork ?: fastArtwork, lyricInfoJson)
                    }
                }
            }
        }
    }

    fun stopAsForeground(removeNotification: Boolean) {
        Log.d("AuraPlaybackDebug", "stopAsForeground called: removeNotification=$removeNotification")
        releaseWakeLock()
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        Log.d("AuraPlaybackDebug", "PlaybackService onDestroy called")
        if (isScreenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            isScreenReceiverRegistered = false
        }
        releaseWakeLock()
        notificationJob?.cancel()
        retryJob?.cancel()
        serviceScope.cancel()
        if (::onlinePlaybackManager.isInitialized) {
            onlinePlaybackManager.removeListener(onlinePlayerListener)
            OnlinePlaybackManagerHolder.releaseIfCreated()
        }
        playerManager.release()
        PlaybackQueueCoordinator.unregisterDispatcher()
        PlaybackQueueCoordinator.removeEngineSyncListener(queueSyncListener)
        if (currentInstance == this) {
            currentInstance = null
        }
        super.onDestroy()
    }
}
