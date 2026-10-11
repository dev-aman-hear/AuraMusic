package com.aman.auramusic

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.SavedOnlineArtist
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedOnlineMediaTest {

    @Test
    fun testSavedOnlineArtistSerialization() {
        val artists = listOf(
            SavedOnlineArtist(
                id = "UC1234567890",
                name = "Test Artist",
                profileImageUrl = "https://example.com/artist.jpg",
                source = AudioSource.YOUTUBE
            )
        )

        val array = JSONArray()
        artists.forEach { artist ->
            array.put(
                JSONObject()
                    .put("id", artist.id)
                    .put("name", artist.name)
                    .put("profileImageUrl", artist.profileImageUrl.orEmpty())
                    .put("source", artist.source.name)
            )
        }
        val serialized = array.toString()

        // Decode
        val decodedArray = JSONArray(serialized)
        val decodedList = mutableListOf<SavedOnlineArtist>()
        for (i in 0 until decodedArray.length()) {
            val item = decodedArray.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val name = item.optString("name")
            if (id.isBlank() || name.isBlank()) continue
            val srcStr = item.optString("source", AudioSource.YOUTUBE.name)
            val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.YOUTUBE)
            val profileImageUrl = item.optString("profileImageUrl").takeIf { it.isNotBlank() }
            decodedList.add(SavedOnlineArtist(id, name, profileImageUrl, source))
        }

        assertEquals(1, decodedList.size)
        assertEquals("UC1234567890", decodedList[0].id)
        assertEquals("Test Artist", decodedList[0].name)
        assertEquals("https://example.com/artist.jpg", decodedList[0].profileImageUrl)
        assertEquals(AudioSource.YOUTUBE, decodedList[0].source)
    }

    @Test
    fun testDuplicateOnlineArtistPrevention() {
        val list = mutableListOf(
            SavedOnlineArtist(id = "UC123", name = "Artist One", source = AudioSource.YOUTUBE)
        )

        val duplicate = SavedOnlineArtist(id = "UC123", name = "Artist One Different Case", source = AudioSource.YOUTUBE)
        val exists = list.any { it.id == duplicate.id || (it.name.equals(duplicate.name, ignoreCase = true) && it.source == duplicate.source) }
        assertTrue(exists)

        val distinct = SavedOnlineArtist(id = "UC456", name = "Artist Two", source = AudioSource.YOUTUBE)
        val distinctExists = list.any { it.id == distinct.id || (it.name.equals(distinct.name, ignoreCase = true) && it.source == distinct.source) }
        assertFalse(distinctExists)
    }

    @Test
    fun testSavedOnlineAlbumSerialization() {
        val albums = listOf(
            OnlinePlaylist(
                id = "ytmusic:MPREb_12345",
                title = "Test Album",
                subtitle = "Artist One • 2026",
                artworkUrl = "https://example.com/album.jpg",
                songCount = 10,
                source = AudioSource.YOUTUBE
            )
        )

        val array = JSONArray()
        albums.forEach { pl ->
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
        val serialized = array.toString()

        val decodedArray = JSONArray(serialized)
        val decodedList = mutableListOf<OnlinePlaylist>()
        for (i in 0 until decodedArray.length()) {
            val item = decodedArray.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val srcStr = item.optString("source", AudioSource.JIOSAAVN.name)
            val source = runCatching { AudioSource.valueOf(srcStr) }.getOrDefault(AudioSource.JIOSAAVN)
            decodedList.add(
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

        assertEquals(1, decodedList.size)
        assertEquals("ytmusic:MPREb_12345", decodedList[0].id)
        assertEquals("Test Album", decodedList[0].title)
        assertEquals("Artist One • 2026", decodedList[0].subtitle)
    }
}
