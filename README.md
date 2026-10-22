# Aura Music

<p align="center">
  <img src="app/src/main/res/drawable/aura_logo.png" alt="Aura Music logo" width="120" />
</p>

<p align="center">
  <strong>A local and online Android music player with synchronized lyrics and ColorOS / HeyTap lock-screen lyric integration.</strong>
  <br />
  Built with Kotlin, Jetpack Compose, LibVLC, and AndroidX Media3.
</p>

<p align="center">
  <a href="https://github.com/dev-aman-hear/AuraMusic/tree/lyricTesterPro"><img src="https://img.shields.io/badge/Branch-lyricTesterPro-blue?style=flat-square" alt="lyricTesterPro branch" /></a>
  <img src="https://img.shields.io/badge/Version-1.0.6-informational?style=flat-square" alt="Version 1.0.6" />
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-success?style=flat-square" alt="Android 8.0 and newer" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blueviolet?style=flat-square" alt="Kotlin 2.0.21" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square" alt="Jetpack Compose" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=flat-square" alt="GPL-3.0 license" /></a>
</p>

---

## Overview

Aura Music brings device-stored music and supported online music sources into one Android player. This `lyricTesterPro` branch focuses especially on synchronized lyrics and experiments with delivering timed lyric metadata to compatible ColorOS, realme UI, and OPlus system components.

The app uses the `com.aman.auramusic` code namespace. Its normal Aura Music build uses the `com.aman.auramusic` application ID. An optional Gradle property, `useHeyTap=true` (enabled by default in this branch), changes the application ID and display identity to `com.heytap.music` for testing OEM integration. This identity mode is a compatibility experiment, not a guarantee that every ColorOS build will accept the app as the preinstalled HeyTap Music application.

> **Compatibility note:** Native lock-screen lyric behavior depends on firmware, SystemUI implementation, package identity, privileges, and ROM whitelists. The integration can send lyric metadata through Android media-session metadata, but cannot guarantee that a device will display it. Test on the exact target ROM before relying on this feature.

## Highlights

### Local music playback
- Scans audio exposed through Android MediaStore, subject to storage permissions.
- Uses LibVLC for local-file playback and codec support provided by the bundled LibVLC build.
- Supports browsing tracks and playing compatible audio files opened from file managers.

### Online music playback
- Includes online search and playback code for supported YouTube Music / InnerTube-related paths and JioSaavn-based sources.
- Uses AndroidX Media3 ExoPlayer for online streams resolved by the app.
- Availability and playback quality depend on upstream services, network conditions, and resolver compatibility. Online catalog access is not guaranteed to be permanent.

### Synchronized lyrics
- Reads timestamped local `.lrc` files located beside matching audio files.
- Requests online lyrics through LRCLIB when local or cached lyrics are unavailable.
- Converts timed lyric lines into a consistent internal lyric model.
- Includes lyric payload generation and tests for timestamp sorting, metadata fields, invalid timestamps, and empty lyric results.

### ColorOS / realme UI / OPlus lyrics bridge
This branch includes a ColorOS-focused lyric metadata path:

1. The playback service obtains local or online lyric lines.
2. `ColorOSLyricPayload` formats valid lines as timestamped LRC, sorted by playback time.
3. The generated JSON includes track title, artist, album, song identifier, lyric type, provider/source identifiers, a track key, and a flag indicating that lyrics are present.
4. The playback service publishes the lyric JSON as part of media-session metadata and refreshes metadata after lyrics or artwork load.
5. The manifest declares the `io.github.andrealtb.lockscreenlyrics.OPLUS_MEDIA_HISTORY` metadata key used by compatible lock-screen lyric integrations.

Line-timed LRC is not sent as `rawLyric`; that field is reserved for word-timed lyric data in the companion bridge protocol.

**What this does not mean:** an Android manifest metadata entry or a media-session payload alone does not grant privileged access to SystemUI. Some OEM implementations may require a specific package signature, allowlist entry, privileged permission, companion bridge/module, or exact vendor payload format. The behavior can differ by ColorOS/realme UI version.

### Playback and notifications
- Playback is managed by a foreground service with a media notification.
- Media controls include play/pause, next, previous, and seek actions.
- The service manages local and online playback paths and updates track metadata.
- A wake lock is used while playback is active; normal background execution is still subject to Android and OEM battery-management policies.

### Interface and personalization
- Jetpack Compose-based interface.
- Monochrome light/dark styling with optional accent-border behavior where implemented.
- Album-art display and artwork-aware player presentation.
- Library, search, playback, queue, and settings flows.

### Library and persistence
- Local preferences, playback-related state, and user collections are stored on-device where implemented.
- Online services and lyric providers may receive search metadata needed to resolve a track or find lyrics.
- This README does not claim that every piece of state is backed up or synchronized between devices.

## Version 1.0.6

- Application version name: `1.0.6`
- Application version code: `19` for the normal Aura Music package.
- Namespace: `com.aman.auramusic`
- Normal application ID: `com.aman.auramusic`
- Minimum Android version: Android 8.0 (API 26)
- Compile / target SDK: API 36

The optional HeyTap identity build retains its separately configured versioning so that its package identity behavior is not silently changed by the Aura Music version bump.

## Build and install

### Requirements

- Android Studio with support for the project's Android Gradle Plugin.
- JDK compatible with the configured Gradle / AGP versions (JDK 17 is a suitable baseline for AGP 8.x).
- Android SDK Platform 36.
- Network access to download Gradle and Maven dependencies.

### Clone this branch

```bash
git clone --branch lyricTesterPro --single-branch https://github.com/dev-aman-hear/AuraMusic.git
cd AuraMusic
```

### Build a debug APK

**Windows PowerShell / Command Prompt**

```powershell
.\gradlew.bat assembleDebug
```

**macOS / Linux**

```bash
./gradlew assembleDebug
```

ABI splits are configured in `app/build.gradle.kts`, including a universal APK. Look under:

```text
app/build/outputs/apk/debug/
```

Exact generated filenames can vary with the Gradle configuration and build variant.

### Install with ADB

Connect an Android device with USB debugging enabled, then use the APK path produced by your build. For example:

```bash
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

If the APK filename differs, use the matching file from the output directory.

## Build identity options

This branch defines the Gradle property `useHeyTap`.

- **Aura Music identity:** `useHeyTap=false` sets application ID `com.aman.auramusic`, display name `Aura Music`, version name `1.0.6`, and version code `19`.
- **HeyTap identity experiment:** `useHeyTap=true` sets application ID `com.heytap.music` and uses the separate OEM-style version values defined in Gradle. This may conflict with an installed system app or trigger package/signature restrictions. Use it only on a test device and understand the consequences before installing.

Gradle property examples:

```powershell
.\gradlew.bat assembleDebug -PuseHeyTap=false
.\gradlew.bat assembleRelease -PuseHeyTap=false
```

The second command builds a release variant; signing depends on the local `signing.properties` configuration. Do not commit keystores, signing properties, passwords, or private keys.

## Testing the lock-screen lyric path

For a focused test on the target ColorOS / realme UI device:

1. Build and install the intended package variant.
2. Confirm ordinary playback and the media notification work before testing lyrics.
3. Use a local audio file with a matching, correctly timestamped `.lrc` file.
4. Start playback and inspect logcat for playback-service or lyric-integration messages if the lock screen stays unchanged.
5. Repeat with a track whose lyrics are resolved online.
6. Test after screen off/on and after a track change, noting whether SystemUI displays lyrics, receives only metadata, or ignores the payload.
7. Record the device model, exact ROM/build number, Android version, package variant, and whether root or any companion module is involved.

A successful payload-generation test confirms formatting logic only; it does not prove that a specific device's SystemUI will render the lyrics.

## Troubleshooting

| Symptom | Checks |
|---|---|
| No lock-screen lyrics | Verify timestamped lyrics are available, media-session metadata updates, the selected package variant, and firmware/bridge requirements. |
| Local lyrics not found | Check that the file is named exactly like the audio file with a `.lrc` extension and is in the same directory. |
| Online lyrics unavailable | Check connectivity, title/artist metadata, LRCLIB availability, and whether a matching timed lyric result exists. |
| Android refuses installation | Check for an existing package with the same application ID, signature mismatch, version downgrade, or OEM package protections. |
| Playback stops in background | Check notification permission, foreground-service behavior, battery optimization, and OEM background restrictions. |
| Build fails | Sync Gradle, confirm SDK 36 and a compatible JDK are installed, and review the first actionable Gradle/Kotlin error. |

## Project structure

Important source locations:

```text
app/src/main/java/com/aman/auramusic/
├── coloros/                 # ColorOS/OPlus lyric payload generation
├── data/
│   ├── model/               # Song, lyric-line, playlist, and settings models
│   └── repository/          # Local library, lyrics, and preference access
├── online/
│   ├── lyrics/              # Online lyric models, networking, and parsing
│   ├── model/               # Online song and playlist models
│   ├── network/             # Online service integrations
│   └── player/              # Online playback orchestration
├── playback/                # Playback service, VLC player, notifications, and queue
├── ui/                      # Compose screens and reusable UI
└── viewmodel/               # UI state and playback/library logic
```

## Dependencies and licenses

The project uses libraries including Kotlin, Jetpack Compose / Material 3, AndroidX Media3, LibVLC, Hilt, Coil, OkHttp, Retrofit, DataStore, and AndroidX Palette. See `app/build.gradle.kts` and `gradle/libs.versions.toml` for the exact configured dependencies.

Aura Music is distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for the full license text. Third-party libraries remain subject to their respective licenses.

## Privacy and network behavior

Local playback and local `.lrc` reading operate on files available to the app. Online search, streaming, artwork lookup, and online lyric lookup require network access and may send track names, artist names, or service-specific identifiers to the relevant endpoints. Review the source and provider policies for the exact behavior of each integration.

## Contributing

Bug reports and improvements are welcome. When reporting an issue, include:

- Device and Android / OEM ROM version.
- Branch and build variant (`useHeyTap=true` or `false`).
- Relevant steps to reproduce and expected versus actual behavior.
- Sanitized logs or a small sample LRC file when relevant.

Please remove personal information, access tokens, signing credentials, and private URLs from logs before sharing them.

---

<p align="center">
  <sub>Aura Music · lyricTesterPro · Version 1.0.6</sub>
</p>
