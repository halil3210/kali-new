#!/bin/bash
# Befehle für Hetzner Server Terminal
# Führe diese Befehle auf dem Server aus, um die AI-Verbindung zu erlauben

# 1. Meine IP-Adresse entbannen (ersetze 94.134.176.91 mit der aktuellen IP)
MY_IP="94.134.176.91"

echo "=== Fail2ban Status ==="
fail2ban-client status sshd

echo ""
echo "=== IP-Adresse entbannen ==="
fail2ban-client set sshd unbanip $MY_IP

echo ""
echo "=== IP zur Whitelist hinzufügen (optional) ==="
# Füge meine IP zur Whitelist hinzu
echo "$MY_IP" >> /etc/fail2ban/jail.local 2>/dev/null || echo "# Whitelist IP" >> /etc/fail2ban/jail.local && echo "ignoreip = $MY_IP" >> /etc/fail2ban/jail.local

# Oder besser: In jail.local bearbeiten
# nano /etc/fail2ban/jail.local
# Unter [sshd] Sektion: ignoreip = 127.0.0.1/8 ::1 94.134.176.91

echo ""
echo "=== Fail2ban Rate-Limits anpassen (optional, weniger aggressiv) ==="
# Bearbeite /etc/fail2ban/jail.local:
# maxretry = 10 (statt 3)
# findtime = 600 (10 Minuten statt 10 Minuten)
# bantime = 3600 (1 Stunde statt 10 Minuten)

echo ""
echo "=== Alternative: Fail2ban temporär deaktivieren (NUR für Tests!) ==="
# fail2ban-client stop sshd
# Oder: systemctl stop fail2ban

echo ""
echo "=== Status prüfen ==="
fail2ban-client status sshd | grep -E "(Banned|Currently)"

echo ""
echo "✅ Fertig! Die IP sollte jetzt nicht mehr blockiert werden."

