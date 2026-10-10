package com.aman.auramusic.online.network.repository

import android.util.LruCache
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.piped.PipedService
import com.aman.auramusic.online.util.ArtworkQualityOptimizer
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

        // If chart search returns too few artists, query YouTube Music's artist
        // search directly. Never derive an artist avatar from a track thumbnail.
        if (discovered.size < 12) {
            val popularArtistQueries = listOf(
                "Arijit Singh", "Shreya Ghoshal", "Diljit Dosanjh", "Karan Aujla",
                "Taylor Swift", "The Weeknd", "Armaan Malik", "A. R. Rahman",
                "Pritam", "Anirudh Ravichander", "Billie Eilish", "Bruno Mars"
            )
            for (query in popularArtistQueries) {
                if (discovered.size >= 24) break
                searchInnerTubeArtists(query).forEach { artist ->
                    if (seenNames.add(artist.name.lowercase())) discovered.add(artist)
                }
            }
        }

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

        // Only return genuine YouTube Music artist entities. Song thumbnails are
        // release artwork, not artist portraits, so there is deliberately no song fallback.
        emptyList()
    }

    override suspend fun getArtistSongs(artistName: String, artistId: String?): List<OnlineSong> = withContext(Dispatchers.IO) {
        val cacheKey = artistName.trim().lowercase()
        artistSongsCache.get(cacheKey)?.let { return@withContext it }

        // Artist discography results must stay on YouTube Music. Do not silently
        // switch providers to JioSaavn when the YouTube Music request is empty.
        val candidates = try {
            onlineRepository.search("$artistName songs", AudioSource.YOUTUBE)
                .filter { it.source == AudioSource.YOUTUBE }
                .take(30)
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

    companion object {
        private val artistCache = LruCache<String, List<YouTubeArtist>>(100)
        private val artistSongsCache = LruCache<String, List<OnlineSong>>(100)
        private val artistSearchCache = LruCache<String, List<YouTubeArtist>>(50)

        fun parseInnerTubeArtistResults(jsonString: String): List<YouTubeArtist> {
            val artists = mutableListOf<YouTubeArtist>()
            val seenIds = mutableSetOf<String>()
            try {
                val root = JsonParser.parseString(jsonString).asJsonObject
                fun textOf(element: com.google.gson.JsonElement?): String {
                    if (element == null || !element.isJsonObject) return ""
                    val obj = element.asJsonObject
                    obj.get("simpleText")?.let { return it.asString.replace('\u00A0', ' ').trim() }
                    val runs = obj.getAsJsonArray("runs") ?: return ""
                    return runs.mapNotNull { it.asJsonObject.get("text")?.asString }
                        .joinToString("")
                        .replace('\u00A0', ' ')
                        .trim()
                }

                fun searchElements(elem: com.google.gson.JsonElement) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject

                        // 1. musicCardShelfRenderer (Top spotlight artist card)
                        if (obj.has("musicCardShelfRenderer")) {
                            val renderer = obj.getAsJsonObject("musicCardShelfRenderer")
                            val title = textOf(renderer.getAsJsonObject("title"))
                            val subtitle = textOf(renderer.getAsJsonObject("subtitle"))
                            val nav = renderer.getAsJsonObject("title")
                                ?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                ?: renderer.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                            val browseId = nav?.get("browseId")?.asString.orEmpty()
                            val pageType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                ?.get("pageType")?.asString.orEmpty()

                            val isNotSongOrAlbum = !pageType.contains("SONG", ignoreCase = true) &&
                                    !pageType.contains("VIDEO", ignoreCase = true) &&
                                    !pageType.contains("ALBUM", ignoreCase = true) &&
                                    !pageType.contains("PLAYLIST", ignoreCase = true)

                            val isArtist = isNotSongOrAlbum && (
                                pageType.contains("ARTIST", ignoreCase = true) ||
                                subtitle.contains("Artist", ignoreCase = true) ||
                                subtitle.contains("monthly audience", ignoreCase = true) ||
                                (browseId.startsWith("UC") && subtitle.contains("subscribers", ignoreCase = true))
                            )

                            if (isArtist && title.isNotBlank()) {
                                val cleanName = extractPrimaryArtistName(title)
                                if (cleanName.isNotBlank()) {
                                    val thumbs = renderer.getAsJsonObject("thumbnail")
                                        ?.getAsJsonObject("musicThumbnailRenderer")
                                        ?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonArray("thumbnails")
                                        ?: renderer.getAsJsonObject("thumbnailRenderer")
                                            ?.getAsJsonObject("musicThumbnailRenderer")
                                            ?.getAsJsonObject("thumbnail")
                                            ?.getAsJsonArray("thumbnails")
                                    val thumbUrl = try { thumbs?.last()?.asJsonObject?.get("url")?.asString?.trim().orEmpty() } catch (_: Throwable) { "" }
                                    val finalThumb = if (thumbUrl.isNotBlank()) ArtworkQualityOptimizer.optimizeUrl(thumbUrl).ifBlank { null } else null

                                    val artistId = browseId.ifBlank { "yt_artist_${cleanName.hashCode()}" }
                                    if (seenIds.add(artistId)) {
                                        artists.add(
                                            YouTubeArtist(
                                                id = artistId,
                                                name = cleanName,
                                                profileImageUrl = finalThumb,
                                                subscriberCountText = subtitle.ifBlank { "YouTube Music Artist" },
                                                isVerified = true
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 2. musicTwoRowItemRenderer (Artist grid items)
                        if (obj.has("musicTwoRowItemRenderer")) {
                            val renderer = obj.getAsJsonObject("musicTwoRowItemRenderer")
                            val title = textOf(renderer.getAsJsonObject("title"))
                            val subtitle = textOf(renderer.getAsJsonObject("subtitle"))
                            val nav = renderer.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                ?: renderer.getAsJsonObject("title")?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                    ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                            val browseId = nav?.get("browseId")?.asString.orEmpty()
                            val pageType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                ?.get("pageType")?.asString.orEmpty()

                            val isNotSongOrAlbum = !pageType.contains("SONG", ignoreCase = true) &&
                                    !pageType.contains("VIDEO", ignoreCase = true) &&
                                    !pageType.contains("ALBUM", ignoreCase = true) &&
                                    !pageType.contains("PLAYLIST", ignoreCase = true)

                            val isArtist = isNotSongOrAlbum && (
                                pageType.contains("ARTIST", ignoreCase = true) ||
                                subtitle.contains("Artist", ignoreCase = true) ||
                                subtitle.contains("monthly audience", ignoreCase = true) ||
                                (browseId.startsWith("UC") && subtitle.contains("subscribers", ignoreCase = true))
                            )

                            if (isArtist && title.isNotBlank()) {
                                val cleanName = extractPrimaryArtistName(title)
                                if (cleanName.isNotBlank()) {
                                    val thumbs = renderer.getAsJsonObject("thumbnailRenderer")
                                        ?.getAsJsonObject("musicThumbnailRenderer")
                                        ?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonArray("thumbnails")
                                    val thumbUrl = try { thumbs?.last()?.asJsonObject?.get("url")?.asString?.trim().orEmpty() } catch (_: Throwable) { "" }
                                    val finalThumb = if (thumbUrl.isNotBlank()) ArtworkQualityOptimizer.optimizeUrl(thumbUrl).ifBlank { null } else null

                                    val artistId = browseId.ifBlank { "yt_artist_${cleanName.hashCode()}" }
                                    if (seenIds.add(artistId)) {
                                        artists.add(
                                            YouTubeArtist(
                                                id = artistId,
                                                name = cleanName,
                                                profileImageUrl = finalThumb,
                                                subscriberCountText = subtitle.ifBlank { "YouTube Music Artist" },
                                                isVerified = true
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 3. musicResponsiveListItemRenderer
                        if (obj.has("musicResponsiveListItemRenderer")) {
                            val renderer = obj.getAsJsonObject("musicResponsiveListItemRenderer")

                            // Check if this is a track/song entity (must never treat song results as artists)
                            val isSongEntity = renderer.has("playlistItemData") ||
                                    renderer.getAsJsonObject("overlay")
                                        ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                                        ?.getAsJsonObject("content")
                                        ?.has("musicPlayButtonRenderer") == true

                            if (!isSongEntity) {
                                val flex = renderer.getAsJsonArray("flexColumns")
                                val title = if (flex != null && flex.size() > 0) {
                                    textOf(flex[0].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text"))
                                } else ""
                                val subtitle = if (flex != null && flex.size() > 1) {
                                    textOf(flex[1].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text"))
                                } else ""

                                val nav = renderer.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                    ?: flex?.firstOrNull()?.asJsonObject
                                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                        ?.getAsJsonObject("navigationEndpoint")
                                        ?.getAsJsonObject("browseEndpoint")

                                val browseId = nav?.get("browseId")?.asString.orEmpty()
                                val pageType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                    ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                    ?.get("pageType")?.asString.orEmpty()

                                val isNotSongOrAlbum = !pageType.contains("SONG", ignoreCase = true) &&
                                        !pageType.contains("VIDEO", ignoreCase = true) &&
                                        !pageType.contains("ALBUM", ignoreCase = true) &&
                                        !pageType.contains("PLAYLIST", ignoreCase = true)

                                val isArtist = isNotSongOrAlbum && (
                                    pageType.contains("ARTIST", ignoreCase = true) ||
                                    subtitle.contains("Artist", ignoreCase = true) ||
                                    subtitle.contains("monthly audience", ignoreCase = true) ||
                                    (browseId.startsWith("UC") && subtitle.contains("subscribers", ignoreCase = true))
                                )

                                if (isArtist && title.isNotBlank()) {
                                    val cleanName = extractPrimaryArtistName(title)
                                    if (cleanName.isNotBlank()) {
                                        val thumbs = renderer.getAsJsonObject("thumbnail")
                                            ?.getAsJsonObject("musicThumbnailRenderer")
                                            ?.getAsJsonObject("thumbnail")
                                            ?.getAsJsonArray("thumbnails")
                                            ?: renderer.getAsJsonObject("thumbnail")
                                                ?.getAsJsonObject("croppedSquareThumbnailRenderer")
                                                ?.getAsJsonObject("thumbnail")
                                                ?.getAsJsonArray("thumbnails")
                                        val thumbUrl = try { thumbs?.last()?.asJsonObject?.get("url")?.asString?.trim().orEmpty() } catch (_: Throwable) { "" }
                                        val finalThumb = if (thumbUrl.isNotBlank()) ArtworkQualityOptimizer.optimizeUrl(thumbUrl).ifBlank { null } else null

                                        val artistId = browseId.ifBlank { "yt_artist_${cleanName.hashCode()}" }
                                        if (seenIds.add(artistId)) {
                                            artists.add(
                                                YouTubeArtist(
                                                    id = artistId,
                                                    name = cleanName,
                                                    profileImageUrl = finalThumb,
                                                    subscriberCountText = subtitle.ifBlank { "YouTube Music Artist" },
                                                    isVerified = true
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        for (entry in obj.entrySet()) {
                            searchElements(entry.value)
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

        fun extractPrimaryArtistName(raw: String): String {
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
    }

    /**
     * InnerTube API query targeting YouTube Music artists.
     */
    private fun searchInnerTubeArtists(query: String): List<YouTubeArtist> {
        val directResults = queryInnerTube(query, filterArtists = true)
        if (directResults.isNotEmpty()) return directResults
        return queryInnerTube(query, filterArtists = false)
    }

    private fun queryInnerTube(query: String, filterArtists: Boolean): List<YouTubeArtist> {
        return try {
            val paramsClause = if (filterArtists) {
                """"params": "EgWKAQIgAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D","""
            } else ""

            val payload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20250601.01.00",
                            "hl": "en",
                            "gl": "IN"
                        }
                    },
                    $paramsClause
                    "query": "${query.replace("\\", "\\\\").replace("\"", "\\\"")}"
                }
            """.trimIndent()

            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
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
}
