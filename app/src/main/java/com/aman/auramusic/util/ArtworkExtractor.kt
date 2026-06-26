package com.aman.auramusic.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri

object ArtworkExtractor {

    fun getArtwork(
        context: Context,
        songUri: String
    ): Bitmap? {

        return try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                context,
                songUri.toUri()
            )

            val bytes =
                retriever.embeddedPicture

            retriever.release()

            bytes?.let {
                BitmapFactory.decodeByteArray(
                    it,
                    0,
                    it.size
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getDominantColor(
        context: Context,
        songUri: String
    ): Int? {
        val bitmap = getArtwork(context, songUri) ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L

        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
                count++
            }
        }

        return if (count == 0L) {
            null
        } else {
            Color.rgb(
                (red / count).toInt(),
                (green / count).toInt(),
                (blue / count).toInt()
            )
        }
    }
}
