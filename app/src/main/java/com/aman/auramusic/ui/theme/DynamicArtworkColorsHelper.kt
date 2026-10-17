package com.aman.auramusic.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.util.ArtworkQualityOptimizer
import com.aman.auramusic.util.ArtworkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsulates the extracted harmonious colors for the Apple Music-inspired ambient background.
 */
data class PlayerArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val primaryArgb: Int = 0,
    val secondaryArgb: Int = 0,
    val tertiaryArgb: Int = 0,
    val accentArgb: Int = 0,
    val isLightArtwork: Boolean = false
) {
    companion object {
        fun neutral(isDark: Boolean): PlayerArtworkPalette {
            return if (isDark) {
                PlayerArtworkPalette(
                    primary = Color(0xFF1E1E1E),
                    secondary = Color(0xFF161616),
                    tertiary = Color(0xFF121212),
                    accent = Color(0xFF282828),
                    primaryArgb = 0xFF1E1E1E.toInt(),
                    secondaryArgb = 0xFF161616.toInt(),
                    tertiaryArgb = 0xFF121212.toInt(),
                    accentArgb = 0xFF282828.toInt(),
                    isLightArtwork = false
                )
            } else {
                PlayerArtworkPalette(
                    primary = Color(0xFFE8E8E8),
                    secondary = Color(0xFFEEEEEE),
                    tertiary = Color(0xFFF5F5F5),
                    accent = Color(0xFFDFDFDF),
                    primaryArgb = 0xFFE8E8E8.toInt(),
                    secondaryArgb = 0xFFEEEEEE.toInt(),
                    tertiaryArgb = 0xFFF5F5F5.toInt(),
                    accentArgb = 0xFFDFDFDF.toInt(),
                    isLightArtwork = true
                )
            }
        }
    }
}

/**
 * Reusable helper for high-performance, asynchronous artwork palette extraction and caching.
 */
object DynamicArtworkColorsHelper {

    private const val MAX_CACHE_ENTRIES = 32
    private const val TARGET_BITMAP_SIZE = 160

    private val cacheLock = Any()
    private val cache = object : LinkedHashMap<String, PlayerArtworkPalette>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PlayerArtworkPalette>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    /**
     * Extracts a balanced, multi-color ambient palette from the given [Song]'s artwork.
     * Guaranteed to execute off the main thread, reuse LRU cache, and never throw exceptions.
     */
    suspend fun extractPalette(
        context: Context,
        song: Song,
        isDark: Boolean
    ): PlayerArtworkPalette = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(song, isDark)
        if (cacheKey.isNotEmpty()) {
            synchronized(cacheLock) {
                cache[cacheKey]?.let { return@withContext it }
            }
        }

        try {
            val bitmap = decodeArtworkThumbnail(context, song)
            if (bitmap == null) {
                val fallback = PlayerArtworkPalette.neutral(isDark)
                if (cacheKey.isNotEmpty()) {
                    synchronized(cacheLock) { cache[cacheKey] = fallback }
                }
                return@withContext fallback
            }

            val palette = try {
                Palette.from(bitmap)
                    .maximumColorCount(16)
                    .generate()
            } finally {
                // We do not hold a strong reference to the bitmap after palette generation
            }

            val extracted = processPalette(palette, isDark)
            if (cacheKey.isNotEmpty()) {
                synchronized(cacheLock) { cache[cacheKey] = extracted }
            }
            extracted
        } catch (_: Exception) {
            PlayerArtworkPalette.neutral(isDark)
        }
    }

    /**
     * Generates a unique key for the LRU cache.
     */
    private fun buildCacheKey(song: Song, isDark: Boolean): String {
        val base = song.artworkUri?.takeIf { it.isNotBlank() }
            ?: song.uri.takeIf { it.isNotBlank() }
            ?: "${song.id}_${song.title}_${song.artist}"
        return "${base}_isDark=$isDark"
    }

    /**
     * Decodes a downsampled thumbnail of the artwork off the main thread to minimize memory.
     */
    private suspend fun decodeArtworkThumbnail(context: Context, song: Song): Bitmap? {
        val artUriStr = song.artworkUri?.trim() ?: ""

        // 1. Online artwork (HTTP/HTTPS)
        if (artUriStr.startsWith("http://", ignoreCase = true) ||
            artUriStr.startsWith("https://", ignoreCase = true)
        ) {
            val optimizedUrl = ArtworkQualityOptimizer.optimizeUrl(artUriStr)
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(optimizedUrl)
                .size(TARGET_BITMAP_SIZE, TARGET_BITMAP_SIZE)
                .allowHardware(false)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            return (result as? BitmapDrawable)?.bitmap
        }

        // 2. Embedded artwork from standalone external audio file
        if (song.id == -1L && song.uri.isNotBlank()) {
            val raw = ArtworkExtractor.getArtwork(context, song.uri) ?: return null
            return if (raw.width > TARGET_BITMAP_SIZE || raw.height > TARGET_BITMAP_SIZE) {
                Bitmap.createScaledBitmap(raw, TARGET_BITMAP_SIZE, TARGET_BITMAP_SIZE, true)
            } else {
                raw
            }
        }

        // 3. Local ContentResolver URI
        if (artUriStr.isNotBlank()) {
            return runCatching {
                val uri = Uri.parse(artUriStr)
                // Downsample decode using BitmapFactory
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                val sampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    boundsOptions.outHeight,
                    TARGET_BITMAP_SIZE,
                    TARGET_BITMAP_SIZE
                )

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull()
        }

        // 4. Fallback: try extracting from song.uri if available
        if (song.uri.isNotBlank() && song.uri.startsWith("content://")) {
            return runCatching {
                val uri = Uri.parse(song.uri)
                val raw = ArtworkExtractor.getArtwork(context, uri.toString()) ?: return null
                if (raw.width > TARGET_BITMAP_SIZE || raw.height > TARGET_BITMAP_SIZE) {
                    Bitmap.createScaledBitmap(raw, TARGET_BITMAP_SIZE, TARGET_BITMAP_SIZE, true)
                } else {
                    raw
                }
            }.getOrNull()
        }

        return null
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Converts Palette swatches into a harmonious [PlayerArtworkPalette].
     * Detects low saturation/monochrome art, excessive neon brightness, or pure darkness.
     */
    fun processPalette(palette: Palette, isDark: Boolean): PlayerArtworkPalette {
        val swatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch,
            palette.dominantSwatch
        )

        return processSwatches(swatches, isDark, palette.dominantSwatch?.rgb)
    }

    /**
     * Process list of swatches with harmonious theme adjustments and saturation safeguards.
     */
    fun processSwatches(
        swatches: List<Palette.Swatch>,
        isDark: Boolean,
        dominantRgb: Int? = null
    ): PlayerArtworkPalette {
        return processColors(swatches.map { it.rgb }, isDark, dominantRgb)
    }

    /**
     * Process list of color integers with harmonious theme adjustments and saturation safeguards.
     */
    fun processColors(
        colors: List<Int>,
        isDark: Boolean,
        dominantRgb: Int? = null
    ): PlayerArtworkPalette {
        if (colors.isEmpty()) {
            return PlayerArtworkPalette.neutral(isDark)
        }

        // Check if artwork is predominantly grayscale/low-saturation
        val avgSaturation = colors.map { color ->
            ColorHarmonizer.rgbToHsl(color)[1]
        }.average().toFloat()

        if (avgSaturation < 0.08f) {
            // Artwork is grayscale or monochromatic - preserve clean neutral aesthetic
            return PlayerArtworkPalette.neutral(isDark)
        }

        val primaryInt = colors.firstOrNull() ?: colors[0]
        val secondaryInt = colors.getOrNull(1) ?: primaryInt
        val tertiaryInt = colors.getOrNull(2) ?: secondaryInt
        val accentInt = colors.getOrNull(3) ?: primaryInt

        // Harmonize luminance and saturation for theme contrast
        val finalPrimary = adjustColorForTheme(primaryInt, isDark, targetLightness = if (isDark) 0.38f else 0.62f)
        val finalSecondary = adjustColorForTheme(secondaryInt, isDark, targetLightness = if (isDark) 0.28f else 0.72f)
        val finalTertiary = adjustColorForTheme(tertiaryInt, isDark, targetLightness = if (isDark) 0.22f else 0.78f)
        val finalAccent = adjustColorForTheme(accentInt, isDark, targetLightness = if (isDark) 0.45f else 0.58f)

        val dominantLuminance = ColorHarmonizer.calculateLuminance(dominantRgb ?: primaryInt)
        val isLightArtwork = dominantLuminance > 0.5f

        return PlayerArtworkPalette(
            primary = Color(finalPrimary),
            secondary = Color(finalSecondary),
            tertiary = Color(finalTertiary),
            accent = Color(finalAccent),
            primaryArgb = finalPrimary,
            secondaryArgb = finalSecondary,
            tertiaryArgb = finalTertiary,
            accentArgb = finalAccent,
            isLightArtwork = isLightArtwork
        )
    }

    /**
     * Adjusts saturation and lightness to ensure harmonious ambient diffusion without
     * neon glare, muddy darkness, or unreadable contrast.
     */
    fun adjustColorForTheme(colorInt: Int, isDark: Boolean, targetLightness: Float): Int {
        val hsl = ColorHarmonizer.rgbToHsl(colorInt)

        // Moderate saturation to prevent harsh neon blooms
        hsl[1] = hsl[1].coerceIn(0.25f, 0.75f)

        // Adjust lightness toward harmonious ambient background range
        hsl[2] = if (isDark) {
            hsl[2].coerceIn(0.18f, targetLightness.coerceAtMost(0.48f))
        } else {
            hsl[2].coerceIn(targetLightness.coerceAtLeast(0.55f), 0.82f)
        }

        return ColorHarmonizer.hslToRgb(hsl[0], hsl[1], hsl[2])
    }

    /**
     * Clear the in-memory cache (e.g. for testing or memory pressure).
     */
    fun clearCache() = synchronized(cacheLock) {
        cache.clear()
    }
}

/**
 * Pure Kotlin color calculations for RGB <-> HSL and luminance.
 * Independent of Android SDK runtime, allowing deterministic, high-speed execution in tests and production.
 */
object ColorHarmonizer {

    fun rgbToHsl(rgb: Int): FloatArray {
        val r = ((rgb shr 16) and 0xFF) / 255.0f
        val g = ((rgb shr 8) and 0xFF) / 255.0f
        val b = (rgb and 0xFF) / 255.0f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        var h = 0f
        var s = 0f
        val l = (max + min) / 2.0f

        if (delta != 0f) {
            s = if (l <= 0.5f) delta / (max + min) else delta / (2.0f - max - min)
            h = when (max) {
                r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
                g -> ((b - r) / delta + 2f) * 60f
                else -> ((r - g) / delta + 4f) * 60f
            }
        }
        return floatArrayOf(h, s, l)
    }

    fun hslToRgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val normalizedH = ((h % 360f) + 360f) % 360f
        val x = c * (1f - kotlin.math.abs((normalizedH / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when {
            normalizedH < 60f -> Triple(c, x, 0f)
            normalizedH < 120f -> Triple(x, c, 0f)
            normalizedH < 180f -> Triple(0f, c, x)
            normalizedH < 240f -> Triple(0f, x, c)
            normalizedH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = kotlin.math.round((rPrime + m) * 255f).toInt().coerceIn(0, 255)
        val g = kotlin.math.round((gPrime + m) * 255f).toInt().coerceIn(0, 255)
        val b = kotlin.math.round((bPrime + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun calculateLuminance(rgb: Int): Float {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
