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
import androidx.palette.graphics.Palette
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.data.repository.UserPreferencesRepository
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
import android.content.ServiceConnection
import android.os.IBinder
import com.aman.auramusic.playback.PlaybackService
import com.aman.auramusic.playback.RepeatMode
import androidx.core.content.edit

import java.util.UUID

data class QueueEntry(
    val id: String = UUID.randomUUID().toString(),
    val song: Song
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var playerManager: VlcPlayerManager? = null
    private var notificationManager: PlaybackNotificationManager? = null
    private var playbackService: PlaybackService? = null
    private val lyricsRepository = LyricsRepository()
    private val userRepository = UserPreferencesRepository(application)
    private val musicRepository = com.aman.auramusic.data.repository.MusicRepository(application)
    private var favoriteJob: Job? = null
    private var isServiceBound = false

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
            duration = pm.duration()
            
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

    private fun refreshQueueFromIds(ids: List<Long>) {
        val allSongs = musicRepository.getAllSongs()
        val songs = ids.mapNotNull { id -> allSongs.find { it.id == id } }
        
        // Update originalQueue but try to keep current _queue if shuffled
        val newOriginal = songs.map { QueueEntry(song = it) }
        originalQueue = newOriginal
        
        if (isShuffled) {
            // Keep current song if it's still in the new list
            val current = _queue.value.find { it.song.id == currentSongId }
            if (current != null && songs.any { it.id == currentSongId }) {
                val remaining = songs.filter { it.id != currentSongId }.shuffled()
                _queue.value = listOf(current) + remaining.map { s -> QueueEntry(song = s) }
            } else {
                _queue.value = originalQueue.shuffled()
            }
        } else {
            _queue.value = originalQueue
        }
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
                
                if (duration > 0) {
                    val remaining = duration - position
                    if (appSettings.skipSilence && remaining in 1..800L) {
                        playNext()
                    }
                    if (appSettings.crossfadeEnabled && remaining in 1..3000L) {
                        playNext()
                    }
                }

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
                    if (playing) {
                        playbackService?.startAsForeground(song, true)
                    } else {
                        playbackService?.stopAsForeground(false)
                    }
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
        playbackService?.let { service ->
            service.currentPlaylistId = playlistId
            val uniqueSongs = songs.distinctBy { it.id }
            service.queue = uniqueSongs
            
            // If shuffled was already on, we should shuffle the new queue
            if (service.isShuffled) {
                service.queue = uniqueSongs.shuffled()
            }
        }
        
        // Also update local for immediate feedback if not bound yet (though it should be)
        if (playbackService == null) {
            currentPlaylistId = playlistId
            val uniqueSongs = songs.distinctBy { it.id }
            originalQueue = uniqueSongs.map { QueueEntry(song = it) }
            _queue.value = originalQueue
        }
    }

    fun play(song: Song, startPosition: Long = 0L) {
        val pm = playerManager ?: return
        val service = playbackService ?: return
        
        service.play(song)
        
        currentPosition = startPosition
        duration = song.duration
        currentSong = song
        currentSongId = song.id
        
        val finalStartPosition = if (startPosition == 0L && appSettings.skipSilence) 500L else startPosition
        if (finalStartPosition > 0) {
            pm.seekTo(finalStartPosition)
        }
        
        loadLyrics(song)
        extractColor(song)
        checkIsFavorite(song)
        recordPlayback(song)
        saveLastSong(song, startPosition)
        
        viewModelScope.launch(Dispatchers.IO) {
            val artwork = runCatching {
                getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(song.artworkUri))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
            pm.setMetadata(song.title, song.artist, song.album, song.duration, artwork)
            notificationManager?.show(song, true, pm.getSessionToken())
        }
    }

    private fun recordPlayback(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordPlayback(song.id, System.currentTimeMillis())
        }
    }

    private fun saveLastSong(song: Song, position: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = getApplication<Application>().getSharedPreferences("playback_state", android.content.Context.MODE_PRIVATE)
            prefs.edit(commit = true) {
                putLong("last_song_id", song.id)
                putLong("last_position", position)
            }
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
            val prefs = getApplication<Application>().getSharedPreferences("playback_state", android.content.Context.MODE_PRIVATE)
            val lastId = prefs.getLong("last_song_id", -1L)
            val lastPos = prefs.getLong("last_position", 0L)

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

    fun seekTo(position: Long) {
        playerManager?.seekTo(position)
    }

    fun toggleShuffle() {
        playbackService?.let { service ->
            val newShuffled = !service.isShuffled
            service.isShuffled = newShuffled
            if (newShuffled) {
                val current = service.queue.find { it.id == currentSongId }
                val remaining = service.queue.filter { it.id != currentSongId }.shuffled()
                service.queue = if (current != null) listOf(current) + remaining else service.queue.shuffled()
            } else {
                // To restore original order, we'd need to keep it. 
                // For now, just keep as is or we can improve this.
            }
        }
        isShuffled = playbackService?.isShuffled ?: !isShuffled
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
        if (!isShuffled) {
            originalQueue = q
        }
    }

    fun removeQueueItem(songId: Long) {
        val q = _queue.value.filterNot { it.song.id == songId }
        _queue.value = q
        playbackService?.queue = q.map { it.song }
        if (!isShuffled) {
            originalQueue = q
        }
        if (currentSongId == songId) {
            playNext()
        }
    }

    fun clearQueueExceptCurrent() {
        val current = currentSong ?: return
        val q = listOf(QueueEntry(song = current))
        _queue.value = emptyList() // The filtered queue will be empty
        playbackService?.queue = listOf(current)
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

    private fun extractColor(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val uri = android.net.Uri.parse(song.artworkUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        dominantColor = palette.getVibrantColor(
                            palette.getDominantColor(0xFF1E1E1E.toInt())
                        )
                        accentColor = palette.getMutedColor(
                            palette.getDarkVibrantColor(0xFF1E1E1E.toInt())
                        )
                    }
                }
            } catch (e: Exception) {
                dominantColor = 0xFF1E1E1E.toInt()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }
        
        if (!appSettings.keepPlayingOnClose || !isPlaying) {
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
