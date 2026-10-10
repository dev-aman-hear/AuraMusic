package com.aman.auramusic.online.network.piped

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.OnlinePlaylist
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

/**
 * Service interacting with public Piped instances to search and stream tracks from YouTube Music.
 * Features automated multi-instance failover as implemented in SimpMusic and RiMusic.
 */
class PipedService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    // Curated high-availability Piped instances with automatic failover
    private val instances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.adminforge.de",
        "https://piped-api.privacy.com.de",
        "https://api.piped.yt"
    )

    private var activeInstanceIndex = 0

    private val currentBaseUrl: String
        get() = instances[activeInstanceIndex]

    private fun rotateInstance() {
        activeInstanceIndex = (activeInstanceIndex + 1) % instances.size
    }

    suspend fun searchSongs(query: String): List<OnlineSong> = withContext(Dispatchers.IO) {
        val innertubeResults = searchInnertube(query)
        if (innertubeResults.isNotEmpty()) {
            return@withContext innertubeResults
        }

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        for (attempt in instances.indices) {
            val url = "$currentBaseUrl/search?q=$encoded&filter=music_songs"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val parsed = parsePipedSearchResults(body)
                        if (parsed.isNotEmpty()) return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                rotateInstance()
            }
        }
        emptyList()
    }

    /**
     * Search actual YouTube Music album/playlist cards via InnerTube.
     * Unlike song search, this preserves the provider's collection ID so we can
     * browse the collection and load its real track list.
     */
    suspend fun searchMusicCollections(query: String, limit: Int = 12): List<OnlinePlaylist> =
        withContext(Dispatchers.IO) {
            try {
                val payload = """
                    {
                      "context": {"client": {"clientName":"WEB_REMIX","clientVersion":"1.20250601.01.00","hl":"en","gl":"IN"}},
                      "query": "${query.replace("\\", "\\\\").replace("\"", "\\\"")}"
                    }
                """.trimIndent()
                val request = Request.Builder()
                    .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://music.youtube.com")
                    .header("Referer", "https://music.youtube.com/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val root = JsonParser.parseString(response.body?.string() ?: return@withContext emptyList())
                    val found = linkedMapOf<String, OnlinePlaylist>()
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
                    fun walk(element: com.google.gson.JsonElement) {
                        if (found.size >= limit * 3) return
                        if (element.isJsonObject) {
                            val obj = element.asJsonObject

                            // 1. musicTwoRowItemRenderer (standard album/playlist grid cards)
                            val renderer = obj.getAsJsonObject("musicTwoRowItemRenderer")
                            if (renderer != null) {
                                val title = textOf(renderer.getAsJsonObject("title"))
                                val subtitle = textOf(renderer.getAsJsonObject("subtitle"))
                                val navigation = renderer.getAsJsonObject("navigationEndpoint")
                                    ?.getAsJsonObject("browseEndpoint")
                                    ?: renderer.getAsJsonObject("title")?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                        ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                val browseId = navigation?.get("browseId")?.asString.orEmpty()
                                val pageType = navigation?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                    ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                    ?.get("pageType")?.asString.orEmpty()
                                val thumbnails = renderer.getAsJsonObject("thumbnailRenderer")
                                    ?.getAsJsonObject("musicThumbnailRenderer")
                                    ?.getAsJsonObject("thumbnail")
                                    ?.getAsJsonArray("thumbnails")
                                val artwork = try { thumbnails?.last()?.asJsonObject?.get("url")?.asString.orEmpty() } catch (_: Exception) { "" }
                                val looksLikeCollection = browseId.isNotBlank() &&
                                    (pageType.contains("ALBUM", true) || pageType.contains("PLAYLIST", true) ||
                                     browseId.startsWith("MPRE") || browseId.startsWith("VL") ||
                                     browseId.startsWith("OLAK"))
                                if (looksLikeCollection && title.isNotBlank()) {
                                    found.putIfAbsent(browseId, OnlinePlaylist(
                                        id = "ytmusic:$browseId",
                                        title = title,
                                        subtitle = subtitle,
                                        artworkUrl = ArtworkQualityOptimizer.optimizeUrl(artwork),
                                        songCount = 0,
                                        source = AudioSource.YOUTUBE
                                    ))
                                }
                            }

                            // 2. musicCardShelfRenderer (Top result album spotlight card)
                            val cardRenderer = obj.getAsJsonObject("musicCardShelfRenderer")
                            if (cardRenderer != null) {
                                val nav = cardRenderer.getAsJsonObject("title")
                                    ?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                    ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                    ?: cardRenderer.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                                val bId = nav?.get("browseId")?.asString.orEmpty()
                                val pType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                    ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                    ?.get("pageType")?.asString.orEmpty()
                                val isCollection = bId.isNotBlank() && (
                                    pType.contains("ALBUM", true) || pType.contains("PLAYLIST", true) ||
                                    bId.startsWith("MPRE") || bId.startsWith("VL") || bId.startsWith("OLAK")
                                )
                                if (isCollection) {
                                    val title = textOf(cardRenderer.getAsJsonObject("title"))
                                    val subtitle = textOf(cardRenderer.getAsJsonObject("subtitle"))
                                    val thumbs = cardRenderer.getAsJsonObject("thumbnail")
                                        ?.getAsJsonObject("musicThumbnailRenderer")
                                        ?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonArray("thumbnails")
                                    val artwork = try { thumbs?.last()?.asJsonObject?.get("url")?.asString.orEmpty() } catch (_: Exception) { "" }
                                    if (title.isNotBlank()) {
                                        found.putIfAbsent(bId, OnlinePlaylist(
                                            id = "ytmusic:$bId",
                                            title = title,
                                            subtitle = subtitle,
                                            artworkUrl = ArtworkQualityOptimizer.optimizeUrl(artwork),
                                            songCount = 0,
                                            source = AudioSource.YOUTUBE
                                        ))
                                    }
                                }
                            }

                            // 3. musicResponsiveListItemRenderer (list item collections)
                            val responsiveRenderer = obj.getAsJsonObject("musicResponsiveListItemRenderer")
                            if (responsiveRenderer != null) {
                                val nav = responsiveRenderer.getAsJsonObject("navigationEndpoint")
                                    ?.getAsJsonObject("browseEndpoint")
                                    ?: responsiveRenderer.getAsJsonArray("flexColumns")?.firstOrNull()?.asJsonObject
                                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
                                        ?.getAsJsonObject("navigationEndpoint")
                                        ?.getAsJsonObject("browseEndpoint")
                                val bId = nav?.get("browseId")?.asString.orEmpty()
                                val pType = nav?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                                    ?.getAsJsonObject("browseEndpointContextMusicConfig")
                                    ?.get("pageType")?.asString.orEmpty()
                                val isCollection = bId.isNotBlank() && (
                                    pType.contains("ALBUM", true) || pType.contains("PLAYLIST", true) ||
                                    bId.startsWith("MPRE") || bId.startsWith("VL") || bId.startsWith("OLAK")
                                )
                                if (isCollection) {
                                    val flex = responsiveRenderer.getAsJsonArray("flexColumns")
                                    val title = if (flex != null && flex.size() > 0) {
                                        textOf(flex[0].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text"))
                                    } else ""
                                    val subtitle = if (flex != null && flex.size() > 1) {
                                        textOf(flex[1].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text"))
                                    } else ""
                                    val thumbs = responsiveRenderer.getAsJsonObject("thumbnail")
                                        ?.getAsJsonObject("musicThumbnailRenderer")
                                        ?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonArray("thumbnails")
                                    val artwork = try { thumbs?.last()?.asJsonObject?.get("url")?.asString.orEmpty() } catch (_: Exception) { "" }
                                    if (title.isNotBlank()) {
                                        found.putIfAbsent(bId, OnlinePlaylist(
                                            id = "ytmusic:$bId",
                                            title = title,
                                            subtitle = subtitle,
                                            artworkUrl = ArtworkQualityOptimizer.optimizeUrl(artwork),
                                            songCount = 0,
                                            source = AudioSource.YOUTUBE
                                        ))
                                    }
                                }
                            }
                            for ((_, child) in obj.entrySet()) walk(child)
                        } else if (element.isJsonArray) {
                            element.asJsonArray.forEach(::walk)
                        }
                    }
                    walk(root)
                    found.values.take(limit)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    /** Browse a YouTube Music album/playlist and return the tracks actually in it. */
    suspend fun getMusicCollectionSongs(collection: OnlinePlaylist): List<OnlineSong> =
        withContext(Dispatchers.IO) {
            val browseId = collection.id.substringAfter("ytmusic:").trim()
            if (browseId.isBlank()) return@withContext emptyList()
            try {
                val payload = """
                    {
                      "context": {"client": {"clientName":"WEB_REMIX","clientVersion":"1.20250601.01.00","hl":"en","gl":"IN"}},
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
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val json = response.body?.string() ?: return@withContext emptyList()

                    val headerArtwork = try {
                        val rootObj = JsonParser.parseString(json).asJsonObject
                        var art = ""
                        fun findHeaderArt(el: com.google.gson.JsonElement) {
                            if (art.isNotBlank()) return
                            if (el.isJsonObject) {
                                val o = el.asJsonObject
                                if (o.has("musicDetailHeaderRenderer") || o.has("musicResponsiveHeaderRenderer") || o.has("musicVisualHeaderRenderer")) {
                                    val header = o.getAsJsonObject("musicDetailHeaderRenderer")
                                        ?: o.getAsJsonObject("musicResponsiveHeaderRenderer")
                                        ?: o.getAsJsonObject("musicVisualHeaderRenderer")
                                    val thumbs = header?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonObject("musicThumbnailRenderer")
                                        ?.getAsJsonObject("thumbnail")
                                        ?.getAsJsonArray("thumbnails")
                                        ?: header?.getAsJsonObject("thumbnail")
                                            ?.getAsJsonObject("croppedSquareThumbnailRenderer")
                                            ?.getAsJsonObject("thumbnail")
                                            ?.getAsJsonArray("thumbnails")
                                    art = thumbs?.lastOrNull()?.asJsonObject?.get("url")?.asString.orEmpty()
                                }
                                if (art.isBlank()) {
                                    for ((_, child) in o.entrySet()) findHeaderArt(child)
                                }
                            }
                        }
                        findHeaderArt(rootObj)
                        ArtworkQualityOptimizer.optimizeUrl(art)
                    } catch (_: Exception) { "" }

                    val fallbackArt = collection.artworkUrl.ifBlank { headerArtwork }

                    parseInnertubeSearch(json).distinctBy { it.id }.map {
                        val trackArt = it.artworkUrl.ifBlank { fallbackArt }
                            .ifBlank { "https://i.ytimg.com/vi/${it.id}/hqdefault.jpg" }
                        it.copy(
                            album = collection.title,
                            artworkUrl = trackArt,
                            source = AudioSource.YOUTUBE
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun searchInnertube(query: String): List<OnlineSong> {
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
                    "query": "${query.replace("\"", "\\\"")}",
                    "params": "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D"
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

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = resp.body?.string() ?: return emptyList()
                parseInnertubeSearch(json)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseInnertubeSearch(jsonString: String): List<OnlineSong> {
        val songs = mutableListOf<OnlineSong>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            fun searchElements(elem: com.google.gson.JsonElement) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val renderer = obj.getAsJsonObject("musicResponsiveListItemRenderer")
                        var videoId = renderer.getAsJsonObject("playlistItemData")?.get("videoId")?.asString.orEmpty()
                        if (videoId.isBlank()) {
                            videoId = renderer.getAsJsonObject("navigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")
                                ?.get("videoId")?.asString.orEmpty()
                        }
                        if (videoId.isBlank()) {
                            videoId = renderer.getAsJsonObject("overlay")
                                ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                                ?.getAsJsonObject("content")
                                ?.getAsJsonObject("musicPlayButtonRenderer")
                                ?.getAsJsonObject("playNavigationEndpoint")
                                ?.getAsJsonObject("watchEndpoint")
                                ?.get("videoId")?.asString.orEmpty()
                        }
                        if (videoId.isBlank()) {
                            val flex = renderer.getAsJsonArray("flexColumns")
                            if (flex != null) {
                                for (f in flex) {
                                    val runs = f.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")
                                    if (runs != null) {
                                        for (r in runs) {
                                            val v = r.asJsonObject.getAsJsonObject("navigationEndpoint")
                                                ?.getAsJsonObject("watchEndpoint")
                                                ?.get("videoId")?.asString.orEmpty()
                                            if (v.isNotBlank()) {
                                                videoId = v
                                                break
                                            }
                                        }
                                    }
                                    if (videoId.isNotBlank()) break
                                }
                            }
                        }

                        if (videoId.isNotBlank()) {
                            val flex = renderer.getAsJsonArray("flexColumns")
                            var title = "Unknown Title"
                            var artist = "YouTube Artist"
                            if (flex != null && flex.size() > 0) {
                                try {
                                    val textObj = flex[0].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                    val simple = textObj?.get("simpleText")?.asString
                                    if (!simple.isNullOrBlank()) {
                                        title = simple.replace('\u00A0', ' ').trim()
                                    } else {
                                        val runs = textObj?.getAsJsonArray("runs")
                                        if (runs != null && runs.size() > 0) {
                                            title = runs.mapNotNull { it.asJsonObject.get("text")?.asString }
                                                .joinToString("")
                                                .replace('\u00A0', ' ')
                                                .trim()
                                        }
                                    }
                                } catch (t: Throwable) {}
                            }
                            if (flex != null && flex.size() > 1) {
                                try {
                                    val runs = flex[1].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")
                                    if (runs != null && runs.size() > 0) {
                                        val artistSb = StringBuilder()
                                        for (r in runs) {
                                            val t = r.asJsonObject.get("text")?.asString.orEmpty()
                                            val trimmed = t.trim()
                                            if (trimmed == "•" || trimmed == "·" || trimmed == "|") break
                                            artistSb.append(t)
                                        }
                                        val parsedArtist = artistSb.toString()
                                            .replace('\u00A0', ' ')
                                            .trim()
                                            .trimEnd(',', '&', ' ')
                                        if (parsedArtist.isNotBlank()) {
                                            artist = parsedArtist
                                        }
                                    }
                                } catch (t: Throwable) {}
                            }

                            val thumb = try {
                                renderer.getAsJsonObject("thumbnail")
                                    ?.getAsJsonObject("musicThumbnailRenderer")
                                    ?.getAsJsonObject("thumbnail")
                                    ?.getAsJsonArray("thumbnails")
                                    ?.last()?.asJsonObject?.get("url")?.asString ?: ""
                            } catch (t: Throwable) { "" }

                            // Extract duration from fixedColumns or flexColumns
                            var durationSeconds = 0L
                            val fixed = renderer.getAsJsonArray("fixedColumns")
                            if (fixed != null) {
                                for (f in fixed) {
                                    val textObj = f.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                                        ?.getAsJsonObject("text")
                                    val simple = textObj?.get("simpleText")?.asString
                                    if (!simple.isNullOrBlank()) {
                                        val sec = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(simple)
                                        if (sec > 0L) {
                                            durationSeconds = sec
                                            break
                                        }
                                    }
                                    val runs = textObj?.getAsJsonArray("runs")
                                    if (runs != null) {
                                        for (r in runs) {
                                            val t = r.asJsonObject.get("text")?.asString ?: ""
                                            val sec = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(t)
                                            if (sec > 0L) {
                                                durationSeconds = sec
                                                break
                                            }
                                        }
                                    }
                                    if (durationSeconds > 0L) break
                                }
                            }

                            if (durationSeconds == 0L && flex != null) {
                                for (f in flex) {
                                    val textObj = f.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                    val simple = textObj?.get("simpleText")?.asString
                                    if (!simple.isNullOrBlank()) {
                                        val sec = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(simple)
                                        if (sec > 0L) {
                                            durationSeconds = sec
                                            break
                                        }
                                    }
                                    val runs = textObj?.getAsJsonArray("runs")
                                    if (runs != null) {
                                        for (r in runs) {
                                            val t = r.asJsonObject.get("text")?.asString ?: ""
                                            val sec = com.aman.auramusic.online.filter.TrendingSongFilter.parseDurationToSeconds(t)
                                            if (sec > 0L) {
                                                durationSeconds = sec
                                                break
                                            }
                                        }
                                    }
                                    if (durationSeconds > 0L) break
                                }
                            }

                            songs.add(
                                OnlineSong(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    album = "YouTube Music",
                                    artworkUrl = ArtworkQualityOptimizer.optimizeUrl(thumb)
                                        .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                                    durationSeconds = durationSeconds,
                                    source = AudioSource.YOUTUBE,
                                    bitrate = "Adaptive",
                                    format = "M4A/Opus"
                                )
                            )
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs
    }

    suspend fun resolveAudioStream(song: OnlineSong): OnlineSong = withContext(Dispatchers.IO) {
        if (song.streamUrl.isNotBlank()) return@withContext song

        for (attempt in instances.indices) {
            val url = "$currentBaseUrl/streams/${song.id}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val resolvedSong = parsePipedStream(body, song)
                        if (resolvedSong.streamUrl.isNotBlank()) {
                            return@withContext resolvedSong
                        }
                    }
                }
            } catch (e: Exception) {
                rotateInstance()
            }
        }
        song
    }

    private fun parsePipedSearchResults(jsonString: String): List<OnlineSong> {
        val songs = mutableListOf<OnlineSong>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val items = root.getAsJsonArray("items") ?: return emptyList()

            for (elem in items) {
                val obj = elem.asJsonObject
                val urlPath = obj.get("url")?.asString ?: continue
                val videoId = urlPath.substringAfter("v=").substringBefore("&")
                if (videoId.isBlank()) continue

                val title = obj.get("title")?.asString ?: "Unknown Title"
                val uploader = obj.get("uploaderName")?.asString ?: "Unknown Artist"
                val thumbnail = obj.get("thumbnail")?.asString ?: ""
                val duration = obj.get("duration")?.asLong ?: 0L

                songs.add(
                    OnlineSong(
                        id = videoId,
                        title = title,
                        artist = uploader,
                        album = "YouTube Music",
                        artworkUrl = ArtworkQualityOptimizer.optimizeUrl(thumbnail),
                        durationSeconds = duration,
                        source = AudioSource.YOUTUBE,
                        bitrate = "Adaptive",
                        format = "M4A/Opus"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs
    }

    private fun parsePipedStream(jsonString: String, baseSong: OnlineSong): OnlineSong {
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val audioStreams = root.getAsJsonArray("audioStreams") ?: return baseSong

            var bestUrl = ""
            var bestBitrate = 0
            var bestQuality = "160 kbps"
            var bestFormat = "M4A"

            for (elem in audioStreams) {
                val stream = elem.asJsonObject
                val url = stream.get("url")?.asString ?: continue
                val bitrate = stream.get("bitrate")?.asInt ?: 0
                val quality = stream.get("quality")?.asString ?: "128 kbps"
                val format = stream.get("format")?.asString ?: "M4A"

                if (bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                    bestQuality = quality
                    bestFormat = format
                }
            }

            if (bestUrl.isNotBlank()) {
                return baseSong.copy(
                    streamUrl = bestUrl,
                    bitrate = bestQuality,
                    format = bestFormat
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return baseSong
    }
}
