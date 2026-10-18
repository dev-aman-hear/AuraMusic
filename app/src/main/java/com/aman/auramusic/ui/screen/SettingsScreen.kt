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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    username: String,
    appSettings: AppSettings,
    onUsernameChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onChangeThemeEnabled: (Boolean) -> Unit = {},
    onThemeScopeChange: (String) -> Unit = {},
    onThemeColorStyleChange: (String) -> Unit = {},
    onThemePresetChange: (String) -> Unit = {},
    onThemeCustomHexesChange: (String) -> Unit = {},
    onDynamicArtworkColorsChange: (Boolean) -> Unit = {},
    onPlayerThemeModeChange: (String) -> Unit = {},
    onColorPresetChange: (String) -> Unit = {},
    onCustomPaletteHexesChange: (String) -> Unit = {},
    onEnableFullAppDynamicThemeChange: (Boolean) -> Unit = {},
    onAppPaletteModeChange: (String) -> Unit = {},
    onAppColorPresetChange: (String) -> Unit = {},
    onAppCustomPaletteHexesChange: (String) -> Unit = {},
    onThemeAreaChange: (String, Boolean) -> Unit = { _, _ -> },
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

            // Unified Appearance / Theme Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. Master Switch: Change Theme
                        ToggleRow(
                            title = "Change Theme",
                            subtitle = "Personalize colors across AuraMusic with dynamic artwork, presets, or custom palettes.",
                            checked = appSettings.changeThemeEnabled,
                            onCheckedChange = { isChecked ->
                                onChangeThemeEnabled(isChecked)
                                onEnableFullAppDynamicThemeChange(isChecked && appSettings.themeScope.equals("Entire App", ignoreCase = true))
                            }
                        )

                        // 2. Progressive disclosure: hidden when OFF
                        AnimatedVisibility(
                            visible = appSettings.changeThemeEnabled,
                            enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                            exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180))
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // 3. Compact Live Theme Preview
                                ThemeLivePreview(
                                    isDark = LocalIsDark.current,
                                    scope = appSettings.themeScope,
                                    style = appSettings.themeColorStyle,
                                    preset = appSettings.appColorPreset,
                                    customHexes = appSettings.appCustomPaletteHexes
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // 4. Theme Scope Selector
                                Text(
                                    text = "Theme Scope",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose where color themes are applied.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ThemeScopeSelector(
                                    selectedScope = appSettings.themeScope,
                                    onScopeSelected = { scope ->
                                        onThemeScopeChange(scope)
                                        onEnableFullAppDynamicThemeChange(scope.equals("Entire App", ignoreCase = true))
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // 5. Color Style Selector
                                Text(
                                    text = "Color Style",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose how theme colors are resolved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val colorStyles = listOf("Dynamic Artwork", "Preset", "Custom")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().selectableGroup()
                                ) {
                                    colorStyles.forEach { style ->
                                        val isSelected = appSettings.themeColorStyle.equals(style, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                onThemeColorStyleChange(style)
                                                onAppPaletteModeChange(style)
                                                onPlayerThemeModeChange(style)
                                            },
                                            label = { Text(style, style = MaterialTheme.typography.labelMedium) }
                                        )
                                    }
                                }

                                // 6. Style-specific configuration
                                val activeStyle = appSettings.themeColorStyle
                                when {
                                    activeStyle.equals("Dynamic Artwork", ignoreCase = true) -> {
                                        Text(
                                            text = "Extracts a balanced, harmonious color palette from the currently playing song's artwork and adapts smoothly as tracks change.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    activeStyle.equals("Preset", ignoreCase = true) -> {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        Text(
                                            text = "Color Preset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val presets = listOf("Midnight Indigo", "Crimson Velvet", "Emerald Pine", "Nordic Slate")
                                        val isDark = LocalIsDark.current
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().selectableGroup()
                                        ) {
                                            presets.forEach { preset ->
                                                val isSelected = appSettings.appColorPreset.equals(preset, ignoreCase = true)
                                                val samplePal = remember(preset, isDark) {
                                                    PlayerArtworkPalette.fromPreset(preset, isDark)
                                                }
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        onThemePresetChange(preset)
                                                        onAppColorPresetChange(preset)
                                                        onColorPresetChange(preset)
                                                    },
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clip(CircleShape)
                                                                .background(samplePal.accent)
                                                        )
                                                    },
                                                    label = { Text(preset, style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    }
                                    activeStyle.equals("Custom", ignoreCase = true) || activeStyle.equals("Custom Palette", ignoreCase = true) -> {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        Text(
                                            text = "Custom Color Editor",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Enter 2 to 5 comma-separated hex colors (e.g. #1A237E, #3949AB, #0D1642).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        var hexInput by remember(appSettings.appCustomPaletteHexes) {
                                            mutableStateOf(appSettings.appCustomPaletteHexes)
                                        }
                                        val parsedHexes = remember(hexInput) {
                                            CustomPaletteValidator.parseHexList(hexInput)
                                        }
                                        val isValid = remember(parsedHexes) {
                                            CustomPaletteValidator.validateHexList(parsedHexes)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            parsedHexes.forEach { hex ->
                                                val col = CustomPaletteValidator.parseHexToColor(hex)
                                                if (col != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(col, CircleShape)
                                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = hexInput,
                                            onValueChange = { newText ->
                                                hexInput = newText
                                                if (CustomPaletteValidator.validate(newText)) {
                                                    onThemeCustomHexesChange(newText)
                                                    onAppCustomPaletteHexesChange(newText)
                                                    onCustomPaletteHexesChange(newText)
                                                }
                                            },
                                            label = { Text("Hex Colors") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (!isValid) {
                                            Text(
                                                text = "Requires between 2 and 5 valid hex codes (e.g., #1A237E, #3949AB)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    hexInput = CustomPaletteValidator.DEFAULT_HEX_STRING
                                                    onThemeCustomHexesChange(CustomPaletteValidator.DEFAULT_HEX_STRING)
                                                    onAppCustomPaletteHexesChange(CustomPaletteValidator.DEFAULT_HEX_STRING)
                                                    onCustomPaletteHexesChange(CustomPaletteValidator.DEFAULT_HEX_STRING)
                                                }
                                            ) {
                                                Text("Reset to Default", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }

                                // 7. Themed Content Areas (visible only when Entire App is selected)
                                if (appSettings.themeScope.equals("Entire App", ignoreCase = true)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    Text(
                                        text = "Themed Content Areas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Choose which screens use theme colors when full-app theming is active.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    ToggleRow(
                                        title = "Home Screen",
                                        subtitle = "Featured cards and recommendations",
                                        checked = appSettings.themeHomeEnabled,
                                        onCheckedChange = { onThemeAreaChange("HOME", it) }
                                    )

                                    ToggleRow(
                                        title = "Explore & Search",
                                        subtitle = "Online feeds, mood charts, and search screens",
                                        checked = appSettings.themeExploreEnabled,
                                        onCheckedChange = { onThemeAreaChange("EXPLORE", it) }
                                    )

                                    ToggleRow(
                                        title = "Library",
                                        subtitle = "Tracks, favorites, and playlists lists",
                                        checked = appSettings.themeLibraryEnabled,
                                        onCheckedChange = { onThemeAreaChange("LIBRARY", it) }
                                    )

                                    ToggleRow(
                                        title = "Album Details",
                                        subtitle = "Album view and song collections",
                                        checked = appSettings.themeAlbumDetailEnabled,
                                        onCheckedChange = { onThemeAreaChange("ALBUM_DETAIL", it) }
                                    )

                                    ToggleRow(
                                        title = "Artist Pages",
                                        subtitle = "Artist bio, discography, and profile headers",
                                        checked = appSettings.themeArtistDetailEnabled,
                                        onCheckedChange = { onThemeAreaChange("ARTIST_DETAIL", it) }
                                    )

                                    ToggleRow(
                                        title = "Playlist Details",
                                        subtitle = "Local and online playlist views",
                                        checked = appSettings.themePlaylistDetailEnabled,
                                        onCheckedChange = { onThemeAreaChange("PLAYLIST_DETAIL", it) }
                                    )
                                }
                            }
                        }
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
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                label = "scopeBorder"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                    onClick = null
                )
            }
        }
    }
}



