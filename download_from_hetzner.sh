#!/bin/bash
# Befehle für Hetzner-Konsole - HTTP-Server starten

# 1. In /tmp wechseln
cd /tmp

# 2. Python HTTP-Server starten (Port 8080)
python3 -m http.server 8080

# Dann vom Laptop aus:
# curl http://188.245.153.241:8080/klcp-backup.tar.gz -o ~/newmultichoice/klcp-server-hetzner-backup.tar.gz

