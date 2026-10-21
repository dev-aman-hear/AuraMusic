package com.aman.auramusic.util

import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.model.OnlineSong

/**
 * Resolves a local-library selection to saved online tracks without fabricating stream URLs.
 * Unmatched queue items are omitted; the selected online track is always included.
 */
data class ResolvedOnlineQueue(
    val selectedSong: OnlineSong,
    val songs: List<OnlineSong>
)

object OnlineSongQueueResolver {
    fun resolve(
        selected: Song,
        queue: List<Song>,
        candidates: List<OnlineSong>
    ): ResolvedOnlineQueue? {
        val selectedOnline = SongIdentityMatcher.findOnlineMatch(selected, candidates)
            ?: return null

        val resolvedQueue = queue.mapNotNull { song ->
            SongIdentityMatcher.findOnlineMatch(song, candidates)
        }.distinctBy { it.id }.toMutableList()

        if (resolvedQueue.none { it.id == selectedOnline.id }) {
            resolvedQueue.add(0, selectedOnline)
        }

        return ResolvedOnlineQueue(selectedOnline, resolvedQueue)
    }
}
