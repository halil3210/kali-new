# KLCP Quiz Server v2.0

Vollständiger Server für die KLCP Quiz Android-App mit allen Sync-Features.

## 🚀 Schnellstart

```bash
# Dependencies installieren
npm install

# .env-Datei erstellen (aus .env.example kopieren)
cp .env.example .env

# Server starten
npm start
```

## 📋 API-Endpunkte

### Authentifizierung
- `POST /api/auth/register` - User registrieren
- `POST /api/auth/login` - User einloggen
- `GET /api/auth/verify` - Email verifizieren
- `GET /api/auth/verify-app` - App-Verifikation
- `DELETE /api/auth/delete-account` - Account löschen

### Synchronisation
- `POST /api/sync/upload` - Daten hochladen
- `GET /api/sync/download/:deviceId` - Daten herunterladen

### Statistiken
- `GET /api/stats/user/:deviceId` - User-Stats abrufen
- `POST /api/stats/update` - Stats aktualisieren
- `GET /api/stats/unlock-status/:deviceId` - Achievements & Unlocks
- `GET /api/stats/exam-unlock/:deviceId/:examNumber` - Exam-Status prüfen
- `POST /api/stats/unlock-exam` - Exam freischalten

### Sessions
- `GET /api/sessions/:deviceId` - Sessions abrufen
- `POST /api/sessions/save` - Session speichern

### Backup
- `POST /api/backup/create` - Backup erstellen
- `GET /api/backup/restore/:deviceId` - Backup abrufen
- `POST /api/backup/restore/:deviceId` - Backup wiederherstellen

## 🔧 Konfiguration

### .env-Datei

```env
# Server
PORT=3000
NODE_ENV=production

# Sicherheit
JWT_SECRET=dein-super-geheimer-schluessel

# Email (optional)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=deine-email@gmail.com
SMTP_PASS=dein-app-passwort

# URLs
BASE_URL=https://deine-domain.com
```

### Datenbank

Die SQLite-Datenbank wird automatisch erstellt mit folgenden Tabellen:
- `users` - User-Accounts
- `quiz_sessions` - Quiz-Sessions
- `user_stats` - User-Statistiken
- `achievements` - Freigeschaltete Achievements
- `exam_unlocks` - Freigeschaltete Exams
- `backup_data` - Backup-Daten

## 🛡️ Sicherheit

- JWT-Token für Authentifizierung
- Rate-Limiting (100 Requests/15min)
- Strenges Auth-Limiting (10 Requests/15min)
- Helmet für Security-Headers
- CORS-Konfiguration
- Passwort-Hashing mit bcrypt

## 📊 Monitoring

- Health-Check: `GET /api/health`
- Uptime-Monitoring verfügbar
- Request-Logging im Debug-Modus

## 🚀 Deployment

```bash
# PM2 für Production
npm install -g pm2
pm2 start server.js --name "klcp-server"
pm2 save
pm2 startup
```

## 🐛 Troubleshooting

### Server startet nicht
- Node.js Version prüfen: `node --version` (min. 18.0.0)
- Dependencies installieren: `npm install`
- .env-Datei prüfen

### Datenbank-Fehler
- Schreibrechte für das Verzeichnis prüfen
- SQLite3 installiert: `npm list sqlite3`

### Email funktioniert nicht
- SMTP-Credentials in .env prüfen
- Gmail: App-Password anstatt normales Passwort verwenden
