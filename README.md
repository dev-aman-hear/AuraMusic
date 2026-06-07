# 🎵 Aura Music

**Aura Music** is a premium, lightweight, and modern offline music player for Android, built entirely with **Jetpack Compose** and **Material 3**. Designed for audiophiles and lovers of clean UI, it brings an immersive listening experience to your local music library.

---

## ✨ Key Features

- 🎭 **Dynamic Backgrounds**: The UI automatically extracts vibrant colors from the current album artwork and creates a smooth, blurred gradient background.
- 📱 **Fluid Pager Navigation**: Seamlessly swipe between **Lyrics ↔ Now Playing ↔ Queue** with a high-performance horizontal pager.
- 🎤 **Synced Lyrics**: Immersive, auto-scrolling lyrics with "tap-to-seek" functionality, inspired by Apple Music.
- 🎶 **Smart Library Scan**: Fast local music scanning with automatic exclusion of voice recordings and call logs.
- ❤️ **Persistent Favorites**: Save your favorite tracks to your local device storage with a single tap.
- 🎧 **Audiophile Badges**: Real-time display of audio quality, including **Hi-Res** and **Lossless** badges based on file metadata.
- 🧊 **Glassmorphic Design**: Modern, translucent UI elements and a sleek floating mini-player for quick control.
- ⚡ **Performance Optimized**: Asynchronous data loading ensures a smooth 60fps experience without UI jank.

---

## 📸 Screenshots

| Now Playing | Synced Lyrics | Music Library |
| :---: | :---: | :---: |
| *(Add Screenshot)* | *(Add Screenshot)* | *(Add Screenshot)* |

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel, StateFlow)
- **Engine**: LibVLC (VLC) for high-performance audio playback
- **Images**: Coil for asynchronous artwork loading and caching
- **Colors**: Android Palette API for dynamic theme extraction
- **Data**: MediaStore API for local library access
- **Language**: 100% Kotlin

---

## 🚀 Installation & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/AuraMusic.git
   ```
2. **Open in Android Studio**:
   - Use the latest version of Android Studio (Koala or newer).
   - Java 11 or higher required.
3. **Build & Run**:
   - Sync Gradle and click **Run** to install on your device or emulator.

---

## 📝 Roadmap (Planned)

- [ ] Folder-based browsing
- [ ] Custom Playlist creation
- [ ] Equalizer support
- [ ] Sleep Timer customization
- [ ] Multi-select for queue management

---

## 👨‍💻 Developed By

**Aman** - *Passionate Android Developer*

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
