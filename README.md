# 🎵 Aura Music V3.2.0 (Glass Edition)

A premium, modern Android music player built with Kotlin and Jetpack Compose. Featuring a futuristic Frosted Glass design language, Apple Music-inspired aesthetics, ultra-high-definition online media artwork, powerful local & online audio playback (VLC + Media3 ExoPlayer), advanced playlist controls, and a unique "Dynamic Pill" system overlay for global music control.

---

## 🚀 What's New in V3.2.0

* **🖼️ Ultra-HD Online Artwork Engine**:
  * **YouTube Music Master Resolution**: Automatically intercepts and upgrades compressed mobile thumbnails (`=w60`, `=w120`, `=w544`) to studio-grade `1080x1080` (`=w1080-h1080-l90-rj`) uncompressed master covers directly from Google content servers.
  * **YouTube Video Thumbnail Clarity**: Strips low-res mobile compression query parameters (`?sqp=...`) and upgrades low-res previews to clean, full-fidelity `hqdefault` artwork.
  * **JioSaavn Studio Covers**: Delivers `500x500` uncompressed Akamai CDN album covers with strict HTTPS protocol enforcement across search results, curated playlists, detail views, and full-screen player backgrounds.
  * **Coil High-Fidelity Caching**: Centralized `ArtworkQualityOptimizer` with automated disk/memory cache policies, hardware bitmap acceleration, and smooth 250ms crossfades.
* **✨ Universal Frosted Glass Design System**:
  * System-wide frosted glass surface treatment with specular highlight borders and subtle blur overlays across the bottom navigation bar, mini player, category cards, library sheets, song rows, dialogs, and player controls.
* **🔍 Dribbble-Inspired Dynamic Search & Discovery**:
  * Fluid atmospheric glowing mesh category cards, full Light and Dark mode adaptability, instantaneous keyboard dismissal on selection, and quick-access YouTube artist discovery chips.
* **☁️ Artist Cloud & Discography Discovery**:
  * Dedicated YouTube Music & JioSaavn artist discovery sheets with high-resolution profile imagery and one-tap discography browsing.
* **🏠 Unified Home Experience**:
  * Refined proportional greeting headers, daily featured recommendations, and smooth frosted glass navigation.

---

## ✨ Key Features

### 🖼️ Online Streaming & High-Fidelity Audio
* **Dual Audio Engines**: LibVLC for pristine local audio reproduction and AndroidX Media3 ExoPlayer for instant 320kbps JioSaavn & YouTube streams.
* **Studio-Quality Artwork**: Never look at blurry album covers again. Every online track displays crystal-clear uncompressed artwork.
* **Real-Time Synced Lyrics**: Powered by LRCLIB with line-by-line real-time tracking, karaoke glow, and ColorOS status bar live lyrics.

### 💊 Dynamic Pill (System Overlay)
* **Global Access**: Controls your music from any screen, even when outside the app.
* **Orientation Smart**: Automatically hides in landscape mode to stay out of the way during games or videos.
* **Fully Customizable**:
  * **Positioning**: Move the pill to Left, Center, or Right.
  * **Vertical Offset**: Fine-tune the height (0–64dp) to tuck it into your status bar or notch.
  * **Scaling**: Adjust pill scale from 1.0x to 2.0x.
* **Intuitive Controls**:
  * Click artwork to expand full player.
  * Click song info to toggle mini/expanded view.
  * Direct playback controls (Play/Pause, Skip, Previous).
  * Built-in live audio wave visualizer.

### 🎧 Local Music Management
* **Format Versatility**: MP3, FLAC, WAV, M4A, ALAC, OGG, and OPUS.
* **"Open With" Support**: Open and play audio files directly from any Android file manager.
* **External URI Support**: Automatically resolves metadata and starts playback for downloaded or shared tracks.
* **Smart Organization**: Browse by Songs, Albums, Artists, and Playlists.
* **Playlist Portability**: Export and import playlists seamlessly as JSON.

### 🎨 Modern UI / UX Design
* **Apple Music Aesthetic**: Clean typography, fluid gestures, and subtle micro-animations.
* **Dynamic Palette**: UI accents that automatically sample dominant colors from current album artwork.
* **AMOLED Dark & Crisp Light Themes**: Full dark and light theme support with specular glass reflections and optimal contrast.
* **Sleep Timer**: Built-in sleep timer with gentle audio fade-out.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin 2.0+
* **UI**: Jetpack Compose, Material 3, Custom Glassmorphic Shader & Blur Modifiers
* **Architecture**: Clean Architecture + MVVM + MVI
* **Dependency Injection**: Dagger Hilt
* **Audio**: VLC Android SDK (Offline) + AndroidX Media3 ExoPlayer (Online Streams)
* **Networking**: OkHttp 4, Retrofit, Gson
* **Image Loading & Optimization**: Coil 2.6 with custom `ArtworkQualityOptimizer`
* **Concurrency**: Kotlin Coroutines & StateFlow
* **Storage**: AndroidX DataStore (Preferences) + MediaStore API (Audio Scanning)
* **Palette**: AndroidX Palette API

---

## 📦 Installation & Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AuraMusic.git
   ```
2. Open in Android Studio (Ladybug / Koala or newer).
3. Ensure JDK 17 or higher is configured.
4. Build and install to your Android device (API 26+):
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
   ```
5. Grant "Overlay Permission" and "Usage Access" for Dynamic Pill features.

---

## 🚀 Roadmap

- [x] High-Resolution Online Artwork Engine (YouTube & JioSaavn)
- [x] Universal Frosted Glass Design System
- [x] ColorOS Status Bar Live Lyrics Bridge
- [x] Dynamic Pill customization & wave visualizer
- [x] Synced lyrics (LRCLIB)
- [x] Sleep timer
- [ ] Parametric Equalizer
- [ ] Folder browsing
- [ ] Android Auto integration

---

Made with ❤️ by Aman
