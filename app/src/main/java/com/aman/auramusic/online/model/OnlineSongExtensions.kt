package com.aman.auramusic.online.model

import com.aman.auramusic.data.model.LyricLine
import com.aman.auramusic.data.model.Song
import com.aman.auramusic.online.lyrics.model.SongLyrics
import com.aman.auramusic.online.util.ArtworkQualityOptimizer

/**
 * Canonical conversion extensions between online streaming models and core player models.
 */

fun OnlineSong.toSong(): Song {
    com.aman.auramusic.playback.PlaybackQueueCoordinator.registerOnlineSong(this)
    val optimizedArt = ArtworkQualityOptimizer.optimizeUrl(artworkUrl)
    return Song(
        id = (id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L),
        title = title,
        artist = artist,
        album = album.ifBlank { "Online Stream" },
        duration = durationSeconds * 1000L,
        dateAdded = System.currentTimeMillis(),
        uri = streamUrl.ifBlank { optimizedArt },
        filePath = streamUrl,
        artworkUri = optimizedArt.ifBlank { null },
        mimeType = "audio/mpeg",
        isOnline = true,
        onlineId = id
    )
}

fun Song.toOnlineSong(): OnlineSong? {
    if (!isOnline && onlineId == null) return null
    val targetId = onlineId ?: id.toString()
    return com.aman.auramusic.playback.PlaybackQueueCoordinator.getOnlineSong(targetId) ?: OnlineSong(
        id = targetId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUri ?: "",
        durationSeconds = duration / 1000L,
        streamUrl = filePath
    )
}

fun SongLyrics.toLyricLines(): List<LyricLine> {
    if (lines.isNotEmpty()) {
        return lines.map {
            LyricLine(
                timeMs = it.startMs,
                text = it.lineText
            )
        }
    }
    if (!plainLyrics.isNullOrBlank()) {
        return plainLyrics.lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
            LyricLine(
                timeMs = index * 3000L,
                text = line.trim()
            )
        }
    }
    return emptyList()
}
