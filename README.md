# Aura Music

<p align="center">
  <img src="app/src/main/res/drawable/aura_logo.png" alt="Aura Music Logo" width="128" />
</p>

<p align="center">
  <b>A modern Android audio player combining local high-fidelity playback with online streaming discovery.</b><br />
  Built with Kotlin and Jetpack Compose, featuring an Apple Music-inspired flat monochrome design system,<br />
  dual-engine audio architecture (LibVLC & AndroidX Media3 ExoPlayer), synchronized lyrics, and unified library management.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Branch-Exp01-blue?style=flat-square" alt="Branch Exp01" />
  <img src="https://img.shields.io/badge/Version-1.0.3--beta%20(Code%2016)-informational?style=flat-square" alt="Version 1.0.3-beta" />
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%E2%80%9336)-success?style=flat-square" alt="Android API 26-36" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blueviolet?style=flat-square" alt="Kotlin 2.0.21" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=flat-square" alt="GPL-3.0 License" />
</p>

---

## Table of Contents

- [Overview](#overview)
- [Interface & Screenshot Plan](#interface--screenshot-plan)
- [Features](#features)
  - [Monochrome Interface & Appearance](#monochrome-interface--appearance)
  - [Local Music Library (LibVLC)](#local-music-library-libvlc)
  - [Online Streaming & Discovery (ExoPlayer)](#online-streaming--discovery-exoplayer)
  - [Unified Library, Favorites & Playlists](#unified-library-favorites--playlists)
  - [Synchronized Lyrics & Device Integrations](#synchronized-lyrics--device-integrations)
  - [Artwork Resolution Optimization](#artwork-resolution-optimization)
  - [Playback Controls, Queue & Background Service](#playback-controls-queue--background-service)
  - [Utilities & Privacy](#utilities--privacy)
- [Technology Stack](#technology-stack)
- [Project Architecture](#project-architecture)
- [Requirements & Installation](#requirements--installation)
  - [Prerequisites](#prerequisites)
  - [Building from Source](#building-from-source)
  - [Installing via ADB](#installing-via-adb)
- [Development & Testing](#development--testing)
- [Versioning & Release Information](#versioning--release-information)
- [Limitations & Troubleshooting](#limitations--troubleshooting)
- [Contributing](#contributing)
- [License & Credits](#license--credits)

---

## Overview

Aura Music is an open-source Android music player developed to provide a unified listening experience across local offline files and online music catalogs. Rather than switching between a barebones offline player and proprietary streaming apps, Aura Music brings both together under a clean, cohesive, Apple Music-inspired flat monochrome interface.

The application uses two specialized audio engines:
1. **LibVLC Engine**: Manages local audio files directly on your device storage, supporting standard formats as well as lossless codecs (such as FLAC, WAV, and ALAC) with native playback handling.
2. **AndroidX Media3 ExoPlayer Engine**: Manages online audio streaming from multi-source catalog backends (including YouTube Music and JioSaavn endpoints) with HTTP caching and network error resilience.

---

## Interface & Screenshot Plan

Aura Music centers on a clean, four-tab navigation model (`Home`, `Online`, `Library`, `Search`) paired with an expandable bottom mini-player and full-screen player sheet.

> **Note on Visual Assets**: Screenshots for the current `BETA` release are currently being prepared. To maintain documentation integrity, no placeholder or external mockup images are linked. 

### Core Screen Layout Overview

| Screen | Key Responsibilities | Primary UI Components |
|---|---|---|
| **Home** | Daily featured releases, quick mixes, trending tracks, and recent history. | Dynamic banner cards, horizontal artist rails, quick pick rows. |
| **Online (Explore)** | Catalog discovery, genre playlists, chart listings, and artist discographies. | Multi-section feeds, curated playlist grids, artist profile sheets. |
| **Library** | Unified management of local tracks, playlists, folders, and favorites. | Category filter chips (`Songs`, `Artists`, `Albums`, `Playlists`, `Folders`, `Favorites`), batch actions. |
| **Search** | Integrated search across local device storage and online streaming catalogs. | Live query autocomplete, scope filter chips, search history tags. |
| **Player Sheet** | Full playback interface, real-time lyrics, queue reordering, and audio settings. | Album art carousel, synced LRCLIB lyrics pane, drag-and-drop queue, sleep timer. |

---

## Features

### Monochrome Interface & Appearance
* **Apple Music-Inspired Flat Aesthetics**: Replaces ornate glass and blur effects with a clean, minimalist monochrome design using crisp borders, elevated surfaces, and modern typography.
* **Strict Semantic Color Palette**: Uses a single source of truth monochrome system for all UI chrome:
  * **Light Theme**: Background (`#FFFFFF`), Surface (`#F5F5F5`), Secondary Surface (`#EEEEEE`), Pressed Surface (`#E5E5E5`), Primary Text/Icons (`#000000`), Secondary Text (`#666666`), Tertiary Text (`#888888`), Subtle Border (`#D9D9D9`), Strong Border (`#BFBFBF`), Selected Control (Black with White text/icons).
  * **Dark Theme**: Background (`#000000`), Surface (`#111111`), Secondary Surface (`#1A1A1A`), Pressed Surface (`#262626`), Primary Text/Icons (`#FFFFFF`), Secondary Text (`#AAAAAA`), Tertiary Text (`#808080`), Subtle Border (`#333333`), Strong Border (`#555555`), Selected Control (White with Black text/icons).
* **Theme & Appearance Modes**: Supports System Default, Light Theme, and Dark Theme with pure pitch-black (`#000000`) backgrounds.
* **Artwork Fidelity**: Genuine album artwork, artist photographs, and downloaded media artwork retain their authentic colors and are never altered or desaturated.
* **Artwork-Driven Ambient Player**: The full-screen player extracts soft ambient color swatches from active track artwork via `androidx.palette` for the player backdrop, while playback controls, menus, sheets, and typography remain strictly monochrome.

### Local Music Library (LibVLC)
* **Local MediaStore Scanner**: Queries audio content from device storage with permissions handling for Android 13+ (`READ_MEDIA_AUDIO`) and older releases (`READ_EXTERNAL_STORAGE`).
* **Broad Codec Support**: Decodes lossy and lossless formats supported by LibVLC, including **FLAC**, **WAV**, **ALAC**, **MP3**, **AAC**, **OGG**, and **OPUS**.
* **Folder Navigation**: Browse music organized by physical file directories in addition to ID3 tag metadata.
* **File Association**: Registered with an Android intent filter (`ACTION_VIEW`, `audio/*`), allowing you to open audio files directly from third-party file managers and messengers.

### Online Streaming & Discovery (ExoPlayer)
* **Multi-Service Catalog**: Integrates search and track resolution across YouTube Music (via InnerTube / Piped endpoints) and JioSaavn APIs.
* **ExoPlayer Streaming**: Streams audio via AndroidX Media3 ExoPlayer with a custom `CacheDataSourceFactory` for local cache persistence.
* **Artist Discographies**: View comprehensive artist profiles, top songs, studio albums, and singles.
* **Network Diagnostics**: Built-in streaming diagnostics sheet displaying stream URL, bitrate, content type, and connection latency.

### Unified Library, Favorites & Playlists
* **Dedicated Favorites Section**: A dedicated **Favorites** tab in the Library merges favorited local files and saved online streaming songs into a single actionable collection with **Play All** and **Shuffle**.
* **Persistent Online Favorites**: Online tracks marked as favorite are stored in DataStore and re-resolved on playback.
* **Clean Collection Separation**: Saved online playlists and albums remain distinct from user-created custom local playlists to avoid clutter.
* **Playlist Portability**: Export and import custom playlists to JSON format (`.aura` / `.json`) for device migration.

### Synchronized Lyrics & Device Integrations
* **Real-Time Synchronized Lyrics**: Fetches timestamped LRC lyrics from [LRCLIB](https://lrclib.net/) with active line highlight tracking and tap-to-seek synchronization.
* **Fuzzy Metadata Matching**: Normalizes artist and title queries to locate lyrics even when track titles contain extra tag descriptors (e.g., "(Remastered)", "[Official Audio]").
* **Status Bar Lyrics Bridge (ColorOS / RealmeUI)**: Broadcasts lyric payloads via `io.github.andrealtb.lockscreenlyrics.OPLUS_MEDIA_HISTORY` for supported devices with status bar or lockscreen lyric display capabilities.

### Artwork Resolution Optimization
* **ArtworkQualityOptimizer**: Intercepts compressed thumbnail URLs and requests higher-resolution masters:
  * Upgrades Google/YouTube thumbnail dimensions (`=w60`, `=w120`, `=s88`) up to `=w1080-h1080-l90-rj`.
  * Replaces low-resolution JioSaavn image paths (`50x50`, `150x150`) with uncompressed `500x500` CDN cover art.
* **Disk & Memory Caching**: Uses Coil 2.7.0 with custom disk and memory cache policies.

### Playback Controls, Queue & Background Service
* **Foreground Service**: `PlaybackService` maintains background playback, handles lockscreen controls, and coordinates system audio focus (`AUDIOFOCUS_GAIN`, `AUDIOFOCUS_LOSS`).
* **MediaSession Notification**: Standard Android media notifications with play, pause, skip, and scrubber controls.
* **Interactive Queue**: Reorder upcoming tracks with drag-and-drop handles, remove items, or clear the queue.
* **Playback Modes**: Loop All, Repeat One, and Shuffle.

### Utilities & Privacy
* **Sleep Timer**: Schedule playback to stop automatically after 15, 30, 45, or 60 minutes.
* **No Telemetry or User Tracking**: No analytics frameworks, ad SDKs, or third-party tracking identifiers are included.
* **Local Storage Only**: User playlists, favorites, playback history, and configuration are persisted entirely on-device via AndroidX DataStore.

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Kotlin | `2.0.21` | Modern language features, Coroutines, StateFlow |
| **UI Framework** | Jetpack Compose | BOM `2024.10.01` | Declarative UI, animated transitions, custom modifiers |
| **Design System** | Jetpack Compose + Material 3 + Monochrome Theme | `1.3.1` (Compose M3) | Flat monochrome tokens, semantic palette, typography |
| **Offline Audio** | LibVLC Android SDK | `3.6.0-eap14` | Native audio decoding (FLAC, WAV, ALAC, etc.) |
| **Streaming Audio** | AndroidX Media3 ExoPlayer | `1.2.1` | Network streaming, HTTP DataSource caching |
| **Dependency Injection** | Dagger Hilt | `2.51.1` | ViewModels, Repositories, Application context |
| **Image Loading** | Coil Compose | `2.7.0` | Asynchronous image decoding, memory/disk cache |
| **Networking** | Retrofit 2 + OkHttp 3 | `2.9.0` / `4.12.0` | API clients, stream resolvers, JSON converters |
| **Data Persistence** | AndroidX DataStore Preferences | `1.1.1` | Reactive preference storage and playlist persistence |
| **Palette Extraction**| AndroidX Palette | `1.0.0` | Dominant color sampling from album art |
| **Build Tooling** | Android Gradle Plugin (AGP) | `8.13.2` | Gradle build system with KSP (`2.0.21-1.0.27`) |

---

## Project Architecture

Aura Music uses a layered architecture following the Model-View-ViewModel (MVVM) pattern with unidirectional data flow:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  Jetpack Compose · Material 3 · Monochrome Theme · ComponentActivity   │
│  MainActivity · HomeScreen · OnlineScreen · LibraryScreen · Search     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ UI Events / StateFlow
┌───────────────────────────────────▼────────────────────────────────────┐
│                             DOMAIN LAYER                               │
│  PlayerViewModel  ·  MusicViewModel  ·  HomeViewModel  · StateFlows    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Coroutine Scopes & Repository Calls
┌───────────────────────────────────▼────────────────────────────────────┐
│                           REPOSITORY LAYER                             │
│  MusicRepository · OnlineMusicRepository · UserPreferencesRepository   │
│  YouTubeArtistRepository · SaavnRepository · LyricsRepository          │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
┌───────────────────▼──────────────┐   ┌─────────────▼───────────────────┐
│           DATA SOURCES           │   │          AUDIO ENGINES          │
│ • Android MediaStore             │   │ • LibVLC Native Player (Local)  │
│ • AndroidX DataStore Preferences │   │ • Media3 ExoPlayer (Streaming)  │
│ • OkHttp / Retrofit (APIs)       │   │ • PlaybackService & MediaSession│
│ • LRCLIB API Client              │   │ • AudioFocusChangeListener      │
└──────────────────────────────────┘   └─────────────────────────────────┘
```

### Key Package Structure

```text
app/src/main/java/com/aman/auramusic/
├── AuraMusicApp.kt                 # Application class (@HiltAndroidApp)
├── MainActivity.kt                 # Single Activity entry point & navigation tabs
├── coloros/                        # ColorOS / RealmeUI status bar lyrics bridge
├── data/
│   ├── model/                      # Data models (Song, Playlist, AppSettings)
│   └── repository/                 # Local music, lyrics, and user preference repositories
├── di/                             # Dagger Hilt dependency injection modules
├── online/
│   ├── data/                       # Online caching helpers
│   ├── lyrics/                     # LRCLIB API models and client
│   ├── model/                      # Online models (OnlineSong, OnlinePlaylist)
│   ├── network/                    # YouTube, JioSaavn, and Piped network extractors
│   ├── player/                     # OnlinePlaybackManager & ExoPlayer orchestration
│   └── util/                       # ArtworkQualityOptimizer and stream utilities
├── playback/                       # PlaybackService, VlcPlayerManager, MediaNotification
├── ui/
│   ├── component/                  # Reusable UI controls (MiniPlayer, SongRow, Dialogs)
│   ├── screen/                     # Primary screens (Library, Player, Search, Settings)
│   │   └── home/                   # HomeScreen, HomeViewModel, and components
│   └── theme/                      # AuraDesign, Color, Theme, Type
└── viewmodel/                      # PlayerViewModel, MusicViewModel
```

---

## Requirements & Installation

### Prerequisites

* **Operating System**: Windows 10/11, macOS, or Linux.
* **Android Studio**: Android Studio Ladybug (2024.2+) or newer.
* **Java Development Kit**: JDK 17 or higher (configured in Android Studio / `JAVA_HOME`).
* **Android SDK**:
  * Minimum Supported SDK: **API 26** (Android 8.0 Oreo)
  * Target & Compile SDK: **API 36** (Android 15 / 16 preview)

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone -b BETA https://github.com/dev-aman-hear/AuraMusic.git
   cd AuraMusic
   ```

2. **Build the Debug APK**:
   * **Windows (PowerShell / Command Prompt)**:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   * **macOS / Linux**:
     ```bash
     ./gradlew assembleDebug
     ```

3. **Locate the Output APKs**:
   The project has ABI splits enabled in `app/build.gradle.kts`. Successful compilation generates split APKs in `app/build/outputs/apk/debug/`:
   * `app-arm64-v8a-debug.apk`: Recommended for most modern 64-bit Android smartphones.
   * `app-universal-debug.apk`: Contains all native ABIs (larger file size).
   * `app-armeabi-v7a-debug.apk`: For older 32-bit ARM hardware.
   * `app-x86_64-debug.apk`: For 64-bit Android emulators.

### Installing via ADB

With USB debugging enabled on your connected device:

* **Install modern 64-bit ARM APK**:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
  ```
* **Or install the universal APK**:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
  ```

---

## Development & Testing

### Running Unit Tests

Unit tests are located in `app/src/test/java/` and cover online playlist parsing (`OnlinePlaylistSongsTest`) as well as search filtering navigation logic (`SearchFilterNavigationTest`).

Run the unit tests via Gradle:

* **Windows**:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
* **macOS / Linux**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

*Verification*: All unit tests compile and execute cleanly against the current `BETA` codebase.

### Code Style & Compilation Check

To verify code compilation without building APK packages:

```bash
# Windows
.\gradlew.bat compileDebugKotlin

# macOS / Linux
./gradlew compileDebugKotlin
```

---

## Versioning & Release Information

* **Current Development Branch**: `Exp01`
* **Internal Application Version**: `1.0.3` (`versionCode = 16`)
* **Status**: Development & Beta Testing.
* **Changelog Highlights (v1.0.3)**:
  * **Dynamic & Smooth Bottom Navigation**: Fluid physics-based spring animations for tab switching, subtle animated selection capsule/indicator, dynamic icon scaling, and responsive feedback.
  * **Direct One-Tap Artist Saving**: Replaced the previous 3-dot dropdown menu in the Artist section with direct, state-aware bookmark buttons on artist cards and detail screens with animated transitions.
  * **Subtle Action Button Transparency**: Refined action and navigation buttons across Artist, Album, and Playlist sections to 75% transparency with fully opaque icons for an Apple Music-inspired look.
  * **Dynamic Artwork Player Colors**: Optional soft artwork-based ambient player background while maintaining strict monochrome aesthetics throughout the rest of the app.
  * **Code Audit & Cleanup**: Pruned dead code paths and unused imports across UI modules.
* **Changelog Highlights (v1.0.2)**:
  * **Daily Featured Playlist Badge Refinement**: Made the badge pill compact, fully rounded, and naturally sized with balanced monochrome palette tokens across dark and light modes.
  * **Concurrency & Safety Fixes**: Thread-safe playlist caching with `ConcurrentHashMap`, safe `MediaMetadataRetriever` release, and offloaded library queries.
* **Changelog Highlights (v1.0.1)**:
  * **Monochrome Design System Migration**: Completely removed Liquid Glass, frosted glass modifiers, custom shaders, and obsolete glass settings in favor of an Apple Music-inspired flat monochrome design system.
  * **Strict Semantic Color Palette**: Standardized light and dark themes with strict contrast boundaries, flat surfaces, and subtle borders.
  * **Dedicated Library Favorites**: Added a dedicated Favorites category tab in `LibraryScreen` with quick Play All and Shuffle actions.
  * **Online Song Favorites Persistence**: Favorited online songs now persist in DataStore and appear alongside local favorites.
  * **Refined Category Chips**: Category chips in the Library use clean, text-only styling without legacy accent badges.

> **Release Note**: Signed release builds require a configured `signing.properties` file in the project root containing `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. When no signing properties file is present, the release build type automatically falls back to debug signing.

---

## Limitations & Troubleshooting

* **Network Audio Availability**: Online streaming relies on external endpoints (YouTube Music, Piped instances, JioSaavn CDN). Network interruptions, regional Geo-blocking, or upstream API changes may occasionally cause track resolution errors.
* **Storage Permissions**: If local music does not appear on Android 13+, ensure that the **Music and audio** permission (`android.permission.READ_MEDIA_AUDIO`) is granted in device settings.
* **ColorOS / RealmeUI Lyrics**: The status bar lyrics bridge relies on vendor-specific system broadcasts (`io.github.andrealtb.lockscreenlyrics.OPLUS_MEDIA_HISTORY`) and is only functional on compatible ColorOS, OxygenOS, or RealmeUI devices.
* **System Overlay**: Aura Music uses an in-app player sheet and mini-player; it does not request `SYSTEM_ALERT_WINDOW` or draw floating overlays over other apps.

---

## Contributing

Contributions and feedback for the `BETA` branch are welcome:

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes with descriptive messages:
   ```bash
   git commit -m "feat: describe your change"
   ```
4. Verify that unit tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Push to your fork and submit a Pull Request to the `BETA` branch.

To report bugs or suggest enhancements, open an issue on the [GitHub Issues tracker](https://github.com/dev-aman-hear/AuraMusic/issues).

---

## License & Credits

* **License**: This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for the full text.
* **Author**: [Aman](https://github.com/dev-aman-hear)
* **Open Source Acknowledgements**:
  * [LibVLC Android](https://code.videolan.org/videolan/vlc-android) by VideoLAN
  * [AndroidX Media3 ExoPlayer](https://github.com/androidx/media) by Google
  * [Coil](https://github.com/coil-kt/coil) by Coil Contributors
  * [LRCLIB](https://lrclib.net/) by LRCLIB Contributors
  * [Android-YouTube-Player](https://github.com/PierfrancescoSoffritti/android-youtube-player) by Pierfrancesco Soffritti
