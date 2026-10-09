package com.aman.auramusic.online.filter

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import java.text.Normalizer
import java.util.Locale

/**
 * Result of evaluating a single track through the filter.
 */
data class FilterDecision(
    val song: OnlineSong,
    val isExcluded: Boolean,
    val reason: String? = null,
    val rankingScore: Double = 1.0,
    val matchedPattern: String? = null
)

/**
 * Output of filtering a collection of songs.
 */
data class TrendingFilterOutput(
    val filteredSongs: List<OnlineSong>,
    val decisions: List<FilterDecision>,
    val excludedCount: Int,
    val retainedCount: Int
)

/**
 * Smart Trending Song Filter for AuraMusic.
 *
 * Removes multi-song compilations, full albums, nonstop mixes, jukeboxes,
 * medleys, mashups, and excessive duration collections from the Trending section
 * while strictly preserving genuine individual songs, live performances,
 * classical compositions, and legitimate remixes.
 */
class TrendingSongFilter(
    val config: TrendingFilterConfig = TrendingFilterConfig()
) {

    companion object {
        /**
         * Parses duration strings (e.g. "3:45", "1:12:30") into total seconds.
         * Returns 0L for malformed, non-numeric, or missing duration strings.
         */
        fun parseDurationToSeconds(text: String?): Long {
            if (text.isNullOrBlank()) return 0L
            val clean = text.trim()
            val parts = clean.split(":")
            return when (parts.size) {
                2 -> {
                    val m = parts[0].trim().toLongOrNull() ?: return 0L
                    val s = parts[1].trim().toLongOrNull() ?: return 0L
                    (m * 60L) + s
                }
                3 -> {
                    val h = parts[0].trim().toLongOrNull() ?: return 0L
                    val m = parts[1].trim().toLongOrNull() ?: return 0L
                    val s = parts[2].trim().toLongOrNull() ?: return 0L
                    (h * 3600L) + (m * 60L) + s
                }
                else -> 0L
            }
        }

        /**
         * Normalizes a title with Unicode NFC normalization, collapses repeated whitespace,
         * and standardizes typographic quotes and dashes for resilient pattern matching.
         */
        fun normalizeTitle(title: String?): String {
            if (title.isNullOrBlank()) return ""
            val nfc = Normalizer.normalize(title, Normalizer.Form.NFC)
            return nfc
                .replace('’', '\'')
                .replace('‘', '\'')
                .replace('“', '"')
                .replace('”', '"')
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-')
                .replace(Regex("""\s+"""), " ")
                .trim()
        }
    }

    /**
     * High-confidence multi-song compilation patterns that warrant immediate exclusion
     * regardless of track duration. Compiled once with word boundaries.
     */
    private val compilationPatterns: List<Pair<String, Regex>> = listOf(
        "Full Album" to Regex("""\b(?:full|complete|entire)\s+album\b""", RegexOption.IGNORE_CASE),
        "Full Album Bracket" to Regex("""[\[\(]\s*full\s+album\s*[\]\)]""", RegexOption.IGNORE_CASE),
        "Non-Stop" to Regex("""\bnon[\s\-_]*stop\b""", RegexOption.IGNORE_CASE),
        "Duration in Title (Hours)" to Regex("""\b\d+\s*(?:hours?|hrs?)\b""", RegexOption.IGNORE_CASE),
        "Duration in Title (Word Hours)" to Regex("""\b(?:one|two|three|four)\s+hours?\b""", RegexOption.IGNORE_CASE),
        "Duration in Title (Long Minutes Mix)" to Regex("""\b(?:30|45|60|90|120)\s*(?:mins?|minutes?)\s+(?:mix|non[\s\-_]*stop|chill|study|sleep|workout|collection)\b""", RegexOption.IGNORE_CASE),
        "Jukebox" to Regex("""\bjukebox\b""", RegexOption.IGNORE_CASE),
        "All Songs Collection" to Regex("""\ball\s+songs(?:\s+collection|\s+jukebox)?\b""", RegexOption.IGNORE_CASE),
        "All Tracks Collection" to Regex("""\ball\s+tracks\b""", RegexOption.IGNORE_CASE),
        "Hits Collection" to Regex("""\bhits?\s+collection\b""", RegexOption.IGNORE_CASE),
        "Song Collection" to Regex("""\bsongs?\s+collection\b""", RegexOption.IGNORE_CASE),
        "Track Collection" to Regex("""\btrack\s+collection\b""", RegexOption.IGNORE_CASE),
        "Collection of Songs" to Regex("""\bcollection\s+of\s+songs\b""", RegexOption.IGNORE_CASE),
        "Complete Collection" to Regex("""\bcomplete\s+collection\b""", RegexOption.IGNORE_CASE),
        "Continuous Mix" to Regex("""\bcontinuous\s+mix\b""", RegexOption.IGNORE_CASE),
        "Continuous Play" to Regex("""\bcontinuous\s+play\b""", RegexOption.IGNORE_CASE),
        "Mega Mix" to Regex("""\bmega[\s\-_]*mix\b""", RegexOption.IGNORE_CASE),
        "Compilation" to Regex("""\bcompilation\b""", RegexOption.IGNORE_CASE),
        "Medley" to Regex("""\bmedley\b""", RegexOption.IGNORE_CASE),
        "Mashup" to Regex("""\bmash[\s\-_]*up\b""", RegexOption.IGNORE_CASE),
        "Merged Songs" to Regex("""\bmerged\s+songs?\b""", RegexOption.IGNORE_CASE),
        "Merged Audio" to Regex("""\bmerged\s+audio\b""", RegexOption.IGNORE_CASE),
        "Multi-song Count" to Regex("""\b(?:top|best|\d+)\s+\d+\s+(?:songs?|tracks?|hits?)\b""", RegexOption.IGNORE_CASE),
        "Greatest Hits Album" to Regex("""\bgreatest\s+hits\s+album\b""", RegexOption.IGNORE_CASE),
        "Discography" to Regex("""\bdiscography\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Supporting cues that, when paired with long duration (> 25 minutes), indicate
     * compilation content rather than extended singles or classical compositions.
     */
    private val suspiciousLongDurationCues: List<Regex> = listOf(
        Regex("""\b(?:best\s+of|top\s+hits|greatest\s+hits|all\s+time\s+hits)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:mix|mixtape|chillout|relaxing|study|sleep|workout)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:collection|playlist|anthology|vol(?:ume)?\.?\s*\d+)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:songs?|tracks?|hits?)\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Evaluates a single track against compilation patterns, duration boundaries, and metadata rules.
     */
    fun evaluateSong(song: OnlineSong): FilterDecision {
        val normalizedTitle = normalizeTitle(song.title)

        // 1. Direct compilation pattern detection in title
        for ((patternName, regex) in compilationPatterns) {
            if (regex.containsMatchIn(normalizedTitle)) {
                return FilterDecision(
                    song = song,
                    isExcluded = true,
                    reason = "Title matched compilation pattern: $patternName",
                    rankingScore = 0.0,
                    matchedPattern = patternName
                )
            }
        }

        // 2. Multi-item container metadata detection (e.g. albums/playlists returned as songs)
        if (song.album.equals("Playlist", ignoreCase = true) ||
            song.artist.equals("Various Artists", ignoreCase = true) && song.durationSeconds > config.softDurationLimitSec
        ) {
            return FilterDecision(
                song = song,
                isExcluded = true,
                reason = "Metadata indicates multi-item container or compilation artist",
                rankingScore = 0.0,
                matchedPattern = "Container Metadata"
            )
        }

        // 3. Balanced Duration Evaluation
        val duration = song.durationSeconds
        if (duration > 0L) {
            // Hard duration ceiling: over 45 minutes is virtually guaranteed to be a multi-song compilation
            if (duration >= config.hardDurationExclusionSec) {
                return FilterDecision(
                    song = song,
                    isExcluded = true,
                    reason = "Duration (${song.durationFormatted}) exceeds hard compilation limit of ${config.hardDurationExclusionSec / 60}m",
                    rankingScore = 0.1,
                    matchedPattern = "Duration > ${config.hardDurationExclusionSec / 60}m"
                )
            }

            // Duration between 25 and 45 minutes
            if (duration > config.softDurationLimitSec) {
                val hasSuspiciousCue = suspiciousLongDurationCues.any { it.containsMatchIn(normalizedTitle) }
                if (hasSuspiciousCue) {
                    return FilterDecision(
                        song = song,
                        isExcluded = true,
                        reason = "Duration (${song.durationFormatted}) exceeds 25m with compilation indicators in title",
                        rankingScore = 0.2,
                        matchedPattern = "Duration > 25m + compilation cue"
                    )
                }

                // Legitimate long track (e.g., extended live jam, classical movement): apply ranking penalty but RETAIN
                val penalty = if (song.source == AudioSource.YOUTUBE && config.strictYouTubeDurationPenalty) 0.50 else 0.70
                return FilterDecision(
                    song = song,
                    isExcluded = false,
                    rankingScore = penalty
                )
            }

            // Duration between 15 and 25 minutes: soft ranking penalty
            if (duration > config.maxDurationNoPenaltySec) {
                val penalty = if (song.source == AudioSource.YOUTUBE && config.strictYouTubeDurationPenalty) 0.85 else 0.95
                return FilterDecision(
                    song = song,
                    isExcluded = false,
                    rankingScore = penalty
                )
            }
        }

        // Standard track (<= 15 minutes or unknown duration): retain with full ranking score
        return FilterDecision(
            song = song,
            isExcluded = false,
            rankingScore = 1.0
        )
    }

    /**
     * Filters a list of online songs and returns detailed diagnostic output.
     */
    fun filter(songs: List<OnlineSong>): TrendingFilterOutput {
        // Step 1: Remove duplicates within the feed
        val deduplicated = deduplicate(songs)

        // Step 2: Evaluate each track
        val decisions = deduplicated.map { evaluateSong(it) }

        // Step 3: Partition into retained and excluded
        val retained = if (config.dryRun) {
            // Dry run mode: keep all tracks, but sort by rankingScore
            decisions.sortedByDescending { it.rankingScore }.map { it.song }
        } else {
            decisions.filter { !it.isExcluded }
                .sortedByDescending { it.rankingScore }
                .map { it.song }
        }

        val excludedCount = decisions.count { it.isExcluded }
        val retainedCount = retained.size

        return TrendingFilterOutput(
            filteredSongs = retained,
            decisions = decisions,
            excludedCount = excludedCount,
            retainedCount = retainedCount
        )
    }

    /**
     * Convenience method returning only the filtered, clean tracks.
     */
    fun filterSongs(songs: List<OnlineSong>): List<OnlineSong> {
        return filter(songs).filteredSongs
    }

    /**
     * Deduplicates tracks within the feed using stable provider-specific IDs.
     * Falls back to normalized title and artist if an ID is missing.
     * Prevents cross-provider ID collisions by prefixing the provider source.
     */
    fun deduplicate(songs: List<OnlineSong>): List<OnlineSong> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<OnlineSong>()

        for (song in songs) {
            val key = if (config.deduplicateAcrossProviders) {
                // Conservative cross-provider deduplication: normalized title + artist
                val normTitle = normalizeTitle(song.title).lowercase(Locale.ROOT)
                val normArtist = normalizeTitle(song.artist).lowercase(Locale.ROOT)
                "$normTitle|$normArtist"
            } else {
                // Provider-isolated deduplication: source + ID
                if (song.id.isNotBlank()) {
                    "${song.source.name}:${song.id.trim()}"
                } else {
                    val normTitle = normalizeTitle(song.title).lowercase(Locale.ROOT)
                    val normArtist = normalizeTitle(song.artist).lowercase(Locale.ROOT)
                    "${song.source.name}:$normTitle|$normArtist"
                }
            }

            if (seenKeys.add(key)) {
                result.add(song)
            }
        }
        return result
    }
}
