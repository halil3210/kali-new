# KLCP Server - Anleitung für AI-Modelle

## KRITISCHE FEHLER DIE VERMIEDEN WERDEN MÜSSEN

### 1. HTML in JavaScript KORREKT einbetten

**FALSCH** (was andere Modelle gemacht haben):

```javascript
await transporter.sendMail({
  from: process.env.SMTP_USER,
  to: email,
  subject: "Subject",
<!DOCTYPE html>        // <-- HTML schwebt hier ohne Property-Name!
<html>...</html>
});
```

**RICHTIG**:

```javascript
const htmlContent = `<!DOCTYPE html>
<html>...</html>`;

await transporter.sendMail({
  from: process.env.SMTP_USER,
  to: email,
  subject: 'Subject',
  html: htmlContent      // <-- HTML muss als Wert der 'html' Property!
});
```

### 2. NIEMALS sed für mehrzeilige HTML-Ersetzungen verwenden

`sed` ist für einzeilige Ersetzungen gedacht. Bei mehrzeiligem HTML:
- Quoting-Probleme
- Escape-Probleme mit `$`, `<`, `>`, `"`, `'`
- Zeilenumbruch-Probleme

**Lösung**: Datei komplett neu schreiben mit heredoc oder die Datei lokal erstellen und per scp kopieren.

### 3. Datenbank-Schema prüfen BEVOR Code deployed wird

Wenn Code eine Spalte verwendet (z.B. `verification_code`), muss diese in der Datenbank existieren:

```bash
sqlite3 database.db '.schema tablename'
# Falls Spalte fehlt:
sqlite3 database.db 'ALTER TABLE tablename ADD COLUMN columnname TYPE;'
```

### 4. Port-Konflikte beheben

Vor Server-Neustart immer Port freigeben:

```bash
fuser -k 3000/tcp 2>/dev/null || true
pm2 restart appname
```

### 5. Boolean vs Integer in SQLite

SQLite speichert Booleans als 0/1. Für JSON-APIs die Boolean erwarten:

```javascript
// FALSCH:
isVerified: user.is_verified    // gibt 0 oder 1 zurück

// RICHTIG:
isVerified: !!user.is_verified  // gibt true oder false zurück
```

### 6. Gmail SMTP Konfiguration

- Das `from` Feld MUSS die authentifizierte Email sein (nicht beliebig wählbar)
- App-Passwort erforderlich (nicht das normale Gmail-Passwort)
- Format: xxxx xxxx xxxx xxxx (4 Gruppen à 4 Zeichen)

## Server-Struktur

```
/opt/klcp-server/
├── server.js          # Express-Server Entry Point
├── authRoutes.js      # Auth-Endpunkte (Register, Login, Verify)
├── database.js        # SQLite-Wrapper
├── .env               # Umgebungsvariablen (SMTP_USER, SMTP_PASS, etc.)
├── klcp_quiz.db       # SQLite-Datenbank
└── node_modules/
```

## Wichtige Befehle

```bash
# Server-Status
pm2 list
pm2 logs klcp-server --lines 50

# Server neustarten
fuser -k 3000/tcp 2>/dev/null || true
pm2 restart klcp-server

# Datenbank prüfen
sqlite3 /opt/klcp-server/klcp_quiz.db '.tables'
sqlite3 /opt/klcp-server/klcp_quiz.db '.schema users'
sqlite3 /opt/klcp-server/klcp_quiz.db 'SELECT * FROM users;'

# API testen
curl http://localhost:3000/api/health
curl -X POST http://localhost:3000/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"test12345"}'
```

## Zusammenfassung der Fehler die andere Modelle gemacht haben

1. **HTML ohne `html:` Property** - JavaScript-Syntax-Fehler der still ignoriert wird, Email wird ohne Body gesendet
2. **sed für HTML** - Funktioniert nicht mit mehrzeiligem Content, Shell-Quoting macht alles kaputt
3. **Fehlende DB-Spalten** - Schema muss zum Code passen, vor Deploy prüfen
4. **Port-Konflikte** - Alte Prozesse müssen beendet werden bevor neuer Server startet
5. **Boolean-Typen** - SQLite gibt Integer (0/1), Gson erwartet Boolean (true/false)
6. **Gmail from-Feld** - Muss authentifizierte Adresse sein, sonst wird Email abgelehnt

