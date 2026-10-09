package com.aman.auramusic.ui.screen.hometest

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata for the daily featured album or playlist card.
 */
data class DailyFeaturedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val artworkUrl: String?,
    val isOnline: Boolean,
    val albumName: String? = null,
    val playlistId: Long? = null,
    val onlinePlaylistId: String? = null,
    val songCountText: String = "",
    val dayKey: String = "" // yyyyMMdd
)

/**
 * Persistent 24h cache ensuring the Home Featured selection updates
 * strictly once per calendar day rather than on every app restart.
 */
class DailyFeaturedCache(private val context: Context) {
    private val gson = Gson()
    private val cacheFile: File
        get() = File(context.filesDir, "daily_featured_cache.json")

    private var memoryCache: DailyFeaturedItem? = null

    @Synchronized
    fun getDailyFeatured(currentDayKey: String): DailyFeaturedItem? {
        memoryCache?.let {
            if (it.dayKey == currentDayKey) return it
        }

        return try {
            if (!cacheFile.exists()) return null
            val json = cacheFile.readText()
            if (json.isBlank()) return null
            val item = gson.fromJson(json, DailyFeaturedItem::class.java)
            if (item != null && item.dayKey == currentDayKey) {
                memoryCache = item
                item
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun saveDailyFeatured(item: DailyFeaturedItem) {
        memoryCache = item
        try {
            val json = gson.toJson(item)
            cacheFile.writeText(json)
        } catch (_: Exception) {}
    }

    companion object {
        fun getCurrentDayKey(): String {
            return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }
    }
}
