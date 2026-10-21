package com.aman.auramusic.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object LockScreenManager {
    private const val TAG = "LockScreenManager"

    /**
     * Checks if the app has permission to draw overlays (display over other apps / keyguard).
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Directs the user to the system permission screen for "Display over other apps".
     */
    fun openOverlayPermissionSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open overlay permission settings", e)
        }
    }

    /**
     * Checks prerequisites and launches LockScreenPlayerActivity if playback is active and device is locked.
     */
    fun showLockScreenIfNeeded(context: Context, isPlaying: Boolean, isFeatureEnabled: Boolean) {
        if (!isFeatureEnabled) {
            Log.d(TAG, "Lockscreen player is disabled in settings.")
            return
        }

        if (!isPlaying) {
            Log.d(TAG, "Playback is not active, skipping lockscreen player.")
            return
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true

        if (!isLocked) {
            Log.d(TAG, "Keyguard is not locked, skipping lockscreen player.")
            return
        }

        try {
            Log.i(TAG, "Launching LockScreenPlayerActivity...")
            val intent = Intent(context, LockScreenPlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting LockScreenPlayerActivity", e)
        }
    }
}
