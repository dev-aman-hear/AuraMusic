package com.aman.auramusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.SavedOnlineArtist
import com.aman.auramusic.data.repository.MusicRepository
import com.aman.auramusic.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

@HiltViewModel
class MusicViewModel @Inject constructor(
    application: Application,
    private val repository: MusicRepository,
    private val userRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val onlineRepository by lazy { com.aman.auramusic.online.network.repository.OnlineMusicRepository() }

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

    val favoriteOnlineSongs = userRepository.favoriteOnlineSongsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
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

    val savedOnlinePlaylists = userRepository.savedOnlinePlaylistsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val savedOnlineArtists = userRepository.savedOnlineArtistsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val savedOnlineAlbums = userRepository.savedOnlineAlbumsFlow.stateIn(
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

    fun toggleFavorite(songId: Long, isFavorite: Boolean, onlineSong: OnlineSong? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setFavorite(songId, isFavorite, onlineSong)
        }
    }

    fun toggleFavorites(items: List<Pair<Long, OnlineSong?>>, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setFavorites(items, isFavorite)
        }
    }

    fun recordPlayback(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordPlayback(songId, System.currentTimeMillis())
        }
    }

    fun recordOnlinePlayback(onlineSong: OnlineSong) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.recordOnlinePlayback(onlineSong, System.currentTimeMillis())
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

    fun addOnlineSongToPlaylist(playlistId: Long, onlineSong: OnlineSong) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.addOnlineSongToPlaylist(playlistId, onlineSong)
        }
    }

    fun removeOnlineSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeOnlineSongFromPlaylist(playlistId, songId)
        }
    }

    fun saveOnlinePlaylistToLibrary(playlist: OnlinePlaylist) {
        viewModelScope.launch(Dispatchers.IO) {
            var pl = playlist
            if (pl.songs.isEmpty()) {
                val fetched = onlineRepository.getPlaylistSongs(pl)
                if (fetched.isNotEmpty()) {
                    pl = pl.copy(songs = fetched, songCount = fetched.size)
                }
            }
            userRepository.saveOnlinePlaylist(pl)
        }
    }

    fun removeOnlinePlaylistFromLibrary(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeOnlinePlaylist(playlistId)
        }
    }

    fun saveOnlineArtistToLibrary(artist: SavedOnlineArtist) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.saveOnlineArtist(artist)
        }
    }

    fun removeOnlineArtistFromLibrary(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeOnlineArtist(artistId)
        }
    }

    fun saveOnlineAlbumToLibrary(album: OnlinePlaylist) {
        viewModelScope.launch(Dispatchers.IO) {
            var al = album
            if (al.songs.isEmpty()) {
                val fetched = onlineRepository.getPlaylistSongs(al)
                if (fetched.isNotEmpty()) {
                    al = al.copy(songs = fetched, songCount = fetched.size)
                }
            }
            userRepository.saveOnlineAlbum(al)
        }
    }

    fun removeOnlineAlbumFromLibrary(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeOnlineAlbum(albumId)
        }
    }

    fun savePlaylist(name: String, songIds: List<Long>, onlineSongs: List<OnlineSong> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.savePlaylist(name, songIds, onlineSongs = onlineSongs)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.renamePlaylist(playlistId, newName)
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

    sealed interface OperationMessage {
        data class Success(val message: String) : OperationMessage
        data class Error(val message: String) : OperationMessage
    }

    data class ImportResult(
        val playlistName: String,
        val matchedCount: Int,
        val unmatchedSongs: List<String>
    )

    private val _operationEvent = MutableSharedFlow<OperationMessage>(extraBufferCapacity = 1)
    val operationEvent: SharedFlow<OperationMessage> = _operationEvent.asSharedFlow()

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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemeMode(mode)
        }
    }

    fun setDynamicBottomNavigationEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setDynamicBottomNavigationEnabled(enabled)
        }
    }

    fun setPlayerThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setPlayerThemeMode(mode)
        }
    }

    fun setDynamicArtworkColors(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setDynamicArtworkColors(enabled)
        }
    }

    fun setColorPreset(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setColorPreset(preset)
        }
    }

    fun setCustomPaletteHexes(hexes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setCustomPaletteHexes(hexes)
        }
    }

    fun setEnableFullAppDynamicTheme(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setEnableFullAppDynamicTheme(enabled)
        }
    }

    fun setAppPaletteMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setAppPaletteMode(mode)
        }
    }

    fun setAppColorPreset(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setAppColorPreset(preset)
        }
    }

    fun setAppCustomPaletteHexes(hexes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setAppCustomPaletteHexes(hexes)
        }
    }

    fun setChangeThemeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setChangeThemeEnabled(enabled)
        }
    }

    fun setThemeScope(scope: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemeScope(scope)
        }
    }

    fun setThemeColorStyle(style: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemeColorStyle(style)
        }
    }

    fun setThemePreset(preset: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemePreset(preset)
        }
    }

    fun setThemeCustomHexes(hexes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemeCustomHexes(hexes)
        }
    }

    fun setThemeAreaEnabled(areaName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setThemeAreaEnabled(areaName, enabled)
        }
    }

    fun exportCurrentSongs(songs: List<Song>, outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (outputStream == null) {
                _operationEvent.tryEmit(OperationMessage.Error("Unable to write export file"))
                return@launch
            }
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
                outputStream.use { it.write(json.toString().toByteArray()) }
                _operationEvent.tryEmit(OperationMessage.Success("All songs exported successfully"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.tryEmit(OperationMessage.Error("Failed to export songs: invalid or unavailable file"))
            }
        }
    }

    fun exportPlaylistToFile(playlist: Playlist, outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (outputStream == null) {
                _operationEvent.tryEmit(OperationMessage.Error("Unable to write export file"))
                return@launch
            }
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
                outputStream.use { it.write(json.toString().toByteArray()) }
                _operationEvent.tryEmit(OperationMessage.Success("Playlist \"${playlist.name}\" exported successfully"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.tryEmit(OperationMessage.Error("Failed to export playlist: invalid or unavailable file"))
            }
        }
    }

    fun importPlaylistFromFile(inputStream: InputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (inputStream == null) {
                _operationEvent.tryEmit(OperationMessage.Error("Unable to read import file"))
                return@launch
            }
            try {
                if (_songs.value.isEmpty()) {
                    _songs.value = repository.getAllSongs()
                }

                val content = inputStream.bufferedReader().use { it.readText() }
                if (content.isBlank()) {
                    _operationEvent.tryEmit(OperationMessage.Error("Import file is empty"))
                    return@launch
                }
                val json = JSONObject(content)

                if (json.has("playlists")) {
                    val playlistsArray = json.getJSONArray("playlists")
                    if (playlistsArray.length() == 0) {
                        _operationEvent.tryEmit(OperationMessage.Error("No playlists found in file"))
                        return@launch
                    }
                    for (i in 0 until playlistsArray.length()) {
                        processPlaylistObject(playlistsArray.getJSONObject(i))
                    }
                } else {
                    processPlaylistObject(json)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: JSONException) {
                _operationEvent.tryEmit(OperationMessage.Error("Invalid or corrupted playlist file format"))
            } catch (e: Exception) {
                _operationEvent.tryEmit(OperationMessage.Error("Failed to import playlist"))
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
            _operationEvent.tryEmit(OperationMessage.Success("Playlist \"$name\" imported (${validSongIds.size} matched)"))
        } else {
            _operationEvent.tryEmit(OperationMessage.Error("No matching songs found for \"$name\""))
        }

        _lastImportResult.value = ImportResult(
            playlistName = name,
            matchedCount = validSongIds.size,
            unmatchedSongs = unmatchedSongs
        )
    }
}
