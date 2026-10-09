package com.aman.auramusic

import com.aman.auramusic.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenLogicTest {

    private val sampleSongs = listOf(
        Song(
            id = 1L,
            title = "Attention",
            artist = "Charlie Puth",
            album = "Voicenotes",
            duration = 210000L,
            uri = "content://media/1",
            filePath = "/music/attention.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        ),
        Song(
            id = 2L,
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            duration = 268000L,
            uri = "content://media/2",
            filePath = "/music/kesariya.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        ),
        Song(
            id = 3L,
            title = "Starboy",
            artist = "The Weeknd",
            album = "Starboy",
            duration = 230000L,
            uri = "content://media/3",
            filePath = "/music/starboy.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        ),
        Song(
            id = 4L,
            title = "Softly",
            artist = "Karan Aujla",
            album = "Making Memories",
            duration = 195000L,
            uri = "content://media/4",
            filePath = "/music/softly.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )
    )

    @Test
    fun testLocalSongMatchingByTitle() {
        val query = "atten"
        val q = query.trim().lowercase()
        val matched = sampleSongs.filter { song ->
            song.title.lowercase().contains(q) ||
            song.artist.lowercase().contains(q) ||
            song.album.lowercase().contains(q)
        }
        assertEquals(1, matched.size)
        assertEquals("Attention", matched[0].title)
    }

    @Test
    fun testLocalSongMatchingByArtist() {
        val query = "arijit"
        val q = query.trim().lowercase()
        val matched = sampleSongs.filter { song ->
            song.title.lowercase().contains(q) ||
            song.artist.lowercase().contains(q) ||
            song.album.lowercase().contains(q)
        }
        assertEquals(1, matched.size)
        assertEquals("Kesariya", matched[0].title)
    }

    @Test
    fun testDistinctArtistsExtraction() {
        val query = "a"
        val matchedArtists = sampleSongs.map { it.artist }.distinct().filter { it.contains(query, ignoreCase = true) }
        assertTrue(matchedArtists.contains("Charlie Puth"))
        assertTrue(matchedArtists.contains("Arijit Singh"))
        assertTrue(matchedArtists.contains("Karan Aujla"))
    }

    @Test
    fun testRecentSearchSerialization() {
        val queries = listOf("Arijit Singh", "Attention", "Pop Hits")
        val serialized = queries.joinToString("|||")
        val deserialized = serialized.split("|||").filter { it.isNotBlank() }

        assertEquals(3, deserialized.size)
        assertEquals("Arijit Singh", deserialized[0])
        assertEquals("Attention", deserialized[1])
        assertEquals("Pop Hits", deserialized[2])
    }

    @Test
    fun testCategoryKeywordsMatching() {
        val popKeywords = listOf("pop", "puth", "weeknd", "swift", "sheeran", "bieber", "lipa", "ariana", "drake", "bruno")
        val bollywoodKeywords = listOf("bollywood", "hindi", "arijit", "neha", "badshah", "jubin", "shreya")

        val matchesPop = sampleSongs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            popKeywords.any { text.contains(it) }
        }

        val matchesBollywood = sampleSongs.filter { song ->
            val text = "${song.title} ${song.artist} ${song.album}".lowercase()
            bollywoodKeywords.any { text.contains(it) }
        }

        assertTrue(matchesPop.isNotEmpty())
        assertTrue(matchesBollywood.isNotEmpty())
        assertEquals("Kesariya", matchesBollywood[0].title)
    }
}
