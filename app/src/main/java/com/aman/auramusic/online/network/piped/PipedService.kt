package com.aman.auramusic.online.network.piped

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
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
                        val videoId = renderer.getAsJsonObject("playlistItemData")?.get("videoId")?.asString ?: ""
                        if (videoId.isNotBlank()) {
                            val flex = renderer.getAsJsonArray("flexColumns")
                            var title = "Unknown Title"
                            var artist = "YouTube Artist"
                            if (flex != null && flex.size() > 0) {
                                try {
                                    title = flex[0].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        .getAsJsonObject("text")
                                        .getAsJsonArray("runs")[0].asJsonObject
                                        .get("text").asString
                                } catch (t: Throwable) {}
                            }
                            if (flex != null && flex.size() > 1) {
                                try {
                                    artist = flex[1].asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        .getAsJsonObject("text")
                                        .getAsJsonArray("runs")[0].asJsonObject
                                        .get("text").asString
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
                                    val runs = f.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")
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
                                    val runs = f.asJsonObject
                                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.getAsJsonObject("text")
                                        ?.getAsJsonArray("runs")
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
                                    artworkUrl = ArtworkQualityOptimizer.optimizeUrl(thumb),
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
