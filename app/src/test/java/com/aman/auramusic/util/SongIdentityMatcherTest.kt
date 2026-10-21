package com.aman.auramusic.util

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongIdentityMatcherTest {

    private fun localSong(
        title: String,
        artist: String = "The Example Artist",
        durationSeconds: Long = 210L
    ) = Song(
        id = 987654321L,
        title = title,
        artist = artist,
        album = "Test Album",
        duration = durationSeconds * 1000L,
        uri = "content://test/song",
        filePath = "/test/song.mp3",
        artworkUri = null,
        mimeType = "audio/mpeg"
    )

    private fun onlineSong(
        title: String,
        artist: String = "The Example Artist",
        durationSeconds: Long = 210L
    ) = OnlineSong(
        id = "provider-track-xyz",
        title = title,
        artist = artist,
        durationSeconds = durationSeconds
    )

    @Test
    fun matchesEquivalentTitlesAndArtists() {
        assertTrue(
            SongIdentityMatcher.isConfidentMatch(
                localSong("A Beautiful Song"),
                onlineSong("A Beautiful Song")
            )
        )
    }

    @Test
    fun rejectsSubstringOnlyTitleMatches() {
        assertFalse(
            SongIdentityMatcher.isConfidentMatch(
                localSong("Stay"),
                onlineSong("Stay With Me")
            )
        )
    }

    @Test
    fun rejectsDifferentVersions() {
        assertFalse(
            SongIdentityMatcher.isConfidentMatch(
                localSong("A Beautiful Song"),
                onlineSong("A Beautiful Song (Remix)")
            )
        )
    }

    @Test
    fun rejectsDifferentArtists() {
        assertFalse(
            SongIdentityMatcher.isConfidentMatch(
                localSong("A Beautiful Song", artist = "Artist One"),
                onlineSong("A Beautiful Song", artist = "Artist Two")
            )
        )
    }

    @Test
    fun rejectsMissingArtistEvidence() {
        assertFalse(
            SongIdentityMatcher.isConfidentMatch(
                localSong("A Beautiful Song", artist = ""),
                onlineSong("A Beautiful Song", artist = "")
            )
        )
    }

    @Test
    fun rejectsLargeDurationMismatch() {
        assertFalse(
            SongIdentityMatcher.isConfidentMatch(
                localSong("A Beautiful Song", durationSeconds = 210L),
                onlineSong("A Beautiful Song", durationSeconds = 260L)
            )
        )
    }
}
