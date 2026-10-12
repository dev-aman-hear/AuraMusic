# 🎵 Aura Music

<p align="center">
  <img src="app/src/main/res/drawable/aura_logo.png" alt="Aura Music Logo" width="120" />
</p>

<p align="center">
  <b>A next-generation, high-fidelity music streaming and local audio player for Android.</b><br />
  Built from the ground up with <b>Kotlin</b> and <b>Jetpack Compose</b>, featuring an Apple Music-inspired <b>Liquid Glass</b> design system, dual audio engines, real-time synced lyrics, and studio-grade artwork.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-v1.0.0-FA2D48?style=for-the-badge&logo=android" alt="Version 1.0.0" />
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android" alt="Android API 26+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Audio-LibVLC%20%2B%20Media3%20ExoPlayer-FF6F00?style=for-the-badge&logo=vlcmediaplayer" alt="Audio Engines" />
</p>

---

## 🌟 Highlights & Features

### 🎨 Liquid Glass & Apple Music-Inspired Aesthetics
* **Translucent Glass Surfaces**: Multi-tiered frosted glass shaders (`liquidGlass`) with custom refraction, specular borders, and subtle depth elevation across controls, navigation bars, bottom sheets, and dialogs.
* **Dynamic Palette & Contrast**: Intelligent ambient mesh glows dynamically sampled from current album artwork.
* **Flawless Light & Dark Themes**: True obsidian dark mode and crisp light mode engineered for optimal text contrast and readability.
* **Micro-Animations & Gestures**: Fluid swipe-down sheets, interactive carousels, and responsive touch feedback throughout the interface.

---

### 🔊 Dual High-Fidelity Audio Architecture
* **Offline Engine (LibVLC)**: Bit-perfect local playback supporting all major lossy and lossless formats:
  * **FLAC**, **ALAC**, **WAV**, **MP3**, **AAC**, **M4A**, **OGG**, and **OPUS**.
* **Online Streaming Engine (AndroidX Media3 ExoPlayer)**:
  * High-bitrate playback with intelligent multi-segment caching for instant, zero-buffer playback.
  * Robust network recovery and seamless background audio session handling.
* **External File Association**: Open and stream audio files directly from any Android file manager via `"Open with Aura Music"`.

---

### 🌐 Universal Online Streaming Hub
* **Multi-Service Catalog**: Direct, ad-free streaming search and exploration powered by YouTube Music and JioSaavn backends.
* **Curated Discovery**: Explore trending charts, new releases, mood mixes, and curated regional playlists.
* **Artist Discographies**: Rich artist profiles, top tracks, singles, and full album discographies.
* **Personal Library Synchronization**:
  * Save online albums and playlists directly to your library with automatic offline metadata caching.
  * Clean collection separation: Saved online collections remain distinct from your custom user playlists to eliminate clutter and duplication.

---

### 🖼️ Studio-Grade Artwork Optimizer
* **Master Resolution Interceptor**: Automatically boosts standard compressed mobile thumbnails (`=w60`, `=w120`, `=w544`) to uncompressed **1080x1080** master studio artwork.
* **Lossless CDN Protocol**: Enforces high-definition 500x500 Akamai CDN album covers for JioSaavn tracks and strips mobile compression query parameters from video thumbnails.
* **Smart Coil Cache**: Disk and memory caching with hardware bitmap rendering and smooth 250ms crossfades.

---

### 🎤 Synchronized Real-Time Lyrics
* **Live Karaoke Scrolling**: Powered by LRCLIB with line-by-line synchronized tracking, active line highlight glows, and manual offset fine-tuning.
* **Intelligent Query Matching**: Fuzzy search fallback engine to retrieve lyrics even with slight title or metadata variations.
* **ColorOS Status Bar Lyrics Bridge**: Support for floating and status-bar lyrics on compatible Android devices.

---

### 🎛️ Dynamic Floating Pill & Advanced Controls
* **System Overlay Dynamic Pill**: Optional floating music pill accessible from any app with live waveform visualization, track scrubbing, and quick playback toggles.
* **Sleep Timer**: Customizable sleep countdown with a gentle audio fade-out curve.
* **Playlist Portability**: Export and import your custom playlists seamlessly in JSON format.
* **Queue Management**: Dynamic queue reordering, shuffle modes, and one-tap clearing.

---

## 🏗️ Architecture & Technology Stack

Aura Music follows modern Android architectural patterns: **Clean Architecture** with unidirectional data flow (**MVI/MVVM**).

```text
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Jetpack Compose · Material 3 · Liquid Glass System   │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                      Domain / VM                       │
│    PlayerViewModel · MusicViewModel · StateFlows       │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    Repository Layer                    │
│   AudioRepository · OnlineRepository · UserPreferences │
└─────────────────┬──────────────────┬───────────────────┘
                  │                  │
┌─────────────────▼────────┐       ┌─▼───────────────────┐
│       Data Sources       │       │    Audio Engines    │
│  MediaStore · DataStore  │       │  LibVLC (Offline)   │
│  OkHttp · Retrofit · API │       │  Media3 (Streaming) │
└──────────────────────────┘       └─────────────────────┘
```

| Component | Library / Framework | Purpose |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern expressive syntax and coroutines |
| **UI Toolkit** | Jetpack Compose | Declarative UI, animated layouts, custom modifiers |
| **Design System** | Material 3 + Custom Liquid Glass | Frosted glass surfaces, ambient gradients, dynamic color |
| **Offline Audio** | LibVLC Android SDK | Lossless decoding (FLAC, WAV, ALAC, etc.) |
| **Streaming Audio**| AndroidX Media3 ExoPlayer | 320kbps streams, DataSource caching |
| **DI** | Dagger Hilt | Dependency injection across ViewModels & Services |
| **Networking** | OkHttp 4 + Gson + Retrofit | High-performance HTTP client, API interceptors |
| **Image Loading**| Coil 2.6 | Asynchronous image loading with studio-art transforms |
| **Lyrics** | LRCLIB API | Real-time synchronized LRC lyrics provider |
| **Local Storage**| AndroidX DataStore Preferences | Lightweight, reactive preference & library storage |

---

## 📱 Getting Started & Building

### Prerequisites
* **Android Studio**: Ladybug (2024.2+) or newer.
* **JDK**: Version 17 or higher.
* **Target Android OS**: Android 8.0 (API 26) through Android 15 (API 35+).

### Build & Run
1. **Clone the repository**:
   ```bash
   git clone https://github.com/dev-aman-hear/AuraMusic.git
   cd AuraMusic
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install to connected device via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📋 Release Notes — v1.0.0

* **Fresh 1.0.0 Release**: Standardized versioning across application manifests, build scripts, settings, and network clients.
* **Independent Online Library Collections**: Saving online playlists or albums now cleanly routes to Saved Online collections without generating duplicate custom playlists in "Your Playlists".
* **Synchronized Dark & Light Theme Contrast**: Resolved color-provider synchronization so text, badges, and controls maintain high-contrast visibility regardless of OS-level light/dark modes.
* **Studio-Quality Online Art Engine**: Fully integrated master-resolution thumbnail scalers for YouTube Music and JioSaavn tracks.
* **Full Song Caching on Save**: Ensured online playlists and albums preserve pre-fetched tracklists upon being saved to library to eliminate 0-song view issues.

---

## 📄 License & Credits

Developed with ❤️ by **Aman**.  
This project is open-source for personal, educational, and non-commercial exploration.
