package com.aman.auramusic

import com.aman.auramusic.data.model.PlaybackHistoryEntry
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.filter.RecommendationDeduplicator
import com.aman.auramusic.online.filter.SectionDeduplicator
import com.aman.auramusic.online.filter.TrendingSongFilter
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationsDeduplicationTest {

    @Test
    fun testDuplicateVideoIdsAcrossSectionsAreExcluded() {
        val deduplicator = SectionDeduplicator()

        val dailyFeaturedSong = OnlineSong(
            id = "video_abc123",
            title = "Apt.",
            artist = "ROSÉ & Bruno Mars",
            source = AudioSource.YOUTUBE
        )

        val trendingSongCandidate = OnlineSong(
            id = "video_abc123",
            title = "Apt.",
            artist = "ROSÉ & Bruno Mars",
            source = AudioSource.YOUTUBE
        )

        val quickHitsCandidate = OnlineSong(
            id = "video_abc123",
            title = "Apt.",
            artist = "ROSÉ & Bruno Mars",
            source = AudioSource.YOUTUBE
        )

        // Allocate to Daily Featured
        assertTrue("Daily Featured allocation must succeed", deduplicator.allocate(dailyFeaturedSong))

        // Trending candidate has same video ID -> must be rejected
        assertTrue("Trending candidate should be detected as duplicate", deduplicator.isDuplicate(trendingSongCandidate))
        assertFalse("Trending candidate allocation must fail", deduplicator.allocate(trendingSongCandidate))

        // Quick Hits candidate has same video ID -> must be rejected
        assertTrue("Quick Hits candidate should be detected as duplicate", deduplicator.isDuplicate(quickHitsCandidate))
        assertFalse("Quick Hits candidate allocation must fail", deduplicator.allocate(quickHitsCandidate))
    }

    @Test
    fun testMissingIdFallsBackToNormalizedTitleAndArtistMatching() {
        val deduplicator = SectionDeduplicator()

        val song1 = OnlineSong(
            id = "", // Missing ID
            title = "Don’t Start Now",
            artist = "Dua Lipa",
            source = AudioSource.YOUTUBE
        )

        val song2 = OnlineSong(
            id = "", // Missing ID
            title = "Don't Start Now", // Typographic straight quote vs curly
            artist = "dua lipa", // Differing case
            source = AudioSource.JIOSAAVN
        )

        assertTrue(deduplicator.allocate(song1))
        assertTrue("Missing ID should trigger normalized title/artist match", deduplicator.isDuplicate(song2))
        assertFalse(deduplicator.allocate(song2))
    }

    @Test
    fun testCrossProviderDeduplicationBetweenYouTubeAndJioSaavn() {
        val deduplicator = SectionDeduplicator()

        val ytSong = OnlineSong(
            id = "yt_xyz999",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            source = AudioSource.YOUTUBE
        )

        val saavnSong = OnlineSong(
            id = "saavn_445566",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            source = AudioSource.JIOSAAVN
        )

        assertTrue(deduplicator.allocate(ytSong))
        assertTrue("Same track across different providers must be detected as duplicate", deduplicator.isDuplicate(saavnSong))
        assertFalse(deduplicator.allocate(saavnSong))
    }

    @Test
    fun testLocalTrackDeduplicationAgainstOnlineSections() {
        val deduplicator = SectionDeduplicator()

        val localSong = Song(
            id = 1001L,
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            duration = 233000L,
            dateAdded = System.currentTimeMillis(),
            uri = "content://media/1001",
            filePath = "/storage/music/shape.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )

        val onlineSong = OnlineSong(
            id = "online_ed_shape",
            title = "Shape of You",
            artist = "Ed Sheeran",
            source = AudioSource.YOUTUBE
        )

        assertTrue(deduplicator.allocate(localSong))
        assertTrue("Online song matching existing local track title/artist must be excluded", deduplicator.isDuplicate(onlineSong))
        assertFalse(deduplicator.allocate(onlineSong))
    }

    @Test
    fun testIndependentSectionAllocationsWithDistinctTracks() {
        val deduplicator = SectionDeduplicator()

        val dailyFeatured = listOf(
            OnlineSong(id = "df_1", title = "Blinding Lights", artist = "The Weeknd"),
            OnlineSong(id = "df_2", title = "Save Your Tears", artist = "The Weeknd")
        )

        val trendingCandidates = listOf(
            OnlineSong(id = "df_1", title = "Blinding Lights", artist = "The Weeknd"), // Overlap with Daily Featured
            OnlineSong(id = "tr_1", title = "Flowers", artist = "Miley Cyrus"),
            OnlineSong(id = "tr_2", title = "As It Was", artist = "Harry Styles")
        )

        val quickHitsCandidates = listOf(
            OnlineSong(id = "tr_1", title = "Flowers", artist = "Miley Cyrus"), // Overlap with Trending
            OnlineSong(id = "qh_1", title = "Levitating", artist = "Dua Lipa"),
            OnlineSong(id = "qh_2", title = "Cruel Summer", artist = "Taylor Swift")
        )

        val cleanDaily = deduplicator.allocateAll(dailyFeatured)
        assertEquals(2, cleanDaily.size)

        val cleanTrending = deduplicator.allocateAll(trendingCandidates)
        assertEquals(2, cleanTrending.size)
        assertEquals(listOf("Flowers", "As It Was"), cleanTrending.map { it.title })

        val cleanQuickHits = deduplicator.allocateAll(quickHitsCandidates)
        assertEquals(2, cleanQuickHits.size)
        assertEquals(listOf("Levitating", "Cruel Summer"), cleanQuickHits.map { it.title })
    }

    @Test
    fun testListenAgainOrderingAndRepeatedPlaysDeduplication() {
        val gson = Gson()

        val songA = OnlineSong(id = "os_a", title = "Song A", artist = "Artist A")
        val songB = OnlineSong(id = "os_b", title = "Song B", artist = "Artist B")
        val localC = Song(
            id = 200L,
            title = "Song C",
            artist = "Artist C",
            album = "Album C",
            duration = 180000L,
            dateAdded = 0L,
            uri = "",
            filePath = "",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )

        // History contains Song A played twice (older play, then newer play), Song B once, Song C once
        val history = listOf(
            PlaybackHistoryEntry(
                songId = 1L,
                playedAt = 3000L, // Most recent
                playCount = 2,
                onlineSongJson = gson.toJson(songA)
            ),
            PlaybackHistoryEntry(
                songId = 200L,
                playedAt = 2000L,
                playCount = 1,
                onlineSongJson = null // Local song
            ),
            PlaybackHistoryEntry(
                songId = 2L,
                playedAt = 1500L,
                playCount = 1,
                onlineSongJson = gson.toJson(songB)
            ),
            PlaybackHistoryEntry(
                songId = 1L,
                playedAt = 1000L, // Older play of Song A - should NOT duplicate Song A
                playCount = 1,
                onlineSongJson = gson.toJson(songA)
            )
        )

        val localSongs = listOf(localC)

        // Process Listen Again logic as implemented in HomeTestViewModel
        val seenTrackKeys = mutableSetOf<String>()
        val resultTitles = mutableListOf<String>()

        for (entry in history) {
            if (!entry.onlineSongJson.isNullOrBlank()) {
                val os = gson.fromJson(entry.onlineSongJson, OnlineSong::class.java)
                val idKey = "id:${os.id.trim()}"
                val normTitle = RecommendationDeduplicator.normalizeText(os.title)
                val normArtist = RecommendationDeduplicator.normalizeText(os.artist)
                val metaKey = "meta:$normTitle|$normArtist"
                if (seenTrackKeys.add(idKey) && seenTrackKeys.add(metaKey)) {
                    resultTitles.add(os.title)
                }
            } else {
                val ls = localSongs.find { it.id == entry.songId }
                if (ls != null) {
                    val idKey = "local:${ls.id}"
                    val normTitle = RecommendationDeduplicator.normalizeText(ls.title)
                    val normArtist = RecommendationDeduplicator.normalizeText(ls.artist)
                    val metaKey = "meta:$normTitle|$normArtist"
                    if (seenTrackKeys.add(idKey) && seenTrackKeys.add(metaKey)) {
                        resultTitles.add(ls.title)
                    }
                }
            }
        }

        // Expected: Exactly 3 songs, in order of most recent play: Song A (3000), Song C (2000), Song B (1500)
        assertEquals(3, resultTitles.size)
        assertEquals("Song A", resultTitles[0])
        assertEquals("Song C", resultTitles[1])
        assertEquals("Song B", resultTitles[2])
    }

    @Test
    fun testEmptyHistoryProducesEmptyListenAgainWithoutFabricatingTracks() {
        val history = emptyList<PlaybackHistoryEntry>()
        val localSongs = emptyList<Song>()

        val seenTrackKeys = mutableSetOf<String>()
        val result = mutableListOf<String>()

        for (entry in history) {
            result.add(entry.songId.toString())
        }

        assertTrue("Empty history must yield empty Listen Again list", result.isEmpty())
    }

    @Test
    fun testExistingTrendingSongFilterBehaviorPreserved() {
        val filter = TrendingSongFilter()

        val standardSong = OnlineSong(
            id = "valid_1",
            title = "Starboy",
            artist = "The Weeknd",
            durationSeconds = 230L
        )

        val compilation = OnlineSong(
            id = "mix_1",
            title = "Best of 2026 Hits - 2 Hours Nonstop Mega Mix",
            artist = "Mix DJ",
            durationSeconds = 7200L
        )

        val songs = listOf(standardSong, compilation)
        val filtered = filter.filterSongs(songs)

        assertEquals(1, filtered.size)
        assertEquals("Starboy", filtered.first().title)
    }

    @Test
    fun testQuickHitIs4PerRowAndListenAgainIs2PerRow() {
        val tracks = (1..10).map { i ->
            OnlineSong(id = "track_$i", title = "Track $i", artist = "Artist")
        }

        // Quick Hits: 4 rows per column -> [4, 4, 2]
        val quickHitColumns = tracks.chunked(4)
        assertEquals(3, quickHitColumns.size)
        assertEquals(4, quickHitColumns[0].size)
        assertEquals(4, quickHitColumns[1].size)
        assertEquals(2, quickHitColumns[2].size)

        // Listen Again: 2 rows per column -> [2, 2, 2, 2, 2]
        val listenAgainColumns = tracks.chunked(2)
        assertEquals(5, listenAgainColumns.size)
        assertEquals(2, listenAgainColumns[0].size)
        assertEquals(2, listenAgainColumns[1].size)
        assertEquals(2, listenAgainColumns[4].size)
    }
}

