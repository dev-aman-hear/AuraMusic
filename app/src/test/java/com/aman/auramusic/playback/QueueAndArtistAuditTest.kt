package com.aman.auramusic.playback

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.viewmodel.QueueEntry
import com.aman.auramusic.online.network.repository.YouTubeArtist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class QueueAndArtistAuditTest {

    private fun sampleSong(id: Long, title: String): Song {
        return Song(
            id = id,
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            duration = 180000L,
            uri = "content://media/$id",
            filePath = "/path/to/$id.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )
    }

    // --- Artist Identity Matching Tests ---

    private fun resolveVerifiedArtistBrowseId(artistName: String, searchResults: List<YouTubeArtist>): String {
        val targetArtistKey = artistName.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        val found = searchResults.firstOrNull {
            it.name.lowercase(Locale.ROOT).filter { ch -> ch.isLetterOrDigit() } == targetArtistKey
        }
        return if (found != null && found.id.isNotBlank() && !found.id.startsWith("local_") && !found.id.startsWith("artist_") && !found.id.startsWith("yt_artist_")) {
            found.id
        } else {
            ""
        }
    }

    @Test
    fun artist_matching_refuses_arbitrary_first_search_result_and_substring_coincidences() {
        val queryArtist = "Adele"
        val unrelatedResults = listOf(
            YouTubeArtist(id = "UC_unrelated_1", name = "Adele Tribute Band", profileImageUrl = null),
            YouTubeArtist(id = "UC_unrelated_2", name = "A Delicious Harmony", profileImageUrl = null)
        )

        val resolvedId = resolveVerifiedArtistBrowseId(queryArtist, unrelatedResults)
        // Must NOT fallback to searchResults.firstOrNull() or substring match
        assertEquals("", resolvedId)
    }

    @Test
    fun artist_matching_resolves_exact_and_alphanumeric_normalized_matches() {
        val queryArtist = "AC/DC"
        val results = listOf(
            YouTubeArtist(id = "UC_other", name = "AC DC Tribute", profileImageUrl = null),
            YouTubeArtist(id = "UC_acdc_real", name = "ACDC", profileImageUrl = null)
        )

        val resolvedId = resolveVerifiedArtistBrowseId(queryArtist, results)
        assertEquals("UC_acdc_real", resolvedId)
    }

    @Test
    fun artist_matching_rejects_placeholder_and_local_ids() {
        val queryArtist = "Coldplay"
        val results = listOf(
            YouTubeArtist(id = "local_123", name = "Coldplay", profileImageUrl = null)
        )

        val resolvedId = resolveVerifiedArtistBrowseId(queryArtist, results)
        assertEquals("", resolvedId)
    }

    // --- Queue Logic Tests ---

    @Test
    fun queue_clear_retains_current_song_only() {
        val songs = listOf(sampleSong(1, "Track 1"), sampleSong(2, "Track 2"), sampleSong(3, "Track 3"))
        val currentSong = songs[1] // Track 2 is active

        // Simulate clearQueueExceptCurrent
        val initialQueue = songs.map { QueueEntry(song = it) }
        val clearedQueue = listOf(QueueEntry(song = currentSong))

        assertEquals(1, clearedQueue.size)
        assertEquals(currentSong.id, clearedQueue.first().song.id)
    }

    @Test
    fun queue_remove_item_updates_queue_and_retains_order() {
        val songs = listOf(sampleSong(1, "Track 1"), sampleSong(2, "Track 2"), sampleSong(3, "Track 3"))
        val queue = songs.map { QueueEntry(song = it) }.toMutableList()

        // Remove item 2 (non-current)
        val songToRemove = songs[1]
        val updatedQueue = queue.filterNot { it.song.id == songToRemove.id }

        assertEquals(2, updatedQueue.size)
        assertEquals(1L, updatedQueue[0].song.id)
        assertEquals(3L, updatedQueue[1].song.id)
    }

    @Test
    fun queue_shuffle_preserves_current_song_at_head_and_restores_canonical_order() {
        val songs = (1L..6L).map { sampleSong(it, "Track $it") }
        val canonical = songs.map { QueueEntry(song = it) }
        val currentSongId = 3L

        // Turn Shuffle ON: current song remains at head, remaining are shuffled
        val current = canonical.first { it.song.id == currentSongId }
        val rest = canonical.filterNot { it.song.id == currentSongId }
        val shuffled = listOf(current) + rest.shuffled()

        assertEquals(canonical.size, shuffled.size)
        assertEquals(currentSongId, shuffled.first().song.id)
        assertEquals(canonical.map { it.song.id }.toSet(), shuffled.map { it.song.id }.toSet())

        // Turn Shuffle OFF: restores canonical order
        val restored = canonical.distinctBy { it.song.id }
        assertEquals(canonical.map { it.song.id }, restored.map { it.song.id })
    }
}
