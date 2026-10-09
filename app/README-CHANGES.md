# AuraMusic source update (review build)

## Included
- `app/src/main`: source tree from the uploaded `main.zip`, with the updated `OnlineScreen.kt`, `SongRow.kt`, and `online/ui/components/SongCard.kt`.
- `app/build.gradle.kts`: copied from the uploaded Gradle file, unchanged.

## Online screen improvements
- Reworked the online landing page into an editorial discovery layout with a featured track card, curated playlist rail, and cleaner track list.
- Consolidated the separate online `SongCard` implementation into the shared `SongRow` component (about 110 lines removed from the duplicated card implementation).
- Added separate active-track and currently-playing states so a paused current track can remain highlighted without showing a playing indicator.
- Removed empty overflow menus from rows when no menu actions are supplied.
- Preserved the existing `OnlinePlaybackManager` and `onOnlineSongSelected` playback handoff.
- Playlist cards now call `getPlaylistSongs()` and show the returned tracks rather than treating the playlist title as a search query.
- Debounced search is cancellation-aware so an older request is less likely to overwrite newer results.
- Added explicit retry states for catalog, search, and playlist failures.
- Uses stable source+track keys to reduce collisions between providers.

## Important verification note
The uploaded archive contains `app/src/main` and the app Gradle file, but not the complete project root (`settings.gradle.kts`, version catalog, Gradle wrapper, etc.). A full Android Gradle build could not be run from these uploads alone. Treat this as a source update for review, not a verified release build. Copy the changed `OnlineScreen.kt` into the full project and run `./gradlew :app:assembleDebug` before release.
