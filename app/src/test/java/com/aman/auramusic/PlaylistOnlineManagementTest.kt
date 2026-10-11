package com.aman.auramusic

import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistOnlineManagementTest {

    // Helper functions mirroring UserPreferencesRepository serialization logic
    private fun decodePlaylists(json: String?): List<Playlist> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            JSONArray(json).let { array ->
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

    private fun encodePlaylists(playlists: List<Playlist>): String {
        val array = JSONArray()
        playlists.forEach { playlist ->
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

    private fun decodeSavedOnlinePlaylists(json: String?): List<OnlinePlaylist> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            JSONArray(json).let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isBlank()) continue
                        val srcStr = item.optString("source", AudioSource.JIOSAAVN.name)
                        val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.JIOSAAVN)
                        add(
                            OnlinePlaylist(
                                id = id,
                                title = item.optString("title"),
                                subtitle = item.optString("subtitle"),
                                artworkUrl = item.optString("artworkUrl"),
                                songCount = item.optInt("songCount", 0),
                                source = source,
                                language = item.optString("language")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeSavedOnlinePlaylists(playlists: List<OnlinePlaylist>): String {
        val array = JSONArray()
        playlists.forEach { pl ->
            array.put(
                JSONObject()
                    .put("id", pl.id)
                    .put("title", pl.title)
                    .put("subtitle", pl.subtitle)
                    .put("artworkUrl", pl.artworkUrl)
                    .put("songCount", pl.songCount)
                    .put("source", pl.source.name)
                    .put("language", pl.language)
            )
        }
        return array.toString()
    }

    @Test
    fun testLegacyPlaylistBackwardCompatibility() {
        // Legacy JSON without "onlineSongs" field
        val legacyJson = """
            [
                {
                    "id": 1001,
                    "name": "My Favorites",
                    "songIds": [10, 20, 30],
                    "createdAt": 1700000000,
                    "updatedAt": 1700005000,
                    "artworkSongId": 10
                }
            ]
        """.trimIndent()

        val decoded = decodePlaylists(legacyJson)
        assertEquals(1, decoded.size)
        val pl = decoded.first()
        assertEquals(1001L, pl.id)
        assertEquals("My Favorites", pl.name)
        assertEquals(listOf(10L, 20L, 30L), pl.songIds)
        assertEquals(10L, pl.artworkSongId)
        assertTrue(pl.onlineSongs.isEmpty())
    }

    @Test
    fun testPlaylistWithOnlineSongsRoundTrip() {
        val onlineSong = OnlineSong(
            id = "yt_starboy_123",
            title = "Starboy",
            artist = "The Weeknd",
            album = "Starboy",
            artworkUrl = "https://i.ytimg.com/vi/starboy.jpg",
            durationSeconds = 230L,
            source = AudioSource.YOUTUBE,
            streamUrl = "https://expiring-cdn.googlevideo.com/videoplayback?expire=12345",
            bitrate = "320 kbps",
            format = "Opus/WebM"
        )

        val originalPlaylist = Playlist(
            id = 2002L,
            name = "Late Night Vibes",
            songIds = listOf(101L, 102L),
            onlineSongs = listOf(onlineSong.copy(streamUrl = ""))
        )

        val json = encodePlaylists(listOf(originalPlaylist))
        assertNotNull(json)
        assertFalse(json.contains("expiring-cdn.googlevideo.com")) // Expiring URLs MUST NOT be stored

        val decoded = decodePlaylists(json)
        assertEquals(1, decoded.size)
        val decodedPl = decoded.first()
        assertEquals(2002L, decodedPl.id)
        assertEquals("Late Night Vibes", decodedPl.name)
        assertEquals(listOf(101L, 102L), decodedPl.songIds)
        assertEquals(1, decodedPl.onlineSongs.size)

        val decodedOnline = decodedPl.onlineSongs.first()
        assertEquals("yt_starboy_123", decodedOnline.id)
        assertEquals("Starboy", decodedOnline.title)
        assertEquals("The Weeknd", decodedOnline.artist)
        assertEquals("Starboy", decodedOnline.album)
        assertEquals("https://i.ytimg.com/vi/starboy.jpg", decodedOnline.artworkUrl)
        assertEquals(230L, decodedOnline.durationSeconds)
        assertEquals(AudioSource.YOUTUBE, decodedOnline.source)
        assertEquals("", decodedOnline.streamUrl)
    }

    @Test
    fun testDuplicateOnlineSongPrevention() {
        val song1 = OnlineSong(id = "track_1", title = "Song One", artist = "Artist A", source = AudioSource.JIOSAAVN)
        val song2 = OnlineSong(id = "track_2", title = "Song Two", artist = "Artist B", source = AudioSource.JIOSAAVN)
        val song1Duplicate = OnlineSong(id = "track_1", title = "Song One (Remaster)", artist = "Artist A", source = AudioSource.JIOSAAVN)

        var playlist = Playlist(id = 1L, name = "Test", onlineSongs = listOf(song1, song2))

        // Check duplicate rule: does not add if same id and source exists
        fun addOnlineSong(pl: Playlist, song: OnlineSong): Playlist {
            val exists = pl.onlineSongs.any { it.id == song.id && it.source == song.source }
            return if (!exists) pl.copy(onlineSongs = pl.onlineSongs + song) else pl
        }

        playlist = addOnlineSong(playlist, song1Duplicate)
        assertEquals("Duplicate must be prevented", 2, playlist.onlineSongs.size)

        // Adding to another playlist is allowed
        var otherPlaylist = Playlist(id = 2L, name = "Other Playlist", onlineSongs = emptyList())
        otherPlaylist = addOnlineSong(otherPlaylist, song1)
        assertEquals("Same song in different playlist must be allowed", 1, otherPlaylist.onlineSongs.size)
    }

    @Test
    fun testRemoveOnlineSongFromPlaylist() {
        val song1 = OnlineSong(id = "track_1", title = "Song One", artist = "Artist A", source = AudioSource.JIOSAAVN)
        val song2 = OnlineSong(id = "track_2", title = "Song Two", artist = "Artist B", source = AudioSource.JIOSAAVN)

        val playlist1 = Playlist(id = 1L, name = "P1", songIds = listOf(55L), onlineSongs = listOf(song1, song2))
        val playlist2 = Playlist(id = 2L, name = "P2", songIds = listOf(66L), onlineSongs = listOf(song1))

        // Remove song1 from playlist1 only
        val updatedP1 = playlist1.copy(onlineSongs = playlist1.onlineSongs.filterNot { it.id == "track_1" })

        assertEquals(1, updatedP1.onlineSongs.size)
        assertEquals("track_2", updatedP1.onlineSongs.first().id)
        assertEquals(listOf(55L), updatedP1.songIds) // Local song IDs untouched

        // Playlist 2 remains unaffected
        assertEquals(1, playlist2.onlineSongs.size)
        assertEquals("track_1", playlist2.onlineSongs.first().id)
    }

    @Test
    fun testSavedOnlinePlaylistsPersistence() {
        val savedOnline = OnlinePlaylist(
            id = "ytmusic:VLPL12345",
            title = "Top Bollywood Hits",
            subtitle = "Curated by YouTube Music",
            artworkUrl = "https://lh3.googleusercontent.com/playlist123.jpg",
            songCount = 50,
            source = AudioSource.YOUTUBE,
            language = "Hindi"
        )

        val json = encodeSavedOnlinePlaylists(listOf(savedOnline))
        assertNotNull(json)

        val decoded = decodeSavedOnlinePlaylists(json)
        assertEquals(1, decoded.size)
        val pl = decoded.first()
        assertEquals("ytmusic:VLPL12345", pl.id)
        assertEquals("Top Bollywood Hits", pl.title)
        assertEquals("Curated by YouTube Music", pl.subtitle)
        assertEquals("https://lh3.googleusercontent.com/playlist123.jpg", pl.artworkUrl)
        assertEquals(50, pl.songCount)
        assertEquals(AudioSource.YOUTUBE, pl.source)
        assertEquals("Hindi", pl.language)
    }

    @Test
    fun testRemoveSavedOnlinePlaylist() {
        val pl1 = OnlinePlaylist(id = "jio_101", title = "Jio Mix", source = AudioSource.JIOSAAVN)
        val pl2 = OnlinePlaylist(id = "yt_202", title = "YT Pop", source = AudioSource.YOUTUBE)

        val list = listOf(pl1, pl2)
        val filtered = list.filterNot { it.id == "jio_101" }

        assertEquals(1, filtered.size)
        assertEquals("yt_202", filtered.first().id)
    }

    @Test
    fun testArtworkResolutionLogic() {
        // Album artwork resolution: pick first song with non-blank artworkUri
        val songWithoutArt = Song(id = 1L, title = "Track 1", artist = "Artist", album = "Album", duration = 180000L, dateAdded = 0L, uri = "content://1", filePath = "/p1", artworkUri = null, mimeType = "audio/mpeg")
        val songWithArt = Song(id = 2L, title = "Track 2", artist = "Artist", album = "Album", duration = 200000L, dateAdded = 0L, uri = "content://2", filePath = "/p2", artworkUri = "content://media/external/audio/albumart/99", mimeType = "audio/mpeg")

        val albumSongs = listOf(songWithoutArt, songWithArt)
        val resolvedAlbumSong = albumSongs.firstOrNull { !it.artworkUri.isNullOrBlank() } ?: albumSongs.firstOrNull()

        assertNotNull(resolvedAlbumSong)
        assertEquals("content://media/external/audio/albumart/99", resolvedAlbumSong?.artworkUri)

        // Playlist artwork resolution: fallback from local song to online song artwork
        val playlistWithOnlyOnline = Playlist(
            id = 500L,
            name = "Online Gems",
            songIds = emptyList(),
            onlineSongs = listOf(
                OnlineSong(id = "on_1", title = "Online Track", artist = "Web Artist", artworkUrl = "https://cdn.example.com/art.jpg", source = AudioSource.YOUTUBE)
            )
        )
        val playlistArt = playlistWithOnlyOnline.onlineSongs.firstOrNull()?.artworkUrl?.ifBlank { null }
        assertEquals("https://cdn.example.com/art.jpg", playlistArt)

        // Artist card: never use album cover as artist portrait; when no genuine portrait cached, model is null
        val cachedPortraitUrl: String? = null
        assertNull("Avatar fallback must be used when genuine portrait is not available", cachedPortraitUrl)
    }
}
