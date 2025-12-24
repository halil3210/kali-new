#!/bin/bash
# Setup-Befehle für neuen Hetzner-Server
# Führe diese Befehle in der Hetzner-Konsole aus

# 1. SSH-Verzeichnis erstellen
mkdir -p /root/.ssh
chmod 700 /root/.ssh

# 2. SSH-Key hinzufügen (FÜGE DEN PUBLIC KEY HIER EIN)
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQCp0Bt2brlgVzmhmHKQZq8xphhI59mJs39M3Mau6lPOaXqNP/ynhv6ogdx691u71s793BpgqIQD/Rd9BZOodfJKqfhyZQ8T1OMfHbOZJSeM5ML8ZWfHEOyNCggBg1BnciV3Jkl06TqrkHpbDXzB80nr3lkhz//O1s9y9JXzKQ7mNuo5dcG4EtrzSBw/W9RSXP+SzWoSQvZA9UFUR7Tfmz29M3/J8ETPjzQbxiJHu3G5TWLfp1FaHjMYXg1Yb6nNUZ3EZWB2noFIpjrlhd6B2lHWLqUDnXcFXMSSmht4iSS219fvxdzz2poRrHJseQOiMW4xXZUMuq47vDSzPQInzzQNOSlJo9HtVZge4vEsfL69vFBUVy33HTWoIG48xdD3dOA3Spsx2UTvNLkW1dW3nun9lqX/buDRecnMcEyEvRTGkOwVzeUwc7t8PhQZDU+IJ9bCHtGpcMPf98ZiuBsJBV5Cxm48GXYQRkG4Mj1hzpbNIkiQBMgce9gFRwooGkLBAohIljRzgIISZKBVZZCIXLR2h3e8wPC6PYselQ9NXvBV8/yAnFtllPvGvWbY2p/2EhoKDSVKJ83gXzQa84V06xyPH+q0D/PVBjIl4BxOdJxTJdtKwsUxJcHpCDMCyloRFaF/J6VMuJkp5GLQx1VtgG+fsCNFE4qcg9sMUjqET9jX6Q== klcp-security@halil.local" > /root/.ssh/authorized_keys

# 3. Berechtigungen setzen
chmod 600 /root/.ssh/authorized_keys
chown root:root /root/.ssh/authorized_keys

# 4. SSH-Konfiguration anpassen (Root-Login mit Key erlauben)
sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
sed -i 's/PermitRootLogin yes/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config

# 5. SSH neu starten
systemctl restart sshd

# 6. Firewall konfigurieren
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# 7. fail2ban installieren und konfigurieren
apt update
apt install -y fail2ban

# 8. Meine IP zur fail2ban Whitelist hinzufügen
echo "[sshd]" >> /etc/fail2ban/jail.local
echo "ignoreip = 127.0.0.1/8 ::1 94.134.176.91" >> /etc/fail2ban/jail.local

# 9. fail2ban starten
systemctl enable fail2ban
systemctl start fail2ban

# 10. Status prüfen
echo "=== SSH Status ==="
systemctl status sshd | head -5
echo ""
echo "=== Firewall Status ==="
ufw status
echo ""
echo "=== fail2ban Status ==="
fail2ban-client status sshd 2>/dev/null || echo "fail2ban läuft noch nicht"

