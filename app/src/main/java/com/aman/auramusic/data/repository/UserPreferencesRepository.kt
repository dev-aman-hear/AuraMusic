package com.aman.auramusic.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.appDataStore by preferencesDataStore(name = "aura_music_state")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.appDataStore

    val usernameFlow: Flow<String> = dataStore.data.map { prefs ->
        val name = prefs[Keys.username]
        if (name.isNullOrBlank()) "Master" else name
    }.distinctUntilChanged()

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            dynamicColors = prefs[Keys.dynamicColors] ?: true,
            amoledMode = prefs[Keys.amoledMode] ?: true,
            blurIntensity = prefs[Keys.blurIntensity] ?: 42,
            karaokeMode = prefs[Keys.karaokeMode] ?: true,
            lyricFontScale = prefs[Keys.lyricFontScale] ?: 1.0f,
            crossfadeEnabled = prefs[Keys.crossfadeEnabled] ?: false,
            gaplessEnabled = prefs[Keys.gaplessEnabled] ?: true,
            skipSilence = prefs[Keys.skipSilence] ?: false,
            playlistGridColumns = prefs[Keys.playlistGridColumns] ?: 2
        )
    }.distinctUntilChanged()

    val favoriteIdsFlow: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[Keys.favoriteIds].decodeLongSet()
    }.distinctUntilChanged()

    val recentSearchesFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.recentSearches].decodeStringList()
    }.distinctUntilChanged()

    val historyFlow: Flow<List<PlaybackHistoryEntry>> = dataStore.data.map { prefs ->
        prefs[Keys.playbackHistory].decodeHistory()
    }.distinctUntilChanged()

    val playlistsFlow: Flow<List<Playlist>> = dataStore.data.map { prefs ->
        prefs[Keys.playlists].decodePlaylists()
    }.distinctUntilChanged()

    suspend fun setUsername(name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.username] = name
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[Keys.dynamicColors] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { it[Keys.amoledMode] = enabled }
    }

    suspend fun setBlurIntensity(value: Int) {
        dataStore.edit { it[Keys.blurIntensity] = value.coerceIn(0, 100) }
    }

    suspend fun setKaraokeMode(enabled: Boolean) {
        dataStore.edit { it[Keys.karaokeMode] = enabled }
    }

    suspend fun setLyricFontScale(scale: Float) {
        dataStore.edit { it[Keys.lyricFontScale] = scale.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.crossfadeEnabled] = enabled }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.gaplessEnabled] = enabled }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        dataStore.edit { it[Keys.skipSilence] = enabled }
    }

    suspend fun setPlaylistGridColumns(columns: Int) {
        dataStore.edit { it[Keys.playlistGridColumns] = columns.coerceIn(1, 2) }
    }

    suspend fun setFavorite(songId: Long, favorite: Boolean) {
        dataStore.edit { prefs ->
            val updated = prefs[Keys.favoriteIds].decodeLongSet().toMutableSet()
            if (favorite) {
                updated += songId
            } else {
                updated -= songId
            }
            prefs[Keys.favoriteIds] = updated.encodeLongSet()
        }
    }

    suspend fun recordRecentSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        dataStore.edit { prefs ->
            val items = prefs[Keys.recentSearches].decodeStringList().toMutableList()
            items.removeAll { it.equals(normalized, ignoreCase = true) }
            items.add(0, normalized)
            prefs[Keys.recentSearches] = items.take(10).encodeStringList()
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { it[Keys.recentSearches] = "[]" }
    }

    suspend fun clearHistory() {
        dataStore.edit { it[Keys.playbackHistory] = "[]" }
    }

    suspend fun recordPlayback(songId: Long, playedAt: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            val entries = prefs[Keys.playbackHistory].decodeHistory().toMutableList()
            val index = entries.indexOfFirst { it.songId == songId }
            if (index >= 0) {
                val existing = entries[index]
                entries[index] = existing.copy(
                    playedAt = playedAt,
                    playCount = existing.playCount + 1
                )
            } else {
                entries.add(0, PlaybackHistoryEntry(songId = songId, playedAt = playedAt, playCount = 1))
            }
            prefs[Keys.playbackHistory] = entries
                .sortedByDescending { it.playedAt }
                .take(200)
                .encodeHistory()
        }
    }

    suspend fun savePlaylist(name: String, songIds: List<Long>, artworkSongId: Long? = songIds.firstOrNull()): Playlist {
        val playlist = Playlist(
            id = System.currentTimeMillis(),
            name = name.trim().ifBlank { "New Playlist" },
            songIds = songIds.distinct(),
            artworkSongId = artworkSongId
        )
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().toMutableList()
            playlists.add(0, playlist)
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
        return playlist
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().toMutableList()
            val index = playlists.indexOfFirst { it.id == playlist.id }
            if (index >= 0) {
                playlists[index] = playlist.copy(updatedAt = System.currentTimeMillis())
                prefs[Keys.playlists] = playlists.encodePlaylists()
            }
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().filterNot { it.id == playlistId }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(name = newName.trim().ifBlank { playlist.name }, updatedAt = System.currentTimeMillis())
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(
                        songIds = (playlist.songIds + songId).distinct(),
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(
                        songIds = playlist.songIds.filterNot { it == songId },
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
    }

    fun isFavoriteFlow(songId: Long): Flow<Boolean> {
        return favoriteIdsFlow.map { ids -> ids.contains(songId) }.distinctUntilChanged()
    }

    companion object {
        private object Keys {
            val username = stringPreferencesKey("username")
            val dynamicColors = booleanPreferencesKey("dynamic_colors")
            val amoledMode = booleanPreferencesKey("amoled_mode")
            val blurIntensity = intPreferencesKey("blur_intensity")
            val karaokeMode = booleanPreferencesKey("karaoke_mode")
            val lyricFontScale = floatPreferencesKey("lyric_font_scale")
            val crossfadeEnabled = booleanPreferencesKey("crossfade_enabled")
            val gaplessEnabled = booleanPreferencesKey("gapless_enabled")
            val skipSilence = booleanPreferencesKey("skip_silence")
            val playlistGridColumns = intPreferencesKey("playlist_grid_columns")
            val favoriteIds = stringPreferencesKey("favorite_ids")
            val recentSearches = stringPreferencesKey("recent_searches")
            val playbackHistory = stringPreferencesKey("playback_history")
            val playlists = stringPreferencesKey("playlists")
        }
    }
}

private fun String?.decodeLongSet(): Set<Long> {
    if (this.isNullOrBlank()) return emptySet()
    return runCatching {
        JSONArray(this).let { array ->
            buildSet {
                for (i in 0 until array.length()) {
                    add(array.optLong(i))
                }
            }
        }
    }.getOrDefault(emptySet())
}

private fun Set<Long>.encodeLongSet(): String {
    val array = JSONArray()
    forEach { array.put(it) }
    return array.toString()
}

private fun String?.decodeStringList(): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    add(array.optString(i))
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<String>.encodeStringList(): String {
    val array = JSONArray()
    forEach { array.put(it) }
    return array.toString()
}

private fun String?.decodeHistory(): List<PlaybackHistoryEntry> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        PlaybackHistoryEntry(
                            songId = item.optLong("songId"),
                            playedAt = item.optLong("playedAt"),
                            playCount = item.optInt("playCount", 1)
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<PlaybackHistoryEntry>.encodeHistory(): String {
    val array = JSONArray()
    forEach { entry ->
        array.put(
            JSONObject()
                .put("songId", entry.songId)
                .put("playedAt", entry.playedAt)
                .put("playCount", entry.playCount)
        )
    }
    return array.toString()
}

private fun String?.decodePlaylists(): List<Playlist> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val songs = buildList {
                        val songsArray = item.optJSONArray("songIds") ?: JSONArray()
                        for (j in 0 until songsArray.length()) {
                            add(songsArray.optLong(j))
                        }
                    }
                    add(
                        Playlist(
                            id = item.optLong("id"),
                            name = item.optString("name"),
                            songIds = songs,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                            artworkSongId = if (item.has("artworkSongId") && !item.isNull("artworkSongId")) item.optLong("artworkSongId") else null
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<Playlist>.encodePlaylists(): String {
    val array = JSONArray()
    forEach { playlist ->
        array.put(
            JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("songIds", JSONArray(playlist.songIds))
                .put("createdAt", playlist.createdAt)
                .put("updatedAt", playlist.updatedAt)
                .put("artworkSongId", playlist.artworkSongId)
        )
    }
    return array.toString()
}
