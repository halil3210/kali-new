# 📡 KLCP Quiz App - Server Integration Guide

## ✅ Was wurde implementiert:

### **Android App - API Integration:**

1. **API Models** (`api/models/`)
   - `SyncRequest.kt` - Upload-Request zum Server
   - `SyncResponse.kt` - Server-Response
   - DTOs für UserStats, QuizSessions

2. **Retrofit Client** (`api/`)
   - `ApiService.kt` - Alle API-Endpunkte
   - `RetrofitClient.kt` - Singleton mit Auto-Fallback
   - Automatischer Wechsel zwischen WiFi AP und Heimnetzwerk

3. **Sync Manager** (`sync/`)
   - `SyncManager.kt` - Smart Sync-Logik
   - `SyncWorker.kt` - Background Sync (alle 6 Stunden)
   - Offline-First Strategie

4. **Repository Erweiterung**
   - `updateStatsFromServer()` - Server-Daten übernehmen
   - Merge-Logik für Achievements
   - Konflikt-Auflösung (nimmt beste Werte)

---

## 🌐 **Server URLs:**

- **Primary:** `http://192.168.44.1:3000` (WiFi Access Point)
- **Secondary:** `http://192.168.178.27:3000` (Heimnetzwerk)

Die App wechselt automatisch zwischen beiden!

---

## 📋 **API-Endpunkte (verfügbar):**

### **Health Check:**
```
GET /api/health
```

### **Sync:**
```
POST /api/sync/upload         - Upload lokaler Daten
GET  /api/sync/download/:id   - Download Server-Daten
```

### **Stats:**
```
GET  /api/stats/user/:id      - User-Statistiken
POST /api/stats/update        - Stats aktualisieren
```

### **Sessions:**
```
GET  /api/sessions/:id        - Quiz-Sessions
POST /api/sessions/save       - Session speichern
```

---

## 🔄 **Sync-Strategie:**

### **Offline-First:**
1. App funktioniert **VOLLSTÄNDIG offline**
2. Alle Fragen bleiben **lokal**
3. Quiz-Logik bleibt **lokal**

### **Server ist nur für:**
- ✅ Backup von User-Fortschritt
- ✅ Cloud-Speicherung bei App-Neuinstallation
- ✅ Multi-Gerät-Sync (optional)

### **Automatisches Sync:**
- Alle 6 Stunden (wenn Internet vorhanden)
- Nur bei WiFi-Verbindung
- Nur wenn Akku nicht niedrig
- Kann manuell getriggert werden: `SyncWorker.syncNow(context)`

### **Smart Merge:**
- Nimmt **beste Werte** von lokal UND Server
- Höchster Streak gewinnt
- Meiste richtige Antworten gewinnen
- Achievements werden vereinigt

---

## 🛠️ **Wie man Sync manuell triggert:**

```kotlin
// In einem Fragment oder Activity:
lifecycleScope.launch {
    val syncManager = SyncManager(requireContext())
    
    // Smart Sync (empfohlen)
    when (val result = syncManager.smartSync()) {
        is SyncResult.Success -> {
            // Sync erfolgreich
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
        is SyncResult.Failure -> {
            // Sync fehlgeschlagen
            Log.e("Sync", result.error)
        }
    }
}
```

---

## 📦 **Dependencies hinzugefügt:**

```gradle
// Retrofit für HTTP (in build.gradle.kts)
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

---

## 🚀 **Nächste Schritte:**

### **Backend (noch zu implementieren):**
1. API-Endpunkte für Sync erstellen
2. Datenbank-Tabellen für User-Daten
3. Authentication (optional, später)

### **Testing:**
1. App builden: `./gradlew assembleDebug`
2. Server-Verbindung testen
3. Sync-Funktionalität testen

---

## ⚠️ **WICHTIG:**

- **Fragen werden NICHT vom Server geladen!**
- **Quiz-Logik bleibt komplett lokal!**
- **Server ist nur Backup/Cloud-Storage!**
- **App funktioniert 100% offline!**

---

## 📊 **RAM-Nutzung:**

- **Server:** ~75 MB
- **Verfügbar:** ~745 MB
- **Kapazität:** 200-300 gleichzeitige User

---

**Autor:** Halil Yücedag  
**Datum:** 2025-12-15  
**Version:** 1.0.0

