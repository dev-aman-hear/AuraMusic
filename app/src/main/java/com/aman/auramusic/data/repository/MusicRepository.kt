package com.aman.auramusic.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import com.aman.auramusic.data.model.Song

class MusicRepository(private val context: Context) {

    fun getAllSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol) ?: ""
                val title = cursor.getString(titleCol).orUnknown("Untitled")
                val artist = cursor.getString(artistCol).orUnknown("Unknown Artist")
                val album = cursor.getString(albumCol).orUnknown("Unknown Album")

                if (isCallRecording(title, artist, album, path)) continue

                songs.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = title,
                        artist = artist,
                        album = album,
                        duration = cursor.getLong(durationCol),
                        dateAdded = cursor.getLong(dateAddedCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol)).toString(),
                        filePath = path,
                        artworkUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        cursor.getLong(albumIdCol)
                    ).toString(),
                        mimeType = cursor.getString(mimeTypeCol).orUnknownAudioType()
                    )
                )
            }
        }
        return songs
    }

    fun getSongFromUri(uri: android.net.Uri): Song? {
        if (uri.scheme == "content") {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA
            )

            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                        val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                        val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                        val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                        val id = if (idCol != -1) cursor.getLong(idCol) else -1L
                        val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L
                        val title = if (titleCol != -1) cursor.getString(titleCol) else uri.lastPathSegment ?: "Unknown"
                        val artist = if (artistCol != -1) cursor.getString(artistCol) else "Unknown Artist"
                        val album = if (albumCol != -1) cursor.getString(albumCol) else "Unknown Album"
                        val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                        val mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) else "audio/*"
                        val path = (if (pathCol != -1) cursor.getString(pathCol) else null) ?: uri.toString()

                        return Song(
                            id = id,
                            title = title.orUnknown("Untitled"),
                            artist = artist.orUnknown("Unknown Artist"),
                            album = album.orUnknown("Unknown Album"),
                            duration = duration,
                            uri = uri.toString(),
                            filePath = path,
                            artworkUri = if (albumId != -1L) {
                                ContentUris.withAppendedId(
                                    "content://media/external/audio/albumart".toUri(),
                                    albumId
                                ).toString()
                            } else null,
                            mimeType = mimeType.orUnknownAudioType()
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore and fall through to default
            }

            return Song(
                id = -1,
                title = uri.lastPathSegment ?: "External Audio",
                artist = "Unknown Artist",
                album = "Unknown Album",
                duration = 0,
                uri = uri.toString(),
                filePath = uri.toString(),
                artworkUri = null,
                mimeType = "audio/*"
            )
        } else if (uri.scheme == "file") {
            return Song(
                id = -1,
                title = uri.lastPathSegment ?: "Untitled",
                artist = "Unknown Artist",
                album = "Unknown Album",
                duration = 0,
                uri = uri.toString(),
                filePath = uri.path ?: uri.toString(),
                artworkUri = null,
                mimeType = "audio/*"
            )
        }
        return null
    }

    private fun String?.orUnknownAudioType(): String {
        return this ?: "audio/*"
    }

    private fun String?.orUnknown(fallback: String): String {
        return if ((this.isNullOrBlank()) || this == "<unknown>") fallback else this
    }

    private fun isCallRecording(
        title: String,
        artist: String,
        album: String,
        filePath: String?
    ): Boolean {
        // Quick folder check first
        if (filePath != null) {
            val lowerPath = filePath.lowercase()
            if (folderExclusionMarkers.any { lowerPath.contains(it) }) return true
        }

        // Then check searchable text
        val searchableText = (title + artist + album + (filePath ?: "")).lowercase()

        return callRecordingMarkers.any { searchableText.contains(it) }
    }

    private companion object {
        val folderExclusionMarkers = listOf(
            "/recordings/",
            "/voicerecorder/",
            "/voice recorder/",
            "/call recorder/",
            "/callrecorder/",
            "/miui/sound_recorder/",
            "/standardrecorder/",
            "/recordings/voice/"
        )

        val callRecordingMarkers = listOf(
            "call recording",
            "call recordings",
            "callrecorder",
            "phone recording",
            "recorded call"
        )
    }

}
