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
import com.aman.auramusic.playback.RepeatMode
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

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private var originalQueue: List<QueueEntry> = emptyList()

    init {
        val intent = Intent(application, PlaybackService::class.java)
        application.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        application.startService(intent)

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
                            refreshQueueFromIds(playlist.songIds)
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshQueueFromIds(ids: List<Long>) {
        val allSongs = kotlinx.coroutines.withContext(Dispatchers.IO) {
            musicRepository.getAllSongs()
        }
        val songs = ids.mapNotNull { id -> allSongs.find { it.id == id } }.distinctBy { it.id }
        originalQueue = songs.map { QueueEntry(song = it) }

        val targetSongs = if (isShuffled) {
            val current = songs.firstOrNull { it.id == currentSongId }
            if (current != null) listOf(current) + songs.filterNot { it.id == current.id }.shuffled()
            else songs.shuffled()
        } else songs

        playbackService?.queue = targetSongs
        _queue.value = targetSongs.map { QueueEntry(song = it) }
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

                currentSong?.let { song ->
                    notificationManager?.show(song, playing, pm.getSessionToken())
                }
            }

            override fun onEnd() {
                // Service handles playback end now
            }
        }
        
        pm.addListener(playerListener!!)
    }

    var currentPlaylistId by mutableStateOf<Long?>(null)
        private set

    fun setQueue(songs: List<Song>, playlistId: Long? = null) {
        val uniqueSongs = songs.distinctBy { it.id }
        currentPlaylistId = playlistId
        originalQueue = uniqueSongs.map { QueueEntry(song = it) }

        val service = playbackService
        if (service != null) service.currentPlaylistId = playlistId
        val targetSongs = if (service?.isShuffled == true || (service == null && isShuffled)) {
            val current = uniqueSongs.firstOrNull { it.id == currentSongId }
            if (current != null) listOf(current) + uniqueSongs.filterNot { it.id == current.id }.shuffled()
            else uniqueSongs.shuffled()
        } else uniqueSongs

        service?.queue = targetSongs
        _queue.value = targetSongs.map { QueueEntry(song = it) }
    }

    fun addToQueue(song: Song) = addToQueue(listOf(song))

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val currentList = playbackService?.queue ?: _queue.value.map { it.song }
        val newSongs = songs.distinctBy { it.id }
            .filter { candidate -> currentList.none { it.id == candidate.id } }
        if (newSongs.isEmpty()) return

        val canonical = originalQueue.map { it.song }
        val canonicalIds = canonical.mapTo(mutableSetOf()) { it.id }
        val canonicalBase = if (canonical.isEmpty()) currentList
            else canonical + currentList.filterNot { it.id in canonicalIds }
        originalQueue = (canonicalBase + newSongs)
            .distinctBy { it.id }
            .map { QueueEntry(song = it) }

        val updated = currentList + newSongs
        playbackService?.queue = updated
        _queue.value = updated.map { QueueEntry(song = it) }
    }

    fun play(song: Song, startPosition: Long = 0L) {
        val pm = playerManager ?: return
        val service = playbackService ?: return
        
        isOnlinePlaying = false
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
        val service = playbackService
        val newShuffled = !(service?.isShuffled ?: isShuffled)
        val activeQueue = service?.queue ?: _queue.value.map { it.song }
        val canonical = originalQueue.map { it.song }.ifEmpty { activeQueue }
        if (originalQueue.isEmpty()) {
            originalQueue = canonical.map { QueueEntry(song = it) }
        }

        val targetQueue = if (newShuffled) {
            val current = canonical.firstOrNull { it.id == currentSongId }
            if (current != null) listOf(current) + canonical.filterNot { it.id == current.id }.shuffled()
            else canonical.shuffled()
        } else {
            canonical.distinctBy { it.id }
        }

        // Update the flag before publishing the reordered queue so the queueFlow
        // collector knows whether this is canonical order or shuffle presentation order.
        service?.isShuffled = newShuffled
        service?.queue = targetQueue
        _queue.value = targetQueue.map { QueueEntry(song = it) }
        isShuffled = newShuffled
    }

    fun toggleRepeat() {
        playbackService?.let { service ->
            val nextMode = when (service.repeatMode) {
                RepeatMode.NONE -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.NONE
            }
            service.repeatMode = nextMode
            repeatMode = nextMode
        }
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
        playbackService?.playNext()
    }

    fun playPrevious() {
        playbackService?.playPrevious()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queue.value.toMutableList()
        if (fromIndex !in q.indices || toIndex !in q.indices) return
        val item = q.removeAt(fromIndex)
        q.add(toIndex, item)
        _queue.value = q
        playbackService?.queue = q.map { it.song }
        if (!isShuffled) originalQueue = q
    }

    fun removeQueueItem(songId: Long) {
        val wasCurrent = currentSongId == songId
        val q = _queue.value.filterNot { it.song.id == songId }
        _queue.value = q
        playbackService?.queue = q.map { it.song }

        val canonical = originalQueue.map { it.song }.ifEmpty { q.map { it.song } }
        originalQueue = canonical.filterNot { it.id == songId }.map { QueueEntry(song = it) }

        if (wasCurrent) {
            if (q.isNotEmpty()) {
                playNext()
            } else {
                currentSong = null
                currentSongId = null
                playerManager?.pause()
            }
        }
    }

    fun clearQueueExceptCurrent() {
        val current = currentSong ?: return
        val q = listOf(QueueEntry(song = current))
        _queue.value = q
        playbackService?.queue = listOf(current)
        playbackService?.currentPlaylistId = null
        currentPlaylistId = null
        originalQueue = q
    }

    fun saveQueueAsPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.savePlaylist(name, _queue.value.map { it.song.id })
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            sleepTimerRemaining = 0L
            return
        }
        if (minutes == -1) {
            sleepTimerRemaining = -1L
            return
        }
        sleepTimerRemaining = minutes * 60 * 1000L
        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                sleepTimerRemaining -= 1000
            }
            if (isPlaying) {
                togglePlayPause()
            }
            sleepTimerRemaining = 0L
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
        if (isServiceBound) {
            runCatching { getApplication<Application>().unbindService(serviceConnection) }
            isServiceBound = false
        }
        playbackService = null
        playerManager = null
        notificationManager = null
        
        val isOnlineActive = playbackService?.isOnlinePlaybackActive == true
        val isAnyPlaying = isPlaying || (isOnlineActive && isOnlinePlaying)
        if (!appSettings.keepPlayingOnClose || !isAnyPlaying) {
            val intent = Intent(getApplication(), PlaybackService::class.java)
            getApplication<Application>().stopService(intent)
            
            // Only clear if the service is actually stopping
            PlaybackActionRegistry.onPlayPause = null
            PlaybackActionRegistry.onNext = null
            PlaybackActionRegistry.onPrevious = null
            PlaybackActionRegistry.onFavorite = null
        }
    }
}
