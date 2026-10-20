package com.aman.auramusic.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.data.repository.MusicRepository
import com.aman.auramusic.data.repository.UserPreferencesRepository
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.util.LastPlayedStore
import com.aman.auramusic.playback.PlaybackActionRegistry
import com.aman.auramusic.playback.PlaybackNotificationManager
import com.aman.auramusic.playback.VlcPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.ComponentName
import android.content.Intent
import android.annotation.SuppressLint
import android.content.ServiceConnection
import android.os.IBinder
import com.aman.auramusic.playback.PlaybackService
import com.aman.auramusic.playback.PlaybackQueueCoordinator
import com.aman.auramusic.playback.QueueActionResult
import com.aman.auramusic.playback.RepeatMode
import com.aman.auramusic.data.model.Playlist
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import java.util.UUID

data class QueueEntry(
    val id: String = UUID.randomUUID().toString(),
    val song: Song
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val lyricsRepository: LyricsRepository,
    private val userRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {

    private var playerManager: VlcPlayerManager? = null
    private var notificationManager: PlaybackNotificationManager? = null
    @SuppressLint("StaticFieldLeak")
    private var playbackService: PlaybackService? = null
    private var favoriteJob: Job? = null
    var isServiceBound by mutableStateOf(false)
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            val serviceInstance = binder.getService()
            playbackService = serviceInstance
            playerManager = serviceInstance.getPlayerManager()
            notificationManager = serviceInstance.getNotificationManager()
            isServiceBound = true
            syncWithService()
            setupListeners()

            viewModelScope.launch {
                serviceInstance.queueFlow.collect { songs ->
                    // Preserve a canonical unshuffled order for the shuffle toggle.
                    // When shuffled, queueFlow emits presentation order and must not
                    // overwrite the canonical source list.
                    if ((!serviceInstance.isShuffled && songs.isNotEmpty()) || originalQueue.isEmpty()) {
                        originalQueue = songs.map { QueueEntry(song = it) }
                    }
                    _queue.value = songs.map { QueueEntry(song = it) }
                }
            }

            viewModelScope.launch {
                serviceInstance.repeatModeFlow.collect { mode ->
                    repeatMode = mode
                }
            }

            viewModelScope.launch {
                serviceInstance.isShuffledFlow.collect { shuffled ->
                    isShuffled = shuffled
                }
            }
            
            viewModelScope.launch {
                userRepository.settingsFlow.collect { settings ->
                    appSettings = settings
                    playerManager?.smartAudioFocusEnabled = settings.smartAudioFocus
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            playerManager = null
            notificationManager = null
            playbackService = null
        }
    }

    private fun syncWithService() {
        val service = playbackService ?: return
        val pm = playerManager ?: return
        
        val song = service.currentSong
        if (song != null) {
            currentSong = song
            currentSongId = song.id
            isPlaying = pm.isPlaying()
            currentPosition = pm.position()
            duration = if (pm.duration() > 0L) pm.duration() else song.duration
            
            loadLyrics(song)
            extractColor(song)
            checkIsFavorite(song)
        }
    }

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var currentSongId by mutableStateOf<Long?>(null)
        private set

    var currentPosition by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isOnlinePlaying by mutableStateOf(false)
        private set

    var dominantColor by mutableIntStateOf(0xFF1E1E1E.toInt())
        private set

    var accentColor by mutableIntStateOf(0xFF1E1E1E.toInt())
        private set

    var appSettings by mutableStateOf(com.aman.auramusic.data.model.AppSettings())
        private set

    var isShuffled by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(RepeatMode.NONE)
        private set

    var isFavorite by mutableStateOf(false)
        private set

    var sleepTimerRemaining by mutableLongStateOf(0L)
        private set

    private var sleepTimerJob: Job? = null

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private var originalQueue: List<QueueEntry> = emptyList()
    private val manuallyAddedSongIds = mutableSetOf<Long>()

    init {
        val intent = Intent(application, PlaybackService::class.java)
        application.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        application.startService(intent)

        viewModelScope.launch {
            PlaybackQueueCoordinator.queueFlow.collect { tracks ->
                _queue.value = tracks.map { QueueEntry(id = it.queueId, song = it.song) }
            }
        }

        viewModelScope.launch {
            PlaybackQueueCoordinator.isShuffledFlow.collect { shuffled ->
                isShuffled = shuffled
            }
        }

        viewModelScope.launch {
            PlaybackQueueCoordinator.repeatModeFlow.collect { mode ->
                repeatMode = mode
            }
        }

        // Observe favorites and playlists to keep queue synced
        viewModelScope.launch {
            userRepository.favoriteIdsFlow.collect { ids ->
                if (currentPlaylistId == -1L) {
                    refreshQueueFromIds(ids.toList())
                }
            }
        }

        viewModelScope.launch {
            userRepository.playlistsFlow.collect { playlists ->
                currentPlaylistId?.let { pid ->
                    if (pid != -1L) {
                        playlists.find { it.id == pid }?.let { playlist ->
                            refreshQueueFromPlaylist(playlist)
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshQueueFromPlaylist(playlist: Playlist) {
        if (currentPlaylistId == null) return
        val allSongs = kotlinx.coroutines.withContext(Dispatchers.IO) {
            musicRepository.getAllSongs()
        }
        val localSongs = playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
        PlaybackQueueCoordinator.refreshFromPlaylist(
            playlistId = playlist.id,
            localSongs = localSongs,
            onlineSongs = playlist.onlineSongs
        )
    }

    private suspend fun refreshQueueFromIds(ids: List<Long>) {
        if (currentPlaylistId == null) return
        val allSongs = kotlinx.coroutines.withContext(Dispatchers.IO) {
            musicRepository.getAllSongs()
        }
        val localSongs = ids.mapNotNull { id -> allSongs.find { it.id == id } }
        PlaybackQueueCoordinator.refreshFromPlaylist(
            playlistId = -1L,
            localSongs = localSongs,
            onlineSongs = emptyList()
        )
    }

    private var playerListener: VlcPlayerManager.PlayerListener? = null
    private fun setupListeners() {
        val pm = playerManager ?: return
        // Let the service handle these by default, they are set up in PlaybackService.setupActions()
        // We only set onFavorite here as it involves userRepository logic usually in ViewModel
        PlaybackActionRegistry.onFavorite = { toggleFavorite() }
        
        // Remove old listener if it exists to avoid double triggers
        playerListener?.let { pm.removeListener(it) }
        
        playerListener = object : VlcPlayerManager.PlayerListener {
            override fun onProgress(position: Long, duration: Long) {
                currentPosition = position
                this@PlayerViewModel.duration = duration
                
                // Crossfade and silence skipping need actual audio processing. Never
                // approximate them by advancing the queue near the end of a track.
                if (position > 0 && position % 5000 < 1000) {
                    currentSong?.let { saveLastSong(it, position) }
                }
            }

            override fun onPlaybackState(playing: Boolean) {
                isPlaying = playing
                
                // Update local state if service changed song (e.g. naturally ended)
                val serviceSong = playbackService?.currentSong
                if (serviceSong != null && serviceSong.id != currentSongId) {
                    syncWithService()
                }
            }

            override fun onEnd() {
                // Service handles playback end now
            }
        }
        
        pm.addListener(playerListener!!)
    }

    private val _currentPlaylistId = mutableStateOf<Long?>(null)
    var currentPlaylistId: Long?
        get() = _currentPlaylistId.value
        set(value) {
            _currentPlaylistId.value = value
            playbackService?.currentPlaylistId = value
        }

    fun setQueue(songs: List<Song>, playlistId: Long? = null) {
        currentPlaylistId = playlistId
        val service = playbackService
        if (service != null) service.currentPlaylistId = playlistId
        PlaybackQueueCoordinator.setQueueSongs(songs, currentSong, playlistId)
    }

    fun setQueueOnline(onlineSongs: List<OnlineSong>, playlistId: Long? = null) {
        currentPlaylistId = playlistId
        PlaybackQueueCoordinator.setQueueOnline(onlineSongs, null, playlistId)
    }

    fun addToQueue(song: Song) = addToQueue(listOf(song))

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val res = PlaybackQueueCoordinator.addToQueue(songs.map { PlaybackQueueCoordinator.createTrack(it) })
        when (res) {
            is QueueActionResult.Added -> _userFeedback.tryEmit(res.message)
            is QueueActionResult.AlreadyQueued -> _userFeedback.tryEmit(res.message)
        }
    }

    fun addToQueue(onlineSong: OnlineSong) = addToQueueOnline(listOf(onlineSong))

    fun addToQueueOnline(onlineSongs: List<OnlineSong>) {
        if (onlineSongs.isEmpty()) return
        val res = PlaybackQueueCoordinator.addToQueue(onlineSongs.map { PlaybackQueueCoordinator.createTrack(it) })
        when (res) {
            is QueueActionResult.Added -> _userFeedback.tryEmit(res.message)
            is QueueActionResult.AlreadyQueued -> _userFeedback.tryEmit(res.message)
        }
    }

    fun playNext(song: Song) = playNext(listOf(song))

    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val res = PlaybackQueueCoordinator.playNext(songs.map { PlaybackQueueCoordinator.createTrack(it) })
        when (res) {
            is QueueActionResult.Added -> _userFeedback.tryEmit(res.message)
            is QueueActionResult.AlreadyQueued -> _userFeedback.tryEmit(res.message)
        }
    }

    fun playNext(onlineSong: OnlineSong) = playNextOnline(listOf(onlineSong))

    fun playNextOnline(onlineSongs: List<OnlineSong>) {
        if (onlineSongs.isEmpty()) return
        val res = PlaybackQueueCoordinator.playNext(onlineSongs.map { PlaybackQueueCoordinator.createTrack(it) })
        when (res) {
            is QueueActionResult.Added -> _userFeedback.tryEmit(res.message)
            is QueueActionResult.AlreadyQueued -> _userFeedback.tryEmit(res.message)
        }
    }

    fun play(song: Song, startPosition: Long = 0L) {
        if (song.isOnline) {
            PlaybackQueueCoordinator.playTrackBySong(song)
            return
        }
        val pm = playerManager ?: return
        val service = playbackService ?: return
        
        isOnlinePlaying = false
        PlaybackQueueCoordinator.playTrackBySong(song)
        service.play(song)
        
        currentPosition = startPosition
        duration = song.duration
        currentSong = song
        currentSongId = song.id
        
        // Do not skip an arbitrary 500 ms as a substitute for silence detection.
        if (startPosition > 0) {
            pm.seekTo(startPosition)
        }
        
        loadLyrics(song)
        extractColor(song)
        checkIsFavorite(song)
        recordPlayback(song)
        saveLastSong(song, startPosition)
    }

    fun startOnlinePlayback(
        song: OnlineSong,
        isPlaying: Boolean,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onSeekTo: ((Long) -> Unit)? = null
    ) {
        currentSong = null
        currentSongId = null
        isOnlinePlaying = isPlaying
        if (!isServiceBound) {
            val intent = Intent(getApplication(), PlaybackService::class.java)
            getApplication<Application>().startService(intent)
            viewModelScope.launch {
                var attempts = 0
                while (!isServiceBound && attempts < 10) {
                    delay(100)
                    attempts++
                }
                playbackService?.startOnlinePlayback(song, isPlaying, onPlayPause, onNext, onPrevious, onSeekTo)
            }
        } else {
            playbackService?.startOnlinePlayback(song, isPlaying, onPlayPause, onNext, onPrevious, onSeekTo)
        }
    }

    fun restoreOnlineState(
        song: OnlineSong,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onSeekTo: ((Long) -> Unit)? = null
    ) {
        currentSong = null
        currentSongId = null
        isOnlinePlaying = false
        if (!isServiceBound) {
            viewModelScope.launch {
                var attempts = 0
                while (!isServiceBound && attempts < 10) {
                    delay(100)
                    attempts++
                }
                if (isServiceBound) {
                    playbackService?.startOnlinePlayback(song, false, onPlayPause, onNext, onPrevious, onSeekTo)
                }
            }
        } else {
            playbackService?.startOnlinePlayback(song, false, onPlayPause, onNext, onPrevious, onSeekTo)
        }
    }

    fun updateOnlinePlaybackState(
        song: OnlineSong,
        isPlaying: Boolean,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        isOnlinePlaying = isPlaying
        playbackService?.updateOnlinePlaybackState(song, isPlaying, positionMs, durationMs)
    }

    fun updateOnlineLyrics(song: OnlineSong, lyrics: List<LyricLine>) {
        playbackService?.updateOnlineLyrics(song, lyrics)
    }

    fun onOnlineProgressUpdate(song: OnlineSong, positionMs: Long, durationMs: Long = 0L) {
        playbackService?.updateOnlinePosition(positionMs, durationMs)
    }

    fun onOnlineSeek(song: OnlineSong, positionMs: Long) {
        playbackService?.onOnlineSeek(positionMs)
    }

    fun stopOnlinePlayback() {
        isOnlinePlaying = false
        playbackService?.stopOnlinePlayback()
    }

    private fun recordPlayback(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordPlayback(song.id, System.currentTimeMillis())
        }
    }

    private fun saveLastSong(song: Song, position: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            LastPlayedStore.saveOffline(getApplication(), song, position)
        }
    }

    fun restoreLastState(songs: List<Song>) {
        if (currentSong != null || songs.isEmpty()) return
        
        // Wait for service to be bound before deciding to restore
        if (!isServiceBound) {
            viewModelScope.launch {
                var attempts = 0
                while (!isServiceBound && attempts < 10) {
                    delay(100)
                    attempts++
                }
                if (isServiceBound && currentSong == null) {
                    performRestore(songs)
                }
            }
        } else {
            performRestore(songs)
        }
    }

    private fun performRestore(songs: List<Song>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val lastSource = LastPlayedStore.getLastSourceType(context)
            if (lastSource == LastPlayedStore.SOURCE_ONLINE) {
                // Online song was played last, do not restore offline song!
                return@launch
            }

            val lastId = LastPlayedStore.getLastOfflineSongId(context)
            val lastPos = LastPlayedStore.getLastPosition(context)

            if (lastId != -1L) {
                val song = songs.find { it.id == lastId }
                if (song != null) {
                    viewModelScope.launch(Dispatchers.Main) {
                        val pm = playerManager ?: return@launch
                        if (currentSong != null) return@launch // Double check
                        
                        currentSong = song
                        currentSongId = song.id
                        currentPosition = lastPos
                        duration = song.duration
                        playbackService?.prepare(song, lastPos)
                        
                        viewModelScope.launch(Dispatchers.IO) {
                            val artwork = runCatching {
                                getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(song.artworkUri))?.use { input ->
                                    BitmapFactory.decodeStream(input)
                                }
                            }.getOrNull()
                            pm.setMetadata(song.title, song.artist, song.album, song.duration, artwork)
                            notificationManager?.show(song, false, pm.getSessionToken())
                        }
                        
                        extractColor(song)
                        checkIsFavorite(song)
                        loadLyrics(song)
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        playerManager?.togglePlayPause()
    }

    fun pause() {
        if (isPlaying) {
            currentSong?.let { saveLastSong(it, currentPosition) }
            playerManager?.togglePlayPause()
        }
    }

    fun seekTo(position: Long) {
        currentPosition = position
        currentSong?.let { saveLastSong(it, position) }
        playerManager?.seekTo(position)
    }

    fun toggleShuffle() {
        val newShuffled = PlaybackQueueCoordinator.toggleShuffle()
        isShuffled = newShuffled
        playbackService?.isShuffled = newShuffled
    }

    fun toggleRepeat() {
        val nextMode = PlaybackQueueCoordinator.toggleRepeat()
        repeatMode = nextMode
        playbackService?.repeatMode = nextMode
    }

    fun toggleFavorite() {
        val song = currentSong ?: return
        isFavorite = !isFavorite
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setFavorite(song.id, isFavorite)
        }
    }

    private fun checkIsFavorite(song: Song) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            userRepository.favoriteIdsFlow
                .map { it.contains(song.id) }
                .distinctUntilChanged()
                .collect { favorite ->
                    isFavorite = favorite
                }
        }
    }

    fun playNext() {
        PlaybackQueueCoordinator.playNextTrack(isManual = true)
    }

    fun playPrevious() {
        PlaybackQueueCoordinator.playPreviousTrack()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        PlaybackQueueCoordinator.moveTrack(fromIndex, toIndex)
    }

    fun removeQueueItem(songId: Long) {
        val target = _queue.value.find { it.song.id == songId }
        if (target != null) {
            PlaybackQueueCoordinator.removeSong(target.song)
        } else {
            PlaybackQueueCoordinator.removeTrack("local:$songId")
        }
    }

    fun removeQueueTrack(stableKey: String) {
        PlaybackQueueCoordinator.removeTrack(stableKey)
    }

    fun clearQueueExceptCurrent() {
        PlaybackQueueCoordinator.clearQueueExceptCurrent()
    }

    fun saveQueueAsPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.savePlaylist(name, _queue.value.map { it.song.id })
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        if (minutes == 0) {
            sleepTimerRemaining = 0L
            playbackService?.cancelSleepTimer()
            return
        }
        if (minutes == -1) {
            // -1 represents the existing "end of current track" option.
            sleepTimerRemaining = -1L
            playbackService?.cancelSleepTimer()
            return
        }
        if (minutes < 1) {
            sleepTimerRemaining = 0L
            playbackService?.cancelSleepTimer()
            return
        }

        sleepTimerRemaining = minutes * 60_000L
        // The service owns the actual timer so Activity/ViewModel teardown cannot cancel it.
        playbackService?.startSleepTimer(minutes)
        sleepTimerJob = viewModelScope.launch {
            val deadline = android.os.SystemClock.elapsedRealtime() + minutes * 60_000L
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val remaining = (deadline - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                sleepTimerRemaining = remaining
                if (remaining == 0L) break
            }
            sleepTimerJob = null
        }
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            _lyrics.value = lyricsRepository.lyricsFor(song)
        }
    }

    fun extractColor(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val palette = com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper.extractPalette(context, song, isDark = true)
                dominantColor = palette.primaryArgb
                accentColor = palette.accentArgb
            } catch (_: Exception) {
                dominantColor = 0xFF1E1E1E.toInt()
                accentColor = 0xFF2A2A2A.toInt()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerListener?.let { playerManager?.removeListener(it) }
        playerListener = null

        if (isServiceBound) {
            runCatching { getApplication<Application>().unbindService(serviceConnection) }
            isServiceBound = false
        }
        playbackService = null
        playerManager = null
        notificationManager = null

    }
}
