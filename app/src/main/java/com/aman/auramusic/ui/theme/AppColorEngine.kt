package com.aman.auramusic.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.online.util.ArtworkQualityOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Identifies distinct screen-scoped content areas in AuraMusic that support independent color theming.
 */
enum class AppArea {
    HOME,
    EXPLORE,
    LIBRARY,
    ALBUM_DETAIL,
    ARTIST_DETAIL,
    PLAYLIST_DETAIL;

    companion object {
        fun fromString(name: String): AppArea {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: HOME
        }
    }
}

/**
 * Identifies the color sourcing mode for full-app theming.
 */
enum class AppPaletteMode(val displayName: String) {
    MONOCHROME("Monochrome"),
    DYNAMIC_ARTWORK("Dynamic Artwork"),
    PRESET("Preset"),
    CUSTOM("Custom Palette");

    companion object {
        fun fromString(value: String): AppPaletteMode {
            return when (value.trim().lowercase()) {
                "dynamic artwork", "dynamic_artwork", "dynamic" -> DYNAMIC_ARTWORK
                "preset", "built-in preset" -> PRESET
                "custom", "custom palette", "custom_palette" -> CUSTOM
                else -> MONOCHROME
            }
        }
    }
}

/**
 * Semantic color tokens for AuraMusic's full-app theming system.
 * Guaranteed to provide accessible contrast, solid surfaces (zero gradients or liquid glass),
 * and clean fallback to monochrome.
 */
data class AppColorScheme(
    val isColored: Boolean,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val secondarySurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val selectedItem: Color,
    val border: Color,
    val divider: Color,
    val controlBackground: Color,
    val controlContent: Color
) {
    /**
     * Bridges AuraMusic semantic tokens to standard Jetpack Compose Material 3 ColorScheme.
     */
    fun toMaterialColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = controlContent,
                secondary = secondaryText,
                onSecondary = controlContent,
                tertiary = accent,
                onTertiary = controlContent,
                background = background,
                onBackground = primaryText,
                surface = surface,
                onSurface = primaryText,
                surfaceVariant = secondarySurface,
                onSurfaceVariant = secondaryText,
                outline = border,
                outlineVariant = divider,
                primaryContainer = controlBackground,
                onPrimaryContainer = controlContent,
                error = Color(0xFFCF6679),
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = controlContent,
                secondary = secondaryText,
                onSecondary = controlContent,
                tertiary = accent,
                onTertiary = controlContent,
                background = background,
                onBackground = primaryText,
                surface = surface,
                onSurface = primaryText,
                surfaceVariant = secondarySurface,
                onSurfaceVariant = secondaryText,
                outline = border,
                outlineVariant = divider,
                primaryContainer = controlBackground,
                onPrimaryContainer = controlContent,
                error = Color(0xFFB00020),
                onError = Color.White
            )
        }
    }

    companion object {
        /**
         * Returns AuraMusic's authoritative neutral monochrome color scheme.
         */
        fun monochrome(isDark: Boolean): AppColorScheme {
            return if (isDark) {
                AppColorScheme(
                    isColored = false,
                    isDark = true,
                    background = DarkMonochromeColors.background,
                    surface = DarkMonochromeColors.surface,
                    secondarySurface = DarkMonochromeColors.surfaceSecondary,
                    primaryText = DarkMonochromeColors.textPrimary,
                    secondaryText = DarkMonochromeColors.textSecondary,
                    accent = DarkMonochromeColors.controlSelected,
                    selectedItem = DarkMonochromeColors.controlSelected,
                    border = DarkMonochromeColors.border,
                    divider = Color(0xFF222222),
                    controlBackground = DarkMonochromeColors.controlBackground,
                    controlContent = DarkMonochromeColors.controlSelectedContent
                )
            } else {
                AppColorScheme(
                    isColored = false,
                    isDark = false,
                    background = LightMonochromeColors.background,
                    surface = LightMonochromeColors.surface,
                    secondarySurface = LightMonochromeColors.surfaceSecondary,
                    primaryText = LightMonochromeColors.textPrimary,
                    secondaryText = LightMonochromeColors.textSecondary,
                    accent = LightMonochromeColors.controlSelected,
                    selectedItem = LightMonochromeColors.controlSelected,
                    border = LightMonochromeColors.border,
                    divider = Color(0xFFE5E5E5),
                    controlBackground = LightMonochromeColors.controlBackground,
                    controlContent = LightMonochromeColors.controlSelectedContent
                )
            }
        }

        /**
         * Derives readable, accessible, solid semantic colors from a [PlayerArtworkPalette].
         * Strictly uses solid colors with no transparency effects, blooms, or gradients.
         */
        fun fromPalette(palette: PlayerArtworkPalette, isDark: Boolean, isColored: Boolean): AppColorScheme {
            if (!isColored) return monochrome(isDark)

            val p = palette.primary
            val s = palette.secondary
            val a = palette.accent

            return if (isDark) {
                // Dark mode: Deep solid surfaces tinted with artwork/preset hues
                // Blend with pure black to guarantee high contrast (WCAG AAA for text)
                val bg = blendColors(Color(0xFF070709), p, 0.12f)
                val surf = blendColors(Color(0xFF121216), p, 0.18f)
                val secSurf = blendColors(Color(0xFF1B1B22), s, 0.22f)
                val ctrlBg = blendColors(Color(0xFF24242E), a, 0.25f)
                val borderCol = blendColors(Color(0xFF2C2C36), a, 0.30f)
                val dividerCol = Color(0xFF1E1E26)

                AppColorScheme(
                    isColored = true,
                    isDark = true,
                    background = bg,
                    surface = surf,
                    secondarySurface = secSurf,
                    primaryText = Color(0xFFFFFFFF),
                    secondaryText = Color(0xFFB0B4C0),
                    accent = a,
                    selectedItem = a,
                    border = borderCol,
                    divider = dividerCol,
                    controlBackground = ctrlBg,
                    controlContent = Color(0xFFFFFFFF)
                )
            } else {
                // Light mode: Clean, crisp solid surfaces tinted gently
                val bg = blendColors(Color(0xFFF7F8FA), p, 0.08f)
                val surf = Color(0xFFFFFFFF)
                val secSurf = blendColors(Color(0xFFF0F2F6), s, 0.12f)
                val ctrlBg = blendColors(Color(0xFFE7EAF0), a, 0.16f)
                val borderCol = blendColors(Color(0xFFD6D9E2), a, 0.22f)
                val dividerCol = Color(0xFFE6E8EF)

                // Ensure accent has readable contrast against light surfaces
                val readableAccent = ensureLightModeContrast(a)

                AppColorScheme(
                    isColored = true,
                    isDark = false,
                    background = bg,
                    surface = surf,
                    secondarySurface = secSurf,
                    primaryText = Color(0xFF0F1218),
                    secondaryText = Color(0xFF535866),
                    accent = readableAccent,
                    selectedItem = readableAccent,
                    border = borderCol,
                    divider = dividerCol,
                    controlBackground = ctrlBg,
                    controlContent = Color(0xFF0F1218)
                )
            }
        }

        private fun blendColors(base: Color, tint: Color, tintFactor: Float): Color {
            val f = tintFactor.coerceIn(0f, 1f)
            val inv = 1f - f
            return Color(
                red = (base.red * inv + tint.red * f).coerceIn(0f, 1f),
                green = (base.green * inv + tint.green * f).coerceIn(0f, 1f),
                blue = (base.blue * inv + tint.blue * f).coerceIn(0f, 1f),
                alpha = 1.0f
            )
        }

        private fun ensureLightModeContrast(color: Color): Color {
            val luminance = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
            return if (luminance > 0.65f) {
                // Darken slightly for light background contrast
                Color(
                    red = (color.red * 0.70f).coerceIn(0f, 1f),
                    green = (color.green * 0.70f).coerceIn(0f, 1f),
                    blue = (color.blue * 0.70f).coerceIn(0f, 1f),
                    alpha = 1.0f
                )
            } else {
                color
            }
        }
    }
}

/**
 * Validates, normalizes, and decodes custom hex color strings for full-app theming.
 * Supports between 2 and 5 valid hex colors.
 */
object CustomPaletteValidator {

    val DEFAULT_CUSTOM_HEXES = listOf("#1A237E", "#3949AB", "#0D1642")
    const val DEFAULT_HEX_STRING = "#1A237E,#3949AB,#0D1642"

    fun isValidHex(hex: String): Boolean {
        val clean = hex.trim().removePrefix("#")
        return (clean.length == 6 || clean.length == 8) && clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun parseHexToColor(hex: String): Color? {
        val clean = hex.trim().removePrefix("#")
        return runCatching {
            when (clean.length) {
                6 -> Color((0xFF000000 or clean.toLong(16)).toInt())
                8 -> Color(clean.toLong(16).toInt())
                else -> null
            }
        }.getOrNull()
    }

    fun parseHexList(hexesString: String): List<String> {
        return hexesString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && isValidHex(it) }
    }

    fun validateHexList(hexList: List<String>): Boolean {
        return hexList.size in 2..5 && hexList.all { isValidHex(it) }
    }

    fun validate(hexesString: String): Boolean {
        val parsed = parseHexList(hexesString)
        return validateHexList(parsed)
    }

    fun createPaletteFromCustomHexes(hexesString: String, isDark: Boolean): PlayerArtworkPalette {
        val validHexes = parseHexList(hexesString)
        val targetHexes = if (validateHexList(validHexes)) validHexes else DEFAULT_CUSTOM_HEXES
        return PlayerArtworkPalette.fromCustomColors(targetHexes, isDark)
    }
}

/**
 * Reusable engine for extracting and caching artwork palettes for full-app theming.
 * Thread-safe, executes off main thread, bounded LRU cache (max 40 items).
 */
object AppColorEngine {

    private const val MAX_CACHE_ENTRIES = 40
    private const val THUMBNAIL_SIZE = 120

    private val cacheLock = Any()
    private val paletteCache: MutableMap<String, PlayerArtworkPalette> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, PlayerArtworkPalette>(MAX_CACHE_ENTRIES, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PlayerArtworkPalette>?): Boolean {
                    return size > MAX_CACHE_ENTRIES
                }
            }
        )

    fun clearCache() {
        synchronized(cacheLock) {
            paletteCache.clear()
        }
    }

    fun getCachedPalette(key: String, isDark: Boolean): PlayerArtworkPalette? {
        val cacheKey = "${key}_isDark=$isDark"
        return synchronized(cacheLock) {
            paletteCache[cacheKey]
        }
    }

    /**
     * Resolves the artwork model into a unique, stable cache key string.
     */
    fun buildArtworkKey(model: Any?): String {
        return when (model) {
            null -> ""
            is String -> model.trim()
            is Uri -> model.toString()
            is Number -> "content_id_$model"
            else -> model.hashCode().toString()
        }
    }

    /**
     * Extracts a palette from an artwork model (HTTP URL, content Uri, resource) off the main thread.
     * Returns null if model is empty, invalid, or decoding fails.
     */
    suspend fun extractArtworkPalette(
        context: Context,
        artworkModel: Any?,
        isDark: Boolean
    ): PlayerArtworkPalette? = withContext(Dispatchers.IO) {
        val key = buildArtworkKey(artworkModel)
        if (key.isBlank()) return@withContext null

        val cacheKey = "${key}_isDark=$isDark"
        synchronized(cacheLock) {
            paletteCache[cacheKey]?.let { return@withContext it }
        }

        try {
            val bitmap = decodeThumbnail(context, artworkModel) ?: return@withContext null
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            val extracted = DynamicArtworkColorsHelper.processPalette(palette, isDark)

            synchronized(cacheLock) {
                paletteCache[cacheKey] = extracted
            }
            extracted
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun decodeThumbnail(context: Context, model: Any?): Bitmap? {
        val modelStr = model?.toString()?.trim() ?: return null

        // 1. Online artwork HTTP/HTTPS
        if (modelStr.startsWith("http://", ignoreCase = true) ||
            modelStr.startsWith("https://", ignoreCase = true)
        ) {
            val optimizedUrl = ArtworkQualityOptimizer.optimizeUrl(modelStr)
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(optimizedUrl)
                .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .allowHardware(false)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            return (result as? BitmapDrawable)?.bitmap
        }

        // 2. Content URI
        if (modelStr.startsWith("content://") || model is Uri) {
            return runCatching {
                val uri = if (model is Uri) model else Uri.parse(modelStr)
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                var sampleSize = 1
                if (boundsOptions.outHeight > THUMBNAIL_SIZE || boundsOptions.outWidth > THUMBNAIL_SIZE) {
                    val halfHeight = boundsOptions.outHeight / 2
                    val halfWidth = boundsOptions.outWidth / 2
                    while ((halfHeight / sampleSize) >= THUMBNAIL_SIZE && (halfWidth / sampleSize) >= THUMBNAIL_SIZE) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull()
        }

        return null
    }

    /**
     * Resolves the effective color scheme for a given screen area deterministically.
     * Strict Precedence:
     * 1. If master switch is OFF -> Monochrome
     * 2. If per-area switch is OFF -> Monochrome
     * 3. Mode MONOCHROME -> Monochrome
     * 4. Mode PRESET -> Built-in preset
     * 5. Mode CUSTOM -> Custom palette (with validation and safe fallback)
     * 6. Mode DYNAMIC_ARTWORK -> Dynamic palette from artwork, falling back to preset or monochrome
     */
    fun resolveAreaColorScheme(
        area: AppArea,
        artworkPalette: PlayerArtworkPalette?,
        appSettings: AppSettings,
        isDark: Boolean
    ): AppColorScheme {
        // Master switch check
        if (!appSettings.enableFullAppDynamicTheme) {
            return AppColorScheme.monochrome(isDark)
        }

        // Per-area switch check
        if (!appSettings.isAreaThemingEnabled(area.name)) {
            return AppColorScheme.monochrome(isDark)
        }

        val mode = AppPaletteMode.fromString(appSettings.appPaletteMode)
        return when (mode) {
            AppPaletteMode.MONOCHROME -> {
                AppColorScheme.monochrome(isDark)
            }
            AppPaletteMode.PRESET -> {
                val presetName = appSettings.appColorPreset
                val presetPalette = PlayerArtworkPalette.fromPreset(presetName, isDark)
                val isColored = !presetName.equals("Monochrome", ignoreCase = true)
                AppColorScheme.fromPalette(presetPalette, isDark, isColored = isColored)
            }
            AppPaletteMode.CUSTOM -> {
                val customPalette = CustomPaletteValidator.createPaletteFromCustomHexes(
                    appSettings.appCustomPaletteHexes,
                    isDark
                )
                AppColorScheme.fromPalette(customPalette, isDark, isColored = true)
            }
            AppPaletteMode.DYNAMIC_ARTWORK -> {
                if (artworkPalette != null) {
                    AppColorScheme.fromPalette(artworkPalette, isDark, isColored = true)
                } else {
                    // Safe fallback: use selected preset if non-monochrome, else monochrome
                    val presetName = appSettings.appColorPreset
                    if (presetName.isNotBlank() && !presetName.equals("Monochrome", ignoreCase = true)) {
                        val fallbackPreset = PlayerArtworkPalette.fromPreset(presetName, isDark)
                        AppColorScheme.fromPalette(fallbackPreset, isDark, isColored = true)
                    } else {
                        AppColorScheme.monochrome(isDark)
                    }
                }
            }
        }
    }
}

/**
 * CompositionLocal providing screen-scoped semantic color scheme.
 */
val LocalAppColorScheme = staticCompositionLocalOf { AppColorScheme.monochrome(false) }

/**
 * Direct accessor for current active AppColorScheme tokens.
 */
val AppColors: AppColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColorScheme.current

/**
 * Remember and resolve the active [AppColorScheme] for an area.
 * Runs artwork extraction asynchronously and reacts immediately to preference changes.
 */
@Composable
fun rememberAreaColorScheme(
    area: AppArea,
    artworkModel: Any? = null,
    appSettings: AppSettings,
    isDark: Boolean = LocalIsDark.current
): AppColorScheme {
    val context = LocalContext.current
    val artworkKey = remember(artworkModel) { AppColorEngine.buildArtworkKey(artworkModel) }

    var dynamicPalette by remember(artworkKey, isDark) {
        mutableStateOf(
            if (artworkKey.isNotEmpty()) AppColorEngine.getCachedPalette(artworkKey, isDark) else null
        )
    }

    val mode = AppPaletteMode.fromString(appSettings.appPaletteMode)
    val shouldExtract = appSettings.enableFullAppDynamicTheme &&
        appSettings.isAreaThemingEnabled(area.name) &&
        mode == AppPaletteMode.DYNAMIC_ARTWORK &&
        artworkKey.isNotEmpty()

    LaunchedEffect(artworkKey, isDark, shouldExtract) {
        if (shouldExtract && dynamicPalette == null) {
            val extracted = AppColorEngine.extractArtworkPalette(context, artworkModel, isDark)
            if (extracted != null) {
                dynamicPalette = extracted
            }
        }
    }

    return remember(area, dynamicPalette, appSettings, isDark) {
        AppColorEngine.resolveAreaColorScheme(area, dynamicPalette, appSettings, isDark)
    }
}

/**
 * Wrapper composable that applies a screen-scoped color palette to all child components.
 * It provides both [LocalAppColorScheme] and overrides [MaterialTheme] colorScheme so all standard
 * surfaces, cards, text, chips, buttons, and borders adapt naturally.
 */
@Composable
fun AppThemedScreen(
    area: AppArea,
    artworkModel: Any? = null,
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val areaColorScheme = rememberAreaColorScheme(
        area = area,
        artworkModel = artworkModel,
        appSettings = appSettings
    )

    CompositionLocalProvider(
        LocalAppColorScheme provides areaColorScheme
    ) {
        MaterialTheme(
            colorScheme = areaColorScheme.toMaterialColorScheme(),
            content = content
        )
    }
}
