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

@OptIn(ExperimentalLayoutApi::class)
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ThemeSelectionRow(
                            selectedTheme = appSettings.themeMode,
                            onThemeSelected = onThemeModeChange
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                        Text("Lyric font size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Slider(
                            value = appSettings.lyricFontScale,
                            onValueChange = onLyricFontScaleChange,
                            valueRange = 0.8f..1.4f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveTickColor = MaterialTheme.colorScheme.outline
                            )
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


                        var isThemeExpanded by rememberSaveable { mutableStateOf(false) }

                        // 1. Master Switch: Change Theme
                        ToggleRow(
                            title = "Change Theme",
                            subtitle = "Personalize colors across AuraMusic with dynamic artwork, presets, or custom palettes.",
                            checked = appSettings.changeThemeEnabled,
                            onCheckedChange = { isChecked ->
                                onChangeThemeEnabled(isChecked)
                                if (!isChecked) {
                                    isThemeExpanded = false
                                }
                            }
                        )

                        // 2. Expand / Retract control and configuration (only shown when Change Theme is ON)
                        AnimatedVisibility(
                            visible = appSettings.changeThemeEnabled,
                            enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                            exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180))
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                                // Working Expand / Retract button control
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable(
                                            role = Role.Button,
                                            onClick = { isThemeExpanded = !isThemeExpanded }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isThemeExpanded) "Theme Configuration" else "Theme Options",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val summary = if (appSettings.themeScope.equals("Entire App", ignoreCase = true)) {
                                            "Scope: Entire App • Style: ${appSettings.themeColorStyle}"
                                        } else {
                                            "Scope: Player Only • Style: ${appSettings.themeColorStyle}"
                                        }
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isThemeExpanded) "Retract" else "Expand",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = if (isThemeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isThemeExpanded) "Retract Theme Options" else "Expand Theme Options",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // 3. Expandable configuration options
                                AnimatedVisibility(
                                    visible = isThemeExpanded,
                                    enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                                    exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180))
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        // Theme options start directly with the configuration controls.
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                                        // 3b. Theme Scope Selector
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

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                                        // 3c. Color Style Selector
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
                                                    label = { Text(style, style = MaterialTheme.typography.labelMedium) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSelected,
                                                        borderColor = MaterialTheme.colorScheme.outline,
                                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }

                                        // 3d. Style-specific configuration
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
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
                                                            label = { Text(preset, style = MaterialTheme.typography.labelMedium) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = isSelected,
                                                                borderColor = MaterialTheme.colorScheme.outline,
                                                                selectedBorderColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            activeStyle.equals("Custom", ignoreCase = true) || activeStyle.equals("Custom Palette", ignoreCase = true) -> {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        cursorColor = MaterialTheme.colorScheme.primary
                                                    ),
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

                                        // 3e. Themed Content Areas (visible only when Entire App is selected)
                                        if (appSettings.themeScope.equals("Entire App", ignoreCase = true)) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

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

                                        // Bottom Retract helper
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            TextButton(
                                                onClick = { isThemeExpanded = false }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ExpandLess,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Retract theme options", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        title = "Scan local music",
                        subtitle = "Refresh songs from device storage",
                        onClick = onRefresh
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
        uncheckedBorderColor = colors.borderStrong,
        disabledCheckedThumbColor = colors.controlSelectedContent.copy(alpha = 0.5f),
        disabledCheckedTrackColor = colors.controlSelected.copy(alpha = 0.5f),
        disabledUncheckedThumbColor = colors.textTertiary,
        disabledUncheckedTrackColor = colors.surfaceSecondary
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
