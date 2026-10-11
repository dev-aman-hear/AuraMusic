package com.aman.auramusic.online.filter

import com.aman.auramusic.online.model.OnlineSong
import java.text.Normalizer
import java.util.Locale

/**
 * Conservative title-only filter for explicit content in automatic Home
 * recommendations and ordinary Library listings.
 *
 * This is deliberately separate from TrendingSongFilter: compilation filtering
 * and explicit-title filtering solve different problems. Search and playback
 * must not call this utility globally.
 */
object ExplicitTitleFilter {
    private val explicitTitlePatterns = listOf(
        Regex("""\\b(?:porn|porno|pornography|xxx|hentai|blowjob|handjob|deepthroat|cumshot|orgasm|masturbat(?:e|ion|ing)|dildo|vibrator)\\b""", RegexOption.IGNORE_CASE),
        Regex("""\\b(?:fuck(?:ing|ed|er|ers)?|motherfucker|bitch(?:es)?|whore|slut)\\b""", RegexOption.IGNORE_CASE),
        Regex("""\\b(?:pussy|cock|dick|penis|vagina|boobs?|tits?)\\b""", RegexOption.IGNORE_CASE),
        Regex("""\\b(?:sex|erotic|nudes?|naked)\\s+(?:playlist|mix|songs?|audio|video|collection)\\b""", RegexOption.IGNORE_CASE),
        Regex("""\\b(?:adult|18\\+)\\s+(?:content|playlist|songs?|mix)\\b""", RegexOption.IGNORE_CASE)
    )

    fun normalize(title: String?): String {
        if (title.isNullOrBlank()) return ""
        return Normalizer.normalize(title, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("""[\\p{Punct}\\p{S}]+"""), " ")
            .replace(Regex("""\\s+"""), " ")
            .trim()
    }

    fun shouldHideTitle(title: String?): Boolean {
        val normalized = normalize(title)
        if (normalized.isBlank()) return false
        return explicitTitlePatterns.any { it.containsMatchIn(normalized) }
    }

    fun filterOnline(songs: List<OnlineSong>): List<OnlineSong> =
        songs.filterNot { shouldHideTitle(it.title) }
}
