# KLCP Quiz App - 2025 Edition

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Android](https://img.shields.io/badge/Android-8.0+-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![Material 3](https://img.shields.io/badge/Material%20You-3.0-red.svg)

Eine moderne Android-Quiz-App für die KLCP-Zertifizierung mit 375 Praxisfragen

</div>

## 🌟 Features 2025

### 🎨 Modernes Design
- **Material Design 3 (Material You)** mit Dynamic Colors
- **Automatischer Dark Mode** - folgt Systemeinstellungen
- **Moderne Animationen** - flüssige Übergänge und Feedback
- **Edge-to-Edge Display** - maximale Bildschirmnutzung
- **Splash Screen API** - Native Android 12+ Splash Screen

### 📚 Quiz-Funktionalität
- **375 KLCP-Zertifizierungsfragen** aus offiziellem Fragenkatalog
- **Zweisprachig** - Englisch & Deutsch umschaltbar
- **Sofortiges Feedback** - richtig/falsch mit visuellen Effekten
- **Progress Tracking** - automatische Speicherung des Fortschritts
- **Ergebnis-Statistiken** - detaillierte Auswertung nach jedem Quiz

### 🎯 User Experience
- **Haptic Feedback** - unterschiedliche Vibrationen für richtig/falsch
- **Smooth Animations** - Scale, Fade, Slide, Shake Effekte
- **Intuitive Navigation** - Material 3 Navigation Drawer
- **Optimierte Performance** - Room Database mit Kotlin Coroutines
- **Offline-First** - keine Internetverbindung nötig

### 🏆 Gamification (System vorbereitet)
- **Achievement System** - 10+ Badges zum Freischalten
- Kategorien: Beginner, Learner, Master, Persistent, Perfectionist
- Achievements für: First Quiz, Perfect Score, Dedikation, Speed, etc.

### 💾 Moderne Datenverwaltung
- **DataStore** statt SharedPreferences - moderne Android-Best-Practice
- **Room Database** - lokale SQLite mit Type-Safety
- **Coroutines & Flow** - reaktive Datenverwaltung
- **MVVM Architecture** - saubere Code-Struktur

## 🏗️ Architektur

```
app/
├── data/
│   ├── Question.kt              // Fragenmodell
│   ├── QuizSession.kt           // Session-Tracking
│   ├── UserAnswer.kt            // Antwort-Tracking
│   ├── Achievement.kt           // Achievement-System
│   ├── QuizDatabase.kt          // Room Database
│   ├── QuizRepository.kt        // Daten-Repository
│   └── PreferencesManager.kt    // DataStore Manager
├── ui/
│   ├── home/                    // Start-Screen
│   ├── quiz/                    // Quiz-Logic & UI
│   └── result/                  // Ergebnis-Anzeige
└── utils/
    └── HapticFeedbackHelper.kt  // Vibrations-Helper
```

## 🔧 Technologie-Stack

### Core
- **Kotlin** 2.0.21
- **Android SDK** 26+ (Android 8.0+)
- **Material Design 3** (Material You)
- **Jetpack Components**

### Libraries
- `Room` 2.6.1 - Lokale Datenbank
- `Navigation Component` 2.9.6 - Navigation
- `DataStore` 1.1.1 - Preferences
- `Coroutines` - Asynchrone Programmierung
- `LiveData & ViewModel` - MVVM
- `Splash Screen API` 1.0.1 - Native Splash
- `Lottie` 6.5.2 - Animations (vorbereitet)
- `Gson` 2.10.1 - JSON Parsing

### Build System
- Gradle 8.12.3
- Kotlin DSL
- KSP (Kotlin Symbol Processing)
- Safe Args für Navigation

## 📱 Screenshots & Features im Detail

### Material You Dynamic Colors
Die App nutzt das Material You Design-System und passt sich automatisch an die Systemfarben an:
- Light Theme mit Pastell-Tönen
- Dark Theme mit OLED-optimierten Farben
- Automatische Umschaltung basierend auf Systemeinstellungen

### Haptic Feedback
Verschiedene Vibrationen für unterschiedliche Aktionen:
- **Light Tap** - Button-Presses (10ms)
- **Medium Feedback** - Auswahl-Änderungen (30ms)
- **Success** - Richtige Antwort (Doppel-Vibration)
- **Error** - Falsche Antwort (Shake-Vibration)
- **Heavy** - Quiz abgeschlossen (100ms)

### Animationen
- **Entrance Animations** - Overshoot-Effekt beim Laden
- **Selection Animations** - Scale-Effekt bei Auswahl
- **Success Animations** - Pulse-Effekt für richtige Antworten
- **Error Animations** - Shake-Effekt für falsche Antworten
- **Transitions** - Smooth Slide & Fade zwischen Screens

## 🚀 Installation & Build

### Voraussetzungen
- Android Studio Ladybug | 2024.2.1+
- JDK 11+
- Android SDK 26+
- Gradle 8.12.3

### 🔐 Build-Sicherheit

**Wichtig:** Aus Sicherheitsgründen werden Signing-Credentials nicht im Quellcode gespeichert.

#### Setup für Release-Builds

1. **Template kopieren:**
   ```bash
   cp local.properties.template local.properties
   ```

2. **local.properties konfigurieren:**
   ```properties
   # Füge deine Signing-Credentials hinzu
   storeFile=../your-release-key.jks
   storePassword=your_store_password_here
   keyAlias=your_key_alias_here
   keyPassword=your_key_password_here
   ```

3. **JKS-Datei erstellen:**
   ```bash
   # Erstelle deine Release-Key-Datei im Projektroot
   # Die Datei wird von .gitignore ausgeschlossen
   keytool -genkeypair -v -keystore klcp-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias klcp-key
   ```

#### CI/CD Konfiguration

Für automatisierte Builds (GitHub Actions, Jenkins, etc.) verwende Umgebungsvariablen:

```yaml
# Beispiel für GitHub Actions
env:
  SIGNING_KEY_STORE_PASSWORD: ${{ secrets.SIGNING_KEY_STORE_PASSWORD }}
  SIGNING_KEY_PASSWORD: ${{ secrets.SIGNING_KEY_PASSWORD }}
  SIGNING_KEY_ALIAS: ${{ secrets.SIGNING_KEY_ALIAS }}
```

### Build-Anleitung
```bash
# Repository klonen
git clone [repository-url]
cd newmultichoice

# Debug Build (keine Credentials nötig)
./gradlew assembleDebug

# Release Build (benötigt konfigurierte local.properties)
./gradlew assembleRelease

# Tests ausführen
./gradlew test

# APK installieren
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Datenstruktur

### Question Model
```kotlin
data class Question(
    val id: Int,
    val questionEn: String,
    val questionDe: String,
    val optionAEn: String,
    val optionBEn: String,
    val optionCEn: String,
    val optionDEn: String,
    val optionADe: String,
    val optionBDe: String,
    val optionCDe: String,
    val optionDDe: String,
    val correct: String  // A, B, C, or D
)
```

### Quiz Session Tracking
```kotlin
data class QuizSession(
    val id: Long,
    val timestamp: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val percentage: Float,
    val durationMinutes: Int,
    val language: String
)
```

## 🎯 Roadmap & Zukünftige Features

### Phase 1 ✅ (Abgeschlossen)
- Material Design 3 Implementation
- Haptic Feedback System
- Modern Animations
- DataStore Integration
- Achievement System (Backend)
- Dark Mode Support

### Phase 2 🚧 (In Planung)
- [ ] Bottom Sheet Settings Panel
- [ ] Swipe Gestures für Navigation
- [ ] Jetpack Compose für neue Screens
- [ ] Quiz-Statistiken mit Charts
- [ ] Onboarding-Flow für neue User
- [ ] Share-Funktion für Ergebnisse

### Phase 3 💡 (Ideen)
- [ ] Timed Quiz Mode
- [ ] Custom Quiz (Fragenanzahl wählbar)
- [ ] Bookmark-System für schwierige Fragen
- [ ] Learning Mode mit Erklärungen
- [ ] Cloud Sync der Fortschritte
- [ ] Multi-User Support

## 🧪 Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

Verfügbare Tests:
- `QuizViewModelTest` - Quiz-Logik Tests
- Datenmodell-Tests
- Score-Berechnungs-Tests

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## 📄 Lizenz

Dieses Projekt wurde für Bildungszwecke erstellt.

## 🤝 Beitragen

Contributions sind willkommen! Bitte erstelle einen Pull Request oder öffne ein Issue.

## 📞 Support

Bei Fragen oder Problemen öffne bitte ein Issue im Repository.

---

<div align="center">

Entwickelt mit ❤️ für die KLCP-Community

**Version 1.0.0** | 2025 Edition

</div>

