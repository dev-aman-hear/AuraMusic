package com.aman.auramusic.online.util

import android.content.Context
import android.net.Uri
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Universal optimizer that upgrades online album art and thumbnail URLs to their
 * highest available studio/HD quality for YouTube Music, YouTube videos, and JioSaavn.
 */
object ArtworkQualityOptimizer {

    private val JIO_DIMENSION_REGEX = Regex(
        """([_-]?)(?:50x50|150x150|250x250|350x350)(\.(?:jpg|jpeg|png|webp))?""",
        RegexOption.IGNORE_CASE
    )
    private val GOOGLE_WIDTH_REGEX = Regex("""=w\d+-h\d+[^?&]*""")
    private val GOOGLE_SQUARE_REGEX = Regex("""=s\d+[^?&]*""")
    private val YOUTUBE_LOWRES_THUMB_REGEX = Regex("""/(?:default|mqdefault)\.jpg""", RegexOption.IGNORE_CASE)

    /**
     * Upgrades an artwork or thumbnail URL to high resolution.
     */
    fun optimizeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        var u = url.trim()

        // Normalize protocol
        if (u.startsWith("//")) {
            u = "https:$u"
        } else if (u.startsWith("http://")) {
            u = "https://" + u.substring(7)
        }

        // 1. JioSaavn: Upgrade 50x50 / 150x150 / 250x250 to 500x500 HD master cover
        if (u.contains("saavncdn.com", ignoreCase = true) ||
            u.contains("jiosaavn.com", ignoreCase = true) ||
            u.contains("saavn.com", ignoreCase = true)
        ) {
            u = JIO_DIMENSION_REGEX.replace(u) { matchResult ->
                val prefix = matchResult.groupValues[1]
                val ext = matchResult.groupValues[2]
                "${prefix}500x500$ext"
            }
            // Fallback manual replace in case pattern didn't match extension
            u = u.replace("150x150", "500x500")
                .replace("50x50", "500x500")
                .replace("250x250", "500x500")
            return u
        }

        // 2. YouTube Music / Google User Content / Ggpht
        // Upgrade tiny mobile thumbnails (=w60-h60, =w120-h120, =s88) to 1080p uncompressed master covers
        if (u.contains("googleusercontent.com", ignoreCase = true) ||
            u.contains("ggpht.com", ignoreCase = true)
        ) {
            u = if (u.contains("=w")) {
                GOOGLE_WIDTH_REGEX.replace(u, "=w1080-h1080-l90-rj")
            } else if (u.contains("=s")) {
                GOOGLE_SQUARE_REGEX.replace(u, "=s800-c-k-c0x00ffffff-no-rj")
            } else {
                u
            }
            return u
        }

        // 3. YouTube standard video thumbnails (i.ytimg.com / piped proxy)
        if (u.contains("ytimg.com", ignoreCase = true) ||
            u.contains("pipedproxy", ignoreCase = true) ||
            u.contains("/vi/", ignoreCase = true)
        ) {
            // Strip compression query params (?sqp=... &rs=...) which force mobile web downsampling
            if (u.contains("?")) {
                u = u.substringBefore("?")
            }
            // Upgrade low-res default (120x90) and mqdefault (320x180) to clean hqdefault (480x360)
            u = YOUTUBE_LOWRES_THUMB_REGEX.replace(u, "/hqdefault.jpg")
            return u
        }

        // 4. Unsplash curated playlist covers
        if (u.contains("images.unsplash.com", ignoreCase = true)) {
            u = u.replace("w=500", "w=1080").replace("q=80", "q=85")
            return u
        }

        return u
    }

    /**
     * Optimizes any model object (String, Uri, etc.) before passing to Coil.
     */
    fun optimizeModel(model: Any?): Any? {
        return when (model) {
            null -> null
            is String -> optimizeUrl(model)
            is Uri -> {
                val str = model.toString()
                val optimized = optimizeUrl(str)
                if (optimized != str) Uri.parse(optimized) else model
            }
            else -> model
        }
    }

    /**
     * Creates a high-fidelity Coil [ImageRequest] with optimized caching and crossfade.
     */
    fun buildImageRequest(context: Context, model: Any?): ImageRequest {
        val targetModel = optimizeModel(model)
        return ImageRequest.Builder(context)
            .data(targetModel)
            .crossfade(250)
            .allowHardware(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
