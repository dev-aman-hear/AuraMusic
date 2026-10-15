package com.aman.auramusic.online

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aman.auramusic.data.repository.UserPreferencesRepository
import com.aman.auramusic.online.model.AudioSource
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Regression tests verifying persistence and restoration of online album favourites
 * and collection favourites in UserPreferencesRepository.
 */
class OnlineAlbumFavoritesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.IO + Job())
        dataStoreFile = tempFolder.newFile("test_user_preferences.preferences_pb")
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

    private fun createSampleOnlineSong(
        id: String,
        title: String = "Online Track $id",
        artist: String = "Test Artist",
        album: String = "Online Test Album"
    ): OnlineSong {
        return OnlineSong(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = "https://images.example.com/artwork/$id.jpg",
            durationSeconds = 185L,
            source = AudioSource.SPOTIFY,
            streamUrl = "https://stream.example.com/audio/$id.mp4",
            bitrate = "320 kbps",
            format = "AAC/MP4"
        )
    }

    @Test
    fun favouritingOnlineAlbumTrackPersistsMetadata() = runBlocking {
        val onlineTrack = createSampleOnlineSong("online_album_song_1")
        val song = onlineTrack.toSong()

        repository.setFavorite(song.id, true, onlineTrack)

        val favoriteIds = repository.favoriteIdsFlow.first()
        val onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertTrue("Favorite IDs should contain the hashed song ID", favoriteIds.contains(song.id))
        assertEquals("There should be exactly 1 online favourite saved", 1, onlineFavorites.size)

        val saved = onlineFavorites.first()
        assertEquals(onlineTrack.id, saved.id)
        assertEquals(onlineTrack.title, saved.title)
        assertEquals(onlineTrack.artist, saved.artist)
        assertEquals(onlineTrack.album, saved.album)
        assertEquals(onlineTrack.artworkUrl, saved.artworkUrl)
        assertEquals(onlineTrack.durationSeconds, saved.durationSeconds)
        assertEquals(onlineTrack.source, saved.source)
        assertEquals("", saved.streamUrl) // Expiring stream URL must never be persisted
    }

    @Test
    fun favouriteRemainsAvailableAfterReload() = runBlocking {
        val onlineTrack = createSampleOnlineSong("reloaded_album_song")
        val song = onlineTrack.toSong()

        repository.setFavorite(song.id, true, onlineTrack)

        // Cancel first DataStore scope so OkioStorage releases the file lock
        testScope.cancel()
        testScope.coroutineContext[Job]?.join()

        // Simulate app restart / reload by creating a new repository with a new DataStore over the same file
        val reloadScope = CoroutineScope(Dispatchers.IO + Job())
        val reloadedDataStore = PreferenceDataStoreFactory.create(
            scope = reloadScope,
            produceFile = { dataStoreFile }
        )
        val reloadedRepo = UserPreferencesRepository(reloadedDataStore)

        val reloadedIds = reloadedRepo.favoriteIdsFlow.first()
        val reloadedOnlineSongs = reloadedRepo.favoriteOnlineSongsFlow.first()

        assertTrue(reloadedIds.contains(song.id))
        assertEquals(1, reloadedOnlineSongs.size)

        val restoredSong = reloadedOnlineSongs.first().toSong()
        assertEquals("Restored song ID should match original hashed ID", song.id, restoredSong.id)
        assertEquals(song.title, restoredSong.title)
        assertEquals(song.artist, restoredSong.artist)
        assertEquals(song.album, restoredSong.album)

        reloadScope.cancel()
    }

    @Test
    fun unfavouritingRemovesCorrectOnlineTrack() = runBlocking {
        val trackA = createSampleOnlineSong("track_a", title = "Alpha")
        val trackB = createSampleOnlineSong("track_b", title = "Beta")
        val songA = trackA.toSong()
        val songB = trackB.toSong()

        repository.setFavorite(songA.id, true, trackA)
        repository.setFavorite(songB.id, true, trackB)

        var favoriteIds = repository.favoriteIdsFlow.first()
        var onlineFavorites = repository.favoriteOnlineSongsFlow.first()
        assertEquals(2, favoriteIds.size)
        assertEquals(2, onlineFavorites.size)

        // Unfavourite track A
        repository.setFavorite(songA.id, false, trackA)

        favoriteIds = repository.favoriteIdsFlow.first()
        onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertFalse(favoriteIds.contains(songA.id))
        assertTrue(favoriteIds.contains(songB.id))
        assertEquals(1, onlineFavorites.size)
        assertEquals(trackB.id, onlineFavorites.first().id)
    }

    @Test
    fun repeatedFavouriteActionsDoNotCreateDuplicates() = runBlocking {
        val track = createSampleOnlineSong("duplicate_check_track")
        val song = track.toSong()

        // Favourite the same track 3 times consecutively
        repository.setFavorite(song.id, true, track)
        repository.setFavorite(song.id, true, track)
        repository.setFavorite(song.id, true, track)

        val favoriteIds = repository.favoriteIdsFlow.first()
        val onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertEquals("Favorite IDs must not contain duplicates", 1, favoriteIds.size)
        assertEquals("Favorite online songs must not contain duplicates", 1, onlineFavorites.size)
        assertEquals(track.id, onlineFavorites.first().id)
    }

    @Test
    fun favouritingEntireOnlineAlbumHandlesEveryTrack() = runBlocking {
        val albumTracks = (1..5).map { index ->
            createSampleOnlineSong("album_track_$index", title = "Album Track $index")
        }
        val items = albumTracks.map { track ->
            val song = track.toSong()
            song.id to track
        }

        // Batch favourite the entire album
        repository.setFavorites(items, true)

        val favoriteIds = repository.favoriteIdsFlow.first()
        val onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertEquals(5, favoriteIds.size)
        assertEquals(5, onlineFavorites.size)
        items.forEach { (songId, _) ->
            assertTrue(favoriteIds.contains(songId))
        }

        // Re-favouriting the entire album in batch must remain idempotent without duplicates
        repository.setFavorites(items, true)

        val reloadedIds = repository.favoriteIdsFlow.first()
        val reloadedOnlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertEquals("Batch re-favouriting should not duplicate IDs", 5, reloadedIds.size)
        assertEquals("Batch re-favouriting should not duplicate online songs", 5, reloadedOnlineFavorites.size)
    }

    @Test
    fun localFavouritesContinueToWork() = runBlocking {
        val localSongId = 987654321L

        // Favourite local song (onlineSong = null)
        repository.setFavorite(localSongId, true, null)

        var favoriteIds = repository.favoriteIdsFlow.first()
        var onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertTrue("Local song ID must be in favoriteIds", favoriteIds.contains(localSongId))
        assertTrue("No online songs should be saved for local favourite", onlineFavorites.isEmpty())

        // Unfavourite local song
        repository.setFavorite(localSongId, false, null)

        favoriteIds = repository.favoriteIdsFlow.first()
        onlineFavorites = repository.favoriteOnlineSongsFlow.first()

        assertFalse("Local song ID should be removed", favoriteIds.contains(localSongId))
        assertTrue(onlineFavorites.isEmpty())
    }
}
