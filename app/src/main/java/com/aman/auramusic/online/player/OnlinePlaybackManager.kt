package com.aman.auramusic.online.player

import android.content.Context
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
import com.aman.auramusic.online.network.repository.OnlineMusicRepository
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
    private val lyricsManager: LyricsManager = LyricsManager()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    interface PlaybackEventListener {
        fun onSongChanged(song: OnlineSong) {}
        fun onPlaybackStateChanged(song: OnlineSong, isPlaying: Boolean, positionMs: Long) {}
        fun onProgressUpdate(song: OnlineSong, positionMs: Long) {}
        fun onSeek(song: OnlineSong, positionMs: Long) {}
        fun onLyricsLoaded(song: OnlineSong, lyrics: com.aman.auramusic.online.lyrics.model.SongLyrics) {}
        fun onPlaybackStopped(song: OnlineSong?) {}
    }

    var eventListener: PlaybackEventListener? = null

    private var isUsingIFramePlayer = false

    // 1. AndroidX Media3 ExoPlayer for direct CDN streams (JioSaavn 320k untouched)
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AuraMusicPlayer/3.1.0")
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
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (isUsingIFramePlayer) return
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _playbackState.update { it.copy(isBuffering = true) }
                        }
                        Player.STATE_READY -> {
                            _playbackState.update {
                                it.copy(
                                    isBuffering = false,
                                    durationMs = duration.coerceAtLeast(0L),
                                    errorMessage = null
                                )
                            }
                        }
                        Player.STATE_ENDED -> {
                            _playbackState.update { it.copy(isBuffering = false, isPlaying = false) }
                            handleSongEnded()
                        }
                        Player.STATE_IDLE -> {
                            _playbackState.update { it.copy(isBuffering = false) }
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isUsingIFramePlayer) return
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                    _playbackState.value.currentSong?.let { song ->
                        eventListener?.onPlaybackStateChanged(song, isPlaying, exoPlayer.currentPosition.coerceAtLeast(0L))
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (isUsingIFramePlayer) return
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
                _playbackState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        activePlayerType = "RiPlay IFrame"
                    )
                }
                _playbackState.value.currentSong?.let { song ->
                    eventListener?.onPlaybackStateChanged(song, isPlaying, _playbackState.value.currentPositionMs)
                }
            }

            override fun onProgressUpdate(currentMs: Long, durationMs: Long) {
                if (!isUsingIFramePlayer) return
                val lyrics = _playbackState.value.currentLyrics
                val activeLine = lyrics?.findActiveLineIndex(currentMs) ?: -1
                _playbackState.update {
                    it.copy(
                        currentPositionMs = currentMs,
                        durationMs = if (durationMs > 0) durationMs else it.durationMs,
                        activeLyricsLineIndex = activeLine,
                        bufferedPositionMs = (currentMs + 20000).coerceAtMost(durationMs)
                    )
                }
                _playbackState.value.currentSong?.let { song ->
                    eventListener?.onProgressUpdate(song, currentMs)
                }
            }

            override fun onSongEnded() {
                if (!isUsingIFramePlayer) return
                handleSongEnded()
            }

            override fun onError(message: String) {
                if (!isUsingIFramePlayer) return
                _playbackState.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = message
                    )
                }
            }
        }
    )

    fun setStreamingEngine(engine: StreamingEngine) {
        _playbackState.update { it.copy(selectedEngine = engine) }
    }

    fun playSong(song: OnlineSong, newQueue: List<OnlineSong> = emptyList()) {
        scope.launch {
            _playbackState.update {
                it.copy(
                    isBuffering = true,
                    errorMessage = null,
                    currentSong = song,
                    currentLyrics = null,
                    activeLyricsLineIndex = -1,
                    isLoadingLyrics = true,
                    queue = if (newQueue.isNotEmpty()) newQueue else if (!it.queue.contains(song)) it.queue + song else it.queue
                )
            }

            eventListener?.onSongChanged(song)

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
                    eventListener?.onLyricsLoaded(song, lyrics)
                }
            }

            val currentQueue = _playbackState.value.queue
            val index = currentQueue.indexOfFirst { it.id == song.id }
            _playbackState.update { it.copy(queueIndex = index) }

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

                        youTubeIFramePlayer.loadAndPlay(song.id, forceWebController = false)
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

                        youTubeIFramePlayer.loadAndPlay(song.id, forceWebController = true)
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

                            youTubeIFramePlayer.loadAndPlay(song.id)
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
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
        if (isUsingIFramePlayer) {
            youTubeIFramePlayer.pause()
        } else {
            exoPlayer.pause()
        }
        _playbackState.value.currentSong?.let { song ->
            eventListener?.onPlaybackStateChanged(song, false, _playbackState.value.currentPositionMs)
        }
    }

    fun seekTo(positionMs: Long) {
        if (isUsingIFramePlayer) {
            youTubeIFramePlayer.seekTo(positionMs)
            _playbackState.update { it.copy(currentPositionMs = positionMs) }
        } else {
            exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
            _playbackState.update { it.copy(currentPositionMs = positionMs) }
        }
        _playbackState.value.currentSong?.let { song ->
            eventListener?.onSeek(song, positionMs)
        }
    }

    fun seekToFraction(fraction: Float) {
        val duration = _playbackState.value.durationMs
        val targetMs = (duration * fraction).toLong()
        seekTo(targetMs)
    }

    fun playNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

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

        val queue = state.queue
        if (queue.isEmpty()) return

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else queue.size - 1
        playSong(queue[prevIndex])
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeat() {
        _playbackState.update { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
        exoPlayer.repeatMode = if (_playbackState.value.isRepeatEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
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
        _playbackState.update {
            if (!it.queue.any { q -> q.id == song.id }) {
                it.copy(queue = it.queue + song)
            } else it
        }
    }

    fun removeQueueItem(songId: String) {
        _playbackState.update { state ->
            val newQueue = state.queue.filterNot { it.id == songId }
            val newIndex = newQueue.indexOfFirst { it.id == state.currentSong?.id }
            state.copy(queue = newQueue, queueIndex = newIndex)
        }
    }

    fun clearQueueExceptCurrent() {
        _playbackState.update { state ->
            val current = state.currentSong
            if (current != null) {
                state.copy(queue = listOf(current), queueIndex = 0)
            } else {
                state.copy(queue = emptyList(), queueIndex = -1)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _playbackState.update { state ->
            val q = state.queue.toMutableList()
            if (fromIndex !in q.indices || toIndex !in q.indices) return@update state
            val item = q.removeAt(fromIndex)
            q.add(toIndex, item)
            val newIndex = q.indexOfFirst { it.id == state.currentSong?.id }
            state.copy(queue = q, queueIndex = newIndex)
        }
    }

    private fun handleSongEnded() {
        if (_playbackState.value.isRepeatEnabled) {
            seekTo(0)
            if (isUsingIFramePlayer) youTubeIFramePlayer.play() else exoPlayer.play()
        } else {
            playNext()
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

                    _playbackState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
                            activeLyricsLineIndex = activeLine
                        )
                    }
                    _playbackState.value.currentSong?.let { song ->
                        eventListener?.onProgressUpdate(song, currentPos)
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
        eventListener?.onPlaybackStopped(_playbackState.value.currentSong)
        stopProgressTracker()
        exoPlayer.release()
        youTubeIFramePlayer.release()
    }
}
