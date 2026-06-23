# 🎵 Aura Music V2.6

A premium, modern Android music player built with Kotlin and Jetpack Compose. Featuring a clean Apple Music-inspired design, powerful local audio playback, and a unique "Dynamic Pill" overlay for global music control.

## ✨ Key Features

### 💊 Dynamic Pill (System Overlay)
* **Global Access**: Controls your music from any screen, even when outside the app.
* **Orientation Smart**: Automatically hides in landscape mode to stay out of the way during games or videos.
* **Fully Customizable**: 
    *   **Positioning**: Move the pill to the Left, Center, or Right.
    *   **Vertical Offset**: Fine-tune the height (0-64dp) to tuck it perfectly into your status bar.
    *   **Scaling**: Increase the minimized pill size from 1.0x to 2.0x.
* **Intuitive Controls**: 
    *   Click artwork to launch the full player.
    *   Click song info to collapse/expand.
    *   Full playback controls (Play/Pause, Skip, Previous).

### 🎧 Music Playback
* **VLC Engine**: Powered by the robust VLC Android SDK for high-fidelity audio.
* **Wide Format Support**: Play MP3, FLAC, WAV, M4A, ALAC, and more.
* **Smart Audio Focus**: Automatically pauses when you receive calls or play other media.
* **Background Playback**: Continuous music even when the screen is off or app is closed.

### 🎨 Modern UI/UX
* **Apple Music Inspired**: A clean, premium aesthetic with smooth animations.
* **Dynamic Colors**: UI accents that adapt to your current album artwork.
* **AMOLED Mode**: Pure black theme for battery saving and stunning contrast.
* **Karaoke Mode**: Real-time synchronized lyrics display.
* **Visualizer**: Dynamic wave visualizer built directly into the Dynamic Pill.

### ❤️ Library & Management
* **Fast Scanning**: Quickly index all local music on your device.
* **Smart Organization**: Browse by Songs, Albums, Artists, or Playlists.
* **History & Favorites**: Revisit your recent tracks or heart your top songs.
* **Playlist Sharing**: Export and import playlists as JSON for easy sharing.

## 🛠️ Built With
* **Language**: Kotlin
* **Framework**: Jetpack Compose (UI)
* **Audio**: VLC Android SDK / Media3
* **Concurrency**: Coroutines & StateFlow
* **Storage**: DataStore (Settings) & MediaStore (Library)
* **Visuals**: Palette API & Coil

## 📦 Installation
1. Download the latest APK from the [Releases](https://github.com/yourusername/AuraMusic/releases) section.
2. Grant "Overlay Permission" and "Usage Access" for the Dynamic Pill features.
3. Enjoy your music!

## 🚀 Roadmap
- [x] Lyrics support
- [x] Sleep timer
- [x] Dynamic Pill customization
- [ ] Equalizer
- [ ] Folder browsing
- [ ] Android Auto support

---
Made with ❤️ by Aman
