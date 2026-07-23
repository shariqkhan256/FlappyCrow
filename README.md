# 🐦 FlappyCrow

<p align="center">
  <img src="app/src/main/res/drawable/ic_crow_logo.jpg" alt="FlappyCrow App Icon" width="160" style="border-radius: 28px; box-shadow: 0 8px 16px rgba(0,0,0,0.3);" />
</p>

<p align="center">
  <b>Kid-Friendly, Cute & Minimal Android Arcade Game</b>
</p>

<p align="center">
  <img src="app/src/main/res/drawable/flappy_crow_banner.jpg" alt="FlappyCrow Gameplay Banner" width="100%" style="border-radius: 12px;" />
</p>

---

## 📌 Overview
**FlappyCrow** is a native Android casual arcade game built with **Kotlin** and **Jetpack Compose**. Tap to help Coco the baby crow navigate through obstacle pipes, collect coins and gems, unlock custom accessories, and set high scores!

---

## ✨ Key Features
- **Canvas Game Engine**: Hardware-accelerated 2D gameplay loop with smooth physics, particle effects, and dynamic parallax backgrounds.
- **Shop & Customization**: Unlock and equip fun accessories like hats, sunglasses, and bowties.
- **Persistent Progress**: High scores, coins, and unlocked shop items saved using **Room Database**.
- **Audio & Haptics**: Built-in sound effects and haptic vibration feedback.
- **Child-Friendly Design**: Cute aesthetic, soft colors, and intuitive touch controls.

---

## 🛠️ Tech Stack
- **Language**: Kotlin
- **UI & Graphics**: Jetpack Compose, Compose Canvas
- **Architecture**: MVVM + Clean Architecture
- **Persistence**: Room SQLite Database
- **Async & State**: Coroutines, StateFlow
- **Testing**: Robolectric & Roborazzi

---

## 📂 Project Structure
```
app/src/main/java/com/example/
├── MainActivity.kt               # Entry point & Navigation router
├── data/                         # Room Database, DAOs & Game Repository
└── ui/
    ├── theme/                    # Colors, Typography & Material 3 Theme
    └── game/
        ├── FlappyCrowGame.kt     # Canvas Game Engine & Physics
        ├── GameScreens.kt        # Menus, Shop, Settings & High Scores
        └── GameViewModel.kt      # State Management & Game Logic
```

---

## 🧪 Testing Commands
Run unit tests and screenshot verifications locally:

```bash
# Run Unit Tests
gradle :app:testDebugUnitTest

# Run Screenshot Checks
gradle :app:verifyRoborazziDebug
```
