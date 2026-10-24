package com.aman.auramusic.party

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Small LAN-only relay for Party Mode. It forwards control metadata, never audio.
 * Guests authenticate with the one-time party code; the host uses a random private token.
 */
internal class LocalPartyServer(
    private val pairingCode: String,
    private val hostToken: String,
    port: Int = DEFAULT_PORT
) : NanoWSD(port) {
    private val clients = CopyOnWriteArraySet<ClientSocket>()
    @Volatile private var host: ClientSocket? = null
    @Volatile private var latestPlayback: JSONObject? = null
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun openWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession): WebSocket =
        ClientSocket(this, handshakeRequest)

    private inner class ClientSocket(server: NanoWSD, request: NanoHTTPD.IHTTPSession) :
        WebSocket(server, request) {
        @Volatile var role: String? = null

        override fun onOpen() {
            clients.add(this)
        }

        override fun onMessage(message: WebSocketFrame) {
            val json = runCatching { JSONObject(message.textPayload) }.getOrNull() ?: run {
                close(WebSocketFrame.CloseCode.ProtocolError, "Invalid JSON", false)
                return
            }
            when (json.optString("type")) {
                "authenticate" -> {
                    if (role != null) return
                    val token = json.optString("memberToken")
                    role = when {
                        token.isNotBlank() && token == hostToken && host == null -> "host"
                        token.isNotBlank() && token == pairingCode -> "guest"
                        else -> null
                    }
                    if (role == null) {
                        close(WebSocketFrame.CloseCode.PolicyViolation, "Invalid party credentials", false)
                        return
                    }
                    if (role == "host") host = this
                    latestPlayback?.let { sendJson(JSONObject().put("type", "party_state").put("song", it).put("members", clients.count { c -> c.role != null }).put("serverTimeMs", System.currentTimeMillis())) }
                    broadcast(JSONObject().put("type", "member_count").put("members", clients.count { it.role != null }))
                }
                "playback" -> if (role == "host") {
                    latestPlayback = json.optJSONObject("song")
                    broadcast(JSONObject()
                        .put("type", "party_state")
                        .put("song", latestPlayback)
                        .put("members", clients.count { it.role != null })
                        .put("serverTimeMs", System.currentTimeMillis()), this)
                }
                "host_command" -> if (role == "host") broadcast(json, this)
                "song_request" -> if (role == "guest") {
                    val requestId = json.optString("requestId").ifBlank { randomId() }
                    json.put("requestId", requestId)
                    json.put("requester", "Guest")
                    pendingRequests[requestId] = json.optString("title")
                    host?.sendJson(json)
                }
                "song_request_response" -> if (role == "host") {
                    val requestId = json.optString("requestId")
                    val title = pendingRequests.remove(requestId).orEmpty()
                    broadcast(JSONObject()
                        .put("type", "song_request_decision")
                        .put("requestId", requestId)
                        .put("title", title)
                        .put("approved", json.optBoolean("approved")), this)
                }
                "leave" -> close(WebSocketFrame.CloseCode.Normal, "Member left", false)
                else -> if (role != null) {
                    sendJson(JSONObject().put("type", "error").put("message", "Unsupported or unauthorized message."))
                }
            }
        }

        override fun onClose(code: WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean) {
            clients.remove(this)
            if (host === this) host = null
            broadcast(JSONObject().put("type", "member_count").put("members", clients.count { it.role != null }))
        }

        override fun onPong(pong: WebSocketFrame) = Unit

        override fun onException(exception: java.io.IOException) {
            clients.remove(this)
            if (host === this) host = null
        }

        fun sendJson(json: JSONObject) {
            runCatching { send(json.toString()) }
        }
    }

    private fun broadcast(json: JSONObject, except: ClientSocket? = null) {
        clients.filter { it.role != null && it !== except }.forEach { it.sendJson(json) }
    }

    override fun serveHttp(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Party Mode WebSocket only")

    companion object {
        const val DEFAULT_PORT = 8765

        fun localIpv4Address(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.startsWith("169.254.") != true }
                ?.hostAddress
        }.getOrNull()

        fun newPairingCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val random = SecureRandom()
            return (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }

        fun newHostToken(): String = java.util.UUID.randomUUID().toString()
        private fun randomId(): String = java.util.UUID.randomUUID().toString()
    }
}
