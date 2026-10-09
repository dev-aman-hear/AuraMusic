package com.aman.auramusic.online.model

/**
 * 4 Selectable YouTube Music Streaming Strategies inspired by research repositories:
 *
 * 1. RIPLAY (fast4x/RiPlay): Headless YouTube Player wrapper (android-youtube-player v13 + background audio).
 * 2. BITCHORD (kushagrasinghx/BitChord): InnerTube multi-client rotation (ANDROID_VR, ANDROID_MUSIC, IOS, WEB_REMIX).
 * 3. DA_TUNES (VikrantRuhela/DA-Tunes): Direct stream manifest resolver (Piped & Invidious failover).
 * 4. OBSIDIAN (cr7pt0gr4ph7/obsidian): Direct Mobile Web Controller (m.youtube.com HTML5 - 100% unrestricted).
 */
enum class StreamingEngine(
    val shortName: String,
    val repoAuthor: String,
    val description: String,
    val badgeColorHex: Long
) {
    RIPLAY(
        shortName = "RiPlay",
        repoAuthor = "fast4x/RiPlay",
        description = "Headless YouTube IFrame Player (100% Zero-Block, Official Google Engine)",
        badgeColorHex = 0xFF10B981 // Emerald Green
    ),
    BITCHORD(
        shortName = "BitChord",
        repoAuthor = "kushagrasinghx/BitChord",
        description = "InnerTubeX Client Rotation (ANDROID_VR, Quest 3, Opus/AAC Direct Streams)",
        badgeColorHex = 0xFF6366F1 // Indigo
    ),
    DA_TUNES(
        shortName = "DA-Tunes",
        repoAuthor = "VikrantRuhela/DA-Tunes",
        description = "Distributed Stream Manifest Resolver (Multi-Instance Failover)",
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    OBSIDIAN(
        shortName = "Obsidian",
        repoAuthor = "cr7pt0gr4ph7/obsidian",
        description = "Direct Web Controller (m.youtube.com HTML5, Zero Embed Restrictions)",
        badgeColorHex = 0xFFEC4899 // Pink
    );

    val repoTag: String
        get() = repoAuthor
}
