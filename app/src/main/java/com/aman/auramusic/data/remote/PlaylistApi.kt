package com.aman.auramusic.data.remote

import com.aman.auramusic.data.model.OnlinePlaylist
import com.aman.auramusic.data.model.OnlinePlaylistDetails
import retrofit2.http.GET
import retrofit2.http.Url

interface PlaylistApi {
    @GET("api/playlists")
    suspend fun getPlaylists(): List<OnlinePlaylist>

    @GET
    suspend fun getPlaylistDetails(@Url url: String): OnlinePlaylistDetails
}
