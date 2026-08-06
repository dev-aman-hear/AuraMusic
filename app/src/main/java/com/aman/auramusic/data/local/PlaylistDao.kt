package com.aman.auramusic.data.local

import androidx.room.*
import com.aman.auramusic.data.local.entity.ImportedPlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM imported_playlists ORDER BY importedAt DESC")
    fun getAllImportedPlaylists(): Flow<List<ImportedPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: ImportedPlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: ImportedPlaylistEntity)

    @Query("SELECT * FROM imported_playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): ImportedPlaylistEntity?
}
