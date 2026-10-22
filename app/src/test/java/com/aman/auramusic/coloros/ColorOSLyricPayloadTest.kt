package com.aman.auramusic.coloros

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorOSLyricPayloadTest {

    private fun song() = Song(
        id = 42L,
        title = "Example Song",
        artist = "Example Artist",
        album = "Example Album",
        duration = 180_000L,
        uri = "content://media/external/audio/media/42",
        filePath = "/not-used/example.mp3",
        artworkUri = null,
        mimeType = "audio/mpeg",
        onlineId = null
    )

    @Test
    fun payloadContainsNativeColorOsFieldsAndTimedLrc() {
        val json = JSONObject(
            ColorOSLyricPayload.generateLyricInfoJson(
                song(),
                listOf(
                    LyricLine(14_500L, "Second line"),
                    LyricLine(10_000L, "First line")
                )
            )
        )

        assertEquals("Example Song", json.getString("songName"))
        assertEquals("Example Artist", json.getString("artist"))
        assertEquals("42", json.getString("songId"))
        assertEquals(0, json.getInt("lyricType"))
        assertFalse(json.getBoolean("noLyric"))
        assertTrue(json.getString("lyric").startsWith("[00:10.000]First line"))
        assertTrue(json.getString("lyric").contains("[00:14.500]Second line"))
        assertTrue(json.getString("trackKey").contains("|example song|example artist|180"))
        assertFalse("Line-timed LRC must not be mislabeled as word-timed rawLyric", json.has("rawLyric"))
    }

    @Test
    fun emptyOrUntimedLyricsProduceNoPayload() {
        assertEquals("", ColorOSLyricPayload.generateLyricInfoJson(song(), emptyList()))
        assertEquals(
            "",
            ColorOSLyricPayload.generateLyricInfoJson(song(), listOf(LyricLine(-1L, "Invalid timestamp")))
        )
    }
}
