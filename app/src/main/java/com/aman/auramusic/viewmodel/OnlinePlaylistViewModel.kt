package com.aman.auramusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.auramusic.data.local.entity.ImportedPlaylistEntity
import com.aman.auramusic.data.model.OnlinePlaylist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.OnlinePlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnlinePlaylistUiState {
    object Loading : OnlinePlaylistUiState()
    data class Success(val playlists: List<OnlinePlaylist>) : OnlinePlaylistUiState()
    data class Error(val message: String) : OnlinePlaylistUiState()
    object Empty : OnlinePlaylistUiState()
}

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    private val repository: OnlinePlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnlinePlaylistUiState>(OnlinePlaylistUiState.Loading)
    val uiState: StateFlow<OnlinePlaylistUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var allPlaylists = listOf<OnlinePlaylist>()

    val importedPlaylists: StateFlow<List<ImportedPlaylistEntity>> = repository.getImportedPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Specifically for the Home Screen: Online playlists that have matches in local library.
     */
    private val _matchedPlaylists = MutableStateFlow<List<OnlinePlaylist>>(emptyList())
    val matchedPlaylists: StateFlow<List<OnlinePlaylist>> = _matchedPlaylists.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = OnlinePlaylistUiState.Loading
            try {
                allPlaylists = repository.getPlaylists()
                filterPlaylists()
            } catch (t: Throwable) {
                _uiState.value = OnlinePlaylistUiState.Error("Unable to connect to online music server.")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Logic to identify online playlists that contain local songs.
     * This is called when local library songs are available.
     */
    fun computeMatches(localSongs: List<Song>) {
        viewModelScope.launch {
            val matching = allPlaylists.filter { playlist ->
                try {
                    val details = repository.getPlaylistDetails(playlist.playlistUrl)
                    repository.findMatches(details.songs, localSongs).isNotEmpty()
                } catch (t: Throwable) {
                    false
                }
            }
            _matchedPlaylists.value = matching
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        loadPlaylists()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterPlaylists()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        filterPlaylists()
    }

    private fun filterPlaylists() {
        val query = _searchQuery.value
        val category = _selectedCategory.value

        val filtered = allPlaylists.filter {
            (category == "All" || it.category == category) &&
            (it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true))
        }

        if (filtered.isEmpty()) {
            _uiState.value = OnlinePlaylistUiState.Empty
        } else {
            _uiState.value = OnlinePlaylistUiState.Success(filtered)
        }
    }

    fun importPlaylist(playlist: OnlinePlaylist) {
        viewModelScope.launch {
            try {
                repository.downloadAndImportPlaylist(playlist)
            } catch (t: Throwable) {
                // Handle error safely
            }
        }
    }
}
