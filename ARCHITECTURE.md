# AuraMusic Architecture Map & Documentation

## 1. Executive Summary
AuraMusic is a modern Android music application featuring local playback (LibVLC), online catalog discovery & streaming (JioSaavn, YouTube Music, Spotify), synchronized lyrics display (LRCLIB & local .lrc), and ColorOS/OPlus lockscreen integration.

This document details the architectural state, the completed Clean Architecture + MVVM refactoring, package structures, and single source of truth guidelines.

---

## 2. Refactored Architecture & Package Structure

```
com.aman.auramusic/
├── AuraMusicApp.kt                  // @HiltAndroidApp application entrypoint
├── MainActivity.kt                  // Clean Coordinator Activity (reduced from 2,149 to 892 LOC)
│
├── coloros/                         // ColorOS/OPlus live lockscreen lyrics bridge
│   ├── ColorOSBridgeConfig.kt
│   ├── ColorOSLiveLyricsBridge.kt
│   └── ColorOSLyricPayload.kt
│
├── data/                            // Data Layer (Local)
│   ├── model/                       // Core Models (Song, Playlist, AppSettings, LyricLine, ThemeMode)
│   └── repository/
│       ├── MusicRepository.kt       // MediaStore audio queries & call-recording filtering
│       ├── UserPreferencesRepository.kt // Jetpack DataStore (settings, playlists, favorites, history)
│       └── LyricsRepository.kt      // Local .lrc file search & parsing
│
├── di/
│   └── AppModule.kt                 // Dagger Hilt Module providing OkHttpClient and Singleton Repositories
│
├── online/                          // Online Catalog & Streaming Feature
│   ├── data/
│   │   └── LibraryOnlineCache.kt    // Persistent 24h JSON cache for curated library feed
│   ├── lyrics/                      // Online synchronized lyrics (LRCLIB + LrcParser)
│   ├── model/                       // OnlineSong, OnlinePlaylist, AudioSource, PlaybackState
│   │   └── OnlineSongExtensions.kt  // Single Source of Truth for Model Conversions (OnlineSong <-> Song)
│   ├── network/                     // JioSaavn, Piped, DualTierExtractor, DES crypto
│   ├── player/                      // OnlinePlaybackManager (ExoPlayer + YouTube IFrame wrapper)
│   └── ui/                          // Online discovery UI components & sheets
│
├── playback/                        // Offline Playback (LibVLC)
│   ├── PlaybackService.kt           // Foreground MediaPlayback Service & MediaSessionCompat
│   ├── VlcPlayerManager.kt          // LibVLC player engine & audio focus
│   ├── PlaybackNotificationManager.kt // NotificationCompat builder & artwork decoding
│   ├── PlaybackActionReceiver.kt    // Intent broadcast receiver for notification actions
│   └── PlaybackActionRegistry.kt    // Global callback registry for playback actions
│
├── ui/                              // Presentation Layer (Jetpack Compose)
│   ├── component/                   // Universal Reusable Aura UI components
│   │   ├── AlbumCard.kt             // Adaptive square album card
│   │   ├── ArtistCard.kt            // Circular artist avatar with accent ring
│   │   ├── AuraStates.kt            // Unified Empty, Loading, and Error state components
│   │   ├── BottomNavBar.kt          // Floating capsule navigation bar & AppTab enum
│   │   ├── CommonLibraryComponents.kt // MadeForYouCard, PlaylistRail, EmptyLibrary
│   │   ├── MiniPlayer.kt            // Global offline & online miniplayer with gesture support
│   │   ├── PlaylistCard.kt          // Reusable square playlist card with badges
│   │   ├── PlaylistDialogs.kt       // AddToPlaylist, NewPlaylist, Rename, Export, Import dialogs
│   │   ├── SectionHeader.kt         // Editorial section header with eyebrow and actions
│   │   ├── SongArtwork.kt           // AuraArtwork high-performance image loading
│   │   ├── SongOptionsDialog.kt     // Reusable song context menu dialog
│   │   └── SongRow.kt               // Universal master track row component (local & online)
│   ├── navigation/                  // Screen sealed class
│   ├── screen/                      // Feature Screens
│   │   ├── AllPlaylistsScreen.kt    // Extracted custom playlist grid/list view
│   │   ├── CollectionDetailScreen.kt// Extracted Album, Artist, and Playlist detail view
│   │   ├── HomeScreen.kt            // Home recommendation feed
│   │   ├── LibraryScreen.kt         // Refactored Library screen with 24h caching
│   │   ├── OnlinePlaylistDetailScreen.kt // Extracted Online playlist tracklist view
│   │   ├── OnlineScreen.kt          // Online catalog discovery
│   │   ├── PlayerScreen.kt          // Full player screen (with lyrics, queue, quality, gestures)
│   │   ├── SearchScreen.kt          // Unified local and online search
│   │   └── SettingsScreen.kt        // Settings, equalizer, appearance, cache management
│   └── theme/                       // Theme, Color, Type, GlassUtils
│
├── util/                            // Helpers (ArtworkExtractor, Formatters)
└── viewmodel/                       // Hilt ViewModels: MusicViewModel, PlayerViewModel
```

---

## 3. Playback Architecture & Dual-Engine Reality

Currently, the application contains two playback subsystems coordinated through a single service:
1. **Offline Engine (VLC Player)**:
   - Handled by `PlaybackService` + `VlcPlayerManager` (LibVLC).
   - Runs in a sticky foreground service.
   - Connects to system `MediaSessionCompat` and system media controls.
2. **Online Engine (ExoPlayer & YouTube IFrame)**:
   - Handled by `OnlinePlaybackManager` (AndroidX Media3 ExoPlayer for JioSaavn 320k direct streams + WebView YouTube IFrame player for YT hits).
   - Bridged to `PlaybackService` via `startOnlinePlayback` / `updateOnlinePlaybackState` so notification controls and MediaSession stay active.

---

## 4. Architectural Problem Resolution Matrix

| Issue ID | Component | Problem Description | Resolution | Status |
|---|---|---|---|---|
| **ARCH-01** | `MainActivity.kt` | **God Activity (2,149 LOC)**: contained sub-screens, navigation bar, and dialogs. | Extracted `CollectionDetailScreen`, `BottomNavBar`, and `PlaylistDialogs`. Reduced to 892 LOC (-58.5%). | **RESOLVED** |
| **ARCH-02** | `LibraryScreen.kt` | **God Screen (1,989 LOC)**: mixed main catalog, detail screens, and redundant UI. | Extracted `AllPlaylistsScreen` and `OnlinePlaylistDetailScreen`. Removed duplicate UI cards. Reduced to 1,416 LOC (-28.8%). | **RESOLVED** |
| **ARCH-03** | UI & Playback | **Scattered Model Conversions**: `OnlineSong` manually mapped in 4 files. | Created `OnlineSongExtensions.kt` (`toSong()`, `toLyricLines()`) as single source of truth. | **RESOLVED** |
| **ARCH-04** | DI / Hilt | **Manual repository instantiation in ViewModels**. | Registered singletons in `AppModule.kt` and constructor-injected into `MusicViewModel` and `PlayerViewModel`. | **RESOLVED** |
| **ARCH-05** | UI Components | **Dead / Duplicated Components**: `FullPlayerModal.kt` (389 LOC) and `OnlineMiniPlayer.kt` (171 LOC). | Deleted dead components safely after reference validation. | **RESOLVED** |
| **ARCH-06** | UI Layer | **Deprecated API usages**: icons and dividers producing compiler warnings. | Replaced with `Icons.AutoMirrored.Filled.*` across screens. Zero warnings. | **RESOLVED** |

---

## 5. Single Source of Truth Principles

1. **Music Library**: `MusicRepository` (local) + `OnlineMusicRepository` (streaming).
2. **Playback Authority**: `PlaybackService` + `PlaybackActionRegistry` / `PlayerViewModel`.
3. **Queue**: `PlaybackService.queue` (unified).
4. **Playlists & Preferences**: `UserPreferencesRepository` (backed by Jetpack DataStore).
5. **Lyrics**: `LyricsRepository` leveraging `LrcParser` and `LrclibService`.
6. **Artwork**: `ArtworkExtractor` with LRU caching.
