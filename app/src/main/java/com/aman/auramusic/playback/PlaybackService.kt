package com.aman.auramusic.playback

import android.app.Service
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
import com.aman.auramusic.coloros.ColorOSLiveLyricsBridge
import com.aman.auramusic.coloros.ColorOSLyricPayload
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.online.player.OnlinePlaybackManager
import com.aman.auramusic.online.player.OnlinePlaybackManagerHolder
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toLyricLines
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.util.LastPlayedStore

class PlaybackService : Service() {

    private lateinit var playerManager: VlcPlayerManager
    private lateinit var notificationManager: PlaybackNotificationManager
    private lateinit var userRepository: UserPreferencesRepository
    private lateinit var colorOSBridge: ColorOSLiveLyricsBridge
    private lateinit var onlinePlaybackManager: OnlinePlaybackManager
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

    var isOnlinePlaybackActive: Boolean = false
        private set
    var currentOnlineSong: OnlineSong? = null
        private set
    private var cachedOnlineLyrics: List<LyricLine> = emptyList()
    private var isOnlinePlaying: Boolean = false
    private var currentOnlinePosition: Long = 0L
    private var currentOnlineDuration: Long = 0L

    private fun getOnlineDisplaySong(song: OnlineSong? = currentOnlineSong): Song? {
        val target = song ?: currentOnlineSong ?: return null
        val baseSong = target.toSong()
        return if (currentOnlineDuration > 0L) baseSong.copy(duration = currentOnlineDuration) else baseSong
    }

    private fun setupOnlineActions() {
        PlaybackActionRegistry.onPlayPause = { onlinePlaybackManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { onlinePlaybackManager.playNext() }
        PlaybackActionRegistry.onPrevious = { onlinePlaybackManager.playPrevious() }
        PlaybackActionRegistry.onSeekTo = { onlinePlaybackManager.seekTo(it) }
    }

    private val onlinePlayerListener = object : OnlinePlaybackManager.PlaybackEventListener {
        override fun onSongChanged(song: OnlineSong) {
            Log.d("AuraPlaybackDebug", "PlaybackService onlinePlayerListener.onSongChanged: ${song.title}")
            isOnlinePlaybackActive = true
            currentOnlineSong = song
            currentSong = null
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
            if (!isPlaying && !playerManager.isPlaying() && isTaskRemoved) {
                Log.d("AuraPlaybackDebug", "PlaybackService: Task removed and online playback paused, checking keepPlayingOnClose=$keepPlayingOnClose")
                if (!keepPlayingOnClose) {
                    stopSelf()
                }
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
            if (isTaskRemoved && !keepPlayingOnClose) {
                stopSelf()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = VlcPlayerManager(this)
        notificationManager = PlaybackNotificationManager(this)
        userRepository = UserPreferencesRepository(this)
        colorOSBridge = ColorOSLiveLyricsBridge(this)
        // Share the online player with the UI, but keep its lifetime tied to this service.
        onlinePlaybackManager = OnlinePlaybackManagerHolder.get(this)
        onlinePlaybackManager.addListener(onlinePlayerListener)
        
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
                Log.d("AuraPlaybackDebug", "PlaybackService onPlaybackState: isPlaying=$isPlaying, isTransitioning=${playerManager.isTransitioning}, currentSong=${currentSong?.title}")
                currentSong?.let { song ->
                    if (isPlaying) {
                        startAsForeground(song, true)
                    } else {
                        // Do not show paused notification or detach if actively transitioning to next song
                        if (!playerManager.isTransitioning) {
                            serviceScope.launch {
                                notificationManager.show(song, false, playerManager.getSessionToken())
                            }
                            colorOSBridge.onPlaybackStateChanged(song, false, playerManager.position())
                        } else {
                            Log.d("AuraPlaybackDebug", "PlaybackService: Suppressing paused notification during transition")
                        }
                    }
                }
                if (!isPlaying && !isOnlinePlaybackActive && isTaskRemoved && !playerManager.isTransitioning) {
                    Log.d("AuraPlaybackDebug", "PlaybackService: Task was removed and playback stopped, stopping service")
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

    private fun handlePlaybackEnd() {
        Log.d("AuraPlaybackDebug", "handlePlaybackEnd called. RepeatMode: $repeatMode, currentSong: ${currentSong?.title}")
        when (repeatMode) {
            RepeatMode.ONE -> {
                currentSong?.let { play(it) }
            }
            else -> {
                playNext(isManual = false)
            }
        }
    }

    fun playNext(isManual: Boolean = false) {
        if (queue.isEmpty()) {
            Log.d("AuraPlaybackDebug", "playNext: Queue is empty")
            return
        }
        val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
        Log.d("AuraPlaybackDebug", "playNext: currentIndex=$currentIndex, queueSize=${queue.size}, repeatMode=$repeatMode, isManual=$isManual")

        if (currentIndex == -1) {
            play(queue[0])
            return
        }

        val nextIndex = currentIndex + 1
        if (nextIndex >= queue.size) {
            when (repeatMode) {
                RepeatMode.ALL -> {
                    play(queue[0])
                }
                RepeatMode.ONE -> {
                    currentSong?.let { play(it) } ?: play(queue[0])
                }
                RepeatMode.NONE -> {
                    if (isManual) {
                        play(queue[0])
                    } else {
                        Log.d("AuraPlaybackDebug", "playNext: Natural end of queue reached with RepeatMode.NONE. Cleanly pausing.")
                        playerManager.pause()
                    }
                }
            }
        } else {
            play(queue[nextIndex])
        }
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
        val prevIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        Log.d("AuraPlaybackDebug", "playPrevious: currentIndex=$currentIndex, prevIndex=$prevIndex")
        play(queue[prevIndex])
    }

    fun play(song: Song) {
        Log.d("AuraPlaybackDebug", "play() called for: ${song.title} (${song.id})")
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
        cachedOnlineLyrics = emptyList()

        // Stop offline player if running
        playerManager.pause()

        val displaySong = getOnlineDisplaySong(song) ?: song.toSong()

        setupOnlineActions()
        PlaybackActionRegistry.onPlayPause = onPlayPause
        PlaybackActionRegistry.onNext = onNext
        PlaybackActionRegistry.onPrevious = onPrevious
        PlaybackActionRegistry.onSeekTo = { pos ->
            currentOnlinePosition = pos
            val curDisplay = getOnlineDisplaySong() ?: displaySong
            colorOSBridge.onSeek(curDisplay, pos, isOnlinePlaying)
            if (onSeekTo != null) {
                onSeekTo.invoke(pos)
            } else {
                onlinePlaybackManager.seekTo(pos)
            }
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
        val displaySong = getOnlineDisplaySong(song) ?: song.toSong()

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
        val displaySong = getOnlineDisplaySong() ?: return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
        colorOSBridge.onPlaybackStateChanged(displaySong, isOnlinePlaying, positionMs)
        if (durationUpdated) {
            currentOnlineSong?.let { updateOnlineNotification(it, isOnlinePlaying) }
        }
    }

    fun onOnlineSeek(positionMs: Long) {
        if (!isOnlinePlaybackActive) return
        currentOnlinePosition = positionMs
        val displaySong = getOnlineDisplaySong() ?: return
        playerManager.updateSessionPlaybackState(isOnlinePlaying, positionMs)
        colorOSBridge.onSeek(displaySong, positionMs, isOnlinePlaying)
    }

    fun stopOnlinePlayback() {
        if (!isOnlinePlaybackActive) return
        Log.d("AuraPlaybackDebug", "stopOnlinePlayback called")
        val displaySong = getOnlineDisplaySong() ?: currentOnlineSong?.toSong()
        isOnlinePlaybackActive = false
        currentOnlineSong = null
        currentOnlineDuration = 0L
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
        val displaySong = getOnlineDisplaySong(onlineSong) ?: onlineSong.toSong()
        Log.d("AuraPlaybackDebug", "updateOnlineNotification: song=${onlineSong.title}, isPlaying=$isPlaying, isOnlinePlaying=$isOnlinePlaying")

        retryJob?.cancel()
        notificationJob?.cancel()

        // 1. Immediate fast notification to maintain foreground service state without network delay
        val fastArtwork = notificationManager.loadArtworkFast(displaySong)
        val initialNotification = notificationManager.createNotification(
            displaySong,
            isPlaying,
            playerManager.getSessionToken(),
            customArtwork = fastArtwork
        )
        try {
            if (isPlaying || isOnlinePlaying) {
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, initialNotification)
            } else {
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
        }
    }

    private fun setupActions() {
        PlaybackActionRegistry.onPlayPause = { playerManager.togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext(isManual = true) }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
        PlaybackActionRegistry.onSeekTo = { playerManager.seekTo(it) }
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
        val isAnyPlaying = if (isOnlinePlaybackActive) isOnlinePlaying else playerManager.isPlaying()
        Log.d("AuraPlaybackDebug", "onTaskRemoved: keepPlayingOnClose=$keepPlayingOnClose, isAnyPlaying=$isAnyPlaying")
        if (!keepPlayingOnClose || !isAnyPlaying) {
            stopSelf()
        }
    }

    fun getPlayerManager(): VlcPlayerManager = playerManager
    fun getNotificationManager(): PlaybackNotificationManager = notificationManager

    fun startAsForeground(song: Song, isPlaying: Boolean) {
        Log.d("AuraPlaybackDebug", "startAsForeground: song=${song.title}, isPlaying=$isPlaying")
        currentSong = song

        retryJob?.cancel()
        notificationJob?.cancel()

        // 1. Immediately create and attach foreground notification with cached/local artwork.
        // This guarantees the service is promoted to / kept in foreground instantly without
        // waiting for network I/O or LRCLIB fetching.
        val fastArtwork = notificationManager.loadArtworkFast(song)
        val initialNotification = notificationManager.createNotification(
            song,
            isPlaying,
            playerManager.getSessionToken(),
            customArtwork = fastArtwork
        )
        try {
            if (isPlaying) {
                startForeground(PlaybackNotificationManager.NOTIFICATION_ID, initialNotification)
            } else {
                notificationManager.show(song, false, playerManager.getSessionToken(), customArtwork = fastArtwork)
            }
        } catch (e: Exception) {
            Log.e("AuraPlaybackDebug", "Failed to startForeground initially", e)
        }

        // 2. Asynchronously load high-resolution artwork, lyrics, and metadata
        notificationJob = serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { notificationManager.loadArtwork(song) }
            val lyrics = withContext(Dispatchers.IO) { lyricsRepository.lyricsFor(song) }

            // Guard against stale track if user or queue shifted during background load
            if (currentSong?.id != song.id) {
                Log.d("AuraPlaybackDebug", "startAsForeground: song changed during async load, discarding stale update")
                return@launch
            }

            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(song, lyrics)
            playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork ?: fastArtwork, lyricInfoJson)
            colorOSBridge.onTrackChanged(song, lyrics, isPlaying, playerManager.position())

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
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        val activeSong = currentSong ?: currentOnlineSong?.toSong()
        colorOSBridge.onPlaybackStopped(activeSong)
    }

    override fun onDestroy() {
        Log.d("AuraPlaybackDebug", "PlaybackService onDestroy called")
        val activeSong = currentSong ?: currentOnlineSong?.toSong()
        colorOSBridge.onPlaybackStopped(activeSong)
        notificationJob?.cancel()
        retryJob?.cancel()
        serviceScope.cancel()
        if (::onlinePlaybackManager.isInitialized) {
            onlinePlaybackManager.removeListener(onlinePlayerListener)
            OnlinePlaybackManagerHolder.releaseIfCreated()
        }
        playerManager.release()
        super.onDestroy()
    }
}
