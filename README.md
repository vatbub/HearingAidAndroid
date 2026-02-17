# ATLAS Hearing Aid

ATLAS is a modern, open-source hearing aid application for Android. It transforms your smartphone into a powerful sound amplifier, capturing audio via the microphone, processing it in real-time, and playing it back through wired or Bluetooth headphones.

The app features a **sleek, dark-themed "ATLAS" design system** focused on accessibility, clarity, and ease of use in low-light environments.

## ✨ Features

- **Real-time Amplification:** Low-latency audio streaming from microphone to headphones using the Superpowered SDK.
- **ATLAS Design System:** A modern, high-contrast dark theme with card-based layouts and intuitive interactions.
- **Environment Profiles:** Quickly switch between presets like "General," "Restaurant," "Outdoor," and "TV" to adapt to your surroundings.
- **Volume Boost:** Integrated volume slider for up to +30dB of digital gain.
- **Noise Monitoring:** Visual dashboard for tracking ambient noise levels (dB).
- **Safety First:** Automatic feedback suppression and ear safety limits.

## 🚧 Status

**Current Version:** 2.0 (ATLAS Redesign)

- [x] **New Dashboard UI:** Complete overhaul with dark mode, circular toggle, and volume sliders.
- [x] **Profile Management:** Create, edit, and select hearing profiles.
- [x] **Real-time Audio Engine:** Core C++ audio processing pipeline is active.
- [ ] **EQ Visualizer:** Real-time frequency visualization on the dashboard.
- [ ] **Hearing Test:** Integrated audiogram test to auto-calibrate the EQ (Coming Soon).

## 🛠️ Build & Installation

### Prerequisites

- **Android Studio Ladybug (or newer)**
- **JDK 11 or 17** (Do not use JDK 21 yet as it conflicts with older Gradle versions)
- **NDK (Side-by-side)** installed via SDK Manager

### Fix Guide

If you encounter build errors related to C++ compilation, deprecated dependencies, or missing libraries, please refer to our **[Build Fix Guide](BUILD_FIX_GUIDE.md)** included in the repository.

### Quick Start

1. **Clone the Repo:**

   ```bash
   git clone https://github.com/vatbub/hearingaid.git
   ```

2. **Configure SDKs in `local.properties`:**
   Create a `local.properties` file in the project root with the paths to your generic Android SDK and NDK.

   ```properties
   sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
   ndk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk\\ndk\\21.4.7075529
   ```

3. **Install Superpowered SDK:**
   This project relies on the Superpowered Audio SDK for low-latency performance.

   ```bash
   ./gradlew installSuperpowered
   ```

4. **Build & Run:**
   Open the project in Android Studio and run on a physical device (Emulators cannot record audio reliably).

## 🧩 Architecture

- **Frontend:** Native Android (Java/XML) with custom Views and Drawables for the ATLAS design.
- **Backend:** Java-based `MediaBrowserServiceCompat` for managing the audio session.
- **Audio Engine:** C++ layer using JNI and Superpowered SDK for high-performance audio manipulation.

## 📄 License

This application is published under the Apache License v2. It utilizes the [Superpowered Audio SDK](http://superpowered.com/).

---

_Original Project by [vatbub](https://github.com/vatbub/hearingaid). Redesigned with the ATLAS System._
