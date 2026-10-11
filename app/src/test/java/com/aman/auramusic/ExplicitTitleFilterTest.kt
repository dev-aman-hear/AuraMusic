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
}
