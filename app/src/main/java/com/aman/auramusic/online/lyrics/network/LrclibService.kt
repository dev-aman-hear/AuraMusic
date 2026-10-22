package com.aman.auramusic.online.lyrics.network

import com.aman.auramusic.online.lyrics.model.SongLyrics
import com.aman.auramusic.online.lyrics.parser.LrcParser
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fetches synchronized or plain lyrics from LRCLIB, preferring an exact track match
 * before falling back to a text search.
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

        fetchDirect(cleanTitle, cleanArtist, durationSeconds)
            ?.takeIf { it.hasLyrics() }
            ?: searchFallback("$cleanTitle $cleanArtist", durationSeconds)
            ?: SongLyrics(trackName = title, artistName = artist)
    }

    private fun fetchDirect(title: String, artist: String, durationSeconds: Long): SongLyrics? {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val url = buildString {
            append("$baseUrl/get?track_name=$encodedTitle&artist_name=$encodedArtist")
            if (durationSeconds > 0L) append("&duration=$durationSeconds")
        }
        return fetch(url, title, artist, durationSeconds, isSearch = false)
    }

    private fun searchFallback(query: String, durationSeconds: Long): SongLyrics? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        return fetch(
            url = "$baseUrl/search?q=$encodedQuery",
            fallbackTitle = query,
            fallbackArtist = "",
            durationSeconds = durationSeconds,
            isSearch = true
        )
    }

    private fun fetch(
        url: String,
        fallbackTitle: String,
        fallbackArtist: String,
        durationSeconds: Long,
        isSearch: Boolean
    ): SongLyrics? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JsonParser.parseString(body)

                val candidate = if (isSearch) {
                    if (!json.isJsonArray || json.asJsonArray.size() == 0) return null
                    // Prefer a candidate with timed lyrics, but retain the first search result
                    // as a fallback so plain lyrics remain available.
                    json.asJsonArray.firstOrNull { it.hasNonBlank("syncedLyrics") }
                        ?: json.asJsonArray.first()
                } else {
                    if (!json.isJsonObject) return null
                    json
                }

                candidate.toSongLyrics(
                    fallbackTitle = fallbackTitle,
                    fallbackArtist = fallbackArtist,
                    durationSeconds = durationSeconds
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement.toSongLyrics(
        fallbackTitle: String,
        fallbackArtist: String,
        durationSeconds: Long
    ): SongLyrics {
        val obj = asJsonObject
        val trackName = obj.stringOrNull("trackName") ?: fallbackTitle
        val artistName = obj.stringOrNull("artistName") ?: fallbackArtist
        val syncedLyrics = obj.stringOrNull("syncedLyrics").orEmpty()
        val plainLyrics = obj.stringOrNull("plainLyrics")

        return LrcParser.parse(
            lrcContent = syncedLyrics,
            trackName = trackName,
            artistName = artistName,
            durationSeconds = durationSeconds,
            plainLyrics = plainLyrics
        )
    }

    private fun JsonElement.hasNonBlank(name: String): Boolean =
        stringOrNull(name)?.isNotBlank() == true

    private fun JsonElement.stringOrNull(name: String): String? {
        if (!isJsonObject) return null
        val value = asJsonObject.get(name) ?: return null
        return if (value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            null
        } else {
            value.asString
        }
    }

    private fun SongLyrics.hasLyrics(): Boolean =
        isSynced || !plainLyrics.isNullOrBlank()

    private fun sanitizeTitle(title: String): String =
        title
            .replace(Regex("\\(From [^)]+\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[From [^\\]]+\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(feat\\.[^)]+\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Official[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Official[^\\]]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((?:Music )?Video[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((?:Official )?Audio[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Lyrical[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Original Motion Picture Soundtrack\\)", RegexOption.IGNORE_CASE), "")
            .trim()

    private fun sanitizeArtist(artist: String): String =
        artist.split(",", "&", "feat.", "ft.", ";")[0].trim()

    private companion object {
        const val USER_AGENT =
            "AuraMusic/1.0.2 (https://github.com/dev-aman-hear/AuraMusic)"
    }
}
