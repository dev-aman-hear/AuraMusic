package com.aman.auramusic.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val filePath: String,
    val artworkUri: String?,
    val mimeType: String
)
