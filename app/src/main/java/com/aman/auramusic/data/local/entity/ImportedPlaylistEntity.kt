package com.aman.auramusic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aman.auramusic.data.model.OnlineSong

@Entity(tableName = "imported_playlists")
data class ImportedPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val artwork: String,
    val songs: List<OnlineSong>,
    val category: String,
    val importedAt: Long = System.currentTimeMillis()
)
