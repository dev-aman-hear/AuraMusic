package com.aman.auramusic.party

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PartyModeSafetyTest {

    private val repo = PartyModeRepository()

    @Test
    fun testInternetPartyModeRequiresWss() {
        val validInternetJson = JSONObject()
            .put("partyId", "party-123")
            .put("pairingCode", "INT123")
            .put("memberToken", "token-abc")
            .put("websocketUrl", "wss://party.example.com/ws")

        val session = repo.parseSession(validInternetJson, PartyRole.HOST)
        assertEquals("party-123", session.partyId)
        assertEquals(PartyConnectionMode.INTERNET, session.connectionMode)
        assertTrue(session.websocketUrl.startsWith("wss://"))

        // Reject clear-text ws:// for Internet mode
        val insecureInternetJson = JSONObject()
            .put("partyId", "party-456")
            .put("pairingCode", "INT456")
            .put("memberToken", "token-def")
            .put("websocketUrl", "ws://party.example.com/ws")

        try {
            repo.parseSession(insecureInternetJson, PartyRole.GUEST)
            fail("Insecure ws:// must be rejected for Internet Party Mode")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("secure WSS") == true)
        }
    }

    @Test
    fun testNeverTransmitsAudioOrStreamUrlsOrCookies() {
        val sensitiveKeys = listOf("streamUrl", "cookie", "token", "password", "audio", "bytes", "authorization")

        // 1. Playback metadata
        val playback = PartyPlayback(
            songId = "song1",
            title = "Test Song",
            artist = "Test Artist",
            artworkUrl = "https://img.example.com/art.jpg",
            source = "YOUTUBE",
            positionMs = 1000L,
            isPlaying = true
        )
        // Check properties
        assertFalse("PartyPlayback must not have streamUrl", PartyPlayback::class.java.declaredFields.any { it.name == "streamUrl" })
        assertFalse("PartyPlayback must not have cookies", PartyPlayback::class.java.declaredFields.any { it.name == "cookie" })
        assertFalse("PartyPlayback must not have audioBytes", PartyPlayback::class.java.declaredFields.any { it.name.contains("audio", true) })

        // 2. Song requests
        val request = PartySongRequest(
            requestId = "req-1",
            title = "Requested Song",
            artist = "Requested Artist",
            source = "JIOSAAVN",
            requester = "Guest"
        )
        assertFalse("PartySongRequest must not have streamUrl", PartySongRequest::class.java.declaredFields.any { it.name == "streamUrl" })
        assertFalse("PartySongRequest must not have cookies", PartySongRequest::class.java.declaredFields.any { it.name == "cookie" })
    }

    @Test
    fun testHandleServerMessagesUpdatesStateCleanly() {
        // 1. party_state message
        val stateMsg = JSONObject()
            .put("type", "party_state")
            .put("members", 4)
            .put("serverTimeMs", 100000L)
            .put("song", JSONObject()
                .put("songId", "song_abc")
                .put("title", "Synchronized Song")
                .put("artist", "Sync Artist")
                .put("artworkUrl", "https://example.com/art.png")
                .put("source", "YOUTUBE")
                .put("positionMs", 5000L)
                .put("isPlaying", true))
        repo.handleServerMessage(stateMsg.toString())

        assertEquals(4, repo.state.value.members)
        assertNotNull(repo.state.value.playback)
        assertEquals("Synchronized Song", repo.state.value.playback?.title)
        assertEquals(5000L, repo.state.value.playback?.positionMs)
        assertTrue(repo.state.value.playback?.isPlaying == true)

        // 2. song_request message
        val reqMsg = JSONObject()
            .put("type", "song_request")
            .put("requestId", "req-99")
            .put("title", "Song 99")
            .put("artist", "Artist 99")
            .put("source", "YOUTUBE")
            .put("requester", "Guest 1")
        repo.handleServerMessage(reqMsg.toString())
        assertEquals(1, repo.state.value.requests.size)
        assertEquals("req-99", repo.state.value.requests[0].requestId)

        // 3. song_request_decision message
        val decisionMsg = JSONObject()
            .put("type", "song_request_decision")
            .put("requestId", "req-99")
            .put("title", "Song 99")
            .put("approved", true)
        repo.handleServerMessage(decisionMsg.toString())
        assertTrue(repo.state.value.decisionMessage?.contains("Host approved") == true)

        // 4. song_request_removed message
        val removeMsg = JSONObject()
            .put("type", "song_request_removed")
            .put("requestId", "req-99")
        repo.handleServerMessage(removeMsg.toString())
        assertEquals(0, repo.state.value.requests.size)

        // 5. member_count message
        val memberMsg = JSONObject()
            .put("type", "member_count")
            .put("members", 7)
        repo.handleServerMessage(memberMsg.toString())
        assertEquals(7, repo.state.value.members)

        // 6. error message
        val errMsg = JSONObject()
            .put("type", "error")
            .put("message", "Something went wrong")
        repo.handleServerMessage(errMsg.toString())
        assertEquals("Something went wrong", repo.state.value.error)
    }

    @Test
    fun partyPlaybackSyncPolicyNormalizesProvidersAndCompensatesPosition() {
        assertEquals("YOUTUBE", PartyPlaybackSyncPolicy.normalizeSource("YouTube Music"))
        assertEquals("YOUTUBE", PartyPlaybackSyncPolicy.normalizeSource("YOUTUBE"))
        assertEquals("JIOSAAVN", PartyPlaybackSyncPolicy.normalizeSource("JioSaavn"))
        assertNull(PartyPlaybackSyncPolicy.normalizeSource("unknown-provider"))

        assertTrue(
            PartyPlaybackSyncPolicy.sameTrack(
                currentSource = "YOUTUBE",
                currentId = "track-1",
                currentTitle = "A Song",
                currentArtist = "An Artist",
                remoteSource = "YOUTUBE",
                remoteId = "track-1",
                remoteTitle = "Different display title",
                remoteArtist = "Different display artist"
            )
        )
        assertFalse(
            PartyPlaybackSyncPolicy.sameTrack(
                currentSource = "YOUTUBE",
                currentId = "track-1",
                currentTitle = "A Song",
                currentArtist = "An Artist",
                remoteSource = "JIOSAAVN",
                remoteId = "track-1",
                remoteTitle = "A Song",
                remoteArtist = "An Artist"
            )
        )

        assertEquals(7_500L, PartyPlaybackSyncPolicy.targetPosition(5_000L, true, 2_500L))
        assertEquals(5_000L, PartyPlaybackSyncPolicy.targetPosition(5_000L, false, 2_500L))
        assertEquals(0L, PartyPlaybackSyncPolicy.targetPosition(-100L, false, -1L))
    }

    @Test
    fun partyPlaybackResolveKeyIsStableAndIncludesProvider() {
        assertEquals(
            PartyPlaybackSyncPolicy.resolveKey("YOUTUBE", "id-1", "Title", "Artist"),
            PartyPlaybackSyncPolicy.resolveKey("YOUTUBE", "id-1", "Other Title", "Other Artist")
        )
        assertTrue(
            PartyPlaybackSyncPolicy.resolveKey("YOUTUBE", "id-1", "Title", "Artist") !=
                PartyPlaybackSyncPolicy.resolveKey("JIOSAAVN", "id-1", "Title", "Artist")
        )
        assertEquals(
            PartyPlaybackSyncPolicy.resolveKey("YOUTUBE", "", "Title", "Artist"),
            PartyPlaybackSyncPolicy.resolveKey("YOUTUBE", "", " title ", " artist ")
        )
    }

}
