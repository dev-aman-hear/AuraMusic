package com.aman.auramusic.online.filter

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import java.util.Locale

/**
 * Robust cross-section deduplication engine for AuraMusic Home Recommendations.
 * Ensures Daily Featured, Trending Now, and Quick Hits have zero overlapping songs.
 * Matches on normalized video/song IDs with normalized title/artist fallback.
 */
object RecommendationDeduplicator {

    fun normalizeText(text: String?): String {
        return TrendingSongFilter.normalizeTitle(text).lowercase(Locale.ROOT).trim()
    }

    fun getCanonicalKeys(song: OnlineSong): Set<String> {
        val keys = mutableSetOf<String>()
        val cleanId = song.id.trim()
        if (cleanId.isNotBlank()) {
            keys.add("id:$cleanId")
            keys.add("${song.source.name}:$cleanId")
        }

        val normTitle = normalizeText(song.title)
        val normArtist = normalizeText(song.artist)
        if (normTitle.isNotBlank()) {
            keys.add("meta:$normTitle|$normArtist")
            // Also add title only for unique long titles
            if (normTitle.length > 5) {
                keys.add("title:$normTitle")
            }
        }
        return keys
    }

    fun getCanonicalKeys(song: Song): Set<String> {
        val keys = mutableSetOf<String>()
        keys.add("local:${song.id}")

        val normTitle = normalizeText(song.title)
        val normArtist = normalizeText(song.artist)
        if (normTitle.isNotBlank()) {
            keys.add("meta:$normTitle|$normArtist")
            if (normTitle.length > 5) {
                keys.add("title:$normTitle")
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
