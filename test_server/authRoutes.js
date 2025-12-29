const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const nodemailer = require('nodemailer');
const { runQuery, getQuery } = require('./database');

const router = express.Router();

// JWT Secret (sollte in .env gespeichert werden)
const JWT_SECRET = process.env.JWT_SECRET || 'klcp-super-secret-key-2025';

// Email-Konfiguration
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: 587,
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS
  }
});

// Middleware für JWT-Verifikation
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ message: 'Access token required' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ message: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
}

// Registrierung
router.post('/register', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }

    // Email-Validierung
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: 'Invalid email format' });
    }

    // Passwort-Validierung (mindestens 8 Zeichen)
    if (password.length < 8) {
      return res.status(400).json({ message: 'Password must be at least 8 characters long' });
    }

    // Prüfen ob User bereits existiert
    const existingUser = await getQuery('SELECT id FROM users WHERE email = ?', [email]);
    if (existingUser) {
      return res.status(409).json({ message: 'User already exists' });
    }

    // User-ID und Verification-Token generieren
    const userId = uuidv4();
    const verificationToken = uuidv4();

    // Passwort hashen
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // User in Datenbank speichern
    await runQuery(
      'INSERT INTO users (id, email, password_hash, verification_token) VALUES (?, ?, ?, ?)',
      [userId, email, passwordHash, verificationToken]
    );

    // Verifikations-Email senden (falls SMTP konfiguriert)
    if (process.env.SMTP_USER && process.env.SMTP_PASS) {
      try {
        const verificationUrl = `${process.env.BASE_URL || 'https://klcp.alie.info'}/api/auth/verify?token=${verificationToken}`;

        await transporter.sendMail({
          from: process.env.SMTP_USER,
          to: email,
          subject: 'Verify your KLCP Quiz Account',
          html: `
            <h2>Welcome to KLCP Quiz!</h2>
            <p>Please click the link below to verify your email address:</p>
            <a href="${verificationUrl}">Verify Email</a>
            <p>This link will expire in 24 hours.</p>
            <p>If you didn't create an account, you can safely ignore this email.</p>
          `
        });
      } catch (emailError) {
        console.error('Email sending failed:', emailError);
        // Email-Fehler nicht als kritisch behandeln
      }
    }

    // JWT-Token für sofortige Anmeldung erstellen
    const token = jwt.sign(
      { userId, email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.status(201).json({
      message: 'Registration successful',
      token,
      userId,
      email,
      isVerified: false
    });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }

    // User finden
    const user = await getQuery('SELECT * FROM users WHERE email = ?', [email]);
    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Passwort verifizieren
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // JWT-Token erstellen
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      message: 'Login successful',
      token,
      userId: user.id,
      email: user.email,
      isVerified: user.is_verified
    });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Email-Verifikation (für Browser-Link)
router.get('/verify', async (req, res) => {
  try {
    const { token } = req.query;

    if (!token) {
      return res.status(400).send('<h2>Invalid verification link</h2>');
    }

    // Token in Datenbank finden und User verifizieren
    const user = await getQuery('SELECT * FROM users WHERE verification_token = ?', [token]);
    if (!user) {
      return res.status(400).send('<h2>Invalid or expired verification token</h2>');
    }

    // User als verifiziert markieren und Token entfernen
    await runQuery(
      'UPDATE users SET is_verified = TRUE, verification_token = NULL WHERE id = ?',
      [user.id]
    );

    // JWT-Token für automatische Anmeldung erstellen
    const jwtToken = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.send(`
      <h2>Email verification successful!</h2>
      <p>Your account has been verified. You can now use the KLCP Quiz app.</p>
      <p>JWT Token (for app integration): ${jwtToken}</p>
      <script>
        // Versuche die App zu öffnen
        setTimeout(function() {
          window.location.href = 'klcp://verify?token=${jwtToken}';
        }, 2000);
      </script>
    `);

  } catch (error) {
    console.error('Verification error:', error);
    res.status(500).send('<h2>Verification failed</h2>');
  }
});

// Email-Verifikation für App (behaltet Token)
router.get('/verify-app', async (req, res) => {
  try {
    const { token } = req.query;

    if (!token) {
      return res.status(400).json({ message: 'Verification token required' });
    }

    // Token in Datenbank finden und User verifizieren
    const user = await getQuery('SELECT * FROM users WHERE verification_token = ?', [token]);
    if (!user) {
      return res.status(400).json({ message: 'Invalid or expired verification token' });
    }

    // User als verifiziert markieren (Token bleibt für App-Auto-Login)
    await runQuery('UPDATE users SET is_verified = TRUE WHERE id = ?', [user.id]);

    // JWT-Token für App erstellen
    const jwtToken = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      message: 'Email verified successfully',
      token: jwtToken,
      userId: user.id,
      email: user.email,
      isVerified: true
    });

  } catch (error) {
    console.error('App verification error:', error);
    res.status(500).json({ message: 'Verification failed' });
  }
});

// Account löschen
router.delete('/delete-account', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Alle User-Daten löschen (kaskadierende Deletes durch FOREIGN KEYs)
    await runQuery('DELETE FROM users WHERE id = ?', [userId]);

    res.json({ message: 'Account deleted successfully' });

  } catch (error) {
    console.error('Account deletion error:', error);
    res.status(500).json({ message: 'Account deletion failed' });
  }
});

module.exports = router;
