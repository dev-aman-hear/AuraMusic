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
            updateQueue()
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
    }

    private fun setupListeners() {
        val pm = playerManager ?: return
        PlaybackActionRegistry.onPlayPause = { togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext() }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
        PlaybackActionRegistry.onFavorite = { toggleFavorite() }
        
        pm.setListeners(
            onProgress = { pos, dur ->
                currentPosition = pos
                duration = dur
                
                if (dur > 0) {
                    val remaining = dur - pos
                    if (appSettings.skipSilence && remaining in 1..800L) {
                        playNext()
                    }
                    if (appSettings.crossfadeEnabled && remaining in 1..3000L) {
                        playNext()
                    }
                }

                if (pos > 0 && pos % 5000 < 1000) {
                    currentSong?.let { saveLastSong(it, pos) }
                }
            },
            onPlaybackState = { playing ->
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
            },
            onEnd = {
                handlePlaybackEnd()
            }
        )
    }

    fun setQueue(songs: List<Song>) {
        originalQueue = songs.map { QueueEntry(song = it) }
        updateQueue()
    }

    fun play(song: Song, startPosition: Long = 0L) {
        val pm = playerManager ?: return
        playbackService?.currentSong = song
        currentPosition = startPosition
        duration = song.duration

        currentSong = song
        currentSongId = song.id
        
        updateQueue()
        pm.play(song.filePath)

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
            userRepository.recordPlayback(song.id)
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
                        playbackService?.currentSong = song

                        updateQueue()
                        pm.prepare(song.filePath, lastPos)
                        
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
        isShuffled = !isShuffled
        updateQueue()
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.NONE -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.NONE
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

    private fun updateQueue() {
        // Restore simple logic: current song stays in the list
        _queue.value = if (isShuffled) originalQueue.shuffled() else originalQueue
    }

    private fun handlePlaybackEnd() {
        if (sleepTimerRemaining == -1L) {
            if (isPlaying) togglePlayPause()
            sleepTimerRemaining = 0L
            return
        }

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
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        val currentIndex = currentQueue.indexOfFirst { it.song.id == currentSongId }
        val nextIndex = (currentIndex + 1) % currentQueue.size
        
        if (nextIndex == 0 && repeatMode == RepeatMode.NONE && currentIndex != -1) {
             return
        }
        
        play(currentQueue[nextIndex].song)
    }

    fun playPrevious() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        val currentIndex = currentQueue.indexOfFirst { it.song.id == currentSongId }
        val prevIndex = if (currentIndex <= 0) currentQueue.size - 1 else currentIndex - 1
        play(currentQueue[prevIndex].song)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queue.value.toMutableList()
        if (fromIndex !in q.indices || toIndex !in q.indices) return
        val item = q.removeAt(fromIndex)
        q.add(toIndex, item)
        _queue.value = q
        if (!isShuffled) {
            originalQueue = q
        }
    }

    fun removeQueueItem(songId: Long) {
        val q = _queue.value.filterNot { it.song.id == songId }
        _queue.value = q
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
        }
        
        PlaybackActionRegistry.onPlayPause = null
        PlaybackActionRegistry.onNext = null
        PlaybackActionRegistry.onPrevious = null
        PlaybackActionRegistry.onFavorite = null
    }

    enum class RepeatMode {
        NONE, ONE, ALL
    }
}
