#!/bin/bash
# Befehle für Hetzner-Konsole - Server auf Laptop kopieren

# 1. Auf Hetzner: Server-Ordner komprimieren
cd /opt
tar -czf /tmp/klcp-server-hetzner-backup-$(date +%Y%m%d_%H%M%S).tar.gz klcp-server/

# 2. Auf Hetzner: Datei-Größe prüfen
ls -lh /tmp/klcp-server-hetzner-backup-*.tar.gz

# 3. VOM LAPTOP AUS: Server vom Hetzner kopieren
# Führe diesen Befehl auf dem LAPTOP aus (nicht auf Hetzner!):
# scp root@188.245.153.241:/tmp/klcp-server-hetzner-backup-*.tar.gz ~/newmultichoice/

# 4. Auf Hetzner: Temporäre Datei löschen (optional)
# rm /tmp/klcp-server-hetzner-backup-*.tar.gz

