package com.aman.auramusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlaybackActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> PlaybackActionRegistry.onPlayPause?.invoke()
            ACTION_NEXT -> PlaybackActionRegistry.onNext?.invoke()
            ACTION_PREVIOUS -> PlaybackActionRegistry.onPrevious?.invoke()
            ACTION_FAVORITE -> PlaybackActionRegistry.onFavorite?.invoke()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.aman.auramusic.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.aman.auramusic.action.NEXT"
        const val ACTION_PREVIOUS = "com.aman.auramusic.action.PREVIOUS"
        const val ACTION_FAVORITE = "com.aman.auramusic.action.FAVORITE"
    }
}
