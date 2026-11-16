# NowlSolitaire 🃏🦉

A modern, responsive Solitaire collection for Android built with **Kotlin** and **Jetpack Compose**.

---

## 🎮 Game Modes

### 1. Klondike Solitaire
The classic card game experience:
- **Draw 1 & Draw 3 Modes**: Standard rules with configurable draw counts.
- **Tableau & Foundations**: Build foundations up by suit from Ace to King, and tableau down in alternating colors.
- **Auto-Complete & Hints**: Helper features for quick finishes when all remaining cards are revealed.
- **Undo / Redo & Moves/Timer**: Track your moves, time, and undo moves seamlessly.

### 2. Pyramid Solitaire
A fast-paced pairing solitaire puzzle:
- **Sum-to-13 Matching**: Pair exposed cards that sum up to 13 (e.g., King alone, Queen + Ace, Jack + 2, 10 + 3, etc.).
- **Pyramid Clearing**: Discard pairs to uncover overlapping cards and clear the entire pyramid.
- **Stock & Waste Pile**: Cycle through stock cards to find matching pairs for pyramid cards.

---

## ✨ Features

- **Modern Jetpack Compose UI**: Clean card visuals, smooth drag/tap interactions, and responsive layouts.
- **Material 3 Theming**: Consistent color palettes and typography.
- **State Persistence**: Saves settings, game preferences, and statistics using Jetpack DataStore.
- **Game Engine & Rules**: Pure domain models and rule engines decoupled from the UI for testability.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture**: Clean Architecture / MVVM (Model-View-ViewModel) + MVI state patterns
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Asynchronous**: Kotlin Coroutines & `StateFlow`
- **Data Storage**: Jetpack DataStore (Preferences)
- **Serialization**: `kotlinx.serialization`
- **Minimum SDK**: API 29 (Android 10)
- **Target SDK**: API 37

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 11 or higher
- Android SDK with platform API 35+ / 37

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/nowlsantos/NowlSolitaire.git
   cd NowlSolitaire
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run on an Android device or emulator running Android 10+ (API 29+).

Alternatively, build from the command line:
```bash
# Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 🧪 Testing

The project includes unit tests for all core rule engines and ViewModels:
- `KlondikeEngineTest` & `KlondikeRulesTest`
- `PyramidEngineTest` & `PyramidRulesTest`
- `KlondikeViewModelTest` & `PyramidViewModelTest`
- `KoinModulesTest`

Run all tests via Gradle:
```bash
./gradlew check
```

---

## 📄 License
This project is open source and available under the standard repository terms.
# NowlSolitaire
