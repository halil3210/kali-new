#!/bin/bash

# KLCP Server v2.0 Deployment Script für Hetzner
# Führe dieses Script auf dem Hetzner-Server aus

set -e

echo "🚀 KLCP Server v2.0 Deployment"
echo "================================"

# Server-Stoppen falls läuft
echo "📍 Stopping existing server..."
sudo pm2 stop klcp-server 2>/dev/null || echo "No existing server found"
sudo pm2 delete klcp-server 2>/dev/null || echo "No PM2 process found"

# Backup der alten Installation
echo "💾 Creating backup of old installation..."
if [ -d "/opt/klcp-quiz-server" ]; then
    sudo cp -r /opt/klcp-quiz-server /opt/klcp-quiz-server-backup-$(date +%Y%m%d_%H%M%S)
fi

# Neue Installation
echo "📦 Installing new server version..."
sudo mkdir -p /opt/klcp-server-v2.0
sudo tar -xzf klcp-server-v2.0.tar.gz -C /opt/klcp-server-v2.0

# Dependencies installieren
echo "📦 Installing dependencies..."
cd /opt/klcp-server-v2.0
sudo npm install --production

# .env-Datei erstellen
echo "⚙️  Configuring environment..."
cat > .env << EOF
PORT=3000
NODE_ENV=production
JWT_SECRET=klcp-production-secret-key-$(openssl rand -hex 32)
BASE_URL=https://klcp.alie.info
DATABASE_PATH=./klcp_quiz_v2.db
EOF

# Nginx-Konfiguration prüfen (falls vorhanden)
echo "🌐 Checking Nginx configuration..."
if [ -f "/etc/nginx/sites-available/klcp" ]; then
    echo "Nginx config exists - keeping current configuration"
else
    echo "No Nginx config found - you may need to configure reverse proxy"
fi

# Server starten
echo "🚀 Starting new server..."
sudo pm2 start server.js --name "klcp-server-v2"
sudo pm2 save

# Health-Check
echo "🏥 Testing server health..."
sleep 3
if curl -s http://localhost:3000/api/health | grep -q "status.*ok"; then
    echo "✅ Server is healthy!"
else
    echo "❌ Server health check failed!"
    exit 1
fi

# Firewall prüfen
echo "🔥 Checking firewall..."
sudo ufw status | grep 3000 || echo "Port 3000 may not be open in firewall"

echo ""
echo "🎉 Deployment completed successfully!"
echo "======================================"
echo "📊 Server URL: https://klcp.alie.info"
echo "🏥 Health Check: https://klcp.alie.info/api/health"
echo "📋 PM2 Status: sudo pm2 status"
echo "📄 Logs: sudo pm2 logs klcp-server-v2"
echo ""
echo "⚠️  IMPORTANT: Configure SMTP settings in .env for email verification"
echo "🔑 JWT Secret has been randomly generated for security"
