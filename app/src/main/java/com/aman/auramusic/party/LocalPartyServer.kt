package com.aman.auramusic.party

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Small LAN-only relay for Party Mode. It forwards control metadata, never audio.
 * Guests authenticate with the one-time party code; the host uses a random private token.
 * Does not transmit audio, stream URLs, cookies, or credentials.
 */
internal class LocalPartyServer(
    private val pairingCode: String,
    private val hostToken: String,
    port: Int = DEFAULT_PORT
) : NanoWSD(port) {
    private val clients = CopyOnWriteArraySet<ClientSocket>()
    @Volatile private var host: ClientSocket? = null
    @Volatile private var latestPlayback: JSONObject? = null
    private val pendingRequests = ConcurrentHashMap<String, String>()

    override fun openWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession): WebSocket =
        ClientSocket(handshakeRequest)

    override fun stop() {
        for (client in clients) {
            runCatching {
                client.close(WebSocketFrame.CloseCode.NormalClosure, "Party host stopped server", false)
            }
        }
        clients.clear()
        host = null
        latestPlayback = null
        pendingRequests.clear()
        super.stop()
    }

    private inner class ClientSocket(request: NanoHTTPD.IHTTPSession) :
        WebSocket(request) {
        @Volatile var role: String? = null
        @Volatile private var lastRequestTimestampMs: Long = 0L

        override fun onOpen() {
            if (clients.size >= MAX_CLIENTS) {
                runCatching { close(WebSocketFrame.CloseCode.PolicyViolation, "Party is full", false) }
                return
            }
            clients.add(this)
        }

        override fun onMessage(message: WebSocketFrame) {
            val text = runCatching { message.textPayload }.getOrNull()
            if (text == null || text.length > MAX_PAYLOAD_CHARS) {
                close(WebSocketFrame.CloseCode.MessageTooBig, "Payload exceeds size limit", false)
                return
            }

            val json = runCatching { JSONObject(text) }.getOrNull() ?: run {
                close(WebSocketFrame.CloseCode.ProtocolError, "Invalid JSON payload", false)
                return
            }

            val messageType = json.optString("type")
            if (role == null && messageType != "authenticate") {
                close(WebSocketFrame.CloseCode.PolicyViolation, "Must authenticate before sending messages", false)
                return
            }

            when (messageType) {
                "authenticate" -> {
                    if (role != null) return
                    val token = json.optString("memberToken").trim()

                    if (token == hostToken) {
                        // Allow host reconnection from another socket or dropped connection
                        val prevHost = host
                        if (prevHost != null && prevHost !== this) {
                            runCatching {
                                prevHost.close(WebSocketFrame.CloseCode.NormalClosure, "Host reconnected from another session", false)
                            }
                            clients.remove(prevHost)
                        }
                        host = this
                        role = "host"
                    } else if (token == pairingCode) {
                        role = "guest"
                    } else {
                        close(WebSocketFrame.CloseCode.PolicyViolation, "Invalid party credentials", false)
                        return
                    }

                    // Send current playback state to the newly connected/reconnected member
                    latestPlayback?.let {
                        sendJson(
                            JSONObject()
                                .put("type", "party_state")
                                .put("song", it)
                                .put("members", clients.count { c -> c.role != null })
                                .put("serverTimeMs", System.currentTimeMillis())
                        )
                    }
                    broadcast(JSONObject().put("type", "member_count").put("members", clients.count { it.role != null }))
                }

                "playback" -> if (role == "host") {
                    val songObj = json.optJSONObject("song")
                    if (songObj != null) {
                        val source = songObj.optString("source").trim().lowercase()
                        if (source !in ALLOWED_SOURCES) {
                            sendJson(JSONObject().put("type", "error").put("message", "Only supported online songs can be shared."))
                            return
                        }
                        // Sanitize song payload: strictly control metadata only, no stream URLs or credentials
                        val sanitizedSong = JSONObject()
                            .put("songId", songObj.optString("songId").take(128))
                            .put("title", songObj.optString("title").take(128))
                            .put("artist", songObj.optString("artist").take(128))
                            .put("artworkUrl", songObj.optString("artworkUrl").take(512))
                            .put("source", songObj.optString("source").take(32))
                            .put("positionMs", songObj.optLong("positionMs", 0L).coerceAtLeast(0L))
                            .put("isPlaying", songObj.optBoolean("isPlaying", false))
                            .put("sentAtMs", System.currentTimeMillis())

                        latestPlayback = sanitizedSong
                        broadcast(
                            JSONObject()
                                .put("type", "party_state")
                                .put("song", sanitizedSong)
                                .put("members", clients.count { it.role != null })
                                .put("serverTimeMs", System.currentTimeMillis()),
                            except = this
                        )
                    }
                }

                "host_command" -> if (role == "host") {
                    val command = json.optString("command").trim().lowercase()
                    if (command in ALLOWED_HOST_COMMANDS) {
                        val payload = JSONObject().put("type", "host_command").put("command", command)
                        if (json.has("positionMs")) {
                            payload.put("positionMs", json.optLong("positionMs").coerceAtLeast(0L))
                        }
                        broadcast(payload, except = this)
                    }
                }

                "song_request" -> if (role == "guest") {
                    val now = System.currentTimeMillis()
                    if (now - lastRequestTimestampMs < RATE_LIMIT_REQUEST_WINDOW_MS) {
                        sendJson(JSONObject().put("type", "error").put("message", "Please wait a moment before requesting another song."))
                        return
                    }
                    lastRequestTimestampMs = now

                    if (pendingRequests.size >= MAX_PENDING_REQUESTS) {
                        sendJson(JSONObject().put("type", "error").put("message", "The song request queue is full. Try again shortly."))
                        return
                    }

                    val title = json.optString("title").trim().take(128)
                    val artist = json.optString("artist").trim().take(128)
                    val source = json.optString("source").trim().take(32)
                    if (title.isBlank()) {
                        sendJson(JSONObject().put("type", "error").put("message", "Song title cannot be blank."))
                        return
                    }

                    // Generate IDs on the host relay; never trust client-supplied IDs.
                    val requestId = randomId()
                    pendingRequests[requestId] = title
                    val sanitized = JSONObject()
                        .put("type", "song_request")
                        .put("requestId", requestId)
                        .put("title", title)
                        .put("artist", artist)
                        .put("source", source)
                        .put("requester", json.optString("requester").trim().take(32).ifBlank { "Guest" })

                    host?.sendJson(sanitized)
                }

                "song_request_response" -> if (role == "host") {
                    val requestId = json.optString("requestId").take(64)
                    val title = pendingRequests.remove(requestId) ?: return
                    val approved = json.optBoolean("approved")

                    // Broadcast decision so requesting guest sees feedback
                    broadcast(
                        JSONObject()
                            .put("type", "song_request_decision")
                            .put("requestId", requestId)
                            .put("title", title)
                            .put("approved", approved)
                    )

                    // Also broadcast removal so host and guests remove request from pending list
                    broadcast(
                        JSONObject()
                            .put("type", "song_request_removed")
                            .put("requestId", requestId)
                    )
                }

                "leave" -> close(WebSocketFrame.CloseCode.NormalClosure, "Member left", false)

                else -> if (role != null) {
                    sendJson(JSONObject().put("type", "error").put("message", "Unsupported or unauthorized message."))
                }
            }
        }

        override fun onClose(code: WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean) {
            clients.remove(this)
            val wasHost = (host === this)
            if (wasHost) {
                host = null
                // Notify guests that host disconnected
                broadcast(JSONObject().put("type", "error").put("message", "Party host disconnected."))
            }
            if (role != null) {
                broadcast(JSONObject().put("type", "member_count").put("members", clients.count { it.role != null }))
            }
        }

        override fun onPong(pong: WebSocketFrame) = Unit

        override fun onException(exception: java.io.IOException) {
            clients.remove(this)
            if (host === this) {
                host = null
                broadcast(JSONObject().put("type", "error").put("message", "Party host disconnected."))
            }
            if (role != null) {
                broadcast(JSONObject().put("type", "member_count").put("members", clients.count { it.role != null }))
            }
        }

        fun sendJson(json: JSONObject) {
            runCatching {
                if (isOpen) {
                    send(json.toString())
                }
            }
        }
    }

    private fun broadcast(json: JSONObject, except: ClientSocket? = null) {
        for (client in clients) {
            if (client.role != null && client !== except) {
                client.sendJson(json)
            }
        }
    }

    override fun serveHttp(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Party Mode WebSocket only")

    companion object {
        const val DEFAULT_PORT = 8765
        const val MAX_CLIENTS = 32
        const val MAX_PENDING_REQUESTS = 50
        const val MAX_PAYLOAD_CHARS = 32768
        const val RATE_LIMIT_REQUEST_WINDOW_MS = 1200L
        private val ALLOWED_SOURCES = setOf("youtube", "youtube_music", "youtubemusic", "youtube music", "jiosaavn", "jio saavn", "jiosaavn 320k")
        private val ALLOWED_HOST_COMMANDS = setOf("play", "pause", "next", "previous", "seek")

        internal fun scoreInterface(name: String): Int {
            val n = name.lowercase()
            return when {
                n.startsWith("wlan") || n.startsWith("wifi") -> 100
                n.startsWith("ap") || n.contains("softap") || n.startsWith("swlan") -> 90
                n.contains("p2p") -> 70
                n.startsWith("eth") -> 50
                n.startsWith("rndis") || n.startsWith("usb") -> 40
                n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("pdp") -> -50
                n.startsWith("tun") || n.startsWith("tap") || n.startsWith("ppp") -> -100
                else -> 10
            }
        }

        internal fun scoreAddress(addr: Inet4Address, ifaceName: String): Int {
            val host = addr.hostAddress ?: return -10000
            if (addr.isLoopbackAddress || host.startsWith("127.") || host.startsWith("169.254.")) {
                return -10000
            }
            val ifScore = scoreInterface(ifaceName)
            var score = ifScore * 10
            if (addr.isSiteLocalAddress) score += 50
            if (host == "192.168.43.1") score += 30 // Typical Android hotspot IP
            return score
        }

        internal fun selectBestLocalIpv4(interfaces: List<NetworkInterface>): String? {
            return interfaces
                .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
                .flatMap { iface ->
                    iface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .map { addr -> addr to scoreAddress(addr, iface.name) }
                }
                .filter { it.second > -500 }
                .maxByOrNull { it.second }
                ?.first?.hostAddress
        }

        fun localIpv4Address(): String? = runCatching {
            val ifaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            selectBestLocalIpv4(ifaces)
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
