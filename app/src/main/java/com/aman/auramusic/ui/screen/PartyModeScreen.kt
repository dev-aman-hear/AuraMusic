package com.aman.auramusic.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aman.auramusic.party.PartyModeRepository
import com.aman.auramusic.party.PartyRole
import com.aman.auramusic.party.PartyPlayback
import kotlinx.coroutines.launch

@Composable
fun PartyModeScreen(
    repository: PartyModeRepository,
    onBack: () -> Unit,
    onRemotePlayback: (PartyPlayback) -> Unit = {},
    onHostCommand: (String) -> Unit = {},
    onApprovedSongRequest: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var localHostAddress by remember { mutableStateOf("") }
    var localPairingCode by remember { mutableStateOf("") }
    var requestTitle by remember { mutableStateOf("") }
    var requestArtist by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(state.playback, state.session?.role) {
        val playback = state.playback
        if (state.session?.role == PartyRole.GUEST && playback != null) onRemotePlayback(playback)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Party Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Listen together, wherever you are", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(28.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Online songs only", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Party Mode synchronizes online playback. Local files and offline transfers are not supported.", style = MaterialTheme.typography.bodyMedium)
                    Text("Both devices need compatible access to the same song source.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.session == null) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your name (optional)") },
                    singleLine = true
                )
                Button(
                    onClick = { scope.launch { repository.createParty(displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting
                ) { Text("Create Internet Party") }
                OutlinedButton(
                    onClick = { scope.launch { repository.createLocalParty(displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting
                ) { Text("Create Local Wi-Fi Party") }
                Divider()
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pairing code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedButton(
                    onClick = { scope.launch { repository.joinParty(pairingCode, displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting && pairingCode.length >= 4
                ) { Text("Join Internet Party") }
                Divider()
                Text("Join a local Wi-Fi party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = localHostAddress,
                    onValueChange = { localHostAddress = it.filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == ':' }.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Host local IP address") },
                    placeholder = { Text("e.g. 192.168.1.10") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = localPairingCode,
                    onValueChange = { localPairingCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Local party code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedButton(
                    onClick = { scope.launch { repository.joinLocalParty(localHostAddress, localPairingCode, displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting && localHostAddress.isNotBlank() && localPairingCode.length >= 4
                ) { Text("Join Local Wi-Fi Party") }
            } else {
                val session = state.session!!
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (session.role == PartyRole.HOST) "You're the host" else "You're listening", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(state.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (state.connected) Icon(Icons.Default.Check, contentDescription = "Connected")
                            else if (state.connecting) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                        if (session.connectionMode == com.aman.auramusic.party.PartyConnectionMode.LOCAL_WIFI) {
                            Text("CONNECTION: LOCAL WI-FI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (session.role == PartyRole.HOST && !session.hostAddress.isNullOrBlank()) {
                                Text("Host IP: " + session.hostAddress + ":" + com.aman.auramusic.party.LocalPartyServer.DEFAULT_PORT, style = MaterialTheme.typography.bodyMedium)
                            } else if (session.role == PartyRole.GUEST) {
                                Text("Host IP: " + (session.hostAddress ?: ""), style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text("CONNECTION: INTERNET", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (session.role == PartyRole.HOST && session.pairingCode.isNotBlank()) {
                            Text("PARTY CODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(session.pairingCode, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AuraMusic Party code", session.pairingCode))
                                    Toast.makeText(context, "Pairing code copied", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy pairing code") }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Text("  " + state.members + if (state.members == 1) " listener" else " listeners")
                        }
                        state.playback?.let { playback ->
                            Divider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(34.dp))
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(playback.title.ifBlank { "Waiting for a song" }, fontWeight = FontWeight.SemiBold)
                                    Text(playback.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (session.role == PartyRole.HOST) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    IconButton(onClick = { onHostCommand("previous") }) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous") }
                                    IconButton(onClick = { onHostCommand(if (playback.isPlaying) "pause" else "play") }) {
                                        Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play or pause")
                                    }
                                    IconButton(onClick = { onHostCommand("next") }) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
                                }
                            }
                        }
                        OutlinedButton(onClick = { repository.leaveParty() }, modifier = Modifier.fillMaxWidth()) { Text("Leave Party") }
                    }
                }

                if (session.role == PartyRole.GUEST) {
                    Text("Request a song", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = requestTitle, onValueChange = { requestTitle = it.take(180) }, modifier = Modifier.fillMaxWidth(), label = { Text("Song title") }, singleLine = true)
                    OutlinedTextField(value = requestArtist, onValueChange = { requestArtist = it.take(180) }, modifier = Modifier.fillMaxWidth(), label = { Text("Artist") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { repository.requestSong(requestTitle, requestArtist, "YOUTUBE"); requestTitle = ""; requestArtist = "" }, enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Request YouTube Music") }
                        OutlinedButton(onClick = { repository.requestSong(requestTitle, requestArtist, "JIOSAAVN"); requestTitle = ""; requestArtist = "" }, enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Request JioSaavn") }
                    }
                    state.decisionMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                if (session.role == PartyRole.HOST && state.requests.isNotEmpty()) {
                    Text("Song requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    state.requests.forEach { request ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(request.title, fontWeight = FontWeight.SemiBold)
                                Text(request.artist + " • " + request.source, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        repository.respondToRequest(request.requestId, true)
                                        onApprovedSongRequest(request.title, request.artist, request.source)
                                    }) { Text("Approve") }
                                    OutlinedButton(onClick = { repository.respondToRequest(request.requestId, false) }) { Text("Reject") }
                                }
                            }
                        }
                    }
                }
            }

            if (state.connecting) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("  Connecting…")
                }
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (repository.isConfigured()) "Internet Party is configured. Local Wi-Fi Party works without a public backend." else "Local Wi-Fi Party works without a public backend. Internet Party requires a secure backend URL.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
