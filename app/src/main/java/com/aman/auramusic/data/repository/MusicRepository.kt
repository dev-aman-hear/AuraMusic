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
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"


        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {

            val idColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            val albumIdColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM_ID
                )
            val titleColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            val artistColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            val albumColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            val durationColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            val mimeTypeColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            val pathColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {

                val path =
                    it.getString(pathColumn)


                val mimeType =
                    it.getString(mimeTypeColumn)


                val id = it.getLong(idColumn)

                val albumId =
                    it.getLong(albumIdColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Use the standardized album art URI
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val title = it.getString(titleColumn).orUnknown("Untitled")
                val artist = it.getString(artistColumn).orUnknown("Unknown Artist")
                val album = it.getString(albumColumn).orUnknown("Unknown Album")

                if (isCallRecording(title, artist, album, path)) {
                    continue
                }

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = it.getLong(durationColumn),
                        uri = uri.toString(),
                        filePath = path,
                        artworkUri = artworkUri,
                        mimeType = mimeType.orUnknownAudioType()
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
        val searchableText = listOfNotNull(title, artist, album, filePath)
            .joinToString(" ")
            .lowercase()
            .replace("_", " ")
            .replace("-", " ")

        val isRecorderFolder = filePath?.lowercase()?.let { path ->
            folderExclusionMarkers.any { marker -> path.contains(marker) }
        } ?: false

        return isRecorderFolder || callRecordingMarkers.any { marker ->
            searchableText.contains(marker)
        }
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
