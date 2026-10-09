package com.aman.auramusic.online.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Media cache manager implementing chunk-based stream caching using ExoPlayer's SimpleCache.
 * Follows patterns from InnerTune and ViMusic to cache songs automatically during playback.
 */
@OptIn(UnstableApi::class)
object CacheManager {
    private const val MAX_CACHE_SIZE_BYTES = 512L * 1024L * 1024L // 512 MB
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "online_audio_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
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
}
