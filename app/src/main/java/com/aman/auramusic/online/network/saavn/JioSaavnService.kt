package com.aman.auramusic.online.network.saavn

import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.network.crypto.DesDecryptor
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Service interacting directly with JioSaavn's internal REST API.
 * Provides high-speed search, track resolution, and instant 320kbps MP4/AAC streaming.
 */
class JioSaavnService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl = "https://www.jiosaavn.com/api.php"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun searchSongs(query: String, limit: Int = 25): List<OnlineSong> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl?__call=search.getResults&_format=json&_marker=0&cc=in&q=$encodedQuery&p=1&n=$limit"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseSearchResults(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTrendingSongs(): List<OnlineSong> = withContext(Dispatchers.IO) {
        searchSongs("Trending Hits 2026", limit = 25)
    }

    suspend fun searchPlaylists(query: String, limit: Int = 15): List<com.aman.auramusic.online.model.OnlinePlaylist> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl?__call=search.getPlaylistResults&_format=json&_marker=0&cc=in&q=$encodedQuery&p=1&n=$limit"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parsePlaylistResults(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCuratedPlaylists(): List<com.aman.auramusic.online.model.OnlinePlaylist> = withContext(Dispatchers.IO) {
        val queries = listOf("Trending", "Top Hindi Hits", "Punjabi Pop", "Global Hits", "Spotify Viral")
        val allPlaylists = mutableListOf<com.aman.auramusic.online.model.OnlinePlaylist>()
        val seenIds = mutableSetOf<String>()

        for (q in queries) {
            val results = searchPlaylists(q, limit = 4)
            for (p in results) {
                if (seenIds.add(p.id)) {
                    allPlaylists.add(p)
                }
            }
        }
        allPlaylists
    }

    suspend fun getPlaylistDetails(listId: String): List<OnlineSong> = withContext(Dispatchers.IO) {
        val url = "$baseUrl?__call=playlist.getDetails&_format=json&_marker=0&cc=in&listid=$listId"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JsonParser.parseString(body).asJsonObject
                val songsArray = root.getAsJsonArray("songs") ?: root.getAsJsonArray("results")
                if (songsArray != null) {
                    parseSongElements(songsArray)
                } else emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun resolveStream(song: OnlineSong, targetBitrate: String = "320"): OnlineSong {
        if (song.encryptedMediaUrl.isBlank()) return song
        val decrypted = DesDecryptor.decrypt(song.encryptedMediaUrl)
        val upgraded = DesDecryptor.upgradeBitrate(decrypted, targetBitrate)
        return song.copy(
            streamUrl = upgraded.ifBlank { song.mediaPreviewUrl },
            bitrate = "$targetBitrate kbps",
            format = "AAC/MP4"
        )
    }

    private fun parsePlaylistResults(jsonString: String): List<com.aman.auramusic.online.model.OnlinePlaylist> {
        val playlists = mutableListOf<com.aman.auramusic.online.model.OnlinePlaylist>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val results = root.getAsJsonArray("results") ?: return emptyList()

            for (element in results) {
                val obj = element.asJsonObject
                val id = obj.get("listid")?.asString ?: continue
                val rawName = obj.get("listname")?.asString ?: "Featured Playlist"
                val rawImage = obj.get("image")?.asString ?: ""
                val count = obj.get("count")?.asString?.toIntOrNull() ?: 0
                val language = obj.get("language")?.asString ?: ""
                val firstname = obj.get("firstname")?.asString ?: ""
                val lastname = obj.get("lastname")?.asString ?: ""
                val author = listOf(firstname, lastname).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "JioSaavn Editor" }

                val highResArtwork = rawImage
                    .replace("150x150", "500x500")
                    .replace("50x50", "500x500")

                val source = if (rawName.contains("Spotify", ignoreCase = true)) {
                    AudioSource.SPOTIFY
                } else {
                    AudioSource.JIOSAAVN
                }

                playlists.add(
                    com.aman.auramusic.online.model.OnlinePlaylist(
                        id = id,
                        title = cleanHtmlEntities(rawName),
                        subtitle = "$author • $language",
                        artworkUrl = highResArtwork,
                        songCount = count,
                        source = source,
                        language = language
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return playlists
    }

    private fun parseSearchResults(jsonString: String): List<OnlineSong> {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val results = root.getAsJsonArray("results") ?: return emptyList()
            parseSongElements(results)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseSongElements(elements: com.google.gson.JsonArray): List<OnlineSong> {
        val songs = mutableListOf<OnlineSong>()
        try {
            for (element in elements) {
                val obj = element.asJsonObject
                val id = obj.get("id")?.asString ?: continue
                val rawTitle = obj.get("song")?.asString ?: obj.get("title")?.asString ?: "Unknown Title"
                val rawArtist = obj.get("primary_artists")?.asString
                    ?: obj.get("singers")?.asString
                    ?: "Unknown Artist"
                val rawAlbum = obj.get("album")?.asString ?: ""
                val rawImage = obj.get("image")?.asString ?: ""
                val durationStr = obj.get("duration")?.asString ?: "0"
                val encryptedMediaUrl = obj.get("encrypted_media_url")?.asString ?: ""
                val mediaPreviewUrl = obj.get("media_preview_url")?.asString ?: ""

                val highResArtwork = rawImage
                    .replace("150x150", "500x500")
                    .replace("50x50", "500x500")

                val decryptedUrl = if (encryptedMediaUrl.isNotBlank()) {
                    val initial = DesDecryptor.decrypt(encryptedMediaUrl)
                    DesDecryptor.upgradeBitrate(initial, "320")
                } else ""

                val song = OnlineSong(
                    id = id,
                    title = cleanHtmlEntities(rawTitle),
                    artist = cleanHtmlEntities(rawArtist),
                    album = cleanHtmlEntities(rawAlbum),
                    artworkUrl = highResArtwork,
                    durationSeconds = durationStr.toLongOrNull() ?: 0L,
                    source = AudioSource.JIOSAAVN,
                    streamUrl = decryptedUrl.ifBlank { mediaPreviewUrl },
                    bitrate = "320 kbps",
                    format = "AAC/MP4",
                    encryptedMediaUrl = encryptedMediaUrl,
                    mediaPreviewUrl = mediaPreviewUrl
                )
                songs.add(song)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songs
    }


    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&copy;", "")
            .trim()
    }
}
