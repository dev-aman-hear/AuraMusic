package com.aman.auramusic.data.repository

import com.aman.auramusic.data.local.PlaylistDao
import com.aman.auramusic.data.local.entity.ImportedPlaylistEntity
import com.aman.auramusic.data.model.OnlinePlaylist
import com.aman.auramusic.data.model.OnlinePlaylistDetails
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.remote.PlaylistApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlinePlaylistRepository @Inject constructor(
    private val api: PlaylistApi,
    private val dao: PlaylistDao
) {
    suspend fun getPlaylists(): List<OnlinePlaylist> {
        return api.getPlaylists()
    }

    suspend fun getPlaylistDetails(url: String): OnlinePlaylistDetails {
        return api.getPlaylistDetails(url)
    }

    suspend fun downloadAndImportPlaylist(playlist: OnlinePlaylist) {
        val details = api.getPlaylistDetails(playlist.playlistUrl)
        val entity = ImportedPlaylistEntity(
            id = playlist.id,
            name = details.name,
            description = details.description,
            artwork = details.artwork,
            songs = details.songs,
            category = playlist.category
        )
        dao.insertPlaylist(entity)
    }

    fun getImportedPlaylists(): Flow<List<ImportedPlaylistEntity>> {
        return dao.getAllImportedPlaylists()
    }

    suspend fun deleteImportedPlaylist(playlistId: String) {
        dao.getPlaylistById(playlistId)?.let {
            dao.deletePlaylist(it)
        }
    }

    /**
     * Logic to find matches between online songs and local library.
     * Match criteria: Exact Title and Artist (Case Insensitive).
     */
    fun findMatches(onlineSongs: List<com.aman.auramusic.data.model.OnlineSong>, localSongs: List<Song>): List<com.aman.auramusic.data.model.OnlineSong> {
        val localKeys = localSongs.map { "${it.title.lowercase()}|${it.artist.lowercase()}" }.toSet()
        return onlineSongs.filter { 
            "${it.title.lowercase()}|${it.artist.lowercase()}" in localKeys 
        }
    }
}
