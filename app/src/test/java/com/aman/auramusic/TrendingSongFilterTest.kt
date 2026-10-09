package com.aman.auramusic

import com.aman.auramusic.online.filter.TrendingFilterConfig
import com.aman.auramusic.online.filter.TrendingSongFilter
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendingSongFilterTest {

    private val filter = TrendingSongFilter()

    @Test
    fun testNormalFourMinuteSongIsRetained() {
        val song = OnlineSong(
            id = "normal_1",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            durationSeconds = 175L,
            source = AudioSource.YOUTUBE
        )

        val decision = filter.evaluateSong(song)
        assertFalse(decision.isExcluded)
        assertEquals(1.0, decision.rankingScore, 0.001)

        val output = filter.filter(listOf(song))
        assertEquals(1, output.retainedCount)
        assertEquals(0, output.excludedCount)
        assertEquals("Espresso", output.filteredSongs.first().title)
    }

    @Test
    fun testClearlyNamedOneHourNonstopCompilationIsExcluded() {
        val song = OnlineSong(
            id = "nonstop_1",
            title = "Best Pop Hits 2026 | 1 Hour Non-Stop Mix",
            artist = "Vibe Music",
            durationSeconds = 3600L,
            source = AudioSource.YOUTUBE
        )

        val decision = filter.evaluateSong(song)
        assertTrue(decision.isExcluded)
        assertNotNull(decision.reason)

        val output = filter.filter(listOf(song))
        assertEquals(0, output.retainedCount)
        assertEquals(1, output.excludedCount)
    }

    @Test
    fun testFullAlbumCollectionIsExcluded() {
        val song1 = OnlineSong(
            id = "album_1",
            title = "Taylor Swift - The Tortured Poets Department [Full Album]",
            artist = "Taylor Swift",
            durationSeconds = 4100L,
            source = AudioSource.YOUTUBE
        )
        val song2 = OnlineSong(
            id = "album_2",
            title = "Kendrick Lamar - GNX (Complete Album Audio)",
            artist = "Kendrick Lamar",
            durationSeconds = 3200L,
            source = AudioSource.YOUTUBE
        )

        assertTrue(filter.evaluateSong(song1).isExcluded)
        assertTrue(filter.evaluateSong(song2).isExcluded)

        val output = filter.filter(listOf(song1, song2))
        assertEquals(0, output.retainedCount)
        assertEquals(2, output.excludedCount)
    }

    @Test
    fun testLegitimate18MinuteLivePerformanceIsRetainedWithAppropriateScore() {
        val liveSong = OnlineSong(
            id = "live_1",
            title = "Pink Floyd - Echoes (Live at Pompeii)",
            artist = "Pink Floyd",
            durationSeconds = 18 * 60L, // 1080s
            source = AudioSource.YOUTUBE
        )

        val decision = filter.evaluateSong(liveSong)
        assertFalse("Live performance should not be excluded", decision.isExcluded)
        assertTrue("Ranking score should have soft duration penalty", decision.rankingScore in 0.80..0.90)

        val output = filter.filter(listOf(liveSong))
        assertEquals(1, output.retainedCount)
    }

    @Test
    fun testLongClassicalCompositionIsNotAutomaticallyRejected() {
        val classical = OnlineSong(
            id = "classical_1",
            title = "Beethoven - Symphony No. 9 in D minor, Op. 125",
            artist = "Berlin Philharmonic",
            durationSeconds = 22 * 60L, // 1320s
            source = AudioSource.YOUTUBE
        )

        val decision = filter.evaluateSong(classical)
        assertFalse("Classical composition should be retained", decision.isExcluded)
        assertTrue("Ranking score reflects soft penalty", decision.rankingScore in 0.80..0.90)

        val output = filter.filter(listOf(classical))
        assertEquals(1, output.retainedCount)
    }

    @Test
    fun testLegitimateRemixIsRetained() {
        val remix1 = OnlineSong(
            id = "remix_1",
            title = "Levitating (Don Diablo Remix)",
            artist = "Dua Lipa",
            durationSeconds = 210L,
            source = AudioSource.YOUTUBE
        )
        val remix2 = OnlineSong(
            id = "remix_2",
            title = "Kesariya (Lofi Mix)",
            artist = "Arijit Singh",
            durationSeconds = 195L,
            source = AudioSource.JIOSAAVN
        )

        assertFalse(filter.evaluateSong(remix1).isExcluded)
        assertFalse(filter.evaluateSong(remix2).isExcluded)

        val output = filter.filter(listOf(remix1, remix2))
        assertEquals(2, output.retainedCount)
    }

    @Test
    fun testBroadKeywordsIncidentallyDoNotCauseFalsePositives() {
        val tracks = listOf(
            OnlineSong(id = "kw_1", title = "Wings", artist = "Little Mix", durationSeconds = 220L),
            OnlineSong(id = "kw_2", title = "Hits Different", artist = "Taylor Swift", durationSeconds = 230L),
            OnlineSong(id = "kw_3", title = "Live to Rise", artist = "Soundgarden", durationSeconds = 280L),
            OnlineSong(id = "kw_4", title = "Mix & Match", artist = "LOONA / ODD EYE CIRCLE", durationSeconds = 110L)
        )

        for (t in tracks) {
            val decision = filter.evaluateSong(t)
            assertFalse("Track '${t.title}' by '${t.artist}' should not be excluded", decision.isExcluded)
        }

        val output = filter.filter(tracks)
        assertEquals(4, output.retainedCount)
        assertEquals(0, output.excludedCount)
    }

    @Test
    fun testMissingOrMalformedDurationDoesNotCrashFiltering() {
        val missingZero = OnlineSong(
            id = "no_dur_0",
            title = "Shape of You",
            artist = "Ed Sheeran",
            durationSeconds = 0L,
            source = AudioSource.YOUTUBE
        )
        val missingNegative = OnlineSong(
            id = "no_dur_neg",
            title = "Perfect",
            artist = "Ed Sheeran",
            durationSeconds = -1L,
            source = AudioSource.YOUTUBE
        )

        val d1 = filter.evaluateSong(missingZero)
        val d2 = filter.evaluateSong(missingNegative)

        assertFalse(d1.isExcluded)
        assertFalse(d2.isExcluded)
        assertEquals(1.0, d1.rankingScore, 0.001)

        val output = filter.filter(listOf(missingZero, missingNegative))
        assertEquals(2, output.retainedCount)
    }

    @Test
    fun testDuplicateProviderIdsAreRemovedWithinFeed() {
        val songA = OnlineSong(id = "dup_1", title = "Blinding Lights", artist = "The Weeknd", durationSeconds = 200L, source = AudioSource.YOUTUBE)
        val songB = OnlineSong(id = "dup_1", title = "Blinding Lights", artist = "The Weeknd", durationSeconds = 200L, source = AudioSource.YOUTUBE)
        val songC = OnlineSong(id = "dup_2", title = "Starboy", artist = "The Weeknd", durationSeconds = 230L, source = AudioSource.YOUTUBE)

        val output = filter.filter(listOf(songA, songB, songC))
        assertEquals(2, output.filteredSongs.size)
        assertEquals(listOf("dup_1", "dup_2"), output.filteredSongs.map { it.id })
    }

    @Test
    fun testIdenticalIdsFromDifferentProvidersDoNotCollide() {
        val ytSong = OnlineSong(id = "common_id", title = "Song A", artist = "Artist A", durationSeconds = 180L, source = AudioSource.YOUTUBE)
        val jioSong = OnlineSong(id = "common_id", title = "Song B", artist = "Artist B", durationSeconds = 200L, source = AudioSource.JIOSAAVN)

        val output = filter.filter(listOf(ytSong, jioSong))
        assertEquals(2, output.filteredSongs.size)
        assertEquals(2, output.retainedCount)
    }

    @Test
    fun testHindiAndRegionalLanguageTitlesHandledSafely() {
        val validTracks = listOf(
            OnlineSong(id = "hi_1", title = "O Maahi (From 'Dunki')", artist = "Arijit Singh", durationSeconds = 233L, source = AudioSource.JIOSAAVN),
            OnlineSong(id = "hi_2", title = "Chuttamalle - Devara", artist = "Anirudh Ravichander, Shilpa Rao", durationSeconds = 210L, source = AudioSource.YOUTUBE),
            OnlineSong(id = "hi_3", title = "Kesariya - Brahmāstra", artist = "Pritam, Arijit Singh", durationSeconds = 268L, source = AudioSource.JIOSAAVN),
            OnlineSong(id = "hi_4", title = "Illuminati (From 'Aavesham')", artist = "Sushin Shyam, Dabzee", durationSeconds = 194L, source = AudioSource.YOUTUBE)
        )

        val invalidTracks = listOf(
            OnlineSong(id = "hi_bad_1", title = "Bollywood Sad Songs 2026 - Audio Jukebox", artist = "Various Artists", durationSeconds = 3000L, source = AudioSource.YOUTUBE),
            OnlineSong(id = "hi_bad_2", title = "90s Hindi Romantic Songs Nonstop Collection", artist = "Various Artists", durationSeconds = 4200L, source = AudioSource.YOUTUBE),
            OnlineSong(id = "hi_bad_3", title = "Top 20 Bollywood Songs 2026 Jukebox", artist = "T-Series", durationSeconds = 3500L, source = AudioSource.YOUTUBE),
            OnlineSong(id = "hi_bad_4", title = "Arijit Singh All Songs Mashup 2026", artist = "DJ Mix", durationSeconds = 1800L, source = AudioSource.YOUTUBE)
        )

        for (v in validTracks) {
            assertFalse("Track '${v.title}' should be retained", filter.evaluateSong(v).isExcluded)
        }
        for (inv in invalidTracks) {
            assertTrue("Track '${inv.title}' should be excluded", filter.evaluateSong(inv).isExcluded)
        }

        val output = filter.filter(validTracks + invalidTracks)
        assertEquals(4, output.retainedCount)
        assertEquals(4, output.excludedCount)
    }

    @Test
    fun testUnknownResultTypesDoNotCauseValidSongsToDisappear() {
        val song = OnlineSong(
            id = "unknown_type_1",
            title = "Midnight City",
            artist = "M83",
            album = "Unknown Album",
            durationSeconds = 243L,
            source = AudioSource.YOUTUBE
        )

        val output = filter.filter(listOf(song))
        assertEquals(1, output.retainedCount)
        assertEquals("Midnight City", output.filteredSongs.first().title)
    }

    @Test
    fun testDurationParsingHelper() {
        assertEquals(225L, TrendingSongFilter.parseDurationToSeconds("3:45"))
        assertEquals(225L, TrendingSongFilter.parseDurationToSeconds("03:45"))
        assertEquals(4530L, TrendingSongFilter.parseDurationToSeconds("1:15:30"))
        assertEquals(0L, TrendingSongFilter.parseDurationToSeconds(null))
        assertEquals(0L, TrendingSongFilter.parseDurationToSeconds(""))
        assertEquals(0L, TrendingSongFilter.parseDurationToSeconds("3.5M views"))
        assertEquals(0L, TrendingSongFilter.parseDurationToSeconds("Chapter 1: The End"))
    }

    @Test
    fun testDryRunModeRecordsExclusionsWithoutRemovingSongs() {
        val dryFilter = TrendingSongFilter(TrendingFilterConfig(dryRun = true))
        val compilation = OnlineSong(
            id = "dry_comp",
            title = "Best Hindi Songs Jukebox 2026",
            artist = "DJ",
            durationSeconds = 3600L,
            source = AudioSource.YOUTUBE
        )
        val validSong = OnlineSong(
            id = "dry_valid",
            title = "Normal Song",
            artist = "Artist",
            durationSeconds = 200L,
            source = AudioSource.YOUTUBE
        )

        val output = dryFilter.filter(listOf(compilation, validSong))
        // In dry run mode, both songs are kept in filteredSongs
        assertEquals(2, output.filteredSongs.size)
        // But decisions correctly mark the compilation
        assertEquals(1, output.excludedCount)
        val compDecision = output.decisions.find { it.song.id == "dry_comp" }
        assertNotNull(compDecision)
        assertTrue(compDecision!!.isExcluded)
    }

    @Test
    fun testAdditionalCompilationPatterns() {
        val testCases = listOf(
            "Top 50 Songs of 2026 Continuous Mix",
            "Romantic Medley 2026 - Merged Songs",
            "Best of Arijit Singh Compilation",
            "Non-Stop Party Mega Mix 2026",
            "English Hits Collection | Jukebox",
            "Bollywood Merged Audio Nonstop",
            "2 Hours Relaxing Piano Music All Songs",
            "Top 20 Hit Songs 2026",
            "Greatest Hits Album Discography",
            "Top Hits Playlist ~ Spotify Playlist",
            "Best of Bollywood Vol. 1",
            "Top Hits 2026 (New Popular Songs 2026 Best English Songs Best Music)"
        )

        for (title in testCases) {
            val song = OnlineSong(id = "test_$title", title = title, artist = "Various", durationSeconds = 2000L)
            assertTrue("Expected '$title' to be excluded", filter.evaluateSong(song).isExcluded)
        }
    }
}
