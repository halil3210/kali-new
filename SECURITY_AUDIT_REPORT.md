# 🔒 Sicherheitsaudit Report - KLCP Quiz Server & Codebase
**Datum:** 23. Dezember 2025  
**Server:** Hetzner 188.245.153.241  
**Codebase:** /home/halil/newmultichoice

---

## 📊 Executive Summary

**Gesamtbewertung:** ⚠️ **MITTEL-HOCH RISIKO**

Es wurden **8 kritische**, **5 hohe** und **7 mittlere** Sicherheitsprobleme identifiziert, die sofort behoben werden sollten.

---

## 🚨 KRITISCHE PROBLEME (Sofort beheben!)

### 1. Port 3000 ist öffentlich erreichbar
**Risiko:** 🔴 **KRITISCH**  
**Status:** Port 3000 ist von außen erreichbar (200 Response)

**Problem:**
```bash
# Port 3000 ist extern erreichbar
curl http://188.245.153.241:3000/api/health → 200 OK
```

**Lösung:**
```bash
# Firewall-Regel hinzufügen, um Port 3000 nur lokal zu erlauben
ufw deny 3000/tcp
# Oder nur localhost binden in server.js:
app.listen(PORT, '127.0.0.1', ...)
```

**Empfehlung:** Port 3000 sollte nur über Nginx Reverse Proxy (Port 443) erreichbar sein, nicht direkt.

---

### 2. SSH Root-Login aktiviert
**Risiko:** 🔴 **KRITISCH**  
**Status:** `PermitRootLogin yes` in `/etc/ssh/sshd_config`

**Problem:**
- Root-Login per Passwort ist aktiviert
- Erhöhtes Risiko bei Brute-Force-Angriffen

**Lösung:**
```bash
# SSH-Konfiguration ändern
sed -i 's/PermitRootLogin yes/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
# Oder besser: SSH-Key-basiertes Login einrichten und Root-Login deaktivieren
systemctl restart sshd
```

---

### 3. Hardcoded JWT Secret Fallback
**Risiko:** 🔴 **KRITISCH**  
**Datei:** `/opt/klcp-server/authRoutes.js:11`

**Problem:**
```javascript
const JWT_SECRET = process.env.JWT_SECRET || 'klcp-super-secret-key-2025';
```

**Lösung:**
```javascript
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET environment variable is required');
}
```

---

### 4. .env Datei hat unsichere Berechtigungen
**Risiko:** 🔴 **KRITISCH**  
**Status:** `-rw-rw-r--` (664) - sollte `600` sein

**Problem:**
```bash
-rw-rw-r-- 1 1000 1000 253 Dec 23 10:17 /opt/klcp-server/.env
```

**Lösung:**
```bash
chmod 600 /opt/klcp-server/.env
chown root:root /opt/klcp-server/.env
```

---

### 5. CORS erlaubt alle Origins
**Risiko:** 🔴 **KRITISCH**  
**Datei:** `/opt/klcp-server/server.js:23-26`

**Problem:**
```javascript
app.use(cors({
  origin: true, // Erlaubt alle Origins (für Entwicklung)
  credentials: true
}));
```

**Lösung:**
```javascript
app.use(cors({
  origin: ['https://klcp.alie.info', 'https://www.klcp.alie.info'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

---

### 6. JWT Token in HTML ausgegeben (XSS Risiko)
**Risiko:** 🔴 **KRITISCH**  
**Datei:** `/opt/klcp-server/authRoutes.js:199`

**Problem:**
```javascript
res.send(`
  <h2>Email verification successful!</h2>
  <p>JWT Token (for app integration): ${jwtToken}</p>
  ...
`);
```

**Lösung:**
- Token nicht im HTML ausgeben
- Token nur per POST-Request oder Deep Link übergeben
- HTML-Escape für alle User-Inputs

---

### 7. Hardcoded Passwörter in build.gradle.kts
**Risiko:** 🔴 **KRITISCH**  
**Datei:** `app/build.gradle.kts:36-38`

**Problem:**
```kotlin
storePassword = localProperties.getProperty("storePassword", "klcp2024secure")
keyPassword = localProperties.getProperty("keyPassword", "klcp2024secure")
```

**Lösung:**
- Fallback-Passwörter entfernen
- Fehler werfen, wenn nicht konfiguriert

---

### 8. Express Trust Proxy nicht konfiguriert
**Risiko:** 🔴 **KRITISCH**  
**Datei:** `/opt/klcp-server/server.js`

**Problem:**
- Rate Limiting kann umgangen werden durch X-Forwarded-For Header
- Logs zeigen: "trust proxy setting is false"

**Lösung:**
```javascript
// Nach app.use(helmet(...))
app.set('trust proxy', 1); // Trust first proxy (Nginx)
```

---

## ⚠️ HOHE PROBLEME

### 9. System-Updates verfügbar
**Risiko:** 🟠 **HOCH**  
**Status:** Mehrere Pakete haben Updates verfügbar

**Lösung:**
```bash
apt update && apt upgrade -y
apt autoremove -y
```

---

### 10. Datenbank-Datei Berechtigungen
**Risiko:** 🟠 **HOCH**  
**Status:** `-rw-r--r--` (644) - sollte `600` sein

**Lösung:**
```bash
chmod 600 /opt/klcp-server/klcp_quiz.db
chown root:root /opt/klcp-server/klcp_quiz.db
```

---

### 11. PM2 läuft als Root
**Risiko:** 🟠 **HOCH**  
**Status:** PM2 und Node.js-Prozess laufen als root

**Lösung:**
```bash
# Dedizierten User erstellen
useradd -r -s /bin/false klcp-server
chown -R klcp-server:klcp-server /opt/klcp-server
# PM2 als User starten
su - klcp-server -c "pm2 start server.js"
```

---

### 12. Keine Rate Limiting auf Health Endpoint
**Risiko:** 🟠 **HOCH**  
**Datei:** `/opt/klcp-server/server.js:60`

**Problem:**
- Health Endpoint hat kein Rate Limiting
- Kann für DDoS missbraucht werden

**Lösung:**
```javascript
const healthLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 Minute
  max: 10
});
app.get('/api/health', healthLimiter, (req, res) => { ... });
```

---

### 13. Fehlende Security Headers in Nginx
**Risiko:** 🟠 **HOCH**  
**Status:** Nginx hat nur basic Security Headers

**Lösung:**
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline';" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

---

## ⚡ MITTLERE PROBLEME

### 14. Passwort-Validierung zu schwach
**Risiko:** 🟡 **MITTEL**  
**Datei:** `/opt/klcp-server/authRoutes.js:58`

**Problem:**
- Nur Mindestlänge 8 Zeichen
- Keine Komplexitätsprüfung

**Lösung:**
- Mindestens 12 Zeichen
- Groß-/Kleinbuchstaben, Zahlen, Sonderzeichen erforderlich
- Passwort-Stärke-Meter implementieren

---

### 15. JWT Token Ablaufzeit zu lang
**Risiko:** 🟡 **MITTEL**  
**Datei:** `/opt/klcp-server/authRoutes.js:109`

**Problem:**
```javascript
{ expiresIn: '30d' } // 30 Tage ist sehr lang
```

**Lösung:**
- Access Token: 15 Minuten
- Refresh Token: 7 Tage
- Refresh Token Mechanismus implementieren

---

### 16. Keine Account-Lockout bei fehlgeschlagenen Logins
**Risiko:** 🟡 **MITTEL**  
**Status:** Keine Brute-Force-Schutz

**Lösung:**
- Nach 5 fehlgeschlagenen Versuchen Account temporär sperren
- Exponential Backoff implementieren

---

### 17. Keine Log-Rotation konfiguriert
**Risiko:** 🟡 **MITTEL**  
**Status:** PM2 Logs können unbegrenzt wachsen

**Lösung:**
```bash
pm2 install pm2-logrotate
pm2 set pm2-logrotate:max_size 10M
pm2 set pm2-logrotate:retain 7
```

---

### 18. Keine Input-Sanitization für Email
**Risiko:** 🟡 **MITTEL**  
**Datei:** `/opt/klcp-server/authRoutes.js:52`

**Problem:**
- Nur Regex-Validierung, keine Sanitization

**Lösung:**
- Email-Normalisierung (lowercase, trim)
- XSS-Schutz für alle User-Inputs

---

### 19. Fehlende Request-ID für Logging
**Risiko:** 🟡 **MITTEL**  
**Status:** Schwierig, Requests zu tracken

**Lösung:**
```javascript
const { v4: uuidv4 } = require('uuid');
app.use((req, res, next) => {
  req.id = uuidv4();
  res.setHeader('X-Request-ID', req.id);
  next();
});
```

---

### 20. Keine Monitoring/Alerting
**Risiko:** 🟡 **MITTEL**  
**Status:** Keine automatische Überwachung

**Lösung:**
- PM2 Monitoring einrichten
- Uptime-Monitoring (z.B. UptimeRobot)
- Error-Alerts konfigurieren

---

## ✅ POSITIVE ASPEKTE

1. ✅ **Fail2ban aktiv** - Schutz vor Brute-Force-Angriffen
2. ✅ **Firewall aktiv** - UFW ist konfiguriert
3. ✅ **HTTPS/SSL** - Let's Encrypt Zertifikate aktiv
4. ✅ **Helmet.js** - Security Headers Middleware
5. ✅ **Rate Limiting** - Express Rate Limit aktiv
6. ✅ **bcrypt** - Passwörter werden gehasht (12 Runden)
7. ✅ **Prepared Statements** - SQL Injection Schutz durch Parameterized Queries
8. ✅ **CORS aktiv** - Cross-Origin-Schutz (aber zu permissiv)
9. ✅ **Nginx Reverse Proxy** - Server nicht direkt exponiert (aber Port 3000 trotzdem erreichbar)

---

## 📋 SOFORTMASSNAHMEN (Priorität 1)

1. **Port 3000 blockieren** - Nur über Nginx erreichbar machen
2. **SSH Root-Login deaktivieren** - SSH-Key-basiertes Login einrichten
3. **.env Berechtigungen ändern** - `chmod 600`
4. **CORS einschränken** - Nur erlaubte Domains
5. **JWT Secret Fallback entfernen** - Fehler werfen wenn nicht gesetzt
6. **Trust Proxy aktivieren** - `app.set('trust proxy', 1)`

---

## 📋 WICHTIGE MASSNAHMEN (Priorität 2)

1. **System-Updates installieren**
2. **Datenbank-Berechtigungen ändern** - `chmod 600`
3. **PM2 als non-root User laufen lassen**
4. **Security Headers in Nginx hinzufügen**
5. **JWT Token aus HTML entfernen**
6. **Hardcoded Passwörter aus build.gradle.kts entfernen**

---

## 📋 VERBESSERUNGEN (Priorität 3)

1. **Passwort-Validierung verschärfen**
2. **JWT Token Ablaufzeit verkürzen** + Refresh Token
3. **Account-Lockout implementieren**
4. **Log-Rotation konfigurieren**
5. **Monitoring/Alerting einrichten**

---

## 🔧 QUICK FIX SCRIPT

```bash
#!/bin/bash
# Sicherheits-Fixes für Hetzner Server

# 1. Port 3000 blockieren
ufw deny 3000/tcp

# 2. .env Berechtigungen
chmod 600 /opt/klcp-server/.env
chown root:root /opt/klcp-server/.env

# 3. Datenbank Berechtigungen
chmod 600 /opt/klcp-server/klcp_quiz.db
chown root:root /opt/klcp-server/klcp_quiz.db

# 4. SSH Root-Login deaktivieren
sed -i 's/PermitRootLogin yes/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
systemctl restart sshd

# 5. System-Updates
apt update && apt upgrade -y

echo "✅ Sicherheits-Fixes angewendet!"
```

---

## 📊 RISIKO-MATRIX

| Kategorie | Kritisch | Hoch | Mittel | Niedrig |
|-----------|----------|------|--------|---------|
| **Server** | 3 | 2 | 2 | 0 |
| **Code** | 5 | 1 | 5 | 0 |
| **Konfiguration** | 0 | 2 | 0 | 0 |
| **Gesamt** | **8** | **5** | **7** | **0** |

---

## 📝 NÄCHSTE SCHRITTE

1. ✅ Sofortmaßnahmen (Priorität 1) innerhalb von 24 Stunden umsetzen
2. ✅ Wichtige Maßnahmen (Priorität 2) innerhalb von 1 Woche umsetzen
3. ✅ Verbesserungen (Priorität 3) innerhalb von 1 Monat umsetzen
4. ✅ Regelmäßige Sicherheitsaudits (alle 3 Monate)
5. ✅ Penetration Testing durchführen

---

**Report erstellt von:** AI Security Auditor  
**Nächster Audit:** 23. März 2026

