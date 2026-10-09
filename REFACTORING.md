# AuraMusic Refactoring Plan & Progress Log

## Phase 2 Duplicate Code Audit Report

| File | Function / Class | Duplicate Of | Similarity | Recommended Action | Status |
|---|---|---|---|---|---|
| `HomeScreen.kt:775` | `SongOptionsDialog` | `MainActivity.kt:915` (`SongOptionsDialog`) | 95% | Extract to reusable `ui/component/SongOptionsDialog.kt` and use across all screens. | **RESOLVED** |
| `MainActivity.kt:1761` | `MadeForYouCard` | `LibraryScreen.kt:1214` (`MadeForYouCard`) | 100% | Extract to `ui/component/CommonLibraryComponents.kt`. | **RESOLVED** |
| `MainActivity.kt:1786` | `EmptyLibrary` | `LibraryScreen.kt:1303` (`EmptyLibrary`) | 100% | Extract to `ui/component/CommonLibraryComponents.kt`. | **RESOLVED** |
| `MainActivity.kt:1571` | `PlaylistRail` | `LibraryScreen.kt:1253` (`PlaylistRail`) | 92% | Extract to `ui/component/CommonLibraryComponents.kt`. | **RESOLVED** |
| `MainActivity.kt:625, 685, 702`, `PlaybackService.kt:206` | Inline `OnlineSong -> Song` mappings | Repeated manual instantiation | 95% | Create extension functions `OnlineSong.toSong()` and `SongLyrics.toLyricLines()`. | **RESOLVED** |
| `online/ui/components/OnlineMiniPlayer.kt` | `OnlineMiniPlayer` | `ui/component/MiniPlayer.kt` (`MiniPlayer`) | 85% | Remove dead `OnlineMiniPlayer.kt`; unified `MiniPlayer.kt` handles both offline and online. | **RESOLVED** |
| `online/ui/components/FullPlayerModal.kt` | `FullPlayerModal` | `ui/screen/PlayerScreen.kt` (`PlayerScreen`) | 80% | Remove dead `FullPlayerModal.kt`; unified `PlayerScreen.kt` handles both offline and online. | **RESOLVED** |
| `LyricsRepository.kt:27` | `parseLine` with `timeTagRegex` | `LrcParser.kt:18` (`parse`) | 85% | Delegate LRC parsing in `LyricsRepository` to `LrcParser`. | Analyzed |
| `PlayerViewModel.kt:592` | Inline Bitmap decode | `ArtworkExtractor.kt` & `PlaybackNotificationManager.kt` | 90% | Consolidate artwork decoding inside `ArtworkExtractor`. | Retained for performance |

---

## Phase 3 Architectural Problem Ratings & Status

| Rating | Problem | Location | Impact | Status |
|---|---|---|---|---|
| **CRITICAL** | God Activity (2,149 LOC) with inline screens, dialogs, and navigation | `MainActivity.kt` | Unmaintainable, violates Single Responsibility Principle. | **RESOLVED** (Trimmed to 892 LOC, sub-screens & dialogs extracted) |
| **CRITICAL** | Oversized Screen (1,989 LOC) mixing catalog, detail screens, and redundant UI | `LibraryScreen.kt` | Difficult to debug and maintain. | **RESOLVED** (Trimmed to 1,416 LOC, sub-screens extracted) |
| **HIGH** | Scattered model conversion (`OnlineSong` <-> `Song`) | `MainActivity.kt`, `PlaybackService.kt` | Bugs when fields are added or changed in one place but not another. | **RESOLVED** (`OnlineSongExtensions.kt` single source of truth) |
| **MEDIUM** | ViewModels manually instantiating repositories without Hilt DI | `MusicViewModel.kt`, `PlayerViewModel.kt` | Untestable code, tight coupling. | **RESOLVED** (Dagger Hilt `@Singleton` providers & `@Inject constructor`) |
| **MEDIUM** | Dead UI classes lingering in repo | `FullPlayerModal.kt`, `OnlineMiniPlayer.kt` | Codebase bloat and developer confusion. | **RESOLVED** (Safely deleted) |
| **LOW** | Deprecated API usages (Icons, Divider) | `LibraryScreen.kt`, `PlayerScreen.kt` | Minor compiler warnings. | **RESOLVED** (All auto-mirrored icons updated, zero warnings) |

---

## Completed Refactoring Stages & Metrics

### Stage 1: Core Extension Mappings & Dead Code Removal (Completed)
- **New Files**:
  - `app/src/main/java/com/aman/auramusic/online/model/OnlineSongExtensions.kt`: Added `OnlineSong.toSong()` and `SongLyrics.toLyricLines()`.
  - `app/src/main/java/com/aman/auramusic/ui/component/SongOptionsDialog.kt`: Consolidated dialog from `HomeScreen.kt` and `MainActivity.kt`.
- **Deleted Dead Files**:
  - `online/ui/components/FullPlayerModal.kt` (389 LOC deleted)
  - `online/ui/components/OnlineMiniPlayer.kt` (171 LOC deleted)
- **Results**: Verified with `BUILD SUCCESSFUL in 21s`.

### Stage 2: God Activity Extraction (Completed)
- **Extracted Files**:
  - `app/src/main/java/com/aman/auramusic/ui/screen/CollectionDetailScreen.kt` (~550 LOC extracted)
  - `app/src/main/java/com/aman/auramusic/ui/component/BottomNavBar.kt` (`BottomNavBar` + `AppTab` enum, ~180 LOC extracted)
  - `app/src/main/java/com/aman/auramusic/ui/component/PlaylistDialogs.kt` (5 dialogs extracted: `AddToPlaylistDialog`, `ExportPlaylistDialog`, `ImportResultDialog`, `NewPlaylistDialog`, `RenamePlaylistDialog`)
  - `app/src/main/java/com/aman/auramusic/ui/component/CommonLibraryComponents.kt` (`MadeForYouCard`, `EmptyLibrary`, `PlaylistRail`, `PlaylistPreviewCard`)
- **Metrics**: `MainActivity.kt` reduced from **2,149 LOC down to 892 LOC** (-58.5% reduction).
- **Results**: Verified with `BUILD SUCCESSFUL in 15s`.

### Stage 3: LibraryScreen Sub-screens & Redundancy Extraction (Completed)
- **Extracted Files**:
  - `app/src/main/java/com/aman/auramusic/ui/screen/AllPlaylistsScreen.kt` (~254 LOC extracted)
  - `app/src/main/java/com/aman/auramusic/ui/screen/OnlinePlaylistDetailScreen.kt` (~264 LOC extracted)
- **Cleanup**: Removed unused duplicate declarations (`MadeForYouCard`, `PlaylistRail`, `EmptyLibrary`, `OnlinePlaylistSongRow`).
- **Metrics**: `LibraryScreen.kt` reduced from **1,989 LOC down to 1,416 LOC** (-28.8% reduction).
- **Results**: Verified with `BUILD SUCCESSFUL in 12s`.

### Stage 4: Dagger Hilt Dependency Injection (Completed)
- **Enhanced Module**:
  - `app/src/main/java/com/aman/auramusic/di/AppModule.kt`: Added `@Singleton` providers for `MusicRepository`, `UserPreferencesRepository`, `LyricsRepository`, and `OnlineMusicRepository`.
- **Updated ViewModels**:
  - `MusicViewModel.kt`: Refactored constructor to inject `MusicRepository` and `UserPreferencesRepository`.
  - `PlayerViewModel.kt`: Refactored constructor to inject `LyricsRepository`, `UserPreferencesRepository`, and `MusicRepository`.
- **Deprecations Fixed**:
  - Replaced deprecated `Icons.Default.*` with `Icons.AutoMirrored.Filled.*` across `LibraryScreen.kt` and `PlayerScreen.kt`.
- **Results**: Zero compiler warnings, verified with `BUILD SUCCESSFUL in 8s`.

### Stage 5: Full Assembly Verification (Completed)
- Verified with `.\gradlew.bat assembleDebug`:
  - Dexing, bytecode transformations (ASM), Hilt code-generation, and packaging completed cleanly.
  - **Status**: `BUILD SUCCESSFUL in 51s`.

---

### Stage 6: Unified UI/UX Redesign & Aura Design System (Completed)

- **Aura Design System Tokens**:
  - `ui/theme/Color.kt`: Established Obsidian Void dark base (`#090A0F`), crisp light base (`#F7F8FC`), signature Electric Rose primary (`#FF2D55`), Hi-Fi Cyan (`#00E5FF`), and translucent glass borders.
  - `ui/theme/Type.kt`: Implemented modern editorial typography scale (`displayLarge`, `headlineLarge`, `titleLarge`, `bodyLarge`, `labelMedium`).
  - `ui/theme/Theme.kt`: Complete Dark and Light color schemes with dynamic system palette switching.

- **Universal Reusable Music Components**:
  - `ui/component/SongArtwork.kt`: Built `AuraArtwork` with cached image loading, smooth fallback gradients, and elevation shadows.
  - `ui/component/SongRow.kt`: Universal master track component with overloaded `OnlineSong` conversion, active animated equalizer indicator, track numbering, duration formatting, and contextual menu actions.
  - `ui/component/AlbumCard.kt`: Reusable square album artwork card with subtle title and artist typography.
  - `ui/component/ArtistCard.kt`: Reusable circular artist avatar card with luminous accent ring.
  - `ui/component/PlaylistCard.kt`: Reusable playlist card with track count badge and deep gradient vignette.
  - `ui/component/SectionHeader.kt`: Editorial section header with eyebrow, title, and action buttons.
  - `ui/component/AuraStates.kt`: Unified `AuraEmptyState`, `AuraLoadingState`, and `AuraErrorState` for empty, loading, and error states.

- **Screen Redesigns**:
  - `HomeScreen.kt`: Editorial music hub with personalized greeting, hero stations carousel, Continue Listening carousel (`AlbumCard`), Featured Albums (`AlbumCard`), Top Artists (`ArtistCard`), and Favorite Tracks (`SongRow`).
  - `OnlineScreen.kt`: High-fidelity discovery area with catalog source selector pills, curated playlists carousel (`PlaylistCard`), quick vibe chips, and universal `SongRow` track items.
  - `LibraryScreen.kt`: Categorized collection hub with segmented chips (`Songs` | `Albums` | `Artists` | `Playlists`), quick Play All & Shuffle bar, adaptive grids for albums and artists, and custom + curated online playlists, retaining 24-hour cache and playlist drilldowns.
  - `SearchScreen.kt`: Unified local and online search utilizing `SongRow` for both sources, `SearchMediaRow` for albums/artists, and `AuraEmptyState`.
  - `MiniPlayer.kt`: Modernized with non-deprecated lambda progress indicator and Aura glass border tokens.

- **Results**: Verified with `.\gradlew.bat compileDebugKotlin` (0 errors, 0 warnings) and `.\gradlew.bat assembleDebug` (`BUILD SUCCESSFUL in 29s`).

