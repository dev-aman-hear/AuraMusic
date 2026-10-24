package com.aman.auramusic.party

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class LocalPartyServerTest {

    private var server: LocalPartyServer? = null
    private var port: Int = 0
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val pairingCode = "ABC123"
    private val hostToken = "secret-host-token-xyz"

    @Before
    fun setUp() {
        val ss = ServerSocket(0)
        port = ss.localPort
        ss.close()

        server = LocalPartyServer(pairingCode, hostToken, port).apply {
            start()
        }
    }

    @After
    fun tearDown() {
        server?.stop()
        server = null
    }

    private class TestSocketListener : WebSocketListener() {
        val messages: BlockingQueue<String> = LinkedBlockingQueue()
        val closeEvents: BlockingQueue<Pair<Int, String>> = LinkedBlockingQueue()
        val failureEvents: BlockingQueue<Throwable> = LinkedBlockingQueue()
        @Volatile var socket: WebSocket? = null

        override fun onOpen(webSocket: WebSocket, response: Response) {
            socket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            messages.offer(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            closeEvents.offer(code to reason)
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            closeEvents.offer(code to reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            failureEvents.offer(t)
        }

        fun awaitMessage(timeoutMs: Long = 3000): JSONObject? {
            val raw = messages.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
            return JSONObject(raw)
        }
    }

    private fun connectClient(): Pair<WebSocket, TestSocketListener> {
        val listener = TestSocketListener()
        val req = Request.Builder().url("ws://127.0.0.1:$port").build()
        val ws = client.newWebSocket(req, listener)
        return ws to listener
    }

    @Test
    fun testHostStartupAndAuthentication() {
        val (ws, listener) = connectClient()
        ws.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())

        val msg = listener.awaitMessage()
        assertNotNull("Host should receive a message upon authentication", msg)
        assertEquals("member_count", msg?.getString("type"))
        assertEquals(1, msg?.getInt("members"))

        ws.close(1000, "Done")
    }

    @Test
    fun testGuestAuthenticationAndRejectionOfInvalidToken() {
        val (badWs, badListener) = connectClient()
        badWs.send(JSONObject().put("type", "authenticate").put("memberToken", "WRONG_CODE").toString())

        val closeEvent = badListener.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: badListener.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1008 to "Failed" }
        assertNotNull("Bad credential socket should be closed", closeEvent)

        // Valid guest authentication
        val (goodWs, goodListener) = connectClient()
        goodWs.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())

        val msg = goodListener.awaitMessage()
        assertNotNull("Guest should receive member_count", msg)
        assertEquals("member_count", msg?.getString("type"))

        goodWs.close(1000, "Done")
    }

    @Test
    fun testHostReconnectionTakesOverExistingHost() {
        // Connect first host
        val (host1, listener1) = connectClient()
        host1.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        assertNotNull(listener1.awaitMessage())

        // Connect second host with same host token
        val (host2, listener2) = connectClient()
        host2.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        val host2Msg = listener2.awaitMessage()
        assertNotNull("Second host connection should succeed", host2Msg)

        // First host socket should be disconnected
        val close1 = listener1.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: listener1.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1000 to "Closed" }
        assertNotNull("First host socket should be closed upon host reconnection", close1)

        host2.close(1000, "Done")
    }

    @Test
    fun testPlaybackSynchronizationFromHostToGuest() {
        val (host, hostListener) = connectClient()
        host.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        hostListener.awaitMessage() // member_count

        val (guest, guestListener) = connectClient()
        guest.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())
        guestListener.awaitMessage() // member_count
        hostListener.awaitMessage() // member_count update

        // Host sends playback update
        val songObj = JSONObject()
            .put("songId", "song_42")
            .put("title", "Midnight Aura")
            .put("artist", "SoundScape")
            .put("source", "YOUTUBE")
            .put("positionMs", 12345L)
            .put("isPlaying", true)
        host.send(JSONObject().put("type", "playback").put("song", songObj).toString())

        // Guest receives party_state
        val partyState = guestListener.awaitMessage()
        assertNotNull("Guest should receive synchronized party state", partyState)
        assertEquals("party_state", partyState?.getString("type"))
        val song = partyState?.getJSONObject("song")
        assertEquals("Midnight Aura", song?.getString("title"))
        assertEquals("SoundScape", song?.getString("artist"))
        assertEquals(12345L, song?.getLong("positionMs"))
        assertTrue(song?.getBoolean("isPlaying") == true)

        // New guest joining receives current playback state immediately
        val (guest2, guest2Listener) = connectClient()
        guest2.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())
        val initialGuest2State = guest2Listener.awaitMessage()
        assertNotNull(initialGuest2State)
        assertEquals("party_state", initialGuest2State?.getString("type"))
        assertEquals("Midnight Aura", initialGuest2State?.getJSONObject("song")?.getString("title"))

        host.close(1000, "Done")
        guest.close(1000, "Done")
        guest2.close(1000, "Done")
    }

    @Test
    fun testSongRequestAndHostDecisionRelay() {
        val (host, hostListener) = connectClient()
        host.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        hostListener.awaitMessage()

        val (guest, guestListener) = connectClient()
        guest.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())
        guestListener.awaitMessage()
        hostListener.awaitMessage()

        // Guest sends song request
        guest.send(JSONObject()
            .put("type", "song_request")
            .put("title", "Starlight")
            .put("artist", "Cosmo")
            .put("source", "JIOSAAVN")
            .toString())

        // Host receives song request
        val hostReq = hostListener.awaitMessage()
        assertNotNull("Host should receive guest song request", hostReq)
        assertEquals("song_request", hostReq?.getString("type"))
        assertEquals("Starlight", hostReq?.getString("title"))
        val reqId = hostReq?.getString("requestId")
        assertNotNull(reqId)

        // Host approves request
        host.send(JSONObject()
            .put("type", "song_request_response")
            .put("requestId", reqId)
            .put("approved", true)
            .toString())

        // Guest receives decision and removal
        val decision = guestListener.awaitMessage()
        assertNotNull("Guest should receive host decision", decision)
        assertEquals("song_request_decision", decision?.getString("type"))
        assertTrue(decision?.getBoolean("approved") == true)
        assertEquals("Starlight", decision?.getString("title"))

        val removal = guestListener.awaitMessage()
        assertNotNull("Clients should receive song request removal", removal)
        assertEquals("song_request_removed", removal?.getString("type"))

        host.close(1000, "Done")
        guest.close(1000, "Done")
    }

    @Test
    fun testGuestRequestAbuseAndRateLimiting() {
        val (host, hostListener) = connectClient()
        host.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        hostListener.awaitMessage()

        val (guest, guestListener) = connectClient()
        guest.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())
        guestListener.awaitMessage()

        // Blank title rejection
        guest.send(JSONObject()
            .put("type", "song_request")
            .put("title", "   ")
            .put("artist", "Artist")
            .put("source", "YOUTUBE")
            .toString())

        val errBlank = guestListener.awaitMessage()
        assertNotNull(errBlank)
        assertEquals("error", errBlank?.getString("type"))
        assertTrue(errBlank?.getString("message")?.contains("cannot be blank") == true)

        // Send first valid request
        guest.send(JSONObject()
            .put("type", "song_request")
            .put("title", "Song 1")
            .put("artist", "Artist")
            .put("source", "YOUTUBE")
            .toString())

        // Send second request immediately (< 1.2s)
        guest.send(JSONObject()
            .put("type", "song_request")
            .put("title", "Song 2")
            .put("artist", "Artist")
            .put("source", "YOUTUBE")
            .toString())

        val rateLimitErr = guestListener.awaitMessage()
        assertNotNull("Spamming requests should be rate limited", rateLimitErr)
        assertEquals("error", rateLimitErr?.getString("type"))
        assertTrue(rateLimitErr?.getString("message")?.contains("wait a moment") == true)

        host.close(1000, "Done")
        guest.close(1000, "Done")
    }

    @Test
    fun testMalformedMessagesAndPreAuthMessageRejection() {
        val (ws, listener) = connectClient()

        // Message before authenticate
        ws.send(JSONObject().put("type", "playback").toString())
        val closePreAuth = listener.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: listener.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1008 to "Closed" }
        assertNotNull("Unauthenticated message should close connection", closePreAuth)

        // Malformed JSON
        val (ws2, listener2) = connectClient()
        ws2.send("NOT_A_JSON_STRING")
        val closeMalformed = listener2.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: listener2.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1002 to "Closed" }
        assertNotNull("Malformed JSON should close connection", closeMalformed)
    }

    @Test
    fun testServerShutdownClosesAllClients() {
        val (host, hostListener) = connectClient()
        host.send(JSONObject().put("type", "authenticate").put("memberToken", hostToken).toString())
        hostListener.awaitMessage()

        val (guest, guestListener) = connectClient()
        guest.send(JSONObject().put("type", "authenticate").put("memberToken", pairingCode).toString())
        guestListener.awaitMessage()

        // Stop server
        server?.stop()

        val hostClose = hostListener.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: hostListener.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1000 to "Closed" }
        val guestClose = guestListener.closeEvents.poll(3000, TimeUnit.MILLISECONDS)
            ?: guestListener.failureEvents.poll(1000, TimeUnit.MILLISECONDS)?.let { 1000 to "Closed" }

        assertNotNull("Host socket must be closed on server shutdown", hostClose)
        assertNotNull("Guest socket must be closed on server shutdown", guestClose)
    }
}
