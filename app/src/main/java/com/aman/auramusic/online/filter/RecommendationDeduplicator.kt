package com.aman.auramusic.online.filter

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import java.util.Locale

/**
 * Robust cross-section deduplication engine for AuraMusic Home Recommendations.
 * Ensures Daily Featured, Trending Now, and Quick Hits have zero overlapping songs
 * without accidentally hiding different songs that share a title.
 *
 * Matching strategy:
 * 1. Prefer stable provider-scoped song IDs to avoid collisions between unrelated IDs.
 * 2. Fall back to normalized (title + artist) metadata matching.
 * 3. Never deduplicate solely on title.
 * 4. Safely handle missing artist names, blank titles, punctuation, whitespace, and capitalization.
 */
object RecommendationDeduplicator {

    private val genericPlaceholders = setOf(
        "unknown", "unknown artist", "<unknown>", "various", "various artists",
        "various artist", "soundtrack", "ost", "compilation", "null", "undefined",
        "untitled", "none"
    )

    private val mediaNoiseRegex = Regex(
        """\s*[\(\[]\s*(?:official\s+)?(?:music\s+)?(?:video|audio|lyric\s+video|visualizer|audio\s+track|hq|hd)\s*[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    fun normalizeText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val clean = TrendingSongFilter.normalizeTitle(text)
            .lowercase(Locale.ROOT)
            .trim()
        return clean.trim('"', '\'', '“', '”', '‘', '’', '-', '—', '–', ':', ';', '.', ',')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun isInformativeTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val clean = title.trim().lowercase(Locale.ROOT)
        return clean.isNotBlank() && !genericPlaceholders.contains(clean) && clean.any { it.isLetterOrDigit() }
    }

    fun isInformativeArtist(artist: String?): Boolean {
        if (artist.isNullOrBlank()) return false
        val clean = artist.trim().lowercase(Locale.ROOT)
        return clean.length >= 2 && !genericPlaceholders.contains(clean) && clean.any { it.isLetterOrDigit() }
    }

    private fun stripMediaNoiseTags(title: String): String {
        return title.replace(mediaNoiseRegex, "").trim()
    }

    private fun cleanPunctuation(text: String): String {
        return text
            .replace(Regex("""[^\p{L}\p{N}\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun getCanonicalKeys(song: OnlineSong): Set<String> {
        val keys = mutableSetOf<String>()
        val cleanId = song.id.trim()
        if (cleanId.isNotBlank()) {
            // Scope ID by provider source to prevent cross-catalog collisions
            keys.add("${song.source.name}:$cleanId")
            // If the ID matches a standard 11-char YouTube video ID, also register YOUTUBE key
            if (cleanId.length == 11 && cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                keys.add("YOUTUBE:$cleanId")
            }
        }

        val normTitle = normalizeText(song.title)
        val normArtist = normalizeText(song.artist)

        // Metadata fallback: only deduplicate when BOTH title and artist are informative and reliable
        if (isInformativeTitle(normTitle) && isInformativeArtist(normArtist)) {
            keys.add("meta:$normTitle|$normArtist")

            val strippedTitle = stripMediaNoiseTags(normTitle)
            if (strippedTitle != normTitle && isInformativeTitle(strippedTitle)) {
                keys.add("meta:$strippedTitle|$normArtist")
            }

            val cleanTitle = cleanPunctuation(normTitle)
            val cleanArtist = cleanPunctuation(normArtist)
            if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                keys.add("meta:$cleanTitle|$cleanArtist")
                if (strippedTitle != normTitle) {
                    val cleanStrippedTitle = cleanPunctuation(strippedTitle)
                    if (cleanStrippedTitle.isNotBlank()) {
                        keys.add("meta:$cleanStrippedTitle|$cleanArtist")
                    }
                }
            }
        }
        return keys
    }

    fun getCanonicalKeys(song: Song): Set<String> {
        val keys = mutableSetOf<String>()
        keys.add("local:${song.id}")

        val normTitle = normalizeText(song.title)
        val normArtist = normalizeText(song.artist)

        // Metadata fallback: only deduplicate when BOTH title and artist are informative and reliable
        if (isInformativeTitle(normTitle) && isInformativeArtist(normArtist)) {
            keys.add("meta:$normTitle|$normArtist")

            val strippedTitle = stripMediaNoiseTags(normTitle)
            if (strippedTitle != normTitle && isInformativeTitle(strippedTitle)) {
                keys.add("meta:$strippedTitle|$normArtist")
            }

            val cleanTitle = cleanPunctuation(normTitle)
            val cleanArtist = cleanPunctuation(normArtist)
            if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                keys.add("meta:$cleanTitle|$cleanArtist")
                if (strippedTitle != normTitle) {
                    val cleanStrippedTitle = cleanPunctuation(strippedTitle)
                    if (cleanStrippedTitle.isNotBlank()) {
                        keys.add("meta:$cleanStrippedTitle|$cleanArtist")
                    }
                }
            }
        }
        return keys
    }
}

/**
 * Stateful pool tracker enforcing mutual exclusivity across recommendation rails.
 */
class SectionDeduplicator {
    private val seenKeys = mutableSetOf<String>()

    @Synchronized
    fun isDuplicate(song: OnlineSong): Boolean {
        val keys = RecommendationDeduplicator.getCanonicalKeys(song)
        return keys.any { it in seenKeys }
    }

    @Synchronized
    fun isDuplicate(song: Song): Boolean {
        val keys = RecommendationDeduplicator.getCanonicalKeys(song)
        return keys.any { it in seenKeys }
    }

    @Synchronized
    fun allocate(song: OnlineSong): Boolean {
        val keys = RecommendationDeduplicator.getCanonicalKeys(song)
        if (keys.any { it in seenKeys }) {
            return false
        }
        seenKeys.addAll(keys)
        return true
    }

    @Synchronized
    fun allocate(song: Song): Boolean {
        val keys = RecommendationDeduplicator.getCanonicalKeys(song)
        if (keys.any { it in seenKeys }) {
            return false
        }
        seenKeys.addAll(keys)
        return true
    }

    @Synchronized
    fun allocateAll(songs: List<OnlineSong>): List<OnlineSong> {
        val result = mutableListOf<OnlineSong>()
        for (song in songs) {
            if (allocate(song)) {
                result.add(song)
            }
        }
        return result
    }

    @Synchronized
    fun allocateAllLocal(songs: List<Song>): List<Song> {
        val result = mutableListOf<Song>()
        for (song in songs) {
            if (allocate(song)) {
                result.add(song)
            }
        }
        return result
    }

    @Synchronized
    fun clear() {
        seenKeys.clear()
    }
}
