package com.aman.auramusic.ui.screen

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import com.aman.auramusic.R
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.LiquidGlassConfig
import com.aman.auramusic.data.model.LiquidGlassPreset
import com.aman.auramusic.data.model.LiquidGlassTintOption
import com.aman.auramusic.data.model.ThemeMode
import com.aman.auramusic.ui.theme.AuraScreenBackground
import com.aman.auramusic.ui.theme.GlassChip
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.LiquidGlassCard
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.liquidGlass

@Composable
fun SettingsScreen(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    username: String,
    appSettings: AppSettings,
    onUsernameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorsChange: (Boolean) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onBlurIntensityChange: (Int) -> Unit,
    onKaraokeChange: (Boolean) -> Unit,
    onLyricFontScaleChange: (Float) -> Unit,
    onCrossfadeChange: (Boolean) -> Unit,
    onGaplessChange: (Boolean) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onSmartAudioFocusChange: (Boolean) -> Unit,
    onKeepPlayingOnCloseChange: (Boolean) -> Unit,
    onPlaylistGridColumnsChange: (Int) -> Unit,
    onImportPlaylistFile: () -> Unit,
    onExportAllSongs: () -> Unit,
    onExportPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onShowAbout: () -> Unit,
    onLiquidGlassPresetChange: (LiquidGlassPreset) -> Unit = {},
    onLiquidGlassIntensityChange: (Float) -> Unit = {},
    onLiquidGlassBlurRadiusChange: (Int) -> Unit = {},
    onLiquidGlassHighlightStrengthChange: (Float) -> Unit = {},
    onLiquidGlassShadowElevationChange: (Int) -> Unit = {},
    onLiquidGlassCornerRadiusChange: (Int) -> Unit = {},
    onLiquidGlassTintHexChange: (String) -> Unit = {},
    onLiquidGlassAccentTintChange: (Boolean) -> Unit = {},
    onLiquidGlassReduceTransparencyChange: (Boolean) -> Unit = {},
    onLiquidGlassReduceMotionChange: (Boolean) -> Unit = {},
    onResetLiquidGlass: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val glassConfig = appSettings.liquidGlass

    AuraScreenBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initial = remember(username) { username.trim().firstOrNull()?.uppercase() ?: "A" }
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(3.dp, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(level = GlassLevel.UltraThin, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // 1. Interactive Liquid Glass Live Preview Card
            item {
                LiquidGlassLivePreview(
                    config = glassConfig,
                    isDark = isDark
                )
            }

            // 2. Personalization
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Personalization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        var localUsername by remember { mutableStateOf(username) }
                        
                        OutlinedTextField(
                            value = localUsername,
                            onValueChange = { 
                                localUsername = it
                                onUsernameChange(it) 
                            },
                            label = { Text("Username") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ThemeSelectionRow(
                            selectedTheme = appSettings.themeMode,
                            onThemeSelected = onThemeModeChange
                        )
                        ToggleRow(title = "Dynamic colors", checked = appSettings.dynamicColors, onCheckedChange = onDynamicColorsChange)
                        ToggleRow(title = "AMOLED dark mode", checked = appSettings.amoledMode, onCheckedChange = onAmoledChange)
                        ToggleRow(title = "Karaoke mode", checked = appSettings.karaokeMode, onCheckedChange = onKaraokeChange)
                    }
                }
            }

            // 3. Dedicated Appearance -> Liquid Glass Customizer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Appearance — Liquid Glass",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Presets
                        PresetSelectorRow(
                            selectedPreset = glassConfig.preset,
                            onPresetSelected = onLiquidGlassPresetChange
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Sliders
                        LiquidGlassSlider(
                            title = "Translucency & Opacity",
                            valueText = "${(glassConfig.intensity * 100).toInt()}%",
                            value = glassConfig.intensity,
                            valueRange = 0.10f..0.90f,
                            onValueChange = onLiquidGlassIntensityChange
                        )

                        LiquidGlassSlider(
                            title = "Background Blur Diffusion",
                            valueText = "${glassConfig.blurRadius} dp",
                            value = glassConfig.blurRadius.toFloat(),
                            valueRange = 0f..50f,
                            onValueChange = { onLiquidGlassBlurRadiusChange(it.toInt()) }
                        )

                        LiquidGlassSlider(
                            title = "Specular Top-Rim Highlight",
                            valueText = "${(glassConfig.highlightStrength * 100).toInt()}%",
                            value = glassConfig.highlightStrength,
                            valueRange = 0.0f..1.0f,
                            onValueChange = onLiquidGlassHighlightStrengthChange
                        )

                        LiquidGlassSlider(
                            title = "Ambient Depth & Shadow",
                            valueText = "${glassConfig.shadowElevation} dp",
                            value = glassConfig.shadowElevation.toFloat(),
                            valueRange = 0f..24f,
                            onValueChange = { onLiquidGlassShadowElevationChange(it.toInt()) }
                        )

                        LiquidGlassSlider(
                            title = "Corner Curvature",
                            valueText = "${glassConfig.cornerRadius} dp",
                            value = glassConfig.cornerRadius.toFloat(),
                            valueRange = 8f..36f,
                            onValueChange = { onLiquidGlassCornerRadiusChange(it.toInt()) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Surface Tint Palette
                        GlassTintPaletteRow(
                            selectedHex = glassConfig.tintHex,
                            onTintSelected = onLiquidGlassTintHexChange
                        )

                        ToggleRow(
                            title = "Subtle Accent Glow",
                            checked = glassConfig.accentTintEnabled,
                            onCheckedChange = onLiquidGlassAccentTintChange
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Accessibility & Fallbacks
                        Text(
                            text = "Accessibility & Performance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ToggleRow(
                            title = "Reduce Transparency (Opaque Material)",
                            checked = glassConfig.reduceTransparency,
                            onCheckedChange = onLiquidGlassReduceTransparencyChange
                        )

                        ToggleRow(
                            title = "Reduce Motion (Disable Sheen Glare)",
                            checked = glassConfig.reduceMotion,
                            onCheckedChange = onLiquidGlassReduceMotionChange
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Reset button
                        OutlinedButton(
                            onClick = onResetLiquidGlass,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Glass to Signature Preset")
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Other Appearance settings
                        Text("Lyric font size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Slider(
                            value = appSettings.lyricFontScale,
                            onValueChange = onLyricFontScaleChange,
                            valueRange = 0.8f..1.4f,
                            steps = 5
                        )
                    }
                }
            }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Playback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ToggleRow(title = "Crossfade", checked = appSettings.crossfadeEnabled, onCheckedChange = onCrossfadeChange)
                    ToggleRow(title = "Gapless playback", checked = appSettings.gaplessEnabled, onCheckedChange = onGaplessChange)
                    ToggleRow(title = "Skip silence", checked = appSettings.skipSilence, onCheckedChange = onSkipSilenceChange)
                    ToggleRow(title = "Smart audio focus", checked = appSettings.smartAudioFocus, onCheckedChange = onSmartAudioFocusChange)
                    ToggleRow(title = "Keep playing on app close", checked = appSettings.keepPlayingOnClose, onCheckedChange = onKeepPlayingOnCloseChange)
                    Text("Playlist view columns: ${appSettings.playlistGridColumns}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appSettings.playlistGridColumns.toFloat(),
                        onValueChange = { onPlaylistGridColumnsChange(it.toInt()) },
                        valueRange = 1f..2f,
                        steps = 0
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("System & Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    SettingsRow(
                        icon = Icons.Default.ArrowUpward,
                        title = "Import from file",
                        subtitle = "Import shared playlist files",
                        onClick = onImportPlaylistFile
                    )

                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Export playlist",
                        subtitle = "Export your playlists to a file",
                        onClick = onExportPlaylist
                    )

                    SettingsRow(
                        icon = Icons.Default.ArrowDownward,
                        title = "Export all songs",
                        subtitle = "Export all songs present on device to JSON",
                        onClick = onExportAllSongs
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        title = "Scan local music",
                        subtitle = "Refresh songs from device storage",
                        onClick = onRefresh
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About Aura Music",
                        subtitle = "Version 3.2.0 (Liquid Glass)",
                        onClick = onShowAbout
                    )
                }
            }
        }
    }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(level = GlassLevel.Thick, shape = RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .liquidGlass(level = GlassLevel.Regular, shape = RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aura_logo),
                        contentDescription = "Aura Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Aura Music",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Version 3.2.0 (Liquid Glass Edition)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "A premium, Apple Music-inspired player with stable background playback, intelligent audio focus, and cross-user playlist sharing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Developed by Aman",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .liquidGlass(level = GlassLevel.Tinted, shape = RoundedCornerShape(22.dp), tint = MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                ThemeMode.SYSTEM to ("System" to Icons.Default.BrightnessAuto),
                ThemeMode.LIGHT to ("Light" to Icons.Default.LightMode),
                ThemeMode.DARK to ("Dark" to Icons.Default.DarkMode)
            )
            themes.forEach { (mode, pair) ->
                val (label, icon) = pair
                val isSelected = selectedTheme == mode
                val pillShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillShape)
                        .clickable { onThemeSelected(mode) }
                        .liquidGlass(
                            level = if (isSelected) GlassLevel.Tinted else GlassLevel.UltraThin,
                            shape = pillShape,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else null
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
                            contentDescription = label,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassLivePreview(
    config: LiquidGlassConfig,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var isPreviewPlaying by remember { mutableStateOf(true) }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape((config.cornerRadius + 4).dp),
        level = GlassLevel.Regular,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE GLASS PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                // Preset badge pill
                Box(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(999.dp),
                            level = GlassLevel.Tinted,
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = config.preset.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Floating Mini-Player Preview Element
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(config.cornerRadius.dp),
                        level = GlassLevel.UltraThin,
                        elevation = (config.shadowElevation * 0.8f).dp
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini artwork placeholder
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(10.dp),
                                level = GlassLevel.Tinted
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liquid Glass Aura",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Apple-inspired Translucent Design",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // Interactive preview buttons
                    IconButton(
                        onClick = { isPreviewPlaying = !isPreviewPlaying },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(shape = CircleShape, level = GlassLevel.UltraThin)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { /* Demo */ },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(shape = CircleShape, level = GlassLevel.UltraThin)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip",
                            tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Live telemetry indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassMetricBadge(label = "Opacity", value = "${(config.intensity * 100).toInt()}%")
                GlassMetricBadge(label = "Blur", value = "${config.blurRadius}dp")
                GlassMetricBadge(label = "Highlight", value = "${(config.highlightStrength * 100).toInt()}%")
                GlassMetricBadge(label = "Depth", value = "${config.shadowElevation}dp")
                GlassMetricBadge(label = "Corner", value = "${config.cornerRadius}dp")
            }
        }
    }
}

@Composable
private fun GlassMetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PresetSelectorRow(
    selectedPreset: LiquidGlassPreset,
    onPresetSelected: (LiquidGlassPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Glass Material Presets",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassPreset.entries.forEach { preset ->
                val selected = selectedPreset == preset
                GlassChip(
                    selected = selected,
                    onClick = { onPresetSelected(preset) },
                    label = preset.displayName
                )
            }
        }
        Text(
            text = selectedPreset.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun LiquidGlassSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun GlassTintPaletteRow(
    selectedHex: String,
    onTintSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Glass Surface Tint",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassTintOption.entries.forEach { option ->
                val isSelected = (option.hexColor.isEmpty() && selectedHex.isEmpty()) ||
                        (option.hexColor.equals(selectedHex, ignoreCase = true))
                val displayColor = if (option.hexColor.isEmpty()) {
                    Color(0xFF33333E)
                } else {
                    runCatching {
                        Color(android.graphics.Color.parseColor(option.hexColor))
                    }.getOrDefault(Color.DarkGray)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTintSelected(option.hexColor) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(displayColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
