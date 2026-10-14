package com.aman.auramusic.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LiquidGlassConfig
import com.aman.auramusic.data.model.LiquidGlassPreset
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.SavedOnlineArtist
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.appDataStore

    val usernameFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.username] ?: "Master"
    }.distinctUntilChanged()

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            dynamicColors = prefs[Keys.dynamicColors] ?: true,
            amoledMode = prefs[Keys.amoledMode] ?: false,
            blurIntensity = prefs[Keys.blurIntensity] ?: 40,
            karaokeMode = prefs[Keys.karaokeMode] ?: false,
            lyricFontScale = prefs[Keys.lyricFontScale] ?: 1.0f,
            crossfadeEnabled = prefs[Keys.crossfadeEnabled] ?: false,
            gaplessEnabled = prefs[Keys.gaplessEnabled] ?: true,
            skipSilence = prefs[Keys.skipSilence] ?: false,
            smartAudioFocus = prefs[Keys.smartAudioFocus] ?: true,
            keepPlayingOnClose = prefs[Keys.keepPlayingOnClose] ?: false,
            playlistGridColumns = prefs[Keys.playlistGridColumns] ?: 2,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            liquidGlass = LiquidGlassConfig(
                preset = LiquidGlassPreset.fromName(prefs[Keys.glassPreset]),
                intensity = prefs[Keys.glassIntensity] ?: 0.50f,
                blurRadius = prefs[Keys.glassBlurRadius] ?: 24,
                highlightStrength = prefs[Keys.glassHighlightStrength] ?: 0.75f,
                shadowElevation = prefs[Keys.glassShadowElevation] ?: 14,
                cornerRadius = prefs[Keys.glassCornerRadius] ?: 24,
                tintHex = prefs[Keys.glassTintHex] ?: "",
                accentTintEnabled = prefs[Keys.glassAccentTintEnabled] ?: false,
                reduceTransparency = prefs[Keys.glassReduceTransparency] ?: false,
                reduceMotion = prefs[Keys.glassReduceMotion] ?: false
            )
        )
    }.distinctUntilChanged()

    val favoriteIdsFlow: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[Keys.favoriteIds].decodeLongSet()
    }.distinctUntilChanged()

    val favoriteOnlineSongsFlow: Flow<List<OnlineSong>> = dataStore.data.map { prefs ->
        prefs[Keys.favoriteOnlineSongs].decodeOnlineSongs()
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

    val savedOnlinePlaylistsFlow: Flow<List<OnlinePlaylist>> = dataStore.data.map { prefs ->
        prefs[Keys.savedOnlinePlaylists].decodeSavedOnlinePlaylists()
    }.distinctUntilChanged()

    val savedOnlineArtistsFlow: Flow<List<SavedOnlineArtist>> = dataStore.data.map { prefs ->
        prefs[Keys.savedOnlineArtists].decodeSavedOnlineArtists()
    }.distinctUntilChanged()

    val savedOnlineAlbumsFlow: Flow<List<OnlinePlaylist>> = dataStore.data.map { prefs ->
        prefs[Keys.savedOnlineAlbums].decodeSavedOnlinePlaylists()
    }.distinctUntilChanged()

    suspend fun setUsername(name: String) {
        dataStore.edit { it[Keys.username] = name }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[Keys.dynamicColors] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { it[Keys.amoledMode] = enabled }
    }

    suspend fun setBlurIntensity(intensity: Int) {
        dataStore.edit { it[Keys.blurIntensity] = intensity }
    }

    suspend fun setKaraokeMode(enabled: Boolean) {
        dataStore.edit { it[Keys.karaokeMode] = enabled }
    }

    suspend fun setLyricFontScale(scale: Float) {
        dataStore.edit { it[Keys.lyricFontScale] = scale }
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

    suspend fun setSmartAudioFocus(enabled: Boolean) {
        dataStore.edit { it[Keys.smartAudioFocus] = enabled }
    }

    suspend fun setKeepPlayingOnClose(enabled: Boolean) {
        dataStore.edit { it[Keys.keepPlayingOnClose] = enabled }
    }

    suspend fun setPlaylistGridColumns(columns: Int) {
        dataStore.edit { it[Keys.playlistGridColumns] = columns.coerceIn(1, 2) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setLiquidGlassPreset(preset: LiquidGlassPreset) {
        val defaultConfig = LiquidGlassConfig.forPreset(preset)
        dataStore.edit {
            it[Keys.glassPreset] = preset.name
            it[Keys.glassIntensity] = defaultConfig.intensity
            it[Keys.glassBlurRadius] = defaultConfig.blurRadius
            it[Keys.glassHighlightStrength] = defaultConfig.highlightStrength
            it[Keys.glassShadowElevation] = defaultConfig.shadowElevation
            it[Keys.glassCornerRadius] = defaultConfig.cornerRadius
            it[Keys.glassTintHex] = defaultConfig.tintHex
            it[Keys.glassAccentTintEnabled] = defaultConfig.accentTintEnabled
        }
    }

    suspend fun updateLiquidGlassConfig(config: LiquidGlassConfig) {
        dataStore.edit {
            it[Keys.glassPreset] = config.preset.name
            it[Keys.glassIntensity] = config.intensity
            it[Keys.glassBlurRadius] = config.blurRadius
            it[Keys.glassHighlightStrength] = config.highlightStrength
            it[Keys.glassShadowElevation] = config.shadowElevation
            it[Keys.glassCornerRadius] = config.cornerRadius
            it[Keys.glassTintHex] = config.tintHex
            it[Keys.glassAccentTintEnabled] = config.accentTintEnabled
            it[Keys.glassReduceTransparency] = config.reduceTransparency
            it[Keys.glassReduceMotion] = config.reduceMotion
        }
    }

    suspend fun setLiquidGlassIntensity(intensity: Float) {
        dataStore.edit {
            it[Keys.glassIntensity] = intensity.coerceIn(0.10f, 0.90f)
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassBlurRadius(blurRadius: Int) {
        dataStore.edit {
            it[Keys.glassBlurRadius] = blurRadius.coerceIn(0, 50)
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassHighlightStrength(highlightStrength: Float) {
        dataStore.edit {
            it[Keys.glassHighlightStrength] = highlightStrength.coerceIn(0.0f, 1.0f)
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassShadowElevation(elevation: Int) {
        dataStore.edit {
            it[Keys.glassShadowElevation] = elevation.coerceIn(0, 24)
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassCornerRadius(radius: Int) {
        dataStore.edit {
            it[Keys.glassCornerRadius] = radius.coerceIn(8, 36)
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassTintHex(tintHex: String) {
        dataStore.edit {
            it[Keys.glassTintHex] = tintHex
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassAccentTintEnabled(enabled: Boolean) {
        dataStore.edit {
            it[Keys.glassAccentTintEnabled] = enabled
            it[Keys.glassPreset] = LiquidGlassPreset.CUSTOM.name
        }
    }

    suspend fun setLiquidGlassReduceTransparency(reduce: Boolean) {
        dataStore.edit { it[Keys.glassReduceTransparency] = reduce }
    }

    suspend fun setLiquidGlassReduceMotion(reduce: Boolean) {
        dataStore.edit { it[Keys.glassReduceMotion] = reduce }
    }

    suspend fun resetLiquidGlassConfig() {
        setLiquidGlassPreset(LiquidGlassPreset.LIQUID)
    }

    suspend fun setFavorite(songId: Long, isFavorite: Boolean, onlineSong: OnlineSong? = null) {
        dataStore.edit { prefs ->
            val ids = prefs[Keys.favoriteIds].decodeLongSet().toMutableSet()
            if (isFavorite) ids.add(songId) else ids.remove(songId)
            prefs[Keys.favoriteIds] = ids.encodeLongSet()

            val onlineList = prefs[Keys.favoriteOnlineSongs].decodeOnlineSongs().toMutableList()
            if (onlineSong != null) {
                onlineList.removeAll { 
                    it.id == onlineSong.id || 
                    (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == songId 
                }
                if (isFavorite) {
                    onlineList.add(0, onlineSong.copy(streamUrl = ""))
                }
                prefs[Keys.favoriteOnlineSongs] = onlineList.encodeOnlineSongs()
            } else if (!isFavorite) {
                val removed = onlineList.removeAll { 
                    (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == songId 
                }
                if (removed) {
                    prefs[Keys.favoriteOnlineSongs] = onlineList.encodeOnlineSongs()
                }
            }
        }
    }

    suspend fun recordRecentSearch(query: String) {
        if (query.isBlank()) return
        dataStore.edit { prefs ->
            val searches = prefs[Keys.recentSearches].decodeStringList().toMutableList()
            searches.remove(query)
            searches.add(0, query)
            prefs[Keys.recentSearches] = searches.take(10).encodeStringList()
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(Keys.recentSearches) }
    }

    suspend fun clearHistory() {
        dataStore.edit { it.remove(Keys.playbackHistory) }
    }

    suspend fun recordPlayback(songId: Long, playedAt: Long) {
        dataStore.edit { prefs ->
            val history = prefs[Keys.playbackHistory].decodeHistory().toMutableList()
            val existing = history.find { it.songId == songId }
            if (existing != null) {
                history.remove(existing)
                history.add(0, existing.copy(playedAt = playedAt, playCount = existing.playCount + 1))
            } else {
                history.add(0, PlaybackHistoryEntry(songId, playedAt))
            }
            prefs[Keys.playbackHistory] = history
                .sortedByDescending { it.playedAt }
                .take(200)
                .encodeHistory()
        }
    }

    suspend fun recordOnlinePlayback(onlineSong: OnlineSong, playedAt: Long) {
        val songId = (onlineSong.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L)
        val onlineJson = runCatching { Gson().toJson(onlineSong) }.getOrNull()
        dataStore.edit { prefs ->
            val history = prefs[Keys.playbackHistory].decodeHistory().toMutableList()
            val existing = history.find { it.songId == songId || (it.onlineSongJson != null && it.onlineSongJson.contains("\"id\":\"${onlineSong.id}\"")) }
            if (existing != null) {
                history.remove(existing)
                history.add(0, existing.copy(playedAt = playedAt, playCount = existing.playCount + 1, onlineSongJson = onlineJson ?: existing.onlineSongJson))
            } else {
                history.add(0, PlaybackHistoryEntry(songId = songId, playedAt = playedAt, playCount = 1, onlineSongJson = onlineJson))
            }
            prefs[Keys.playbackHistory] = history
                .sortedByDescending { it.playedAt }
                .take(200)
                .encodeHistory()
        }
    }

    suspend fun savePlaylist(
        name: String,
        songIds: List<Long>,
        artworkSongId: Long? = songIds.firstOrNull(),
        onlineSongs: List<OnlineSong> = emptyList()
    ): Playlist {
        val sanitizedOnline = onlineSongs.map { it.copy(streamUrl = "") }
        val playlist = Playlist(
            id = System.currentTimeMillis(),
            name = name.trim().ifBlank { "New Playlist" },
            songIds = songIds.distinct(),
            artworkSongId = artworkSongId,
            onlineSongs = sanitizedOnline
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


    suspend fun addOnlineSongToPlaylist(playlistId: Long, song: OnlineSong): Boolean {
        var added = false
        val sanitized = song.copy(streamUrl = "")
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().map { playlist ->
                if (playlist.id == playlistId) {
                    val exists = playlist.onlineSongs.any { it.id == sanitized.id && it.source == sanitized.source }
                    if (!exists) {
                        added = true
                        playlist.copy(
                            onlineSongs = playlist.onlineSongs + sanitized,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        playlist
                    }
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
        return added
    }

    suspend fun removeOnlineSongFromPlaylist(playlistId: Long, songId: String) {
        dataStore.edit { prefs ->
            val playlists = prefs[Keys.playlists].decodePlaylists().map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(
                        onlineSongs = playlist.onlineSongs.filterNot { it.id == songId },
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }
            prefs[Keys.playlists] = playlists.encodePlaylists()
        }
    }

    suspend fun saveOnlinePlaylist(playlist: OnlinePlaylist): Boolean {
        var added = false
        val reference = if (playlist.songs.isNotEmpty()) playlist.copy(songs = playlist.songs.take(100)) else playlist
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlinePlaylists].decodeSavedOnlinePlaylists().toMutableList()
            val existingIndex = list.indexOfFirst { it.id == reference.id }
            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                if (existing.songs.isEmpty() && reference.songs.isNotEmpty()) {
                    list[existingIndex] = existing.copy(
                        songs = reference.songs,
                        songCount = if (existing.songCount > 0) existing.songCount else reference.songs.size
                    )
                    prefs[Keys.savedOnlinePlaylists] = list.encodeSavedOnlinePlaylists()
                }
            } else {
                added = true
                list.add(0, reference)
                prefs[Keys.savedOnlinePlaylists] = list.encodeSavedOnlinePlaylists()
            }
        }
        return added
    }

    suspend fun removeOnlinePlaylist(playlistId: String) {
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlinePlaylists].decodeSavedOnlinePlaylists().filterNot { it.id == playlistId }
            prefs[Keys.savedOnlinePlaylists] = list.encodeSavedOnlinePlaylists()
        }
    }

    suspend fun saveOnlineArtist(artist: SavedOnlineArtist): Boolean {
        if (artist.id.isBlank() || artist.name.isBlank()) return false
        var added = false
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlineArtists].decodeSavedOnlineArtists().toMutableList()
            val exists = list.any { it.id == artist.id || (it.name.equals(artist.name, ignoreCase = true) && it.source == artist.source) }
            if (!exists) {
                added = true
                list.add(0, artist)
                prefs[Keys.savedOnlineArtists] = list.encodeSavedOnlineArtists()
            }
        }
        return added
    }

    suspend fun removeOnlineArtist(artistId: String) {
        if (artistId.isBlank()) return
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlineArtists].decodeSavedOnlineArtists().filterNot { it.id == artistId }
            prefs[Keys.savedOnlineArtists] = list.encodeSavedOnlineArtists()
        }
    }

    suspend fun saveOnlineAlbum(album: OnlinePlaylist): Boolean {
        if (album.id.isBlank() || album.title.isBlank()) return false
        var added = false
        val reference = if (album.songs.isNotEmpty()) album.copy(songs = album.songs.take(100)) else album
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlineAlbums].decodeSavedOnlinePlaylists().toMutableList()
            val existingIndex = list.indexOfFirst { it.id == reference.id }
            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                if (existing.songs.isEmpty() && reference.songs.isNotEmpty()) {
                    list[existingIndex] = existing.copy(
                        songs = reference.songs,
                        songCount = if (existing.songCount > 0) existing.songCount else reference.songs.size
                    )
                    prefs[Keys.savedOnlineAlbums] = list.encodeSavedOnlinePlaylists()
                }
            } else {
                added = true
                list.add(0, reference)
                prefs[Keys.savedOnlineAlbums] = list.encodeSavedOnlinePlaylists()
            }
        }
        return added
    }

    suspend fun removeOnlineAlbum(albumId: String) {
        if (albumId.isBlank()) return
        dataStore.edit { prefs ->
            val list = prefs[Keys.savedOnlineAlbums].decodeSavedOnlinePlaylists().filterNot { it.id == albumId }
            prefs[Keys.savedOnlineAlbums] = list.encodeSavedOnlinePlaylists()
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
            val smartAudioFocus = booleanPreferencesKey("smart_audio_focus")
            val keepPlayingOnClose = booleanPreferencesKey("keep_playing_on_close")
            val playlistGridColumns = intPreferencesKey("playlist_grid_columns")
            val themeMode = stringPreferencesKey("theme_mode")
            val glassPreset = stringPreferencesKey("glass_preset")
            val glassIntensity = floatPreferencesKey("glass_intensity")
            val glassBlurRadius = intPreferencesKey("glass_blur_radius")
            val glassHighlightStrength = floatPreferencesKey("glass_highlight_strength")
            val glassShadowElevation = intPreferencesKey("glass_shadow_elevation")
            val glassCornerRadius = intPreferencesKey("glass_corner_radius")
            val glassTintHex = stringPreferencesKey("glass_tint_hex")
            val glassAccentTintEnabled = booleanPreferencesKey("glass_accent_tint_enabled")
            val glassReduceTransparency = booleanPreferencesKey("glass_reduce_transparency")
            val glassReduceMotion = booleanPreferencesKey("glass_reduce_motion")
            val favoriteIds = stringPreferencesKey("favorite_ids")
            val favoriteOnlineSongs = stringPreferencesKey("favorite_online_songs")
            val recentSearches = stringPreferencesKey("recent_searches")
            val playbackHistory = stringPreferencesKey("playback_history")
            val playlists = stringPreferencesKey("playlists")
            val savedOnlinePlaylists = stringPreferencesKey("saved_online_playlists")
            val savedOnlineArtists = stringPreferencesKey("saved_online_artists")
            val savedOnlineAlbums = stringPreferencesKey("saved_online_albums")
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
                    val onlineJson = item.optString("onlineSongJson").takeIf { it.isNotBlank() }
                    add(
                        PlaybackHistoryEntry(
                            songId = item.optLong("songId"),
                            playedAt = item.optLong("playedAt"),
                            playCount = item.optInt("playCount", 1),
                            onlineSongJson = onlineJson
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
        val obj = JSONObject()
            .put("songId", entry.songId)
            .put("playedAt", entry.playedAt)
            .put("playCount", entry.playCount)
        if (!entry.onlineSongJson.isNullOrBlank()) {
            obj.put("onlineSongJson", entry.onlineSongJson)
        }
        array.put(obj)
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
                    val onlineSongs = buildList {
                        val onlineArray = item.optJSONArray("onlineSongs")
                        if (onlineArray != null) {
                            for (j in 0 until onlineArray.length()) {
                                val oObj = onlineArray.optJSONObject(j) ?: continue
                                val id = oObj.optString("id")
                                if (id.isBlank()) continue
                                val srcStr = oObj.optString("source", AudioSource.JIOSAAVN.name)
                                val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.JIOSAAVN)
                                add(
                                    OnlineSong(
                                        id = id,
                                        title = oObj.optString("title"),
                                        artist = oObj.optString("artist"),
                                        album = oObj.optString("album"),
                                        artworkUrl = oObj.optString("artworkUrl"),
                                        durationSeconds = oObj.optLong("durationSeconds", 0L),
                                        source = source,
                                        streamUrl = "", // NEVER store expiring stream URLs
                                        bitrate = oObj.optString("bitrate", "320 kbps"),
                                        format = oObj.optString("format", "AAC/MP4")
                                    )
                                )
                            }
                        }
                    }
                    add(
                        Playlist(
                            id = item.optLong("id"),
                            name = item.optString("name"),
                            songIds = songs,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                            artworkSongId = if (item.has("artworkSongId") && !item.isNull("artworkSongId")) item.optLong("artworkSongId") else null,
                            onlineSongs = onlineSongs
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
        val onlineArray = JSONArray()
        playlist.onlineSongs.forEach { os ->
            val obj = JSONObject()
                .put("id", os.id)
                .put("title", os.title)
                .put("artist", os.artist)
                .put("album", os.album)
                .put("artworkUrl", os.artworkUrl)
                .put("durationSeconds", os.durationSeconds)
                .put("source", os.source.name)
                .put("bitrate", os.bitrate)
                .put("format", os.format)
            onlineArray.put(obj)
        }
        array.put(
            JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("songIds", JSONArray(playlist.songIds))
                .put("createdAt", playlist.createdAt)
                .put("updatedAt", playlist.updatedAt)
                .put("artworkSongId", playlist.artworkSongId)
                .put("onlineSongs", onlineArray)
        )
    }
    return array.toString()
}

private fun String?.decodeSavedOnlinePlaylists(): List<OnlinePlaylist> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    val srcStr = item.optString("source", AudioSource.JIOSAAVN.name)
                    val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.JIOSAAVN)
                    val songs = buildList {
                        val songsArray = item.optJSONArray("songs") ?: item.optJSONArray("onlineSongs")
                        if (songsArray != null) {
                            for (j in 0 until songsArray.length()) {
                                val oObj = songsArray.optJSONObject(j) ?: continue
                                val sId = oObj.optString("id")
                                if (sId.isBlank()) continue
                                val sSrcStr = oObj.optString("source", source.name)
                                val sSource = runCatching { AudioSource.valueOf(sSrcStr) }.getOrDefault(source)
                                add(
                                    OnlineSong(
                                        id = sId,
                                        title = oObj.optString("title"),
                                        artist = oObj.optString("artist"),
                                        album = oObj.optString("album"),
                                        artworkUrl = oObj.optString("artworkUrl"),
                                        durationSeconds = oObj.optLong("durationSeconds", 0L),
                                        source = sSource,
                                        streamUrl = "", // NEVER store expiring stream URLs
                                        bitrate = oObj.optString("bitrate", "320 kbps"),
                                        format = oObj.optString("format", "AAC/MP4")
                                    )
                                )
                            }
                        }
                    }
                    val rawSongCount = item.optInt("songCount", 0)
                    val finalSongCount = if (rawSongCount > 0) rawSongCount else songs.size
                    add(
                        OnlinePlaylist(
                            id = id,
                            title = item.optString("title"),
                            subtitle = item.optString("subtitle"),
                            artworkUrl = item.optString("artworkUrl"),
                            songCount = finalSongCount,
                            source = source,
                            language = item.optString("language"),
                            songs = songs
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<OnlinePlaylist>.encodeSavedOnlinePlaylists(): String {
    val array = JSONArray()
    forEach { pl ->
        val songsArray = JSONArray()
        pl.songs.take(100).forEach { os ->
            val sObj = JSONObject()
                .put("id", os.id)
                .put("title", os.title)
                .put("artist", os.artist)
                .put("album", os.album)
                .put("artworkUrl", os.artworkUrl)
                .put("durationSeconds", os.durationSeconds)
                .put("source", os.source.name)
                .put("bitrate", os.bitrate)
                .put("format", os.format)
            songsArray.put(sObj)
        }
        array.put(
            JSONObject()
                .put("id", pl.id)
                .put("title", pl.title)
                .put("subtitle", pl.subtitle)
                .put("artworkUrl", pl.artworkUrl)
                .put("songCount", if (pl.songCount > 0) pl.songCount else pl.songs.size)
                .put("source", pl.source.name)
                .put("language", pl.language)
                .put("songs", songsArray)
        )
    }
    return array.toString()
}

private fun String?.decodeSavedOnlineArtists(): List<SavedOnlineArtist> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    if (id.isBlank() || name.isBlank()) continue
                    val srcStr = item.optString("source", AudioSource.YOUTUBE.name)
                    val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.YOUTUBE)
                    val profileImageUrl = item.optString("profileImageUrl").takeIf { it.isNotBlank() }
                    val audienceText = item.optString("audienceText").takeIf { it.isNotBlank() }
                    add(
                        SavedOnlineArtist(
                            id = id,
                            name = name,
                            profileImageUrl = profileImageUrl,
                            source = source,
                            audienceText = audienceText
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<SavedOnlineArtist>.encodeSavedOnlineArtists(): String {
    val array = JSONArray()
    forEach { artist ->
        array.put(
            JSONObject()
                .put("id", artist.id)
                .put("name", artist.name)
                .put("profileImageUrl", artist.profileImageUrl.orEmpty())
                .put("source", artist.source.name)
                .put("audienceText", artist.audienceText.orEmpty())
        )
    }
    return array.toString()
}

private fun String?.decodeOnlineSongs(): List<OnlineSong> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    val oObj = array.optJSONObject(i) ?: continue
                    val id = oObj.optString("id")
                    if (id.isBlank()) continue
                    val srcStr = oObj.optString("source", AudioSource.YOUTUBE.name)
                    val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.YOUTUBE)
                    add(
                        OnlineSong(
                            id = id,
                            title = oObj.optString("title"),
                            artist = oObj.optString("artist"),
                            album = oObj.optString("album"),
                            artworkUrl = oObj.optString("artworkUrl"),
                            durationSeconds = oObj.optLong("durationSeconds", 0L),
                            source = source,
                            streamUrl = "", // NEVER store expiring stream URLs
                            bitrate = oObj.optString("bitrate", "320 kbps"),
                            format = oObj.optString("format", "AAC/MP4")
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun List<OnlineSong>.encodeOnlineSongs(): String {
    val array = JSONArray()
    forEach { os ->
        val obj = JSONObject()
            .put("id", os.id)
            .put("title", os.title)
            .put("artist", os.artist)
            .put("album", os.album)
            .put("artworkUrl", os.artworkUrl)
            .put("durationSeconds", os.durationSeconds)
            .put("source", os.source.name)
            .put("bitrate", os.bitrate)
            .put("format", os.format)
        array.put(obj)
    }
    return array.toString()
}


