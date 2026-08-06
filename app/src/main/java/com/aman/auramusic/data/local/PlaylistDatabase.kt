package com.aman.auramusic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aman.auramusic.data.local.entity.ImportedPlaylistEntity

@Database(entities = [ImportedPlaylistEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
