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

    suspend fun setFavorite(songId: Long, isFavorite: Boolean) {
        dataStore.edit { prefs ->
            val ids = prefs[Keys.favoriteIds].decodeLongSet().toMutableSet()
            if (isFavorite) ids.add(songId) else ids.remove(songId)
            prefs[Keys.favoriteIds] = ids.encodeLongSet()
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

    suspend fun savePlaylist(
        name: String,
        songIds: List<Long>,
        artworkSongId: Long? = songIds.firstOrNull()
    ): Playlist {
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
