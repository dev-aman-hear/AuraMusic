package com.aman.auramusic.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.MusicRepository
import com.aman.auramusic.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.io.InputStream

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val userRepository = UserPreferencesRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val username = userRepository.usernameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "Master"
    )

    val favoriteIds = userRepository.favoriteIdsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet()
    )

    val playbackHistory = userRepository.historyFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val playlists = userRepository.playlistsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val recentSearches = userRepository.recentSearchesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val settings = userRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    init {
        loadSongs()
    }

    fun loadSongs(forceRefresh: Boolean = false) {
        if (!forceRefresh && _songs.value.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _songs.value = repository.getAllSongs()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUsername(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setUsername(name)
        }
    }

    fun toggleFavorite(songId: Long, favorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setFavorite(songId, favorite)
        }
    }

    fun recordPlayback(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordPlayback(songId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.clearHistory()
        }
    }

    fun recordSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordRecentSearch(query)
        }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deletePlaylist(playlistId)
        }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun savePlaylist(name: String, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.savePlaylist(name, songIds)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.renamePlaylist(playlistId, newName)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setDynamicColors(enabled)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setAmoledMode(enabled)
        }
    }

    fun setBlurIntensity(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setBlurIntensity(value)
        }
    }

    fun setKaraokeMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setKaraokeMode(enabled)
        }
    }

    fun setLyricFontScale(scale: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setLyricFontScale(scale)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setCrossfadeEnabled(enabled)
        }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setGaplessEnabled(enabled)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setSkipSilence(enabled)
        }
    }

    fun setSmartAudioFocus(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setSmartAudioFocus(enabled)
        }
    }

    fun setKeepPlayingOnClose(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setKeepPlayingOnClose(enabled)
        }
    }

    fun setPlaylistGridColumns(columns: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPlaylistGridColumns(columns)
        }
    }

    fun importSystemPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            val systemPlaylists = repository.getSystemPlaylists()
            val allSongs = _songs.value
            
            systemPlaylists.forEach { (name, titles) ->
                val songIds = titles.mapNotNull { title ->
                    allSongs.find { it.title.equals(title, ignoreCase = true) }?.id
                }
                if (songIds.isNotEmpty()) {
                    userRepository.savePlaylist(name, songIds)
                }
            }
        }
    }

    fun exportPlaylistToFile(playlist: Playlist, outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("name", playlist.name)
                    put("songs", JSONArray(playlist.songIds))
                }
                outputStream?.use { it.write(json.toString().toByteArray()) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun importPlaylistFromFile(inputStream: InputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: return@launch
                val json = JSONObject(content)
                val name = json.optString("name", "Imported Playlist")
                val songArray = json.optJSONArray("songs") ?: JSONArray()
                val songIds = mutableListOf<Long>()
                for (i in 0 until songArray.length()) {
                    songIds.add(songArray.optLong(i))
                }
                
                // Filter to only include songs that exist in our library
                val allSongIds = _songs.value.map { it.id }.toSet()
                val validSongIds = songIds.filter { it in allSongIds }
                
                if (validSongIds.isNotEmpty()) {
                    userRepository.savePlaylist(name, validSongIds)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
