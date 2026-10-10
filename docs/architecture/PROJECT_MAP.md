# AuraMusic Architecture Map & System Blueprint

## 1. Module & Source Tree Hierarchy

AuraMusic is organized as a single-module Android application (`:app`) following Clean Architecture + MVVM principles:

```
app/src/main/java/com/aman/auramusic/
├── AuraMusicApp.kt                      // @HiltAndroidApp application entrypoint
├── MainActivity.kt                      // Main Compose Activity coordinator
│
├── coloros/                             // ColorOS/OPlus lockscreen live lyrics integration
│   ├── ColorOSBridgeConfig.kt           // Target packages, broadcast actions & intent extras
│   ├── ColorOSLiveLyricsBridge.kt       // Live lyrics state broadcaster & seek observer
│   └── ColorOSLyricPayload.kt           // JSON payload formatter for OPlus lyric engines
│
├── data/                                // Local data layer
│   ├── model/                           // Core domain models
│   │   ├── AppSettings.kt               // Playback & appearance user preferences data class
│   │   ├── LyricLine.kt                 // Timestamped lyric line model (timeMs, text)
│   │   ├── PlaybackHistoryEntry.kt      // History record with song ID and played timestamp
│   │   ├── Playlist.kt                  // User-created playlist with list of song IDs
│   │   ├── Song.kt                      // Domain song entity (id, title, artist, uri, filePath, etc.)
│   │   └── ThemeMode.kt                 // Theme preference enum (SYSTEM, DARK, LIGHT)
│   └── repository/
│       ├── LyricsRepository.kt          // Local .lrc file search & fallback to LRCLIB
│       ├── MusicRepository.kt           // Android MediaStore audio queries & call-recording filter
│       └── UserPreferencesRepository.kt // Jetpack DataStore Preferences for settings, playlists, favorites
│
├── di/                                  // Dependency injection
│   └── AppModule.kt                     // Dagger Hilt module (OkHttpClient, Gson, singletons)
│
├── online/                              // Online discovery & streaming subsystem
│   ├── data/
│   │   └── LibraryOnlineCache.kt        // 24-hour disk JSON cache for online playlists and feeds
│   ├── filter/
│   │   ├── TrendingFilterConfig.kt      // Blacklist keywords & minimum duration criteria
│   │   └── TrendingSongFilter.kt        // Noise cleaner removing non-music YouTube items
│   ├── lyrics/
│   │   ├── model/LyricsModels.kt        // Online lyrics data models & conversion helpers
│   │   ├── network/LrclibService.kt     // REST client querying LRCLIB online API
│   │   └── parser/LrcParser.kt          // Millisecond LRC string parser
│   ├── model/
│   │   ├── AudioSource.kt               // Source enum (LOCAL, JIOSAAVN, YOUTUBE, SPOTIFY)
│   │   ├── OnlinePlaylist.kt            // Online playlist entity
│   │   ├── OnlineSong.kt                // Online track entity with stream URL and metadata
│   │   ├── OnlineSongExtensions.kt      // Conversion bridges (OnlineSong <-> Song)
│   │   ├── PlaybackState.kt             // Online player state container
│   │   └── StreamingEngine.kt           // YouTube strategy enum (RIPLAY, BITCHORD, DA_TUNES, OBSIDIAN)
│   ├── network/
│   │   ├── crypto/DesDecryptor.kt       // DES decryption for JioSaavn CDN audio URLs
│   │   ├── extractor/DualTierStreamExtractor.kt // Multi-source failover stream extractor
│   │   ├── piped/PipedService.kt        // Decentralized Piped YouTube REST service
│   │   ├── repository/OnlineMusicRepository.kt // Unified online catalog & stream resolver
│   │   ├── repository/YouTubeArtistRepository.kt // YouTube Music artist discography extractor
│   │   └── saavn/JioSaavnService.kt     // JioSaavn REST search & media endpoint client
│   ├── player/
│   │   ├── CacheManager.kt              // AndroidX Media3 SimpleCache with LRU disk eviction
│   │   ├── LyricsManager.kt             // Active track lyrics coordinator
│   │   ├── OnlinePlaybackManager.kt     // Master online audio orchestrator (ExoPlayer + RiPlay)
│   │   └── YouTubeIFramePlayer.kt       // Headless WebView YouTube IFrame Player wrapper
│   ├── ui/
│   │   ├── OnlineColors.kt              // Curated gradients and color accents for online feed
│   │   ├── OnlineSearchSection.kt       // Real-time online search results composable
│   │   └── components/                  // Online UI cards and sheets
│   └── util/
│       ├── ArtworkQualityOptimizer.kt   // Thumbnail-to-master high-res artwork URL upscaler
│       └── LastPlayedStore.kt           // State persistence for last played track & position
│
├── playback/                            // Local audio playback & system media integration
│   ├── PlaybackActionReceiver.kt        // Broadcast receiver handling notification intents
│   ├── PlaybackActionRegistry.kt        // Shared lambda registry for playback actions
│   ├── PlaybackNotificationManager.kt   // MediaStyle notification builder with fast artwork decoding
│   ├── PlaybackService.kt               // Sticky foreground Service & MediaSessionCompat host
│   └── VlcPlayerManager.kt              // LibVLC player engine, audio focus, & transition guard
│
├── ui/                                  // Presentation layer (Jetpack Compose)
│   ├── component/                       // Reusable UI components
│   │   ├── AlbumCard.kt                 // Square album card with adaptive art
│   │   ├── ArtistCard.kt                // Circular artist avatar with accent ring
│   │   ├── AuraStates.kt                // Empty, loading, and error state layouts
│   │   ├── BottomNavBar.kt              // Floating capsule tab navigation bar
│   │   ├── CommonLibraryComponents.kt   // MadeForYouCard, PlaylistRail, EmptyLibrary
│   │   ├── MiniPlayer.kt                // Global floating miniplayer with swipe gestures
│   │   ├── PlaylistCard.kt              // Reusable playlist card with badge
│   │   ├── PlaylistDialogs.kt           // Create, Rename, Export, Import playlist dialogs
│   │   ├── SectionHeader.kt             // Section header with title and action button
│   │   ├── SongArtwork.kt               // High-performance Coil image loader with fallback
│   │   ├── SongOptionsDialog.kt         // Track context menu (queue, favorite, playlist)
│   │   └── SongRow.kt                   // Universal track row item (local & online)
│   ├── navigation/
│   │   └── Screen.kt                    // Sealed navigation destination classes
│   ├── screen/                          // Top-level feature screens
│   │   ├── AllPlaylistsScreen.kt        // Playlist grid & management view
│   │   ├── CollectionDetailScreen.kt    // Album, Artist, and Playlist detail view
│   │   ├── HomeScreen.kt                // Quick picks, favorites, & recent tracks
│   │   ├── LibraryScreen.kt             // Local songs, albums, artists, & playlists
│   │   ├── OnlinePlaylistDetailScreen.kt// Online playlist tracklist view
│   │   ├── OnlineScreen.kt              // Online charts, trending, & discovery
│   │   ├── PlayerScreen.kt              // Fullscreen player with live lyrics & queue
│   │   ├── SearchScreen.kt              // Unified local + online search
│   │   └── SettingsScreen.kt            // Equalizer, audio focus, AMOLED, & cache settings
│   └── theme/                           // Design system
│       ├── AuraDesign.kt                // Spacing, corner radius, elevation, and typography tokens
│       ├── Color.kt                     // Color definitions & gradients
│       ├── GlassUtils.kt                // Frosted glassmorphism modifiers (glassEffect, glassSurface)
│       ├── Theme.kt                     // AuraMusicTheme composable
│       └── Type.kt                      // Typography scale
│
├── util/                                // Common utilities
│   ├── ArtworkExtractor.kt              // Local embedded ID3 artwork extractor
│   └── Formatters.kt                    // Millisecond duration and date formatters
│
└── viewmodel/                           // Hilt ViewModels
    ├── MusicViewModel.kt                // Local library catalog, sorting, and playlist state
    └── PlayerViewModel.kt               // Playback coordinator, active queue, lyrics, and colors
```

---

## 2. Core Architectural Entry Points

| Entry Point | Source Path | Role |
|---|---|---|
| **Application** | `app/src/main/java/com/aman/auramusic/AuraMusicApp.kt` | Initializes Dagger-Hilt dependency graph. |
| **Activity** | `app/src/main/java/com/aman/auramusic/MainActivity.kt` | Hosts Compose Navigation, handles external audio `ACTION_VIEW` intents, and renders floating `MiniPlayer`. |
| **Foreground Service** | `app/src/main/java/com/aman/auramusic/playback/PlaybackService.kt` | Runs sticky foreground service with `mediaPlayback` type, owns system `MediaSessionCompat`, and publishes media notification. |
| **Broadcast Receiver** | `app/src/main/java/com/aman/auramusic/playback/PlaybackActionReceiver.kt` | Handles notification pending intents (`PLAY_PAUSE`, `NEXT`, `PREVIOUS`, `SEEK`). |

---

## 3. Data Flows for Major Features

### Feature A: Offline Local Audio Playback
```
MediaStore (Device Audio)
       │
       ▼
MusicRepository.getAllSongs() ──[Filters audio < 30s & call recordings]
       │
       ▼
MusicViewModel / LibraryScreen (User selects song)
       │
       ▼
PlayerViewModel.play(song)
       │
       ▼
PlaybackService.play(song)
       │
       ├──► 1. VlcPlayerManager.play(song.filePath)  ──► LibVLC Engine starts decoding
       │
       ├──► 2. startForeground() (Immediate)         ──► Notification attached with fast artwork (0ms)
       │
       ├──► 3. Background Metadata & Lyrics Fetch     ──► LRCLIB / local .lrc enriched asynchronously
       │
       └──► 4. ColorOSLiveLyricsBridge.onTrackChanged() ──► Broadcasts live state to OPlus lockscreen
```

### Feature B: Online Streaming & Dual-Tier Audio Playback
```
User Search / Online Catalog
       │
       ▼
OnlineMusicRepository.searchSongs() / getTrending()
       │
       ├── JioSaavn: JioSaavnService ──► DesDecryptor ──► 320kbps MP4 CDN URL
       └── YouTube:  PipedService / DualTierExtractor ──► Stream Manifest / YouTube ID
       │
       ▼
OnlinePlaybackManager.playSong(onlineSong)
       │
       ├── If JioSaavn: AndroidX Media3 ExoPlayer plays CDN stream with SimpleCache (CacheManager)
       └── If YouTube:  RiPlay YouTubeIFramePlayer (Headless WebView) plays official stream
       │
       ▼
Bridges to PlaybackService:
  • startOnlinePlayback() / updateOnlinePlaybackState()
  • Synchronizes MediaSessionCompat & Notification with active online track
```

### Feature C: Natural Track Transition & Queue Advancement
```
Current Track Finishes Naturally
       │
       ├── Offline: VlcPlayerManager receives MediaPlayer.Event.EndReached
       └── Online:  ExoPlayer receives Player.STATE_ENDED / YouTube onSongEnded
       │
       ▼
isTransitioning = true  ──► Suppresses transient Stopped/Paused events from demoting service
       │
       ▼
playNext(isManual = false)
       ├── RepeatMode.ONE  ──► Replays current track
       ├── RepeatMode.ALL  ──► Wraps to track 0
       └── RepeatMode.NONE ──► Pauses cleanly at queue boundary
       │
       ▼
Next Track Plays Continuously
       • Notification remains pinned in foreground throughout transition
       • isTransitioning reset to false on Event.Playing / STATE_READY
```

### Feature D: ColorOS / OPlus Live Lockscreen Lyrics
```
PlaybackService Progress Observer
       │
       ▼
ColorOSLiveLyricsBridge.onPlaybackStateChanged(song, isPlaying, positionMs)
       │
       ▼
ColorOSLyricPayload.generateLyricInfoJson(song, lyrics)
       │
       ├── Sets MediaMetadataCompat extra: "lyricInfo"
       └── Broadcasts Intent: "io.github.andrealtb.lockscreenlyrics.UPDATE_LYRIC"
       │
       ▼
OPPO / OnePlus / Realme System UI displays floating live synchronized lyrics
```

---

## 4. How to Find Implementations of Common Features

| What are you looking for? | Where to look | Key Classes & Symbols |
|---|---|---|
| **Local audio playback engine** | `playback/` | `VlcPlayerManager.kt`, `PlaybackService.kt` |
| **Media notifications & lockscreen** | `playback/` | `PlaybackNotificationManager.kt` |
| **Audio focus handling** | `playback/` | `VlcPlayerManager.kt` (`audioFocusChangeListener`) |
| **Online streaming (ExoPlayer)** | `online/player/` | `OnlinePlaybackManager.kt`, `CacheManager.kt` |
| **YouTube Music streaming (RiPlay)** | `online/player/` | `YouTubeIFramePlayer.kt` |
| **Synchronized lyrics (LRC/LRCLIB)** | `online/lyrics/`, `data/repository/` | `LrclibService.kt`, `LrcParser.kt`, `LyricsRepository.kt` |
| **JioSaavn 320k stream resolution** | `online/network/` | `JioSaavnService.kt`, `DesDecryptor.kt` |
| **Local song queries & filtering** | `data/repository/` | `MusicRepository.kt` (`getAllSongs`) |
| **Playlists, settings & favorites** | `data/repository/` | `UserPreferencesRepository.kt` |
| **Full player UI with karaoke lyrics** | `ui/screen/` | `PlayerScreen.kt` |
| **Floating bottom miniplayer** | `ui/component/` | `MiniPlayer.kt` |
| **ColorOS live lyrics integration** | `coloros/` | `ColorOSLiveLyricsBridge.kt`, `ColorOSLyricPayload.kt` |
| **Glassmorphism design tokens** | `ui/theme/` | `AuraDesign.kt`, `GlassUtils.kt` |
