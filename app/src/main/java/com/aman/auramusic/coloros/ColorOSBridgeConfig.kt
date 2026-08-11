package com.aman.auramusic.coloros

object ColorOSBridgeConfig {
    const val ACTION_DIRECT_V4 = "io.github.andrealtb.lockscreenlyrics.action.EXTERNAL_LYRIC_DIRECT_V4"
    const val ACTION_EXTERNAL_LYRIC_CAPTURED = "io.github.andrealtb.lockscreenlyrics.action.EXTERNAL_LYRIC_CAPTURED"
    const val TARGET_PACKAGE = "com.android.systemui"
    const val PROTOCOL_VERSION = 4
    const val SOURCE_ID = "aura-music"

    object ExtraKeys {
        const val SOURCE = "source"
        const val PLAYER_PACKAGE = "playerPackage"
        const val SENDER_PACKAGE = "senderPackage"
        const val PROTOCOL_VERSION = "protocolVersion"
        const val EVENT_TYPE = "eventType"
        const val REQUEST_ID = "requestId"
        const val MEDIA_ID = "mediaId"
        const val MEDIA_URI = "mediaUri"
        const val SONG_NAME = "songName"
        const val ARTIST = "artist"
        const val DURATION = "duration"
        const val TRACK_KEY = "trackKey"
        const val TRACK_GENERATION = "trackGeneration"
        const val LYRIC = "lyric"
        const val RAW_LYRIC = "rawLyric"
        const val TRANSLATION_LYRIC = "translationLyric"
        const val PLAYBACK_STATE = "playbackState"
        const val PLAYBACK_POSITION = "playbackPosition"
        const val PLAYBACK_SPEED = "playbackSpeed"
        const val PLAYBACK_LAST_POSITION_UPDATE_TIME = "playbackLastPositionUpdateTime"
        const val CAPTURED_AT = "capturedAt"
        const val LYRIC_INFO = "lyricInfo"
    }

    object EventType {
        const val TRACK_CHANGED = "trackChanged"
        const val LYRIC_READY = "lyricReady"
        const val PLAYBACK_STATE = "playbackState"
    }

    object PlaybackState {
        const val PLAYING = "PLAYING"
        const val PAUSED = "PAUSED"
        const val STOPPED = "STOPPED"
    }
}
