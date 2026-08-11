package com.aman.auramusic.coloros

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song

class ColorOSLiveLyricsBridge(private val context: Context) {

    var isEnabled: Boolean = false

    private var currentSongId: Long? = null
    private var trackGeneration: Long = 100L
    private var cachedLyrics: List<LyricLine> = emptyList()

    private var lastPositionBroadcastMs: Long = 0L
    private var lastBroadcastTimeMs: Long = 0L

    companion object {
        private const val TAG = "ColorOSLiveLyricsBridge"
        private const val POSITION_THROTTLE_MS = 2000L
    }

    @Synchronized
    fun onTrackChanged(song: Song, lyrics: List<LyricLine>, isPlaying: Boolean, positionMs: Long) {
        if (currentSongId != song.id) {
            currentSongId = song.id
            trackGeneration++
        }

        cachedLyrics = lyrics
        if (!isEnabled) return

        val lrcString = ColorOSLyricPayload.formatLrc(lyrics)
        sendDirectV4Broadcast(
            song = song,
            lrcString = lrcString,
            eventType = ColorOSBridgeConfig.EventType.TRACK_CHANGED,
            isPlaying = isPlaying,
            positionMs = positionMs
        )

        if (lrcString.isNotEmpty()) {
            sendDirectV4Broadcast(
                song = song,
                lrcString = lrcString,
                eventType = ColorOSBridgeConfig.EventType.LYRIC_READY,
                isPlaying = isPlaying,
                positionMs = positionMs
            )
        }
    }

    @Synchronized
    fun onLyricsLoaded(song: Song, lyrics: List<LyricLine>, isPlaying: Boolean, positionMs: Long) {
        cachedLyrics = lyrics
        if (!isEnabled) return

        if (currentSongId == song.id) {
            val lrcString = ColorOSLyricPayload.formatLrc(lyrics)
            if (lrcString.isNotEmpty()) {
                sendDirectV4Broadcast(
                    song = song,
                    lrcString = lrcString,
                    eventType = ColorOSBridgeConfig.EventType.LYRIC_READY,
                    isPlaying = isPlaying,
                    positionMs = positionMs
                )
            }
        }
    }

    @Synchronized
    fun onPlaybackStateChanged(song: Song?, isPlaying: Boolean, positionMs: Long) {
        if (!isEnabled || song == null) return

        val now = SystemClock.elapsedRealtime()
        val posDelta = Math.abs(positionMs - lastPositionBroadcastMs)

        // Broadcast state immediately if playback state toggles, or throttled if just playing
        if (now - lastBroadcastTimeMs >= POSITION_THROTTLE_MS || posDelta >= 1000L) {
            val lrcString = ColorOSLyricPayload.formatLrc(cachedLyrics)
            sendDirectV4Broadcast(
                song = song,
                lrcString = lrcString,
                eventType = ColorOSBridgeConfig.EventType.PLAYBACK_STATE,
                isPlaying = isPlaying,
                positionMs = positionMs
            )
        }
    }

    @Synchronized
    fun onSeek(song: Song?, positionMs: Long, isPlaying: Boolean) {
        if (!isEnabled || song == null) return

        val lrcString = ColorOSLyricPayload.formatLrc(cachedLyrics)
        sendDirectV4Broadcast(
            song = song,
            lrcString = lrcString,
            eventType = ColorOSBridgeConfig.EventType.PLAYBACK_STATE,
            isPlaying = isPlaying,
            positionMs = positionMs
        )
    }

    @Synchronized
    fun onPlaybackStopped(song: Song?) {
        if (!isEnabled || song == null) return

        val lrcString = ColorOSLyricPayload.formatLrc(cachedLyrics)
        sendDirectV4Broadcast(
            song = song,
            lrcString = lrcString,
            eventType = ColorOSBridgeConfig.EventType.PLAYBACK_STATE,
            isPlaying = false,
            positionMs = 0L,
            forcedState = ColorOSBridgeConfig.PlaybackState.STOPPED
        )
    }

    private fun sendDirectV4Broadcast(
        song: Song,
        lrcString: String,
        eventType: String,
        isPlaying: Boolean,
        positionMs: Long,
        forcedState: String? = null
    ) {
        try {
            val packageName = context.packageName
            val nowMs = System.currentTimeMillis()
            val elapsedMs = SystemClock.elapsedRealtime()
            val stateStr = forcedState ?: if (isPlaying) ColorOSBridgeConfig.PlaybackState.PLAYING else ColorOSBridgeConfig.PlaybackState.PAUSED
            val lyricInfoJson = ColorOSLyricPayload.generateLyricInfoJson(song, cachedLyrics)

            // Primary V4 Broadcast Action
            val intentV4 = Intent(ColorOSBridgeConfig.ACTION_DIRECT_V4).apply {
                setPackage(ColorOSBridgeConfig.TARGET_PACKAGE)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SOURCE, ColorOSBridgeConfig.SOURCE_ID)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYER_PACKAGE, packageName)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SENDER_PACKAGE, packageName)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PROTOCOL_VERSION, ColorOSBridgeConfig.PROTOCOL_VERSION)
                putExtra(ColorOSBridgeConfig.ExtraKeys.EVENT_TYPE, eventType)
                putExtra(ColorOSBridgeConfig.ExtraKeys.REQUEST_ID, nowMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.MEDIA_ID, song.id.toString())
                putExtra(ColorOSBridgeConfig.ExtraKeys.MEDIA_URI, song.uri)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SONG_NAME, song.title)
                putExtra(ColorOSBridgeConfig.ExtraKeys.ARTIST, song.artist)
                putExtra(ColorOSBridgeConfig.ExtraKeys.DURATION, song.duration)
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRACK_KEY, ColorOSLyricPayload.buildTrackKey(song))
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRACK_GENERATION, trackGeneration)
                putExtra(ColorOSBridgeConfig.ExtraKeys.LYRIC, lrcString)
                putExtra(ColorOSBridgeConfig.ExtraKeys.RAW_LYRIC, lrcString)
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRANSLATION_LYRIC, "")
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_STATE, stateStr)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_POSITION, positionMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_SPEED, if (isPlaying) 1.0f else 0.0f)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_LAST_POSITION_UPDATE_TIME, elapsedMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.CAPTURED_AT, nowMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.LYRIC_INFO, lyricInfoJson)
            }

            context.sendBroadcast(intentV4)

            // Legacy/Captured Broadcast Action fallback for maximum Bridge compatibility
            val intentCaptured = Intent(ColorOSBridgeConfig.ACTION_EXTERNAL_LYRIC_CAPTURED).apply {
                setPackage(ColorOSBridgeConfig.TARGET_PACKAGE)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SOURCE, "lyricprovider/${ColorOSBridgeConfig.SOURCE_ID}")
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYER_PACKAGE, packageName)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SENDER_PACKAGE, packageName)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PROTOCOL_VERSION, ColorOSBridgeConfig.PROTOCOL_VERSION)
                putExtra(ColorOSBridgeConfig.ExtraKeys.EVENT_TYPE, eventType)
                putExtra(ColorOSBridgeConfig.ExtraKeys.REQUEST_ID, nowMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.MEDIA_ID, song.id.toString())
                putExtra(ColorOSBridgeConfig.ExtraKeys.MEDIA_URI, song.uri)
                putExtra(ColorOSBridgeConfig.ExtraKeys.SONG_NAME, song.title)
                putExtra(ColorOSBridgeConfig.ExtraKeys.ARTIST, song.artist)
                putExtra(ColorOSBridgeConfig.ExtraKeys.DURATION, song.duration)
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRACK_KEY, ColorOSLyricPayload.buildTrackKey(song))
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRACK_GENERATION, trackGeneration)
                putExtra(ColorOSBridgeConfig.ExtraKeys.LYRIC, lrcString)
                putExtra(ColorOSBridgeConfig.ExtraKeys.RAW_LYRIC, lrcString)
                putExtra(ColorOSBridgeConfig.ExtraKeys.TRANSLATION_LYRIC, "")
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_STATE, stateStr)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_POSITION, positionMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_SPEED, if (isPlaying) 1.0f else 0.0f)
                putExtra(ColorOSBridgeConfig.ExtraKeys.PLAYBACK_LAST_POSITION_UPDATE_TIME, elapsedMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.CAPTURED_AT, nowMs)
                putExtra(ColorOSBridgeConfig.ExtraKeys.LYRIC_INFO, lyricInfoJson)
            }

            context.sendBroadcast(intentCaptured)

            lastPositionBroadcastMs = positionMs
            lastBroadcastTimeMs = elapsedMs
            Log.d(TAG, "ColorOS Broadcast sent: eventType=$eventType, song='${song.title}', gen=$trackGeneration, state=$stateStr, pos=$positionMs")
        } catch (e: Exception) {
            Log.e(TAG, "Non-fatal error sending ColorOS Live Lyrics broadcast", e)
        }
    }
}
