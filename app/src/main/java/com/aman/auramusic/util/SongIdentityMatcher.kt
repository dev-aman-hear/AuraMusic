package com.aman.auramusic.util

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import kotlin.math.abs

/**
 * High-confidence metadata matcher unifying local library songs and online streaming tracks.
 * Adheres strictly to the rule:
 * - Only associate when metadata supports a confident match.
 * - Preserve distinct remixes, live recordings, covers, karaoke versions, and remasters.
 */
object SongIdentityMatcher {

    private val VERSION_TAGS = listOf(
        "remix",
        "live",
        "cover",
        "karaoke",
        "acoustic",
        "instrumental",
        "remaster",
        "remastered",
        "slowed",
        "reverb",
        "sped up",
        "tribute"
    )

    private val VIDEO_NOISE_REGEX = Regex(
        "(?i)\\s*[\\[(](official video|official audio|official music video|lyric video|lyrics|audio|4k|hd|visualizer|audio visualizer)[\\])]"
    )

    fun cleanTitle(title: String): String =
        title.replace(VIDEO_NOISE_REGEX, "")
            .lowercase()
            .replace(Regex("\\s*[\\[(](?:feat(?:uring)?|ft)\\.?\\s+[^\\])]+[\\])]"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun extractVersionTags(text: String): Set<String> {
        val normalized = " ${cleanTitle(text)} "
        return VERSION_TAGS.filter { tag ->
            normalized.contains(" ${tag.lowercase()} ") ||
                normalized.contains(" ${tag.lowercase().replace('-', ' ')} ")
        }.toSet()
    }

    private fun artistTokens(artist: String): Set<String> =
        artist.lowercase()
            .split(Regex("[,&/;]|\\bfeat(?:uring)?\\.?\\b|\\bft\\.?\\b|\\band\\b"))
            .map { cleanTitle(it) }
            .filter { it.length >= 2 && it !in setOf("unknown", "unknown artist", "various artists") }
            .toSet()

    fun isConfidentMatch(local: Song, online: OnlineSong): Boolean {
        // Exact deterministic IDs are the strongest available signal.
        val deterministicOnlineId =
            (online.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L)
        if (local.id == deterministicOnlineId || online.id == local.id.toString()) return true

        val localTitle = cleanTitle(local.title)
        val onlineTitle = cleanTitle(online.title)
        if (localTitle.isBlank() || onlineTitle.isBlank() || localTitle != onlineTitle) return false

        // Do not merge different editions such as remixes, live recordings, or remasters.
        if (extractVersionTags(local.title) != extractVersionTags(online.title)) return false

        val localDurationSec = local.duration / 1000L
        if (localDurationSec > 0 && online.durationSeconds > 0 &&
            abs(localDurationSec - online.durationSeconds) > 10L
        ) return false

        // Require artist evidence unless both names are unavailable. This avoids
        // matching unrelated songs that happen to share a title.
        val localArtists = artistTokens(local.artist)
        val onlineArtists = artistTokens(online.artist)
        if (localArtists.isEmpty() || onlineArtists.isEmpty()) return false

        return localArtists.intersect(onlineArtists).isNotEmpty()
    }

    fun findOnlineMatch(song: Song, candidates: List<OnlineSong>): OnlineSong? {
        if (candidates.isEmpty()) return null
        val deterministicId = song.id
        // Priority 1: Exact deterministic ID
        candidates.find { (it.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L) == deterministicId }?.let {
            return it
        }
        // Priority 2: Confident metadata match
        return candidates.find { isConfidentMatch(song, it) }
    }

    /**
     * Filters album tracklists to eliminate unrelated search noise, distinct edition mixing,
     * or third-party covers while preserving legitimate repeated tracks (e.g. multi-part or reprise tracks).
     */
    fun filterAlbumTracks(
        targetAlbumName: String,
        targetArtistName: String?,
        rawTracks: List<OnlineSong>
    ): List<OnlineSong> {
        val cleanTargetAlbum = targetAlbumName.trim().lowercase()
        val isTargetLive = cleanTargetAlbum.contains("live")
        val isTargetAcoustic = cleanTargetAlbum.contains("acoustic")

        return rawTracks.filter { song ->
            val songAlbum = song.album.trim().lowercase()
            val songTitle = song.title.trim().lowercase()

            // If song album is populated, verify album compatibility
            if (songAlbum.isNotBlank() && songAlbum != "online stream") {
                val albumMatches = songAlbum == cleanTargetAlbum ||
                        songAlbum.contains(cleanTargetAlbum) ||
                        cleanTargetAlbum.contains(songAlbum)
                if (!albumMatches) return@filter false
            }

            // Exclude live / acoustic / karaoke covers if the targeted album is not that edition
            if (!isTargetLive && songTitle.contains("live")) return@filter false
            if (!isTargetAcoustic && songTitle.contains("acoustic")) return@filter false
            if (songTitle.contains("karaoke") || songTitle.contains("tribute")) return@filter false

            // Artist consistency if primary artist is known
            if (!targetArtistName.isNullOrBlank() && targetArtistName != "Various Artists") {
                val cleanArtist = targetArtistName.trim().lowercase()
                val songArtist = song.artist.trim().lowercase()
                if (!songArtist.contains(cleanArtist) && !cleanArtist.contains(songArtist)) {
                    // Check if album metadata or title indicates featuring or compilation
                    if (!song.title.lowercase().contains(cleanArtist)) {
                        return@filter false
                    }
                }
            }

            true
        }
    }
}
