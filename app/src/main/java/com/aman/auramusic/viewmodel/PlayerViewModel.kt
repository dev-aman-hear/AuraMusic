package com.aman.auramusic.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.LyricsRepository
import com.aman.auramusic.playback.VlcPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = VlcPlayerManager(application)
    private val lyricsRepository = LyricsRepository()

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

    var dominantColor by mutableStateOf(0xFF1E1E1E.toInt())
        private set

    var isShuffled by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(RepeatMode.NONE)
        private set

    var isFavorite by mutableStateOf(false)
        private set

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var originalQueue: List<Song> = emptyList()

    init {
        playerManager.setListeners(
            onProgress = { pos, dur ->
                currentPosition = pos
                duration = dur
            },
            onPlaybackState = { playing ->
                isPlaying = playing
            },
            onEnd = {
                handlePlaybackEnd()
            }
        )
    }

    fun setQueue(songs: List<Song>) {
        originalQueue = songs
        updateQueue()
    }

    fun play(song: Song) {
        currentSong = song
        currentSongId = song.id
        playerManager.play(song.filePath)
        loadLyrics(song)
        extractColor(song)
        checkIsFavorite(song)
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
            val prefs = getApplication<Application>().getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean(song.id.toString(), isFavorite).apply()
        }
    }

    private fun checkIsFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = getApplication<Application>().getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE)
            isFavorite = prefs.getBoolean(song.id.toString(), false)
        }
    }

    private fun updateQueue() {
        _queue.value = if (isShuffled) {
            originalQueue.shuffled()
        } else {
            originalQueue
        }
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
        playerManager.release()
    }

    enum class RepeatMode {
        NONE, ONE, ALL
    }
}
