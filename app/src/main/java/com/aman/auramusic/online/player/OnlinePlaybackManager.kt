package com.aman.auramusic.online.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.PlaybackState
import com.aman.auramusic.online.model.StreamingEngine
import com.aman.auramusic.online.model.toOnlineSong
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
import com.aman.auramusic.online.util.LastPlayedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Unified Online Audio Playback Engine orchestrating ExoPlayer and RiPlay IFrame Player.
 * - JioSaavn: Plays untouched high-fidelity 320kbps streams on AndroidX Media3 ExoPlayer with caching.
 * - YouTube Music: Offers 4 selectable engine strategies inspired by the research repositories:
 *     1. RiPlay (fast4x/RiPlay): Headless YouTube IFrame Player wrapper (100% zero-block, immune to scraper breakages).
 *     2. BitChord (kushagrasinghx/BitChord): InnerTubeX multi-client rotation.
 *     3. DA-Tunes (VikrantRuhela/DA-Tunes): Direct audio stream manifest resolver.
 *     4. Obsidian (cr7pt0gr4ph7/obsidian): Web stream controller.
 */
@OptIn(UnstableApi::class)
class OnlinePlaybackManager(
    private val context: Context,
    private val repository: OnlineMusicRepository = OnlineMusicRepository()
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var preloadJob: Job? = null
    private val lyricsManager: LyricsManager = LyricsManager()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    interface PlaybackEventListener {
        fun onSongChanged(song: OnlineSong) {}
        /** Return true to consume natural completion and prevent queue advancement. */
        fun onSongEnded(song: OnlineSong): Boolean = false
        fun onPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {}
        fun onProgressUpdate(song: OnlineSong, positionMs: Long, durationMs: Long = 0L) {}
        fun onSeek(song: OnlineSong, positionMs: Long) {}
        fun onLyricsLoaded(song: OnlineSong, lyrics: com.aman.auramusic.online.lyrics.model.SongLyrics) {}
        fun onPlaybackStopped(song: OnlineSong?) {}
    }

    var eventListener: PlaybackEventListener? = null
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<PlaybackEventListener>()

    fun addListener(listener: PlaybackEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PlaybackEventListener) {
        listeners.remove(listener)
    }

    private fun dispatchSongChanged(song: OnlineSong) {
        eventListener?.onSongChanged(song)
        listeners.forEach { runCatching { it.onSongChanged(song) } }
    }

    private fun dispatchPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {
        eventListener?.onPlaybackStateChanged(song, isPlaying, positionMs)
        listeners.forEach { runCatching { it.onPlaybackStateChanged(song, isPlaying, positionMs) } }
    }

    private fun dispatchProgressUpdate(song: OnlineSong, positionMs: Long, durationMs: Long = 0L) {
        eventListener?.onProgressUpdate(song, positionMs, durationMs)
        listeners.forEach { runCatching { it.onProgressUpdate(song, positionMs, durationMs) } }
    }

    private fun dispatchSeek(song: OnlineSong, positionMs: Long) {
        eventListener?.onSeek(song, positionMs)
        listeners.forEach { runCatching { it.onSeek(song, positionMs) } }
    }

    private fun dispatchLyricsLoaded(song: OnlineSong, lyrics: com.aman.auramusic.online.lyrics.model.SongLyrics) {
        eventListener?.onLyricsLoaded(song, lyrics)
        listeners.forEach { runCatching { it.onLyricsLoaded(song, lyrics) } }
    }

    private fun dispatchPlaybackStopped(song: OnlineSong?) {
        eventListener?.onPlaybackStopped(song)
        listeners.forEach { runCatching { it.onPlaybackStopped(song) } }
    }

    private var isUsingIFramePlayer = false
    private var hasStartedPlaybackForCurrentSong = false
    var isTransitioning: Boolean = false
        private set
    private var lastEndHandledTimeMs: Long = 0L

    // 1. AndroidX Media3 ExoPlayer for direct CDN streams (JioSaavn 320k untouched)
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AuraMusicPlayer/1.0.2")
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setAllowCrossProtocolRedirects(true)

    private val cachedDataSourceFactory = CacheManager.buildCacheDataSourceFactory(
        context,
        httpDataSourceFactory
    )

    private val mediaSourceFactory = DefaultMediaSourceFactory(cachedDataSourceFactory)

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        // Allow Media3 to manage the wake lock needed for reliable network audio playback.
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (isUsingIFramePlayer) return
                    Log.d("AuraPlaybackDebug", "ExoPlayer onPlaybackStateChanged: state=$state")
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _playbackState.update { it.copy(isBuffering = true) }
                        }
                        Player.STATE_READY -> {
                            isTransitioning = false
                            _playbackState.update {
                                it.copy(
                                    isBuffering = false,
                                    durationMs = duration.coerceAtLeast(0L),
                                    errorMessage = null
                                )
                            }
                        }
                        Player.STATE_ENDED -> {
                            Log.d("AuraPlaybackDebug", "ExoPlayer STATE_ENDED reached")
                            _playbackState.update { it.copy(isBuffering = false, isPlaying = false) }
                            handleSongEnded()
                        }
                        Player.STATE_IDLE -> {
                            _playbackState.update { it.copy(isBuffering = false) }
                        }
                    }
                }

                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    if (isUsingIFramePlayer) return
                    val format = exoPlayer.audioFormat
                    if (format != null) {
                        _playbackState.update {
                            it.copy(
                                measuredBitrate = if (format.bitrate != androidx.media3.common.Format.NO_VALUE) format.bitrate else null,
                                measuredSampleRate = if (format.sampleRate != androidx.media3.common.Format.NO_VALUE) format.sampleRate else null,
                                measuredMimeType = format.sampleMimeType ?: format.containerMimeType
                            )
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isUsingIFramePlayer) return
                    Log.d("AuraPlaybackDebug", "ExoPlayer onIsPlayingChanged: isPlaying=$isPlaying, isTransitioning=$isTransitioning")
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        isTransitioning = false
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                    _playbackState.value.currentSong?.let { song ->
                        if (!isPlaying && isTransitioning) {
                            Log.d("AuraPlaybackDebug", "Suppressing ExoPlayer onPlaybackStateChanged(false) for ${song.title} during track transition")
                        } else {
                            dispatchPlaybackStateChanged(song, isPlaying, exoPlayer.currentPosition.coerceAtLeast(0L))
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (isUsingIFramePlayer) return
                    Log.e("AuraPlaybackDebug", "ExoPlayer error: ${error.message}", error)
                    isTransitioning = false
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            errorMessage = "ExoPlayer error: ${error.message ?: "Unknown error"}"
                        )
                    }
                }
            })
        }

    // 2. Headless YouTube IFrame Player (fast4x/RiPlay zero-block strategy)
    private val youTubeIFramePlayer: YouTubeIFramePlayer = YouTubeIFramePlayer(
        context = context,
        listener = object : YouTubeIFramePlayer.PlayerListener {
            override fun onPlaybackStateChanged(isPlaying: Boolean, isBuffering: Boolean) {
                if (!isUsingIFramePlayer) return
                Log.d("AuraPlaybackDebug", "YouTubeIFrame onPlaybackStateChanged: isPlaying=$isPlaying, isBuffering=$isBuffering, isTransitioning=$isTransitioning")
                if (isPlaying) {
                    isTransitioning = false
                }
                _playbackState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        activePlayerType = "RiPlay IFrame"
                    )
                }
                _playbackState.value.currentSong?.let { song ->
                    if (!isPlaying && isTransitioning) {
                        Log.d("AuraPlaybackDebug", "Suppressing YouTube onPlaybackStateChanged(false) for ${song.title} during track transition")
                    } else {
                        dispatchPlaybackStateChanged(song, isPlaying, _playbackState.value.currentPositionMs)
                    }
                }
            }

            override fun onProgressUpdate(currentMs: Long, durationMs: Long) {
                if (!isUsingIFramePlayer) return
                val lyrics = _playbackState.value.currentLyrics
                val activeLine = lyrics?.findActiveLineIndex(currentMs) ?: -1
                val effectiveDur = if (durationMs > 0) durationMs else _playbackState.value.durationMs
                _playbackState.update {
                    val updatedSong = if (it.currentSong != null && it.currentSong.durationSeconds <= 0L && effectiveDur > 0L) {
                        it.currentSong.copy(durationSeconds = effectiveDur / 1000L)
                    } else it.currentSong
                    it.copy(
                        currentSong = updatedSong,
                        currentPositionMs = currentMs,
                        durationMs = effectiveDur,
                        activeLyricsLineIndex = activeLine,
                        bufferedPositionMs = (currentMs + 20000).coerceAtMost(effectiveDur)
                    )
                }
                _playbackState.value.currentSong?.let { song ->
                    dispatchProgressUpdate(song, currentMs, effectiveDur)
                    if (currentMs > 0 && currentMs % 5000 < 300) {
                        LastPlayedStore.updatePosition(context, currentMs)
                    }
                }
            }

            override fun onSongEnded() {
                if (!isUsingIFramePlayer) return
                Log.d("AuraPlaybackDebug", "YouTube onSongEnded received")
                handleSongEnded()
            }

            override fun onError(message: String) {
                if (!isUsingIFramePlayer) return
                Log.e("AuraPlaybackDebug", "YouTube error: $message")
                isTransitioning = false
                _playbackState.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = message
                    )
                }
            }
        }
    )

    init {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.addEngineSyncListener { tracks ->
            val onlineTracks = tracks.mapNotNull { it.onlineSong ?: it.song.toOnlineSong() }
            val curId = _playbackState.value.currentSong?.id
            val newIdx = if (curId != null) onlineTracks.indexOfFirst { it.id == curId } else -1
            _playbackState.update { it.copy(queue = onlineTracks, queueIndex = newIdx) }
        }
        scope.launch {
            com.aman.auramusic.playback.PlaybackQueueCoordinator.isShuffledFlow.collect { shuffled ->
                _playbackState.update { it.copy(isShuffleEnabled = shuffled) }
            }
        }
        scope.launch {
            com.aman.auramusic.playback.PlaybackQueueCoordinator.repeatModeFlow.collect { mode ->
                _playbackState.update {
                    it.copy(
                        repeatMode = mode,
                        isRepeatEnabled = (mode != com.aman.auramusic.playback.RepeatMode.NONE)
                    )
                }
                exoPlayer.repeatMode = if (mode == com.aman.auramusic.playback.RepeatMode.ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setStreamingEngine(engine: StreamingEngine) {
        _playbackState.update { it.copy(selectedEngine = engine) }
    }

    /**
     * Prepares an online song from persistent storage so that it appears immediately in
     * the MiniPlayer, PlayerScreen, and Queue in a clean paused state ready to play.
     */
    fun prepareSong(song: OnlineSong, initialPositionMs: Long = 0L, initialQueue: List<OnlineSong> = emptyList()) {
        hasStartedPlaybackForCurrentSong = false
        isUsingIFramePlayer = (song.source == AudioSource.YOUTUBE)
        val finalQueue = if (initialQueue.isNotEmpty()) initialQueue else listOf(song)
        val index = finalQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _playbackState.update {
            it.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = null,
                currentSong = song,
                currentPositionMs = initialPositionMs,
                durationMs = (song.durationSeconds * 1000L).coerceAtLeast(0L),
                queue = finalQueue,
                queueIndex = index,
                isLoadingLyrics = true
            )
        }

        dispatchSongChanged(song)
        dispatchPlaybackStateChanged(song, false, initialPositionMs)

        lyricsJob?.cancel()
        lyricsJob = scope.launch {
            val lyrics = lyricsManager.getOrFetchLyrics(song)
            _playbackState.update {
                if (it.currentSong?.id == song.id) {
                    it.copy(currentLyrics = lyrics, isLoadingLyrics = false)
                } else it
            }
            if (_playbackState.value.currentSong?.id == song.id) {
                dispatchLyricsLoaded(song, lyrics)
            }
        }
    }

    fun playSong(song: OnlineSong, newQueue: List<OnlineSong> = emptyList(), startPositionMs: Long = 0L) {
        Log.d("AuraPlaybackDebug", "OnlinePlaybackManager.playSong: ${song.title} (${song.id}), source=${song.source}, startPositionMs=$startPositionMs")
        if (newQueue.isNotEmpty()) {
            com.aman.auramusic.playback.PlaybackQueueCoordinator.setQueueOnline(newQueue, current = song)
        } else {
            com.aman.auramusic.playback.PlaybackQueueCoordinator.setCurrentOnlineSong(song)
        }
        isTransitioning = true
        hasStartedPlaybackForCurrentSong = true
        scope.launch {
            val effectiveQueue = if (newQueue.isNotEmpty()) newQueue else if (!_playbackState.value.queue.contains(song)) _playbackState.value.queue + song else _playbackState.value.queue
            LastPlayedStore.saveOnline(context, song, startPositionMs, effectiveQueue)

            _playbackState.update {
                it.copy(
                    isBuffering = true,
                    errorMessage = null,
                    currentSong = song,
                    currentPositionMs = startPositionMs,
                    currentLyrics = null,
                    activeLyricsLineIndex = -1,
                    isLoadingLyrics = true,
                    queue = effectiveQueue
                )
            }

            dispatchSongChanged(song)

            // Cancel any pending lyrics fetching job and fetch for current track
            lyricsJob?.cancel()
            lyricsJob = scope.launch {
                val lyrics = lyricsManager.getOrFetchLyrics(song)
                _playbackState.update {
                    if (it.currentSong?.id == song.id) {
                        it.copy(currentLyrics = lyrics, isLoadingLyrics = false)
                    } else it
                }
                if (_playbackState.value.currentSong?.id == song.id) {
                    dispatchLyricsLoaded(song, lyrics)
                }
            }

            val currentQueue = _playbackState.value.queue
            val index = currentQueue.indexOfFirst { it.id == song.id }
            _playbackState.update { it.copy(queueIndex = index) }

            // Safe preloading of upcoming tracks with superseding cancellation
            preloadJob?.cancel()
            if (index != -1 && index + 1 < currentQueue.size) {
                val nextSong = currentQueue[index + 1]
                preloadJob = CacheManager.preloadUpcomingTrack(context, nextSong)
            }

            // ========================================================
            // PLAYBACK ROUTING: JioSaavn vs YouTube Engine Strategies
            // ========================================================
            if (song.source == AudioSource.JIOSAAVN) {
                isUsingIFramePlayer = false
                youTubeIFramePlayer.pause()

                val resolvedSong = repository.resolveStream(song, _playbackState.value.selectedBitrate.replace(" kbps", ""))
                if (resolvedSong.streamUrl.isBlank()) {
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            errorMessage = "Failed to resolve JioSaavn stream for ${song.title}"
                        )
                    }
                    return@launch
                }

                _playbackState.update {
                    it.copy(
                        currentSong = resolvedSong,
                        activePlayerType = "ExoPlayer (JioSaavn 320k)"
                    )
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(resolvedSong.streamUrl)
                    .setMediaId(resolvedSong.id)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                if (startPositionMs > 0L) {
                    exoPlayer.seekTo(startPositionMs)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

            } else {
                val engine = _playbackState.value.selectedEngine

                when (engine) {
                    StreamingEngine.RIPLAY -> {
                        isUsingIFramePlayer = true
                        exoPlayer.pause()
                        stopProgressTracker()

                        val resolvedSong = song.copy(
                            streamUrl = "https://www.youtube.com/watch?v=${song.id}",
                            extractorTier = "RiPlay Headless Player (v13.0.0)",
                            bitrate = "Official Stream",
                            format = "IFrame Audio"
                        )

                        _playbackState.update {
                            it.copy(
                                currentSong = resolvedSong,
                                activePlayerType = "RiPlay Headless Player"
                            )
                        }

                        val startSec = startPositionMs / 1000f
                        youTubeIFramePlayer.loadAndPlay(song.id, forceWebController = false, startSeconds = startSec)
                    }

                    StreamingEngine.OBSIDIAN -> {
                        isUsingIFramePlayer = true
                        exoPlayer.pause()
                        stopProgressTracker()

                        val resolvedSong = song.copy(
                            streamUrl = "https://m.youtube.com/watch?v=${song.id}",
                            extractorTier = "Obsidian Direct Web Player (Unrestricted)",
                            bitrate = "Mobile Web Audio",
                            format = "HTML5 Audio"
                        )

                        _playbackState.update {
                            it.copy(
                                currentSong = resolvedSong,
                                activePlayerType = "Obsidian Web Controller"
                            )
                        }

                        val startSec = startPositionMs / 1000f
                        youTubeIFramePlayer.loadAndPlay(song.id, forceWebController = true, startSeconds = startSec)
                    }

                    StreamingEngine.BITCHORD, StreamingEngine.DA_TUNES -> {
                        val resolvedSong = repository.resolveStream(song)

                        if (resolvedSong.streamUrl.isNotBlank()) {
                            isUsingIFramePlayer = false
                            youTubeIFramePlayer.pause()

                            _playbackState.update {
                                it.copy(
                                    currentSong = resolvedSong,
                                    activePlayerType = "ExoPlayer (${engine.shortName})"
                                )
                            }

                            val mediaItem = MediaItem.Builder()
                                .setUri(resolvedSong.streamUrl)
                                .setMediaId(resolvedSong.id)
                                .build()

                            exoPlayer.setMediaItem(mediaItem)
                            if (startPositionMs > 0L) {
                                exoPlayer.seekTo(startPositionMs)
                            }
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            isUsingIFramePlayer = true
                            exoPlayer.pause()
                            stopProgressTracker()

                            val failoverSong = song.copy(
                                streamUrl = "https://www.youtube.com/watch?v=${song.id}",
                                extractorTier = "${engine.shortName} -> RiPlay Failover",
                                bitrate = "Official Stream",
                                format = "IFrame Audio"
                            )

                            _playbackState.update {
                                it.copy(
                                    currentSong = failoverSong,
                                    activePlayerType = "RiPlay (Failover)",
                                    errorMessage = null
                                )
                            }

                            val startSec = startPositionMs / 1000f
                            youTubeIFramePlayer.loadAndPlay(song.id, forceWebController = false, startSeconds = startSec)
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        val song = _playbackState.value.currentSong
        if (song != null && !hasStartedPlaybackForCurrentSong) {
            val targetPos = _playbackState.value.currentPositionMs.takeIf { it > 0 }
                ?: LastPlayedStore.getLastPosition(context)
            playSong(song, _playbackState.value.queue, targetPos)
            return
        }

        if (isUsingIFramePlayer) {
            if (_playbackState.value.isPlaying) {
                youTubeIFramePlayer.pause()
            } else {
                youTubeIFramePlayer.play()
            }
        } else {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                }
                exoPlayer.play()
            }
        }
    }

    fun pause() {
        Log.d("AuraPlaybackDebug", "OnlinePlaybackManager.pause() called")
        isTransitioning = false
        if (isUsingIFramePlayer) {
            youTubeIFramePlayer.pause()
        } else {
            exoPlayer.pause()
        }
        LastPlayedStore.updatePosition(context, _playbackState.value.currentPositionMs)
        _playbackState.value.currentSong?.let { song ->
            dispatchPlaybackStateChanged(song, false, _playbackState.value.currentPositionMs)
        }
    }

    fun seekTo(positionMs: Long) {
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        LastPlayedStore.updatePosition(context, positionMs)
        _playbackState.value.currentSong?.let { song ->
            dispatchSeek(song, positionMs)
        }
        if (!hasStartedPlaybackForCurrentSong) {
            return
        }
        if (isUsingIFramePlayer) {
            youTubeIFramePlayer.seekTo(positionMs)
        } else {
            exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
        }
    }

    fun seekToFraction(fraction: Float) {
        val duration = _playbackState.value.durationMs
        val targetMs = (duration * fraction).toLong()
        seekTo(targetMs)
    }

    fun playNext(isManual: Boolean = false) {
        val started = com.aman.auramusic.playback.PlaybackQueueCoordinator.playNextTrack(isManual)
        if (started) return

        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) {
            Log.d("AuraPlaybackDebug", "playNext: Online queue is empty")
            return
        }

        Log.d("AuraPlaybackDebug", "Online playNext: queueIndex=${state.queueIndex}, queueSize=${queue.size}, isShuffle=${state.isShuffleEnabled}, isRepeat=${state.isRepeatEnabled}, isManual=$isManual")

        if (!state.isShuffleEnabled && !state.isRepeatEnabled && !isManual && state.queueIndex >= queue.size - 1) {
            Log.d("AuraPlaybackDebug", "Online queue natural end reached with repeat disabled. Cleanly pausing.")
            isTransitioning = false
            pause()
            return
        }

        val nextIndex = if (state.isShuffleEnabled) {
            queue.indices.random()
        } else {
            (state.queueIndex + 1) % queue.size
        }

        playSong(queue[nextIndex])
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }

        val started = com.aman.auramusic.playback.PlaybackQueueCoordinator.playPreviousTrack()
        if (started) return

        val queue = state.queue
        if (queue.isEmpty()) return

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else queue.size - 1
        playSong(queue[prevIndex])
    }

    fun toggleShuffle() {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.toggleShuffle()
    }

    fun toggleRepeat() {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.toggleRepeat()
    }

    fun changeBitrate(bitrate: String) {
        _playbackState.update { it.copy(selectedBitrate = "$bitrate kbps") }
        val currentSong = _playbackState.value.currentSong
        if (currentSong != null && currentSong.source == AudioSource.JIOSAAVN) {
            val currentPos = exoPlayer.currentPosition
            playSong(currentSong)
            exoPlayer.seekTo(currentPos)
        }
    }

    fun toggleLyricsMode() {
        _playbackState.update { it.copy(isLyricsMode = !it.isLyricsMode) }
    }

    fun addToQueue(song: OnlineSong) {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.addToQueue(
            com.aman.auramusic.playback.PlaybackQueueCoordinator.createTrack(song)
        )
    }

    fun removeQueueItem(songId: String) {
        val target = _playbackState.value.queue.find { it.id == songId }
        val targetKey = if (target != null) "online:${target.source}:${target.id}" else "online:$songId"
        com.aman.auramusic.playback.PlaybackQueueCoordinator.removeTrack(targetKey)
    }

    fun clearQueueExceptCurrent() {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.clearQueueExceptCurrent()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        com.aman.auramusic.playback.PlaybackQueueCoordinator.moveTrack(fromIndex, toIndex)
    }

    private fun handleSongEnded() {
        val now = System.currentTimeMillis()
        val state = _playbackState.value

        // Validate the completion before allowing the sleep timer to consume it. Some online
        // engines can emit transient/duplicate end callbacks during source transitions.
        if (!NaturalTrackEndGuard.shouldHandle(
                nowMs = now,
                lastHandledTimeMs = lastEndHandledTimeMs,
                durationMs = state.durationMs,
                positionMs = state.currentPositionMs
            )
        ) {
            if (now - lastEndHandledTimeMs < 500L) {
                Log.d("AuraPlaybackDebug", "handleSongEnded debounced duplicate trigger within 500ms")
            } else {
                Log.w(
                    "AuraPlaybackDebug",
                    "handleSongEnded ignored: premature end event at pos=${state.currentPositionMs}ms (duration=${state.durationMs}ms)"
                )
            }
            return
        }

        // Record every accepted completion, including one consumed by the sleep timer, so a
        // second callback from the same track cannot advance the queue after the timer fires.
        lastEndHandledTimeMs = now

        val endedSong = state.currentSong
        if (endedSong != null) {
            val consumed = (eventListener?.let { listener ->
                runCatching { listener.onSongEnded(endedSong) }.getOrDefault(false)
            } == true) || listeners.any { listener ->
                runCatching { listener.onSongEnded(endedSong) }.getOrDefault(false)
            }
            if (consumed) {
                Log.d("AuraPlaybackDebug", "Natural end consumed by sleep timer")
                return
            }
        }

        val currentRepeatMode = com.aman.auramusic.playback.PlaybackQueueCoordinator.repeatMode
        Log.d("AuraPlaybackDebug", "handleSongEnded triggered: repeatMode=$currentRepeatMode")

        val started = com.aman.auramusic.playback.PlaybackQueueCoordinator.playNextTrack(isManual = false)
        if (!started) {
            Log.d("AuraPlaybackDebug", "handleSongEnded: End of queue reached cleanly, pausing.")
            pause()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (!isUsingIFramePlayer) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val lyrics = _playbackState.value.currentLyrics
                    val activeLine = lyrics?.findActiveLineIndex(currentPos) ?: -1

                    val exoDur = exoPlayer.duration.coerceAtLeast(0L)
                    _playbackState.update {
                        val updatedSong = if (it.currentSong != null && it.currentSong.durationSeconds <= 0L && exoDur > 0L) {
                            it.currentSong.copy(durationSeconds = exoDur / 1000L)
                        } else it.currentSong
                        it.copy(
                            currentSong = updatedSong,
                            currentPositionMs = currentPos,
                            durationMs = exoDur,
                            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
                            activeLyricsLineIndex = activeLine
                        )
                    }
                    _playbackState.value.currentSong?.let { song ->
                        dispatchProgressUpdate(song, currentPos, exoDur)
                        if (currentPos > 0 && currentPos % 5000 < 200) {
                            LastPlayedStore.updatePosition(context, currentPos)
                        }
                    }
                }
                delay(150)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        Log.d("AuraPlaybackDebug", "OnlinePlaybackManager.release() called")
        isTransitioning = false
        dispatchPlaybackStopped(_playbackState.value.currentSong)
        stopProgressTracker()
        exoPlayer.release()
        youTubeIFramePlayer.release()
    }
}


/**
 * Shares one online player between the UI and PlaybackService. The service releases it
 * only when the service itself is destroyed; Compose disposal must not stop audio.
 */
object OnlinePlaybackManagerHolder {
    @Volatile
    private var instance: OnlinePlaybackManager? = null

    @Synchronized
    fun get(context: Context): OnlinePlaybackManager {
        return instance ?: OnlinePlaybackManager(context.applicationContext).also { instance = it }
    }

    @Synchronized
    fun releaseIfCreated() {
        val current = instance ?: return
        instance = null
        current.release()
    }
}
