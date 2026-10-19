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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.ui.theme.CustomPaletteValidator
import com.aman.auramusic.ui.theme.DynamicArtworkColorsHelper
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import kotlin.math.abs

/**
 * Vibrant Liquid Glass accent color inspired by AniWhere / Apple Music navigation.
 */
private val LiquidGlassAccent = Color(0xFFFA2D48)

enum class BottomNavStyle {
    DYNAMIC,
    NORMAL
}

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
 * Bottom Navigation Bar featuring the AniWhere-inspired Liquid Glass capsule navigation.
 *
 * Supports:
 * - Dynamic Style (dynamicStyle = true): Adapts the glowing pill, border accents, and active indicators
 *   dynamically using the theme and playing song's artwork coloring system configured in Settings.
 * - Normal Style (dynamicStyle = false): Uses the clean signature Liquid Glass styling with Aura's classic
 *   accent color without track/theme dynamic recoloring.
 *
 * In both modes, the modern floating liquid-glass capsule with traveling spring pill, dragging,
 * glow, and responsive haptics is used (the old navigation bar is retired).
 */
@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    compact: Boolean = false,
    compactPlayer: (@Composable () -> Unit)? = null,
    isPlaying: Boolean = false,
    dynamicStyle: Boolean = true,
    song: Song? = null,
    appSettings: AppSettings? = null
) {
    val allTabs = remember { listOf(AppTab.Home, AppTab.Online, AppTab.Library, AppTab.Search) }
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val defaultAccent = LiquidGlassAccent

    var dynamicArtworkPalette by remember(song?.id, song?.artworkUri, song?.uri, isDark) {
        mutableStateOf<PlayerArtworkPalette?>(null)
    }

    val colorStyle = appSettings?.let {
        if (it.changeThemeEnabled) it.themeColorStyle.ifBlank { it.appPaletteMode } else "Dynamic Artwork"
    } ?: "Dynamic Artwork"

    LaunchedEffect(song?.id, song?.artworkUri, song?.uri, isDark, colorStyle, dynamicStyle) {
        if (dynamicStyle && colorStyle.equals("Dynamic Artwork", ignoreCase = true) && song != null) {
            val palette = DynamicArtworkColorsHelper.extractPalette(context, song, isDark)
            dynamicArtworkPalette = palette
        }
    }

    val resolvedAccent: Color = if (!dynamicStyle) {
        defaultAccent
    } else {
        when {
            colorStyle.equals("Preset", ignoreCase = true) -> {
                val presetName = appSettings?.appColorPreset?.ifBlank { "Midnight Indigo" } ?: "Midnight Indigo"
                PlayerArtworkPalette.fromPreset(presetName, isDark).accent
            }
            colorStyle.equals("Custom", ignoreCase = true) -> {
                val hexes = appSettings?.appCustomPaletteHexes ?: "#1A237E,#3949AB,#0D1642"
                CustomPaletteValidator.createPaletteFromCustomHexes(hexes, isDark).accent
            }
            else -> {
                dynamicArtworkPalette?.accent ?: defaultAccent
            }
        }
    }

    val animatedAccent by animateColorAsState(
        targetValue = resolvedAccent,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "bottomNavAccentColor"
    )

    if (compact) {
        CompactDockNavBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            compactPlayer = compactPlayer,
            isDark = isDark,
            isPlaying = isPlaying,
            accentColor = animatedAccent
        )
    } else {
        ExpandedLiquidGlassNavBar(
            tabs = allTabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            isDark = isDark,
            isPlaying = isPlaying,
            accentColor = animatedAccent
        )
    }
}

// ==========================================================================================
// DYNAMIC LIQUID GLASS IMPLEMENTATION
// ==========================================================================================

@Composable
private fun ExpandedLiquidGlassNavBar(
    tabs: List<AppTab>,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    isDark: Boolean,
    isPlaying: Boolean,
    accentColor: Color = LiquidGlassAccent
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val tabOffsetsPx = remember { mutableStateMapOf<AppTab, Float>() }
    val tabWidthsPx = remember { mutableStateMapOf<AppTab, Float>() }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var dragXPx by remember { mutableFloatStateOf(0f) }
    var hoverTab by remember { mutableStateOf(selectedTab) }

    val activeTab = if (isDragging) hoverTab else selectedTab

    val navBarHeight = 62.dp
    val navBarCornerRadius = 31.dp
    val navShape = RoundedCornerShape(navBarCornerRadius)

    val surfaceGlassColor = if (isDark) Color(0xDC101018) else Color(0xEBFCFCFD)

    val surfaceBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f))
        } else {
            listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.08f))
        }
    )

    val ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.14f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarHeight)
                .shadow(
                    elevation = if (isDark) 16.dp else 10.dp,
                    shape = navShape,
                    ambientColor = ambientShadowColor,
                    spotColor = ambientShadowColor
                )
                .clip(navShape)
                .background(surfaceGlassColor, navShape)
                .border(BorderStroke(1.dp, surfaceBorderBrush), navShape)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .onGloballyPositioned { coords ->
                    parentCoordinates = coords
                    containerWidthPx = coords.size.width.toFloat()
                }
                .pointerInput(tabs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragXPx = offset.x.coerceIn(0f, containerWidthPx)
                            val tabWidth = (containerWidthPx / tabs.size).coerceAtLeast(1f)
                            val targetIndex = (dragXPx / tabWidth).toInt().coerceIn(0, tabs.size - 1)
                            hoverTab = tabs[targetIndex]
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragXPx = (dragXPx + dragAmount).coerceIn(0f, containerWidthPx)
                            val tabWidth = (containerWidthPx / tabs.size).coerceAtLeast(1f)
                            val newIndex = (dragXPx / tabWidth).toInt().coerceIn(0, tabs.size - 1)
                            val newHover = tabs[newIndex]
                            if (newHover != hoverTab) {
                                hoverTab = newHover
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onTabSelected(hoverTab)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragCancel = {
                            isDragging = false
                            hoverTab = selectedTab
                        }
                    )
                }
        ) {
            val totalWidthDp = maxWidth
            val defaultTabWidthDp = totalWidthDp / tabs.size
            val activeIndex = tabs.indexOf(activeTab).coerceAtLeast(0)

            val measuredTabOffsetDp = tabOffsetsPx[activeTab]?.let { with(density) { it.toDp() } }
                ?: (defaultTabWidthDp * activeIndex)
            val measuredTabWidthDp = tabWidthsPx[activeTab]?.let { with(density) { it.toDp() } }
                ?: defaultTabWidthDp

            val basePillWidth = (measuredTabWidthDp - 6.dp).coerceIn(54.dp, 82.dp)
            val restingPillHeight = 46.dp
            val targetPillWidth = if (isDragging) (basePillWidth + 12.dp) else basePillWidth

            val targetPillLeft = if (isDragging) {
                val dragCenterDp = with(density) { dragXPx.toDp() }
                (dragCenterDp - targetPillWidth / 2).coerceIn(0.dp, totalWidthDp - targetPillWidth)
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

            val displayPillLeft = (animatedPillLeft - stretchAmountDp / 2).coerceAtLeast(0.dp)

            // Diffused Glow Behind Pill
            val glowColor = accentColor.copy(alpha = if (isDark) 0.32f else 0.22f)
            Box(
                modifier = Modifier
                    .offset(x = displayPillLeft, y = 4.dp)
                    .size(width = animatedPillWidth, height = restingPillHeight)
                    .drawBehind {
                        val blurRadius = 14.dp.toPx()
                        val paint = Paint().apply {
                            isAntiAlias = true
                            color = glowColor.toArgb()
                            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                        }
                        try {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    -3.dp.toPx(),
                                    -2.dp.toPx(),
                                    size.width + 3.dp.toPx(),
                                    size.height + 2.dp.toPx(),
                                    size.height / 2,
                                    size.height / 2,
                                    paint
                                )
                            }
                        } catch (_: Throwable) {
                            drawRoundRect(
                                color = glowColor,
                                topLeft = Offset(-4.dp.toPx(), -2.dp.toPx()),
                                size = Size(size.width + 8.dp.toPx(), size.height + 4.dp.toPx()),
                                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                            )
                        }
                    }
            )

            // Animated Traveling Liquid Glass Capsule
            val pillShape = RoundedCornerShape(restingPillHeight / 2)
            val pillBackgroundBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        accentColor.copy(alpha = if (isDragging) 0.34f else 0.26f),
                        accentColor.copy(alpha = if (isDragging) 0.18f else 0.12f)
                    )
                } else {
                    listOf(
                        accentColor.copy(alpha = if (isDragging) 0.25f else 0.18f),
                        accentColor.copy(alpha = if (isDragging) 0.12f else 0.07f)
                    )
                }
            )

            val pillBorderBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = if (isDragging) 0.70f else 0.55f),
                        accentColor.copy(alpha = if (isDragging) 0.50f else 0.35f)
                    )
                } else {
                    listOf(
                        accentColor.copy(alpha = if (isDragging) 0.80f else 0.65f),
                        accentColor.copy(alpha = if (isDragging) 0.40f else 0.25f)
                    )
                }
            )

            Box(
                modifier = Modifier
                    .offset(x = displayPillLeft, y = 4.dp)
                    .size(width = animatedPillWidth, height = restingPillHeight)
                    .clip(pillShape)
                    .background(pillBackgroundBrush, pillShape)
                    .border(BorderStroke(1.1.dp, pillBorderBrush), pillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.16f else 0.32f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Foreground Navigation Tabs Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.14f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tabIconScale_${tab.name}"
                    )

                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "tabPressScale_${tab.name}"
                    )

                    val inactiveTint = if (isDark) {
                        Color.White.copy(alpha = 0.52f)
                    } else {
                        Color.Black.copy(alpha = 0.48f)
                    }

                    val tabTint by animateColorAsState(
                        targetValue = if (isSelected) accentColor else inactiveTint,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "tabTint_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = interactionSource,
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
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = tabTint,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale * pressScale
                                            scaleY = iconScale * pressScale
                                        }
                                )

                                if (isPlaying && isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-2).dp)
                                            .size(5.dp)
                                            .background(accentColor, CircleShape)
                                            .drawBehind {
                                                drawCircle(
                                                    color = accentColor.copy(alpha = 0.8f),
                                                    radius = size.minDimension * 1.5f
                                                )
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.5.dp))

                            Text(
                                text = tab.label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabTint,
                                maxLines = 1,
                                letterSpacing = (-0.15).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDockNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    compactPlayer: (@Composable () -> Unit)?,
    isDark: Boolean,
    isPlaying: Boolean,
    accentColor: Color = LiquidGlassAccent
) {
    val haptic = LocalHapticFeedback.current
    val navShape = RoundedCornerShape(27.dp)

    val surfaceGlassColor = if (isDark) Color(0xDC101018) else Color(0xEBFCFCFD)
    val surfaceBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f))
        } else {
            listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.08f))
        }
    )
    val ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.14f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 6.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isHomeSelected = selectedTab == AppTab.Home
        CompactGlassButton(
            icon = if (isHomeSelected) AppTab.Home.selectedIcon else AppTab.Home.unselectedIcon,
            label = AppTab.Home.label,
            isSelected = isHomeSelected,
            isDark = isDark,
            surfaceGlassColor = surfaceGlassColor,
            surfaceBorderBrush = surfaceBorderBrush,
            ambientShadowColor = ambientShadowColor,
            navShape = navShape,
            isPlaying = isPlaying,
            accentColor = accentColor,
            onTap = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(AppTab.Home)
            }
        )

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
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(AppTab.Home)
                    },
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

        val isSearchSelected = selectedTab == AppTab.Search
        CompactGlassButton(
            icon = if (isSearchSelected) AppTab.Search.selectedIcon else AppTab.Search.unselectedIcon,
            label = null,
            isSelected = isSearchSelected,
            isDark = isDark,
            surfaceGlassColor = surfaceGlassColor,
            surfaceBorderBrush = surfaceBorderBrush,
            ambientShadowColor = ambientShadowColor,
            navShape = navShape,
            isPlaying = false,
            accentColor = accentColor,
            onTap = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(AppTab.Search)
            }
        )
    }
}

@Composable
private fun CompactGlassButton(
    icon: ImageVector,
    label: String?,
    isSelected: Boolean,
    isDark: Boolean,
    surfaceGlassColor: Color,
    surfaceBorderBrush: Brush,
    ambientShadowColor: Color,
    navShape: RoundedCornerShape,
    isPlaying: Boolean,
    accentColor: Color = LiquidGlassAccent,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (isSelected) accentColor else if (isDark) Color.White.copy(alpha = 0.52f) else Color.Black.copy(alpha = 0.48f),
        animationSpec = tween(200),
        label = "compactTint"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "compactScale"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "compactPressScale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "compactPillAlpha"
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
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        if (pillAlpha > 0.01f) {
            val glowColor = accentColor.copy(alpha = if (isDark) 0.28f else 0.18f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .drawBehind {
                        val blurRadius = 10.dp.toPx()
                        val paint = Paint().apply {
                            isAntiAlias = true
                            color = glowColor.toArgb()
                            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                        }
                        try {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    0f,
                                    0f,
                                    size.width,
                                    size.height,
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
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        accentColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                        RoundedCornerShape(23.dp)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.50f) else accentColor.copy(alpha = 0.60f)
                        ),
                        RoundedCornerShape(23.dp)
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label ?: "Navigation Icon",
                    tint = tint,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = scale * pressScale
                            scaleY = scale * pressScale
                        }
                )

                if (isPlaying && isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-2).dp)
                            .size(5.dp)
                            .background(accentColor, CircleShape)
                    )
                }
            }

            if (label != null) {
                Spacer(modifier = Modifier.height(1.5.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}

