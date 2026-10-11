package com.aman.auramusic

import com.aman.auramusic.online.filter.ExplicitTitleFilter
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplicitTitleFilterTest {

    @Test
    fun hidesExplicitPlaylistTitle() {
        assertTrue(ExplicitTitleFilter.shouldHideTitle("Sex Playlist"))
        assertTrue(ExplicitTitleFilter.shouldHideTitle("XXX Mix"))
        assertTrue(ExplicitTitleFilter.shouldHideTitle("Explicit FUCK song"))
    }

    @Test
    fun preservesOrdinaryAndLegitimateTitles() {
        assertFalse(ExplicitTitleFilter.shouldHideTitle("Love Me Like You Do"))
        assertFalse(ExplicitTitleFilter.shouldHideTitle("Sex on Fire"))
        assertFalse(ExplicitTitleFilter.shouldHideTitle("Pussycat Dolls - Buttons"))
    }

    @Test
    fun normalizesCaseAndPunctuation() {
        assertTrue(ExplicitTitleFilter.shouldHideTitle("XXX---MIX"))
        assertTrue(ExplicitTitleFilter.shouldHideTitle("sex   playlist"))
        assertTrue(ExplicitTitleFilter.shouldHideTitle("  \tsex\nplaylist\r  "))
    }

    @Test
    fun handlesNullBlankAndUnusualUnicodeSafely() {
        assertFalse(ExplicitTitleFilter.shouldHideTitle(null))
        assertFalse(ExplicitTitleFilter.shouldHideTitle(""))
        assertFalse(ExplicitTitleFilter.shouldHideTitle("   "))
        // Fullwidth letters
        assertTrue(ExplicitTitleFilter.shouldHideTitle("\uFF58\uFF58\uFF58 \uFF4D\uFF49\uFF58")) // ｘｘｘ ｍｉｘ
        // Non-breaking spaces and accents
        assertFalse(ExplicitTitleFilter.shouldHideTitle("Caf\u00E9\u00A0Del\u00A0Mar"))
    }

    @Test
    fun filtersOnlyByTitleAndDoesNotMutateInput() {
        val normal = OnlineSong(id = "1", title = "Normal Song", artist = "Explicit Artist", source = AudioSource.YOUTUBE)
        val explicit = OnlineSong(id = "2", title = "XXX Mix", artist = "Artist", source = AudioSource.YOUTUBE)
        val input = listOf(normal, explicit)

        val result = ExplicitTitleFilter.filterOnline(input)

        assertEquals(listOf(normal), result)
        assertEquals(2, input.size)
    }

    @Test
    fun explicitArtistNameAloneDoesNotHideSong() {
        val song = OnlineSong(id = "3", title = "Beautiful Day", artist = "Pornstar / XXX Artist", source = AudioSource.YOUTUBE)
        assertFalse(ExplicitTitleFilter.shouldHideTitle(song.title))
        val result = ExplicitTitleFilter.filterOnline(listOf(song))
        assertEquals(1, result.size)
    }
}
