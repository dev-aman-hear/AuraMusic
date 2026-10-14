# 🎵 Aura Music

<p align="center">
  <img src="app/src/main/res/drawable/aura_logo.png" alt="Aura Music Logo" width="130" />
</p>

<h3 align="center">Aura Music — High-Fidelity Audio Streaming & Local Music Player</h3>

<p align="center">
  <b>A modern, open-source Android music experience inspired by Apple Music's Liquid Glass aesthetics.</b><br />
  Built natively with <b>Kotlin 2.0</b> and <b>Jetpack Compose</b>, driven by a <b>Dual Audio Engine (LibVLC + Media3 ExoPlayer)</b>,<br />
  real-time synchronized karaoke lyrics, master-grade studio artwork optimization, and seamless offline-online unification.
</p>

<p align="center">
  <a href="https://github.com/dev-aman-hear/AuraMusic/releases"><img src="https://img.shields.io/badge/Version-v1.0.1-FA2D48?style=for-the-badge&logo=android&logoColor=white" alt="Version 1.0.1" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Audio%20Engines-LibVLC%20%7C%20Media3%20ExoPlayer-FF6F00?style=for-the-badge&logo=vlcmediaplayer&logoColor=white" alt="Audio Engines" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white" alt="License GPL-3.0" /></a>
</p>

<p align="center">
  <a href="#-features-at-a-glance">Features</a> •
  <a href="#-dual-audio-engine-architecture">Audio Engines</a> •
  <a href="#-architecture--tech-stack">Architecture</a> •
  <a href="#-supported-formats">Supported Formats</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-whats-new-in-v101">What's New</a> •
  <a href="#-license">License</a>
</p>

---

## 💎 Why Aura Music?

Most Android music players force a compromise: either sleek streaming apps laden with subscriptions, ads, and compressed audio, or barebones offline media players lacking modern UI and online discovery.

**Aura Music bridges both worlds.** It brings audiophile-grade, bit-perfect offline lossless playback alongside an ad-free, high-bitrate online streaming catalog, wrapped inside a fluid **Liquid Glass** user interface that adapts dynamically to your music in real time.

---

## ✨ Features at a Glance

### 🎨 Liquid Glass & Apple Music-Inspired Aesthetics
* **Translucent Frosted Shaders (`liquidGlass`)**: Multi-layered frosted glass surfaces with real-time blur, specular border highlights, dynamic refractions, and elevation depth across navigation bars, player sheets, cards, and dialogs.
* **Ambient Dynamic Palette**: Intelligent color-mesh ambient glow dynamically sampled from current album art, casting subtle, pulsing backlights tailored to every track.
* **Synchronized High-Contrast Themes**: Obsidian dark mode and pure light mode calibrated with contrast-aware color providers for effortless legibility under bright sunlight or dark rooms.
* **Fluid Micro-Interactions**: Elastic gestures, swipe-down dismissible player sheets, dynamic volume/scrubber sliders, and interactive carousel transitions.

### 🔊 Dual Audio Engine Architecture
* **Lossless Offline Engine (LibVLC)**: Native C++ audio decoding via the LibVLC Android SDK. Delivers bit-perfect rendering of lossless audiophile codecs (**FLAC, ALAC, WAV, DSD/PCM, OGG, OPUS, APE, WV**) without Android OS resampling artifacts.
* **High-Bitrate Online Engine (AndroidX Media3 ExoPlayer)**: Powers instant, bufferless streaming with adaptive segmented caching, seamless background audio services, media notification controls, and automatic network reconnection.
* **External File Association**: Set Aura Music as your default player or use `"Open with Aura Music"` directly from any file manager, browser download, or chat app.

### 🌐 Universal Online Streaming & Catalog
* **Multi-Service Catalog**: Direct, unrestricted search and exploration powered by YouTube Music and JioSaavn backends.
* **Curated Discovery**: Browse trending global & regional charts, latest releases, mood-based mixes, and genre channels.
* **Full Artist Discographies**: Explore comprehensive artist profiles, top hit tracks, complete studio albums, and singles.
* **Zero Ads & Zero Tracking**: Uninterrupted listening experience with no promotional interruptions or telemetry tracking.

### 📚 Unified Library & Favorites
* **All-New Dedicated Favorites Hub**: Centralized favorite collection seamlessly unifying your local device favorites and saved online streaming tracks.
* **One-Tap Actions**: Instant **Play All** and **Shuffle** across your entire favorites collection.
* **Independent Online Collections**: Saved online albums and playlists stay organized in their own dedicated space without cluttering custom local playlists.
* **Local MediaStore Scanner**: Automatic indexing by Songs, Albums, Artists, and Folders with instantaneous search and filtering.
* **Playlists & Portability**: Create, reorder, and customize playlists, with one-click **JSON Backup & Restore** for easy device migration.

### 🖼️ Studio-Grade Artwork Optimizer
* **Master Resolution Interceptor**: Automatically detects and upscales standard compressed mobile thumbnails (`=w60`, `=w120`, `=w544`) into uncompressed **1080x1080** master studio artwork.
* **Lossless CDN Protocol**: Enforces uncompressed high-resolution 500x500 Akamai CDN album covers for JioSaavn tracks and strips mobile compression headers.
* **Hardware Accelerated Caching**: Dual-tier disk and memory caching via Coil 2.6 with smooth crossfade animations.

### 🎤 Synchronized Real-Time Karaoke Lyrics
* **Live Synchronized Scrolling**: Powered by the LRCLIB API with millisecond-accurate line highlighting, glowing active lyrics, and tap-to-seek synchronization.
* **Smart Fuzzy Match Engine**: Robust fallback heuristic that identifies matching lyrics even with slight title variations, remix tags, or featuring artist labels.
* **ColorOS & RealmeUI Status Bar Bridge**: Experimental floating and status bar lyrics integration for compatible devices.

### 🎛️ Dynamic Floating Pill & Advanced Controls
* **System Overlay Dynamic Pill**: Optional floating island widget accessible over any application with a real-time reactive audio waveform visualizer and quick controls.
* **Sleep Timer**: Programmable sleep countdown with a logarithmic volume fade-out curve.
* **Playback Queue Management**: Dynamic drag-and-drop queue reordering, loop modes, smart shuffle, and one-tap queue flushing.

---

## 🎧 Dual Audio Engine Architecture

Aura Music intelligently routes audio through specialized engines based on the source:

```
                              ┌─────────────────────────────┐
                              │      Aura Audio Router      │
                              └──────────────┬──────────────┘
                                             │
                      ┌──────────────────────┴──────────────────────┐
                      ▼                                             ▼
        ┌───────────────────────────┐                 ┌───────────────────────────┐
        │       Offline Audio       │                 │      Online Streaming     │
        │   LibVLC Native Engine    │                 │ AndroidX Media3 ExoPlayer │
        ├───────────────────────────┤                 ├───────────────────────────┤
        │ • Bit-perfect decoding    │                 │ • 320 kbps high-bitrate   │
        │ • Hardware-accelerated    │                 │ • Multi-segment caching   │
        │ • FLAC, ALAC, WAV, OPUS   │                 │ • Instant seek & buffer   │
        │ • Gapless offline playback│                 │ • MediaSession background │
        └───────────────────────────┘                 └───────────────────────────┘
```

---

## 🎼 Supported Formats

| Format | Extension | Engine | Quality / Features |
|---|---|---|---|
| **FLAC** | `.flac` | LibVLC | Lossless 24-bit / 192 kHz audiophile grade |
| **ALAC** | `.m4a` | LibVLC | Apple Lossless Audio Codec |
| **WAV** | `.wav` | LibVLC | Raw Uncompressed Linear PCM |
| **OPUS** | `.opus` | LibVLC / Media3 | Low latency, transparent high-efficiency |
| **MP3** | `.mp3` | LibVLC / Media3 | All bitrates (VBR / CBR up to 320 kbps) |
| **AAC / M4A** | `.aac`, `.m4a` | LibVLC / Media3 | Advanced Audio Coding |
| **OGG Vorbis** | `.ogg` | LibVLC | Open container audio format |
| **Online Streams** | HLS / DASH / Progressive | Media3 ExoPlayer | Dynamic 320 kbps stream resolution & caching |

---

## 🏗️ Architecture & Tech Stack

Aura Music adheres to **Clean Architecture** with reactive, unidirectional state flow (**MVI / MVVM**):

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  Jetpack Compose · Material 3 · Liquid Glass · Animated Transitions    │
│  LibraryScreen · PlayerScreen · SearchScreen · OnlineHub · Settings    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ UI Events / StateFlow
┌───────────────────────────────────▼────────────────────────────────────┐
│                             DOMAIN LAYER                               │
│  PlayerViewModel · MusicViewModel · OnlinePlaybackManager · History    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Coroutines & Flows
┌───────────────────────────────────▼────────────────────────────────────┐
│                           REPOSITORY LAYER                             │
│   AudioRepository  ·  OnlineRepository  ·  UserPreferencesRepository   │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
┌───────────────────▼──────────────┐   ┌─────────────▼───────────────────┐
│           DATA SOURCES           │   │          AUDIO ENGINES          │
│ • Android MediaStore             │   │ • LibVLC Native C++ Audio       │
│ • AndroidX DataStore Preferences │   │ • AndroidX Media3 ExoPlayer     │
│ • OkHttp 4 / Retrofit / Gson     │   │ • MediaSessionCompat Service    │
│ • LRCLIB Lyrics API Client       │   │ • System Audio Focus Manager    │
└──────────────────────────────────┘   └─────────────────────────────────┘
```

### Core Technologies

* **Language**: [Kotlin 2.0+](https://kotlinlang.org/) — Coroutines, StateFlow, Flow operators
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
* **Offline Audio**: [LibVLC Android SDK](https://code.videolan.org/videolan/vlc-android)
* **Online Audio**: [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3)
* **Image Loading & Processing**: [Coil 2.6](https://coil-kt.github.io/coil/) with custom hardware bitmap transformers
* **Local Persistence**: [AndroidX DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
* **HTTP & Networking**: [OkHttp 4](https://square.github.io/okhttp/) & [Retrofit 2](https://square.github.io/retrofit/) with [Gson](https://github.com/google/gson)
* **Lyrics Provider**: [LRCLIB](https://lrclib.net/) synchronized lyrics API

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2+) or newer
* **Android SDK**: `minSdkVersion 26` (Android 8.0) | `targetSdkVersion 35` (Android 15)
* **JDK**: Version 17 or higher
* **NDK**: Installed via Android SDK Manager (for LibVLC native libraries)

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dev-aman-hear/AuraMusic.git
   cd AuraMusic
   ```

2. **Assemble the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on a connected device via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
   ```

4. **Assemble Release Bundle / APK**:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📋 What's New in v1.0.1

* ⭐ **Dedicated Library Favorites Tab**: Brand-new dedicated **Favorites** section in the Library tab featuring quick **Play All** and **Shuffle** controls.
* 🌐 **Persistent Online Favorites**: Online tracks marked as favorites in the search or player views now reliably persist in DataStore and appear in your Library Favorites with direct stream URL resolution.
* 🏷️ **Refined Category Chips**: Clean, distraction-free text-only category pills across Library views for a sleek and modern appearance.
* ⚡ **Performance & Playback Stability**: Refined audio session state handling when alternating between local LibVLC playback and online ExoPlayer streams.

---

## 🤝 Contributing

Contributions, feature requests, and issue reports are warmly welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for complete details.

<p align="center">
  Crafted with ❤️ by <a href="https://github.com/dev-aman-hear"><b>Aman</b></a>
</p>
