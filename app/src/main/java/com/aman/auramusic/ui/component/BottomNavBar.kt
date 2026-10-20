package com.aman.auramusic.ui.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.CustomPaletteValidator
import com.aman.auramusic.ui.theme.DarkMonochromeColors
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.LightMonochromeColors
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import kotlin.math.abs

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Online("Explore", Icons.Filled.Sensors, Icons.Outlined.Sensors),
    Library("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    Search("Search", Icons.Filled.Search, Icons.Outlined.Search)
}

/**
 * AuraMusic Dynamic Glass Navigation Bar.
 *
 * Structure:
 * - Home, Explore, and Library remain inside the floating glass capsule.
 * - Search remains a separate circular button outside the capsule.
 * - In compact mode: circular button for Home, compact miniplayer in the middle, and circular button for Search.
 *
 * Color Behavior:
 * - Dynamic Colour OFF: strictly follows the monochrome palette (white/black/shades of grey),
 *   with translucent glass surfaces, glass gradient borders, and crisp monochrome tab indicators.
 * - Dynamic Colour ON: preserves dynamic artwork-, preset-, or custom-derived accents across the
 *   active indicators and icons while retaining the glass aesthetic.
 */
@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    compact: Boolean = false,
    compactPlayer: (@Composable () -> Unit)? = null,
    isPlaying: Boolean = false,
    song: Song? = null,
    appSettings: AppSettings? = null
) {
    // Follow AuraMusic's selected theme, not the device's independent system theme.
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isDynamicActive = appSettings?.isFullAppThemeActive() == true

    var dynamicArtworkPalette by remember(song?.id, song?.artworkUri, song?.uri, isDark) {
        mutableStateOf<PlayerArtworkPalette?>(null)
    }

    val colorStyle = appSettings?.let {
        if (it.changeThemeEnabled) it.themeColorStyle.ifBlank { it.appPaletteMode } else "Dynamic Artwork"
    } ?: "Dynamic Artwork"

    LaunchedEffect(song?.id, song?.artworkUri, song?.uri, isDark, colorStyle, isDynamicActive) {
        if (isDynamicActive && colorStyle.equals("Dynamic Artwork", ignoreCase = true) && song != null) {
            val palette = DynamicArtworkColorsHelper.extractPalette(context, song, isDark)
            dynamicArtworkPalette = palette
        }
    }

    val monochromeAccent = if (isDark) DarkMonochromeColors.controlSelected else LightMonochromeColors.controlSelected

    val resolvedAccent: Color = if (!isDynamicActive) {
        monochromeAccent
    } else {
        when {
            colorStyle.equals("Preset", ignoreCase = true) -> {
                val presetName = appSettings?.appColorPreset?.ifBlank { "Midnight Indigo" } ?: "Midnight Indigo"
                PlayerArtworkPalette.fromPreset(presetName, isDark).accent
            }
            colorStyle.equals("Custom", ignoreCase = true) || colorStyle.equals("Custom Palette", ignoreCase = true) -> {
                val hexes = appSettings?.appCustomPaletteHexes ?: "#1A237E,#3949AB,#0D1642"
                CustomPaletteValidator.createPaletteFromCustomHexes(hexes, isDark).accent
            }
            else -> {
                dynamicArtworkPalette?.accent ?: monochromeAccent
            }
        }
    }

    val animatedAccent by animateColorAsState(
        targetValue = resolvedAccent,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bottomNavAccentColor"
    )

    val navBarCornerRadius = 27.dp
    val navShape = RoundedCornerShape(navBarCornerRadius)

    val surfaceGlassColor = if (isDark) Color(0xDC111118) else Color(0xEBFCFCFD)

    val surfaceBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
        } else {
            listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.08f))
        }
    )

    val ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.12f)
    val unselectedTint = if (isDark) Color.White.copy(alpha = 0.58f) else Color.Black.copy(alpha = 0.52f)
    // Keep every navigation destination artwork-themed, even while it is not selected.
    // Use a softer accent for inactive items so the selected destination remains obvious.
    val unselectedNavTint = if (isDynamicActive) {
        animatedAccent.copy(alpha = if (isDark) 0.78f else 0.72f)
    } else {
        unselectedTint
    }

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp, top = 2.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home Button (54.dp circular glass button)
            val isHomeSelected = selectedTab == AppTab.Home
            val homeTint by animateColorAsState(
                targetValue = if (isHomeSelected) animatedAccent else unselectedNavTint,
                animationSpec = tween(220),
                label = "compactHomeTint"
            )
            val homeScale by animateFloatAsState(
                targetValue = if (isHomeSelected) 1.12f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "compactHomeScale"
            )
            val homePillAlpha by animateFloatAsState(
                targetValue = if (isHomeSelected) 1f else 0f,
                animationSpec = tween(220),
                label = "compactHomePillAlpha"
            )
            val homePillScale by animateFloatAsState(
                targetValue = if (isHomeSelected) 1f else 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "compactHomePillScale"
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(
                        elevation = if (isDark) 12.dp else 8.dp,
                        shape = navShape,
                        ambientColor = ambientShadowColor,
                        spotColor = ambientShadowColor
                    )
                    .clip(navShape)
                    .background(surfaceGlassColor, navShape)
                    .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(AppTab.Home)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (homePillAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .graphicsLayer {
                                scaleX = homePillScale
                                scaleY = homePillScale
                                alpha = homePillAlpha
                            }
                            .background(
                                animatedAccent.copy(alpha = if (isDark) 0.16f else 0.12f),
                                RoundedCornerShape(23.dp)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    animatedAccent.copy(alpha = if (isDark) 0.30f else 0.20f)
                                ),
                                RoundedCornerShape(23.dp)
                            )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isHomeSelected) AppTab.Home.selectedIcon else AppTab.Home.unselectedIcon,
                        contentDescription = AppTab.Home.label,
                        tint = homeTint,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = homeScale
                                scaleY = homeScale
                            }
                    )
                    Text(
                        text = AppTab.Home.label,
                        fontSize = 9.sp,
                        fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Medium,
                        color = homeTint,
                        maxLines = 1
                    )
                }
            }

            // 2. Center MiniPlayer (weight 1f)
            if (compactPlayer != null) {
                Box(modifier = Modifier.weight(1f)) {
                    compactPlayer()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .shadow(
                            elevation = if (isDark) 12.dp else 8.dp,
                            shape = navShape,
                            ambientColor = ambientShadowColor,
                            spotColor = ambientShadowColor
                        )
                        .clip(navShape)
                        .background(surfaceGlassColor, navShape)
                        .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                        .clickable { onTabSelected(AppTab.Home) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "AuraMusic",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Search Button (54.dp circular glass button)
            val isSearchSelected = selectedTab == AppTab.Search
            val searchTint by animateColorAsState(
                targetValue = if (isSearchSelected) animatedAccent else unselectedNavTint,
                animationSpec = tween(220),
                label = "compactSearchTint"
            )
            val searchScale by animateFloatAsState(
                targetValue = if (isSearchSelected) 1.14f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "compactSearchScale"
            )
            val searchPillAlpha by animateFloatAsState(
                targetValue = if (isSearchSelected) 1f else 0f,
                animationSpec = tween(220),
                label = "compactSearchPillAlpha"
            )
            val searchPillScale by animateFloatAsState(
                targetValue = if (isSearchSelected) 1f else 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "compactSearchPillScale"
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(
                        elevation = if (isDark) 12.dp else 8.dp,
                        shape = navShape,
                        ambientColor = ambientShadowColor,
                        spotColor = ambientShadowColor
                    )
                    .clip(navShape)
                    .background(surfaceGlassColor, navShape)
                    .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(AppTab.Search)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (searchPillAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .graphicsLayer {
                                scaleX = searchPillScale
                                scaleY = searchPillScale
                                alpha = searchPillAlpha
                            }
                            .background(
                                animatedAccent.copy(alpha = if (isDark) 0.16f else 0.12f),
                                RoundedCornerShape(23.dp)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    animatedAccent.copy(alpha = if (isDark) 0.30f else 0.20f)
                                ),
                                RoundedCornerShape(23.dp)
                            )
                    )
                }

                Icon(
                    imageVector = if (isSearchSelected) AppTab.Search.selectedIcon else AppTab.Search.unselectedIcon,
                    contentDescription = "Search",
                    tint = searchTint,
                    modifier = Modifier
                        .size(21.dp)
                        .graphicsLayer {
                            scaleX = searchScale
                            scaleY = searchScale
                        }
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp, top = 2.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Glass Capsule containing Home, Explore, Library (weight 1f, height 54dp)
            val mainTabs = remember { listOf(AppTab.Home, AppTab.Online, AppTab.Library) }
            val density = LocalDensity.current

            var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            val tabOffsetsPx = remember { mutableStateMapOf<AppTab, Float>() }
            val tabWidthsPx = remember { mutableStateMapOf<AppTab, Float>() }
            var containerWidthPx by remember { mutableFloatStateOf(0f) }

            var isDragging by remember { mutableStateOf(false) }
            var dragXPx by remember { mutableFloatStateOf(0f) }
            var hoverTab by remember { mutableStateOf(if (selectedTab in mainTabs) selectedTab else AppTab.Home) }
            var lastSelectedMainTab by remember { mutableStateOf(if (selectedTab in mainTabs) selectedTab else AppTab.Home) }

            LaunchedEffect(selectedTab) {
                if (selectedTab in mainTabs) {
                    lastSelectedMainTab = selectedTab
                    if (!isDragging) {
                        hoverTab = selectedTab
                    }
                }
            }

            val currentSelectedTab by rememberUpdatedState(selectedTab)
            val currentOnTabSelected by rememberUpdatedState(onTabSelected)

            fun findTabForX(xPx: Float): AppTab {
                for (tab in mainTabs) {
                    val start = tabOffsetsPx[tab]
                    val width = tabWidthsPx[tab]
                    if (start != null && width != null && width > 0f) {
                        if (xPx >= start && xPx < (start + width)) {
                            return tab
                        }
                    }
                }
                val fallbackTabWidth = (containerWidthPx / mainTabs.size).coerceAtLeast(1f)
                val index = (xPx / fallbackTabWidth).toInt().coerceIn(0, mainTabs.size - 1)
                return mainTabs[index]
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .shadow(
                        elevation = if (isDark) 12.dp else 8.dp,
                        shape = navShape,
                        ambientColor = ambientShadowColor,
                        spotColor = ambientShadowColor
                    )
                    .clip(navShape)
                    .background(surfaceGlassColor, navShape)
                    .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                    .padding(4.dp)
                    .onGloballyPositioned { coords ->
                        parentCoordinates = coords
                        containerWidthPx = coords.size.width.toFloat()
                    }
                    .pointerInput(mainTabs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragXPx = offset.x.coerceIn(0f, containerWidthPx)
                                hoverTab = findTabForX(dragXPx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragXPx = (dragXPx + dragAmount).coerceIn(0f, containerWidthPx)
                                val newHover = findTabForX(dragXPx)
                                if (newHover != hoverTab) {
                                    hoverTab = newHover
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                if (hoverTab != currentSelectedTab) {
                                    currentOnTabSelected(hoverTab)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                hoverTab = if (currentSelectedTab in mainTabs) currentSelectedTab else lastSelectedMainTab
                            }
                        )
                    }
            ) {
                val totalWidthDp = maxWidth
                val defaultTabWidthDp = totalWidthDp / mainTabs.size
                val activeTab = if (isDragging) hoverTab else (if (selectedTab in mainTabs) selectedTab else lastSelectedMainTab)
                val isMainTabActive = isDragging || (selectedTab in mainTabs)
                val activeIndex = mainTabs.indexOf(activeTab).let { if (it >= 0) it else 0 }

                val measuredTabOffsetDp = tabOffsetsPx[activeTab]?.let { with(density) { it.toDp() } }
                    ?: (defaultTabWidthDp * activeIndex)
                val measuredTabWidthDp = tabWidthsPx[activeTab]?.let { with(density) { it.toDp() } }
                    ?: defaultTabWidthDp

                val basePillWidth = (measuredTabWidthDp - 4.dp).coerceAtLeast(36.dp)
                val restingPillHeight = 42.dp
                val restingPillY = 2.dp
                val targetPillWidth = if (isDragging) (basePillWidth + 10.dp) else basePillWidth

                val targetPillLeft = if (isDragging) {
                    val dragCenterDp = with(density) { dragXPx.toDp() }
                    (dragCenterDp - targetPillWidth / 2).coerceIn(0.dp, (totalWidthDp - targetPillWidth).coerceAtLeast(0.dp))
                } else {
                    measuredTabOffsetDp + (measuredTabWidthDp - targetPillWidth) / 2
                }

                val animatedPillLeft by animateDpAsState(
                    targetValue = targetPillLeft,
                    animationSpec = if (isDragging) {
                        tween(durationMillis = 20, easing = LinearEasing)
                    } else {
                        spring(
                            dampingRatio = 0.74f,
                            stiffness = 380f
                        )
                    },
                    label = "liquidPillLeft"
                )

                val travelDistanceDp = abs((targetPillLeft - animatedPillLeft).value)
                val stretchAmountDp = if (!isDragging) {
                    (travelDistanceDp * 0.14f).coerceAtMost(14f).dp
                } else {
                    0.dp
                }

                val animatedPillWidth by animateDpAsState(
                    targetValue = targetPillWidth + stretchAmountDp,
                    animationSpec = if (isDragging) {
                        tween(durationMillis = 35)
                    } else {
                        spring(
                            dampingRatio = 0.80f,
                            stiffness = 360f
                        )
                    },
                    label = "liquidPillWidth"
                )

                val maxAllowedLeft = (totalWidthDp - animatedPillWidth).coerceAtLeast(0.dp)
                val clampedDisplayLeft = (animatedPillLeft - stretchAmountDp / 2).coerceIn(0.dp, maxAllowedLeft)

                val targetPillAlpha = if (isMainTabActive) 1f else 0f
                val animatedPillAlpha by animateFloatAsState(
                    targetValue = targetPillAlpha,
                    animationSpec = tween(durationMillis = 220),
                    label = "liquidPillAlpha"
                )
                val targetPillScale = if (isMainTabActive) 1f else 0.88f
                val animatedPillScale by animateFloatAsState(
                    targetValue = targetPillScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "liquidPillScale"
                )

                val pillShape = RoundedCornerShape(restingPillHeight / 2)

                if (animatedPillAlpha > 0.001f) {
                    // Diffused Glow Behind Pill
                    val glowColor = animatedAccent.copy(alpha = if (isDark) 0.28f else 0.18f)
                    Box(
                        modifier = Modifier
                            .offset(x = clampedDisplayLeft, y = restingPillY)
                            .size(width = animatedPillWidth, height = restingPillHeight)
                            .graphicsLayer {
                                scaleX = animatedPillScale
                                scaleY = animatedPillScale
                                alpha = animatedPillAlpha
                            }
                            .drawBehind {
                                val blurRadius = 12.dp.toPx()
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    color = glowColor.toArgb()
                                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                                }
                                try {
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawRoundRect(
                                            -2.dp.toPx(),
                                            -1.dp.toPx(),
                                            size.width + 2.dp.toPx(),
                                            size.height + 1.dp.toPx(),
                                            size.height / 2,
                                            size.height / 2,
                                            paint
                                        )
                                    }
                                } catch (_: Throwable) {
                                    drawRoundRect(
                                        color = glowColor,
                                        cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                                    )
                                }
                            }
                    )

                    // Animated Traveling Liquid Glass Pill
                    val pillBackgroundBrush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                animatedAccent.copy(alpha = if (isDragging) 0.26f else 0.18f),
                                animatedAccent.copy(alpha = if (isDragging) 0.14f else 0.08f)
                            )
                        } else {
                            listOf(
                                animatedAccent.copy(alpha = if (isDragging) 0.20f else 0.14f),
                                animatedAccent.copy(alpha = if (isDragging) 0.08f else 0.04f)
                            )
                        }
                    )

                    val pillBorderBrush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = if (isDragging) 0.60f else 0.45f),
                                animatedAccent.copy(alpha = if (isDragging) 0.40f else 0.25f)
                            )
                        } else {
                            listOf(
                                animatedAccent.copy(alpha = if (isDragging) 0.65f else 0.50f),
                                animatedAccent.copy(alpha = if (isDragging) 0.30f else 0.15f)
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = clampedDisplayLeft, y = restingPillY)
                            .size(width = animatedPillWidth, height = restingPillHeight)
                            .graphicsLayer {
                                scaleX = animatedPillScale
                                scaleY = animatedPillScale
                                alpha = animatedPillAlpha
                            }
                            .clip(pillShape)
                            .background(pillBackgroundBrush, pillShape)
                            .border(BorderStroke(1.dp, pillBorderBrush), pillShape)
                    ) {
                        // Subtle specular liquid highlight on top half of pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(restingPillHeight * 0.42f)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (isDark) 0.14f else 0.28f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Foreground Navigation Tabs Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mainTabs.forEach { tab ->
                        val selected = activeTab == tab && isMainTabActive
                        val tabTint by animateColorAsState(
                            targetValue = if (selected) animatedAccent else unselectedNavTint,
                            animationSpec = tween(220),
                            label = "tabTint_${tab.name}"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tabScale_${tab.name}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(pillShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                                .onGloballyPositioned { coords ->
                                    parentCoordinates?.let { parent ->
                                        if (parent.isAttached && coords.isAttached) {
                                            val local = parent.localPositionOf(coords, Offset.Zero)
                                            tabOffsetsPx[tab] = local.x
                                            tabWidthsPx[tab] = coords.size.width.toFloat()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = tabTint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = tabTint,
                                    maxLines = 1,
                                    letterSpacing = (-0.1).sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Separate Circular Glass Button for Search (54.dp)
            val isSearchSelected = selectedTab == AppTab.Search
            val searchScale by animateFloatAsState(
                targetValue = if (isSearchSelected) 1.14f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "searchScale"
            )
            val searchTint by animateColorAsState(
                targetValue = if (isSearchSelected) animatedAccent else unselectedNavTint,
                animationSpec = tween(220),
                label = "searchTint"
            )
            val searchPillAlpha by animateFloatAsState(
                targetValue = if (isSearchSelected) 1f else 0f,
                animationSpec = tween(220),
                label = "searchPillAlpha"
            )
            val searchPillScale by animateFloatAsState(
                targetValue = if (isSearchSelected) 1f else 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "searchPillScale"
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(
                        elevation = if (isDark) 12.dp else 8.dp,
                        shape = navShape,
                        ambientColor = ambientShadowColor,
                        spotColor = ambientShadowColor
                    )
                    .clip(navShape)
                    .background(surfaceGlassColor, navShape)
                    .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(AppTab.Search)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (searchPillAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .graphicsLayer {
                                scaleX = searchPillScale
                                scaleY = searchPillScale
                                alpha = searchPillAlpha
                            }
                            .background(
                                animatedAccent.copy(alpha = if (isDark) 0.16f else 0.12f),
                                RoundedCornerShape(23.dp)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    animatedAccent.copy(alpha = if (isDark) 0.30f else 0.20f)
                                ),
                                RoundedCornerShape(23.dp)
                            )
                    )
                }

                Icon(
                    imageVector = if (isSearchSelected) AppTab.Search.selectedIcon else AppTab.Search.unselectedIcon,
                    contentDescription = "Search",
                    tint = searchTint,
                    modifier = Modifier
                        .size(21.dp)
                        .graphicsLayer {
                            scaleX = searchScale
                            scaleY = searchScale
                        }
                )
            }
        }
    }
}
