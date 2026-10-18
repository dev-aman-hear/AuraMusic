package com.aman.auramusic.playback

import android.util.Log
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong
import com.aman.auramusic.online.model.toSong
import com.aman.auramusic.online.model.toOnlineSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class QueueTrack(
    val queueId: String = UUID.randomUUID().toString(),
    val song: Song,
    val onlineSong: OnlineSong? = null
) {
    val isOnline: Boolean
        get() = onlineSong != null || song.isOnline

    val stableKey: String
        get() = when {
            onlineSong != null -> "online:${onlineSong.source}:${onlineSong.id}"
            song.onlineId != null -> "online:${song.onlineId}"
            song.isOnline -> "online:${song.id}"
            else -> "local:${song.id}"
        }
}

sealed class QueueActionResult {
    data class Added(val count: Int, val message: String) : QueueActionResult()
    data class AlreadyQueued(val message: String) : QueueActionResult()
}

interface PlaybackDispatcher {
    fun playLocal(song: Song)
    fun playOnline(onlineSong: OnlineSong, startPositionMs: Long = 0L)
    fun pauseLocal()
    fun pauseOnline()
    fun isOnlineActive(): Boolean
}

/**
 * Authoritative Queue Coordinator managing unified queue state, source routing,
 * duplicate filtering, ordering, and playback transitions across local (VLC)
 * and online (ExoPlayer/RiPlay) engines.
 */
object PlaybackQueueCoordinator {
    private const val TAG = "PlaybackQueueCoord"

    private val onlineRegistry = ConcurrentHashMap<String, OnlineSong>()

    private val _queue = MutableStateFlow<List<QueueTrack>>(emptyList())
    val queueFlow: StateFlow<List<QueueTrack>> = _queue.asStateFlow()

    private val _currentTrack = MutableStateFlow<QueueTrack?>(null)
    val currentTrackFlow: StateFlow<QueueTrack?> = _currentTrack.asStateFlow()
    val currentTrack: QueueTrack?
        get() = _currentTrack.value

    private var originalQueue: List<QueueTrack> = emptyList()
    val manuallyAddedKeys = mutableSetOf<String>()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffledFlow: StateFlow<Boolean> = _isShuffled.asStateFlow()
    val isShuffled: Boolean
        get() = _isShuffled.value

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatModeFlow: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    val repeatMode: RepeatMode
        get() = _repeatMode.value

    var currentPlaylistId: Long? = null

    private var dispatcher: PlaybackDispatcher? = null
    private val engineSyncListeners = java.util.concurrent.CopyOnWriteArrayList<(List<QueueTrack>) -> Unit>()

    fun registerDispatcher(d: PlaybackDispatcher) {
        dispatcher = d
    }

    fun unregisterDispatcher(d: PlaybackDispatcher? = null) {
        if (d == null || dispatcher == d) {
            dispatcher = null
        }
    }

    fun addEngineSyncListener(listener: (List<QueueTrack>) -> Unit) {
        if (!engineSyncListeners.contains(listener)) {
            engineSyncListeners.add(listener)
        }
    }

    fun removeEngineSyncListener(listener: (List<QueueTrack>) -> Unit) {
        engineSyncListeners.remove(listener)
    }

    fun registerOnlineSong(song: OnlineSong) {
        onlineRegistry[song.id] = song
        val hashKey = (song.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L).toString()
        onlineRegistry[hashKey] = song
    }

    fun getOnlineSong(id: String): OnlineSong? = onlineRegistry[id]

    fun createTrack(song: Song): QueueTrack {
        val online = if (song.isOnline || song.onlineId != null) {
            getOnlineSong(song.onlineId ?: song.id.toString())
        } else null
        return QueueTrack(song = song, onlineSong = online)
    }

    fun createTrack(onlineSong: OnlineSong): QueueTrack {
        registerOnlineSong(onlineSong)
        return QueueTrack(song = onlineSong.toSong(), onlineSong = onlineSong)
    }

    fun setCurrentLocalSong(song: Song) {
        val existing = _queue.value.find { it.stableKey == "local:${song.id}" }
        val track = existing ?: createTrack(song)
        _currentTrack.value = track
        if (existing == null && _queue.value.isEmpty()) {
            _queue.value = listOf(track)
            originalQueue = listOf(track)
            notifySync(_queue.value)
        }
    }

    fun setCurrentOnlineSong(song: OnlineSong) {
        registerOnlineSong(song)
        val targetKey = "online:${song.source}:${song.id}"
        val existing = _queue.value.find { it.stableKey == targetKey || it.stableKey == "online:${song.id}" }
        val track = existing ?: createTrack(song)
        _currentTrack.value = track
        if (existing == null && _queue.value.isEmpty()) {
            _queue.value = listOf(track)
            originalQueue = listOf(track)
            notifySync(_queue.value)
        }
    }

    fun setQueue(
        tracks: List<QueueTrack>,
        current: QueueTrack? = null,
        playlistId: Long? = null
    ) {
        val distinct = tracks.distinctBy { it.stableKey }
        currentPlaylistId = playlistId
        manuallyAddedKeys.clear()
        originalQueue = distinct

        val targetList = if (isShuffled) {
            val targetCur = current ?: distinct.firstOrNull()
            if (targetCur != null) listOf(targetCur) + distinct.filterNot { it.stableKey == targetCur.stableKey }.shuffled()
            else distinct.shuffled()
        } else {
            distinct
        }

        _queue.value = targetList
        if (current != null) {
            _currentTrack.value = current
        } else if (targetList.isNotEmpty()) {
            _currentTrack.value = targetList.first()
        }
        notifySync(targetList)
    }

    fun setQueueSongs(
        songs: List<Song>,
        current: Song? = null,
        playlistId: Long? = null
    ) {
        val tracks = songs.map { createTrack(it) }
        val curTrack = current?.let { createTrack(it) }
        setQueue(tracks, curTrack, playlistId)
    }

    fun setQueueOnline(
        onlineSongs: List<OnlineSong>,
        current: OnlineSong? = null,
        playlistId: Long? = null
    ) {
        val tracks = onlineSongs.map { createTrack(it) }
        val curTrack = current?.let { createTrack(it) }
        setQueue(tracks, curTrack, playlistId)
    }

    fun addToQueue(track: QueueTrack): QueueActionResult = addToQueue(listOf(track))

    fun addToQueue(tracks: List<QueueTrack>): QueueActionResult {
        if (tracks.isEmpty()) return QueueActionResult.AlreadyQueued("No songs to add")

        val currentList = _queue.value
        val existingKeys = currentList.mapTo(mutableSetOf()) { it.stableKey }

        val distinctIncoming = tracks.distinctBy { it.stableKey }
        val newTracks = distinctIncoming.filter { it.stableKey !in existingKeys }

        if (newTracks.isEmpty()) {
            return QueueActionResult.AlreadyQueued(
                if (tracks.size == 1) "Song is already in queue" else "All songs are already in queue"
            )
        }

        manuallyAddedKeys.addAll(newTracks.map { it.stableKey })

        val updated = currentList + newTracks
        _queue.value = updated

        val canonicalKeys = originalQueue.mapTo(mutableSetOf()) { it.stableKey }
        originalQueue = (originalQueue + newTracks.filter { it.stableKey !in canonicalKeys }).distinctBy { it.stableKey }

        notifySync(updated)

        val msg = if (newTracks.size == 1) "Added to queue" else "Added ${newTracks.size} songs to queue"
        return QueueActionResult.Added(newTracks.size, msg)
    }

    fun playNext(track: QueueTrack): QueueActionResult = playNext(listOf(track))

    fun playNext(tracks: List<QueueTrack>): QueueActionResult {
        if (tracks.isEmpty()) return QueueActionResult.AlreadyQueued("No songs to add")

        val currentList = _queue.value
        if (currentList.isEmpty()) {
            return addToQueue(tracks)
        }

        val existingKeys = currentList.mapTo(mutableSetOf()) { it.stableKey }
        val distinctIncoming = tracks.distinctBy { it.stableKey }
        val newTracks = distinctIncoming.filter { it.stableKey !in existingKeys }

        if (newTracks.isEmpty()) {
            return QueueActionResult.AlreadyQueued(
                if (tracks.size == 1) "Song is already in queue" else "All songs are already in queue"
            )
        }

        manuallyAddedKeys.addAll(newTracks.map { it.stableKey })

        val cur = _currentTrack.value
        val insertIndex = if (cur != null) {
            val idx = currentList.indexOfFirst { it.stableKey == cur.stableKey }
            if (idx != -1) idx + 1 else currentList.size
        } else {
            0
        }

        val updated = currentList.toMutableList().apply {
            addAll(insertIndex, newTracks)
        }
        _queue.value = updated

        val canonicalKeys = originalQueue.mapTo(mutableSetOf()) { it.stableKey }
        originalQueue = (originalQueue + newTracks.filter { it.stableKey !in canonicalKeys }).distinctBy { it.stableKey }

        notifySync(updated)

        val msg = if (newTracks.size == 1) "Playing next" else "Playing ${newTracks.size} songs next"
        return QueueActionResult.Added(newTracks.size, msg)
    }

    fun playNextTrack(isManual: Boolean = false): Boolean {
        val currentList = _queue.value
        if (currentList.isEmpty()) {
            Log.d(TAG, "playNextTrack: Queue is empty")
            return false
        }

        val cur = _currentTrack.value
        val curIndex = if (cur != null) currentList.indexOfFirst { it.stableKey == cur.stableKey } else -1

        if (curIndex == -1) {
            playTrack(currentList[0])
            return true
        }

        val nextIndex = when {
            isShuffled -> currentList.indices.random()
            repeatMode == RepeatMode.ONE && !isManual -> curIndex
            curIndex + 1 < currentList.size -> curIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            isManual -> 0
            else -> {
                Log.d(TAG, "playNextTrack: Natural end of queue reached with RepeatMode.NONE.")
                dispatcher?.pauseLocal()
                dispatcher?.pauseOnline()
                return false
            }
        }

        playTrack(currentList[nextIndex])
        return true
    }

    fun playPreviousTrack(): Boolean {
        val currentList = _queue.value
        if (currentList.isEmpty()) return false

        val cur = _currentTrack.value
        val curIndex = if (cur != null) currentList.indexOfFirst { it.stableKey == cur.stableKey } else -1

        val prevIndex = when {
            curIndex <= 0 -> currentList.size - 1
            else -> curIndex - 1
        }

        playTrack(currentList[prevIndex])
        return true
    }

    fun playTrack(track: QueueTrack, startPositionMs: Long = 0L) {
        _currentTrack.value = track

        if (track.isOnline) {
            val online = track.onlineSong ?: getOnlineSong(track.song.onlineId ?: track.song.id.toString()) ?: track.song.toOnlineSong()
            if (online != null) {
                registerOnlineSong(online)
                dispatcher?.pauseLocal()
                dispatcher?.playOnline(online, startPositionMs)
                return
            }
        }

        // Local playback
        dispatcher?.pauseOnline()
        dispatcher?.playLocal(track.song)
    }

    fun playTrackBySong(song: Song) {
        val track = _queue.value.find { it.song.id == song.id || it.stableKey == "local:${song.id}" || (song.onlineId != null && it.stableKey.endsWith(song.onlineId)) }
            ?: createTrack(song)
        playTrack(track)
    }

    fun removeTrack(stableKey: String) {
        val currentList = _queue.value
        val itemToRemove = currentList.find { it.stableKey == stableKey } ?: return
        val isCurrent = _currentTrack.value?.stableKey == stableKey

        val updated = currentList.filterNot { it.stableKey == stableKey }
        _queue.value = updated
        originalQueue = originalQueue.filterNot { it.stableKey == stableKey }
        manuallyAddedKeys.remove(stableKey)

        notifySync(updated)

        if (isCurrent) {
            if (updated.isNotEmpty()) {
                playNextTrack(isManual = true)
            } else {
                _currentTrack.value = null
                dispatcher?.pauseLocal()
                dispatcher?.pauseOnline()
            }
        }
    }

    fun removeSong(song: Song) {
        val key = when {
            song.onlineId != null -> "online:${song.onlineId}"
            song.isOnline -> "online:${song.id}"
            else -> "local:${song.id}"
        }
        val match = _queue.value.find { it.song.id == song.id || it.stableKey == key || (song.onlineId != null && it.stableKey.endsWith(song.onlineId)) }
        if (match != null) {
            removeTrack(match.stableKey)
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _queue.value = currentList
        notifySync(currentList)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = moveTrack(fromIndex, toIndex)

    fun clearQueueExceptCurrent() {
        val cur = _currentTrack.value ?: return
        val updated = listOf(cur)
        _queue.value = updated
        originalQueue = updated
        manuallyAddedKeys.clear()
        notifySync(updated)
    }

    fun toggleShuffle(): Boolean {
        return setShuffle(!isShuffled)
    }

    fun setShuffle(enabled: Boolean): Boolean {
        _isShuffled.value = enabled
        val cur = _currentTrack.value
        val list = if (_isShuffled.value) {
            if (cur != null) listOf(cur) + originalQueue.filterNot { it.stableKey == cur.stableKey }.shuffled()
            else originalQueue.shuffled()
        } else {
            originalQueue
        }
        _queue.value = list
        notifySync(list)
        return _isShuffled.value
    }

    fun toggleRepeat(): RepeatMode {
        val next = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _repeatMode.value = next
        return next
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun refreshFromPlaylist(
        playlistId: Long,
        localSongs: List<Song>,
        onlineSongs: List<OnlineSong> = emptyList()
    ) {
        val cur = _currentTrack.value
        val currentList = _queue.value

        val playlistTracks = (localSongs.map { createTrack(it) } + onlineSongs.map { createTrack(it) })
            .distinctBy { it.stableKey }

        val manuallyAddedTracks = currentList.filter { it.stableKey in manuallyAddedKeys }

        val baseTracks = if (cur != null && playlistTracks.none { it.stableKey == cur.stableKey } && manuallyAddedTracks.none { it.stableKey == cur.stableKey }) {
            listOf(cur) + playlistTracks
        } else {
            playlistTracks
        }

        val fullCanonical = (baseTracks.filterNot { it.stableKey in manuallyAddedKeys } + manuallyAddedTracks)
            .distinctBy { it.stableKey }
        originalQueue = fullCanonical

        val targetList = if (isShuffled) {
            if (cur != null) listOf(cur) + fullCanonical.filterNot { it.stableKey == cur.stableKey }.shuffled()
            else fullCanonical.shuffled()
        } else {
            fullCanonical
        }

        _queue.value = targetList
        notifySync(targetList)
    }

    private fun notifySync(updated: List<QueueTrack>) {
        engineSyncListeners.forEach { runCatching { it(updated) } }
    }

    fun resetForTesting() {
        onlineRegistry.clear()
        _queue.value = emptyList()
        _currentTrack.value = null
        originalQueue = emptyList()
        manuallyAddedKeys.clear()
        _isShuffled.value = false
        _repeatMode.value = RepeatMode.NONE
        currentPlaylistId = null
        dispatcher = null
        engineSyncListeners.clear()
    }
}
