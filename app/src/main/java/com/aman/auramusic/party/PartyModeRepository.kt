package com.aman.auramusic.party

import android.content.Context
import com.aman.auramusic.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Client for AuraMusic's Party Mode protocol.
 * Configure -PpartyApiBaseUrl=https://your-host.example when building the app for Internet Party Mode.
 * Local Wi-Fi Party works without an external backend.
 * The backend must implement the protocol documented in PARTY_MODE.md.
 */
class PartyModeRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val context: Context? = null
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val mutableState = MutableStateFlow(PartyUiState())
    val state: StateFlow<PartyUiState> = mutableState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val discovery = context?.let { LocalPartyDiscovery(it) }

    private var socket: WebSocket? = null
    private var latestSession: PartySession? = null
    private var localServer: LocalPartyServer? = null

    init {
        discovery?.let { disc ->
            repositoryScope.launch {
                disc.discoveredParties.collect { parties ->
                    mutableState.value = mutableState.value.copy(discoveredParties = parties)
                }
            }
        }
    }

    private val baseUrl: String
        get() = BuildConfig.PARTY_API_BASE_URL.trim().trimEnd('/')

    fun isConfigured(): Boolean = baseUrl.startsWith("https://", ignoreCase = true)

    fun startLocalDiscovery() {
        discovery?.startDiscovery()
    }

    fun stopLocalDiscovery() {
        discovery?.stopDiscovery()
    }

    suspend fun createParty(displayName: String) {
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Creating party…")
        try {
            requireConfigured()
            val response = post("/v1/parties", JSONObject().put("displayName", displayName.trim().ifBlank { "AuraMusic listener" }))
            connect(parseSession(response, PartyRole.HOST))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutableState.value = mutableState.value.copy(connecting = false, error = e.message ?: "Could not create party.")
        }
    }

    suspend fun createLocalParty(displayName: String) {
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Starting local Wi-Fi party…")
        try {
            localServer?.stop()
            discovery?.unregisterParty()

            val code = LocalPartyServer.newPairingCode()
            val hostToken = LocalPartyServer.newHostToken()
            val server = LocalPartyServer(code, hostToken)
            server.start()
            localServer = server

            val address = LocalPartyServer.localIpv4Address()
            val hostDisplay = displayName.trim().ifBlank { "AuraMusic Host" }
            discovery?.registerParty(code, hostDisplay, LocalPartyServer.DEFAULT_PORT)

            val session = PartySession(
                partyId = "local-$code",
                pairingCode = code,
                memberToken = hostToken,
                role = PartyRole.HOST,
                websocketUrl = "ws://127.0.0.1:${LocalPartyServer.DEFAULT_PORT}",
                connectionMode = PartyConnectionMode.LOCAL_WIFI,
                hostAddress = address
            )
            connect(session)
            mutableState.value = mutableState.value.copy(
                status = if (address != null) "Local party started. Share the IP, port, and code with guests." else "Local party started. Find the host IP in Wi-Fi settings."
            )
        } catch (e: Exception) {
            localServer?.stop()
            localServer = null
            discovery?.unregisterParty()
            mutableState.value = mutableState.value.copy(connecting = false, connected = false, error = e.message ?: "Could not start local Wi-Fi party.")
        }
    }

    suspend fun joinLocalParty(hostAddress: String, pairingCode: String, displayName: String) {
        val address = hostAddress.trim().removePrefix("ws://").removePrefix("http://").substringBefore(':').trim()
        val code = pairingCode.trim().uppercase().replace(" ", "")
        if (address.isBlank() || code.length !in 4..12) {
            mutableState.value = mutableState.value.copy(error = "Enter the host's local IP address and valid party code.")
            return
        }
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Joining local Wi-Fi party…")
        val session = PartySession(
            partyId = "local-$code",
            pairingCode = code,
            memberToken = code,
            role = PartyRole.GUEST,
            websocketUrl = "ws://$address:${LocalPartyServer.DEFAULT_PORT}",
            connectionMode = PartyConnectionMode.LOCAL_WIFI,
            hostAddress = address
        )
        connect(session)
    }

    suspend fun joinParty(pairingCode: String, displayName: String) {
        val normalizedCode = pairingCode.trim().uppercase().replace(" ", "")
        if (normalizedCode.length !in 4..12) {
            mutableState.value = mutableState.value.copy(error = "Enter a valid pairing code.")
            return
        }
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Joining party…")
        try {
            requireConfigured()
            val body = JSONObject()
                .put("pairingCode", normalizedCode)
                .put("displayName", displayName.trim().ifBlank { "AuraMusic listener" })
            val response = post("/v1/parties/join", body)
            connect(parseSession(response, PartyRole.GUEST))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutableState.value = mutableState.value.copy(connecting = false, error = e.message ?: "Could not join party.")
        }
    }

    fun sendPlayback(songId: String, title: String, artist: String, artworkUrl: String, source: String, positionMs: Long, isPlaying: Boolean) {
        val session = latestSession ?: return
        if (session.role != PartyRole.HOST || !mutableState.value.connected) return
        send(JSONObject()
            .put("type", "playback")
            .put("song", JSONObject()
                .put("songId", songId)
                .put("title", title)
                .put("artist", artist)
                .put("artworkUrl", artworkUrl)
                .put("source", source)
                .put("positionMs", positionMs.coerceAtLeast(0L))
                .put("isPlaying", isPlaying)
                .put("sentAtMs", System.currentTimeMillis())))
    }

    fun requestSong(title: String, artist: String, source: String) {
        if (latestSession == null || !mutableState.value.connected) return
        send(JSONObject().put("type", "song_request")
            .put("title", title.trim())
            .put("artist", artist.trim())
            .put("source", source))
    }

    fun respondToRequest(requestId: String, approved: Boolean) {
        val session = latestSession ?: return
        if (session.role != PartyRole.HOST) return
        mutableState.value = mutableState.value.copy(
            requests = mutableState.value.requests.filterNot { it.requestId == requestId }
        )
        send(JSONObject().put("type", "song_request_response")
            .put("requestId", requestId)
            .put("approved", approved))
    }

    fun sendHostCommand(command: String, positionMs: Long? = null) {
        val session = latestSession ?: return
        if (session.role != PartyRole.HOST) return
        val payload = JSONObject().put("type", "host_command").put("command", command)
        if (positionMs != null) payload.put("positionMs", positionMs.coerceAtLeast(0L))
        send(payload)
    }

    fun leaveParty() {
        runCatching { send(JSONObject().put("type", "leave")) }
        socket?.close(1000, "User left party")
        socket = null
        discovery?.unregisterParty()
        if (latestSession?.connectionMode == PartyConnectionMode.LOCAL_WIFI && latestSession?.role == PartyRole.HOST) {
            runCatching { localServer?.stop() }
            localServer = null
        }
        latestSession = null
        mutableState.value = PartyUiState(
            status = "You left the party.",
            discoveredParties = mutableState.value.discoveredParties
        )
    }

    fun close() {
        socket?.close(1000, "Party screen closed")
        socket = null
        discovery?.unregisterParty()
        discovery?.stopDiscovery()
        if (latestSession?.connectionMode == PartyConnectionMode.LOCAL_WIFI && latestSession?.role == PartyRole.HOST) {
            runCatching { localServer?.stop() }
            localServer = null
        }
        latestSession = null
        repositoryScope.cancel()
    }

    private fun requireConfigured() {
        if (!isConfigured()) {
            throw IllegalStateException("Party Mode needs a secure backend URL. Set partyApiBaseUrl in Gradle properties.")
        }
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl + path)
            .post(body.toString().toRequestBody(jsonType))
            .header("Accept", "application/json")
            .build()
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val raw = it.body?.string().orEmpty()
                        if (!continuation.isActive) return
                        if (!it.isSuccessful) {
                            val message = runCatching { JSONObject(raw).optString("message") }.getOrNull()
                            continuation.resumeWith(Result.failure(IOException(message?.takeIf(String::isNotBlank) ?: "Party server returned HTTP " + it.code)))
                        } else {
                            try {
                                continuation.resume(JSONObject(raw))
                            } catch (e: Exception) {
                                continuation.resumeWith(Result.failure(IOException("Invalid party server response.", e)))
                            }
                        }
                    }
                }
            })
        }
    }

    internal fun parseSession(json: JSONObject, role: PartyRole): PartySession {
        val session = PartySession(
            partyId = json.optNonBlank("partyId") ?: throw IOException("Server response is missing partyId."),
            pairingCode = json.optNonBlank("pairingCode") ?: "",
            memberToken = json.optNonBlank("memberToken") ?: throw IOException("Server response is missing memberToken."),
            role = role,
            websocketUrl = json.optNonBlank("websocketUrl") ?: throw IOException("Server response is missing websocketUrl."),
            expiresAt = json.optNonBlank("expiresAt")
        )
        val wsScheme = session.websocketUrl.substringBefore(':').lowercase()
        if (session.connectionMode == PartyConnectionMode.INTERNET) {
            require(wsScheme == "wss") { "Internet Party Mode requires a secure WSS connection." }
        } else {
            require(wsScheme == "ws" || wsScheme == "wss") { "Invalid local party address." }
        }
        return session
    }

    private fun connect(session: PartySession) {
        socket?.close(1000, "Replacing party connection")
        latestSession = session
        mutableState.value = mutableState.value.copy(
            session = session,
            connecting = true,
            connected = false,
            error = null,
            status = if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) "Connecting over local Wi-Fi…" else "Connecting securely over the Internet…",
            requests = emptyList()
        )
        val request = Request.Builder().url(session.websocketUrl).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject()
                    .put("type", "authenticate")
                    .put("partyId", session.partyId)
                    .put("memberToken", session.memberToken)
                    .toString())
                mutableState.value = mutableState.value.copy(
                    connecting = false,
                    connected = true,
                    status = if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                        if (session.role == PartyRole.HOST) "Local Wi-Fi party is live." else "Connected over local Wi-Fi."
                    } else if (session.role == PartyRole.HOST) "Party is live. Share the code with friends." else "Connected. Waiting for the host's music."
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (latestSession?.partyId == session.partyId) {
                    mutableState.value = mutableState.value.copy(connected = false, connecting = false, status = "Disconnected: " + reason.ifBlank { "connection closed" })
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (latestSession?.partyId == session.partyId) {
                    mutableState.value = mutableState.value.copy(
                        connected = false,
                        connecting = false,
                        error = t.message ?: "Connection to party failed.",
                        status = "Party connection lost."
                    )
                }
            }
        })
    }

    private fun send(payload: JSONObject) {
        socket?.send(payload.toString())
    }

    internal fun handleServerMessage(raw: String) {
        val message = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (message.optString("type")) {
            "party_state" -> {
                val song = message.optJSONObject("song")
                val playback = song?.let {
                    PartyPlayback(
                        songId = it.optString("songId"),
                        title = it.optString("title"),
                        artist = it.optString("artist"),
                        artworkUrl = it.optString("artworkUrl"),
                        source = it.optString("source"),
                        positionMs = it.optLong("positionMs").coerceAtLeast(0L),
                        isPlaying = it.optBoolean("isPlaying"),
                        serverTimeMs = message.optLong("serverTimeMs")
                    )
                }
                mutableState.value = mutableState.value.copy(
                    playback = playback,
                    members = message.optInt("members", mutableState.value.members).coerceAtLeast(1),
                    status = if (playback == null) "Connected. Waiting for the host's music." else "Synchronized with the host."
                )
            }
            "song_request" -> {
                val request = PartySongRequest(
                    requestId = message.optString("requestId"),
                    title = message.optString("title"),
                    artist = message.optString("artist"),
                    source = message.optString("source"),
                    requester = message.optString("requester")
                )
                mutableState.value = mutableState.value.copy(requests = mutableState.value.requests.filterNot { it.requestId == request.requestId } + request)
            }
            "song_request_decision" -> {
                val decision = if (message.optBoolean("approved")) "Host approved: " else "Host declined: "
                mutableState.value = mutableState.value.copy(decisionMessage = decision + message.optString("title"))
            }
            "song_request_removed" -> {
                val id = message.optString("requestId")
                mutableState.value = mutableState.value.copy(requests = mutableState.value.requests.filterNot { it.requestId == id })
            }
            "member_count" -> mutableState.value = mutableState.value.copy(members = message.optInt("members", 1).coerceAtLeast(1))
            "error" -> mutableState.value = mutableState.value.copy(error = message.optString("message", "Party server error."))
        }
    }
}
