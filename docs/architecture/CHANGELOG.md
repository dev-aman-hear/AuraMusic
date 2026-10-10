# AuraMusic Architectural Changelog

Records significant architectural evolution, design decisions, and system-level refactoring milestones.

---

## [3.2.0] — 2026-10-10

### Added
- **Project Intelligence System**:
  - Machine-readable component index at `docs/architecture/COMPONENT_INDEX.json` mapping 42 components, paths, symbols, and dependencies.
  - Zero-dependency CLI tool `tools/project_index.py` supporting `search`, `show`, `validate`, `check-paths`, `list`, and `stats`.
  - Architecture map at `docs/architecture/PROJECT_MAP.md` documenting subsystem data flows.
  - Reusable AI Agent workflow protocol at `docs/architecture/AI_WORKFLOW.md`.
  - Structured issue log at `docs/architecture/KNOWN_ISSUES.md`.
- **Apple-Inspired Liquid Glass Design System**:
  - Centralized visual engine (`LiquidGlassTokens.kt`, `GlassUtils.kt`) featuring translucent refraction gradients, specular illuminated top-rim highlights, feathered ambient drop shadows, and curved pill/capsule shapes matching the visual reference.
  - Dedicated **Appearance → Liquid Glass** customization engine in `SettingsScreen.kt` with live interactive preview, preset selector (`Classic`, `Liquid Glass`, `Crystal`, `Frosted`, `Custom`), fine-grain sliders (translucency, blur, rim highlight, shadow depth, corner roundness), surface tint palette, and accessibility toggles (`Reduce Transparency`, `Reduce Motion`).
  - Persistent typed configuration model (`LiquidGlassConfig.kt`) integrated into Jetpack DataStore preferences via `UserPreferencesRepository`.
  - Reusable components: `LiquidGlassSurface`, `LiquidGlassCard`, `LiquidGlassButton`, `LiquidGlassChip`, `LiquidGlassNavigationBar`, `LiquidGlassMiniPlayer`, `LiquidGlassDialog`, `LiquidGlassSheet`.
  - Comprehensive unit test suite in `LiquidGlassDesignSystemTest.kt` verifying preset parameters, token calculations, and accessibility fallbacks.
- **Apple Music-Inspired Liquid Glass Home Screen**:
  - Redesigned `HomeTestHeader` with dual-line greeting typography (`Good afternoon,` in primary text + username in `AppleMusicAccent` #FA2D48), uppercase date eyebrow (`SATURDAY, 10 OCTOBER`), liquid glass circular notification bell button with red badge dot, and circular coral profile button.
  - Upgraded `DailyFeaturedCard` with Apple Music layout: 24dp frosted glass card, 114dp high-res artwork, `DAILY` pill badge, `MoreHoriz` options menu, bold titles, coral track counter, and 42dp coral play button.
  - Redesigned `TrendingTrackCard` with 20dp corner radius, floating circular translucent glass play button overlay in bottom right corner, and clean Apple Music typography.
  - Redesigned `ArtistsSection` with circular 94dp portraits and centered artist typography (matching Apple Music artist discovery).
  - Redesigned `DiscoverCategoryCard` with 154x92dp gradient tiles and bottom-anchored bold typography.
  - Unified floating bottom navigation capsule in `BottomNavBar.kt` with Apple Music selected-tab pill highlight across all 4 destinations (`Home`, `Online`, `Library`, `Search`).
  - Refined `MiniPlayer.kt` with Apple Music typography, coral-red hairline progress bar, and minimal Play/Pause + Next controls.
- **Online Tab Header Streamlining**:
  - Removed top search input and redundant source selector buttons (`Listen Now`, `JioSaavn`, `YouTube`) from [OnlineScreen.kt](file:///c:/Users/AMAN/AndroidStudioProjects/AuraMusic/app/src/main/java/com/aman/auramusic/ui/screen/OnlineScreen.kt), letting users seamlessly discover aggregated streaming music without duplicate search inputs (unified search is centralized in `SearchScreen`).
- **Playback Transition Safety Guards**:
  - `isTransitioning: Boolean` guard flag in `VlcPlayerManager` and `OnlinePlaybackManager` to prevent transient media unloading events (`Stopped`, `Paused`) from triggering session teardowns.
  - Non-blocking `loadArtworkFast(song)` in `PlaybackNotificationManager` decoding local artwork or using cache without blocking threads or waiting for network.
  - Comprehensive unit test suite in `app/src/test/java/com/aman/auramusic/PlaybackTransitionLogicTest.kt` verifying queue transitions, repeat/shuffle modes, duplicate end debouncing, and job cancellation.

### Changed
- **Immediate Foreground Promotion**:
  - `PlaybackService.startAsForeground` now synchronously pins foreground service status using fast cached/local artwork (0ms delay) before launching asynchronous network fetches for high-res artwork and LRCLIB lyrics.
  - Added `notificationJob: Job?` tracking and cancellation to prevent delayed stale callbacks from previous tracks from demoting active foreground playback.
- **UI & Service Separation**:
  - Removed destructive `playbackService?.startAsForeground` and `playbackService?.stopAsForeground` calls from `PlayerViewModel.onPlaybackState`. `PlaybackService` is now the sole authority for foreground lifecycle management.
- **Queue End Handling**:
  - `PlaybackService.playNext(isManual = false)` now cleanly pauses playback when reaching queue boundary under `RepeatMode.NONE`, eliminating state inversion bugs from `togglePlayPause()`.
- **Online Transition Debounce**:
  - Added 500ms debounce guard to `OnlinePlaybackManager.handleSongEnded` to eliminate duplicate next-track triggers from rapid ExoPlayer state emissions.

---

## [3.1.0] — 2026-09-15

### Changed
- **Clean Architecture & UI Decoupling (God Activity / God Screen Refactoring)**:
  - Extracted `CollectionDetailScreen`, `BottomNavBar`, and `PlaylistDialogs` out of `MainActivity.kt`, reducing activity from 2,149 LOC to 892 LOC (-58.5%).
  - Extracted `AllPlaylistsScreen` and `OnlinePlaylistDetailScreen` out of `LibraryScreen.kt`, reducing screen from 1,989 LOC to 1,416 LOC (-28.8%).
  - Centralized reusable UI components into `ui/component/`: `SongRow`, `MiniPlayer`, `AlbumCard`, `ArtistCard`, `PlaylistCard`, `SongArtwork`.

### Added
- **ColorOS / OPlus Live Lockscreen Bridge**:
  - Added `ColorOSLiveLyricsBridge` and `ColorOSLyricPayload` integrating synchronized live lyrics with ColorOS/OxygenOS lockscreens on OPPO, OnePlus, and Realme devices.
- **Dual-Tier Online Playback Engine**:
  - Integrated AndroidX Media3 ExoPlayer for pristine 320kbps JioSaavn direct CDN streams.
  - Integrated headless RiPlay YouTube IFrame player (`YouTubeIFramePlayer`) for YouTube Music playback without scraper breakages.
  - Added `CacheManager` with Media3 `SimpleCache` for local stream caching.
