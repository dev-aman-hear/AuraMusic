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
import com.aman.auramusic.R
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.ThemeMode

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
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

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Blur intensity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appSettings.blurIntensity.toFloat(),
                        onValueChange = { onBlurIntensityChange(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9
                    )
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
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
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
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)) {
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
            SettingsRow(
                icon = Icons.Default.Refresh,
                title = "Scan local music",
                subtitle = "Refresh songs from device storage",
                onClick = onRefresh
            )
        }

        item {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About Aura Music",
                    subtitle = "Version 3.1.0",
                    onClick = onShowAbout
                )
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.aura_logo),
                            contentDescription = "Aura Logo",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Aura Music",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Version 3.1.0 (Glass Edition)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
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
                Surface(
                    onClick = { onThemeSelected(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
