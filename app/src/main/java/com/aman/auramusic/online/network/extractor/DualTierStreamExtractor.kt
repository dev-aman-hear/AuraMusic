package com.aman.auramusic.online.network.extractor

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.piped.PipedService
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Dual-Tier Audio Stream Extraction Engine inspired by BitChord and InnerTubeX.
 *
 * Tier 1: Direct YouTube InnerTube Player API with rotated client signatures
 *         (ANDROID_VR Quest 3, ANDROID_MUSIC, IOS, WEB_REMIX) extracting direct Opus & AAC CDN streams.
 * Tier 2: Distributed failover stream resolvers (Piped/Invidious) when signature deciphering is required.
 */
class DualTierStreamExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val pipedFallback: PipedService = PipedService(client)
) {
    private val clientContexts = listOf(
        ClientProfile(
            name = "ANDROID_VR",
            clientName = "ANDROID_VR",
            clientVersion = "1.60.19",
            deviceModel = "Quest 3",
            extraHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Android 12; Mobile VR; Quest 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
        ),
        ClientProfile(
            name = "ANDROID_MUSIC",
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.20.51",
            extraHeaders = mapOf(
                "User-Agent" to "com.google.android.apps.youtube.music/6.20.51 (Linux; U; Android 13; en_US; Pixel 7)"
            )
        ),
        ClientProfile(
            name = "IOS",
            clientName = "IOS",
            clientVersion = "19.29.1",
            deviceModel = "iPhone16,2",
            extraHeaders = mapOf(
                "User-Agent" to "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)"
            )
        ),
        ClientProfile(
            name = "WEB_REMIX",
            clientName = "WEB_REMIX",
            clientVersion = "1.20240101.01.00",
            extraHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                "Referer" to "https://music.youtube.com/"
            )
        )
    )

    data class ClientProfile(
        val name: String,
        val clientName: String,
        val clientVersion: String,
        val deviceModel: String? = null,
        val extraHeaders: Map<String, String> = emptyMap()
    )

    suspend fun resolveStream(song: OnlineSong): OnlineSong = withContext(Dispatchers.IO) {
        if (song.streamUrl.isNotBlank()) return@withContext song

        // Tier 1: InnerTube Multi-Client Rotation
        for (profile in clientContexts) {
            val tier1Result = queryInnerTubePlayer(song, profile)
            if (tier1Result != null && tier1Result.streamUrl.isNotBlank()) {
                return@withContext tier1Result
            }
        }

        // Tier 2: Distributed Resolver Failover
        val tier2Result = pipedFallback.resolveAudioStream(song)
        if (tier2Result.streamUrl.isNotBlank()) {
            return@withContext tier2Result.copy(
                extractorTier = "Tier 2: Distributed Resolver",
                audioCodec = if (tier2Result.format.contains("Opus")) "Opus (160k)" else "AAC (128k)"
            )
        }

        song
    }

    private fun queryInnerTubePlayer(song: OnlineSong, profile: ClientProfile): OnlineSong? {
        return try {
            val devicePart = if (profile.deviceModel != null) {
                """, "deviceModel": "${profile.deviceModel}""""
            } else ""

            val payload = """
                {
                    "context": {
                        "client": {
                            "clientName": "${profile.clientName}",
                            "clientVersion": "${profile.clientVersion}",
                            "hl": "en",
                            "gl": "US"
                            $devicePart
                        }
                    },
                    "videoId": "${song.id}"
                }
            """.trimIndent()

            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val requestBuilder = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body)
                .header("Content-Type", "application/json")

            profile.extraHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonString = response.body?.string() ?: return null
                parsePlayerResponse(jsonString, song, profile.name)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePlayerResponse(jsonString: String, song: OnlineSong, clientName: String): OnlineSong? {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val streamingData = root.getAsJsonObject("streamingData") ?: return null
            val adaptiveFormats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

            var bestUrl = ""
            var bestBitrate = 0
            var mime = "audio/webm"
            var formatLabel = "Opus"

            for (elem in adaptiveFormats) {
                val format = elem.asJsonObject
                val mimeType = format.get("mimeType")?.asString ?: ""
                if (mimeType.startsWith("audio/")) {
                    val url = format.get("url")?.asString ?: ""
                    val bitrate = format.get("bitrate")?.asInt ?: 0

                    if (url.isNotBlank() && bitrate > bestBitrate) {
                        bestBitrate = bitrate
                        bestUrl = url
                        mime = mimeType
                        formatLabel = if (mimeType.contains("webm")) "WebM/Opus" else "M4A/AAC"
                    }
                }
            }

            if (bestUrl.isNotBlank()) {
                val bitrateKbps = if (bestBitrate > 0) "${bestBitrate / 1000} kbps" else "160 kbps"
                song.copy(
                    streamUrl = bestUrl,
                    bitrate = bitrateKbps,
                    format = formatLabel,
                    extractorTier = "Tier 1: InnerTube ($clientName)",
                    audioCodec = if (formatLabel.contains("Opus")) "Opus ($bitrateKbps)" else "AAC ($bitrateKbps)"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
