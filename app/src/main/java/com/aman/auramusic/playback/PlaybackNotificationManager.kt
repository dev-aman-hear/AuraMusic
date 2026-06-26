package com.aman.auramusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.aman.auramusic.MainActivity
import com.aman.auramusic.R
import com.aman.auramusic.data.model.Song

class PlaybackNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Aura Music playback controls"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun show(
        song: Song,
        isPlaying: Boolean,
        sessionToken: MediaSession.Token
    ) {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(song, isPlaying, sessionToken))
        }
    }

    fun createNotification(
        song: Song,
        isPlaying: Boolean,
        sessionToken: MediaSession.Token
    ): android.app.Notification {
        ensureChannel()
        val artwork = if (song.id == -1L) {
            com.aman.auramusic.util.ArtworkExtractor.getArtwork(context, song.uri)
        } else {
            loadArtwork(song.artworkUri)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = actionIntent(PlaybackActionReceiver.ACTION_PLAY_PAUSE, 1)
        val previousIntent = actionIntent(PlaybackActionReceiver.ACTION_PREVIOUS, 2)
        val nextIntent = actionIntent(PlaybackActionReceiver.ACTION_NEXT, 3)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    previousIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pause" else "Play",
                    playPauseIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    nextIntent
                )
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(sessionToken))
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    fun clear() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackActionReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun loadArtwork(artworkUri: String?): Bitmap? {
        if (artworkUri.isNullOrBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(artworkUri.toUri())?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }

    companion object {
        const val CHANNEL_ID = "aura_playback"
        const val NOTIFICATION_ID = 101
    }
}
