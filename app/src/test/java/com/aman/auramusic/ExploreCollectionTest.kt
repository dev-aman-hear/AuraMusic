package com.aman.auramusic

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreCollectionTest {

    private fun songDeduplicationKey(song: OnlineSong): String =
        "${song.title.trim().lowercase()}|${song.artist.trim().lowercase()}"

    private fun deduplicateOnlineSongs(songs: List<OnlineSong>): List<OnlineSong> {
        val seenIds = mutableSetOf<String>()
        val seenTitleArtist = mutableSetOf<String>()
        val result = mutableListOf<OnlineSong>()

        for (song in songs) {
            val idKey = "${song.source.name}_${song.id}"
            val titleArtistKey = songDeduplicationKey(song)

            val isIdDuplicate = song.id.isNotBlank() && !seenIds.add(idKey)
            val isExactDuplicate = !seenTitleArtist.add(titleArtistKey)

            if (!isIdDuplicate && !isExactDuplicate) {
                result.add(song)
            }
        }
        return result
    }

    @Test
    fun testSameSongFromDifferentProvidersIsDeduplicated() {
        val ytSong = OnlineSong(
            id = "yt_123",
            title = "Kesariya",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )
        val saavnSong = OnlineSong(
            id = "saavn_456",
            title = "Kesariya",
            artist = "Arijit Singh",
            source = AudioSource.JIOSAAVN
        )

        val deduplicated = deduplicateOnlineSongs(listOf(ytSong, saavnSong))
        assertEquals(1, deduplicated.size)
        assertEquals("yt_123", deduplicated.first().id)
    }

    @Test
    fun testDifferentSongsBySameArtistArePreserved() {
        val song1 = OnlineSong(
            id = "yt_101",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )
        val song2 = OnlineSong(
            id = "yt_102",
            title = "Channa Mereya",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )
        val song3 = OnlineSong(
            id = "yt_103",
            title = "Apna Bana Le",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )

        val deduplicated = deduplicateOnlineSongs(listOf(song1, song2, song3))
        assertEquals(3, deduplicated.size)
    }

    @Test
    fun testRepriseVersionPreservedSeparately() {
        val original = OnlineSong(
            id = "yt_201",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )
        val reprise = OnlineSong(
            id = "yt_202",
            title = "Tum Hi Ho (Reprise)",
            artist = "Arijit Singh",
            source = AudioSource.YOUTUBE
        )

        val deduplicated = deduplicateOnlineSongs(listOf(original, reprise))
        assertEquals(2, deduplicated.size)
    }

    @Test
    fun testCrossSectionExclusivity() {
        val featured = OnlineSong(
            id = "feat_1",
            title = "Featured Track",
            artist = "Artist A",
            source = AudioSource.YOUTUBE
        )
        val trending1 = OnlineSong(
            id = "trend_1",
            title = "Trending Track One",
            artist = "Artist B",
            source = AudioSource.YOUTUBE
        )
        val duplicateOfFeaturedInTrending = OnlineSong(
            id = "trend_dup",
            title = "Featured Track",
            artist = "Artist A",
            source = AudioSource.JIOSAAVN
        )
        val trending2 = OnlineSong(
            id = "trend_2",
            title = "Trending Track Two",
            artist = "Artist C",
            source = AudioSource.YOUTUBE
        )

        val rawTrending = listOf(duplicateOfFeaturedInTrending, trending1, trending2)
        val featuredKey = songDeduplicationKey(featured)

        val cleanTrending = deduplicateOnlineSongs(rawTrending).filterNot {
            songDeduplicationKey(it) == featuredKey
        }

        assertEquals(2, cleanTrending.size)
        assertFalse(cleanTrending.any { songDeduplicationKey(it) == featuredKey })

        val reservedKeys = buildSet {
            add(featuredKey)
            cleanTrending.forEach { add(songDeduplicationKey(it)) }
        }

        val exploreCandidates = listOf(
            featured,
            trending1,
            OnlineSong(id = "exp_1", title = "Unique Explore", artist = "Artist D", source = AudioSource.YOUTUBE)
        )
        val cleanExplore = exploreCandidates.filterNot { songDeduplicationKey(it) in reservedKeys }

        assertEquals(1, cleanExplore.size)
        assertEquals("exp_1", cleanExplore.first().id)
    }

    @Test
    fun testCollectionsWithFewerThanFourTracksFiltered() {
        val smallCollectionTracks = listOf(
            OnlineSong(id = "1", title = "Track 1", artist = "Artist", source = AudioSource.YOUTUBE),
            OnlineSong(id = "2", title = "Track 2", artist = "Artist", source = AudioSource.YOUTUBE),
            OnlineSong(id = "3", title = "Track 3", artist = "Artist", source = AudioSource.YOUTUBE)
        )
        val fullCollectionTracks = smallCollectionTracks + OnlineSong(
            id = "4",
            title = "Track 4",
            artist = "Artist",
            source = AudioSource.YOUTUBE
        )

        val smallUnique = deduplicateOnlineSongs(smallCollectionTracks).filter { it.source == AudioSource.YOUTUBE }
        val fullUnique = deduplicateOnlineSongs(fullCollectionTracks).filter { it.source == AudioSource.YOUTUBE }

        assertTrue(smallUnique.size < 4)
        assertTrue(fullUnique.size >= 4)
    }

    @Test
    fun testYouTubeCollectionBrowseIdExtraction() {
        val collectionWithPrefix = OnlinePlaylist(
            id = "ytmusic:MPREb_aZJfSnqyHvS",
            title = "Guru Soundtrack",
            subtitle = "A.R. Rahman",
            artworkUrl = "https://example.com/art.jpg",
            songCount = 7,
            source = AudioSource.YOUTUBE
        )

        val browseId = collectionWithPrefix.id.substringAfter("ytmusic:").trim()
        assertEquals("MPREb_aZJfSnqyHvS", browseId)

        val compositeId = "ytmusic_album_ytmusic:MPREb_aZJfSnqyHvS"
        val compositeBrowseId = compositeId.substringAfter("ytmusic:").trim()
        assertEquals("MPREb_aZJfSnqyHvS", compositeBrowseId)
    }
}
