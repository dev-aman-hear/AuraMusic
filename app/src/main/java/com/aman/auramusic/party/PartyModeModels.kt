package com.aman.auramusic.party

import org.json.JSONObject

enum class PartyRole { HOST, GUEST }

data class PartySession(
    val partyId: String,
    val pairingCode: String,
    val memberToken: String,
    val role: PartyRole,
    val websocketUrl: String,
    val expiresAt: String? = null
)

data class PartyPlayback(
    val songId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val source: String = "",
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val serverTimeMs: Long = 0L
)

data class PartySongRequest(
    val requestId: String,
    val title: String,
    val artist: String,
    val source: String,
    val requester: String = ""
)

data class PartyUiState(
    val session: PartySession? = null,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val error: String? = null,
    val status: String = "Create a party or join with a code.",
    val playback: PartyPlayback? = null,
    val requests: List<PartySongRequest> = emptyList(),
    val members: Int = 1,
    val decisionMessage: String? = null
)

internal fun JSONObject.optNonBlank(key: String): String? =
    optString(key).takeIf { it.isNotBlank() && it != "null" }
