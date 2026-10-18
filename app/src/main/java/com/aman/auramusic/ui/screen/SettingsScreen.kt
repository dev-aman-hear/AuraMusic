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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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

@Composable
fun SettingsScreen(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    username: String,
    appSettings: AppSettings,
    onUsernameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicArtworkColorsChange: (Boolean) -> Unit = {},
    onColorPresetChange: (String) -> Unit = {},
    onKaraokeChange: (Boolean) -> Unit = {},
    onLyricFontScaleChange: (Float) -> Unit,
    onSmartAudioFocusChange: (Boolean) -> Unit,
    onKeepPlayingOnCloseChange: (Boolean) -> Unit,
    onPlaylistGridColumnsChange: (Int) -> Unit,
    onImportPlaylistFile: () -> Unit,
    onExportAllSongs: () -> Unit,
    onExportPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                        com.aman.auramusic.ui.screen.home.AuraAvatarBadge(
                            initial = initial,
                            size = 42.dp
                        )
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // Personalization
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ToggleRow(
                            title = "Dynamic Artwork Colors",
                            subtitle = "Adapt the player background to current song artwork. Takes precedence over presets when enabled.",
                            checked = appSettings.dynamicArtworkColors,
                            onCheckedChange = onDynamicArtworkColorsChange
                        )

                        val presets = listOf("Monochrome", "Midnight Indigo", "Crimson Velvet", "Emerald Pine", "Nordic Slate")
                        Text("Player Palette Preset", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (appSettings.dynamicArtworkColors) {
                                "Dynamic artwork colors are active. Selecting a preset switches the player to that static palette."
                            } else {
                                "Choose a cohesive color scheme for the player surfaces."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presets.forEach { preset ->
                                val isSelected = appSettings.colorPreset.equals(preset, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onColorPresetChange(preset)
                                        if (appSettings.dynamicArtworkColors) {
                                            onDynamicArtworkColorsChange(false)
                                        }
                                    },
                                    label = { Text(preset, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Playback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ToggleRow(title = "Smart audio focus", checked = appSettings.smartAudioFocus, onCheckedChange = onSmartAudioFocusChange)
                    ToggleRow(title = "Keep playing on app close", checked = appSettings.keepPlayingOnClose, onCheckedChange = onKeepPlayingOnCloseChange)
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
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

                    var cacheUsage by remember { mutableStateOf(com.aman.auramusic.online.player.CacheManager.getFormattedCacheUsage(context)) }

                    SettingsRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear audio cache",
                        subtitle = "Currently used: $cacheUsage",
                        onClick = {
                            com.aman.auramusic.online.player.CacheManager.clearCache(context)
                            cacheUsage = com.aman.auramusic.online.player.CacheManager.getFormattedCacheUsage(context)
                        }
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
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
                        subtitle = "Version ${BuildConfig.VERSION_NAME}",
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
    val colors = Monochrome
    val dialogShape = RoundedCornerShape(28.dp)
    val buttonShape = RoundedCornerShape(22.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetButtonBg = if (isPressed) colors.surfacePressed else colors.controlSelected
    val targetButtonText = if (isPressed) colors.textPrimary else colors.controlSelectedContent
    val targetButtonBorder = if (isPressed) colors.borderStrong else colors.controlSelected

    val buttonBg by animateColorAsState(targetValue = targetButtonBg, animationSpec = tween(150), label = "aboutCloseBg")
    val buttonText by animateColorAsState(targetValue = targetButtonText, animationSpec = tween(150), label = "aboutCloseText")
    val buttonBorder by animateColorAsState(targetValue = targetButtonBorder, animationSpec = tween(150), label = "aboutCloseBorder")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dialogShape)
                .background(colors.surface, dialogShape)
                .border(BorderStroke(1.dp, colors.border), dialogShape)
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surfaceSecondary, RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aura_logo),
                        contentDescription = "Aura Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aura Music is a modern music player inspired by Apple Music, combining a clean listening experience with local music playback, online music discovery, playlists, lyrics, and customizable playback settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Developed by Aman",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(buttonShape)
                        .background(buttonBg, buttonShape)
                        .border(BorderStroke(1.dp, buttonBorder), buttonShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        color = buttonText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    val colors = Monochrome
    val isDark = LocalIsDark.current

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colors.controlSelectedContent,
        checkedTrackColor = colors.controlSelected,
        checkedBorderColor = colors.controlSelected,
        uncheckedThumbColor = if (isDark) colors.textSecondary else colors.background,
        uncheckedTrackColor = if (isDark) colors.border else colors.surfaceSecondary,
        uncheckedBorderColor = colors.borderStrong
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors
        )
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


