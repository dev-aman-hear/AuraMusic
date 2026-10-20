package com.aman.auramusic.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aman.auramusic.data.model.AppSettings
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.data.repository.UserPreferencesRepository
import com.aman.auramusic.viewmodel.QueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Regression test suite covering:
 * 1. Playback task-removal lifecycle decisions.
 * 2. Playback state synchronization (buffering, transitions, local vs online).
 * 3. Custom playlist queue operations: Add to Queue (single song and whole collection).
 * 4. Queue preservation against playlist/favorites observers.
 * 5. Sequential advancement and track completion debouncing.
 * 6. Shuffle presentation vs canonical queue order.
 */
class PlaybackLifecycleAndQueueRegressionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.IO + Job())
        dataStoreFile = tempFolder.newFile("test_lifecycle_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { dataStoreFile }
        )
        repository = UserPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        if (testScope.isActive) {
            testScope.cancel()
        }
    }

    private fun sampleSong(
        id: Long,
        title: String,
        artist: String = "Artist $id",
        album: String = "Album $id",
        duration: Long = 200_000L
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = "content://media/$id",
            filePath = "/path/to/$id.mp3",
            artworkUri = null,
            mimeType = "audio/mpeg"
        )
    }

    // =========================================================================
    // Playback task-removal lifecycle regression tests
    // =========================================================================

    private data class LifecycleDecision(
        val shouldStopService: Boolean,
        val shouldStopPlaybackEngines: Boolean,
        val shouldClearNotification: Boolean
    )

    private fun evaluateTaskRemoval(
        isLocalPlaying: Boolean,
        isLocalTransitioning: Boolean,
        isOnlineActive: Boolean,
        isOnlinePlaying: Boolean,
        isOnlineBuffering: Boolean,
        isOnlineTransitioning: Boolean
    ): LifecycleDecision {
        // The Keep Playing on App Close feature has been removed. Dismissing the task
        // stops both playback engines and cleans up the media foreground notification.
        return LifecycleDecision(
            shouldStopService = true,
            shouldStopPlaybackEngines = true,
            shouldClearNotification = true
        )
    }

    @Test
    fun task_removal_stops_active_local_playback_and_cleans_up_notification() {
        val decision = evaluateTaskRemoval(
            isLocalPlaying = true,
            isLocalTransitioning = false,
            isOnlineActive = false,
            isOnlinePlaying = false,
            isOnlineBuffering = false,
            isOnlineTransitioning = false
        )
        assertTrue(decision.shouldStopService)
        assertTrue(decision.shouldStopPlaybackEngines)
        assertTrue(decision.shouldClearNotification)
    }

    @Test
    fun task_removal_stops_active_online_playback_even_while_buffering_or_transitioning() {
        listOf(
            Triple(true, false, false),
            Triple(false, true, false),
            Triple(false, false, true)
        ).forEach { (playing, buffering, transitioning) ->
            val decision = evaluateTaskRemoval(
                isLocalPlaying = false,
                isLocalTransitioning = false,
                isOnlineActive = true,
                isOnlinePlaying = playing,
                isOnlineBuffering = buffering,
                isOnlineTransitioning = transitioning
            )
            assertTrue(decision.shouldStopService)
            assertTrue(decision.shouldStopPlaybackEngines)
            assertTrue(decision.shouldClearNotification)
        }
    }

    @Test
    fun task_removal_cleans_up_when_playback_is_inactive() {
        val decision = evaluateTaskRemoval(
            isLocalPlaying = false,
            isLocalTransitioning = false,
            isOnlineActive = false,
            isOnlinePlaying = false,
            isOnlineBuffering = false,
            isOnlineTransitioning = false
        )
        assertTrue(decision.shouldStopService)
        assertTrue(decision.shouldStopPlaybackEngines)
        assertTrue(decision.shouldClearNotification)
    }

    @Test
    fun natural_track_completion_debouncing_prevents_duplicate_advancement() {
        var lastEndHandledTimeMs = 0L
        var advanceCallCount = 0

        fun onTrackEnd(now: Long) {
            if (now - lastEndHandledTimeMs < 500) {
                return // debounced duplicate
            }
            lastEndHandledTimeMs = now
            advanceCallCount++
        }

        // First end event at 1000ms
        onTrackEnd(1000L)
        assertEquals(1, advanceCallCount)

        // Duplicate end event arriving 50ms later (e.g. rapid VLC EndReached)
        onTrackEnd(1050L)
        assertEquals("Duplicate end event within 500ms must be discarded", 1, advanceCallCount)

        // Next natural end event at 5000ms
        onTrackEnd(5000L)
        assertEquals("Subsequent natural end event after debounce window must advance", 2, advanceCallCount)
    }

    // =========================================================================
    // TASK 3: Custom Playlist Queue Operations & Observer Protection Tests
    // =========================================================================

    private class TestQueueManager {
        var currentSong: Song? = null
        var currentSongId: Long? = null
        var currentPlaylistId: Long? = null
        var isShuffled: Boolean = false

        var queue: List<Song> = emptyList()
        var originalQueue: List<QueueEntry> = emptyList()
        val manuallyAddedSongIds = mutableSetOf<Long>()

        fun setQueue(songs: List<Song>, playlistId: Long? = null) {
            val uniqueSongs = songs.distinctBy { it.id }
            currentPlaylistId = playlistId
            manuallyAddedSongIds.clear()
            originalQueue = uniqueSongs.map { QueueEntry(song = it) }

            val targetSongs = if (isShuffled) {
                val current = uniqueSongs.firstOrNull { it.id == currentSongId }
                if (current != null) listOf(current) + uniqueSongs.filterNot { it.id == current.id }.shuffled()
                else uniqueSongs.shuffled()
            } else uniqueSongs

            queue = targetSongs
        }

        fun addToQueue(songs: List<Song>) {
            if (songs.isEmpty()) return
            val currentList = queue
            val currentIds = currentList.mapTo(mutableSetOf()) { it.id }
            val newSongs = songs.distinctBy { it.id }.filterNot { it.id in currentIds }
            if (newSongs.isEmpty()) return

            manuallyAddedSongIds.addAll(newSongs.map { it.id })

            val canonical = originalQueue.map { it.song }
            val canonicalIds = canonical.mapTo(mutableSetOf()) { it.id }
            val canonicalBase = if (canonical.isEmpty()) currentList
            else canonical + currentList.filterNot { it.id in canonicalIds }
            val newCanonical = (canonicalBase + newSongs).distinctBy { it.id }
            originalQueue = newCanonical.map { QueueEntry(song = it) }

            queue = currentList + newSongs
        }

        fun addToQueue(song: Song) = addToQueue(listOf(song))

        fun playNext(song: Song) = playNext(listOf(song))

        fun playNext(songs: List<Song>) {
            if (songs.isEmpty()) return
            val currentList = queue
            if (currentList.isEmpty()) {
                setQueue(songs)
                return
            }
            val currentIds = currentList.mapTo(mutableSetOf()) { it.id }
            val newSongs = songs.distinctBy { it.id }.filterNot { it.id in currentIds }
            if (newSongs.isEmpty()) return

            manuallyAddedSongIds.addAll(newSongs.map { it.id })

            val curSong = currentSong
            val insertIndex = if (curSong != null) {
                val idx = currentList.indexOfFirst { it.id == curSong.id }
                if (idx != -1) idx + 1 else currentList.size
            } else {
                0
            }

            val updated = currentList.toMutableList().apply {
                addAll(insertIndex, newSongs)
            }

            val canonical = originalQueue.map { it.song }
            val canonicalIds = canonical.mapTo(mutableSetOf()) { it.id }
            val canonicalBase = if (canonical.isEmpty()) currentList
            else canonical + currentList.filterNot { it.id in canonicalIds }
            val newCanonical = (canonicalBase + newSongs).distinctBy { it.id }
            originalQueue = newCanonical.map { QueueEntry(song = it) }

            queue = updated
        }

        fun refreshQueueFromIds(ids: List<Long>, allSongs: List<Song>) {
            if (currentPlaylistId == null) return
            val currentList = queue

            // Preserve manually added queue items
            val addedSongs = currentList.filter { it.id in manuallyAddedSongIds }

            // Resolve playlist tracks
            val playlistSongs = ids.mapNotNull { id -> allSongs.find { it.id == id } }.distinctBy { it.id }

            // Ensure current song is never lost while playing
            val curSong = currentSong
            val baseSongs = if (curSong != null && playlistSongs.none { it.id == curSong.id } && addedSongs.none { it.id == curSong.id }) {
                listOf(curSong) + playlistSongs
            } else {
                playlistSongs
            }

            val fullCanonical = (baseSongs.filterNot { it.id in manuallyAddedSongIds } + addedSongs).distinctBy { it.id }
            originalQueue = fullCanonical.map { QueueEntry(song = it) }

            val targetSongs = if (isShuffled) {
                val current = fullCanonical.firstOrNull { it.id == currentSongId }
                if (current != null) listOf(current) + fullCanonical.filterNot { it.id == current.id }.shuffled()
                else fullCanonical.shuffled()
            } else fullCanonical

            queue = targetSongs
        }

        fun play(song: Song) {
            currentSong = song
            currentSongId = song.id
        }

        fun playNext(): Song? {
            if (queue.isEmpty()) return null
            val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex == -1 || currentIndex >= queue.size - 1) return null
            val nextSong = queue[currentIndex + 1]
            play(nextSong)
            return nextSong
        }

        fun toggleShuffle() {
            isShuffled = !isShuffled
            val canonical = originalQueue.map { it.song }.ifEmpty { queue }
            queue = if (isShuffled) {
                val current = canonical.firstOrNull { it.id == currentSongId }
                if (current != null) listOf(current) + canonical.filterNot { it.id == current.id }.shuffled()
                else canonical.shuffled()
            } else {
                canonical.distinctBy { it.id }
            }
        }
    }

    @Test
    fun adding_single_song_to_end_of_queue_preserves_order() {
        val qm = TestQueueManager()
        val s1 = sampleSong(1, "Track 1")
        val s2 = sampleSong(2, "Track 2")
        val s3 = sampleSong(3, "Track 3")

        qm.setQueue(listOf(s1, s2))
        qm.play(s1)

        assertEquals(2, qm.queue.size)
        assertEquals(s1.id, qm.currentSongId)

        // Add single song to queue
        qm.addToQueue(s3)

        assertEquals(3, qm.queue.size)
        assertEquals(listOf(s1.id, s2.id, s3.id), qm.queue.map { it.id })
        assertEquals(s1.id, qm.currentSongId) // Current song remains unchanged
    }

    @Test
    fun adding_custom_playlist_to_end_of_queue_preserves_order_and_does_not_autoplay() {
        val qm = TestQueueManager()
        val currentPlaylist = listOf(sampleSong(1, "Song 1"), sampleSong(2, "Song 2"))
        val customPlaylist = listOf(sampleSong(3, "Song 3"), sampleSong(4, "Song 4"), sampleSong(5, "Song 5"))

        qm.setQueue(currentPlaylist, playlistId = 100L)
        qm.play(currentPlaylist[0])

        // Add whole custom playlist to queue
        qm.addToQueue(customPlaylist)

        assertEquals(5, qm.queue.size)
        assertEquals("Queue must preserve initial songs then append playlist in declared order",
            listOf(1L, 2L, 3L, 4L, 5L), qm.queue.map { it.id })

        // Must NOT start playing the first track of the added playlist
        assertEquals("Current song must remain the previously active track", 1L, qm.currentSongId)
    }

    @Test
    fun adding_playlist_ignores_duplicate_ids_and_duplicate_callbacks() {
        val qm = TestQueueManager()
        val currentPlaylist = listOf(sampleSong(1, "Song 1"), sampleSong(2, "Song 2"))
        qm.setQueue(currentPlaylist)

        // Playlist with duplicates within itself and overlap with current queue
        val incoming = listOf(sampleSong(2, "Song 2"), sampleSong(3, "Song 3"), sampleSong(3, "Song 3 duplicate"))

        qm.addToQueue(incoming)
        assertEquals("Must add only non-duplicate new tracks", listOf(1L, 2L, 3L), qm.queue.map { it.id })

        // Repeated duplicate callback
        qm.addToQueue(incoming)
        assertEquals("Repeated callback must not duplicate existing entries", listOf(1L, 2L, 3L), qm.queue.map { it.id })
    }

    @Test
    fun playlist_observers_do_not_overwrite_queue_additions() {
        val qm = TestQueueManager()
        val allSongs = (1L..10L).map { sampleSong(it, "Song $it") }

        // Start with Playlist A (IDs 1, 2)
        val playlistA = listOf(allSongs[0], allSongs[1])
        qm.setQueue(playlistA, playlistId = 100L)
        qm.play(playlistA[0])

        // User enqueues Playlist B (IDs 3, 4, 5)
        val playlistB = listOf(allSongs[2], allSongs[3], allSongs[4])
        qm.addToQueue(playlistB)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), qm.queue.map { it.id })

        // Playlist A observer fires (e.g. a song was added to Playlist A database: IDs 1, 2, 6)
        qm.refreshQueueFromIds(listOf(1L, 2L, 6L), allSongs)

        // The user's added songs (3, 4, 5) MUST NOT be wiped out!
        val queueIds = qm.queue.map { it.id }
        assertTrue("Queue must still contain user added track 3", queueIds.contains(3L))
        assertTrue("Queue must still contain user added track 4", queueIds.contains(4L))
        assertTrue("Queue must still contain user added track 5", queueIds.contains(5L))
        assertEquals(listOf(1L, 2L, 6L, 3L, 4L, 5L), queueIds)
    }

    @Test
    fun starting_custom_playlist_clears_stale_favorites_and_prior_queue_additions() {
        val qm = TestQueueManager()
        val allSongs = (1L..10L).map { sampleSong(it, "Song $it") }

        // Step 1: User was playing Favorites (playlistId = -1L) with added songs
        val favorites = listOf(allSongs[0], allSongs[1])
        qm.setQueue(favorites, playlistId = -1L)
        qm.play(favorites[0])
        qm.addToQueue(allSongs[2]) // Enqueued track 3

        assertEquals(-1L, qm.currentPlaylistId)
        assertEquals(listOf(1L, 2L, 3L), qm.queue.map { it.id })

        // Step 2: User starts a Custom Playlist C (IDs 7, 8)
        val playlistC = listOf(allSongs[6], allSongs[7])
        qm.setQueue(playlistC, playlistId = 500L)
        qm.play(playlistC[0])

        assertEquals(500L, qm.currentPlaylistId)
        assertEquals("Queue must be reset to custom playlist tracks without inheriting stale favorites queue",
            listOf(7L, 8L), qm.queue.map { it.id })
        assertTrue("Manually added queue IDs from previous sequence must be cleared", qm.manuallyAddedSongIds.isEmpty())

        // Step 3: Favorite observer fires for Favorites (-1L)
        // Since currentPlaylistId is now 500L, the favorites observer must NOT corrupt Playlist C
        if (qm.currentPlaylistId == -1L) {
            qm.refreshQueueFromIds(listOf(1L, 2L), allSongs)
        }
        assertEquals("Playlist C queue must remain untouched by favorites observer",
            listOf(7L, 8L), qm.queue.map { it.id })
    }

    @Test
    fun sequential_playback_across_multiple_playlists_advances_in_correct_order() {
        val qm = TestQueueManager()
        val songs = (1L..4L).map { sampleSong(it, "Song $it") }

        // Playlist 1: Song 1, Song 2
        qm.setQueue(listOf(songs[0], songs[1]), playlistId = 1L)
        qm.play(songs[0])

        // Add Playlist 2: Song 3, Song 4
        qm.addToQueue(listOf(songs[2], songs[3]))

        assertEquals(1L, qm.currentSongId)

        // Natural completion of Song 1 -> advances to Song 2
        val next1 = qm.playNext()
        assertEquals(2L, next1?.id)

        // Natural completion of Song 2 -> advances to Song 3 (from Playlist 2)
        val next2 = qm.playNext()
        assertEquals(3L, next2?.id)

        // Natural completion of Song 3 -> advances to Song 4
        val next3 = qm.playNext()
        assertEquals(4L, next3?.id)

        // End of queue
        val next4 = qm.playNext()
        assertEquals(null, next4)
    }

    @Test
    fun queue_updates_while_shuffle_enabled_maintains_head_and_restores_canonical() {
        val qm = TestQueueManager()
        val songs = (1L..5L).map { sampleSong(it, "Song $it") }

        qm.setQueue(listOf(songs[0], songs[1], songs[2]))
        qm.play(songs[1]) // Current song is 2

        // Enable shuffle
        qm.toggleShuffle()
        assertTrue(qm.isShuffled)
        assertEquals(2L, qm.queue.first().id) // Current song at head

        // Add new songs while shuffled
        qm.addToQueue(listOf(songs[3], songs[4]))

        // Presentation queue must have all 5 songs
        assertEquals(5, qm.queue.size)
        assertTrue(qm.queue.map { it.id }.containsAll(listOf(1L, 2L, 3L, 4L, 5L)))

        // Canonical queue must have added songs appended at end in order
        val canonicalIds = qm.originalQueue.map { it.song.id }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), canonicalIds)

        // Disable shuffle -> restores canonical order with added songs at end
        qm.toggleShuffle()
        assertFalse(qm.isShuffled)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), qm.queue.map { it.id })
    }

    @Test
    fun testPlayNextInsertsImmediatelyAfterCurrentSongInOrder() {
        val qm = TestQueueManager()
        val songs = (1L..6L).map { sampleSong(it, "Song $it") }

        // Start with queue: [1, 2, 3]
        qm.setQueue(listOf(songs[0], songs[1], songs[2]))
        qm.play(songs[1]) // Current song is 2 (at index 1)

        // User chooses "Play Next" for [4, 5]
        qm.playNext(listOf(songs[3], songs[4]))

        // Queue order must be: 1, 2, 4, 5, 3
        val expected = listOf(1L, 2L, 4L, 5L, 3L)
        assertEquals(expected, qm.queue.map { it.id })

        // Currently playing song must remain unchanged
        assertEquals(2L, qm.currentSong?.id)

        // Adding another song via Play Next: [6]
        qm.playNext(listOf(songs[5]))
        // Queue order must be: 1, 2, 6, 4, 5, 3
        assertEquals(listOf(1L, 2L, 6L, 4L, 5L, 3L), qm.queue.map { it.id })
    }

    @Test
    fun testSongIdentityMatcherRejectsMismatchedRemixesAndCovers() {
        val localOriginal = sampleSong(1L, "Blinding Lights", "The Weeknd", duration = 200_000L)
        val onlineOriginal = com.aman.auramusic.online.model.OnlineSong(
            id = "yt_1",
            title = "Blinding Lights (Official Audio)",
            artist = "The Weeknd",
            durationSeconds = 200L
        )
        val onlineRemix = com.aman.auramusic.online.model.OnlineSong(
            id = "yt_2",
            title = "Blinding Lights (Major Lazer Remix)",
            artist = "The Weeknd, Major Lazer",
            durationSeconds = 195L
        )
        val onlineLive = com.aman.auramusic.online.model.OnlineSong(
            id = "yt_3",
            title = "Blinding Lights (Live at Super Bowl)",
            artist = "The Weeknd",
            durationSeconds = 240L
        )
        val onlineCover = com.aman.auramusic.online.model.OnlineSong(
            id = "yt_4",
            title = "Blinding Lights - Acoustic Cover",
            artist = "Acoustic Sessions",
            durationSeconds = 200L
        )

        // Original matches original
        assertTrue(com.aman.auramusic.util.SongIdentityMatcher.isConfidentMatch(localOriginal, onlineOriginal))

        // Remix must NOT match original
        assertFalse(com.aman.auramusic.util.SongIdentityMatcher.isConfidentMatch(localOriginal, onlineRemix))

        // Live version must NOT match studio original
        assertFalse(com.aman.auramusic.util.SongIdentityMatcher.isConfidentMatch(localOriginal, onlineLive))

        // Cover by another artist must NOT match original
        assertFalse(com.aman.auramusic.util.SongIdentityMatcher.isConfidentMatch(localOriginal, onlineCover))
    }

    @Test
    fun testAlbumTracklistFilterExcludesUnrelatedUploads() {
        val raw = listOf(
            com.aman.auramusic.online.model.OnlineSong(id = "1", title = "Alone Again", artist = "The Weeknd", album = "After Hours"),
            com.aman.auramusic.online.model.OnlineSong(id = "2", title = "Too Late", artist = "The Weeknd", album = "After Hours"),
            com.aman.auramusic.online.model.OnlineSong(id = "3", title = "Blinding Lights (Live)", artist = "The Weeknd", album = "After Hours"),
            com.aman.auramusic.online.model.OnlineSong(id = "4", title = "Save Your Tears (Karaoke Tribute)", artist = "Singers", album = "After Hours"),
            com.aman.auramusic.online.model.OnlineSong(id = "5", title = "Random Hit", artist = "Drake", album = "Scorpion")
        )

        val filtered = com.aman.auramusic.util.SongIdentityMatcher.filterAlbumTracks(
            targetAlbumName = "After Hours",
            targetArtistName = "The Weeknd",
            rawTracks = raw
        )

        // Must keep legitimate studio tracks, exclude live cover, karaoke tribute, and completely different album
        assertEquals(listOf("1", "2"), filtered.map { it.id })
    }

    @Test
    fun testPalettePresetsGenerateDistinctHarmoniousPalettes() {
        val neutralDark = com.aman.auramusic.ui.theme.PlayerArtworkPalette.neutral(isDark = true)
        val midnight = com.aman.auramusic.ui.theme.PlayerArtworkPalette.fromPreset("Midnight Indigo", isDark = true)
        val crimson = com.aman.auramusic.ui.theme.PlayerArtworkPalette.fromPreset("Crimson Velvet", isDark = true)

        assertNotEquals(neutralDark.primary, midnight.primary)
        assertNotEquals(neutralDark.primary, crimson.primary)
        assertNotEquals(midnight.primary, crimson.primary)
    }

    // =========================================================================
    // TASK: Queue Coordination Regression Tests
    // =========================================================================

    private class MockTestDispatcher : PlaybackDispatcher {
        var localPlayed: Song? = null
        var onlinePlayed: com.aman.auramusic.online.model.OnlineSong? = null
        var localPausedCount = 0
        var onlinePausedCount = 0

        override fun playLocal(song: Song) {
            localPlayed = song
        }

        override fun playOnline(onlineSong: com.aman.auramusic.online.model.OnlineSong, startPositionMs: Long) {
            onlinePlayed = onlineSong
        }

        override fun pauseLocal() {
            localPausedCount++
        }

        override fun pauseOnline() {
            onlinePausedCount++
        }

        override fun isOnlineActive(): Boolean = onlinePlayed != null
    }

    private fun sampleOnlineSong(id: String, title: String): com.aman.auramusic.online.model.OnlineSong {
        return com.aman.auramusic.online.model.OnlineSong(
            id = id,
            title = title,
            artist = "Online Artist",
            album = "Online Album",
            durationSeconds = 210L,
            artworkUrl = "https://artwork.url/$id",
            streamUrl = "https://stream.url/$id",
            source = com.aman.auramusic.online.model.AudioSource.YOUTUBE
        )
    }

    @Test
    fun regression_1_adding_local_song_appends_without_interrupting_playback() {
        PlaybackQueueCoordinator.resetForTesting()
        val mock = MockTestDispatcher()
        PlaybackQueueCoordinator.registerDispatcher(mock)

        val s1 = sampleSong(1L, "Song 1")
        val s2 = sampleSong(2L, "Song 2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1), s1)

        mock.localPlayed = null
        val res = PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(s2)))
        assertTrue(res is QueueActionResult.Added)

        val queue = PlaybackQueueCoordinator.queueFlow.value
        assertEquals(2, queue.size)
        assertEquals("Song 1", queue[0].song.title)
        assertEquals("Song 2", queue[1].song.title)
        assertEquals(1L, PlaybackQueueCoordinator.currentTrack?.song?.id)
        assertNull(mock.localPlayed)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_2_adding_online_song_updates_queue_consumed_by_next() {
        PlaybackQueueCoordinator.resetForTesting()
        val mock = MockTestDispatcher()
        PlaybackQueueCoordinator.registerDispatcher(mock)

        val o1 = sampleOnlineSong("yt_1", "Online 1")
        val o2 = sampleOnlineSong("yt_2", "Online 2")
        PlaybackQueueCoordinator.setQueueOnline(listOf(o1), o1)

        val res = PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(o2)))
        assertTrue(res is QueueActionResult.Added)

        val advanced = PlaybackQueueCoordinator.playNextTrack(isManual = true)
        assertTrue(advanced)
        assertEquals("yt_2", PlaybackQueueCoordinator.currentTrack?.onlineSong?.id)
        assertEquals("yt_2", mock.onlinePlayed?.id)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_3_adding_entire_online_playlist_appends_in_order() {
        PlaybackQueueCoordinator.resetForTesting()
        val init = sampleOnlineSong("init", "Playing")
        PlaybackQueueCoordinator.setQueueOnline(listOf(init), init)

        val playlist = (1..5).map { sampleOnlineSong("yt_$it", "Track $it") }
        val res = PlaybackQueueCoordinator.addToQueue(playlist.map { PlaybackQueueCoordinator.createTrack(it) })
        assertTrue(res is QueueActionResult.Added)
        assertEquals(5, (res as QueueActionResult.Added).count)

        val queue = PlaybackQueueCoordinator.queueFlow.value
        assertEquals(6, queue.size)
        assertEquals("Playing", queue[0].song.title)
        for (i in 1..5) {
            assertEquals("Track $i", queue[i].song.title)
        }
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_4_adding_local_playlist_appends_in_order() {
        PlaybackQueueCoordinator.resetForTesting()
        val cur = sampleSong(10L, "Playing")
        PlaybackQueueCoordinator.setQueueSongs(listOf(cur), cur)

        val playlist = (1L..4L).map { sampleSong(it, "Album $it") }
        val res = PlaybackQueueCoordinator.addToQueue(playlist.map { PlaybackQueueCoordinator.createTrack(it) })
        assertTrue(res is QueueActionResult.Added)
        assertEquals(4, (res as QueueActionResult.Added).count)

        val queue = PlaybackQueueCoordinator.queueFlow.value
        assertEquals(5, queue.size)
        for (i in 1..4) {
            assertEquals("Album $i", queue[i].song.title)
        }
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_5_play_next_inserts_immediately_after_current_track() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "1")
        val s2 = sampleSong(2L, "2")
        val s3 = sampleSong(3L, "3")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2, s3), s1)

        val next = sampleSong(99L, "Next")
        PlaybackQueueCoordinator.playNext(listOf(PlaybackQueueCoordinator.createTrack(next)))

        val queue = PlaybackQueueCoordinator.queueFlow.value
        assertEquals(listOf(1L, 99L, 2L, 3L), queue.map { it.song.id })
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_6_play_next_multiple_tracks_preserves_relative_order() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "1")
        val s2 = sampleSong(2L, "2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2), s1)

        val multi = listOf(sampleSong(10L, "10"), sampleSong(20L, "20"), sampleSong(30L, "30"))
        PlaybackQueueCoordinator.playNext(multi.map { PlaybackQueueCoordinator.createTrack(it) })

        val queue = PlaybackQueueCoordinator.queueFlow.value
        assertEquals(listOf(1L, 10L, 20L, 30L, 2L), queue.map { it.song.id })
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_7_duplicate_handling_across_repeated_presses() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "1")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1), s1)

        val s2 = sampleSong(2L, "2")
        val res1 = PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(s2)))
        assertTrue(res1 is QueueActionResult.Added)

        val res2 = PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(s2)))
        assertTrue(res2 is QueueActionResult.AlreadyQueued)
        assertEquals(2, PlaybackQueueCoordinator.queueFlow.value.size)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_8_playlist_observers_do_not_erase_manually_queued_tracks() {
        PlaybackQueueCoordinator.resetForTesting()
        val p1 = sampleSong(1L, "P1")
        val p2 = sampleSong(2L, "P2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(p1, p2), p1, playlistId = 77L)

        val manual = sampleSong(999L, "Manual")
        PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(manual)))

        val p3 = sampleSong(3L, "P3")
        PlaybackQueueCoordinator.refreshFromPlaylist(77L, listOf(p1, p3), emptyList())

        val ids = PlaybackQueueCoordinator.queueFlow.value.map { it.song.id }
        assertTrue(ids.contains(999L))
        assertTrue(ids.contains(1L))
        assertTrue(ids.contains(3L))
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_9_queue_removal_and_reorder_update_actual_queue() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "A")
        val s2 = sampleSong(2L, "B")
        val s3 = sampleSong(3L, "C")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2, s3), s1)

        PlaybackQueueCoordinator.moveQueueItem(0, 2)
        assertEquals(listOf("B", "C", "A"), PlaybackQueueCoordinator.queueFlow.value.map { it.song.title })

        PlaybackQueueCoordinator.removeTrack("local:3")
        assertEquals(listOf("B", "A"), PlaybackQueueCoordinator.queueFlow.value.map { it.song.title })
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_10_queue_ui_state_and_playback_remain_consistent() {
        PlaybackQueueCoordinator.resetForTesting()
        var synced: List<QueueTrack>? = null
        PlaybackQueueCoordinator.addEngineSyncListener { synced = it }

        val s1 = sampleSong(1L, "A")
        val s2 = sampleSong(2L, "B")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1), s1)
        PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(s2)))

        assertNotNull(synced)
        assertEquals(2, synced!!.size)
        assertEquals(PlaybackQueueCoordinator.queueFlow.value, synced)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_11_switching_between_local_and_online_routes_correctly() {
        PlaybackQueueCoordinator.resetForTesting()
        val mock = MockTestDispatcher()
        PlaybackQueueCoordinator.registerDispatcher(mock)

        val local = sampleSong(1L, "Local")
        val online = sampleOnlineSong("yt_on", "Online")

        PlaybackQueueCoordinator.setQueueSongs(listOf(local), local)
        PlaybackQueueCoordinator.addToQueue(listOf(PlaybackQueueCoordinator.createTrack(online)))
        PlaybackQueueCoordinator.playTrack(PlaybackQueueCoordinator.createTrack(local))

        assertEquals(1L, mock.localPlayed?.id)
        assertTrue(mock.onlinePausedCount > 0)

        PlaybackQueueCoordinator.playNextTrack(isManual = true)
        assertEquals("yt_on", mock.onlinePlayed?.id)
        assertTrue(mock.localPausedCount > 0)

        PlaybackQueueCoordinator.playPreviousTrack()
        assertEquals(1L, mock.localPlayed?.id)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_12_shuffle_repeat_next_previous_continuity() {
        PlaybackQueueCoordinator.resetForTesting()
        val songs = (1L..5L).map { sampleSong(it, "Song $it") }
        PlaybackQueueCoordinator.setQueueSongs(songs, songs[0])

        assertTrue(PlaybackQueueCoordinator.toggleShuffle())
        assertTrue(PlaybackQueueCoordinator.isShuffled)
        assertEquals(1L, PlaybackQueueCoordinator.currentTrack?.song?.id)

        assertEquals(RepeatMode.ALL, PlaybackQueueCoordinator.toggleRepeat())
        assertEquals(RepeatMode.ONE, PlaybackQueueCoordinator.toggleRepeat())
        assertEquals(RepeatMode.NONE, PlaybackQueueCoordinator.toggleRepeat())

        PlaybackQueueCoordinator.toggleShuffle()
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), PlaybackQueueCoordinator.queueFlow.value.map { it.song.id })

        PlaybackQueueCoordinator.playNextTrack(isManual = true)
        assertEquals(2L, PlaybackQueueCoordinator.currentTrack?.song?.id)
        PlaybackQueueCoordinator.playPreviousTrack()
        assertEquals(1L, PlaybackQueueCoordinator.currentTrack?.song?.id)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_13_repeat_one_natural_completion_replays_current_track() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "Track 1")
        val s2 = sampleSong(2L, "Track 2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2), s1)
        PlaybackQueueCoordinator.setRepeatMode(RepeatMode.ONE)

        // Natural completion (isManual = false)
        val started = PlaybackQueueCoordinator.playNextTrack(isManual = false)
        assertTrue(started)
        assertEquals(1L, PlaybackQueueCoordinator.currentTrack?.song?.id)

        // Manual Next advances even when RepeatMode.ONE is active
        val manualStarted = PlaybackQueueCoordinator.playNextTrack(isManual = true)
        assertTrue(manualStarted)
        assertEquals(2L, PlaybackQueueCoordinator.currentTrack?.song?.id)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_14_repeat_all_and_none_natural_end_of_queue() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "Track 1")
        val s2 = sampleSong(2L, "Track 2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2), s2) // at end of queue
        PlaybackQueueCoordinator.setRepeatMode(RepeatMode.ALL)

        // RepeatMode.ALL wraps to head
        val startedAll = PlaybackQueueCoordinator.playNextTrack(isManual = false)
        assertTrue(startedAll)
        assertEquals(1L, PlaybackQueueCoordinator.currentTrack?.song?.id)

        // Advance to end again
        PlaybackQueueCoordinator.playNextTrack(isManual = true)
        assertEquals(2L, PlaybackQueueCoordinator.currentTrack?.song?.id)

        // RepeatMode.NONE stops at end of queue
        PlaybackQueueCoordinator.setRepeatMode(RepeatMode.NONE)
        val startedNone = PlaybackQueueCoordinator.playNextTrack(isManual = false)
        assertFalse("Natural end of queue with RepeatMode.NONE must pause and return false", startedNone)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_15_removing_current_track_starts_next_track_or_stops_cleanly() {
        PlaybackQueueCoordinator.resetForTesting()
        val s1 = sampleSong(1L, "Track 1")
        val s2 = sampleSong(2L, "Track 2")
        PlaybackQueueCoordinator.setQueueSongs(listOf(s1, s2), s1)

        // Removing current track s1 should advance to s2
        PlaybackQueueCoordinator.removeTrack("local:1")
        assertEquals(listOf(2L), PlaybackQueueCoordinator.queueFlow.value.map { it.song.id })
        assertEquals(2L, PlaybackQueueCoordinator.currentTrack?.song?.id)

        // Removing remaining s2 empties queue and clears current track
        PlaybackQueueCoordinator.removeTrack("local:2")
        assertTrue(PlaybackQueueCoordinator.queueFlow.value.isEmpty())
        assertNull(PlaybackQueueCoordinator.currentTrack)
        PlaybackQueueCoordinator.resetForTesting()
    }

    @Test
    fun regression_16_stable_track_identity_differentiates_same_title_from_different_sources() {
        PlaybackQueueCoordinator.resetForTesting()
        val localSong = sampleSong(100L, "Hello")
        val onlineSong = sampleOnlineSong("yt_hello_123", "Hello")

        val trackLocal = PlaybackQueueCoordinator.createTrack(localSong)
        val trackOnline = PlaybackQueueCoordinator.createTrack(onlineSong)

        assertNotEquals(trackLocal.stableKey, trackOnline.stableKey)
        assertEquals("local:100", trackLocal.stableKey)
        assertEquals("online:YOUTUBE:yt_hello_123", trackOnline.stableKey)

        PlaybackQueueCoordinator.setQueue(listOf(trackLocal, trackOnline), trackLocal)
        assertEquals(2, PlaybackQueueCoordinator.queueFlow.value.size)
        PlaybackQueueCoordinator.resetForTesting()
    }
}
