package com.aman.auramusic

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LastPlayedStoreSerializationTest {

    private val gson = Gson()

    @Test
    fun testOnlineSongJsonRoundTrip() {
        val original = OnlineSong(
            id = "yt_12345",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            artworkUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg",
            durationSeconds = 230L,
            streamUrl = "https://www.youtube.com/watch?v=34Na4j8AVgA",
            source = AudioSource.YOUTUBE,
            bitrate = "Official Stream",
            format = "IFrame Audio",
            extractorTier = "RiPlay Headless Player"
        )

        val json = gson.toJson(original)
        assertNotNull(json)

        val deserialized = gson.fromJson(json, OnlineSong::class.java)
        assertEquals(original.id, deserialized.id)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.artist, deserialized.artist)
        assertEquals(original.album, deserialized.album)
        assertEquals(original.artworkUrl, deserialized.artworkUrl)
        assertEquals(original.durationSeconds, deserialized.durationSeconds)
        assertEquals(original.source, deserialized.source)
        assertEquals(original.bitrate, deserialized.bitrate)
        assertEquals(original.extractorTier, deserialized.extractorTier)
    }

    @Test
    fun testOnlineQueueJsonRoundTrip() {
        val song1 = OnlineSong(
            id = "saavn_1",
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            artworkUrl = "https://c.saavncdn.com/150/Kesariya.jpg",
            durationSeconds = 268L,
            streamUrl = "https://aac.saavncdn.com/123/stream.mp4",
            source = AudioSource.JIOSAAVN
        )

        val song2 = OnlineSong(
            id = "yt_2",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            artworkUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            durationSeconds = 200L,
            streamUrl = "https://www.youtube.com/watch?v=4NRXx6U8ABQ",
            source = AudioSource.YOUTUBE
        )

        val queue = listOf(song1, song2)
        val json = gson.toJson(queue)

        val type = object : TypeToken<List<OnlineSong>>() {}.type
        val restoredQueue: List<OnlineSong> = gson.fromJson(json, type)

        assertEquals(2, restoredQueue.size)
        assertEquals("saavn_1", restoredQueue[0].id)
        assertEquals(AudioSource.JIOSAAVN, restoredQueue[0].source)
        assertEquals("yt_2", restoredQueue[1].id)
        assertEquals(AudioSource.YOUTUBE, restoredQueue[1].source)
    }

    @Test
    fun testOnlineSongToSongConversion() {
        val onlineSong = OnlineSong(
            id = "yt_abc123",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            album = "Short n' Sweet",
            artworkUrl = "https://i.ytimg.com/vi/espresso/hqdefault.jpg",
            durationSeconds = 175L
        )

        val song = onlineSong.toSong()
        assertEquals("Espresso", song.title)
        assertEquals("Sabrina Carpenter", song.artist)
        assertEquals("Short n' Sweet", song.album)
        assertEquals("https://i.ytimg.com/vi/espresso/hqdefault.jpg", song.artworkUri)
        assertEquals(175000L, song.duration)
    }
}
