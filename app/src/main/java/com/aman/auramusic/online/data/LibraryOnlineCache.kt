package com.aman.auramusic.online.data

import android.content.Context
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlinePlaylist
import com.aman.auramusic.online.model.OnlineSong
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CachedLibraryData(
    val lastFetchedTimestamp: Long = 0L,
    val playlistsBySource: Map<String, List<OnlinePlaylist>> = emptyMap(),
    val songFeed: List<OnlineSong> = emptyList()
)

class LibraryOnlineCache(private val context: Context) {
    private val gson = Gson()
    private val cacheFile: File
        get() = File(context.filesDir, "library_online_feed_cache.json")

    private var memoryCache: CachedLibraryData? = null

    companion object {
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    @Synchronized
    fun getCachedData(): CachedLibraryData? {
        memoryCache?.let { return it }

        return try {
            if (!cacheFile.exists()) return null
            val json = cacheFile.readText()
            if (json.isBlank()) return null
            val type = object : TypeToken<CachedLibraryData>() {}.type
            val data: CachedLibraryData? = gson.fromJson(json, type)
            memoryCache = data
            data
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun saveCachedData(data: CachedLibraryData) {
        memoryCache = data
        try {
            val json = gson.toJson(data)
            cacheFile.writeText(json)
        } catch (_: Exception) {}
    }

    fun shouldRefreshDaily(lastTimestamp: Long): Boolean {
        if (lastTimestamp <= 0L) return true
        val now = System.currentTimeMillis()
        if (now - lastTimestamp >= ONE_DAY_MILLIS) return true

        // Refresh if calendar day has rolled over
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        val lastDay = format.format(Date(lastTimestamp))
        val currentDay = format.format(Date(now))
        return lastDay != currentDay
    }

    fun getFormattedLastUpdated(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        val isToday = format.format(Date(timestamp)) == format.format(Date(now))

        return when {
            diff < 60 * 1000L -> "Updated just now"
            diff < 60 * 60 * 1000L -> "Updated ${diff / (60 * 1000L)}m ago"
            isToday -> "Updated today"
            diff < 48 * 60 * 60 * 1000L -> "Updated yesterday"
            else -> {
                val displayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                "Updated " + displayFormat.format(Date(timestamp))
            }
        }
    }
}
