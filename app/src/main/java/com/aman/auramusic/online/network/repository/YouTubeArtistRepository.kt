package com.aman.auramusic.online.network.repository

import android.util.LruCache
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.piped.PipedService
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain model representing a discovered artist with real channel/profile metadata.
 */
data class YouTubeArtist(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val subscriberCountText: String? = null,
    val isVerified: Boolean = false,
    val isLocal: Boolean = false
)

/**
 * Dedicated repository abstraction for discovering YouTube artists and fetching ranked artist releases.
 */
interface YouTubeArtistRepository {
    suspend fun getTrendingArtists(): List<YouTubeArtist>
    suspend fun searchArtists(query: String): List<YouTubeArtist>
    suspend fun getArtistSongs(artistName: String, artistId: String? = null): List<OnlineSong>
}

@Singleton
class YouTubeArtistRepositoryImpl @Inject constructor(
    private val pipedService: PipedService,
    private val onlineRepository: OnlineMusicRepository
) : YouTubeArtistRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    companion object {
        private val artistCache = LruCache<String, List<YouTubeArtist>>(100)
        private val artistSongsCache = LruCache<String, List<OnlineSong>>(100)
        private val artistSearchCache = LruCache<String, List<YouTubeArtist>>(50)
    }

    override suspend fun getTrendingArtists(): List<YouTubeArtist> = withContext(Dispatchers.IO) {
        artistCache.get("trending_artists")?.let { return@withContext it }

        val discovered = mutableListOf<YouTubeArtist>()
        val seenNames = mutableSetOf<String>()

        // 1. Fetch from InnerTube music artist search / charts
        val innerTubeArtists = searchInnerTubeArtists("Top Artists Global Hits")
        innerTubeArtists.forEach { artist ->
            if (seenNames.add(artist.name.lowercase())) {
                discovered.add(artist)
            }
        }

        // 2. Fetch from trending tracks and extract real artist channels
        if (discovered.size < 12) {
            val trendingSongs = try {
                onlineRepository.getTrending(AudioSource.YOUTUBE)
            } catch (_: Exception) {
                emptyList()
            }

            trendingSongs.forEach { song ->
                val clean = extractPrimaryArtistName(song.artist)
                if (clean.isNotBlank() && seenNames.add(clean.lowercase())) {
                    discovered.add(
                        YouTubeArtist(
                            id = "yt_trend_${clean.hashCode()}",
                            name = clean,
                            profileImageUrl = song.artworkUrl,
                            subscriberCountText = "YouTube Music Chart",
                            isVerified = true
                        )
                    )
                }
            }
        }

        // 3. Fallback popular artists to ensure robust cloud presentation even if offline/limited network
        val fallback = getFallbackPopularArtists().filter { seenNames.add(it.name.lowercase()) }
        discovered.addAll(fallback)

        val result = discovered.take(24)
        if (result.isNotEmpty()) {
            artistCache.put("trending_artists", result)
        }
        result
    }

    override suspend fun searchArtists(query: String): List<YouTubeArtist> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        artistSearchCache.get(cleanQuery)?.let { return@withContext it }

        val results = searchInnerTubeArtists(cleanQuery)
        if (results.isNotEmpty()) {
            artistSearchCache.put(cleanQuery, results)
            return@withContext results
        }

        // Fallback: search songs and aggregate unique primary artist profiles
        val songs = try {
            onlineRepository.search(query, AudioSource.YOUTUBE).take(15)
        } catch (_: Exception) {
            emptyList()
        }

        val seen = mutableSetOf<String>()
        val derived = songs.mapNotNull { song ->
            val clean = extractPrimaryArtistName(song.artist)
            if (clean.isNotBlank() && clean.contains(cleanQuery, ignoreCase = true) && seen.add(clean.lowercase())) {
                YouTubeArtist(
                    id = "yt_search_${clean.hashCode()}",
                    name = clean,
                    profileImageUrl = song.artworkUrl,
                    subscriberCountText = "Artist",
                    isVerified = true
                )
            } else null
        }

        if (derived.isNotEmpty()) {
            artistSearchCache.put(cleanQuery, derived)
        }
        derived
    }

    override suspend fun getArtistSongs(artistName: String, artistId: String?): List<OnlineSong> = withContext(Dispatchers.IO) {
        val cacheKey = artistName.trim().lowercase()
        artistSongsCache.get(cacheKey)?.let { return@withContext it }

        // Fetch candidates from YouTube Music
        val candidates = try {
            val ytSongs = onlineRepository.search("$artistName songs", AudioSource.YOUTUBE).take(30)
            if (ytSongs.isNotEmpty()) ytSongs
            else onlineRepository.search(artistName, AudioSource.ALL).take(30)
        } catch (_: Exception) {
            emptyList()
        }

        // Apply intelligent ranking and noise filtering
        val ranked = filterAndRankArtistTracks(candidates, artistName)

        if (ranked.isNotEmpty()) {
            artistSongsCache.put(cacheKey, ranked)
        }
        ranked
    }

    /**
     * Filters out reaction videos, karaoke, instrumental loops, tutorials, and ranks official releases.
     */
    private fun filterAndRankArtistTracks(songs: List<OnlineSong>, artistName: String): List<OnlineSong> {
        val nameNorm = artistName.lowercase().trim()

        val excludedKeywords = listOf(
            "karaoke", "instrumental", "reaction", "reacting", "react to", "react ", "review",
            "tutorial", "how to play", "hour loop", "10 hour", "1 hour",
            "slowed + reverb", "slowed and reverb", "nightcore", "guitar lesson",
            "piano lesson", "bass boost", "8d audio", "mashup", "cover by"
        )

        val scored = songs.mapNotNull { song ->
            val titleLower = song.title.lowercase()
            val artistLower = song.artist.lowercase()

            // Filter out obvious unwanted uploads
            if (excludedKeywords.any { titleLower.contains(it) }) {
                return@mapNotNull null
            }

            var score = 0

            // Rank official channels or topic channels higher
            if (artistLower.contains(nameNorm)) score += 6
            if (artistLower.contains("- topic") || artistLower.contains("vevo") || artistLower.contains("official")) score += 4

            // Rank official video/audio releases higher
            if (titleLower.contains("official music video") || titleLower.contains("official audio") || titleLower.contains("official video")) {
                score += 5
            }

            // Normal duration music tracks (1:30 to 8:00)
            if (song.durationSeconds in 90..480) {
                score += 3
            }

            score to song
        }

        // Sort descending by score and deduplicate by normalized title
        val seenTitles = mutableSetOf<String>()
        return scored.sortedByDescending { it.first }
            .map { it.second }
            .filter { song ->
                val cleanTitle = song.title.lowercase()
                    .replace(Regex("""\(.*?(official|video|audio|lyrics|hd|4k).*?\)"""), "")
                    .replace(Regex("""\[.*?(official|video|audio|lyrics|hd|4k).*?\]"""), "")
                    .trim()
                seenTitles.add(cleanTitle)
            }
            .take(25)
    }

    /**
     * InnerTube API query targeting YouTube Music artists.
     */
    private fun searchInnerTubeArtists(query: String): List<YouTubeArtist> {
        return try {
            val payload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240101.01.00",
                            "hl": "en",
                            "gl": "US"
                        }
                    },
                    "query": "${query.replace("\"", "\\\"")}"
                }
            """.trimIndent()

            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = resp.body?.string() ?: return emptyList()
                parseInnerTubeArtistResults(json)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseInnerTubeArtistResults(jsonString: String): List<YouTubeArtist> {
        val artists = mutableListOf<YouTubeArtist>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            fun searchElements(elem: com.google.gson.JsonElement) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val renderer = obj.getAsJsonObject("musicResponsiveListItemRenderer")
                        val flex = renderer.getAsJsonArray("flexColumns")
                        var title = ""
                        var subtitle = ""

                        if (flex != null && flex.size() > 0) {
                            try {
                                title = flex[0].asJsonObject
                                    .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                    .getAsJsonObject("text")
                                    .getAsJsonArray("runs")[0].asJsonObject
                                    .get("text").asString
                            } catch (_: Throwable) {}
                        }

                        if (flex != null && flex.size() > 1) {
                            try {
                                subtitle = flex[1].asJsonObject
                                    .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                    .getAsJsonObject("text")
                                    .getAsJsonArray("runs")[0].asJsonObject
                                    .get("text").asString
                            } catch (_: Throwable) {}
                        }

                        val isArtistType = subtitle.contains("Artist", ignoreCase = true) ||
                                subtitle.contains("Subscribers", ignoreCase = true)

                        if (isArtistType && title.isNotBlank()) {
                            val thumb = try {
                                renderer.getAsJsonObject("thumbnail")
                                    ?.getAsJsonObject("musicThumbnailRenderer")
                                    ?.getAsJsonObject("thumbnail")
                                    ?.getAsJsonArray("thumbnails")
                                    ?.last()?.asJsonObject?.get("url")?.asString ?: ""
                            } catch (_: Throwable) { "" }

                            val cleanName = extractPrimaryArtistName(title)
                            if (cleanName.isNotBlank()) {
                                artists.add(
                                    YouTubeArtist(
                                        id = "yt_artist_${cleanName.hashCode()}",
                                        name = cleanName,
                                        profileImageUrl = thumb.ifBlank { null },
                                        subscriberCountText = subtitle.ifBlank { "YouTube Music Artist" },
                                        isVerified = true
                                    )
                                )
                            }
                        }
                    } else {
                        for (entry in obj.entrySet()) {
                            searchElements(entry.value)
                        }
                    }
                } else if (elem.isJsonArray) {
                    for (child in elem.asJsonArray) {
                        searchElements(child)
                    }
                }
            }
            searchElements(root)
        } catch (_: Exception) {}
        return artists
    }

    private fun extractPrimaryArtistName(raw: String): String {
        var clean = raw
            .replace(" - Topic", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .trim()

        clean = clean.split(Regex(""",|;|&|/|\||\s+feat\.?\s+|\s+ft\.?\s+|\s+with\s+|\s+and\s+|\s+x\s+""", RegexOption.IGNORE_CASE))
            .firstOrNull()?.trim() ?: clean

        val trimmed = clean.trim('"', '\'', '(', ')', '[', ']', '{', '}', '-', '_', '.', '~')
        val lower = trimmed.lowercase()

        val invalidNames = setOf(
            "unknown", "unknown artist", "various", "various artists", "various artist",
            "soundtrack", "ost", "compilation", "audio", "music", "video", "videos",
            "official video", "official audio", "clip", "channel", "top hits", "hits", "mix", "best of",
            "song", "songs", "track", "tracks", "album", "single", "singles"
        )

        if (invalidNames.contains(lower) || lower.startsWith("top ") || lower.endsWith(" hits") || lower.contains("202")) {
            return ""
        }

        return trimmed
    }

    private fun getFallbackPopularArtists(): List<YouTubeArtist> = listOf(
        YouTubeArtist("yt_arijit", "Arijit Singh", "https://c.saavncdn.com/artists/Arijit_Singh_002_20240321111624_500x500.jpg", "Official Artist Channel", true),
        YouTubeArtist("yt_theweeknd", "The Weeknd", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_diljit", "Diljit Dosanjh", "https://c.saavncdn.com/artists/Diljit_Dosanjh_003_20231025173154_500x500.jpg", "Official Artist Channel", true),
        YouTubeArtist("yt_taylor", "Taylor Swift", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_karan", "Karan Aujla", "https://c.saavncdn.com/artists/Karan_Aujla_006_20240410072535_500x500.jpg", "Official Artist Channel", true),
        YouTubeArtist("yt_bruno", "Bruno Mars", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_billie", "Billie Eilish", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_post", "Post Malone", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_dualipa", "Dua Lipa", "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80", "Official Artist Channel", true),
        YouTubeArtist("yt_shreya", "Shreya Ghoshal", "https://c.saavncdn.com/artists/Shreya_Ghoshal_004_20231117074404_500x500.jpg", "Official Artist Channel", true)
    )
}
