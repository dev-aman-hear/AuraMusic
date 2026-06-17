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
import kotlinx.coroutines.launch
import androidx.core.content.edit

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = VlcPlayerManager(application)
    private val lyricsRepository = LyricsRepository()
    private val userRepository = UserPreferencesRepository(application)
    private val notificationManager = PlaybackNotificationManager(application)
    private var favoriteJob: Job? = null

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

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var originalQueue: List<Song> = emptyList()

    init {
        PlaybackActionRegistry.onPlayPause = { togglePlayPause() }
        PlaybackActionRegistry.onNext = { playNext() }
        PlaybackActionRegistry.onPrevious = { playPrevious() }
        PlaybackActionRegistry.onFavorite = { toggleFavorite() }
        playerManager.setListeners(
            onProgress = { pos, dur ->
                currentPosition = pos
                duration = dur
                if (pos > 0 && pos % 5000 < 1000) { // Save every ~5 seconds
                    currentSong?.let { saveLastSong(it, pos) }
                }
            },
            onPlaybackState = { playing ->
                isPlaying = playing
                currentSong?.let { song ->
                    notificationManager.show(song, playing, playerManager.getSessionToken())
                }
            },
            onEnd = {
                handlePlaybackEnd()
            }
        )
    }

    fun setQueue(songs: List<Song>) {
        originalQueue = songs
        _queue.value = songs
    }

    fun play(song: Song, startPosition: Long = 0L) {

        currentPosition = startPosition
        duration = song.duration

        currentSong = song
        currentSongId = song.id

        playerManager.play(song.filePath)

        if (startPosition > 0) {
            playerManager.seekTo(startPosition)
        }
        loadLyrics(song)
        extractColor(song)
        checkIsFavorite(song)
        recordPlayback(song)
        saveLastSong(song, startPosition)
        
        // Update MediaSession Metadata
        viewModelScope.launch(Dispatchers.IO) {
            val artwork = runCatching {
                getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(song.artworkUri))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
            playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork)
            notificationManager.show(song, true, playerManager.getSessionToken())
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
            prefs.edit {
                putLong("last_song_id", song.id)
                    .putLong("last_position", position)
            }
        }
    }

    fun restoreLastState(songs: List<Song>) {
        if (currentSong != null || songs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val prefs = getApplication<Application>().getSharedPreferences("playback_state", android.content.Context.MODE_PRIVATE)
            val lastId = prefs.getLong("last_song_id", -1L)
            val lastPos = prefs.getLong("last_position", 0L)

            if (lastId != -1L) {
                val song = songs.find { it.id == lastId }
                if (song != null) {
                    viewModelScope.launch(Dispatchers.Main) {
                        // Prepare the player with the last song but don't autoplay immediately
                        currentSong = song
                        currentSongId = song.id
                        currentPosition = lastPos
                        duration = song.duration
                        playerManager.prepare(song.filePath, lastPos)
                        
                        // Update MediaSession for restored song
                        viewModelScope.launch(Dispatchers.IO) {
                            val artwork = runCatching {
                                getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(song.artworkUri))?.use { input ->
                                    BitmapFactory.decodeStream(input)
                                }
                            }.getOrNull()
                            playerManager.setMetadata(song.title, song.artist, song.album, song.duration, artwork)
                            notificationManager.show(song, false, playerManager.getSessionToken())
                        }
                        
                        extractColor(song)
                        checkIsFavorite(song)
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
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
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSongId }
        val nextIndex = (currentIndex + 1) % currentQueue.size
        
        if (nextIndex == 0 && repeatMode == RepeatMode.NONE && currentIndex != -1) {
             // Stop at end of queue if no repeat
             return
        }
        
        play(currentQueue[nextIndex])
    }

    fun playPrevious() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        val currentIndex = currentQueue.indexOfFirst { it.id == currentSongId }
        val prevIndex = if (currentIndex <= 0) currentQueue.size - 1 else currentIndex - 1
        play(currentQueue[prevIndex])
    }

    fun  moveQueueItem(fromIndex: Int, toIndex: Int) {
        val queue = _queue.value.toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        _queue.value = queue
        if (!isShuffled) {
            originalQueue = queue
        }
    }

    fun removeQueueItem(songId: Long) {
        val queue = _queue.value.filterNot { it.id == songId }
        _queue.value = queue
        if (!isShuffled) {
            originalQueue = queue
        }
        if (currentSongId == songId) {
            playNext()
        }
    }

    fun clearQueueExceptCurrent() {
        val current = currentSong ?: return
        val queue = listOf(current)
        _queue.value = queue
        originalQueue = queue
    }

    fun saveQueueAsPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.savePlaylist(name, _queue.value.map { it.id })
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            sleepTimerRemaining = 0L
            return
        }

        if (minutes == -1) {
            // End of song mode
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
                        dominantColor = palette.getDominantColor(0xFF1E1E1E.toInt())
                    }
                }
            } catch (e: Exception) {
                dominantColor = 0xFF1E1E1E.toInt()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        PlaybackActionRegistry.onPlayPause = null
        PlaybackActionRegistry.onNext = null
        PlaybackActionRegistry.onPrevious = null
        PlaybackActionRegistry.onFavorite = null
        notificationManager.clear()
        playerManager.release()
    }

    enum class RepeatMode {
        NONE, ONE, ALL
    }
}
