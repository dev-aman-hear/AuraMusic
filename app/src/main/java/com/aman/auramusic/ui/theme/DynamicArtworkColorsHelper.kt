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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

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

        fun fromPreset(presetName: String, isDark: Boolean): PlayerArtworkPalette {
            return when (presetName.trim().lowercase()) {
                "midnight indigo" -> {
                    val p = if (isDark) Color(0xFF1A237E) else Color(0xFFC5CAE9)
                    val s = if (isDark) Color(0xFF0D1642) else Color(0xFF9FA8DA)
                    val t = if (isDark) Color(0xFF070B24) else Color(0xFFE8EAF6)
                    val a = if (isDark) Color(0xFF3949AB) else Color(0xFF7986CB)
                    PlayerArtworkPalette(
                        primary = p, secondary = s, tertiary = t, accent = a,
                        primaryArgb = p.toArgb(), secondaryArgb = s.toArgb(), tertiaryArgb = t.toArgb(), accentArgb = a.toArgb(),
                        isLightArtwork = !isDark
                    )
                }
                "crimson velvet" -> {
                    val p = if (isDark) Color(0xFF880E4F) else Color(0xFFF8BBD0)
                    val s = if (isDark) Color(0xFF4A0027) else Color(0xFFF48FB1)
                    val t = if (isDark) Color(0xFF260013) else Color(0xFFFCE4EC)
                    val a = if (isDark) Color(0xFFC2185B) else Color(0xFFEC407A)
                    PlayerArtworkPalette(
                        primary = p, secondary = s, tertiary = t, accent = a,
                        primaryArgb = p.toArgb(), secondaryArgb = s.toArgb(), tertiaryArgb = t.toArgb(), accentArgb = a.toArgb(),
                        isLightArtwork = !isDark
                    )
                }
                "emerald pine" -> {
                    val p = if (isDark) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
                    val s = if (isDark) Color(0xFF0B330F) else Color(0xFFA5D6A7)
                    val t = if (isDark) Color(0xFF051C08) else Color(0xFFE8F5E9)
                    val a = if (isDark) Color(0xFF2E7D32) else Color(0xFF81C784)
                    PlayerArtworkPalette(
                        primary = p, secondary = s, tertiary = t, accent = a,
                        primaryArgb = p.toArgb(), secondaryArgb = s.toArgb(), tertiaryArgb = t.toArgb(), accentArgb = a.toArgb(),
                        isLightArtwork = !isDark
                    )
                }
                "nordic slate" -> {
                    val p = if (isDark) Color(0xFF263238) else Color(0xFFCFD8DC)
                    val s = if (isDark) Color(0xFF151C1F) else Color(0xFFB0BEC5)
                    val t = if (isDark) Color(0xFF0B0F10) else Color(0xFFECEFF1)
                    val a = if (isDark) Color(0xFF37474F) else Color(0xFF90A4AE)
                    PlayerArtworkPalette(
                        primary = p, secondary = s, tertiary = t, accent = a,
                        primaryArgb = p.toArgb(), secondaryArgb = s.toArgb(), tertiaryArgb = t.toArgb(), accentArgb = a.toArgb(),
                        isLightArtwork = !isDark
                    )
                }
                else -> neutral(isDark)
            }
        }

        fun fromCustomColors(hexList: List<String>, isDark: Boolean): PlayerArtworkPalette {
            val colors = hexList.mapNotNull { hex ->
                runCatching {
                    val clean = hex.trim().removePrefix("#")
                    val colorInt = if (clean.length == 6) {
                        (0xFF000000 or clean.toLong(16)).toInt()
                    } else if (clean.length == 8) {
                        clean.toLong(16).toInt()
                    } else null
                    colorInt?.let { Color(it) }
                }.getOrNull()
            }
            if (colors.size < 2) return neutral(isDark)

            val p = colors[0]
            val s = colors[1]
            val t = if (colors.size > 2) colors[2] else s
            val a = if (colors.size > 3) colors[3] else p

            return PlayerArtworkPalette(
                primary = p,
                secondary = s,
                tertiary = t,
                accent = a,
                primaryArgb = p.toArgb(),
                secondaryArgb = s.toArgb(),
                tertiaryArgb = t.toArgb(),
                accentArgb = a.toArgb(),
                isLightArtwork = !isDark
            )
        }
    }
}

/**
 * Represents the selectable player theme modes in Settings:
 * Dynamic Artwork, Preset, and Custom.
 */
enum class PlayerThemeMode(val displayName: String) {
    DYNAMIC_ARTWORK("Dynamic Artwork"),
    PRESET("Preset"),
    CUSTOM("Custom");

    companion object {
        fun fromString(value: String): PlayerThemeMode {
            return when (value.trim().lowercase()) {
                "dynamic artwork", "dynamic_artwork", "dynamic" -> DYNAMIC_ARTWORK
                "custom", "custom palette", "custom_palette" -> CUSTOM
                else -> PRESET
            }
        }
    }
}

/**
 * Represents the player's active palette source mode.
 */
enum class PlayerPaletteMode {
    MONOCHROME,
    PRESET,
    DYNAMIC_ARTWORK,
    CUSTOM;

    val isColored: Boolean
        get() = this != MONOCHROME
}

/**
 * Resolves the effective palette mode from unified [PlayerThemeMode], preset selection, and custom hexes.
 */
fun resolvePlayerPaletteMode(
    playerThemeMode: String,
    colorPreset: String,
    customPaletteHexes: String = "",
    dynamicArtworkColors: Boolean = false
): PlayerPaletteMode {
    val mode = PlayerThemeMode.fromString(playerThemeMode)
    return when (mode) {
        PlayerThemeMode.DYNAMIC_ARTWORK -> PlayerPaletteMode.DYNAMIC_ARTWORK
        PlayerThemeMode.CUSTOM -> PlayerPaletteMode.CUSTOM
        PlayerThemeMode.PRESET -> {
            if (colorPreset.equals("Monochrome", ignoreCase = true)) {
                PlayerPaletteMode.MONOCHROME
            } else {
                PlayerPaletteMode.PRESET
            }
        }
    }
}

/**
 * Resolves the effective palette mode following strict precedence:
 * 1. Dynamic Artwork Colors (if enabled) takes highest precedence and adapts to current track art.
 * 2. Static Preset (if non-monochrome preset selected).
 * 3. Custom Palette (if custom hex list present).
 * 4. Fallback to Monochrome (clean neutral scheme).
 */
fun resolvePlayerPaletteMode(
    dynamicArtworkColors: Boolean,
    colorPreset: String,
    customPaletteHexes: String = ""
): PlayerPaletteMode {
    val mode = if (dynamicArtworkColors) {
        PlayerThemeMode.DYNAMIC_ARTWORK
    } else if (customPaletteHexes.isNotBlank() && colorPreset.equals("Custom", ignoreCase = true)) {
        PlayerThemeMode.CUSTOM
    } else if (colorPreset.isNotBlank() && !colorPreset.equals("Monochrome", ignoreCase = true)) {
        PlayerThemeMode.PRESET
    } else if (customPaletteHexes.isNotBlank()) {
        PlayerThemeMode.CUSTOM
    } else {
        PlayerThemeMode.PRESET
    }
    return resolvePlayerPaletteMode(
        playerThemeMode = mode.displayName,
        colorPreset = colorPreset,
        customPaletteHexes = customPaletteHexes,
        dynamicArtworkColors = dynamicArtworkColors
    )
}

/**
 * Unified semantic color scheme for player surfaces across the expanded player,
 * mini player, queue, lyrics, and dialogs.
 */
data class PlayerColorScheme(
    val isDynamic: Boolean,
    val isDark: Boolean,
    val ambientPrimary: Color,
    val ambientSecondary: Color,
    val ambientTertiary: Color,
    val ambientAccent: Color,
    val buttonSurface: Color,
    val buttonBorder: Color,
    val buttonIconTint: Color,
    val prominentSurface: Color,
    val prominentBorder: Color,
    val prominentIconTint: Color,
    val capsuleSurface: Color,
    val capsuleBorder: Color,
    val capsuleContent: Color,
    val progressTrack: Color,
    val progressIndicator: Color,
    val queueItemActiveSurface: Color,
    val queueItemActiveBorder: Color,
    val queueItemActiveContent: Color,
    val queueItemDraggingSurface: Color,
    val queueActionSurface: Color,
    val queueActionContent: Color,
    val queueActionActiveSurface: Color,
    val queueActionActiveContent: Color,
    val dragHandle: Color,
    val dialogSurface: Color,
    val dialogBorder: Color,
    val dialogAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val queueItemInactiveTitle: Color = textPrimary,
    val queueItemInactiveArtist: Color = textSecondary,
    val paletteMode: PlayerPaletteMode = if (isDynamic) PlayerPaletteMode.DYNAMIC_ARTWORK else PlayerPaletteMode.MONOCHROME
)

/**
 * Pure calculation function resolving the unified PlayerColorScheme.
 */
fun resolvePlayerColorScheme(
    isDynamic: Boolean,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    accent: Color,
    surfaceVariant: Color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE5E5EA),
    outline: Color = if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6),
    outlineVariant: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
    onSurface: Color = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
    onSurfaceVariant: Color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
    themePrimary: Color = if (isDark) Color.White else Color.Black,
    onPrimary: Color = if (isDark) Color.Black else Color.White,
    background: Color = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
    onBackground: Color = if (isDark) Color.White else Color(0xFF111111),
    paletteMode: PlayerPaletteMode = if (isDynamic) PlayerPaletteMode.DYNAMIC_ARTWORK else PlayerPaletteMode.MONOCHROME
): PlayerColorScheme {
    val fixedProminentSurface = if (isDark) Color.White else Color.Black
    val fixedProminentBorder = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.20f)
    val fixedProminentIconTint = if (isDark) Color.Black else Color.White

    if (!isDynamic) {
        return PlayerColorScheme(
            isDynamic = false,
            isDark = isDark,
            ambientPrimary = background,
            ambientSecondary = background,
            ambientTertiary = background,
            ambientAccent = background,
            buttonSurface = surfaceVariant,
            buttonBorder = outline,
            buttonIconTint = onSurface,
            prominentSurface = fixedProminentSurface,
            prominentBorder = fixedProminentBorder,
            prominentIconTint = fixedProminentIconTint,
            capsuleSurface = surfaceVariant,
            capsuleBorder = outlineVariant,
            capsuleContent = onSurface,
            progressTrack = outline,
            progressIndicator = themePrimary,
            queueItemActiveSurface = surfaceVariant,
            queueItemActiveBorder = Color.Transparent,
            queueItemActiveContent = themePrimary,
            queueItemDraggingSurface = surfaceVariant.copy(alpha = 0.7f),
            queueActionSurface = Color.Transparent,
            queueActionContent = onBackground,
            queueActionActiveSurface = Color.Transparent,
            queueActionActiveContent = onBackground,
            dragHandle = onSurfaceVariant.copy(alpha = 0.40f),
            dialogSurface = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
            dialogBorder = outline,
            dialogAccent = themePrimary,
            textPrimary = onBackground,
            textSecondary = onSurfaceVariant,
            queueItemInactiveTitle = onBackground,
            queueItemInactiveArtist = onSurfaceVariant,
            paletteMode = PlayerPaletteMode.MONOCHROME
        )
    }

    val accentArgb = accent.toArgb()
    val accentLuminance = ColorHarmonizer.calculateLuminance(accentArgb)
    val prominentIcon = if (accentLuminance > 0.55f) Color(0xFF111111) else Color.White
    val highContrastText = if (isDark) Color.White else Color(0xFF111111)
    val secondaryText = if (isDark) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.68f)

    // Ensure accent for queue item active content has adequate contrast against the ambient surface
    val contrastSafeAccent = if (isDark) {
        if (accentLuminance < 0.38f) {
            val hsl = ColorHarmonizer.rgbToHsl(accentArgb)
            Color(ColorHarmonizer.hslToRgb(hsl[0], hsl[1].coerceAtLeast(0.40f), 0.58f))
        } else accent
    } else {
        if (accentLuminance > 0.65f) {
            val hsl = ColorHarmonizer.rgbToHsl(accentArgb)
            Color(ColorHarmonizer.hslToRgb(hsl[0], hsl[1].coerceAtLeast(0.40f), 0.40f))
        } else accent
    }

    // Subtle dynamic ambient tinting for inactive queue titles and artists
    val queueInactiveTitle = if (isDark) {
        Color(
            red = (Color.White.red * 0.85f + primary.red * 0.15f),
            green = (Color.White.green * 0.85f + primary.green * 0.15f),
            blue = (Color.White.blue * 0.85f + primary.blue * 0.15f),
            alpha = 0.95f
        )
    } else {
        Color(
            red = (0.08f * 0.85f + primary.red * 0.15f).coerceIn(0f, 0.22f),
            green = (0.08f * 0.85f + primary.green * 0.15f).coerceIn(0f, 0.22f),
            blue = (0.08f * 0.85f + primary.blue * 0.15f).coerceIn(0f, 0.22f),
            alpha = 1.0f
        )
    }
    val queueInactiveArtist = if (isDark) {
        queueInactiveTitle.copy(alpha = 0.70f)
    } else {
        queueInactiveTitle.copy(alpha = 0.65f)
    }

    return PlayerColorScheme(
        isDynamic = true,
        isDark = isDark,
        ambientPrimary = primary,
        ambientSecondary = secondary,
        ambientTertiary = tertiary,
        ambientAccent = accent,
        // ~75% opacity (roughly 25% transparency) for translucent button surfaces
        buttonSurface = secondary.copy(alpha = if (isDark) 0.75f else 0.72f),
        buttonBorder = primary.copy(alpha = if (isDark) 0.35f else 0.30f),
        buttonIconTint = if (isDark) Color.White else Color(0xFF181818),
        // Prominent Play/Pause button MUST remain strictly monochrome: black in light mode, white in dark mode
        prominentSurface = fixedProminentSurface,
        prominentBorder = fixedProminentBorder,
        prominentIconTint = fixedProminentIconTint,
        // Secondary capsules (Quality, Switcher pill)
        capsuleSurface = secondary.copy(alpha = 0.75f),
        capsuleBorder = primary.copy(alpha = 0.30f),
        capsuleContent = if (isDark) Color.White else Color(0xFF181818),
        // Scrubber / Progress
        progressTrack = secondary.copy(alpha = 0.40f),
        progressIndicator = accent,
        // Queue current track highlight & dragging
        queueItemActiveSurface = primary.copy(alpha = if (isDark) 0.28f else 0.22f),
        queueItemActiveBorder = primary.copy(alpha = if (isDark) 0.45f else 0.35f),
        queueItemActiveContent = contrastSafeAccent,
        queueItemDraggingSurface = secondary.copy(alpha = 0.65f),
        // Queue action pills (Clear, Save, Shuffle, Repeat)
        queueActionSurface = secondary.copy(alpha = 0.50f),
        queueActionContent = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f),
        queueActionActiveSurface = accent.copy(alpha = 0.75f),
        queueActionActiveContent = prominentIcon,
        // Drag handle
        dragHandle = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f),
        // Dialogs
        dialogSurface = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.95f) else Color(0xFFF2F2F7).copy(alpha = 0.95f),
        dialogBorder = primary.copy(alpha = 0.35f),
        dialogAccent = accent,
        textPrimary = highContrastText,
        textSecondary = secondaryText,
        queueItemInactiveTitle = queueInactiveTitle,
        queueItemInactiveArtist = queueInactiveArtist,
        paletteMode = paletteMode
    )
}

/**
 * Pure calculation overload resolving PlayerColorScheme directly from PlayerPaletteMode.
 */
fun resolvePlayerColorScheme(
    paletteMode: PlayerPaletteMode,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    accent: Color,
    surfaceVariant: Color = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE5E5EA),
    outline: Color = if (isDark) Color(0xFF38383A) else Color(0xFFD1D1D6),
    outlineVariant: Color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
    onSurface: Color = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
    onSurfaceVariant: Color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
    themePrimary: Color = if (isDark) Color.White else Color.Black,
    onPrimary: Color = if (isDark) Color.Black else Color.White,
    background: Color = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF),
    onBackground: Color = if (isDark) Color.White else Color(0xFF111111)
): PlayerColorScheme = resolvePlayerColorScheme(
    isDynamic = paletteMode.isColored,
    isDark = isDark,
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    accent = accent,
    surfaceVariant = surfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    themePrimary = themePrimary,
    onPrimary = onPrimary,
    background = background,
    onBackground = onBackground,
    paletteMode = paletteMode
)

/**
 * Remember and observe unified PlayerColorScheme in Compose.
 */
@Composable
fun rememberPlayerColorScheme(
    isDynamic: Boolean,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    accent: Color,
    paletteMode: PlayerPaletteMode = if (isDynamic) PlayerPaletteMode.DYNAMIC_ARTWORK else PlayerPaletteMode.MONOCHROME
): PlayerColorScheme {
    val cs = MaterialTheme.colorScheme
    return remember(isDynamic, isDark, primary, secondary, tertiary, accent, paletteMode, cs) {
        resolvePlayerColorScheme(
            isDynamic = isDynamic,
            isDark = isDark,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            accent = accent,
            surfaceVariant = cs.surfaceVariant,
            outline = cs.outline,
            outlineVariant = cs.outlineVariant,
            onSurface = cs.onSurface,
            onSurfaceVariant = cs.onSurfaceVariant,
            themePrimary = cs.primary,
            onPrimary = cs.onPrimary,
            background = cs.background,
            onBackground = cs.onBackground,
            paletteMode = paletteMode
        )
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
    /**
     * Converts Palette swatches into a harmonious [PlayerArtworkPalette].
     * Evaluates dominant, vibrant, muted, and all swatches to intelligently select
     * Primary (ambient dominant), Secondary (supporting depth), and Accent (controlled highlight).
     */
    fun processPalette(palette: Palette, isDark: Boolean): PlayerArtworkPalette {
        // Collect candidate swatches with preference for high population and vibrancy
        val targeted = listOfNotNull(
            palette.dominantSwatch,
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch
        )
        // Combine with all palette swatches sorted by population
        val allSwatches = (targeted + palette.swatches)
            .distinctBy { it.rgb }
            .sortedByDescending { it.population }

        return processColors(
            colors = allSwatches.map { it.rgb },
            isDark = isDark,
            dominantRgb = palette.dominantSwatch?.rgb
        )
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
     * Intelligently selects Primary, Secondary, and Accent from a list of candidate colors.
     * Evaluates saturation, luminance, visual distinctness, and harmony for Apple Music-style ambient depth.
     */
    fun processColors(
        colors: List<Int>,
        isDark: Boolean,
        dominantRgb: Int? = null
    ): PlayerArtworkPalette {
        if (colors.isEmpty()) {
            return PlayerArtworkPalette.neutral(isDark)
        }

        val hslList = colors.map { ColorHarmonizer.rgbToHsl(it) }

        // 1. Detect grayscale or monochromatic artwork (clean neutral fallback)
        val maxSaturation = hslList.maxOfOrNull { it[1] } ?: 0f
        if (maxSaturation < 0.08f) {
            return PlayerArtworkPalette.neutral(isDark)
        }

        // 2. Detect extreme darkness or brightness across all swatches
        val avgLuminance = colors.map { ColorHarmonizer.calculateLuminance(it) }.average().toFloat()
        if (avgLuminance < 0.03f || avgLuminance > 0.97f) {
            return PlayerArtworkPalette.neutral(isDark)
        }

        // 3. Filter candidates for useful saturation and lightness
        val candidates = colors.filterIndexed { index, _ ->
            val hsl = hslList[index]
            hsl[1] >= 0.08f && hsl[2] in 0.04f..0.96f
        }.ifEmpty { colors }

        // 4. Select PRIMARY (dominant ambient color)
        // Balances dominant population prominence, rich saturation, and theme-appropriate lightness
        val primaryInt = candidates.maxByOrNull { rgb ->
            val hsl = ColorHarmonizer.rgbToHsl(rgb)
            val s = hsl[1]
            val l = hsl[2]
            val targetL = if (isDark) 0.35f else 0.65f
            val satScore = 1f - kotlin.math.abs(s - 0.55f).coerceAtMost(0.55f)
            val lScore = 1f - kotlin.math.abs(l - targetL).coerceAtMost(0.50f)
            val rankWeight = 1f / (1f + candidates.indexOf(rgb) * 0.20f)
            val dominantBonus = if (dominantRgb != null && rgb == dominantRgb) 0.30f else 0f
            (satScore * 0.35f + lScore * 0.30f + rankWeight * 0.20f + dominantBonus)
        } ?: candidates[0]

        val primaryHsl = ColorHarmonizer.rgbToHsl(primaryInt)

        // 5. Select SECONDARY (complementary or closely related supporting color for gradient depth)
        // Filter candidate colors that are distinct from Primary
        val distinctCandidates = candidates.filter { rgb ->
            rgb != primaryInt && calculateColorDifference(primaryHsl, ColorHarmonizer.rgbToHsl(rgb)) >= 0.16f
        }

        val secondaryInt = if (distinctCandidates.isNotEmpty()) {
            // Select candidate with highest ambient background harmony with Primary
            distinctCandidates.maxByOrNull { rgb ->
                val hsl = ColorHarmonizer.rgbToHsl(rgb)
                val hueDiff = minOf(
                    kotlin.math.abs(primaryHsl[0] - hsl[0]),
                    360f - kotlin.math.abs(primaryHsl[0] - hsl[0])
                )
                val harmonyBonus = when {
                    hueDiff <= 85f -> 1.0f // Analogous harmony (smooth blend)
                    hueDiff in 135f..225f -> 0.70f // Complementary harmony
                    else -> 0.50f
                }
                // Ambient background suitability: prefer moderate saturation and depth
                val ambientSat = 1f - kotlin.math.abs(hsl[1] - 0.50f).coerceAtMost(0.50f)
                val ambientL = if (isDark) {
                    1f - kotlin.math.abs(hsl[2] - 0.30f).coerceAtMost(0.40f)
                } else {
                    1f - kotlin.math.abs(hsl[2] - 0.70f).coerceAtMost(0.40f)
                }
                (harmonyBonus * 0.45f + ambientSat * 0.30f + ambientL * 0.25f)
            } ?: distinctCandidates[0]
        } else {
            // Artwork is monochromatic (only one color hue present in candidates)
            // Synthesize a harmonious, analogous supporting secondary to create subtle gradient depth
            val secondaryH = (primaryHsl[0] + if (primaryHsl[0] > 180f) -25f else 25f + 360f) % 360f
            val secondaryS = (primaryHsl[1] * 0.85f).coerceIn(0.20f, 0.70f)
            val secondaryL = if (isDark) {
                (primaryHsl[2] * 0.80f).coerceIn(0.14f, 0.30f)
            } else {
                (primaryHsl[2] * 1.10f).coerceIn(0.70f, 0.86f)
            }
            ColorHarmonizer.hslToRgb(secondaryH, secondaryS, secondaryL)
        }

        // 6. Select ACCENT (controlled highlight color for progress, selected queue, and important playback controls)
        val accentCandidate = candidates.maxByOrNull { rgb ->
            val hsl = ColorHarmonizer.rgbToHsl(rgb)
            val s = hsl[1]
            val l = hsl[2]
            val satScore = s
            val lScore = if (isDark) (1f - kotlin.math.abs(l - 0.55f)) else (1f - kotlin.math.abs(l - 0.45f))
            satScore * 0.70f + lScore * 0.30f
        } ?: primaryInt

        // 7. Harmonize luminance, saturation, and contrast for theme roles
        val finalPrimary = adjustColorForTheme(primaryInt, isDark, targetLightness = if (isDark) 0.32f else 0.72f)
        val finalSecondary = adjustColorForTheme(secondaryInt, isDark, targetLightness = if (isDark) 0.22f else 0.78f)

        // Tertiary provides an ambient bridge between Primary and Secondary
        val priHsl = ColorHarmonizer.rgbToHsl(finalPrimary)
        val secHsl = ColorHarmonizer.rgbToHsl(finalSecondary)
        val tertH = (priHsl[0] + secHsl[0]) / 2f
        val tertS = ((priHsl[1] + secHsl[1]) / 2f).coerceIn(0.20f, 0.65f)
        val tertL = if (isDark) (secHsl[2] * 0.90f).coerceIn(0.14f, 0.26f) else (secHsl[2] * 1.05f).coerceIn(0.75f, 0.88f)
        val finalTertiary = ColorHarmonizer.hslToRgb(tertH, tertS, tertL)

        // Accent contrast and saturation safeguards:
        // Must have sufficient contrast against dark or light ambient surfaces
        val accHsl = ColorHarmonizer.rgbToHsl(accentCandidate)
        val safeAccentS = accHsl[1].coerceIn(0.35f, 0.82f)
        val safeAccentL = if (isDark) {
            accHsl[2].coerceIn(0.48f, 0.72f) // Bright and luminous in dark mode
        } else {
            accHsl[2].coerceIn(0.30f, 0.48f) // Deep and high contrast in light mode
        }
        val finalAccent = ColorHarmonizer.hslToRgb(accHsl[0], safeAccentS, safeAccentL)

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
     * Calculates visual perceptual distance between two colors using weighted HSL differences.
     */
    fun calculateColorDifference(c1Hsl: FloatArray, c2Hsl: FloatArray): Float {
        val hDiff = minOf(
            kotlin.math.abs(c1Hsl[0] - c2Hsl[0]),
            360f - kotlin.math.abs(c1Hsl[0] - c2Hsl[0])
        ) / 180f
        val sDiff = kotlin.math.abs(c1Hsl[1] - c2Hsl[1])
        val lDiff = kotlin.math.abs(c1Hsl[2] - c2Hsl[2])
        return kotlin.math.sqrt(0.50f * hDiff * hDiff + 0.30f * lDiff * lDiff + 0.20f * sDiff * sDiff)
    }

    /**
     * Adjusts saturation and lightness to ensure harmonious ambient diffusion without
     * neon glare, muddy darkness, or unreadable contrast.
     */
    fun adjustColorForTheme(colorInt: Int, isDark: Boolean, targetLightness: Float): Int {
        val hsl = ColorHarmonizer.rgbToHsl(colorInt)

        // Moderate saturation to prevent harsh neon blooms
        hsl[1] = hsl[1].coerceIn(0.20f, 0.70f)

        // Adjust lightness toward harmonious ambient background range
        hsl[2] = if (isDark) {
            hsl[2].coerceIn(0.16f, targetLightness.coerceAtMost(0.42f))
        } else {
            hsl[2].coerceIn(targetLightness.coerceAtLeast(0.60f), 0.85f)
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

    fun calculateContrastRatio(rgb1: Int, rgb2: Int): Float {
        val lum1 = calculateLuminance(rgb1)
        val lum2 = calculateLuminance(rgb2)
        val brightest = maxOf(lum1, lum2)
        val darkest = minOf(lum1, lum2)
        return (brightest + 0.05f) / (darkest + 0.05f)
    }
}
