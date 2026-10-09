package com.aman.auramusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeArtistExtractionTest {

    private fun extractSingleArtistNames(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        var cleaned = raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()

        val parentheticalRegex = Regex("""[\(\[\{](?:feat\.?|ft\.?|with)\s+([^\)\]\}]+)[\)\]\}]""", RegexOption.IGNORE_CASE)
        val fromParens = mutableListOf<String>()
        parentheticalRegex.findAll(cleaned).forEach { match ->
            fromParens.add(match.groupValues[1])
        }
        cleaned = cleaned.replace(parentheticalRegex, " ")

        val splitRegex = Regex(""",|;|&|/|\||\s+feat\.?\s+|\s+ft\.?\s+|\s+with\s+|\s+and\s+|\s+x\s+|\s+vs\.?\s+""", RegexOption.IGNORE_CASE)
        val parts = cleaned.split(splitRegex)

        val invalidNames = setOf(
            "unknown", "unknown artist", "various", "various artists", "various artist",
            "soundtrack", "ost", "compilation", "audio", "music", "null", "undefined",
            "remix", "records", "producer", "feat", "ft", "instrumental"
        )

        return (parts + fromParens)
            .map { it.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_') }
            .filter { name ->
                name.length in 2..40 &&
                !invalidNames.contains(name.lowercase()) &&
                name.any { it.isLetter() }
            }
            .distinctBy { it.lowercase() }
    }

    @Test
    fun testCompositeArtistStringSeparation() {
        val result = extractSingleArtistNames("Anirudh Ravichander, Kaala Bhairava")
        assertEquals(2, result.size)
        assertEquals("Anirudh Ravichander", result[0])
        assertEquals("Kaala Bhairava", result[1])
    }

    @Test
    fun testFeaturedArtistSeparation() {
        val result = extractSingleArtistNames("Taylor Swift feat. Post Malone")
        assertEquals(2, result.size)
        assertEquals("Taylor Swift", result[0])
        assertEquals("Post Malone", result[1])
    }

    @Test
    fun testAmpersandAndTopicSuffix() {
        val result = extractSingleArtistNames("Ed Sheeran & Justin Bieber - Topic")
        assertEquals(2, result.size)
        assertEquals("Ed Sheeran", result[0])
        assertEquals("Justin Bieber", result[1])
    }

    @Test
    fun testInvalidArtistFiltering() {
        val result = extractSingleArtistNames("Unknown Artist, Various Artists, Arijit Singh")
        assertEquals(1, result.size)
        assertEquals("Arijit Singh", result[0])
    }

    @Test
    fun testSingleSoloArtistUnchanged() {
        val result = extractSingleArtistNames("Diljit Dosanjh")
        assertEquals(1, result.size)
        assertEquals("Diljit Dosanjh", result[0])
    }

    @Test
    fun testNoiseFilteringLogic() {
        val excludedKeywords = listOf(
            "karaoke", "instrumental", "reaction", "reacting", "react to", "react ", "review",
            "tutorial", "how to play", "hour loop", "10 hour", "1 hour",
            "slowed + reverb", "nightcore"
        )

        val officialVideoTitle = "Arijit Singh - Official Music Video"
        val reactionTitle = "Reacting to Arijit Singh live!"
        val karaokeTitle = "Arijit Singh track (Karaoke with lyrics)"

        assertFalse(excludedKeywords.any { officialVideoTitle.lowercase().contains(it) })
        assertTrue(excludedKeywords.any { reactionTitle.lowercase().contains(it) })
        assertTrue(excludedKeywords.any { karaokeTitle.lowercase().contains(it) })
    }
}
