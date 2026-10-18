package com.aman.auramusic.online.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.aman.auramusic.online.model.OnlineSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Media cache manager implementing chunk-based stream caching using ExoPlayer's SimpleCache.
 * Follows patterns from InnerTune and ViMusic to cache songs automatically during playback.
 * Provides cache statistics, cache clearing, and preloading of upcoming tracks.
 */
@OptIn(UnstableApi::class)
object CacheManager {
    private const val DEFAULT_MAX_CACHE_SIZE_BYTES = 512L * 1024L * 1024L // 512 MB
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "online_audio_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(DEFAULT_MAX_CACHE_SIZE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    fun buildCacheDataSourceFactory(
        context: Context,
        httpDataSourceFactory: DefaultHttpDataSource.Factory
    ): DataSource.Factory {
        return try {
            val cache = getCache(context)
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } catch (e: Exception) {
            httpDataSourceFactory
        }
    }

    fun getCacheUsageBytes(context: Context): Long {
        return try {
            val cache = getCache(context)
            cache.cacheSpace
        } catch (_: Exception) {
            val cacheDir = File(context.cacheDir, "online_audio_cache")
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        }
    }

    fun getFormattedCacheUsage(context: Context): String {
        val bytes = getCacheUsageBytes(context)
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb < 1.0) {
            "%.1f KB".format(bytes / 1024.0)
        } else if (mb < 1024.0) {
            "%.1f MB".format(mb)
        } else {
            "%.2f GB".format(mb / 1024.0)
        }
    }

    @Synchronized
    fun clearCache(context: Context) {
        try {
            val cache = getCache(context)
            val keys = cache.keys.toList()
            for (key in keys) {
                runCatching { cache.removeResource(key) }
            }
        } catch (e: Exception) {
            Log.e("CacheManager", "Error clearing cache keys", e)
            val cacheDir = File(context.cacheDir, "online_audio_cache")
            if (cacheDir.exists()) {
                runCatching { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
            }
        }
    }

    /**
     * Safely preloads the introductory chunk (~1MB) of the upcoming online track
     * so sequential track advancement starts with zero buffering latency.
     */
    fun preloadUpcomingTrack(context: Context, song: OnlineSong) {
        val streamUrl = song.streamUrl
        if (streamUrl.isBlank() || !streamUrl.startsWith("http")) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cache = getCache(context)
                val uri = android.net.Uri.parse(streamUrl)
                val dataSpec = DataSpec.Builder()
                    .setUri(uri)
                    .setLength(1024L * 1024L) // 1MB initial pre-buffer
                    .build()
                val upstream = DefaultHttpDataSource.Factory().createDataSource()
                val cacheDataSource = CacheDataSource(cache, upstream)
                val buffer = ByteArray(32 * 1024)
                cacheDataSource.open(dataSpec)
                var totalRead = 0
                while (totalRead < 1024 * 1024) {
                    val read = cacheDataSource.read(buffer, 0, buffer.size)
                    if (read <= 0) break
                    totalRead += read
                }
                cacheDataSource.close()
                Log.d("CacheManager", "Successfully preloaded $totalRead bytes for upcoming track: ${song.title}")
            } catch (e: Exception) {
                Log.w("CacheManager", "Safe preload skipped/failed for ${song.title}: ${e.message}")
            }
        }
    }
}
