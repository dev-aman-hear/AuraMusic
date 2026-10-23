package com.aman.auramusic.party

import com.aman.auramusic.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Client for AuraMusic's Party Mode protocol.
 * Configure -PpartyApiBaseUrl=https://your-host.example when building the app.
 * The backend must implement the protocol documented in PARTY_MODE.md.
 */
class PartyModeRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val mutableState = MutableStateFlow(PartyUiState())
    val state: StateFlow<PartyUiState> = mutableState.asStateFlow()

    private var socket: WebSocket? = null
    private var latestSession: PartySession? = null

    private val baseUrl: String
        get() = BuildConfig.PARTY_API_BASE_URL.trim().trimEnd('/')

    fun isConfigured(): Boolean = baseUrl.startsWith("https://", ignoreCase = true)

    suspend fun createParty(displayName: String) {
        requireConfigured()
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Creating party…")
        try {
            val response = post("/v1/parties", JSONObject().put("displayName", displayName.trim().ifBlank { "AuraMusic listener" }))
            connect(parseSession(response, PartyRole.HOST))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            mutableState.value = mutableState.value.copy(connecting = false, error = e.message ?: "Could not create party.")
        }
    }

    suspend fun joinParty(pairingCode: String, displayName: String) {
        requireConfigured()
        val normalizedCode = pairingCode.trim().uppercase().replace(" ", "")
        require(normalizedCode.length in 4..12) { "Enter a valid pairing code." }
        mutableState.value = mutableState.value.copy(connecting = true, error = null, status = "Joining party…")
        try {
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
        latestSession = null
        mutableState.value = PartyUiState(status = "You left the party.")
    }

    fun close() {
        socket?.close(1000, "Party screen closed")
        socket = null
        latestSession = null
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

    private fun parseSession(json: JSONObject, role: PartyRole): PartySession {
        val session = PartySession(
            partyId = json.optNonBlank("partyId") ?: throw IOException("Server response is missing partyId."),
            pairingCode = json.optNonBlank("pairingCode") ?: "",
            memberToken = json.optNonBlank("memberToken") ?: throw IOException("Server response is missing memberToken."),
            role = role,
            websocketUrl = json.optNonBlank("websocketUrl") ?: throw IOException("Server response is missing websocketUrl."),
            expiresAt = json.optNonBlank("expiresAt")
        )
        val wsScheme = session.websocketUrl.substringBefore(':').lowercase()
        require(wsScheme == "wss") { "Party Mode requires a secure WSS connection." }
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
            status = "Connecting securely…",
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
                    status = if (session.role == PartyRole.HOST) "Party is live. Share the code with friends." else "Connected. Waiting for the host's music."
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

    private fun handleServerMessage(raw: String) {
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
            "song_request_removed" -> {
                val id = message.optString("requestId")
                mutableState.value = mutableState.value.copy(requests = mutableState.value.requests.filterNot { it.requestId == id })
            }
            "member_count" -> mutableState.value = mutableState.value.copy(members = message.optInt("members", 1).coerceAtLeast(1))
            "error" -> mutableState.value = mutableState.value.copy(error = message.optString("message", "Party server error."))
        }
    }
}
