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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.appDataStore)

    val usernameFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.username] ?: "Master"
    }.distinctUntilChanged()

    private fun isMasterThemeEnabled(prefs: Preferences): Boolean {
        val explicit = prefs[Keys.changeThemeEnabled]
        if (explicit != null) return explicit

        val legacyFullApp = prefs[Keys.enableFullAppDynamicTheme] ?: false
        val legacyPlayerColored = (prefs[Keys.dynamicArtworkColors] ?: false) ||
            prefs[Keys.playerThemeMode]?.equals("Dynamic Artwork", ignoreCase = true) == true ||
            prefs[Keys.playerThemeMode]?.equals("Custom", ignoreCase = true) == true ||
            (prefs[Keys.colorPreset] != null && !prefs[Keys.colorPreset].equals("Monochrome", ignoreCase = true))

        return legacyFullApp || legacyPlayerColored
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        val masterEnabled = isMasterThemeEnabled(prefs)

        val legacyFullApp = prefs[Keys.enableFullAppDynamicTheme] ?: false
        val legacyPlayerColored = (prefs[Keys.dynamicArtworkColors] ?: false) ||
            prefs[Keys.playerThemeMode]?.equals("Dynamic Artwork", ignoreCase = true) == true ||
            prefs[Keys.playerThemeMode]?.equals("Custom", ignoreCase = true) == true ||
            (prefs[Keys.colorPreset] != null && !prefs[Keys.colorPreset].equals("Monochrome", ignoreCase = true))

        val scope = prefs[Keys.themeScope] ?: if (legacyFullApp) "Entire App" else if (legacyPlayerColored) "Player Only" else "Entire App"

        val style = prefs[Keys.themeColorStyle] ?: prefs[Keys.appPaletteMode]?.takeIf { !it.equals("Monochrome", ignoreCase = true) }
            ?: prefs[Keys.playerThemeMode]?.takeIf { !it.equals("Monochrome", ignoreCase = true) }
            ?: "Preset"

        val preset = prefs[Keys.appColorPreset]?.takeIf { !it.equals("Monochrome", ignoreCase = true) }
            ?: prefs[Keys.colorPreset]?.takeIf { !it.equals("Monochrome", ignoreCase = true) }
            ?: "Midnight Indigo"

        val customHexes = prefs[Keys.appCustomPaletteHexes]?.takeIf { it.isNotBlank() }
            ?: prefs[Keys.customPaletteHexes]?.takeIf { it.isNotBlank() }
            ?: "#1A237E,#3949AB,#0D1642"

        AppSettings(
            karaokeMode = prefs[Keys.karaokeMode] ?: false,
            lyricFontScale = prefs[Keys.lyricFontScale] ?: 1.0f,
            crossfadeEnabled = prefs[Keys.crossfadeEnabled] ?: false,
            gaplessEnabled = prefs[Keys.gaplessEnabled] ?: true,
            skipSilence = prefs[Keys.skipSilence] ?: false,
            smartAudioFocus = prefs[Keys.smartAudioFocus] ?: true,
            keepPlayingOnClose = prefs[Keys.keepPlayingOnClose] ?: true,
            playlistGridColumns = prefs[Keys.playlistGridColumns] ?: 2,
            dynamicArtworkColors = masterEnabled && style.equals("Dynamic Artwork", ignoreCase = true),
            colorPreset = preset,
            customPaletteHexes = customHexes,
            playerThemeMode = style,
            enablePlayerColor = prefs[Keys.enablePlayerColor] ?: true,
            enableMiniPlayerColor = prefs[Keys.enableMiniPlayerColor] ?: false,
            enableFullAppDynamicTheme = masterEnabled && scope.equals("Entire App", ignoreCase = true),
            appPaletteMode = style,
            appColorPreset = preset,
            appCustomPaletteHexes = customHexes,
            themeHomeEnabled = prefs[Keys.themeHomeEnabled] ?: true,
            themeExploreEnabled = prefs[Keys.themeExploreEnabled] ?: true,
            themeLibraryEnabled = prefs[Keys.themeLibraryEnabled] ?: true,
            themeAlbumDetailEnabled = prefs[Keys.themeAlbumDetailEnabled] ?: true,
            themeArtistDetailEnabled = prefs[Keys.themeArtistDetailEnabled] ?: true,
            themePlaylistDetailEnabled = prefs[Keys.themePlaylistDetailEnabled] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            changeThemeEnabled = masterEnabled,
            themeScope = scope,
            themeColorStyle = style
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

    suspend fun getKeepPlayingOnClose(): Boolean {
        return settingsFlow.first().keepPlayingOnClose
    }

    suspend fun setPlaylistGridColumns(columns: Int) {
        dataStore.edit { it[Keys.playlistGridColumns] = columns.coerceIn(1, 2) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setChangeThemeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.changeThemeEnabled] = enabled
            val scope = prefs[Keys.themeScope] ?: "Entire App"
            val style = prefs[Keys.themeColorStyle] ?: "Preset"
            prefs[Keys.enableFullAppDynamicTheme] = enabled && scope.equals("Entire App", ignoreCase = true)
            prefs[Keys.dynamicArtworkColors] = enabled && style.equals("Dynamic Artwork", ignoreCase = true)
        }
    }

    suspend fun setThemeScope(scope: String) {
        dataStore.edit { prefs ->
            val master = isMasterThemeEnabled(prefs)
            prefs[Keys.themeScope] = scope
            prefs[Keys.enableFullAppDynamicTheme] = master && scope.equals("Entire App", ignoreCase = true)
            if (prefs[Keys.changeThemeEnabled] == null) {
                prefs[Keys.changeThemeEnabled] = master
            }
        }
    }

    suspend fun setThemeColorStyle(style: String) {
        dataStore.edit { prefs ->
            val cleanStyle = if (style.equals("Monochrome", ignoreCase = true)) "Preset" else style
            prefs[Keys.themeColorStyle] = cleanStyle
            prefs[Keys.appPaletteMode] = cleanStyle
            prefs[Keys.playerThemeMode] = cleanStyle
            val master = isMasterThemeEnabled(prefs)
            prefs[Keys.dynamicArtworkColors] = master && cleanStyle.equals("Dynamic Artwork", ignoreCase = true)
            if (prefs[Keys.changeThemeEnabled] == null) {
                prefs[Keys.changeThemeEnabled] = master
            }
        }
    }

    suspend fun setThemePreset(preset: String) {
        val cleanPreset = if (preset.equals("Monochrome", ignoreCase = true)) "Midnight Indigo" else preset
        dataStore.edit { prefs ->
            prefs[Keys.appColorPreset] = cleanPreset
            prefs[Keys.colorPreset] = cleanPreset
        }
    }

    suspend fun setThemeCustomHexes(hexes: String) {
        dataStore.edit { prefs ->
            prefs[Keys.appCustomPaletteHexes] = hexes
            prefs[Keys.customPaletteHexes] = hexes
        }
    }

    suspend fun setPlayerThemeMode(mode: String) {
        setThemeColorStyle(mode)
    }

    suspend fun setDynamicArtworkColors(enabled: Boolean) {
        if (enabled) {
            setChangeThemeEnabled(true)
            setThemeColorStyle("Dynamic Artwork")
        } else {
            setThemeColorStyle("Preset")
        }
    }

    suspend fun setColorPreset(preset: String) {
        setThemePreset(preset)
    }

    suspend fun setCustomPaletteHexes(hexes: String) {
        setThemeCustomHexes(hexes)
    }

    suspend fun setSectionColorEnabled(player: Boolean, miniPlayer: Boolean) {
        dataStore.edit {
            it[Keys.enablePlayerColor] = player
            it[Keys.enableMiniPlayerColor] = miniPlayer
        }
    }

    suspend fun setEnableFullAppDynamicTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            val master = isMasterThemeEnabled(prefs)
            prefs[Keys.enableFullAppDynamicTheme] = master && enabled
            prefs[Keys.themeScope] = if (enabled) "Entire App" else "Player Only"
            if (prefs[Keys.changeThemeEnabled] == null) {
                prefs[Keys.changeThemeEnabled] = master
            }
        }
    }

    suspend fun setAppPaletteMode(mode: String) {
        setThemeColorStyle(mode)
    }

    suspend fun setAppColorPreset(preset: String) {
        setThemePreset(preset)
    }

    suspend fun setAppCustomPaletteHexes(hexes: String) {
        setThemeCustomHexes(hexes)
    }

    suspend fun setThemeAreaEnabled(areaName: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            when (areaName.uppercase()) {
                "HOME" -> prefs[Keys.themeHomeEnabled] = enabled
                "EXPLORE", "SEARCH" -> prefs[Keys.themeExploreEnabled] = enabled
                "LIBRARY" -> prefs[Keys.themeLibraryEnabled] = enabled
                "ALBUM_DETAIL" -> prefs[Keys.themeAlbumDetailEnabled] = enabled
                "ARTIST_DETAIL" -> prefs[Keys.themeArtistDetailEnabled] = enabled
                "PLAYLIST_DETAIL" -> prefs[Keys.themePlaylistDetailEnabled] = enabled
            }
        }
    }

    suspend fun setColorPreset(preset: String) {
        dataStore.edit { it[Keys.colorPreset] = preset }
    }

    suspend fun setCustomPaletteHexes(hexes: String) {
        dataStore.edit { it[Keys.customPaletteHexes] = hexes }
    }

    suspend fun setSectionColorEnabled(player: Boolean, miniPlayer: Boolean) {
        dataStore.edit {
            it[Keys.enablePlayerColor] = player
            it[Keys.enableMiniPlayerColor] = miniPlayer
        }
    }

    suspend fun setFavorites(items: List<Pair<Long, OnlineSong?>>, isFavorite: Boolean) {
        if (items.isEmpty()) return
        dataStore.edit { prefs ->
            val ids = prefs[Keys.favoriteIds].decodeLongSet().toMutableSet()
            val onlineList = prefs[Keys.favoriteOnlineSongs].decodeOnlineSongs().toMutableList()

            val songsToRemove = mutableSetOf<Long>()
            val onlineIdsToRemove = mutableSetOf<String>()
            val onlineSongsToAdd = mutableListOf<OnlineSong>()

            for ((songId, onlineSong) in items) {
                if (isFavorite) {
                    ids.add(songId)
                    if (onlineSong != null) {
                        songsToRemove.add(songId)
                        onlineIdsToRemove.add(onlineSong.id)
                        onlineSongsToAdd.add(onlineSong.copy(streamUrl = ""))
                    }
                } else {
                    ids.remove(songId)
                    songsToRemove.add(songId)
                    if (onlineSong != null) {
                        onlineIdsToRemove.add(onlineSong.id)
                    }
                }
            }

            onlineList.removeAll {
                onlineIdsToRemove.contains(it.id) ||
                songsToRemove.contains((it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L))
            }

            if (onlineSongsToAdd.isNotEmpty()) {
                val distinctToAdd = onlineSongsToAdd.distinctBy { it.id }
                onlineList.addAll(0, distinctToAdd)
            }

            prefs[Keys.favoriteIds] = ids.encodeLongSet()
            prefs[Keys.favoriteOnlineSongs] = onlineList.encodeOnlineSongs()
        }
    }

    suspend fun setFavorite(songId: Long, isFavorite: Boolean, onlineSong: OnlineSong? = null) {
        setFavorites(listOf(songId to onlineSong), isFavorite)
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
            val karaokeMode = booleanPreferencesKey("karaoke_mode")
            val lyricFontScale = floatPreferencesKey("lyric_font_scale")
            val crossfadeEnabled = booleanPreferencesKey("crossfade_enabled")
            val gaplessEnabled = booleanPreferencesKey("gapless_enabled")
            val skipSilence = booleanPreferencesKey("skip_silence")
            val smartAudioFocus = booleanPreferencesKey("smart_audio_focus")
            val keepPlayingOnClose = booleanPreferencesKey("keep_playing_on_close")
            val playlistGridColumns = intPreferencesKey("playlist_grid_columns")
            val dynamicArtworkColors = booleanPreferencesKey("dynamic_artwork_colors")
            val colorPreset = stringPreferencesKey("color_preset")
            val customPaletteHexes = stringPreferencesKey("custom_palette_hexes")
            val playerThemeMode = stringPreferencesKey("player_theme_mode")
            val enablePlayerColor = booleanPreferencesKey("enable_player_color")
            val enableMiniPlayerColor = booleanPreferencesKey("enable_mini_player_color")
            val enableFullAppDynamicTheme = booleanPreferencesKey("enable_full_app_dynamic_theme")
            val appPaletteMode = stringPreferencesKey("app_palette_mode")
            val appColorPreset = stringPreferencesKey("app_color_preset")
            val appCustomPaletteHexes = stringPreferencesKey("app_custom_palette_hexes")
            val changeThemeEnabled = booleanPreferencesKey("change_theme_enabled")
            val themeScope = stringPreferencesKey("theme_scope")
            val themeColorStyle = stringPreferencesKey("theme_color_style")
            val themeHomeEnabled = booleanPreferencesKey("theme_home_enabled")
            val themeExploreEnabled = booleanPreferencesKey("theme_explore_enabled")
            val themeLibraryEnabled = booleanPreferencesKey("theme_library_enabled")
            val themeAlbumDetailEnabled = booleanPreferencesKey("theme_album_detail_enabled")
            val themeArtistDetailEnabled = booleanPreferencesKey("theme_artist_detail_enabled")
            val themePlaylistDetailEnabled = booleanPreferencesKey("theme_playlist_detail_enabled")
            val themeMode = stringPreferencesKey("theme_mode")
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


