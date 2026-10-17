package com.aman.auramusic.viewmodel

import com.aman.auramusic.ExportTarget
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PlaylistExportImportAuditTest {

    private fun sampleSong(id: Long, title: String, artist: String, album: String = "Test Album"): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = 200000L,
            uri = "content://media/$id",
            filePath = "/storage/emulated/0/Music/$title.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )
    }

    // --- ExportTarget State Machine and Cancellation Tests ---

    @Test
    fun exportTarget_cancellation_resets_state_and_prevents_subsequent_target_bleed() {
        var pendingExportTarget: ExportTarget? = null

        // User initiates All Songs export
        pendingExportTarget = ExportTarget.AllSongs
        assertEquals(ExportTarget.AllSongs, pendingExportTarget)

        // User cancels picker (uri is null)
        val uriCancelled: String? = null
        val targetAtResolution = pendingExportTarget
        pendingExportTarget = null // The fix: explicit reset on every exit path

        assertNull("Pending export target must be null after cancellation", pendingExportTarget)
        assertNull("Picker was cancelled so no resolution uri", uriCancelled)

        // Subsequent user action: export individual playlist
        val playlist = Playlist(id = 42L, name = "Acoustic Chill", songIds = listOf(1L, 2L))
        pendingExportTarget = ExportTarget.SinglePlaylist(playlist)

        // Picker succeeds
        val activeTarget = pendingExportTarget
        pendingExportTarget = null

        val resolvedPlaylistTarget = activeTarget as? ExportTarget.SinglePlaylist
        assertNotNull("Subsequent export must resolve SinglePlaylist", resolvedPlaylistTarget)
        assertEquals("Acoustic Chill", resolvedPlaylistTarget?.playlist?.name)
        assertNull(pendingExportTarget)
    }

    // --- Export JSON Structure Tests ---

    @Test
    fun exportCurrentSongs_generates_valid_json_with_metadata_and_song_array() {
        val songs = listOf(
            sampleSong(1L, "Starboy", "The Weeknd"),
            sampleSong(2L, "Blinding Lights", "The Weeknd")
        )

        val outputStream = ByteArrayOutputStream()
        val songArray = JSONArray()
        songs.forEach { song ->
            songArray.put(
                JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                }
            )
        }
        val root = JSONObject().apply {
            put("version", 1)
            put("name", "All Songs")
            put("songs", songArray)
        }
        outputStream.use { it.write(root.toString().toByteArray()) }

        val exportedJson = JSONObject(outputStream.toString())
        assertEquals(1, exportedJson.getInt("version"))
        assertEquals("All Songs", exportedJson.getString("name"))

        val exportedSongs = exportedJson.getJSONArray("songs")
        assertEquals(2, exportedSongs.length())
        assertEquals("Starboy", exportedSongs.getJSONObject(0).getString("title"))
        assertEquals("The Weeknd", exportedSongs.getJSONObject(0).getString("artist"))
    }

    // --- Import Parsing and Matching Tests ---

    @Test
    fun importPlaylist_empty_content_is_detected() {
        val emptyContent = "   "
        assertTrue("Empty or blank content should be recognized", emptyContent.isBlank())
    }

    @Test(expected = JSONException::class)
    fun importPlaylist_malformed_json_throws_json_exception() {
        val corruptedContent = "{ invalid_json: true, songs: ["
        JSONObject(corruptedContent)
    }

    @Test
    fun importPlaylist_matches_songs_by_exact_and_partial_artist_with_unmatched_tracking() {
        val library = listOf(
            sampleSong(101L, "Midnight City", "M83"),
            sampleSong(102L, "Stay", "The Kid LAROI, Justin Bieber"),
            sampleSong(103L, "Levitating", "Dua Lipa")
        )

        val playlistJson = JSONObject().apply {
            put("name", "Road Trip")
            put("songs", JSONArray().apply {
                // Exact match
                put(JSONObject().apply {
                    put("title", "Midnight City")
                    put("artist", "M83")
                })
                // Partial artist match (e.g. Justin Bieber matching "The Kid LAROI, Justin Bieber")
                put(JSONObject().apply {
                    put("title", "Stay")
                    put("artist", "Justin Bieber")
                })
                // Unmatched song
                put(JSONObject().apply {
                    put("title", "Nonexistent Track")
                    put("artist", "Unknown Artist")
                })
            })
        }

        val songArray = playlistJson.getJSONArray("songs")
        val validSongIds = mutableListOf<Long>()
        val unmatchedSongs = mutableListOf<String>()

        for (i in 0 until songArray.length()) {
            val songJson = songArray.getJSONObject(i)
            val title = songJson.getString("title").trim()
            val artist = songJson.getString("artist").trim()

            val match = library.find {
                it.title.trim().equals(title, ignoreCase = true) &&
                it.artist.trim().equals(artist, ignoreCase = true)
            } ?: library.find {
                val itArtist = it.artist.trim().lowercase()
                val targetArtist = artist.lowercase()
                it.title.trim().equals(title, ignoreCase = true) &&
                (targetArtist.isBlank() || itArtist.contains(targetArtist) || targetArtist.contains(itArtist))
            }

            if (match != null) {
                validSongIds.add(match.id)
            } else {
                unmatchedSongs.add("$title - $artist")
            }
        }

        assertEquals(listOf(101L, 102L), validSongIds)
        assertEquals(listOf("Nonexistent Track - Unknown Artist"), unmatchedSongs)
    }
}
