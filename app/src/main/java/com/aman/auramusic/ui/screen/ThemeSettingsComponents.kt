package com.aman.auramusic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import com.aman.auramusic.BuildConfig
import com.aman.auramusic.R
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.Monochrome
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.AppPaletteMode
import com.aman.auramusic.ui.theme.CustomPaletteValidator
import com.aman.auramusic.ui.theme.PlayerThemeMode
import com.aman.auramusic.ui.theme.PlayerArtworkPalette
import com.aman.auramusic.ui.theme.resolvePlayerColorScheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.sp

@Composable
fun ThemeSelectionRow(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "App Theme",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                ThemeMode.SYSTEM to ("System" to Icons.Default.BrightnessAuto),
                ThemeMode.LIGHT to ("Light" to Icons.Default.LightMode),
                ThemeMode.DARK to ("Dark" to Icons.Default.DarkMode)
            )
            val colors = Monochrome
            themes.forEach { (mode, pair) ->
                val (label, icon) = pair
                val isSelected = selectedTheme == mode
                val pillShape = RoundedCornerShape(16.dp)
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val targetBackgroundColor = when {
                    isSelected -> colors.controlSelected
                    isPressed -> colors.surfacePressed
                    else -> colors.surfaceSecondary
                }
                val targetContentColor = if (isSelected) colors.controlSelectedContent else colors.textSecondary
                val targetBorderColor = if (isSelected) colors.controlSelected else colors.border

                val backgroundColor by animateColorAsState(
                    targetValue = targetBackgroundColor,
                    animationSpec = tween(durationMillis = 150),
                    label = "themePillBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = tween(durationMillis = 150),
                    label = "themePillContent"
                )
                val borderColor by animateColorAsState(
                    targetValue = targetBorderColor,
                    animationSpec = tween(durationMillis = 150),
                    label = "themePillBorder"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillShape)
                        .background(backgroundColor, pillShape)
                        .border(BorderStroke(1.dp, borderColor), pillShape)
                        .selectable(
                            selected = isSelected,
                            onClick = { onThemeSelected(mode) },
                            role = Role.RadioButton,
                            interactionSource = interactionSource,
                            indication = ripple()
                        )
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, Apple Music-inspired Live Theme Preview showing miniature player and scope reflections.
 * Completely isolated from real playback state (does not change active song or queue).
 */
@Composable
fun ThemeLivePreview(
    isDark: Boolean,
    scope: String,
    style: String,
    preset: String,
    customHexes: String,
    modifier: Modifier = Modifier
) {
    val previewPalette = remember(isDark, style, preset, customHexes) {
        when {
            style.equals("Dynamic Artwork", ignoreCase = true) -> {
                if (isDark) {
                    PlayerArtworkPalette(
                        primary = androidx.compose.ui.graphics.Color(0xFF28104E),
                        secondary = androidx.compose.ui.graphics.Color(0xFF1B0B33),
                        tertiary = androidx.compose.ui.graphics.Color(0xFF0F061D),
                        accent = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                        primaryArgb = 0xFF28104E.toInt(),
                        secondaryArgb = 0xFF1B0B33.toInt(),
                        tertiaryArgb = 0xFF0F061D.toInt(),
                        accentArgb = 0xFF9C27B0.toInt(),
                        isLightArtwork = false
                    )
                } else {
                    PlayerArtworkPalette(
                        primary = androidx.compose.ui.graphics.Color(0xFFE1BEE7),
                        secondary = androidx.compose.ui.graphics.Color(0xFFCE93D8),
                        tertiary = androidx.compose.ui.graphics.Color(0xFFF3E5F5),
                        accent = androidx.compose.ui.graphics.Color(0xFF8E24AA),
                        primaryArgb = 0xFFE1BEE7.toInt(),
                        secondaryArgb = 0xFFCE93D8.toInt(),
                        tertiaryArgb = 0xFFF3E5F5.toInt(),
                        accentArgb = 0xFF8E24AA.toInt(),
                        isLightArtwork = true
                    )
                }
            }
            style.equals("Custom", ignoreCase = true) || style.equals("Custom Palette", ignoreCase = true) -> {
                CustomPaletteValidator.createPaletteFromCustomHexes(customHexes, isDark)
            }
            else -> {
                val cleanPreset = if (preset.equals("Monochrome", ignoreCase = true)) "Midnight Indigo" else preset
                PlayerArtworkPalette.fromPreset(cleanPreset, isDark)
            }
        }
    }

    val previewColors = remember(previewPalette, isDark) {
        resolvePlayerColorScheme(
            isDynamic = true,
            isDark = isDark,
            primary = previewPalette.primary,
            secondary = previewPalette.secondary,
            tertiary = previewPalette.tertiary,
            accent = previewPalette.accent
        )
    }

    val isEntireApp = scope.equals("Entire App", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(previewColors.dialogSurface.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, previewColors.dialogBorder.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(previewColors.dialogAccent)
                )
                Text(
                    text = "Live Preview",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = previewColors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColors.capsuleSurface)
                    .border(BorderStroke(1.dp, previewColors.capsuleBorder), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isEntireApp) "Scope: Entire App" else "Scope: Player Only",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = previewColors.capsuleContent
                )
            }
        }

        // Miniature player preview card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(previewColors.buttonSurface)
                .border(BorderStroke(1.dp, previewColors.buttonBorder), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewColors.ambientAccent.copy(alpha = 0.35f))
                            .border(BorderStroke(1.dp, previewColors.ambientAccent.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = previewColors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aura Harmony",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = previewColors.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = if (style.equals("Dynamic Artwork", ignoreCase = true)) "Dynamic Artwork Palette" else if (style.equals("Custom", ignoreCase = true)) "Custom Palette" else preset,
                            style = MaterialTheme.typography.labelSmall,
                            color = previewColors.textSecondary,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null,
                            tint = previewColors.buttonIconTint,
                            modifier = Modifier.size(18.dp)
                        )

                        // Prominent Play/Pause button - fixed monochrome: black in light mode, white in dark mode
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(previewColors.prominentSurface)
                                .border(BorderStroke(1.dp, previewColors.prominentBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = previewColors.prominentIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = previewColors.buttonIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Miniature progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(previewColors.progressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(previewColors.progressIndicator)
                    )
                }
            }
        }

        if (isEntireApp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Home", "Explore", "Library", "Details").forEach { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(previewColors.buttonSurface.copy(alpha = 0.6f))
                            .border(BorderStroke(0.5.dp, previewColors.buttonBorder.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = previewColors.dialogAccent,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single-choice mutually exclusive Theme Scope Selector.
 */
@Composable
fun ThemeScopeSelector(
    selectedScope: String,
    onScopeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEntireApp = selectedScope.equals("Entire App", ignoreCase = true)
    val options = listOf(
        Triple(
            "Entire App",
            "Apply theme colors across main app screens and the player",
            Icons.Default.Smartphone
        ),
        Triple(
            "Player Only",
            "Keep the main app monochrome and theme the player only",
            Icons.Default.PlayCircleOutline
        )
    )

    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (title, subtitle, icon) ->
            val isSelected = (title == "Entire App" && isEntireApp) || (title == "Player Only" && !isEntireApp)
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                label = "scopeBorder"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                label = "scopeBg"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(16.dp))
                .selectable(
                    selected = isSelected,
                    onClick = { onScopeSelected(title) },
                    role = Role.RadioButton
                )
                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}
