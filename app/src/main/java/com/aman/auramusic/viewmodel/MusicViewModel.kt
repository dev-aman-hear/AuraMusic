package com.aman.auramusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.AppSettings
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

    private val _externalSongToPlay = MutableStateFlow<Song?>(null)
    val externalSongToPlay = _externalSongToPlay.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val username = userRepository.usernameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "Master",
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

    val settings = userRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    init {
        loadSongs()
    }

    fun handleExternalUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = repository.getSongFromUri(uri)
            if (song != null) {
                _externalSongToPlay.value = song
            } else {
                android.util.Log.e("MusicViewModel", "Failed to resolve URI: $uri")
            }
        }
    }

    fun clearExternalSong() {
        _externalSongToPlay.value = null
    }

    fun loadSongs(forceRefresh: Boolean = false) {
        if (!forceRefresh && _songs.value.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _songs.value = repository.getAllSongs()
            } catch (_: Exception) {
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

    fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setFavorite(songId, isFavorite)
        }
    }

    fun recordPlayback(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordPlayback(songId, System.currentTimeMillis())
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.clearHistory()
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

    fun setBlurIntensity(intensity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setBlurIntensity(intensity)
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

    data class ImportResult(
        val playlistName: String,
        val matchedCount: Int,
        val unmatchedSongs: List<String>
    )

    private val _lastImportResult = MutableStateFlow<ImportResult?>(null)
    val lastImportResult = _lastImportResult.asStateFlow()

    fun clearImportResult() {
        _lastImportResult.value = null
    }

    fun setPlaylistGridColumns(columns: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPlaylistGridColumns(columns)
        }
    }

    fun setDynamicPillEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setDynamicPillEnabled(enabled)
        }
    }

    fun setPillPosition(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPillPosition(position)
        }
    }

    fun setPillVerticalOffset(offset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPillVerticalOffset(offset)
        }
    }

    fun setPillSizeScale(scale: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPillSizeScale(scale)
        }
    }

    fun getSongFromUri(uri: android.net.Uri): Song? {
        return repository.getSongFromUri(uri)
    }

    fun exportCurrentSongs(songs: List<Song>, outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songArray = JSONArray()
                songs.forEach { song ->
                    songArray.put(
                        JSONObject().apply {
                            put("id", song.id)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("album", song.album)
                        }
                    )
                }

                val json = JSONObject().apply {
                    put("version", 1)
                    put("name", "All Songs")
                    put("songs", songArray)
                }
                outputStream?.use { it.write(json.toString().toByteArray()) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun exportPlaylistToFile(playlist: Playlist, outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songsInPlaylist = _songs.value.filter { it.id in playlist.songIds }
                val songArray = JSONArray()
                songsInPlaylist.forEach { song ->
                    songArray.put(
                        JSONObject().apply {
                            put("id", song.id)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("album", song.album)
                        }
                    )
                }

                val json = JSONObject().apply {
                    put("version", 1)
                    put("name", playlist.name)
                    put("songs", songArray)
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
                if (_songs.value.isEmpty()) {
                    _songs.value = repository.getAllSongs()
                }

                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: return@launch
                val json = JSONObject(content)

                if (json.has("playlists")) {
                    val playlistsArray = json.getJSONArray("playlists")
                    for (i in 0 until playlistsArray.length()) {
                        processPlaylistObject(playlistsArray.getJSONObject(i))
                    }
                } else {
                    processPlaylistObject(json)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun processPlaylistObject(json: JSONObject) {
        val name = json.optString("name", json.optString("playlistName", "Imported Playlist"))
        val songArray = json.optJSONArray("songs") ?: json.optJSONArray("tracks") ?: JSONArray()
        
        val validSongIds = mutableListOf<Long>()
        val unmatchedSongs = mutableListOf<String>()
        val currentSongs = _songs.value

        for (i in 0 until songArray.length()) {
            val songJson = songArray.opt(i)
            if (songJson is JSONObject) {
                val title = songJson.optString("title").trim()
                val artist = songJson.optString("artist").trim()
                val originalId = songJson.optLong("id", -1L)

                if (title.isBlank()) continue

                val match = currentSongs.find {
                    it.title.trim().equals(title, ignoreCase = true) &&
                    it.artist.trim().equals(artist, ignoreCase = true)
                } ?: currentSongs.find {
                    // Try matching by title exactly, but artist partially (e.g. "Artist A & Artist B" matching "Artist A")
                    val itArtist = it.artist.trim().lowercase()
                    val targetArtist = artist.lowercase()
                    it.title.trim().equals(title, ignoreCase = true) &&
                    (targetArtist.isBlank() || itArtist.contains(targetArtist) || targetArtist.contains(itArtist))
                } ?: if (originalId != -1L) currentSongs.find { it.id == originalId } else null

                if (match != null) {
                    validSongIds.add(match.id)
                } else {
                    unmatchedSongs.add("$title - $artist")
                }
            } else if (songJson is Number) {
                val songId = songJson.toLong()
                val match = currentSongs.find { it.id == songId }
                if (match != null) {
                    validSongIds.add(match.id)
                }
            }
        }
        
        if (validSongIds.isNotEmpty()) {
            userRepository.savePlaylist(name, validSongIds)
        }

        _lastImportResult.value = ImportResult(
            playlistName = name,
            matchedCount = validSongIds.size,
            unmatchedSongs = unmatchedSongs
        )
    }
}
