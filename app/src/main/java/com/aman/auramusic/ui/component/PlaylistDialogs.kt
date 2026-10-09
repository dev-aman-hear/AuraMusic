package com.aman.auramusic.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aman.auramusic.data.model.Playlist
import com.aman.auramusic.viewmodel.MusicViewModel

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Add to Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Create New Playlist", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(playlists) { playlist ->
                        TextButton(onClick = { onPlaylistSelected(playlist) }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun ExportPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Export Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Text(text = "No playlists to export", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { playlist ->
                            TextButton(onClick = { onPlaylistSelected(playlist) }, modifier = Modifier.fillMaxWidth()) {
                                Text(text = playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun ImportResultDialog(
    result: MusicViewModel.ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Result") },
        text = {
            Column {
                Text("Playlist: ${result.playlistName}", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Successfully matched: ${result.matchedCount} local songs")

                if (result.unmatchedSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Unmatched songs (${result.unmatchedSongs.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .padding(top = 8.dp)
                    ) {
                        items(result.unmatchedSongs) { song ->
                            Text(
                                text = "• $song",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "New Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = { if (name.isNotBlank()) onConfirm(name) },
                        enabled = name.isNotBlank()
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Rename Playlist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onRename(newName) }) { Text("Rename") }
                }
            }
        }
    }
}
