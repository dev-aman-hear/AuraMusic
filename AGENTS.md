# AuraMusic Android — AI Agent Development & Navigation Guide

## 1. Project Purpose & Verified Architecture
AuraMusic is a high-fidelity Android music player combining:
- **Offline / Local Audio Engine**: Lossless local playback powered by LibVLC (`org.videolan.android:libvlc-all`), reading local media store audio files and local `.lrc` files.
- **Online Discovery & Streaming Engine**: Direct 320kbps JioSaavn CDN audio playback via AndroidX Media3 ExoPlayer with disk caching (`CacheManager`), and headless YouTube Music streaming via RiPlay WebView IFrame wrapper (`YouTubeIFramePlayer`).
- **Synchronized Lyrics**: Millisecond-accurate live lyrics via LRCLIB online API and local `.lrc` files.
- **ColorOS / OPlus Live Lockscreen Bridge**: Proprietary live lockscreen lyrics broadcasting for OPPO, OnePlus, and Realme devices.
- **Presentation Architecture**: 100% Jetpack Compose using Material 3 and a custom Frosted Glassmorphism design system (`AuraDesign.kt`, `GlassUtils.kt`), structured with Clean Architecture + MVVM + Dagger Hilt dependency injection.

---

## 2. Technical Stack & SDK Specifications
- **Programming Language**: Kotlin (v2.0+, `jvmTarget = "11"`)
- **Android SDK Configuration**:
  - `compileSdk = 36`
  - `targetSdk = 36`
  - `minSdk = 26` (Android 8.0 Oreo)
  - `versionCode = 12`, `versionName = "3.2.0"`
- **Build System**: Gradle 9.0 (AGP 8.x, KSP for annotation processing)
- **Primary Dependencies**:
  - **Offline Audio**: LibVLC Android (`3.6.0-eap14`)
  - **Online Audio**: AndroidX Media3 ExoPlayer (`1.2.1`), `media3-session`, `media3-datasource`, `media3-datasource-okhttp`
  - **DI**: Dagger Hilt (`2.51+`) with `hilt-navigation-compose`
  - **UI**: Jetpack Compose BOM, Material 3, Material Icons Extended, AndroidX Palette
  - **Image Loading**: Coil Compose (`2.7.0`)
  - **Persistence**: Jetpack DataStore Preferences (`1.1.1`)
  - **Networking**: OkHttp (`4.12.0`), Retrofit (`2.9.0`), Gson (`2.10.1`)

---

## 3. Fast Code Navigation & Component Index
**Do not spend tokens scanning unrelated folders.** The project includes a machine-readable index at:
```
docs/architecture/COMPONENT_INDEX.json
```
and a zero-dependency CLI query tool at:
```
python tools/project_index.py <command>
```

### Essential Navigation Commands
```bash
# Search components by keyword, symbol, or filename
python tools/project_index.py search playback
python tools/project_index.py search VlcPlayerManager
python tools/project_index.py search lyrics

# Inspect a component, its dependencies, dependents, source files, and tests
python tools/project_index.py show playback-service
python tools/project_index.py show online-playback-manager

# Validate index schema and dependency integrity
python tools/project_index.py validate

# Verify that all indexed file paths exist on disk
python tools/project_index.py check-paths

# View architecture statistics and breakdown
python tools/project_index.py stats
```

---

## 4. Mandatory Code Navigation Workflow
When assigned any development, feature, or bugfix task:
1. **Consult the Index First**: Run `python tools/project_index.py search <keyword>` or query `docs/architecture/COMPONENT_INDEX.json` to find relevant component IDs and paths.
2. **Review Verified Architecture**: Consult `docs/architecture/PROJECT_MAP.md` to see data flows and component relationships.
3. **Open Only Relevant Files**: Open the target source file(s) and their immediate dependencies. Do not perform wide directory traversals.
4. **Inspect Existing Tests**: Check `related_tests` in the component index or run unit tests under `app/src/test/java/com/aman/auramusic/`.
5. **Form a Focused Plan**: Plan surgical, minimal edits.
6. **Implement with Restraint**: Modify only what is needed. Do not refactor surrounding code or reformat files arbitrarily.
7. **Run Verified Verification Commands**:
   ```bash
   # Compile Kotlin sources
   ./gradlew.bat compileDebugKotlin

   # Run automated regression and logic unit tests
   ./gradlew.bat testDebugUnitTest

   # Assemble debug APK (when verifying packaging)
   ./gradlew.bat assembleDebug
   ```
8. **Update Documentation**: If you add/modify components or solve known issues, update `docs/architecture/COMPONENT_INDEX.json`, `docs/architecture/KNOWN_ISSUES.md`, and `docs/architecture/CHANGELOG.md`.

---

## 5. Coding Conventions & Architectural Guidelines
- **Dual Playback Coordination**:
  - `PlaybackService` is the single source of truth for the Android system `MediaSessionCompat` and foreground media notification.
  - When playing offline music, `PlaybackService` commands `VlcPlayerManager` (LibVLC).
  - When playing online music, `OnlinePlaybackManager` drives ExoPlayer/RiPlay, while forwarding state to `PlaybackService` (`startOnlinePlayback`, `updateOnlinePlaybackState`) to keep the notification and lockscreen controls in sync.
- **Foreground Lifecycle Safety**:
  - `startForeground` must be called immediately with local/cached artwork (`loadArtworkFast`) before launching background network calls (such as LRCLIB lyrics or high-res artwork). Never delay `startForeground` behind network I/O.
  - ViewModels must NEVER call `stopForeground` or `stopAsForeground`. `PlaybackService` manages its own foreground lifecycle.
  - Media unloading transitions must be guarded by `isTransitioning = true` so transient `Stopped` or `Paused` events do not trigger full session teardown.
- **Diagnostic Logging**:
  - Always use the structured tag `AuraPlaybackDebug` for playback lifecycle, queue transitions, audio focus changes, and service state transitions.
- **Design & UI**:
  - All UI is built using Jetpack Compose with custom glassmorphism utilities (`AuraDesign.kt`, `GlassUtils.kt`).
  - Maintain the existing dark/AMOLED glass aesthetic. Do not introduce raw Material 2 or unstyled views.

---

## 6. Prohibited Practices
- **NO Unnecessary Refactoring**: Do not restructure packages or rename classes unless specifically requested.
- **NO Duplicate Implementations**: Do not write new players, cache managers, or network extractors. Inspect existing implementations in `online/` and `playback/`.
- **NO Premature External Dependencies**: Do not add third-party libraries without explicit justification.
- **NO Polling Commands in Agent Turns**: Do not poll background tasks with loops. Allow the system reactive message wake-ups to notify upon completion.
- **NO Stale Index**: If you discover a path or dependency discrepancy, update `docs/architecture/COMPONENT_INDEX.json` and verify with `python tools/project_index.py validate`.
