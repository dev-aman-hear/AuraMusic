package com.aman.auramusic.online.util

import android.content.Context
import androidx.core.content.edit
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persistent store for remembering the last played track—seamlessly supporting
 * both online streams (YouTube, JioSaavn) and local offline tracks across app restarts.
 */
object LastPlayedStore {

    private const val PREFS_NAME = "playback_state"
    private const val KEY_SOURCE_TYPE = "last_source_type" // "online" or "offline"
    private const val KEY_OFFLINE_SONG_ID = "last_song_id"
    private const val KEY_POSITION = "last_position"
    private const val KEY_DURATION = "last_duration"
    private const val KEY_ONLINE_SONG_JSON = "last_online_song_json"
    private const val KEY_ONLINE_QUEUE_JSON = "last_online_queue_json"
    private const val KEY_TIMESTAMP = "last_playback_timestamp"

    const val SOURCE_OFFLINE = "offline"
    const val SOURCE_ONLINE = "online"

    private val gson = Gson()

    fun saveOffline(context: Context, song: Song, positionMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString(KEY_SOURCE_TYPE, SOURCE_OFFLINE)
            putLong(KEY_OFFLINE_SONG_ID, song.id)
            putLong(KEY_POSITION, positionMs)
            putLong(KEY_DURATION, song.duration)
            putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
    }

    fun saveOnline(
        context: Context,
        song: OnlineSong,
        positionMs: Long,
        queue: List<OnlineSong> = emptyList()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val songJson = try {
            gson.toJson(song)
        } catch (_: Exception) { "" }

        val queueJson = try {
            if (queue.isNotEmpty()) gson.toJson(queue) else ""
        } catch (_: Exception) { "" }

        prefs.edit(commit = true) {
            putString(KEY_SOURCE_TYPE, SOURCE_ONLINE)
            putString(KEY_ONLINE_SONG_JSON, songJson)
            putString(KEY_ONLINE_QUEUE_JSON, queueJson)
            putLong(KEY_POSITION, positionMs)
            putLong(KEY_DURATION, song.durationSeconds * 1000L)
            putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
    }

    fun updatePosition(context: Context, positionMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = false) {
            putLong(KEY_POSITION, positionMs)
        }
    }

    fun getLastSourceType(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val explicitType = prefs.getString(KEY_SOURCE_TYPE, null)
        if (!explicitType.isNullOrBlank()) {
            return explicitType
        }
        // Fallback for legacy preference migration:
        val onlineJson = prefs.getString(KEY_ONLINE_SONG_JSON, null)
        if (!onlineJson.isNullOrBlank()) return SOURCE_ONLINE
        val lastId = prefs.getLong(KEY_OFFLINE_SONG_ID, -1L)
        return if (lastId != -1L) SOURCE_OFFLINE else SOURCE_OFFLINE
    }

    fun getLastOfflineSongId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_OFFLINE_SONG_ID, -1L)
    }

    fun getLastPosition(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_POSITION, 0L)
    }

    fun getLastOnlineSong(context: Context): OnlineSong? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ONLINE_SONG_JSON, null) ?: return null
        return try {
            gson.fromJson(json, OnlineSong::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getLastOnlineQueue(context: Context): List<OnlineSong> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ONLINE_QUEUE_JSON, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<OnlineSong>>() {}.type
            gson.fromJson<List<OnlineSong>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { clear() }
    }
}
