# AuraMusic Android — Known Issues & Architectural Caveats

Structured tracking of verified architectural bugs, edge cases, root causes, and verification procedures.

---

## ISSUE-001: Next Song Plays for 2–5 Seconds Then Unexpectedly Stops

- **Issue ID**: `ISSUE-001`
- **Component**: `playback-service`, `playback-vlc-engine`, `online-playback-manager`, `viewmodel-player`
- **Observed Symptoms**:
  1. Current track A reaches natural end.
  2. Next track B begins playing.
  3. After approximately 2–5 seconds, playback unexpectedly stops or pauses.
  4. Occurred during automatic track transitions on both local and online playback.
- **Verified Root Cause**:
  1. **Delayed Foreground Service Promotion**: In `PlaybackService.startAsForeground`, `startForeground` was delayed inside a coroutine *after* executing `lyricsRepository.lyricsFor(song)` (which performs an LRCLIB HTTP network call taking 2–5 seconds). Track B started playing in the background without active foreground status. On Android 12+ (API 31+), the OS ActivityManager / PowerManager restricts background audio processes, killing/suspending them after 2–5 seconds. If `startForeground()` is attempted after the app is backgrounded, it throws `ForegroundServiceStartNotAllowedException`.
  2. **Stale Notification Coroutines Demoting Service**: In `updateOnlineNotification`, ending track A triggered a coroutine with `isPlaying = false` that took 2–5 seconds loading artwork before calling `stopForeground(STOP_FOREGROUND_DETACH)`. This delayed coroutine executed right while Track B was actively playing, stripping the foreground status.
  3. **Destructive Teardown Triggered by UI Layer**: `PlayerViewModel.onPlaybackState(playing)` listened directly to LibVLC. When LibVLC unloaded Track A, it emitted transient `Stopped`/`Paused` events. `PlayerViewModel` commanded `playbackService?.stopAsForeground(false)`, demoting the service during transition.
  4. **Missing Transition Guard**: Neither `VlcPlayerManager` nor `OnlinePlaybackManager` had an `isTransitioning` flag. Transient stop events emitted during media reload leaked as genuine pause events.
  5. **Queue Boundary Inversion**: When reaching the natural end of the queue under `RepeatMode.NONE`, `togglePlayPause()` was called, which restarted playback instead of cleanly pausing.
- **Relevant Source Files & Symbols**:
  - `app/src/main/java/com/aman/auramusic/playback/PlaybackService.kt` (`startAsForeground`, `updateOnlineNotification`, `playNext`, `onPlaybackState`)
  - `app/src/main/java/com/aman/auramusic/playback/VlcPlayerManager.kt` (`isTransitioning`, `play`, `mediaPlayer.setEventListener`)
  - `app/src/main/java/com/aman/auramusic/playback/PlaybackNotificationManager.kt` (`loadArtworkFast`, `createNotification`)
  - `app/src/main/java/com/aman/auramusic/viewmodel/PlayerViewModel.kt` (`onPlaybackState`, UI foreground detachment removal)
  - `app/src/main/java/com/aman/auramusic/online/player/OnlinePlaybackManager.kt` (`isTransitioning`, `handleSongEnded`, `playNext`)
- **Fix Applied**:
  - Pinned foreground status immediately (0ms delay) in `startAsForeground` and `updateOnlineNotification` using `loadArtworkFast()`, offloading high-res artwork & lyrics to non-blocking background jobs.
  - Added `notificationJob?.cancel()` to cancel stale in-flight jobs from previous tracks.
  - Added `isTransitioning: Boolean` guard flag to suppress transient stop/pause events during media teardown.
  - Removed destructive `playbackService?.stopAsForeground` calls from `PlayerViewModel`.
  - Replaced `togglePlayPause()` at queue end with clean `playerManager.pause()`.
  - Added 500ms debounce on `handleSongEnded` in `OnlinePlaybackManager`.
- **Current Status**: **RESOLVED & VERIFIED**
- **Reproduction Steps**:
  1. Queue two tracks (Track A and Track B).
  2. Let Track A play to natural completion.
  3. Send app to background or lock the screen.
  4. Observe if Track B plays past the 5-second mark continuously.
- **Regression Test / Verification Procedure**:
  - Automated tests in `app/src/test/java/com/aman/auramusic/PlaybackTransitionLogicTest.kt`:
    - `testLocalTrackSequentialTransition`
    - `testOnlineTrackSequentialTransition`
    - `testDuplicatePlaybackEndDebounce`
    - `testTransitionFlagSuppressesTransientStoppedEvents`
    - `testStaleNotificationJobRejection`
  - Build validation: `./gradlew.bat testDebugUnitTest` and `./gradlew.bat compileDebugKotlin`.

---

## ISSUE-002: Android 12+ ForegroundServiceStartNotAllowedException on Background Audio Transition

- **Issue ID**: `ISSUE-002`
- **Component**: `playback-service`
- **Observed Symptoms**: App crash or playback silence when transitioning tracks while device screen is locked or app is in background.
- **Verified Root Cause**: Starting in Android 12 (API 31), calling `Service.startForeground()` from the background is prohibited unless the service is already running in foreground. If a service drops foreground status (even momentarily) during track transition, subsequent `startForeground()` calls throw `ForegroundServiceStartNotAllowedException`.
- **Relevant Source Files & Symbols**:
  - `app/src/main/java/com/aman/auramusic/playback/PlaybackService.kt` (`startForeground`, `stopForeground`)
- **Fix Applied**: Never call `stopForeground(STOP_FOREGROUND_DETACH)` during an active track transition. Maintain unbroken foreground service status across track switches.
- **Current Status**: **RESOLVED**
- **Verification Procedure**: Screen-locked queue transitions on Android 12+ physical device/emulator.

---

## ISSUE-003: MediaStore Scanning Including Call Recordings & Audio Clips

- **Issue ID**: `ISSUE-003`
- **Component**: `data-music-repo`
- **Observed Symptoms**: Short WhatsApp voice notes, call recordings, and system notification sounds appearing in music library.
- **Verified Root Cause**: Android MediaStore indexes all files with audio MIME types indiscriminately.
- **Relevant Source Files & Symbols**:
  - `app/src/main/java/com/aman/auramusic/data/repository/MusicRepository.kt` (`getAllSongs`)
- **Fix Applied**: `MusicRepository.getAllSongs()` applies strict filter:
  - Duration must be >= 30,000ms (30 seconds).
  - Path must not contain common call recording folders (`Recordings/Call`, `Voice Recorder`, `WhatsApp Audio/Sent`).
- **Current Status**: **RESOLVED**
- **Verification Procedure**: Verify short audio clips do not appear in `LibraryScreen`.

---

## ISSUE-004: OPlus / ColorOS Lockscreen Media Metadata Lost Updates

- **Issue ID**: `ISSUE-004`
- **Component**: `coloros-bridge`, `playback-service`
- **Observed Symptoms**: Lockscreen live lyrics occasionally missing first lyric line after track change on ColorOS 13/14 devices.
- **Verified Root Cause**: OPlus system lockscreen media controller enforces an 800ms debounce filter on `MediaMetadataCompat` extras updates. If metadata is updated rapidly upon track change, the update can be dropped by the system framework.
- **Relevant Source Files & Symbols**:
  - `app/src/main/java/com/aman/auramusic/playback/PlaybackService.kt` (`retryJob`, 800ms recovery check)
  - `app/src/main/java/com/aman/auramusic/coloros/ColorOSLiveLyricsBridge.kt`
- **Fix Applied**: `PlaybackService` schedules a single 800ms delayed metadata refresh (`retryJob`) if `lyricInfoJson.isNotBlank()`, re-pushing the payload after the system debounce window expires.
- **Current Status**: **RESOLVED**
- **Verification Procedure**: Test on OnePlus/OPPO device running OxygenOS/ColorOS.
