package com.aman.auramusic.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

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
                        artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albumIdCol)).toString(),
                        mimeType = cursor.getString(mimeTypeCol).orUnknownAudioType()
                    )
                )
            }
        }
        return songs
    }

    private fun String?.orUnknownAudioType(): String {
        return this ?: "audio/*"
    }

    private fun String?.orUnknown(fallback: String): String {
        return if (this.isNullOrBlank() || this == "<unknown>") fallback else this
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
            "callrecording",
            "callrecordings",
            "call recorder",
            "callrecorder",
            "phone recording",
            "voice call",
            "recorded call",
            "recordings/call",
            "recordings\\call"
        )
    }

}
