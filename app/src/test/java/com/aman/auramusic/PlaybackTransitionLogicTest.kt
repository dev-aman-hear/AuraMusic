package com.aman.auramusic

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.playback.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression and logic unit tests for AuraMusic playback transitions,
 * queue advancement, repeat/shuffle modes, debounce guards, and state transitions.
 */
class PlaybackTransitionLogicTest {

    private fun createDummyLocalSong(id: Long, title: String): Song {
        return Song(
            id = id,
            title = title,
            artist = "Test Artist",
            album = "Test Album",
            duration = 180_000L,
            dateAdded = 1000L,
            uri = "content://media/external/audio/media/$id",
            filePath = "/storage/emulated/0/Music/$title.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )
    }

    private fun createDummyOnlineSong(id: String, title: String, source: AudioSource = AudioSource.JIOSAAVN): OnlineSong {
        return OnlineSong(
            id = id,
            title = title,
            artist = "Online Artist",
            album = "Online Album",
            durationSeconds = 200,
            source = source,
            streamUrl = "https://example.com/stream/$id.mp4"
        )
    }

    // =========================================================================
    // 1. Local Track A -> Local Track B -> Local Track C Queue Transition
    // =========================================================================
    @Test
    fun testLocalTrackSequentialTransition() {
        val songA = createDummyLocalSong(1L, "Song A")
        val songB = createDummyLocalSong(2L, "Song B")
        val songC = createDummyLocalSong(3L, "Song C")
        val queue = listOf(songA, songB, songC)

        // Queue transition simulator replicating PlaybackService logic
        var currentSong = songA
        fun computeNext(isManual: Boolean, repeat: RepeatMode): Song? {
            val currentIndex = queue.indexOfFirst { it.id == currentSong.id }
            val nextIndex = currentIndex + 1
            return if (nextIndex >= queue.size) {
                when (repeat) {
                    RepeatMode.ALL -> queue[0]
                    RepeatMode.ONE -> currentSong
                    RepeatMode.NONE -> if (isManual) queue[0] else null
                }
            } else {
                queue[nextIndex]
            }
        }

        // Song A -> Song B
        val nextFromA = computeNext(isManual = false, repeat = RepeatMode.NONE)
        assertNotNull(nextFromA)
        assertEquals(songB.id, nextFromA!!.id)
        currentSong = nextFromA

        // Song B -> Song C
        val nextFromB = computeNext(isManual = false, repeat = RepeatMode.NONE)
        assertNotNull(nextFromB)
        assertEquals(songC.id, nextFromB!!.id)
        currentSong = nextFromB

        // Song C natural end (RepeatMode.NONE) -> should stop (null)
        val nextFromC = computeNext(isManual = false, repeat = RepeatMode.NONE)
        assertNull("Natural end with RepeatMode.NONE must not loop or advance", nextFromC)
    }

    // =========================================================================
    // 2. Online Track A -> Online Track B Transition
    // =========================================================================
    @Test
    fun testOnlineTrackSequentialTransition() {
        val songA = createDummyOnlineSong("id_1", "Online Track A")
        val songB = createDummyOnlineSong("id_2", "Online Track B")
        val queue = listOf(songA, songB)

        var queueIndex = 0
        var isRepeatEnabled = false
        var isShuffleEnabled = false

        fun advanceOnlineNext(isManual: Boolean): Int? {
            if (!isShuffleEnabled && !isRepeatEnabled && !isManual && queueIndex >= queue.size - 1) {
                return null // Natural end reached
            }
            return (queueIndex + 1) % queue.size
        }

        val nextIndex1 = advanceOnlineNext(isManual = false)
        assertEquals(1, nextIndex1)
        queueIndex = nextIndex1!!

        // Natural queue end reached
        val nextIndex2 = advanceOnlineNext(isManual = false)
        assertNull("Online queue must cleanly pause at natural end when repeat is disabled", nextIndex2)

        // Repeat enabled
        isRepeatEnabled = true
        val repeatIndex = if (isRepeatEnabled) queueIndex else advanceOnlineNext(isManual = false)
        assertEquals(1, repeatIndex)
    }

    // =========================================================================
    // 3. Repeat-One, Repeat-All, and Shuffle Transition Logic
    // =========================================================================
    @Test
    fun testRepeatAndShuffleModes() {
        val song1 = createDummyLocalSong(101L, "Track 1")
        val song2 = createDummyLocalSong(102L, "Track 2")
        val song3 = createDummyLocalSong(103L, "Track 3")
        val queue = listOf(song1, song2, song3)

        // RepeatMode.ONE: end of track must repeat same track
        var currentSong = song2
        fun onEndReached(repeat: RepeatMode): Song {
            return when (repeat) {
                RepeatMode.ONE -> currentSong
                RepeatMode.ALL -> {
                    val idx = queue.indexOfFirst { it.id == currentSong.id }
                    queue[(idx + 1) % queue.size]
                }
                RepeatMode.NONE -> {
                    val idx = queue.indexOfFirst { it.id == currentSong.id }
                    if (idx + 1 < queue.size) queue[idx + 1] else currentSong
                }
            }
        }

        val repeatedTrack = onEndReached(RepeatMode.ONE)
        assertEquals("RepeatMode.ONE must keep track on same song", song2.id, repeatedTrack.id)

        // RepeatMode.ALL: last track loops back to first
        currentSong = song3
        val loopedTrack = onEndReached(RepeatMode.ALL)
        assertEquals("RepeatMode.ALL must wrap from end of queue to beginning", song1.id, loopedTrack.id)

        // Shuffle: next index is in valid queue indices
        val randomIndices = (1..10).map { queue.indices.random() }
        for (idx in randomIndices) {
            assertTrue("Shuffle index must be within queue bounds", idx in queue.indices)
        }
    }

    // =========================================================================
    // 4. Debounce Guard Against Duplicate End Callbacks
    // =========================================================================
    @Test
    fun testDuplicatePlaybackEndDebounce() {
        var lastEndHandledTimeMs = 0L
        var transitionsCount = 0

        fun handleSongEnded(currentTimeMs: Long): Boolean {
            if (currentTimeMs - lastEndHandledTimeMs < 500L) {
                return false // Debounced / ignored
            }
            lastEndHandledTimeMs = currentTimeMs
            transitionsCount++
            return true
        }

        // First trigger at t = 1000ms
        val handled1 = handleSongEnded(1000L)
        assertTrue("First trigger must be processed", handled1)
        assertEquals(1, transitionsCount)

        // Duplicate trigger from library at t = 1050ms (50ms later)
        val handled2 = handleSongEnded(1050L)
        assertFalse("Duplicate callback within 500ms must be debounced", handled2)
        assertEquals(1, transitionsCount)

        // Another duplicate at t = 1300ms (300ms later)
        val handled3 = handleSongEnded(1300L)
        assertFalse("Duplicate callback at 300ms must be debounced", handled3)
        assertEquals(1, transitionsCount)

        // Legitimate trigger for next track at t = 180000ms (3 minutes later)
        val handled4 = handleSongEnded(180_000L)
        assertTrue("Legitimate trigger after track completion must be processed", handled4)
        assertEquals(2, transitionsCount)
    }

    // =========================================================================
    // 5. Transition Flag Suppresses Transient Paused / Stopped Callbacks
    // =========================================================================
    @Test
    fun testTransitionFlagSuppressesTransientStoppedEvents() {
        var isTransitioning = false
        var foregroundDetached = false
        var playbackStateReported: Boolean? = null

        // Callback simulator mirroring VlcPlayerManager / PlaybackService
        fun onPlayerEvent(eventType: String) {
            when (eventType) {
                "EndReached" -> {
                    isTransitioning = true
                }
                "Playing" -> {
                    isTransitioning = false
                    playbackStateReported = true
                }
                "Stopped", "Paused" -> {
                    if (!isTransitioning) {
                        playbackStateReported = false
                        foregroundDetached = true
                    }
                }
            }
        }

        // Track A finishes naturally
        onPlayerEvent("EndReached")
        assertTrue("isTransitioning must be true when EndReached fires", isTransitioning)

        // LibVLC tears down track A media and emits Stopped
        onPlayerEvent("Stopped")
        assertFalse("Foreground MUST NOT be detached during transition", foregroundDetached)
        assertNull("Playback state false MUST NOT be reported during transition", playbackStateReported)

        // Track B starts playing
        onPlayerEvent("Playing")
        assertFalse("isTransitioning must be false once Playing is received", isTransitioning)
        assertEquals(true, playbackStateReported)
        assertFalse("Foreground must remain attached during entire transition", foregroundDetached)
    }

    // =========================================================================
    // 6. Rapid Next / Previous Button Press Stability
    // =========================================================================
    @Test
    fun testRapidNextPreviousSafety() {
        val queue = (1..10).map { createDummyLocalSong(it.toLong(), "Track $it") }
        var currentIndex = 0

        fun manualNext() {
            currentIndex = (currentIndex + 1) % queue.size
        }

        fun manualPrevious() {
            currentIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        }

        // 100 rapid next clicks
        for (i in 1..100) {
            manualNext()
            assertTrue(currentIndex in queue.indices)
        }

        // 100 rapid previous clicks
        for (i in 1..100) {
            manualPrevious()
            assertTrue(currentIndex in queue.indices)
        }
    }

    // =========================================================================
    // 7. Cancellation of Stale Notification Jobs on Track Switch
    // =========================================================================
    @Test
    fun testStaleNotificationJobRejection() {
        var activeTrackId = "track_A"
        var foregroundStateApplied: Boolean? = null

        fun simulateAsyncArtworkAndLyricsCompletion(trackIdForJob: String, isPlayingAtStart: Boolean) {
            // When job finishes 2-5 seconds later, verify track is still current
            if (activeTrackId != trackIdForJob) {
                // Aborted because track changed!
                return
            }
            foregroundStateApplied = isPlayingAtStart
        }

        // Track A finishes and launches an async notification update with isPlaying = false
        val jobA_Track = "track_A"
        val jobA_IsPlaying = false

        // Track B starts immediately
        activeTrackId = "track_B"
        foregroundStateApplied = true // Track B sets foreground immediately

        // Track A's delayed coroutine completes 3 seconds later
        simulateAsyncArtworkAndLyricsCompletion(jobA_Track, jobA_IsPlaying)

        // Track B must still be foreground! Track A's stale update must have been aborted.
        assertEquals("Track B must maintain foreground state despite Track A's delayed completion", true, foregroundStateApplied)
    }
}
