package com.aman.auramusic.online.lyrics.network

import com.aman.auramusic.online.lyrics.model.SongLyrics
import com.aman.auramusic.online.lyrics.parser.LrcParser
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * High-speed lyrics provider querying LRCLIB (open-source synced lyrics repository).
 * Provides millimeter-accurate word/line synced lyrics for millions of global and Bollywood songs.
 */
class LrclibService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl = "https://lrclib.net/api"

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSeconds: Long = 0L
    ): SongLyrics = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizeTitle(title)
        val cleanArtist = sanitizeArtist(artist)

        // Try exact match first: /api/get?track_name=...&artist_name=...
        val directResult = fetchDirect(cleanTitle, cleanArtist, durationSeconds)
        if (directResult != null && (directResult.isSynced || !directResult.plainLyrics.isNullOrBlank())) {
            return@withContext directResult
        }

        // Fallback: /api/search?q={title + " " + artist}
        val searchResult = searchFallback("$cleanTitle $cleanArtist", durationSeconds)
        if (searchResult != null) {
            return@withContext searchResult
        }

        SongLyrics(trackName = title, artistName = artist, lines = emptyList())
    }

    private fun fetchDirect(title: String, artist: String, durationSeconds: Long): SongLyrics? {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            var url = "$baseUrl/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            if (durationSeconds > 0) {
                url += "&duration=$durationSeconds"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/3.2.0 (https://github.com/aman/AuraMusic)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parseLrclibResponse(body, title, artist, durationSeconds)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchFallback(query: String, durationSeconds: Long): SongLyrics? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/3.2.0 (https://github.com/aman/AuraMusic)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val array = JsonParser.parseString(body).asJsonArray
                if (array.size() == 0) return null

                // Pick first item that has syncedLyrics
                var bestCandidate = array[0].asJsonObject
                for (item in array) {
                    val obj = item.asJsonObject
                    if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull &&
                        obj.get("syncedLyrics").asString.isNotBlank()
                    ) {
                        bestCandidate = obj
                        break
                    }
                }

                val trackName = bestCandidate.get("trackName")?.asString ?: query
                val artistName = bestCandidate.get("artistName")?.asString ?: ""
                val syncedLyrics = if (bestCandidate.has("syncedLyrics") && !bestCandidate.get("syncedLyrics").isJsonNull) {
                    bestCandidate.get("syncedLyrics").asString
                } else ""
                val plainLyrics = if (bestCandidate.has("plainLyrics") && !bestCandidate.get("plainLyrics").isJsonNull) {
                    bestCandidate.get("plainLyrics").asString
                } else null

                LrcParser.parse(
                    lrcContent = syncedLyrics,
                    trackName = trackName,
                    artistName = artistName,
                    durationSeconds = durationSeconds,
                    plainLyrics = plainLyrics
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrclibResponse(
        jsonString: String,
        title: String,
        artist: String,
        durationSeconds: Long
    ): SongLyrics? {
        return try {
            val obj = JsonParser.parseString(jsonString).asJsonObject
            val trackName = obj.get("trackName")?.asString ?: title
            val artistName = obj.get("artistName")?.asString ?: artist
            val syncedLyrics = if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull) {
                obj.get("syncedLyrics").asString
            } else ""
            val plainLyrics = if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull) {
                obj.get("plainLyrics").asString
            } else null

            LrcParser.parse(
                lrcContent = syncedLyrics,
                trackName = trackName,
                artistName = artistName,
                durationSeconds = durationSeconds,
                plainLyrics = plainLyrics
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("\\(From [^)]+\\)"), "")
            .replace(Regex("\\[From [^\\]]+\\]"), "")
            .replace(Regex("\\(feat\\.[^)]+\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Official[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Official[^\\]]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Video[^\\]]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Audio[^\\]]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Lyrical[^\\]]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Original Motion Picture Soundtrack\\)", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun sanitizeArtist(artist: String): String {
        return artist
            .split(",", "&", "feat.", "ft.", ";")[0]
            .trim()
    }
}
