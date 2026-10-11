package com.aman.auramusic.online.network.repository

import android.util.LruCache
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.piped.PipedService
import com.aman.auramusic.online.util.ArtworkQualityOptimizer
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
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
 * Holds songs and genuine releases (albums, singles, EPs) extracted from an artist's browse page.
 */
data class ArtistCatalogue(
    val songs: List<OnlineSong> = emptyList(),
    val albums: List<OnlinePlaylist> = emptyList()
)

/**
 * Dedicated repository abstraction for discovering YouTube artists and fetching ranked artist releases.
 */
interface YouTubeArtistRepository {
    suspend fun getTrendingArtists(): List<YouTubeArtist>
    suspend fun searchArtists(query: String): List<YouTubeArtist>
    suspend fun getArtistSongs(artistName: String, artistId: String? = null): List<OnlineSong>
    suspend fun getArtistCatalogue(artistName: String, artistId: String? = null): ArtistCatalogue
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

    override suspend fun getArtistSongs(artistName: String, artistId: String?): List<OnlineSong> =
        getArtistCatalogue(artistName, artistId).songs

    override suspend fun getArtistCatalogue(artistName: String, artistId: String?): ArtistCatalogue = withContext(Dispatchers.IO) {
        val browseId = artistId?.trim().orEmpty()
        // A real YouTube Music browse ID is required. Never substitute a broad text search:
        // it mixes covers, fan uploads, unrelated artists, and compilations into an artist page.
        if (browseId.isBlank() || browseId.startsWith("local_") || browseId.startsWith("yt_artist_")) {
            return@withContext ArtistCatalogue()
        }

        val cacheKey = "browse:$browseId"
        artistCatalogueCache.get(cacheKey)?.let { return@withContext it }

        val catalogue = try {
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
                  "browseId": "${browseId.replace("\\", "\\\\").replace("\"", "\\\"")}"
                }
            """.trimIndent()
            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .header("Content-Type", "application/json")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use ArtistCatalogue()
                val json = response.body?.string().orEmpty()
                val tracks = parseArtistBrowseTracks(json, artistName)
                val albums = parseArtistBrowseAlbums(json)
                ArtistCatalogue(tracks, albums)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ArtistCatalogue()
        }

        if (catalogue.songs.isNotEmpty() || catalogue.albums.isNotEmpty()) {
            artistCatalogueCache.put(cacheKey, catalogue)
        }
        catalogue
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
        private val artistCatalogueCache = LruCache<String, ArtistCatalogue>(100)
        private val artistSearchCache = LruCache<String, List<YouTubeArtist>>(50)

        fun getCachedArtist(artistName: String): YouTubeArtist? {
            val clean = artistName.trim().lowercase()
            if (clean.isBlank()) return null
            val fromSearch = artistSearchCache.get(clean)?.find { it.name.trim().equals(clean, ignoreCase = true) }
            if (fromSearch != null && !fromSearch.profileImageUrl.isNullOrBlank()) return fromSearch
            artistCache.snapshot().values.forEach { list ->
                val match = list.find { it.name.trim().equals(clean, ignoreCase = true) }
                if (match != null && !match.profileImageUrl.isNullOrBlank()) return match
            }
            return null
        }

        /**
         * Reads track rows from the selected artist's actual YouTube Music browse response.
         * Extracts genuine title, artist, album, duration, and thumbnail without falling back
         * to broad keyword search.
         */
        fun parseArtistBrowseTracks(jsonString: String, artistName: String): List<OnlineSong> {
            val result = mutableListOf<OnlineSong>()
            try {
                val root = JsonParser.parseString(jsonString)
                fun textOf(element: com.google.gson.JsonElement?): String {
                    if (element == null || !element.isJsonObject) return ""
                    val obj = element.asJsonObject
                    obj.get("simpleText")?.let { return it.asString.replace('\u00A0', ' ').trim() }
                    return obj.getAsJsonArray("runs")?.mapNotNull { run ->
                        run.asJsonObject.get("text")?.asString
                    }?.joinToString("")?.replace('\u00A0', ' ')?.trim().orEmpty()
                }
                fun walk(element: com.google.gson.JsonElement) {
                    if (element.isJsonArray) {
                        element.asJsonArray.forEach(::walk)
                        return
                    }
                    if (!element.isJsonObject) return
                    val obj = element.asJsonObject
                    val renderer = obj.getAsJsonObject("musicResponsiveListItemRenderer")
                    if (renderer != null) {
                        val flex = renderer.getAsJsonArray("flexColumns")
                        var videoId = renderer.getAsJsonObject("playlistItemData")?.get("videoId")?.asString.orEmpty()
                        if (videoId.isBlank()) {
                            videoId = renderer.getAsJsonObject("navigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString.orEmpty()
                        }
                        if (videoId.isBlank()) {
                            videoId = renderer.getAsJsonObject("overlay")
                                ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                                ?.getAsJsonObject("content")
                                ?.getAsJsonObject("musicPlayButtonRenderer")
                                ?.getAsJsonObject("playNavigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString.orEmpty()
                        }

                        if (videoId.isNotBlank() && flex != null && flex.size() > 0) {
                            val title = textOf(
                                flex[0].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.getAsJsonObject("text")
                            )

                            var songArtist = artistName
                            var songAlbum = ""
                            if (flex.size() > 1) {
                                val col1Text = textOf(
                                    flex[1].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                )
                                if (col1Text.isNotBlank()) {
                                    songArtist = col1Text
                                }
                            }

                            if (flex.size() > 3) {
                                val col3Text = textOf(
                                    flex[3].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                )
                                if (col3Text.isNotBlank()) {
                                    songAlbum = col3Text
                                }
                            }
                            if (songAlbum.isBlank()) {
                                for (colIndex in 1 until flex.size()) {
                                    val runs = flex[colIndex].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs") ?: continue
                                    for (run in runs) {
                                        val runObj = run.asJsonObject
                                        val bId = runObj.getAsJsonObject("navigationEndpoint")
                                            ?.getAsJsonObject("browseEndpoint")
                                            ?.get("browseId")?.asString.orEmpty()
                                        if (bId.startsWith("MPRE")) {
                                            songAlbum = runObj.get("text")?.asString.orEmpty().trim()
                                            break
                                        }
                                    }
                                    if (songAlbum.isNotBlank()) break
                                }
                            }

                            val thumb = renderer.getAsJsonObject("thumbnail")
                                ?.getAsJsonObject("musicThumbnailRenderer")
                                ?.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                                ?.lastOrNull()?.asJsonObject?.get("url")?.asString.orEmpty()

                            var durationSeconds = 0L
                            val fixedColumns = renderer.getAsJsonArray("fixedColumns")
                            if (fixedColumns != null) {
                                for (column in fixedColumns) {
                                    val text = textOf(column.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                                        ?.getAsJsonObject("text"))
                                    val seconds = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(text)
                                    if (seconds > 0) { durationSeconds = seconds; break }
                                }
                            }
                            if (durationSeconds == 0L) {
                                for (colIndex in 1 until flex.size()) {
                                    val text = textOf(flex[colIndex].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text"))
                                    val seconds = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(text)
                                    if (seconds > 0) { durationSeconds = seconds; break }
                                }
                            }

                            if (title.isNotBlank()) {
                                result.add(
                                    OnlineSong(
                                        id = videoId,
                                        title = title,
                                        artist = songArtist,
                                        album = songAlbum,
                                        artworkUrl = ArtworkQualityOptimizer.optimizeUrl(thumb)
                                            .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                                        durationSeconds = durationSeconds,
                                        source = AudioSource.YOUTUBE,
                                        bitrate = "Adaptive",
                                        format = "M4A/Opus"
                                    )
                                )
                            }
                        }
                    }
                    obj.entrySet().forEach { (_, child) -> walk(child) }
                }
                walk(root)
            } catch (_: Exception) {
                return emptyList()
            }
            return result.distinctBy { it.id }.take(50)
        }

        /**
         * Reads real album and single releases from the artist's InnerTube browse response.
         */
        fun parseArtistBrowseAlbums(jsonString: String): List<OnlinePlaylist> {
            val result = mutableListOf<OnlinePlaylist>()
            val seenIds = mutableSetOf<String>()
            try {
                val root = JsonParser.parseString(jsonString)
                fun textOf(element: com.google.gson.JsonElement?): String {
                    if (element == null || !element.isJsonObject) return ""
                    val obj = element.asJsonObject
                    obj.get("simpleText")?.let { return it.asString.replace('\u00A0', ' ').trim() }
                    return obj.getAsJsonArray("runs")?.mapNotNull { run ->
                        run.asJsonObject.get("text")?.asString
                    }?.joinToString("")?.replace('\u00A0', ' ')?.trim().orEmpty()
                }

                fun walk(element: com.google.gson.JsonElement) {
                    if (element.isJsonArray) {
                        element.asJsonArray.forEach(::walk)
                        return
                    }
                    if (!element.isJsonObject) return
                    val obj = element.asJsonObject

                    val twoRow = obj.getAsJsonObject("musicTwoRowItemRenderer")
                    if (twoRow != null) {
                        val nav = twoRow.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                            ?: twoRow.getAsJsonObject("title")?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                        val bId = nav?.get("browseId")?.asString.orEmpty()
                        val pType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                            ?.getAsJsonObject("browseEndpointContextMusicConfig")
                            ?.get("pageType")?.asString.orEmpty()

                        val isAlbumOrSingle = bId.isNotBlank() && (
                            bId.startsWith("MPRE") ||
                            pType.contains("ALBUM", ignoreCase = true) ||
                            pType.contains("SINGLE", ignoreCase = true)
                        )
                        if (isAlbumOrSingle && seenIds.add(bId)) {
                            val title = textOf(twoRow.getAsJsonObject("title"))
                            val subtitle = textOf(twoRow.getAsJsonObject("subtitle"))
                            val thumbs = twoRow.getAsJsonObject("thumbnailRenderer")
                                ?.getAsJsonObject("musicThumbnailRenderer")
                                ?.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                            val artwork = try { thumbs?.lastOrNull()?.asJsonObject?.get("url")?.asString.orEmpty() } catch (_: Exception) { "" }

                            if (title.isNotBlank()) {
                                result.add(
                                    OnlinePlaylist(
                                        id = "ytmusic:$bId",
                                        title = title,
                                        subtitle = subtitle,
                                        artworkUrl = ArtworkQualityOptimizer.optimizeUrl(artwork),
                                        songCount = 0,
                                        source = AudioSource.YOUTUBE
                                    )
                                )
                            }
                        }
                    }

                    obj.entrySet().forEach { (_, child) -> walk(child) }
                }
                walk(root)
            } catch (_: Exception) {
                return emptyList()
            }
            return result
        }

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
